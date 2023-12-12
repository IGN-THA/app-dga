package com.docprocess.controller;

import com.docprocess.config.ConfigConstant;
import com.docprocess.config.ErrorConfig;
import com.docprocess.constant.PdfQueueProcessingStatus;
import com.docprocess.manager.CacheManager;
import com.docprocess.manager.DocumentRenderException;
import com.docprocess.manager.EmailServiceManager;
import com.docprocess.manager.S3BucketManager;
import com.docprocess.manager.docx.ExternalApiInfoManager;
import com.docprocess.manager.docx.RenderDocumentManager;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.model.DocumentTypeData;
import com.docprocess.pojo.Accessory;
import com.docprocess.pojo.PdfGenerationQueueResponse;
import com.docprocess.pojo.PdfGenerationRequest;
import com.docprocess.repository.*;
import com.docprocess.service.CloudSigningService;
import com.docprocess.service.PdfGenerationService;
import com.docprocess.service.TemplateService;
import com.docprocess.service.impl.CloudSigningServiceImpl;
import com.google.common.net.HttpHeaders;
import io.reactivex.rxjava3.core.Single;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.codehaus.jettison.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.security.PermitAll;
import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@RestController
@RequestMapping("/api/v1/doc")
@PermitAll
//@CrossOrigin("http://localhost:8181/")
@CrossOrigin("https://myaccount.uat-roojai.com/")
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

    @Autowired
    JmsTemplate jmsTemplate;

    @Autowired
    TemplateService templateService;

    @Autowired
    PdfGenerationService pdfGenerationService;

    @Autowired
    @Qualifier("awsAccessKey")
    String accessKey;

    @Autowired
    @Qualifier("awsSecretKey")
    String secretKey;

    @Autowired
    @Qualifier("s3HomeBucketName")
    String bucketName;

    @Autowired
    @Qualifier("s3TemplateFolder")
    String bucketFolderPath;

    @Autowired
    String rootFilePath;

    @Autowired
    String folderPathOnServer;

    @Autowired
    String renderedFilePath;

    @Autowired
    S3BucketManager s3Mgr;

    @Autowired
    String s3DocumentBucketName;

    @Autowired
    ExternalApiInfoRepository externalApiInfoRepository;

    Logger logger = LogManager.getLogger(DocumentController.class);

    @GetMapping("/syncDocument")
    @ResponseBody
    public String signDocumentService() {
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
        boolean rederedDoc = false;
        try {
            RenderDocumentManager mgr = new RenderDocumentManager();
            rederedDoc = mgr.renderDocFromTemplate(renderedFilePath, folderPathOnServer + "/" + bucketFolderPath, folderPathOnServer + "/" + bucketFolderPath + "/" + tempName, tableName, sessionFactory, docData, constantParamRepository, externalApiInfoRepository);
        } catch (Exception e) {
            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "generateDocument", e);
            logger.error(errorMessage);
        }
        if (rederedDoc) {
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
    public ResponseEntity<?> monitor() {
        String dbString = "";
        try {
            Integer intVal = systemConfigRepository.extractByNativeQuery();
            dbString = "Database:OK";
        } catch (Exception e) {
            return new ResponseEntity<>("Application: Fail; " + e.getMessage(), org.springframework.http.HttpStatus.valueOf(HttpStatus.SC_INTERNAL_SERVER_ERROR));
        }
        return new ResponseEntity<>("Application: OK; " + dbString, org.springframework.http.HttpStatus.valueOf(HttpStatus.SC_OK));
    }

    @PostMapping("/generate-pdf")
    public PdfGenerationQueueResponse createPdf(@RequestBody PdfGenerationRequest request) {
        logger.info("Post request from "+request.getCallBackUrl());
        return pdfGenerationService.queueRequest(request).blockingGet();
    }

    @PostMapping("/generate-pdf1")
    public PdfGenerationQueueResponse testSFPostRequest(@RequestBody PdfGenerationRequest request) {
        logger.info("Post request from Salesforce");
        return pdfGenerationService.checkStatus("123489");
    }

    @GetMapping("/check-pdf-status/{requestId}")
    public PdfGenerationQueueResponse checkPdfStatus(@PathVariable(name = "requestId") String requestId) {
        return pdfGenerationService.checkStatus(requestId);
    }

    @GetMapping("/download-pdf/{requestId}")
    public FileSystemResource downloadPdf(@PathVariable(name = "requestId") String requestId,
                                          @RequestParam(name = "locale", defaultValue = "en", required = false) String locale,
                                          HttpServletResponse response) {
        logger.info("downloadPdf: status: "+pdfGenerationService.checkStatus(requestId).getStatus());
        Supplier<File> renderedFileGetter = () -> pdfGenerationService.getRenderedTemplate(requestId, locale);
        return Single.just(renderedFileGetter)
                .filter(__ -> pdfGenerationService.checkStatus(requestId).getStatus() == PdfQueueProcessingStatus.UPLOADED)
                .map(Supplier::get)
                .doOnComplete(() -> response.setStatus(HttpStatus.SC_NOT_FOUND))
                .doOnSuccess(renderedFile -> response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + renderedFile.getName() + "\""))
                .map(FileSystemResource::new)
                .blockingGet();
    }

    //For test sign certificate from az key vault
    @GetMapping("/signCert")
    @ResponseBody
    public String CloudSigningService() throws JSONException, IOException, DocumentRenderException {
        return getCertValue();
    }
    private String getCertValue() throws IOException, JSONException, DocumentRenderException {
        String pdfPasswordOwner = systemConfigRepository.findByConfigKey(ConfigConstant.PDF_PASSWORD_OWNER).getConfigValue();
        String currentPath = System.getProperty("user.dir");
        String fileName = "Voluntary Policy Schedule 02 1000-02540386-01-000.pdf";
        String tempFilePath = "\\DocGenFile\\RenderedFilePath\\";
        FileInputStream pdfInputStream = new FileInputStream(currentPath + "\\DocGenFile\\RenderedFilePath\\"+fileName);
        CloudSigningService cloudSigningService = new CloudSigningServiceImpl("esign-roojai-insurance", pdfPasswordOwner);
        cloudSigningService.getCertValue(pdfInputStream,currentPath + tempFilePath + "\\Signed_" + fileName,null);
        return "Signed";
    }

    @PostMapping("/signCertWithFile")
    @ResponseBody
    public FileSystemResource CloudSigningService2(@RequestBody(required = true) MultipartFile file) throws IOException, JSONException, DocumentRenderException {
        String pdfPasswordOwner = systemConfigRepository.findByConfigKey(ConfigConstant.PDF_PASSWORD_OWNER).getConfigValue();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String currentPath = System.getProperty("user.dir");
        String tempFilePath = "\\DocGenFile\\RenderedFilePath\\";

        InputStream inputStream = file.getInputStream();
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);

        String fileName = timeStamp + file.getOriginalFilename();
//        File directory = new File(tempFilePath);
        File newFile = new File(currentPath+tempFilePath + fileName);
        try (OutputStream outStream = new FileOutputStream(newFile)) {
            outStream.write(buffer);
        }
        FileInputStream pdfInputStream = new FileInputStream(currentPath + "\\DocGenFile\\RenderedFilePath\\"+fileName);
        CloudSigningService cloudSigningService = new CloudSigningServiceImpl("esign-roojai-insurance", pdfPasswordOwner);
        cloudSigningService.getCertValue(pdfInputStream,currentPath + tempFilePath + "\\Signed_" + fileName,null);

        File signedFile = new File(currentPath + tempFilePath + "\\Signed_" + fileName);
        return new FileSystemResource(signedFile);
    }

}
