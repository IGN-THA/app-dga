package com.docprocess.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
@AllArgsConstructor
public enum PaymentFrequency {
    MONTHLY_NO_DEPOSIT("Monthly no deposit", "payment.frequency.monthlyNoDeposit"),
    YEARLY("Yearly", "payment.frequency.yearly");

    private final String value;
    private final String localizationKey;

    @JsonCreator
    public static PaymentFrequency fromValue(String value) {
        for (PaymentFrequency it : values()) {
            if (it.getValue().equalsIgnoreCase(value.trim())) {
                return it;
            }
        }
        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, PaymentFrequency.class));
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
