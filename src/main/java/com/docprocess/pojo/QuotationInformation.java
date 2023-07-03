package com.docprocess.pojo;

import com.docprocess.constant.CarUsage;
import com.docprocess.constant.DrivingExperience;
import com.docprocess.constant.Gender;
import com.docprocess.constant.MaritalStatus;
import com.docprocess.constant.NoClaimBonus;
import com.docprocess.constant.TentativeStartDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class QuotationInformation {
    private Boolean carCamera;
    private Double odometerReading;
    private CarUsage carUsage;
    private Integer numberOfAccidentInLast12Month;
    private Boolean carFinancing;
    private MaritalStatus maritalStatus;
    private LocalDate dateOfBirth;
    private DrivingExperience drivingExperience;
    private String postalCode;
    private NoClaimBonus noClaimBonus;
    private TentativeStartDate tentativeStartDate;
    private Gender gender;
}
