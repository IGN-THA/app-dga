package com.docprocess.job;

import com.docprocess.config.ConfigConstant;
import com.docprocess.config.ErrorConfig;
import com.docprocess.constant.SignatureCardDataType;
import com.docprocess.factory.ApiSigningServiceFactory;
import com.docprocess.manager.DigiSignDocManager;
import com.docprocess.manager.S3BucketManager;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.model.DocumentTypeData;
import com.docprocess.model.SignatureCardData;
import com.docprocess.repository.DocumentDataRepository;
import com.docprocess.repository.DocumentTypeDataRepository;
import com.docprocess.repository.SignatureCardDataRepository;
import com.docprocess.repository.SystemConfigRepository;
import com.docprocess.service.ApiSigningService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.util.IOUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


@DisallowConcurrentExecution
public class ManageDocumentGeneratedJob extends QuartzJobBean {
    @Autowired
    SystemConfigRepository systemConfigRepository;

    @Autowired
    DocumentDataRepository documentDataRepository;

    @Autowired
    DocumentTypeDataRepository documentTypeDataRepository;

    @Autowired
    SignatureCardDataRepository signatureCardDataRepository;

    @Autowired
    ApiSigningServiceFactory apiSigningServiceFactory;

    @Autowired
    private EntityManagerFactory sessionFactory;

