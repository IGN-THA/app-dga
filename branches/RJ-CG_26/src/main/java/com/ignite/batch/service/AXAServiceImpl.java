package com.ignite.batch.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import sun.misc.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AXAServiceImpl implements AXAService {
    private static final String CACHE_TOKEN_EXPIRE_AT = "tokenExpireAt";
    private static final String CACHE_ACCESS_TOKEN = "accessToken";
    private static final String CACHE_ACCESS_TOKEN_TYPE = "accessTokenType";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final static String GRANT_TYPE = "client_credentials";
    private final static String SCOPE = "IFile-Gateway";
    private final static String POLICY_TYPE = "Individual";

    private final String clientId;
    private final String clientSecret;
    private final String tokenEndpoint;
    private final String documentEndpoint;
    private final String applicationName;

    private final Map<String, Object> cache = new HashMap<>();

    public AXAServiceImpl(
            @Value("${axa.authorization.clientSecret}") String clientSecret,
            @Value("${axa.authorization.clientId}") String clientId,
            @Value("${axa.authorization.endpoint}") String tokenEndpoint,
            @Value("${axa.documentSigning.endpoint}") String documentEndpoint,
            @Value("${axa.documentSigning.applicationName}") String applicationName) {
        this.tokenEndpoint = tokenEndpoint;
        this.documentEndpoint = documentEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.applicationName = applicationName;
    }

    @Override
    public AXADocumentSignResponse signDocumentByFilePath(String path, String planId, String policyNumber, Integer specifyYear) throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get(path))) {
            // Check if cached token had expired or not and refresh for new access token
            String accessToken = getCacheAccessToken()
                    .orElseGet(() -> {
                        AXATokenResponse axaTokenResponse = authenticate(clientId, clientSecret);
                        return axaTokenResponse.getAccessToken();
                    });;
            String authorizationHeader = MessageFormat.format("{0} {1}", cache.get(CACHE_ACCESS_TOKEN_TYPE), accessToken);

           // byte[] fileContent = IOUtils.readAllBytes(is);
            AXADocumentSignRequest axaDocumentSignRequest = AXADocumentSignRequest.builder()
                    .planId(planId)
                    .application(applicationName)
                    .policyNumber(policyNumber)
                    .specifyYear(specifyYear)
                    .policyType(POLICY_TYPE)
               //     .files(fileContent)
                    .build();

            RestTemplate restTemplate = new RestTemplateBuilder().build();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set(AUTHORIZATION_HEADER, authorizationHeader);
            HttpEntity<AXADocumentSignRequest> requestEntity = new HttpEntity<>(axaDocumentSignRequest, headers);

            try {
                return restTemplate.postForObject(documentEndpoint, requestEntity, AXADocumentSignResponse.class);
            }
            catch (HttpClientErrorException ex) {
                // If access token is expired, retry 1 more time with new access token
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    accessToken = authenticate(clientId, clientSecret).getAccessToken();
                    authorizationHeader = MessageFormat.format("{0} {1}", cache.get(CACHE_ACCESS_TOKEN_TYPE), accessToken);
                    headers.set(AUTHORIZATION_HEADER, authorizationHeader);
                    requestEntity = new HttpEntity<>(axaDocumentSignRequest, headers);
                    return restTemplate.postForObject(documentEndpoint, requestEntity, AXADocumentSignResponse.class);
                }
                throw ex;
            }
        }
    }

    @Override
    public void persistSignedDocument(String targetPath, AXADocumentSignResponse response) throws IOException {
        try (OutputStream os = Files.newOutputStream(Paths.get(targetPath))) {
            os.write(response.getFile());
        }
    }

    private Optional<String> getCacheAccessToken() {
        Instant tokenExpireAt = (Instant) cache.get(CACHE_TOKEN_EXPIRE_AT);
        if (tokenExpireAt == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(tokenExpireAt)) {
            cache.remove(CACHE_ACCESS_TOKEN);
            cache.remove(CACHE_TOKEN_EXPIRE_AT);
            cache.remove(CACHE_ACCESS_TOKEN_TYPE);
            return Optional.empty();
        }
        return Optional.ofNullable((String) cache.get(CACHE_ACCESS_TOKEN));
    }

    private AXATokenResponse authenticate(String clientId, String clientSecret) {
        AXATokenRequest axaTokenRequest = AXATokenRequest.builder()
                .scope(SCOPE)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(GRANT_TYPE)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<AXATokenRequest> requestEntity = new HttpEntity<>(axaTokenRequest, headers);

        Instant requestTokenAt = Instant.now();
        return Optional.ofNullable(new RestTemplateBuilder().build().postForObject(tokenEndpoint, requestEntity, AXATokenResponse.class))
                .map(it -> {
                    Instant expiresAt = requestTokenAt.plus(Duration.ofMinutes(it.getExpiresInMin()));
                    cache.put(CACHE_ACCESS_TOKEN, it.getAccessToken());
                    cache.put(CACHE_TOKEN_EXPIRE_AT, expiresAt);
                    cache.put(CACHE_ACCESS_TOKEN_TYPE, it.getTokenType());
                    return it;
                }).orElse(null);
    }
}