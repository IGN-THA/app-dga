package com.docprocess.service.impl;

import com.docprocess.config.ErrorConfig;
import com.docprocess.manager.DocumentRenderException;
import com.docprocess.manager.docx.DynamicDataLoader;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.model.DocumentTypeData;
import com.docprocess.model.SignatureCardData;
import com.docprocess.pojo.DocumentSignResponse;
import com.docprocess.pojo.TokenResponse;
import com.docprocess.repository.DocumentDataRepository;
import com.docprocess.repository.DocumentTypeDataRepository;
import com.docprocess.service.ApiSigningService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ApiSigningServiceImpl implements ApiSigningService {
    private static final String PLAN_ID = "OB001";
    private static final String CACHE_TOKEN_EXPIRE_AT = "tokenExpireAt";
    private static final String CACHE_ACCESS_TOKEN = "accessToken";
    private static final String CACHE_ACCESS_TOKEN_TYPE = "accessTokenType";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final static String GRANT_TYPE = "client_credentials";
    private final static String SCOPE = "IFile-Gateway";
    private final static String POLICY_TYPE = "Individual";
    private static final String APPLICATION_NAME = "Roojai";

    private final String clientId;
    private final String clientSecret;
    private final String tokenEndpoint;
    private final String documentEndpoint;
    private final DocumentDataRepository documentDataRepository;
    private final DocumentTypeDataRepository documentTypeDataRepository;

    private final Map<String, Object> cache = new HashMap<>();

    HashMap<String, Object> variables = new HashMap<String, Object>();

    Logger logger = LogManager.getLogger(ApiSigningServiceImpl.class);

    public ApiSigningServiceImpl(
            String clientSecret,
            String clientId,
            String tokenEndpoint,
            String documentEndpoint,
            DocumentDataRepository documentDataRepository,
            DocumentTypeDataRepository documentTypeDataRepository) {
        this.tokenEndpoint = tokenEndpoint;
        this.documentEndpoint = documentEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.documentDataRepository = documentDataRepository;
        this.documentTypeDataRepository = documentTypeDataRepository;
    }

    @Override
    public byte[] signDocument(byte[] input, String documentSfid, EntityManagerFactory sessionFactory, SignatureCardData signatureCardData) throws IOException, DocumentRenderException, JSONException {
        // Get signing info from table fmsapp.document_data
        DocumentGenerateQueueData docData = documentDataRepository.findBySfid(documentSfid);
        // generate dynamic query
        DocumentTypeData docTypeData = documentTypeDataRepository.findByDocumentType(docData.getDocumentType());
        String tableName = null;
        if (docTypeData != null)
            tableName = docTypeData.getQueryName();
        //PAMain paMainInfo = paRepository.findById(docData.getReferenceNumber()).orElse(null);
        List qList = DynamicDataLoader.getData(tableName, sessionFactory, docData.getReferenceNumber());

//        if (paMainInfo == null || paMainInfo.getTransactedDate() == null || paMainInfo.getInsurerPolicyNo() == null) {
//            return null;
//        }
//        String insurerPolicyNo=paMainInfo.getInsurerPolicyNo();
//        insurerPolicyNo=insurerPolicyNo+"_"+docData.getId();
//        logger.info("Document Name "+docData.getDocumentName() +" Policy Number "+insurerPolicyNo);

        if (qList != null && !qList.isEmpty())
            variables = (HashMap<String, Object>) qList.get(0);
        else return null;

        // Check if cached token had expired or not if expired refresh for new access token
        String accessToken = getCacheAccessToken()
                .orElseGet(() -> {
                    TokenResponse tokenResponse = authenticate(clientId, clientSecret);
                    return tokenResponse.getAccessToken();
                });
        String authorizationHeader = MessageFormat.format("{0} {1}", cache.get(CACHE_ACCESS_TOKEN_TYPE), accessToken);

        JSONObject apiRequest = new JSONObject();
        for (Map.Entry<String, Object> entry : signatureCardData.getApiRequestInfo().entrySet()) {
            if(entry.getValue() instanceof String){
                apiRequest.put(entry.getKey(), entry.getValue());
            }else {
                Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
                if(valueMap != null){
                    if(valueMap.containsKey("value")){
                        String[] values = null;
                        if(valueMap.get("value") != null)
                            values = valueMap.get("value").toString().split("[{}]");
                        if (valueMap.get("type").toString().equals("date")) {
                            LocalDate ld = LocalDate.parse((String) variables.get(values[1]), DateTimeFormatter.ofPattern(valueMap.get("format").toString()));
                            apiRequest.put(entry.getKey(), ld);
                        } else if(valueMap.get("type").toString().equals("file")){
                            apiRequest.put(entry.getKey(), Base64.getEncoder().encodeToString(input));
                        } else {
                            apiRequest.put(entry.getKey(), variables.get(values[1]));
                        }
                    }
                }
            }

        }


        RestTemplate restTemplate = new RestTemplateBuilder()
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(AUTHORIZATION_HEADER, authorizationHeader);
        HttpEntity<String> requestEntity = new HttpEntity<>(apiRequest.toString(), headers);

        try {
            DocumentSignResponse documentSignResponse = handleResponseSignDoc(restTemplate, requestEntity);
            if(documentSignResponse != null){
                logger.info("Success sign document for Api Signing: " + documentSignResponse.getSizeByte());
                return documentSignResponse.getFile();
            }
            return null;
        } catch (HttpClientErrorException ex) {
            // If access token is expired, retry 1 more time with new access token
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                accessToken = authenticate(clientId, clientSecret).getAccessToken();
                authorizationHeader = MessageFormat.format("{0} {1}", cache.get(CACHE_ACCESS_TOKEN_TYPE), accessToken);
                headers.set(AUTHORIZATION_HEADER, authorizationHeader);
                requestEntity = new HttpEntity<>(apiRequest.toString(), headers);
                DocumentSignResponse documentSignResponse = handleResponseSignDoc(restTemplate, requestEntity);
                if(documentSignResponse != null){
                    logger.info("[Retry] Success sign document for Api Signing: " + documentSignResponse.getSizeByte());
                    return documentSignResponse.getFile();
                }
                return null;
            }

            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "signDocument", ex);
            throw new DocumentRenderException(errorMessage);
        }
    }

    private DocumentSignResponse handleResponseSignDoc(RestTemplate rest, HttpEntity<String> requestEntity) throws JsonProcessingException {
//        return Optional.ofNullable(rest.postForObject(documentEndpoint, requestEntity, String.class))
//                .map(StringEscapeUtils::unescapeJson)
//                .map(it -> it.substring(1, it.length() - 1))
//                .map(it -> {
//                    ObjectMapper objectMapper = new ObjectMapper();
//                    try {
//                        return objectMapper.readValue(it, DocumentSignResponse.class);
//                    } catch (JsonProcessingException e) {
//                        return null;
//                    }
//                });
        return rest.postForObject(documentEndpoint, requestEntity, DocumentSignResponse.class);
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

    private TokenResponse authenticate(String clientId, String clientSecret) {
        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("scope", SCOPE);
        tokenRequest.add("client_id", clientId);
        tokenRequest.add("client_secret", clientSecret);
        tokenRequest.add("grant_type", GRANT_TYPE);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(tokenRequest, headers);

        Instant requestTokenAt = Instant.now();
        return Optional.ofNullable(new RestTemplateBuilder().additionalMessageConverters(new FormHttpMessageConverter()).additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build().postForObject(tokenEndpoint, requestEntity, TokenResponse.class))
                .map(it -> {
                    Instant expiresAt = requestTokenAt.plus(Duration.ofMinutes(it.getExpiresInMin()));
                    cache.put(CACHE_ACCESS_TOKEN, it.getAccessToken());
                    cache.put(CACHE_TOKEN_EXPIRE_AT, expiresAt);
                    cache.put(CACHE_ACCESS_TOKEN_TYPE, it.getTokenType());
                    return it;
                }).orElse(null);
    }
}