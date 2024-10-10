package com.docprocess.manager.docx;

import com.docprocess.manager.EmailServiceManager;
import com.docprocess.repository.DocumentDataRepository;
import com.docprocess.repository.DocumentTypeDataRepository;
import com.docprocess.repository.SignatureCardDataRepository;
import com.docprocess.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DocumentSigningManager {

    @Autowired
    DocumentTypeDataRepository documentTypeDataRepository;

    @Autowired
    SignatureCardDataRepository signatureCardDataRepository;

    @Autowired
    SystemConfigRepository systemConfigRepository;

    @Autowired
    DocumentDataRepository documentDataRepository;

    @Autowired
    EmailServiceManager mgr;

    public String signDocumentService(String fileName) {
        /*SignatureCardData signCardDataObj = null;
        try {
            String signatureCardData = documentTypeDataRepository.findByDocumentType(data.getDocumentType()).getSignattureCardName();
            signCardDataObj = signatureCardDataRepository.findBySignatureCardName(signatureCardData);

            String accessKey = systemConfigRepository.findByConfigKey("AWS_ACCESS_KEY").getConfigValue();
            String secretKey = systemConfigRepository.findByConfigKey("AWS_SECRET_KEY").getConfigValue();
            String bucketName = systemConfigRepository.findByConfigKey("AWS_S3_BUCKET_NAME").getConfigValue();
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
        }*/
        return "Failed";
    }
}
