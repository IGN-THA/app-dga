package com.docprocess.manager;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;


import java.io.*;
import java.util.List;

public class S3BucketManager {
    AmazonS3 s3Client;

    public S3BucketManager() {

    }

    public S3BucketManager(String accessKey, String secretKey){
        AWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.AP_NORTHEAST_1)
                .build();
    }

    public InputStream getContent(String bucketName, String filePath){
        S3Object fileObj = s3Client.getObject(bucketName, filePath);
        InputStream stream = fileObj.getObjectContent();
        return stream;
    }

    public void uploadContent(String bucketName, String filePath, InputStream inputStream, Long length) {
        ObjectMetadata meta = new ObjectMetadata();
        if(length!=null && length>0)
            meta.setContentLength(length);
        meta.setContentType("application/pdf");
        s3Client.putObject(new PutObjectRequest(bucketName, filePath, inputStream, meta));
    }

//    public void pdfProtectWithPassword(ByteArrayInputStream inputStream) {
//        Document document = new Document();
//        PdfWriter.getInstance(document, new FileOutputStream(inputStream));
//        pdfWriter.setEncryption(userPassword.getBytes(),
//                ownerPassword.getBytes(),
//                PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128);
//    }
/*
    public void uploadContent(String bucketName, String filePath, FileInputStream inputStream, Long length) {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(length);
        meta.setContentType("application/pdf");
        s3Client.putObject(new PutObjectRequest(bucketName, filePath, inputStream, meta));
    }
*/
    public void synchronizeDocumentToServer(String bucketName, String folderPath, String folderPathOnServer, Boolean clearAndSyncFile){

        File f = new File(folderPathOnServer);
        if(!f.exists())
            f.mkdir();

        if(clearAndSyncFile) {
            for (File fObj : f.listFiles()) {
                fObj.delete();
            }
        }

        TransferManager xfer_mgr = TransferManagerBuilder.standard().withS3Client(s3Client).build();

        MultipleFileDownload xfer = xfer_mgr.downloadDirectory(
                bucketName, folderPath, f);

    }

}
