package com.docprocess.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
@AllArgsConstructor
public enum DrivingExperience {
    ZERO("0"),
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    OVER_SIX("6+");

    private final String value;

    @JsonCreator
    public static DrivingExperience fromValue(String value) {
        for (DrivingExperience it : values()) {
            if (it.getValue().equalsIgnoreCase(value.trim())) {
                return it;
            }
        }
        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, DrivingExperience.class));
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
