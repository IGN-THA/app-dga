package com.docprocess.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
@AllArgsConstructor
public enum MaritalStatus {
    SINGLE("Single", "single"),
    MARRIED("Married", "married");

    private final String value;
    private final String localizationKey;

    @JsonCreator
    public static MaritalStatus fromValue(String value) {
        for (MaritalStatus it : values()) {
            if (it.getValue().equalsIgnoreCase(value.trim())) {
                return it;
            }
        }
        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, MaritalStatus.class));
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
