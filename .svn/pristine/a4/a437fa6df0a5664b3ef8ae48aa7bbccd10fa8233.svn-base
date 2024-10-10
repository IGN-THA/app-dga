package com.docprocess.constant;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum DriverPlan {
    NAMED_DRIVERS(Arrays.asList("Named","Named Driver"), "driver.plan.named"),
    ANY_DRIVER_OVER_30(Arrays.asList("AnyOver30","Driver > 30"), "driver.plan.anyOver30"),
    ANY_DRIVER_OVER_25(Arrays.asList("AnyOver25","Driver > 25"), "driver.plan.anyOver25"),
    ANY_DRIVER(Arrays.asList("AnyDriver","Any Driver"), "driver.plan.anyDriver");

    private final List<String> value;
    private final String localizationKey;

    @JsonCreator
    public static DriverPlan fromValue(String value) {
        for (DriverPlan it : values()) {
            if (it.getValue().contains(value.trim())) {
                return it;
            }
        }

        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, DriverPlan.class));
    }

    @JsonValue
    public List<String> toValue() {
        return value;
    }
}
