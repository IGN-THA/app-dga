package com.docprocess.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Template {
    @JsonProperty("EN")
    private LinkedHashMap<String,Object> EN;
    @JsonProperty("TH")
    private LinkedHashMap<String,Object> TH;
    @JsonProperty("ZH")
    private LinkedHashMap<String,Object> ZH;

    public LinkedHashMap<String, LinkedHashMap<String,Object>> getTemplateJson(){
        LinkedHashMap<String, LinkedHashMap<String,Object>> setTemplate = new LinkedHashMap<>();
        setTemplate.put("EN" , EN);
        setTemplate.put("TH", TH);
        setTemplate.put("ZH" , ZH);
        return setTemplate;
    }
}
