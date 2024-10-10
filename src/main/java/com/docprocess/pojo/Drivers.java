package com.docprocess.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Drivers {

    @JsonProperty("countryOfIssuance")
    private String countryOfIssuance;

    @JsonProperty("dob")
    private String dob; //LocalDate

    @JsonProperty("driverAccidents")
    private String driverAccidents;

    @JsonProperty("driverAge")
    private int driverAge;

    @JsonProperty("drivingExperience")
    private String drivingExperience;

    @JsonProperty("email")
    private String email;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("isMainDriver")
    private boolean isMainDriver;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("maritialStatus")
    private String maritialStatus;

    @JsonProperty("occupation")
    private String occupation;

    @JsonProperty("passPortNumber")
    private String passPortNumber;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @JsonProperty("residentStatus")
    private String residentStatus;

    @JsonProperty("thaiIdNumber")
    private String thaiIdNumber;
}