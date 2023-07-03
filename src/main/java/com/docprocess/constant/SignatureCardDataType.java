package com.docprocess.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
@AllArgsConstructor
public enum SignatureCardDataType {
    KPI_ID_01("KPI_IT_01"),
    ROOJAI_SERVICE("Roojai Service"),
    ROOJAI_TOKEN_1("Roojai Token 1"),
    AXA("AXAPA");

    private String name;

    public static SignatureCardDataType fromValue(String name) {
        for (SignatureCardDataType value : values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse value {0} into {1}", name, SignatureCardDataType.class));
    }
}
