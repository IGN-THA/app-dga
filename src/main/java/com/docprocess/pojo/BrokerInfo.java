package com.docprocess.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class BrokerInfo {
    @JsonProperty("intermediaryAccountID")
    private String intermediaryAccountID;

    @JsonProperty("intermediaryReptID")
    private String intermediaryReptID;

    @JsonProperty("intermediaryName")
    private String intermediaryName;

    @JsonProperty("intermediaryPhoneNumber")
    private String intermediaryPhoneNumber;

    @JsonProperty("accountSource")
    private String accountSource;

    @JsonProperty("createCallFile")
    private String createCallFile;

    @JsonProperty("imageUrl")
    private String imageUrl;
}
