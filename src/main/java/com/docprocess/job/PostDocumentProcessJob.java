package com.docprocess.job;

import com.docprocess.config.ConfigConstant;
import com.docprocess.config.ErrorConfig;
import com.docprocess.manager.CacheManager;
import com.docprocess.manager.DocumentRenderException;
import com.docprocess.manager.HttpHandler;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.model.DocumentProcessLog;
import com.docprocess.model.DocumentProcessPending;
import com.docprocess.model.DocumentTypeData;
import com.docprocess.repository.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DisallowConcurrentExecution
public class PostDocumentProcessJob  extends QuartzJobBean {

    @Autowired
    DocumentProcessPendingRepository documentProcessPendingRepository;

    @Autowired
    DocumentDataRepository documentDataRepository;

    @Autowired
    SystemConfigRepository systemConfigRepository;

    @Autowired
    DocumentProcessLogRepository documentProcessLogRepository;

    @Autowired
    DocumentTypeDataRepository documentTypeDataRepository;

    @Autowired
    HttpHandler handler;

    Logger logger = LogManager.getLogger(PostDocumentProcessJob.class);

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long batchStart = System.currentTimeMillis();

        logger.info("Post PDF Document Process Batch started"+ batchStart);

        ArrayList<String> parentList = new ArrayList<String>();
        List<DocumentProcessPending> documentProcPendingList = documentProcessPendingRepository.findTop100ByReferenceNumberNotNullOrderByCreateddateAscReferenceNumberAsc();
        Boolean notifyDocEmail = false;
        Boolean tagDOcument = false;
        List<DocumentProcessLog> logList = new ArrayList<DocumentProcessLog>();
        for(DocumentProcessPending docProcss : documentProcPendingList){
            if(parentList.contains(docProcss.getReferenceNumber())) continue;
                DocumentGenerateQueueData docGenData = documentDataRepository.findBySfid (docProcss.getSfid());

            List<DocumentGenerateQueueData> docPendingToProcess = null;
            logger.info("Document Name :"+ docGenData.getSfid()+"  "+docGenData.getDocumentName());
            if(docGenData.getFlagEmailAttachmentReady()){
                if (docGenData.getFlagSendEmail() && (docProcss.getFlagNotifyEmail()==null ||
                        !docProcss.getFlagNotifyEmail())) {
                    if (docGenData.getAttachmentGroupCode() != null) {
                        docPendingToProcess = documentDataRepository.findAllByReferenceNumberAndAttachmentGroupCodeAndFlagSendEmailAndFlagEmailAttachmentReady(docGenData.getReferenceNumber(), docGenData.getAttachmentGroupCode(), docGenData.getFlagSendEmail(), false);
                    }
                    DocumentTypeData documentTypeData = documentTypeDataRepository.findByDocumentType(docGenData.getDocumentType());
                    if ((docPendingToProcess == null || docPendingToProcess.isEmpty()) && !documentTypeData.getForValidation()) {
                        notifyDocEmail = notifyDocGen(docGenData.getSfid());
                    }
                }else{
                    notifyDocEmail = docProcss.getFlagNotifyEmail();
                }
            }
            JSONObject jsonObj = new JSONObject();
            JSONObject tagObj = new JSONObject();
            //JSONArray jsonArr = new JSONArray();
            try {
                jsonObj.put("prefix", docGenData.getReferenceNumber());

                if (docGenData.getAttachmentGroupCode() != null) {

                    docPendingToProcess = documentDataRepository.findAllByReferenceNumberAndAttachmentGroupCodeAndFlagSendEmailAndFlagEmailAttachmentReady(docGenData.getReferenceNumber(), docGenData.getAttachmentGroupCode(), docGenData.getFlagSendEmail(), true);
                    for (DocumentGenerateQueueData docGenQData1 : docPendingToProcess) {
                        DocumentProcessLog proccLog = new DocumentProcessLog();

                        DocumentTypeData documentTypeData = documentTypeDataRepository.findByDocumentType(docGenQData1.getDocumentType());
                        if (documentTypeData.getUploadTagName() != null && documentTypeData.getUploadTagName().length()>0) {
                            proccLog.setUploadTagUpate(true);
                            tagObj.put(docGenQData1.getDocumentName() + ".pdf", new JSONArray().put(documentTypeData.getUploadTagName()));
                        }
                        proccLog.setId(docGenQData1.getId());
                        proccLog.setSfid(docGenQData1.getSfid());
                        proccLog.setFlagNotifyEmail(notifyDocEmail);
                        logList.add(proccLog);
                        //documentProcessLogRepository.saveAndFlush(proccLog);
                    }
                } else {
                    DocumentTypeData documentTypeData = documentTypeDataRepository.findByDocumentType(docGenData.getDocumentType());
                    DocumentProcessLog proccLog = new DocumentProcessLog();
                    if (documentTypeData.getUploadTagName() != null) {
                        proccLog.setUploadTagUpate(true);
                        tagObj.put(docGenData.getDocumentName() + ".pdf", new JSONArray().put(documentTypeData.getUploadTagName()));
                    }

                    proccLog.setId(docGenData.getId());
                    proccLog.setSfid(docGenData.getSfid());
                    proccLog.setFlagNotifyEmail(notifyDocEmail);

                    logList.add(proccLog);
                    //documentProcessLogRepository.saveAndFlush(proccLog);
                }
                if(tagObj.length()>0) {
                    jsonObj.put("tags", tagObj);
                    tagDOcument = updateTagDetails(jsonObj.toString());
                }

                for(DocumentProcessLog proccLog : logList){
                    proccLog.setUploadTagUpate(tagDOcument);
                    documentProcessLogRepository.saveAndFlush(proccLog);
                }
                parentList.add(docGenData.getReferenceNumber());
            } catch (JSONException e) {

                String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "executeInternal", e);
                logger.error(errorMessage);
            }
        }
        logger.info("Post Doc Process Batch End "+ System.currentTimeMillis() + " within " +(System.currentTimeMillis()-batchStart));

    }


    public Boolean notifyDocGen(String documentGenQueueId) {

        String salesforceNotificationAPIEndpoint = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_NOTIFICATION_API_ENDPOINT).getConfigValue();

        String responseMsg = "Success";


        Map<String, String> headers = new HashMap<String, String>();
        Map<String, String> params = new HashMap<String, String>();
        Boolean apiSuccess = true;
        try {
           // HttpHandler handler = new HttpHandler();
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("docGenQueue", documentGenQueueId);
            apiSuccess= handler.notifySalesforce(jsonBody,salesforceNotificationAPIEndpoint);
        } catch (JSONException e) {

            responseMsg = "Fail: e.getMessage()";
            apiSuccess = false;

            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "notifyDocGen", e);
            logger.error(errorMessage);
        }
        return apiSuccess;
    }

    public Boolean updateTagDetails(String tagList) {

        String fmsappAuthAPIEndpoint = systemConfigRepository.findByConfigKey(ConfigConstant.FMSAPP_AUTHENTICATE_API).getConfigValue();
        String fmsappUpdateTagAPI = systemConfigRepository.findByConfigKey(ConfigConstant.FMSAPP_UPDATETAG_API).getConfigValue();

        String responseMsg = "Success";

        HttpHandler handler = new HttpHandler();
        Map<String, String> headers = new HashMap<String, String>();
        Map<String, String> params = new HashMap<String, String>();

        try {
            JSONObject tokenCache = CacheManager.getFmsAppTokenCache();
            JSONObject response = null;
            String tokenType;
            String accessToken;
            String instanceURL;
            if (tokenCache == null || ((System.currentTimeMillis() - tokenCache.getLong("expiryTime")) >= 300000)) {

                String userName = systemConfigRepository.findByConfigKey(ConfigConstant.FMSAPP_USERNAME).getConfigValue();
                String password = systemConfigRepository.findByConfigKey(ConfigConstant.FMSAPP_PASSWORD).getConfigValue();

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("username", userName);
                jsonBody.put("password", password);

                headers.put("Content-Type", "application/json; charset=UTF-8");

                response = new JSONObject(handler.callRestAPI(jsonBody.toString(), fmsappAuthAPIEndpoint, headers, null));
                accessToken = response.getString("token");
                CacheManager.updateFmaAppCache("bearer", accessToken);
                tokenCache = CacheManager.getFmsAppTokenCache();
            }

            tokenType = tokenCache.getString("token_type");
            accessToken = tokenCache.getString("access_token");

            headers = new HashMap<>();
            headers.put("Authorization", tokenType + " " + accessToken);
            headers.put("Content-Type", "application/json; charset=UTF-8");

            responseMsg = handler.callRestAPI(tagList, fmsappUpdateTagAPI, headers, null);
        } catch (JSONException e) {
            responseMsg = "Failed";
            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "updateTagDetails", e);
            logger.error(errorMessage);
        }
        return responseMsg.equals("Success");
    }
}
