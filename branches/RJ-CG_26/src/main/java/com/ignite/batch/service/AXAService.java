package com.ignite.batch.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.IOException;
import java.util.UUID;

public interface AXAService {
    AXADocumentSignResponse signDocumentByFilePath(String path, String planId, String policyNumber, Integer specifyYear) throws IOException;
    void persistSignedDocument(String targetPath, AXADocumentSignResponse response) throws IOException;

    @Getter
    @AllArgsConstructor
    @Builder
    final class AXADocumentSignResponse {
        @JsonProperty("row_id")
        private final UUID rowId;
        @JsonProperty("key")
        private final String key;
        @JsonProperty("file")
        private final byte[] file;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    final class AXATokenRequest {
        @JsonProperty("grant_type")
        private final String grantType;
        @JsonProperty("client_id")
        private final String clientId;
        @JsonProperty("client_secret")
        private final String clientSecret;
        @JsonProperty("scope")
        private final String scope;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    final class AXATokenResponse {
        @JsonProperty("access_token")
        private final String accessToken;
        @JsonProperty("expires_in")
        private final Integer expiresInMin;
        @JsonProperty("token_type")
        private final String tokenType;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    final class AXADocumentSignRequest {
        @JsonProperty("policy_number")
        private final String policyNumber;
        @JsonProperty("specify_year")
        private final Integer specifyYear;
        @JsonProperty("policy_type")
        private final String policyType;
        @JsonProperty("plan_id")
        private final String planId;
        @JsonProperty("files")
        private final byte[] files;
        @JsonProperty("application")
        private final String application;
    }
}