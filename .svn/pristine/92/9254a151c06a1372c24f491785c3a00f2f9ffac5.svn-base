package com.docprocess.config;

import com.docprocess.repository.SystemConfigRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PdfEncryptionConfig {
    private final SystemConfigRepository systemConfigRepository;

    public PdfEncryptionConfig(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @Bean
    public String pdfPasswordOwner() {
        return systemConfigRepository.findByConfigKey(ConfigConstant.PDF_PASSWORD_OWNER).getConfigValue();
    }
}
