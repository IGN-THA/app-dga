package com.docprocess.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
@AllArgsConstructor
public enum Gender {
    MALE("Male", "male"),
    FEMALE("Female", "female");

    private final String value;
    private final String localizationKey;

    @JsonCreator
    public static Gender fromValue(String value) {
        for (Gender it : values()) {
            if (it.getValue().equalsIgnoreCase(value.trim())) {
                return it;
            }
        }
        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, Gender.class));
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
