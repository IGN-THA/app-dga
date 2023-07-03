package com.docprocess.job;

import com.docprocess.config.ConfigConstant;
import com.docprocess.manager.CacheManager;
import com.docprocess.manager.HttpHandler;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.model.DocumentProcessLog;
import com.docprocess.model.DocumentProcessPending;
import com.docprocess.model.DocumentTypeData;
import com.docprocess.repository.*;
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

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long batchStart = System.currentTimeMillis();

        System.out.println("Post PDF Document Process Batch started"+ batchStart);

        ArrayList<String> parentList = new ArrayList<String>();
        List<DocumentProcessPending> documentProcPendingList = documentProcessPendingRepository.findTop100ByReferenceNumberNotNullOrderByCreateddateAscReferenceNumberAsc();
        Boolean notifyDocEmail = false;
        Boolean tagDOcument = false;
        List<DocumentProcessLog> logList = new ArrayList<DocumentProcessLog>();
        for(DocumentProcessPending docProcss : documentProcPendingList){
            if(parentList.contains(docProcss.getReferenceNumber())) continue;
                DocumentGenerateQueueData docGenData = documentDataRepository.findBySfid (docProcss.getSfid());

            List<DocumentGenerateQueueData> docPendingToProcess = null;
            if(docGenData.getFlagEmailAttachmentReady()){
                if (docGenData.getFlagSendEmail() && (docProcss.getFlagNotifyEmail()==null ||
                        !docProcss.getFlagNotifyEmail())) {
                    if (docGenData.getAttachmentGroupCode() != null) {
                        docPendingToProcess = documentDataRepository.findAllByReferenceNumberAndAttachmentGroupCodeAndFlagSendEmailAndFlagEmailAttachmentReady(docGenData.getReferenceNumber(), docGenData.getAttachmentGroupCode(), docGenData.getFlagSendEmail(), false);
                    }
                    DocumentTypeData documentTypeData = documentTypeDataRepository.findByDocumentType(docGenData.getDocumentType());
                    if ((docPendingToProcess == null || docPendingToProcess.isEmpty()) && !documentTypeData.getForValidation())
                        notifyDocEmail = notifyDocGen(docGenData.getSfid());
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
                e.printStackTrace();
            }
        }
        System.out.println("Post Doc Process Batch End "+ System.currentTimeMillis() + " within " +(System.currentTimeMillis()-batchStart));

    }


    public Boolean notifyDocGen(String documentGenQueueId) {

        String salesforceNotificationAPIEndpoint = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_NOTIFICATION_API_ENDPOINT).getConfigValue();

        String responseMsg = "Success";

        HttpHandler handler = new HttpHandler();
        Map<String, String> headers = new HashMap<String, String>();
        Map<String, String> params = new HashMap<String, String>();
        Boolean apiSuccess = true;
        try {
            JSONObject response = null;
            JSONObject tokenCache = CacheManager.getTokenCache();
            String tokenType;
            String accessToken;
            String instanceURL;
            if (tokenCache == null || ((System.currentTimeMillis() - tokenCache.getLong("expiryTime")) >= 300000)) {

                String userName = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_USERNAME).getConfigValue();
                String password = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_PASSWORD).getConfigValue();
                String grantType = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_GRANT_TYPE).getConfigValue();
                String clientId = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_CLIENT_ID).getConfigValue();
                String clientSecret = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_CLIENT_SECRET).getConfigValue();
                String tokenURL = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_TOKEN_URL).getConfigValue();
                params.put("username", userName);
                params.put("password", password);
                params.put("grant_type", grantType);
                params.put("client_id", clientId);
                params.put("client_secret", clientSecret);
                headers.put("Content-Type", "application/json; charset=UTF-8");

                response = new JSONObject(handler.callRestAPI(null, tokenURL, headers, params));
                tokenType = response.getString("token_type");
                accessToken = response.getString("access_token");
                instanceURL = response.getString("instance_url");
                CacheManager.updateCache(tokenType, accessToken, instanceURL);
                tokenCache = CacheManager.getTokenCache();
            }

            tokenType = tokenCache.getString("token_type");
            accessToken = tokenCache.getString("access_token");
            instanceURL = tokenCache.getString("instance_url");


            headers = new HashMap<>();
            headers.put("Authorization", tokenType + " " + accessToken);
            headers.put("Content-Type", "application/json; charset=UTF-8");
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("docGenQueue", documentGenQueueId);
            response = new JSONObject(handler.callRestAPI(jsonBody.toString(), instanceURL + salesforceNotificationAPIEndpoint, headers, null));
            apiSuccess = Boolean.valueOf(response.get("success").toString());
        } catch (JSONException e) {
            System.out.println("[notifyDocGen][Error], "+ e.getMessage());
            responseMsg = "Fail: e.getMessage()";
            apiSuccess = false;
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

            responseMsg = handler.callRestAPI(tagList.toString(), fmsappUpdateTagAPI, headers, null);
        } catch (JSONException e) {
            System.out.println("[updateTagDetails] "+tagList);
            System.out.println("[updateTagDetails][Error], "+ e.getMessage());
            responseMsg = "Failed";
        }
        return responseMsg.equals("Success");
    }
}
