package com.docprocess.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
@AllArgsConstructor
public enum TentativeStartDate {
    TODAY("Today", "start.date.today"),
    TOMORROW("Tomorrow", "start.date.tomorrow"),
    LATER("Other", "start.date.later");

    private final String value;
    private final String localizationKey;

    @JsonCreator
    public static TentativeStartDate fromValue(String value) {
        for (TentativeStartDate it : values()) {
            if (it.getValue().equalsIgnoreCase(value.trim())) {
                return it;
            }
        }
        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, TentativeStartDate.class));
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
