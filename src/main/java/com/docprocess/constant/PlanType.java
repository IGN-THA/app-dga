package com.docprocess.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum PlanType {
    TYPE_1( Arrays.asList("Type1","Type 1"), "plan.type.1",
            true, true, true, true, true, true, true, true),
    // TYPE_2_PLUS("Type2Plus", "plan.type.2+", true, false, false, true, true, false, true, true),
    TYPE_2_PLUS(Arrays.asList("Type2Plus","Type 2+"), "plan.type.2+",
            true, true, false, true, true, false, true, true),
    TYPE_2(Arrays.asList("Type2","Type 2"), "plan.type.2",
            false, false, false, true, true, false, false, false),

    // TYPE_3_PLUS("Type3Plus", "plan.type.3+", true, false, false, false, false, false, false, true);
    TYPE_3_PLUS(Arrays.asList("Type3Plus","Type 3+"), "plan.type.3+",
            true, true, false, false, false, false, false, true),
    TYPE_3(Arrays.asList("Type3","Type 3"), "plan.type.3",
            false, false, false, false, false, false, false, false);

    private final List<String> value;
    private final String localizationKey;
    private final boolean collisionWithAnotherVehicle;
    private final boolean collisionWithoutThirdParty;
    private final boolean crashByYourself;
    private final boolean theft;
    private final boolean fire;
    private final boolean naturalDisaster;
    private final boolean flood;
    private final boolean towing;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PlanType fromValue(String value) {
        for (PlanType it : values()) {
            if (it.getValue().contains(value.trim())) {
                return it;
            }
        }
        throw new UnsupportedOperationException(MessageFormat.format("Cannot parse string {0} to enum {1}", value, PlanType.class));
    }

    @JsonValue
    public List<String> toValue() {
        return value;
    }
}
