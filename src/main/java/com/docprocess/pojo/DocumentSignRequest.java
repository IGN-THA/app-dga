package com.docprocess.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class DocumentSignRequest {
    @JsonProperty("policy_number")
    private final String policyNumber;
    @JsonProperty("specify_year")
    private final String specifyYear;
    @JsonProperty("policy_type")
    private final String policyType;
    @JsonProperty("plan_id")
    private final String planId;
    @JsonProperty("files")
    private final byte[] files;
    @JsonProperty("application")
    private final String application;
}
