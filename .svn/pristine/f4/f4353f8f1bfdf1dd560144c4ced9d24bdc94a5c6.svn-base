package com.docprocess.job;

import com.docprocess.config.ConfigConstant;
import com.docprocess.manager.CacheManager;
import com.docprocess.manager.DigiSignDocManager;
import com.docprocess.manager.HttpHandler;
import com.docprocess.manager.S3BucketManager;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.model.DocumentTypeData;
import com.docprocess.model.SignatureCardData;
import com.docprocess.repository.DocumentDataRepository;
import com.docprocess.repository.DocumentTypeDataRepository;
import com.docprocess.repository.SignatureCardDataRepository;
import com.docprocess.repository.SystemConfigRepository;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.io.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long batchStart = System.currentTimeMillis();
        System.out.println("Manage Doc Batch started "+ batchStart);

        String rootFilePath = systemConfigRepository.findByConfigKey(ConfigConstant.ROOT_FOLDER_TO_STORE_LOCAL_FILE).getConfigValue();
        String pdfFilePath = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_STORAGE_PATH_FOR_PRINTING).getConfigValue();
        String tempFilePath = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_STORAGE_PATH_FOR_PRINTING).getConfigValue()+"\\Temp";
        File f = new File(tempFilePath);
        if(!f.exists()) f.mkdirs();
        String accessKey = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_ACCESS_KEY).getConfigValue();
        String secretKey = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_SECRET_KEY).getConfigValue();
        String bucketName = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_DOCUMENT_BUCKET_NAME).getConfigValue();
        String homeBucketName = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_HOME_BUCKET_NAME).getConfigValue();
        String pdfPasswordOwner = systemConfigRepository.findByConfigKey(ConfigConstant.PDF_PASSWORD_OWNER).getConfigValue();
        //String renderedFilePath = systemConfigRepository.findByConfigKey(ConfigConstant.RENDERED_FILE_PATH).getConfigValue();


        S3BucketManager s3Mgr = new S3BucketManager(accessKey, secretKey);

        File rootFolder = new File(pdfFilePath);
        File renderedPathForCoversion = null;
        File failedPathForConversion = null;
        List<File> deleteFileList = new ArrayList<File>();
        for(File fileObj : rootFolder.listFiles()){
            if(fileObj.isDirectory() && !fileObj.getName().startsWith("Failed") && !fileObj.getName().startsWith("Temp") && !fileObj.getName().startsWith("InProgress")) {
                //String rootPDFFileFolder = fileObj.getName();
                //Integer fileSize = Integer.parseInt(rootPDFFileFolder.substring(rootPDFFileFolder.lastIndexOf("_")+1));
                if (fileObj.listFiles().length != 0) {
                        renderedPathForCoversion = fileObj;
                }

            }/*else{
                if(fileObj.getName().startsWith("Failed")){
                    failedPathForConversion = fileObj;
                }
            }*/
        }
//        if(renderedPathForCoversion==null && failedPathForConversion!=null) {
//            File newFile = new File(failedPathForConversion.getAbsolutePath().replace("Failed_",""));
//            failedPathForConversion.renameTo(newFile);
//            renderedPathForCoversion = newFile;
//        }
//
//        for(File fileObj : rootFolder.listFiles()) {
//            if (fileObj.isDirectory() && fileObj.getName().startsWith("Failed")) {
//                for (File failPath : fileObj.listFiles()) {
//                    failPath.renameTo(new File(renderedPathForCoversion, failPath.getName()));
//                }
//                if (fileObj.listFiles().length == 0) {
//                    deleteFileList.add(fileObj);
//                }
//            }
//        }


