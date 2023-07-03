package com.docprocess.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "external_api_info", schema = "fmsapp")
public class ExternalApiInfo implements Serializable {

    public ExternalApiInfo() {
    }


    @Id
    @Column(name = "apikey")
    private String apiKey;

    @Column(name = "security_key")
    private String securityKey;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "http_method")
    private String httpMethod;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecurityKey() {
        return securityKey;
    }

    public void setSecurityKey(String securityKey) {
        this.securityKey = securityKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

}
