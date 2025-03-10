package com.docprocess.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.docprocess.repository.SystemConfigRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {
    private final SystemConfigRepository systemConfigRepository;

    public S3Config(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @Bean
    public AmazonS3 s3Client() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey(), awsSecretKey());
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(awsRegion())
                .build();
    }

    @Bean
    public String awsAccessKey() {
        return systemConfigRepository.findByConfigKey(ConfigConstant.AWS_ACCESS_KEY).getConfigValue();
    }

    @Bean
    public String awsRegion(){
        return systemConfigRepository.findByConfigKey(ConfigConstant.AWS_REGION).getConfigValue();
    }
    @Bean
    public String awsSecretKey() {
        return systemConfigRepository.findByConfigKey(ConfigConstant.AWS_SECRET_KEY).getConfigValue();
    }

    @Bean
    public String s3HomeBucketName() {
        return systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_HOME_BUCKET_NAME).getConfigValue();
    }

    @Bean
    public String s3DocumentBucketName() {
        return systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_DOCUMENT_BUCKET_NAME).getConfigValue();
    }

    @Bean
    public String s3TemplateFolder() {
        return systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_TEMPLATE_FOLDER_PATH).getConfigValue();
    }

    @Bean
    public String rootFilePath() {
        return systemConfigRepository.findByConfigKey(ConfigConstant.ROOT_FOLDER_TO_STORE_LOCAL_FILE).getConfigValue();
    }

    @Bean
    public String renderedFilePath() {
        return rootFilePath() + "/" + systemConfigRepository.findByConfigKey(ConfigConstant.RENDERED_FILE_PATH).getConfigValue();
    }

    @Bean
    public String pdfFilePath() {
        return rootFilePath() + "/" + systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_STORAGE_PATH_FOR_PRINTING).getConfigValue();
    }

    @Bean
    public String tempFilePath() {
        return rootFilePath() + "/" + systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_STORAGE_PATH_FOR_PRINTING).getConfigValue() + "/Temp";
    }

    @Bean
    public String folderPathOnServer() {
        return rootFilePath() + "/" + systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_TEMPLATE_PATH_ON_SERVER).getConfigValue();
    }
}
