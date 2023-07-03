package com.docprocess.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
@AllArgsConstructor
public enum CarUsage {
    PERSONAL_USAGE("Social, pleasure and travelling", "vehicle.usage.personal"),
    PERSONAL_USAGE_AND_COMMUTING("Driving to and back from work", "vehicle.usage.commuting"),
    BUSINESS("To support a business", "vehicle.usage.business");

    private final String value;
    private final String localizationKey;

    @JsonCreator
    public static CarUsage fromValue(String value) {
        for (CarUsage it : values()) {
            if (it.getValue().equalsIgnoreCase(value.trim())) {
                return it;
            }
        }
        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, CarUsage.class));
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
