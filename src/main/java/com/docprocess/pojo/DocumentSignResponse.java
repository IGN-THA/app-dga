package com.docprocess.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DocumentSignResponse {
    @JsonProperty("RowID")
    private String rowId;
    @JsonProperty("Key")
    private String key;
    @JsonProperty("File")
    private byte[] file;
    @JsonProperty("Extension")
    private String extension;
    @JsonProperty("SizeBytes")
    private Long sizeByte;
}
