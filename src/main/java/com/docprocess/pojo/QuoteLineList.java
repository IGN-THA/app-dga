package com.docprocess.pojo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@Getter
@Builder
@NoArgsConstructor
@ToString
public class QuoteLineList {
    @JsonProperty("Adjusted_technical_premium__c")
    private double Adjusted_technical_premium__c;

    @JsonProperty("Bail_Bond_SI__c")
    private double Bail_Bond_SI__c;

    @JsonProperty("Basic_premium__c")
    private double Basic_premium__c;

    @JsonProperty("Cover_Code__c")
    private String Cover_Code__c;

    @JsonProperty("Medical_Expenses_SI__c")
    private double Medical_Expenses_SI__c;

    @JsonProperty("PA_Driver_SI__c")
    private double PA_Driver_SI__c;

    @JsonProperty("PA_passenger_SI__c")
    private double PA_passenger_SI__c;

    @JsonProperty("Premium_adjustment_pct__c")
    private double Premium_adjustment_pct__c;

    @JsonProperty("Reason_for_premium_adjustment__c")
    private String Reason_for_premium_adjustment__c;

    @JsonProperty("Tpbi_per_person_si__c")
    @JsonAlias("TPBI_per_person_SI__c")
    private double tpbi_per_person_si__c;

    @JsonProperty("Tpbi_per_accident_si__c")
    @JsonAlias("TPBI_per_accident_SI__c")
    private String Tpbi_per_accident_si__c;

    @JsonProperty("Tppd_si__c")
    @JsonAlias("TPPD_SI__c")
    private double Tppd_si__c;

    @JsonProperty("Od_si__c")
    @JsonAlias("OD_SI__c")
    private double Odsi;

    @JsonProperty("Fire_and_theft_si__c")
    @JsonAlias("Fire_and_Theft_SI__c")
    private double fireAndThefSi;

    @JsonProperty("Policy_Gross_premium__c")
    private double coverGrossPremium;
}