    Logger logger = LogManager.getLogger(ManageDocumentGeneratedJob.class);

    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) throws JobExecutionException {
        long batchStart = System.currentTimeMillis();
        logger.info("Manage Doc Batch started " + batchStart);

        String rootFilePath = systemConfigRepository.findByConfigKey(ConfigConstant.ROOT_FOLDER_TO_STORE_LOCAL_FILE).getConfigValue();
        String pdfFilePath = rootFilePath + "\\" + systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_STORAGE_PATH_FOR_PRINTING).getConfigValue();
        String tempFilePath = rootFilePath + "\\" + systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_STORAGE_PATH_FOR_PRINTING).getConfigValue() + "\\Temp";
        File f = new File(tempFilePath);
        if (!f.exists()) f.mkdirs();
        String accessKey = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_ACCESS_KEY).getConfigValue();
        String secretKey = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_SECRET_KEY).getConfigValue();
        String bucketName = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_DOCUMENT_BUCKET_NAME).getConfigValue();
        String homeBucketName = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_HOME_BUCKET_NAME).getConfigValue();
        String pdfPasswordOwner = systemConfigRepository.findByConfigKey(ConfigConstant.PDF_PASSWORD_OWNER).getConfigValue();

        String s3Region = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_REGION).getConfigValue();
        S3BucketManager s3Mgr = new S3BucketManager(accessKey, secretKey, s3Region);


        Optional.ofNullable(new File(pdfFilePath).listFiles()).ifPresent(rootFolderListFile -> {
            try {
                DigiSignDocManager docManager = new DigiSignDocManager(pdfPasswordOwner);

                Stream.of(rootFolderListFile)
                        .filter(fileObj -> fileObj.isDirectory() && fileObj.getName().matches("[0-9]+") && !fileObj.getName().startsWith("Failed") && !fileObj.getName().startsWith("Temp") && !fileObj.getName().startsWith("InProgress"))
                        .map(File::listFiles)
                        .filter(fileObjList -> fileObjList != null && fileObjList.length > 0)
                        .flatMap(Arrays::stream)
                        .filter(fileObj -> fileObj.isFile() && fileObj.getName().endsWith(".pdf"))
                        .forEach(fileObj -> handleFile(fileObj, s3Mgr, bucketName, homeBucketName, tempFilePath, pdfPasswordOwner, docManager, apiSigningServiceFactory));
            } catch (Exception e) {
                String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "executeInternal", e);
                logger.error(errorMessage);
            }
        });

        logger.info("Manage Doc Batch End " + System.currentTimeMillis() + " within " + (System.currentTimeMillis() - batchStart));
    }

    private void handleFile(File fileObj, S3BucketManager s3Mgr, String bucketName, String homeBucketName, String tempFilePath, String pdfPasswordOwner, DigiSignDocManager docManager, ApiSigningServiceFactory apiSigningServiceFactory) {
        boolean techError = false;

        String fileName = fileObj.getName();
        DocumentGenerateQueueData docDataObj = null;
        try {
            logger.info("File Name Handling Now " + fileName);
            Timestamp currentTime = Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
            String recordId = "";
            int underscoreIndex = fileName.indexOf("_");
            if (underscoreIndex != -1) {
                recordId = fileName.substring(0, underscoreIndex);
            }
            logger.info("Record Id : " + recordId);
            docDataObj = documentDataRepository.findBySfid(recordId);

            if (docDataObj != null) {

                if (docDataObj.getPdfGeneratedDate() == null) {
                    docDataObj.setPdfGeneratedDate(currentTime);
                }

                DocumentTypeData documentTypeData = documentTypeDataRepository.findByDocumentType(docDataObj.getDocumentType());

                List<SignatureCardData> signatureCardDataList = signatureCardDataRepository.findBySignOwnerAndFlagActive(documentTypeData.getSignOwner(), true);

                SignatureCardData signCardDataObj = !signatureCardDataList.isEmpty() ? signatureCardDataList.get(0) : null;

                String fileUploadName = docDataObj.getReferenceNumber() + "/" + docDataObj.getDocumentName() + ".pdf";
                String s3UploadName = fileUploadName;

                String validateFolder = documentTypeData.getForValidation() ? "validate/" : "";
                InputStream pdfInputStream = null;


                Boolean flagEmailAttachmentReady = false;
                if (docDataObj.getFlagPasswordProtect() && docDataObj.getPdfPassword() != null && docDataObj.getPdfPassword().length() > 0 && !docDataObj.getFlagEmailAttachmentReady()) {
                    FileInputStream fis = null;
                    File signedPwdProtectFile = null;
                    pdfInputStream = new FileInputStream(fileObj);
                    try {
                        //docManager.passwordProtectDocument(inputPassProtect, docDataObj.getPdfPassword().getBytes(), pdfPasswordOwner.getBytes(), tempFilePath + "\\" + fileName);
                        //File fObj = new File(tempFilePath +"\\"+ fileName);
                        //FileInputStream fis = new FileInputStream(fObj);
                        if (signCardDataObj != null && docDataObj.getFlagRequireSign() && !signCardDataObj.getFlagSkipSigningDoc())
                            docManager.signDocument(signCardDataObj.getSignatureCardKey(), signCardDataObj.getSignatureCardSlot(), signCardDataObj.getSignatureCardPassword(), pdfInputStream, tempFilePath + "\\signedPwd_" + fileName, docDataObj.getPdfPassword());
                        else
                            docManager.passwordProtectDocument(pdfInputStream, docDataObj.getPdfPassword().getBytes(), pdfPasswordOwner.getBytes(), tempFilePath + "\\signedPwd_" + fileName);


                        signedPwdProtectFile = new File(tempFilePath + "\\signedPwd_" + fileName);
                        fis = new FileInputStream(signedPwdProtectFile);

                        s3UploadName = validateFolder + "passwordEncrypted/" + fileUploadName;
                        s3Mgr.uploadContent(bucketName, s3UploadName, fis, signedPwdProtectFile.length());
                        //flagPasswordProtect = true;
                        flagEmailAttachmentReady = true;
                        docDataObj.setFlagEmailAttachmentReady(flagEmailAttachmentReady);
                    } catch (Exception e) {
                        logger.error("Failed " + fileName);
                        String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "handleFile", e);
                        logger.error(errorMessage);
                        techError = true;
                    } finally {
                        pdfInputStream.close();
                        if (fis != null) fis.close();
                        if (signedPwdProtectFile != null && signedPwdProtectFile.exists())
                            signedPwdProtectFile.delete();
                    }
                }

                //docDataObj.setFlagRequireSign(false);
                if (docDataObj.getFlagRequireSign()) {
                    File signedFile = null;
                    FileInputStream fis = null;
                    pdfInputStream = new FileInputStream(fileObj);
                    try {
                        if (signCardDataObj != null && !docDataObj.getFlagDocumentSigned()) {
                            try {
                                if (!signCardDataObj.getFlagSkipSigningDoc()) {
                                    if (signCardDataObj.getFlagSigningUsingAPI()) {
                                        SignatureCardDataType signType = SignatureCardDataType.fromValue(signCardDataObj.getSignatureCardName());
                                        ApiSigningService apiSigningService = apiSigningServiceFactory.buildService(signType);
                                        byte[] signedPdfData = apiSigningService.signDocument(IOUtils.toByteArray(pdfInputStream), recordId, sessionFactory, signCardDataObj);
                                        s3Mgr.uploadContent(bucketName, s3UploadName, signedPdfData, (long) signedPdfData.length);
                                    } else {
                                        docManager.signDocument(signCardDataObj.getSignatureCardKey(), signCardDataObj.getSignatureCardSlot(), signCardDataObj.getSignatureCardPassword(), pdfInputStream, tempFilePath + "\\Signed_" + fileName, null);
                                        signedFile = new File(tempFilePath + "\\Signed_" + fileName);
                                        fis = new FileInputStream(signedFile);
                                        s3UploadName = validateFolder + fileUploadName;
                                        s3Mgr.uploadContent(bucketName, s3UploadName, fis, signedFile.length());
                                    }
                                    docDataObj.setFlagDocumentSigned(true);
                                    docDataObj.setDocumentSignedDate(currentTime);
                                    docDataObj.setCloudUploadDate(currentTime);
                                } else {
                                    s3Mgr.uploadContent(bucketName, s3UploadName, pdfInputStream, fileObj.length());
                                    docDataObj.setFlagDocumentSigned(true);
                                    docDataObj.setDocumentSignedDate(currentTime);
                                    docDataObj.setCloudUploadDate(currentTime);
                                }
                            } catch (Exception e) {
                                logger.error("Failed Signing" + fileName);
                                String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "handleFile", e);
                                logger.error(errorMessage);
                                techError = true;
                            }
                        } else {
                            techError = (techError) ? techError : (!docDataObj.getFlagDocumentSigned());
                        }
                        if (docDataObj.getCloudUploadDate() == null) {
                            pdfInputStream = new FileInputStream(fileObj);
                            logger.error("Uploading without Signing" + fileName);
                            s3Mgr.uploadContent(bucketName, s3UploadName, pdfInputStream, fileObj.length());
                            docDataObj.setCloudUploadDate(currentTime);
                        }
                    } catch (Exception e) {
                        logger.error("Failed Signing " + fileName);
                        String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "handleFile", e);
                        logger.error(errorMessage);
                        techError = true;
                    } finally {
                        pdfInputStream.close();
                        if (fis != null) fis.close();
                        if (signedFile != null && signedFile.exists()) signedFile.delete();
                    }
                } else {

                    if (docDataObj.getCloudUploadDate() == null) {
                        pdfInputStream = new FileInputStream(fileObj);
                        try {
                            s3UploadName = validateFolder + fileUploadName;
                            s3Mgr.uploadContent(bucketName, s3UploadName, pdfInputStream, fileObj.length());
                            docDataObj.setCloudUploadDate(currentTime);
                        } catch (Exception e) {
                            logger.error("Failed " + fileName);
                            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "handleFile", e);
                            logger.error(errorMessage);
                            techError = true;
                        }
                        pdfInputStream.close();
                    }
                }

                if (!docDataObj.getFlagEmailAttachmentReady() && !docDataObj.getFlagPasswordProtect()) {
                    Boolean flagSkipSignDocument = (signCardDataObj != null && signCardDataObj.getFlagSkipSigningDoc());
                    flagEmailAttachmentReady = ((docDataObj.getFlagRequireSign() && (docDataObj.getFlagDocumentSigned() || flagSkipSignDocument)) ||
                            (!docDataObj.getFlagRequireSign() && docDataObj.getCloudUploadDate() != null));
                    docDataObj.setFlagEmailAttachmentReady(flagEmailAttachmentReady);
                }


                if (!documentTypeData.getForValidation() && docDataObj.getFlagPrintingRequired() && docDataObj.getDocPrintingDate() == null) {
                    FileInputStream printFolder = new FileInputStream(fileObj);
                    s3UploadName = validateFolder + "Printing/" + docDataObj.getDocumentName() + ".pdf";
                    //System.out.println("Printing "+s3UploadName);
                    s3Mgr.uploadContent(homeBucketName, s3UploadName, printFolder, fileObj.length());
                    docDataObj.setDocPrintingDate(currentTime);
                    printFolder.close();
                }

                if (!techError) {
                    fileObj.delete();
                }
                documentDataRepository.saveAndFlush(docDataObj);
            }

        } catch (Exception e) {
            logger.error("Failed " + fileName);
            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "handleFile", e);
            logger.error(errorMessage);
        }

    }
/*
    @Nullable
    private byte[] signDocument(SignatureCardData signCardDataObj, byte[] input, String recordId, DigiSignDocManager docManager, ApiSigningServiceFactory apiSigningServiceFactory, String tempFilePath, String fileName) {
        try {
            if (signCardDataObj.getFlagSkipSigningDoc()) {
                return input;
            }
            if (signCardDataObj.getFlagSigningUsingAPI()) {
                SignatureCardDataType signType = SignatureCardDataType.fromValue(signCardDataObj.getSignatureCardName());
                ApiSigningService apiSigningService = apiSigningServiceFactory.buildService(signType);
                return apiSigningService.signDocument(input, recordId);
            }
            return docManager.signDocument(signCardDataObj.getSignatureCardKey(), signCardDataObj.getSignatureCardSlot(), signCardDataObj.getSignatureCardPassword(), input,tempFilePath + "\\Signed_" + fileName, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    */

}
