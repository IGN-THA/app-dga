package com.docprocess.manager;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.*;
import io.reactivex.rxjava3.core.Single;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Service
public class S3BucketManager {
    @Autowired
    AmazonS3 s3Client;


    public S3BucketManager() {

    }

    Logger logger = LogManager.getLogger(S3BucketManager.class);

    public S3BucketManager(String accessKey, String secretKey, String s3Region){
        AWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(s3Region)
                .build();
    }

    public S3Object getContent(String bucketName, String filePath) {
        return s3Client.getObject(bucketName, filePath);
    }

    public void uploadContent(String bucketName, String filePath, InputStream inputStream, Long length) throws IOException {
        try (InputStream is = inputStream) {
            ObjectMetadata meta = new ObjectMetadata();
            if (length != null && length > 0)
                meta.setContentLength(length);
            meta.setContentType("application/pdf");
            s3Client.putObject(new PutObjectRequest(bucketName, filePath, is, meta));
        }
    }

    public void uploadContent(String bucketName, String filePath, byte[] data, Long length) throws IOException {
        uploadContent(bucketName, filePath, new ByteArrayInputStream(data), length);
    }

    public void uploadContent(String bucketName, String filePath, File file) throws IOException {
        uploadContent(bucketName, filePath, Files.newInputStream(file.toPath()), file.length());
    }

    public Single<Boolean> uploadDirectory(String bucketName, String s3Path, File folder) {
        TransferManager transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
        return Single.just(folder)
                .filter(it -> it.exists() && it.isDirectory())
                .toSingle()
                .doOnSuccess(it -> transferManager.uploadDirectory(bucketName, s3Path, folder, true).waitForCompletion())
                .map(__ -> true);
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

        logger.info("bucketName " + bucketName);
        logger.info("folderPath " + folderPath);
        logger.info("folderPathOnServer" + folderPathOnServer);

        TransferManager xfer_mgr = TransferManagerBuilder.standard().withS3Client(s3Client).build();

        MultipleFileDownload xfer = xfer_mgr.downloadDirectory(
                bucketName, folderPath, f);

    }
}
