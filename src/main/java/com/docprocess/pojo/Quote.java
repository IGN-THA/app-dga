package com.docprocess.pojo;

import com.docprocess.constant.PlanType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Quote {
    @JsonProperty("Additional_Benefit_PA_ME_SI")
    private String additionalBenefitPAMESI;

    @JsonProperty("Adjusted_vehicle_Sum_Insured")
    private String adjustedVehicleSumInsured;

    @JsonProperty("Vehicle_Sum_Insured1")
    private Double sumInsured;

    @JsonProperty("Beneficiary")
    private String beneficiary;

    @JsonProperty("carFinancing")
    private String carFinancing;

    @JsonProperty("CarFinancing")
    private String carFinancingSF;

    @JsonProperty("Compulsory_Plan")
    private String compulsoryPlan;

    @JsonProperty("Compulsory_type")
    private String compulsoryType;

    @JsonProperty("Created_From")
    private String createdFrom;
    
    @JsonProperty("Declared_NCB")
    private String declaredNCB;
    
    @JsonProperty("Driver_Plan")
    private String driverPlan;
    
    @JsonProperty("End_date")
    private String endDate;
    
    @JsonProperty("Engine_size")
    private String engineSize;
    
    @JsonProperty("Excess")
    private int excess;
    
    @JsonProperty("FamilyCode")
    private String familyCode;
    
    @JsonProperty("Final_Premium_Class")
    private String finalPremiumClass;
    
    @JsonProperty("Final_Premium_Group")
    private String finalPremiumGroup;
    
    @JsonProperty("Final_UW_Class")
    private String finalUWClass;
    
    @JsonProperty("Final_UW_Group")
    private String finalUWGroup;
    
    @JsonProperty("GoogleClickId")
    private String googleClickId;
    
    @JsonProperty("Google_Client_ID")
    private String googleClientID;
    
    @JsonProperty("Is_Main_Driver_PH")
    private boolean isMainDriverPH;
    
    @JsonProperty("KerbWeight")
    private String kerbWeight;
    
    @JsonProperty("Make")
    private String make;
    
    @JsonProperty("Masterset_Id")
    private String mastersetId;
    
    @JsonProperty("carModel")
    private String modelFamily;

    @JsonProperty("Model_Description")
    private String subModel;
    
    @JsonProperty("NewPrice")
    private String newPrice;
    
    @JsonProperty("Online_Screen")
    private String onlineScreen;
    
    @JsonProperty("Online_Stage")
    private String onlineStage;
    
    @JsonProperty("Opportunity_Number")
    private String opportunityNumber;
    
    @JsonProperty("PSN_value")
    private String psnValue;
    
    @JsonProperty("Plan_Type")
    private PlanType planType;
    
    @JsonProperty("Power")
    private String power;
    
    @JsonProperty("Product_Type")
    private String productType;
    
    @JsonProperty("QuoteToOppScored")
    private String quoteToOppScored;
    
    @JsonProperty("Rating_Factor_Defaulted")
    private boolean ratingFactorDefaulted;
    
    @JsonProperty("Renewal_Adjustment_Value")
    private String renewalAdjustmentValue;
    
    @JsonProperty("Seat_Capacity")
    private int seatCapacity;
    
    @JsonProperty("ShowMrKumkaInfo")
    private String showMrKumkaInfo;
    
    @JsonProperty("Start_date")
    private String startDate;
    
    @JsonProperty("Tariff_Group")
    private String tariffGroup;
    
    @JsonProperty("Vehicle_Key")
    private String vehicleKey;
    
    @JsonProperty("Vehicle_Sum_Insured")
    private String vehicleSumInsured;
    
    @JsonProperty("Vehicle_Type_Update_Flag")
    private String vehicleTypeUpdateFlag;
    
    @JsonProperty("Vehicle_Usage")
    private String vehicleUsage;
    
    @JsonProperty("Voluntary_TPBI")
    private String voluntaryTPBI;
    
    @JsonProperty("Voluntary_Type")
    private String voluntaryType;
    
    @JsonProperty("Workshop_Type")
    private String workshopType;
    
    @JsonProperty("Year_of_Manufacture")
    private int yearOfManufacture;
    
    @JsonProperty("abTestingVersion")
    private String abTestingVersion;
    
    @JsonProperty("approvedCode")
    private String approvedCode;
    
    @JsonProperty("carAccessoriesSI")
    private double carAccessoriesSI;

    @JsonProperty("carAccessoriesPrice")
    private double carAccessoriesPrice;
    
    @JsonProperty("carAccessoryCarCamera")
    private String carAccessoryCarCamera;
    
    @JsonProperty("carOwnerShip")
    private String carOwnerShip;
    
    @JsonProperty("carPlateNumber")
    private String carPlateNumber;
    
    @JsonProperty("carPlateProvince")
    private String carPlateProvince;
    
    @JsonProperty("carReplacementProductName")
    private String carReplacementProductName;
    
    @JsonProperty("carReplacementProductOptionNo")
    private String carReplacementProductOptionNo;
    
    @JsonProperty("chassisNumber")
    private String chassisNumber;
    
    @JsonProperty("customerIpAddress")
    private String customerIpAddress;
    
    @JsonProperty("derivedNCB")
    private double derivedNCB;
    
    @JsonProperty("ePolicyDocument")
    private String ePolicyDocument;
    
    @JsonProperty("engineNumber")
    private String engineNumber;
    
    @JsonProperty("goodsTransportRoute")
    private String goodsTransportRoute;
    
    @JsonProperty("goodsTransportVehicle")
    private String goodsTransportVehicle;
    
    @JsonProperty("howLongInsured")
    private String howLongInsured;
    
    @JsonProperty("isActivatePolicy")
    private boolean isActivatePolicy;
    
    @JsonProperty("isGAMarketeerlink")
    private String isGAMarketeerlink;
    
    @JsonProperty("isUpdate")
    private boolean isUpdate;
    
    @JsonProperty("isValidSourceCode")
    private boolean isValidSourceCode;
    
    @JsonProperty("odoMeterReading")
    private int odoMeterReading;
    
    @JsonProperty("paymentFrequency")
    private String paymentFrequency;
    
    @JsonProperty("postalCode")
    private String postalCode;
    
    @JsonProperty("prefLang")
    private String prefLang;
    
    @JsonProperty("rentalVehicle")
    private String rentalVehicle;
    
    @JsonProperty("rsaProduct")
    private String rsaProduct;
    
    @JsonProperty("send_email_flag")
    private boolean send_email_flag;
    
    @JsonProperty("taxiVehForHire")
    private String taxiVehForHire;
    
    @JsonProperty("tentativeStartDate")
    private String tentativeStartDate;
    
    @JsonProperty("totalPremium")
    private double totalPremium;
    
    @JsonProperty("validateSourceCode")
    private boolean validateSourceCode;
    
    @JsonProperty("versionId")
    private String versionId;
    
    @JsonProperty("whoDriveTheCar")
    private String whoDriveTheCar;
    
    @JsonProperty("whoseGoods")
    private String whoseGoods;

    @JsonProperty("priceDifferentPercentage")
    private double priceDifferent;

    @JsonProperty("flagAutoRenew")
    private String flagAutoRenew;

    @JsonProperty("assetname")
    private String assetName;

    @JsonProperty("oppCreateDate")
    private String oppCreateDate;

    @JsonProperty("compPrice")
    private double compPrice;

    @JsonProperty("rsaPrice")
    private double rsaPrice;

    @JsonProperty("pamePrice")
    private double pamePrice;

    @JsonProperty("tpbiPrice")
    private double tpbiPrice;

    @JsonProperty("carReplacementPrice")
    private double carReplacementPrice;

    @JsonProperty("addOnPrice")
    private double addOnPrice;

    @JsonProperty("shortLinkUrl")
    private String shortLinkUrl;

    @JsonProperty("samePlanAsLy")
    private String samePlanAsLy;

    @JsonProperty("otherAddOn")
    private double otherAddOn;

    @JsonProperty("volPrice")
    private double volPrice;

    @JsonProperty("rjReward")
    private double rjReward;

    @JsonProperty("Id")
    private String quoteId;
    @JsonProperty("premiumByPmntFrequency")
    private double premiumByPmntFrequency;

    @JsonProperty("policyInsurer")
    private String PolicyInsurer;

    @JsonProperty("salary")
    private String salary;

    @JsonProperty("PAComboSavePA1Prem")
    private BigDecimal PAComboSavePA1Prem;

    @JsonProperty("PAComboSavePA1SumAssured")
    private BigDecimal PAComboSavePA1SumAssured;

    @JsonProperty("PAComboSaveAddOnPrem")
    private BigDecimal PAComboSaveAddOnPrem;

    @JsonProperty("PAComboSaveStampDuty")
    private BigDecimal PAComboSaveStampDuty;

    @JsonProperty("instalmentPremium")
    private double instalmentPremium;

    @JsonProperty("firstInstalment")
    private double firstInstalment;

    @JsonProperty("leadSource")
    private String leadSource;
}