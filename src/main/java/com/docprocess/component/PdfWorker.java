package com.docprocess.component;

import com.docprocess.config.ErrorConfig;
import com.docprocess.constant.PdfQueueProcessingStatus;
import com.docprocess.manager.DocumentRenderException;
import com.docprocess.manager.HttpHandler;
import com.docprocess.manager.S3BucketManager;
import com.docprocess.pojo.PdfGenerationQueueResponse;
import com.docprocess.pojo.PdfGenerationRequest;
import com.docprocess.service.PdfGenerationService;
import com.docprocess.service.TemplateService;
import com.docprocess.service.impl.PdfGenerationServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

@Component
public class PdfWorker {
    private final PdfGenerationService pdfGenerationService;
    private final JmsTemplate jmsTemplate;
    private final S3BucketManager s3Mgr;
    private final String s3DocumentBucketName;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private HttpHandler handler;

    @Autowired
    private TemplateService templateService;

    Logger logger = LogManager.getLogger(PdfWorker.class);

    public PdfWorker(PdfGenerationService pdfGenerationService, JmsTemplate jmsTemplate, S3BucketManager s3Mgr, String s3DocumentBucketName) {
        this.pdfGenerationService = pdfGenerationService;
        this.jmsTemplate = jmsTemplate;
        this.s3Mgr = s3Mgr;
        this.s3DocumentBucketName = s3DocumentBucketName;
    }

    @JmsListener(destination = "pdf.generation.request")
    public void processPdfGenerationRequest(PdfGenerationRequest message) {

        Observable.fromIterable(pdfGenerationService.getPendingTemplate(message.getRequestId()))
                .observeOn(Schedulers.io())
                .map(template ->{
                    LinkedHashMap<String,Object> templateJson = mapper.readValue(new File(
                            templateService.getMessageType("i18n.MotorCar")), new TypeReference<LinkedHashMap<String,Object>>() {
                    });
                    Locale locale = getLocale(template);
                    message.getContext().put("created_from",message.getCallBackUrl().contains("documentstatusapi")?"sf":"web");
                    LinkedHashMap<String,Object> localMap =  (LinkedHashMap<String,Object>) templateJson.get(message.getLocale().toUpperCase());
                    return pdfGenerationService.generatePdfFromTemplate(template, message.getRequestId(), message.getContext(),locale, localMap);
                })
                .lastElement()
                .toSingle()
                .map(__ -> pdfGenerationService.finalizeRenderedFolder(message.getRequestId()))
                .doOnSuccess(renderedFolder ->{
                    Map<String,String> uploadObj=new HashMap<String,String>();
                    uploadObj.put("RenderedFolderPath",renderedFolder.getAbsolutePath());
                    uploadObj.put("CallBackURL",message.getCallBackUrl());
                    uploadObj.put("EmailTemplateName",message.getEmailTemplateName());
                    uploadObj.put("QuoteId",message.getQuoteId());
                    uploadObj.put("RequestId",message.getRequestId());
                    jmsTemplate.convertAndSend("pdf.upload.request", uploadObj);

                })
                .doOnError(__ -> pdfGenerationService.cleanupIntermediaryFolder(message.getRequestId()))
                .subscribe();
    }

    public Locale getLocale(File file){
        String baseName = FilenameUtils.getBaseName(file.getName());
        return new Locale(file.getName().substring(baseName.length() - "**".length() ,baseName.length()));
    }

    @JmsListener(destination = "pdf.upload.request")
    public void processPdfUploadRequest(Map<String,String> uploadInfo) {
        try {
            Single.just(uploadInfo.get("RenderedFolderPath"))
                    .observeOn(Schedulers.io())
                    .map(File::new)
                    // TODO: Uncomment this after testing
                    .flatMap(folder -> {
                        String s3Path=MessageFormat.format("validate/quoteSlip/{0}", folder.getName());
                        if(uploadInfo.get("QuoteId")!=null)
                            s3Path=uploadInfo.get("QuoteId");
                        logger.info("uploading PDF to S3 "+s3Path);
                        return s3Mgr.uploadDirectory(s3DocumentBucketName, s3Path, folder)
                                .doOnSuccess(__ -> {
                                    pdfGenerationService.finalizeUpdatedFolder(folder.getName());

                                    if (uploadInfo.get("CallBackURL").contains("documentstatusapi")) {
                                        JSONObject jsonBody = new JSONObject();
                                        JSONObject reqData = new JSONObject();
                                        reqData.put("requestId", uploadInfo.get("RequestId"));
                                        reqData.put("quoteId", uploadInfo.get("QuoteId"));
                                        reqData.put("emailTemplateName", uploadInfo.get("EmailTemplateName"));
                                        reqData.put("status", "success");
                                        jsonBody.put("apiRequest", reqData);
                                        logger.info("PDF uploaded and to call back Salesforce");
                                        handler.notifySalesforce(jsonBody, "/services/apexrest/documentstatusapi");
                                    }
                                });
                    })
                    .subscribe();
        }catch (Exception ex){

            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "processPdfUploadRequest", ex);
            logger.error(errorMessage);
        }
    }

    /*@JmsListener(destination = "pdf.salesforce.callback")
    public void notifyDocStatusToSalesforce(String requestId) {
        Single.just(renderedFolderPath)
                .observeOn(Schedulers.io())
                .map(File::new)
                // TODO: Uncomment this after testing
                .flatMap(folder -> s3Mgr
                        .uploadDirectory(s3DocumentBucketName, MessageFormat.format("validate/quoteSlip/{0}", folder.getName()), folder)
                        .doOnSuccess(__ -> pdfGenerationService.finalizeUpdatedFolder(folder.getName()))
                )
                .subscribe();
    }*/

}
