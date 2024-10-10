package com.docprocess.controller;

import com.docprocess.config.ConfigConstant;
import com.docprocess.manager.EmailServiceManager;
import com.docprocess.manager.S3BucketManager;
import com.docprocess.manager.docx.RenderDocumentManager;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.model.DocumentTypeData;

import com.docprocess.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/doc")
public class DocumentController {

    @Autowired
    private EntityManagerFactory sessionFactory;

    @Autowired
    DocumentTypeDataRepository documentTypeDataRepository;

    @Autowired
    SignatureCardDataRepository signatureCardDataRepository;

    @Autowired
    SystemConfigRepository systemConfigRepository;

    @Autowired
    ConstantParamRepository constantParamRepository;

    @Autowired
    DocumentDataRepository documentDataRepository;

    @Autowired
    EmailServiceManager mgr;


    @GetMapping("/syncDocument")
    @ResponseBody
    public String signDocumentService() {
        String accessKey = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_ACCESS_KEY).getConfigValue();
        String secretKey = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_SECRET_KEY).getConfigValue();
        String bucketName = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_HOME_BUCKET_NAME).getConfigValue();
        String bucketFolderPath = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_TEMPLATE_FOLDER_PATH).getConfigValue();
        String rootFilePath = systemConfigRepository.findByConfigKey(ConfigConstant.ROOT_FOLDER_TO_STORE_LOCAL_FILE).getConfigValue();
        String folderPathOnServer = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_TEMPLATE_PATH_ON_SERVER).getConfigValue();
        S3BucketManager s3Mgr = new S3BucketManager(accessKey, secretKey);
        s3Mgr.synchronizeDocumentToServer(bucketName, bucketFolderPath, folderPathOnServer, false);
        return "Success";
    }

    @GetMapping("/generateDocument")
    @ResponseBody
    public String generateDocument(@RequestParam String documentGenQueueId) {
        DocumentGenerateQueueData docData = documentDataRepository.findBySfid(documentGenQueueId);
        //for(DocumentGenerateQueueData docData : documentDataList) {
            DocumentTypeData docTypeData = documentTypeDataRepository.findByDocumentType(docData.getDocumentType());
            String tempName = docTypeData.getTemplateName();
            String tableName = docTypeData.getQueryName();
            String rootFilePath = systemConfigRepository.findByConfigKey(ConfigConstant.ROOT_FOLDER_TO_STORE_LOCAL_FILE).getConfigValue();
            String renderedFilePath = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.RENDERED_FILE_PATH).getConfigValue();
            String bucketFolderPath = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_TEMPLATE_FOLDER_PATH).getConfigValue();
            String folderPathOnServer = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_TEMPLATE_PATH_ON_SERVER).getConfigValue();
            boolean rederedDoc = false;
            try {
                RenderDocumentManager mgr = new RenderDocumentManager();
                rederedDoc = mgr.renderDocFromTemplate(renderedFilePath, folderPathOnServer+"/"+bucketFolderPath, folderPathOnServer+"/"+bucketFolderPath+"/"+tempName, tableName, sessionFactory, docData, constantParamRepository);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(rederedDoc) {
                docData.setRenderedDate(Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)));
                documentDataRepository.saveAndFlush(docData);
            }
        //}
        return "Success";
    }
/*
    @GetMapping("/updateDocStatus")
    @ResponseBody
    public String updateDocStatus() {

        return "Success";
    }*/
/*


    @PostMapping("/sign")
    @ResponseBody
    public String signDocumentService(@RequestBody(required = true) DocumentDataObj data) {
        DocumentGenerateQueueData dataTab = new DocumentGenerateQueueData();
        SignatureCardData signCardDataObj = null;
        try {
            String signatureCardData = documentTypeDataRepository.findByDocumentType(data.getDocumentType()).getSignattureCardName();
            signCardDataObj = signatureCardDataRepository.findBySignatureCardName(signatureCardData);

            String accessKey = systemConfigRepository.findByConfigKey("AWS_ACCESS_KEY").getConfigValue();
            String secretKey = systemConfigRepository.findByConfigKey("AWS_SECRET_KEY").getConfigValue();
            String bucketName = systemConfigRepository.findByConfigKey("AWS_S3_DOCUMENT_BUCKET_NAME").getConfigValue();
            String bucketFolderPath = systemConfigRepository.findByConfigKey("AWS_S3_BUCKET_FOLDER_PATH").getConfigValue();
            String folderPath = systemConfigRepository.findByConfigKey("DOCUMENT_STORAGE_PATH_FOR_PRINTING").getConfigValue();

            S3BucketManager s3Mgr = new S3BucketManager(accessKey, secretKey);
            InputStream inpStream = s3Mgr.getContent(bucketName, bucketFolderPath+"/"+data.getDocumentName());
            DigiSignDocManager docManager = new DigiSignDocManager();

            docManager.storeDocumentForPrinting(inpStream, folderPath+data.getDocumentName().replace(':',' '));

            if(data.getFlagRequireSign()) {
                ByteArrayOutputStream baos = docManager.signDocument(signCardDataObj.getSignatureCardKey(), signCardDataObj.getSignatureCardSlot(), signCardDataObj.getSignatureCardPassword(), new FileInputStream(folderPath + data.getDocumentName().replace(':', ' ')));
                s3Mgr.uploadContent(bucketName, bucketFolderPath + "/" + data.getDocumentName(), new ByteArrayInputStream(baos.toByteArray()));
                dataTab.setFlagDocumentSigned(true);
                dataTab.setDocumentSignedDate(new Timestamp(System.currentTimeMillis()));
            }
            if(!data.getFlagPrintingRequired()) {
                new File(folderPath + data.getDocumentName().replace(':', ' ')).delete();
            }
            inpStream.close();
            return "Success";
        } catch (Exception e) {
            e.printStackTrace();
            SignatureFailureException except = new SignatureFailureException(e, signCardDataObj.getSignatureCardName()+", "+data.getDocumentType());
            mgr.send(systemConfigRepository.findByConfigKey("SENDGRID_SMTP_FROM_EMAIL_ADDRESS").getConfigValue(), systemConfigRepository.findByConfigKey("SENDGRID_SMTP_TO_EMAIL_ADDRESS").getConfigValue(),"Document Signing Failed", except.getMessageInHtmlFormat());
        } finally {
            try {
                documentDataRepository.saveAndFlush(dataTab);
            }catch(Exception e){
                e.printStackTrace();
                SignatureFailureException except = new SignatureFailureException(e, signCardDataObj.getSignatureCardName()+", "+data.getDocumentType());
                mgr.send(systemConfigRepository.findByConfigKey("SENDGRID_SMTP_FROM_EMAIL_ADDRESS").getConfigValue(), systemConfigRepository.findByConfigKey("SENDGRID_SMTP_TO_EMAIL_ADDRESS").getConfigValue(),"Document Signing Failed", except.getMessageInHtmlFormat());
            }
        }
        return "Failed";
    }
*/

    @GetMapping("/monitor")
    public String monitor() {
        String dbString="";
        try {
            Integer intVal = systemConfigRepository.extractByNativeQuery();
            dbString = "Database:OK";
        }catch(Exception e){

        }
        return "Application: OK; "+dbString;
    }
}
