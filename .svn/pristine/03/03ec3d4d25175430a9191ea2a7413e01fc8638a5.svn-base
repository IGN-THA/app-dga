package com.docprocess.service.impl;

import com.amazonaws.services.s3.model.S3Object;
import com.docprocess.manager.S3BucketManager;
import com.docprocess.model.DocumentTypeData;
import com.docprocess.repository.DocumentTypeDataRepository;
import com.docprocess.service.TemplateService;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.MessageFormat;

@Service
public class TemplateServiceImpl implements TemplateService {
    private final S3BucketManager s3BucketManager;
    private final String folderPathOnServer;
    private final String bucketName;
    private final String s3TemplateFolder;

    @Autowired
    private DocumentTypeDataRepository documentTypeDataRepository;

    Logger logger = LogManager.getLogger(TemplateServiceImpl.class);

    public TemplateServiceImpl(S3BucketManager s3BucketManager, String folderPathOnServer, @Qualifier("s3HomeBucketName") String bucketName, String s3TemplateFolder) {
        this.s3BucketManager = s3BucketManager;
        this.folderPathOnServer = folderPathOnServer;
        this.bucketName = bucketName;
        this.s3TemplateFolder = s3TemplateFolder;
    }

    @Override
    public Single<File> getTemplate(String templateType) {
        return Single.just(getTemplatePath(templateType))
                .observeOn(Schedulers.io())
                .map(File::new)
                .flatMap(localTemplate -> {
                    if (localTemplate.exists() && !localTemplate.isFile()) {
                        return Single.error(new IOException(MessageFormat.format("Cannot sync template {0} to {1} because there's already a directory with the same name", templateType, localTemplate.getAbsolutePath())));
                    }
                    if (!localTemplate.exists()) {
                        return syncS3Template(localTemplate, templateType);
                    }
                    return Single.just(localTemplate);
                });
    }
    @Override
    public String getMessageType(String messageType) {
        return MessageFormat.format("{0}/{1}/translation/{2}.json", folderPathOnServer, s3TemplateFolder, messageType);
    }

    private String getTemplatePath(String templateType) {
        logger.info("[CALL]getTemplatePath with: "+ templateType);
        DocumentTypeData documentType = documentTypeDataRepository.findByDocumentType(templateType);
        logger.info("documentTypeData: "+documentType);
        logger.info("DocumentType: "+documentType.getDocumentType());
        return MessageFormat.format("{0}/{1}/{2}.html", folderPathOnServer, s3TemplateFolder, documentType.getDocumentType());
    }

    private String getS3TemplatePath(String templateType) {
        return getTemplatePath(templateType);
    }

    private String getTemplatePathInProgress(String templateType) {
        DocumentTypeData documentType = documentTypeDataRepository.findByDocumentType(templateType);
        return MessageFormat.format("{0}/{1}_InProgress/{2}.html", folderPathOnServer, s3TemplateFolder, documentType.getDocumentType());
    }

    // Sync the template from s3 to local
    private Single<File> syncS3Template(File localTemplate, String templateType) {
        String s3TemplatePath = getS3TemplatePath(templateType);
        return Single.just(s3BucketManager.getContent(bucketName, s3TemplatePath))
                .observeOn(Schedulers.io())
                .map(S3Object::getObjectContent)
                .flatMap(s3ObjectInputStream -> {
                    File tempTemplate = new File(getTemplatePathInProgress(templateType));
                    try (InputStream is = s3ObjectInputStream) {
                        Files.copy(is, tempTemplate.toPath());
                        FileUtils.moveFile(tempTemplate, localTemplate);
                        return Single.just(localTemplate);
                    } finally {
                        if (tempTemplate.exists()) {
                            tempTemplate.delete();
                        }
                    }
                });
    }
}
