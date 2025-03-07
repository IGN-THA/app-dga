package com.docprocess.pojo;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class PhAccount {
    private String PersonHasOptedOutOfEmail;
    private String businessRegNumber;
    private String companyNumber;
    private String countryOfIssuance;
    private String district;
    private  String districtCode;
    private String dob;
    private String driverAccidents;
    private int driverAge;
    private String drivingExperience;
    private String email;
    private String firstName;
    private String flagPDPAConsent;
    private String gender;
    private String houseNumber;
    private boolean isMainDriver;
    private String lastName;
    private String maritialStatus;
    private String occupation;
    private String passPortNumber;
    private String postalCode;
    private String province;
    private String provinceCode;
    private String residentStatus;
    private String soi;
    private String subDistrict;
    private String subDistrictCode;
    private String thaiIdNumber;
    private String village;
    private String phoneNumber;
}
