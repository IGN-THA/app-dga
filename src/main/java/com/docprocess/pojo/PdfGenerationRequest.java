package com.docprocess.pojo;


import lombok.*;

import java.io.Serializable;
import java.util.LinkedHashMap;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class PdfGenerationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String locale;
    private LinkedHashMap<String, Object> context;
    private String templateType;
    private String callBackUrl="roojai.com";
    private String source;
    private String emailTemplateName;
    private String quoteId;
}
