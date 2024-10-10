package com.docprocess.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
@AllArgsConstructor
public enum NoClaimBonus {
    TEN_PERCENT("10", "no.claim.bonus.10Percent"),
    TWENTY_PERCENT("20", "no.claim.bonus.20Percent"),
    THIRTY_PERCENT("30", "no.claim.bonus.30Percent"),
    FORTY_PERCENT("40", "no.claim.bonus.40Percent"),
    FIVE("50", "no.claim.bonus.50Percent"),
    NOT_KNOW("N", "no.claim.bonus.notKnow");

    private final String value;
    private final String localizationKey;

    @JsonCreator
    public static NoClaimBonus fromValue(String value) {
        for (NoClaimBonus it : values()) {
            if (it.getValue().equalsIgnoreCase(value.trim())) {
                return it;
            }
        }
        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, NoClaimBonus.class));
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}

