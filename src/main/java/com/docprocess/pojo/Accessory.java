package com.docprocess.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Accessory {

    @JsonProperty("accessoryName")
    private String accessoryName;

    @JsonProperty("accessoryValue")
    private String accessoryValue;
}