//
//        for(File fObj : deleteFileList){
//            fObj.delete();
//        }

        if(renderedPathForCoversion==null || renderedPathForCoversion.listFiles().length==0) return;


        DigiSignDocManager docManager = null;
        try {
            docManager = new DigiSignDocManager(pdfPasswordOwner);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (File fileObj : renderedPathForCoversion.listFiles()) {
            Boolean techError=false;
            String fileName = fileObj.getName();
            if(!fileName.endsWith(".pdf")) continue;
            System.out.println("File Name Handling Now "+fileName);
            Timestamp currentTime = Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
            String recordId = fileName.substring(0, fileName.indexOf("_"));
            DocumentGenerateQueueData docDataObj = documentDataRepository.findBySfid(recordId);
            if(docDataObj.getPdfGeneratedDate()==null)
                docDataObj.setPdfGeneratedDate(currentTime);

            DocumentTypeData documentTypeData = documentTypeDataRepository.findByDocumentType(docDataObj.getDocumentType());

            List<SignatureCardData> signatureCardDataList= signatureCardDataRepository.findBySignOwnerAndFlagActive(documentTypeData.getSignOwner(), true);
            SignatureCardData signCardDataObj = null;
            if(!signatureCardDataList.isEmpty())
                signCardDataObj = signatureCardDataList.get(0);

            String fileUploadName = docDataObj.getReferenceNumber() + "/" + docDataObj.getDocumentName() + ".pdf";
            String s3UploadName = fileUploadName;



            String validateFolder = "";
            if (documentTypeData.getForValidation()) {
                validateFolder = "validate/";
            }
            InputStream pdfInputStream = null;
            //File passwordProtectFile = fileObj;

            try {
                //Boolean uploadWithouSign = true;
                Boolean flagEmailAttachmentReady = false;
                /*
                if(uploadWithouSign && docDataObj.getCloudUploadDate()==null){
                    inputPassProtect =  new FileInputStream(passwordProtectFile);
                    s3Mgr.uploadContent(bucketName, s3UploadName, inputPassProtect, passwordProtectFile.length());
                    inputPassProtect.close();
                    docDataObj.setCloudUploadDate(currentTime);
                }
*/
                //Boolean flagPasswordProtect = false;
                if(docDataObj.getFlagPasswordProtect() && docDataObj.getPdfPassword()!=null && docDataObj.getPdfPassword().length()>0 && !docDataObj.getFlagEmailAttachmentReady()) {
                    FileInputStream fis = null;
                    File signedPwdProtectFile = null;
                    pdfInputStream = new FileInputStream(fileObj);
                    try {
                        //docManager.passwordProtectDocument(inputPassProtect, docDataObj.getPdfPassword().getBytes(), pdfPasswordOwner.getBytes(), tempFilePath + "\\" + fileName);
                        //File fObj = new File(tempFilePath +"\\"+ fileName);
                        //FileInputStream fis = new FileInputStream(fObj);
                        if(signCardDataObj!=null && docDataObj.getFlagRequireSign())
                            docManager.signDocument(signCardDataObj.getSignatureCardKey(), signCardDataObj.getSignatureCardSlot(), signCardDataObj.getSignatureCardPassword(), pdfInputStream, tempFilePath + "\\signedPwd_" + fileName, docDataObj.getPdfPassword());
                        else
                            docManager.passwordProtectDocument(pdfInputStream, docDataObj.getPdfPassword().getBytes(), pdfPasswordOwner.getBytes(), tempFilePath + "\\signedPwd_" + fileName);


                        signedPwdProtectFile = new File(tempFilePath + "\\signedPwd_" + fileName);
                        fis = new FileInputStream(signedPwdProtectFile);

                        s3UploadName = validateFolder+"passwordEncrypted/" + fileUploadName;
                        s3Mgr.uploadContent(bucketName, s3UploadName, fis, signedPwdProtectFile.length());
                        //flagPasswordProtect = true;
                        flagEmailAttachmentReady = true;
                        docDataObj.setFlagEmailAttachmentReady(flagEmailAttachmentReady);
                    }catch(Exception e){
                        System.err.println("Failed "+ fileName);
                        e.printStackTrace();
                        techError = true;
                    }finally {
                        pdfInputStream.close();
                        if(fis!=null) fis.close();
                        if(signedPwdProtectFile!=null && signedPwdProtectFile.exists()) signedPwdProtectFile.delete();
                    }
                }

                //docDataObj.setFlagRequireSign(false);
                if(docDataObj.getFlagRequireSign()) {
                    File signedFile = null;
                    FileInputStream fis = null;
                    pdfInputStream = new FileInputStream(fileObj);
                    try {
                        if (signCardDataObj != null && !docDataObj.getFlagDocumentSigned()) {
                            try {
                                docManager.signDocument(signCardDataObj.getSignatureCardKey(), signCardDataObj.getSignatureCardSlot(), signCardDataObj.getSignatureCardPassword(), pdfInputStream, tempFilePath + "\\Signed_" + fileName, null);
                                signedFile = new File(tempFilePath + "\\Signed_" + fileName);
                                fis = new FileInputStream(signedFile);
                                s3UploadName = validateFolder + fileUploadName;
                                s3Mgr.uploadContent(bucketName, s3UploadName, fis, signedFile.length());

                                docDataObj.setFlagDocumentSigned(true);
                                docDataObj.setDocumentSignedDate(currentTime);
                                    docDataObj.setCloudUploadDate(currentTime);
                            } catch (Exception e) {
                                System.err.println("Failed Signing"+ fileName);
                                techError = true;
                                e.printStackTrace();
                            }
                        }else{
                            techError=(techError)?techError:(!docDataObj.getFlagDocumentSigned());
                        }
                        if (docDataObj.getCloudUploadDate() == null) {
                            System.err.println("Uploading without Signing"+ fileName);
                            s3Mgr.uploadContent(bucketName, s3UploadName, pdfInputStream, fileObj.length());
                            docDataObj.setCloudUploadDate(currentTime);
                        }
                    }catch(Exception e){
                        System.err.println("Failed Signing "+ fileName);
                        e.printStackTrace();
                        techError = true;
                    }finally {
                        pdfInputStream.close();
                        if(fis!=null) fis.close();
                        if(signedFile!=null && signedFile.exists()) signedFile.delete();
                    }
                }else{

                    if (docDataObj.getCloudUploadDate() == null) {
                        pdfInputStream = new FileInputStream(fileObj);
                        try {
                            s3UploadName = validateFolder + fileUploadName;
                            s3Mgr.uploadContent(bucketName, s3UploadName, pdfInputStream, fileObj.length());
                            docDataObj.setCloudUploadDate(currentTime);
                        }catch(Exception e){
                            System.err.println("Failed "+ fileName);
                            e.printStackTrace();
                            techError = true;
                        }
                        pdfInputStream.close();
                    }
                }

                if(!docDataObj.getFlagEmailAttachmentReady() && !docDataObj.getFlagPasswordProtect()){
                    flagEmailAttachmentReady = ((docDataObj.getFlagRequireSign() && docDataObj.getFlagDocumentSigned()) ||
                            (!docDataObj.getFlagRequireSign() && docDataObj.getCloudUploadDate()!=null));
                    docDataObj.setFlagEmailAttachmentReady(flagEmailAttachmentReady);
                }



                if (!documentTypeData.getForValidation() && docDataObj.getFlagPrintingRequired() && docDataObj.getDocPrintingDate() == null) {
                    FileInputStream printFolder =  new FileInputStream(fileObj);
                    s3UploadName = validateFolder+"Printing/" + docDataObj.getDocumentName() + ".pdf";
                    //System.out.println("Printing "+s3UploadName);
                    s3Mgr.uploadContent(homeBucketName, s3UploadName, printFolder, fileObj.length());
                    docDataObj.setDocPrintingDate(currentTime);
                    printFolder.close();
                }

                //todo: Only for UAT
                //if(signCardDataObj==null && docDataObj.getCloudUploadDate()!=null){
                //    techError=false;
                //}

            } catch (IOException e) {
                System.err.println("Failed "+ fileName);
                e.printStackTrace();
                techError=true;
        } catch (Exception e) {
                System.err.println("Failed "+ fileName);
                e.printStackTrace();
                techError=true;
            }finally {

            }

            if(!techError) {
                fileObj.delete();
            }

            documentDataRepository.saveAndFlush(docDataObj);

        }


        System.out.println("Manage Doc Batch End "+ System.currentTimeMillis() + " within " +(System.currentTimeMillis()-batchStart));
    }

}
