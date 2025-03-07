package com.docprocess.pojo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Coverages {
    @JsonProperty("PA1")
    private String PA1;
    @JsonProperty("PA1Prem")
    private BigDecimal PA1Prem;
    @JsonProperty("ME")
    private String ME;
    @JsonProperty("MEPrem")
    private BigDecimal MEPrem;
    @JsonProperty("Daily_Cash")
    private String Daily_Cash;
    @JsonProperty("Daily_CashPrem")
    private BigDecimal Daily_CashPrem;
    @JsonProperty("Public_Accident")
    private String Public_Accident;
    @JsonProperty("Public_AccidentPrem")
    private BigDecimal Public_AccidentPrem;
    @JsonProperty("Broken_Bone")
    private String Broken_Bone;
    @JsonProperty("Broken_BonePrem")
    private BigDecimal Broken_BonePrem;
    @JsonProperty("Murdered")
    private String Murdered;
    @JsonProperty("MurderedPrem")
    private BigDecimal MurderedPrem;
    @JsonProperty("Motorcycle")
    private String Motorcycle;
    @JsonProperty("MotorcyclePrem")
    private BigDecimal MotorcyclePrem;
    @JsonProperty("Extreme_Sports")
    private String Extreme_Sports;
    @JsonProperty("Extreme_SportsPrem")
    private BigDecimal Extreme_SportsPrem;
    @JsonProperty("Dental")
    private String Dental;
    @JsonProperty("DentalPrem")
    private BigDecimal DentalPrem;
    @JsonProperty("FE_Injury")
    private String FE_Injury;
    @JsonProperty("FE_InjuryPrem")
    private BigDecimal FE_InjuryPrem;
    @JsonProperty("FE_Illness")
    private String FE_Illness;
    @JsonProperty("FE_IllnessPrem")
    private BigDecimal FE_IllnessPrem;
}
