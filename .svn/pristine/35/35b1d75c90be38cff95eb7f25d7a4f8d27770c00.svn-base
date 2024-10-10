package com.docprocess.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
@AllArgsConstructor
public enum GarageOption {
    PREFERRED_GARAGES("PanelWorkshop", "garage.option.preferred"),
    DEALER_GARAGES("", "garage.option.dealer"),
    ANY_GARAGES("AnyWorkshop", "garage.option.any");

    private final String value;
    private final String localizationKey;

    @JsonCreator
    public static GarageOption fromValue(String value) {
        for (GarageOption it : values()) {
            if (it.getValue().equalsIgnoreCase(value.trim())) {
                return it;
            }
        }
        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, GarageOption.class));
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
