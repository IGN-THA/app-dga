package com.docprocess.factory;

import com.docprocess.constant.SignatureCardDataType;
import com.docprocess.model.SignatureCardData;
import com.docprocess.repository.DocumentDataRepository;
import com.docprocess.repository.DocumentTypeDataRepository;
import com.docprocess.repository.SignatureCardDataRepository;
import com.docprocess.service.ApiSigningService;
import com.docprocess.service.impl.ApiSigningServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Map;

@Component
public class ApiSigningServiceFactory {
    private final SignatureCardDataRepository signatureCardDataRepository;
    private final DocumentDataRepository documentDataRepository;

    private final DocumentTypeDataRepository documentTypeDataRepository;

    public ApiSigningServiceFactory(
            SignatureCardDataRepository signatureCardDataRepository,
            DocumentDataRepository documentDataRepository,
            DocumentTypeDataRepository documentTypeDataRepository) {
        this.signatureCardDataRepository = signatureCardDataRepository;
        this.documentDataRepository = documentDataRepository;
        this.documentTypeDataRepository = documentTypeDataRepository;
    }

    @Cacheable(value = "apiSigningService")
    public ApiSigningService buildService(SignatureCardDataType signatureCardDataType) {
        if (signatureCardDataType == SignatureCardDataType.AXA) {
            SignatureCardData signatureCardData = signatureCardDataRepository.findBySignatureCardName(signatureCardDataType.getName());
            Map<String, Object> apiConfigInfo = signatureCardData.getApiConfigInfo();
            String clientId = (String) apiConfigInfo.get("client_id");
            String clientSecret = (String) apiConfigInfo.get("client_secret");
            String tokenEndpoint = (String) apiConfigInfo.get("token_endpoint");
            String documentEndpoint = (String) apiConfigInfo.get("digital_sign_endpoint");
            return new ApiSigningServiceImpl(clientSecret, clientId, tokenEndpoint, documentEndpoint, documentDataRepository, documentTypeDataRepository);
        }
        throw new UnsupportedOperationException(MessageFormat.format("Not support getting api signing service for {0}", signatureCardDataType.getName()));
    }
}
