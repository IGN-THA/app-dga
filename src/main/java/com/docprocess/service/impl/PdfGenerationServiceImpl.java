package com.docprocess.service.impl;

import com.docprocess.config.ErrorConfig;
import com.docprocess.constant.PdfQueueProcessingStatus;
import com.docprocess.manager.DocumentRenderException;
import com.docprocess.pojo.*;
import com.docprocess.repository.DocumentTypeDataRepository;
import com.docprocess.service.PdfGenerationService;
import com.docprocess.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.expression.ThymeleafEvaluationContext;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;

@Service
public class PdfGenerationServiceImpl implements PdfGenerationService {

    // public static final String[] FONTS = {
    //         "src/main/resources/fonts/cour.ttf"
    // };

    private final static BigDecimal STAMP_DUTY_PERCENTAGE = BigDecimal.valueOf(0.004);
    private final static BigDecimal VAT_PERCENTAGE = BigDecimal.valueOf(0.07);
    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;
    private final String renderedFilePath;
    private final JmsTemplate jmsTemplate;
    private final TemplateService templateService;
    private final ApplicationContext applicationContext;

    Logger logger = LogManager.getLogger(PdfGenerationServiceImpl.class);




    public PdfGenerationServiceImpl(DocumentTypeDataRepository documentTypeDataRepository, ObjectMapper objectMapper, TemplateEngine templateEngine, String renderedFilePath, JmsTemplate jmsTemplate, TemplateService templateService, ApplicationContext applicationContext) {
        this.objectMapper = objectMapper;
        this.templateEngine = templateEngine;
        this.renderedFilePath = renderedFilePath;
        this.jmsTemplate = jmsTemplate;
        this.templateService = templateService;
        this.applicationContext = applicationContext;
    }

    @Override
    public Single<PdfGenerationQueueResponse> queueRequest(PdfGenerationRequest request) {
        String requestId = UUID.randomUUID().toString();
        return Single.just(request)
                .observeOn(Schedulers.io())
                .flatMap(req -> templateService.getTemplate(req.getTemplateType()))
                .doOnSuccess(templateFile -> {
                    File pendingFile = new File(getRenderedFilePathPending(templateFile.getName(), requestId));
                    FileUtils.copyFile(templateFile, pendingFile);
                })
                .map(__ -> PdfGenerationRequest.builder()
                        .templateType(request.getTemplateType())
                        .context(request.getContext())
                        .requestId(requestId)
                        .locale(request.getLocale())
                        .callBackUrl(request.getCallBackUrl())
                        .emailTemplateName(request.getEmailTemplateName())
                        .quoteId(request.getQuoteId())
                        .build())
                .doOnSuccess(it -> jmsTemplate.convertAndSend("pdf.generation.request", it))
                .map(__ -> PdfGenerationQueueResponse.builder().status(PdfQueueProcessingStatus.PENDING).requestId(requestId).build())
                .onErrorResumeNext(err -> {
                    cleanupIntermediaryFolder(requestId);
                    String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "queueRequest", (Exception) err);
                    logger.error(errorMessage);
                    return Single.just(PdfGenerationQueueResponse.builder().status(PdfQueueProcessingStatus.FAILED).requestId(requestId).build());
                });

    }

    // TODO: Implemented cron job to clean-up rendered file after some period (retention policy)
    @Override
    public File generatePdfFromTemplate(File template, String requestId, LinkedHashMap<String, Object> ctx, Locale locale, LinkedHashMap<String,Object> localeMap) throws IOException, DocumentRenderException {
        String templateName = template.getName();
        File renderedFileInProgress = new File(getRenderedFilePathInProgress(templateName, requestId));
        return renderPdfFromTemplate(renderedFileInProgress, template, buildTemplateContext(ctx,locale,localeMap));
    }

    @Override
    public List<File> getPendingTemplate(String requestId) {
        return Optional.of(getPendingFolder(requestId))
                .map(File::new)
                .map(this::listFile)
                .orElse(null);
    }

    @Override
    public void cleanupIntermediaryFolder(String requestId) throws IOException {
        File pendingFolder = new File(getPendingFolder(requestId));
        File inProgressFolder = new File(getInProgressFolder(requestId));
        if (pendingFolder.exists()) {
            FileUtils.deleteDirectory(pendingFolder);
        }
        if (inProgressFolder.exists()) {
            FileUtils.deleteDirectory(inProgressFolder);
        }
    }

    @Override
    public PdfGenerationQueueResponse checkStatus(String requestId) {
        Function<PdfQueueProcessingStatus, PdfGenerationQueueResponse> builder =
                status -> PdfGenerationQueueResponse.builder().requestId(requestId).status(status).build();
        File inProgressFolder = new File(getInProgressFolder(requestId));
        if (!listFile(inProgressFolder).isEmpty()) {
            return builder.apply(PdfQueueProcessingStatus.RENDERING);
        }
        File pendingFolder = new File(getPendingFolder(requestId));
        if (!listFile(pendingFolder).isEmpty()) {
            return builder.apply(PdfQueueProcessingStatus.PENDING);
        }
        File renderedFolder = new File(getRenderedFolder(requestId));
        if (!listFile(renderedFolder).isEmpty()) {
            return builder.apply(PdfQueueProcessingStatus.RENDERED);
        }
        File renderedUpdate = new File(getUpdatedFilePath(requestId));
        if(!listFile(renderedUpdate).isEmpty()) {
            return builder.apply(PdfQueueProcessingStatus.UPLOADED);
        }

        return builder.apply(PdfQueueProcessingStatus.FAILED);
    }

    @Override
    public File finalizeRenderedFolder(String requestId) throws IOException {
        logger.error("\n[CALL]finalizeRenderedFolder with requestId: "+ requestId);
        File inProgressFolder = new File(getInProgressFolder(requestId));
        if (inProgressFolder!=null) {
            logger.error("inProgressFolder.getPath: "+inProgressFolder.getPath());
            logger.error("inProgressFolder.absolutePath: "+inProgressFolder.getAbsolutePath());
        }

        File renderedFolder = new File(getRenderedFolder(requestId));
        if (renderedFolder!=null) {
            logger.error("renderedFolder.getPath: "+renderedFolder.getPath());
            logger.error("renderedFolder.absolutePath: "+renderedFolder.getAbsolutePath());
        }
        FileUtils.moveDirectory(inProgressFolder, renderedFolder);
        cleanupIntermediaryFolder(requestId);
        return renderedFolder;
    }

    @Override
    public File finalizeUpdatedFolder(String requestId) throws IOException{
        File renderedFolder = new File(getRenderedFolder(requestId));
        File updatedFolder = new File(getUpdatedFilePath(requestId));
        FileUtils.moveDirectory(renderedFolder, updatedFolder);
        if (renderedFolder.exists()) {
            FileUtils.deleteDirectory(renderedFolder);
        }
        return renderedFolder;
    }

    @Override
    public File getRenderedTemplate(String requestId, String locale) {
        return Optional.of(requestId)
                .map(this::getUpdatedFilePath)
                .map(File::new)
                .map(this::listFile)
                .flatMap(it -> it.stream()
                .filter(file -> getLocale(file.getName()).getLanguage().equalsIgnoreCase(locale))
                .findAny())
                .orElse(null);
    }


    private List<File> listFile(File folder) {
        return Optional.of(folder)
                .filter(it -> it.isDirectory() && it.exists())
                .map(File::listFiles)
                .map(Arrays::asList)
                .orElse(new ArrayList<>());
    }

    private CoverOption calculateCoverOption(CoverOption option, Function<BigDecimal, BigDecimal> calculatePremiumFn) {
        return Optional.ofNullable(option)
                .map(it -> CoverOption.builder()
                        .sumInsured(it.getSumInsured())
                        .premiumBeforeTaxes(it.getPremiumBeforeTaxes())
                        .premium(calculatePremiumFn.apply(it.getPremiumBeforeTaxes()))
                        .build())
                .orElse(null);

    }

    private Locale getLocale(String templateName) {
        String baseName = FilenameUtils.getBaseName(templateName);
        return new Locale((baseName.substring(baseName.length() - "**".length())));
    }

    private void printObj(Object obj, String display) {
        if (obj!=null) {
            logger.info("\nctx."+display+" : "+obj.toString());
        }
    }
    private Context buildTemplateContext(LinkedHashMap<String, Object> context, Locale locale, LinkedHashMap<String,Object> localeMap) {
       //if (TemplateType.QUOTE_SLIP == templateType) {

        QuoteSlipGenerationContext ctx = objectMapper.convertValue(context.get("item"), QuoteSlipGenerationContext.class);

        InsuredVehicle insuredVehicle = InsuredVehicle.builder().make(ctx.getQuote().getMake()).model(ctx.getQuote().getModelFamily()).subModel(ctx.getQuote().getSubModel()).year(ctx.getQuote().getYearOfManufacture()).build();

        Context templateDetail = new Context();
        templateDetail.setLocale(locale);
        templateDetail.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME, new ThymeleafEvaluationContext(applicationContext, null));

        /*printObj(ctx.getQuote().getOpportunityNumber(), "getQuote().getOpportunityNumber()");
        printObj(ctx.getQuote(), "getQuote");
        printObj(ctx.getPhaccount(), "getPhaccount");
        printObj(ctx.getDrivers(), "getDrivers");
        printObj(ctx.getBrokerInfo(), "getBrokerInfo");
        printObj(ctx.getQuoteLineList(), "getQuoteLineList");*/

        /** Static text **/
        templateDetail.setVariable("created_from", context.get("created_from"));
         templateDetail.setVariable("Quotation_information", "Quotation information");
         templateDetail.setVariable("valid_for30Days", "valid for 30 days");
         templateDetail.setVariable("Details.header", "Details");
         templateDetail.setVariable("Coverage", "Coverage");
         templateDetail.setVariable("Your_product_selection", "Your product selection");
         templateDetail.setVariable("Damage_toOther", "Damages to other people or property when you cause an accident");
         templateDetail.setVariable("Additional_coverage", "Additional coverage");
         templateDetail.setVariable("windscreen_damaged", "If your windscreen is damaged");

        templateDetail.setVariable("Opportunity_number", ctx.getQuote().getOpportunityNumber());
        templateDetail.setVariable("Start_Date", ctx.getQuote().getStartDate());
        templateDetail.setVariable("End_Date", ctx.getQuote().getEndDate());
        templateDetail.setVariable("Quote_createdOn", OffsetDateTime.now());
        templateDetail.setVariable("Total_AnnualPrice",ctx.getQuote().getPremiumByPmntFrequency()>0?ctx.getQuote().getPremiumByPmntFrequency():ctx.getQuote().getTotalPremium()); // templateDetail.setVariable("Quote", ctx.getQuote());

        if (ctx.getQuote().getSalary() != null){
            templateDetail.setVariable("Quote_salary", ctx.getQuote().getSalary());
        }
        if (ctx.getBrokerInfo()!=null && ctx.getBrokerInfo().getIntermediaryAccountID()!=null && ctx.getBrokerInfo().getIntermediaryName()!=null) {
            templateDetail.setVariable("Service_provider", ctx.getBrokerInfo().getIntermediaryName());
            templateDetail.setVariable("Service_provider_phone", ctx.getBrokerInfo().getIntermediaryPhoneNumber());
            templateDetail.setVariable("Service_provider_id", ctx.getBrokerInfo().getIntermediaryAccountID());
            templateDetail.setVariable("Service_provider_account_source", ctx.getBrokerInfo().getAccountSource());
            templateDetail.setVariable("Service_provider_email", ctx.getBrokerInfo().getIntermediaryReptEmail());
            templateDetail.setVariable("Service_provider_call_file", ctx.getBrokerInfo().getCreateCallFile());
            if (ctx.getBrokerInfo().getImageUrl()!=null) {
                templateDetail.setVariable("Service_provider_imageUrl", ctx.getBrokerInfo().getImageUrl());
            }
        }
        // templateDetail.setVariable("Covers_upTo", );
        // templateDetail.setVariable("AddOn_cost", );

        /** NewBiz asset **/
        templateDetail.setVariable("Insured_vehicle", insuredVehicle);

        /** Renewal **/
            if (ctx.getQuote().getFlagAutoRenew()!=null) {
                templateDetail.setVariable("Flag_Auto_Renew", ctx.getQuote().getFlagAutoRenew());
            }
            templateDetail.setVariable("opp_assetName", isValidString(ctx.getQuote().getAssetName()));
            if(ctx.getQuote().getAssetName()==null){
                templateDetail.setVariable("opp_assetName",ctx.getQuote().getMake()+"/"+ctx.getQuote().getModelFamily()+"/"+ctx.getQuote().getSubModel()+"/"+ctx.getQuote().getYearOfManufacture());
            }
            if (ctx.getQuote().getPriceDifferent()>0) {
                templateDetail.setVariable("Price_Different_Percentage", ctx.getQuote().getPriceDifferent());
            }
            if (ctx.getQuote().getSamePlanAsLy()!=null) {
                templateDetail.setVariable("SamePlanAsLy", isValidString(ctx.getQuote().getSamePlanAsLy()));
            }


        templateDetail.setVariable("Opp_createdOn", isValidString(ctx.getQuote().getOppCreateDate()));
        templateDetail.setVariable("Car_camera", ctx.getQuote().getCarAccessoryCarCamera());
        templateDetail.setVariable("Odometer_reading", ctx.getQuote().getOdoMeterReading());
        templateDetail.setVariable("Car_usage", ctx.getQuote().getVehicleUsage());
        templateDetail.setVariable("Car_financing", (ctx.getQuote().getCarFinancing()==null?ctx.getQuote().getCarFinancingSF():ctx.getQuote().getCarFinancing()));

        if (ctx.getPhaccount()!=null) {
            templateDetail.setVariable("PhAccount", ctx.getPhaccount());
            templateDetail.setVariable("PhAccount_Email", isValidString(ctx.getPhaccount().getEmail()));
            templateDetail.setVariable("PhAccount_PhoneNumber", isValidString(ctx.getPhaccount().getPhoneNumber()));
            templateDetail.setVariable("PhAccount_Dob", isValidString(ctx.getPhaccount().getDob()));
            templateDetail.setVariable("PhAccount_Driving_Experience", isValidString(ctx.getPhaccount().getDrivingExperience()));
            templateDetail.setVariable("PhAccount_Number_AccidentsIn12Months", isValidString(ctx.getPhaccount().getDriverAccidents()));
            if (ctx.getPhaccount().getGender()!=null && ctx.getPhaccount().getMaritialStatus()!=null) {
                templateDetail.setVariable("PhAccount_Gender_MaritalStatus", ctx.getPhaccount().getGender()+" / "+ctx.getPhaccount().getMaritialStatus());
            }
            if(ctx.getPhaccount().getOccupation() != null){
                templateDetail.setVariable("PhAccount_Occupation", isValidString(ctx.getPhaccount().getOccupation()));
            }
        }

        if (ctx.getDrivers()!=null && ctx.getDrivers().size()>0 && ctx.getDrivers().get(0)!=null) {
            templateDetail.setVariable("Named_Drivers", ctx.getDrivers());
            templateDetail.setVariable("Driver_Email", isValidString(ctx.getDrivers().get(0).getEmail()));
            templateDetail.setVariable("Driver_PhoneNumber", isValidString(ctx.getDrivers().get(0).getPhoneNumber()));
            templateDetail.setVariable("Driver_Dob", isValidString(ctx.getDrivers().get(0).getDob()));
            templateDetail.setVariable("Driver_Driving_Experience", isValidString(ctx.getDrivers().get(0).getDrivingExperience()));
            templateDetail.setVariable("Driver_Number_AccidentsIn12Months", isValidString(ctx.getDrivers().get(0).getDriverAccidents()));
            if (ctx.getDrivers().get(0).getGender()!=null && ctx.getDrivers().get(0).getMaritialStatus()!=null) {
                templateDetail.setVariable("Driver_Gender_MaritalStatus", ctx.getDrivers().get(0).getGender() + " / " + ctx.getDrivers().get(0).getMaritialStatus());
            }
        }

        templateDetail.setVariable("Postal_code", isValidString(ctx.getQuote().getPostalCode()));
        String ncb=ctx.getQuote().getDeclaredNCB();
        ncb="I don't know".equalsIgnoreCase(ncb)?"N":ncb;
        templateDetail.setVariable("No_Claim_bonus", isValidString(ncb).replace("%",""));
        templateDetail.setVariable("Tentative_start_date", ctx.getQuote().getTentativeStartDate());
        double volPremium = 0;
        if(ctx.getQuoteLineList().size()>0){
            volPremium= ctx.getQuoteLineList().get(0).getCoverGrossPremium();
        }
        templateDetail.setVariable("Voluntary_premium",volPremium>0?volPremium:ctx.getQuote().getTotalPremium());
        if (ctx.getQuote().getVolPrice()>0) {
            templateDetail.setVariable("Voluntary_premium", ctx.getQuote().getVolPrice());
        }
        // templateDetail.setVariable("Compulsory_premium", compulsoryPremium);
        // templateDetail.setVariable("Other_addOns", otherAddonPremium);

        templateDetail.setVariable("Payment_frequency", ctx.getQuote().getPaymentFrequency());
        templateDetail.setVariable("Sum_insured", ctx.getQuote().getSumInsured()==null?ctx.getQuote().getVehicleSumInsured():ctx.getQuote().getSumInsured());
        templateDetail.setVariable("Plan_type", ctx.getQuote().getPlanType());
        templateDetail.setVariable("Excess", ctx.getQuote().getExcess());
        templateDetail.setVariable("DriverPlan", ctx.getQuote().getDriverPlan());
        templateDetail.setVariable("GaragePlan", ctx.getQuote().getWorkshopType());
        if(ctx.getQuote().getPlanType() != null){
            templateDetail.setVariable("hit_another_vehicle", ctx.getQuote().getPlanType().isCollisionWithAnotherVehicle());
            templateDetail.setVariable("another_vehicle_hits_you", ctx.getQuote().getPlanType().isCollisionWithoutThirdParty());
            templateDetail.setVariable("crash_by_yourself", ctx.getQuote().getPlanType().isCrashByYourself());
            templateDetail.setVariable("Theft", ctx.getQuote().getPlanType().isTheft());
            templateDetail.setVariable("Fire", ctx.getQuote().getPlanType().isFire());
            templateDetail.setVariable("Flood", ctx.getQuote().getPlanType().isFlood());
            templateDetail.setVariable("towing", ctx.getQuote().getPlanType().isTowing());
        }

        templateDetail.setVariable("Addon_accessories", ctx.getQuote().getCarAccessoriesSI());

        // templateDetail.setVariable("Addon_accessories_SI", ctx.getQuote().getCarAccessoriesSI());
        templateDetail.setVariable("Addon_accessories_SI", setDoubleVariable(ctx.getQuote().getCarAccessoriesSI()));


        if (ctx.getQuoteLineList()!=null && ctx.getQuoteLineList().size()>0) {
            /** Adjusted_technical_premium__c **/

            /** Bail_Bond_SI__c **/
                templateDetail.setVariable("Bail_bond", ctx.getQuoteLineList().get(0).getBail_Bond_SI__c());
                templateDetail.setVariable("Bail_bond_SI", ctx.getQuoteLineList().get(0).getBail_Bond_SI__c());

            /** Basic_premium__c **/
            /** Cover_Code__c **/

            /** Medical_Expenses_SI__c **/
                templateDetail.setVariable("MedicalExpense_you_your_passengers", ctx.getQuoteLineList().get(0).getMedical_Expenses_SI__c());
                templateDetail.setVariable("Medical_Expenses_SI", ctx.getQuoteLineList().get(0).getMedical_Expenses_SI__c());

            /** PA_Driver_SI__c **/
                templateDetail.setVariable("PersonalAccident_you_your_drivers" , ctx.getQuoteLineList().get(0).getPA_Driver_SI__c());
                templateDetail.setVariable("PA_Driver_SI" , ctx.getQuoteLineList().get(0).getPA_Driver_SI__c());

            /** PA_passenger_SI__c **/
                templateDetail.setVariable("PersonalAccident_you_your_passengers" , ctx.getQuoteLineList().get(0).getPA_passenger_SI__c());
                templateDetail.setVariable("PA_passenger_SI" , ctx.getQuoteLineList().get(0).getPA_passenger_SI__c());

            /** Premium_adjustment_pct__c **/
            /** Reason_for_premium_adjustment__c **/

            /** Tpbi_per_person_si__c **/
                // templateDetail.setVariable("BodilyInjury_perPerson", ctx.getQuote().getVoluntaryTPBI());
                templateDetail.setVariable("BodilyInjury_perPerson", ctx.getQuoteLineList().get(0).getTpbi_per_person_si__c());
                templateDetail.setVariable("Tpbi_per_person_SI", ctx.getQuoteLineList().get(0).getTpbi_per_person_si__c());

            /** Tpbi_per_accident_si__c **/
                // templateDetail.setVariable("BodilyInjury_perAccident", 10000000);
                templateDetail.setVariable("BodilyInjury_perAccident", ctx.getQuoteLineList().get(0).getTpbi_per_accident_si__c());
                templateDetail.setVariable("Tpbi_per_accident_SI", ctx.getQuoteLineList().get(0).getTpbi_per_accident_si__c());

            /** Tppd_si__c **/
                // templateDetail.setVariable("Property_damage", 5000000);
                templateDetail.setVariable("Property_damage", ctx.getQuoteLineList().get(0).getTppd_si__c());
                templateDetail.setVariable("Tppd_SI", ctx.getQuoteLineList().get(0).getTppd_si__c());

            /** Od_si__c **/
                templateDetail.setVariable("Od_SI", ctx.getQuoteLineList().get(0).getOdsi());

            /** Fire_and_theft_si__c **/
                templateDetail.setVariable("Fire_and_theft_SI", ctx.getQuoteLineList().get(0).getFireAndThefSi());
        }

        if(ctx.getQuote().getCompulsoryPlan() != null){
            String compulsoryPlan = ctx.getQuote().getCompulsoryPlan();
            templateDetail.setVariable("Compulsory_carInsurance",compulsoryPlan.equalsIgnoreCase("NoComp") || compulsoryPlan.equalsIgnoreCase("No")  ? "Not Included" : "Included");
        }

        if (ctx.getQuote().getRsaProduct()!=null) {
            templateDetail.setVariable("Roadside_assistance", ctx.getQuote().getRsaProduct().equalsIgnoreCase("No") ? "Not Included" : "Included");
        }
//        templateDetail.setVariable("Roadside_assistance", ctx.getQuote().getRsaProduct().equalsIgnoreCase("No") ? "Not Included" : "Included");

        if (ctx.getQuote().getCarReplacementProductName()!=null) {
            templateDetail.setVariable("Car_replacement", StringUtils.isEmpty(ctx.getQuote().getCarReplacementProductName()) ? "Not Included" : "Included");
        }
//        templateDetail.setVariable("Car_replacement", StringUtils.isEmpty(ctx.getQuote().getCarReplacementProductName()) ? "Not Included" : "Included");

        // templateDetail.setVariable("Addon_accessories_price", ctx.getQuote().getCarAccessoriesPrice());
        templateDetail.setVariable("Addon_accessories_price", setDoubleVariable(ctx.getQuote().getCarAccessoriesPrice()));

        // templateDetail.setVariable("Comp_price", ctx.getQuote().getCompPrice()!=null ? ctx.getQuote().getCompPrice() : "0");
        templateDetail.setVariable("Comp_price", setDoubleVariable(ctx.getQuote().getCompPrice()));

        // templateDetail.setVariable("Rsa_price", ctx.getQuote().getRsaPrice()!=null ? ctx.getQuote().getRsaPrice() : "0");
        templateDetail.setVariable("Rsa_price", setDoubleVariable(ctx.getQuote().getRsaPrice()));

        // templateDetail.setVariable("Pame_price", ctx.getQuote().getPamePrice()!=null ? ctx.getQuote().getPamePrice() : "0");
        templateDetail.setVariable("Pame_price", setDoubleVariable(ctx.getQuote().getPamePrice()));

        // templateDetail.setVariable("Tpbi_price", ctx.getQuote().getTpbiPrice()!=null ? ctx.getQuote().getTpbiPrice() : "0");
        templateDetail.setVariable("Tpbi_price", setDoubleVariable(ctx.getQuote().getTpbiPrice()));

        // templateDetail.setVariable("Car_Replacement_price", setStringVariable(ctx.getQuote().getCarReplacementPrice()));
        templateDetail.setVariable("Car_Replacement_price", setDoubleVariable(ctx.getQuote().getCarReplacementPrice()));

        templateDetail.setVariable("Policy_Insurer", ctx.getQuote().getPolicyInsurer());
        
        /** Total addon price **/ // Double addOnPrice = ctx.getQuote().getAddOnPrice();
        // templateDetail.setVariable("Add_on_price", addOnPrice!=null ? addOnPrice : "0");
        templateDetail.setVariable("Add_on_price", setDoubleVariable(ctx.getQuote().getAddOnPrice()).toString());
        templateDetail.setVariable("Other_Add_on_price", setDoubleVariable(ctx.getQuote().getOtherAddOn()).toString());

        templateDetail.setVariable("personalAccidentMedicalExpense_premiumBeforeTaxes", context.get("personalAccidentMedicalExpense"));
        templateDetail.setVariable("bodilyInjurePerPerson_premiumBeforeTaxes", context.get("bodilyInjurePerPerson"));

        if (ctx.getQuote().getShortLinkUrl()!=null) {
            templateDetail.setVariable("retrieveQuotationLink", ctx.getQuote().getShortLinkUrl());
        }

        templateDetail.setVariable("rjReward", setDoubleVariable(ctx.getQuote().getRjReward()).toString());


        templateDetail.setVariable("locales", localeMap);

        if(ctx.getQuote().getProductType()!=null){
            if (ctx.getQuote().getProductType().equals("PAComboSave")){
                templateDetail.setVariable("Product_type", ctx.getQuote().getProductType());
                templateDetail.setVariable("PAC_PA1Prem", ctx.getQuote().getPAComboSavePA1Prem());
                templateDetail.setVariable("PAC_PA1SumAssured", ctx.getQuote().getPAComboSavePA1SumAssured());
                templateDetail.setVariable("PAC_AddOnPrem", ctx.getQuote().getPAComboSaveAddOnPrem());
                templateDetail.setVariable("PAC_StampDuty", ctx.getQuote().getPAComboSaveStampDuty());
            }
        }


        if (ctx.getCoverages()!=null){
            templateDetail.setVariable("PAC_Cover_PA1", ctx.getCoverages().getPA1());
            templateDetail.setVariable("PAC_Cover_PA1Prem", ctx.getCoverages().getPA1Prem());

            templateDetail.setVariable("PAC_Cover_ME", ctx.getCoverages().getME());
            templateDetail.setVariable("PAC_Cover_MEPrem", ctx.getCoverages().getMEPrem());

            templateDetail.setVariable("PAC_Cover_Daily_Cash", ctx.getCoverages().getDaily_Cash());
            templateDetail.setVariable("PAC_Cover_Daily_CashPrem", ctx.getCoverages().getDaily_CashPrem());

            templateDetail.setVariable("PAC_Cover_Public_Accident", ctx.getCoverages().getPublic_Accident());
            templateDetail.setVariable("PAC_Cover_Public_AccidentPrem", ctx.getCoverages().getPublic_AccidentPrem());

            templateDetail.setVariable("PAC_Cover_Broken_Bone", ctx.getCoverages().getBroken_Bone());
            templateDetail.setVariable("PAC_Cover_Broken_BonePrem", ctx.getCoverages().getBroken_BonePrem());

            templateDetail.setVariable("PAC_Cover_Murdered", ctx.getCoverages().getMurdered());
            templateDetail.setVariable("PAC_Cover_MurderedPrem", ctx.getCoverages().getMurderedPrem());

            templateDetail.setVariable("PAC_Cover_Motorcycle", ctx.getCoverages().getMotorcycle());
            templateDetail.setVariable("PAC_Cover_MotorcyclePrem", ctx.getCoverages().getMotorcyclePrem());

            templateDetail.setVariable("PAC_Cover_Extreme_Sports", ctx.getCoverages().getExtreme_Sports());
            templateDetail.setVariable("PAC_Cover_Extreme_SportsPrem", ctx.getCoverages().getExtreme_SportsPrem());

            templateDetail.setVariable("PAC_Cover_Dental", ctx.getCoverages().getDental());
            templateDetail.setVariable("PAC_Cover_DentalPrem", ctx.getCoverages().getDentalPrem());

            templateDetail.setVariable("PAC_Cover_FE_Injury", ctx.getCoverages().getFE_Injury());
            templateDetail.setVariable("PAC_Cover_FE_InjuryPrem", ctx.getCoverages().getFE_InjuryPrem());

            templateDetail.setVariable("PAC_Cover_FE_Illness", ctx.getCoverages().getFE_Illness());
            templateDetail.setVariable("PAC_Cover_FE_IllnessPrem", ctx.getCoverages().getFE_IllnessPrem());
        }

        templateDetail.setVariable("carPlate", ctx.getQuote().getCarPlateNumber() + ctx.getQuote().getCarPlateProvince());

        templateDetail.setVariable("instalmentPremium", setDoubleVariable(ctx.getQuote().getInstalmentPremium()).toString());
        templateDetail.setVariable("firstInstalment", setDoubleVariable(ctx.getQuote().getFirstInstalment()).toString());

        templateDetail.setVariable("leadSource", ctx.getQuote().getLeadSource()!=null? ctx.getQuote().getLeadSource():"");
//        Set<String> variables = templateDetail.getVariableNames();
//
//        logger.info("\nVariables in template");
//        variables.forEach((n) ->
//                logger.info(n + ": " + templateDetail.getVariable(n))
//        );
        return templateDetail;
    }

    private Double setDoubleVariable(Double input) {
        return input = input>0 ? input: 0;
    }
    private String setStringNumberVariable(String input) {
        return input = input!=null ? input: "0";
    }
    private String isValidString(String input) {
        return input != null && !input.isEmpty() ? input : "-";
    }

    private String getTemplateType(String templateName) {
        String baseName = FilenameUtils.getBaseName(templateName);
        return templateName.substring(0, baseName.length() - "-**".length());
    }

    private File renderPdfFromTemplate(File renderedFileInProgress, File template, Context ctx) throws IOException, DocumentRenderException {
        logger.info("[CALL] renderPdfFromTemplate with template: "+template);
        /*logger.debug("[CALL] renderPdfFromTemplate with template: "+template);
        logger.error("[CALL] renderPdfFromTemplate with template: "+template);*/
        if (!renderedFileInProgress.getParentFile().exists()) {
            renderedFileInProgress.getParentFile().mkdirs();
        }
        File tempFile = null;
        try (OutputStream os = Files.newOutputStream(Paths.get(renderedFileInProgress.getAbsolutePath()))) {
            String htmlContent = templateEngine.process(template.getAbsolutePath(),  ctx);
            tempFile = File.createTempFile("temp", ".html");
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
                 OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
                outputStreamWriter.write(htmlContent);
            }
            PdfPlayRight convertToPDF = new PdfPlayRight(tempFile);
            convertToPDF.set(renderedFileInProgress.getAbsolutePath());
            convertToPDF.get();

            return renderedFileInProgress;
        } catch (Exception e) {

            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "renderPdfFromTemplate", e);
            throw new DocumentRenderException(errorMessage);
        } finally {
            if(tempFile != null)
            {
                logger.info("deleteTemp : "+ tempFile.getAbsolutePath());
                boolean result = tempFile.delete();
                if (result) {
                    logger.info(tempFile.getName() + " is deleted!");
                } else {
                    logger.info(tempFile.getName() + " unable to delete the file.");
                }
            }
        }
    }

//    private File renderPdfFromTemplate(File renderedFileInProgress, File template, Context ctx) throws IOException {
//        logger.info("[CALL] renderPdfFromTemplate with template: "+template);
//        logger.debug("[CALL] renderPdfFromTemplate with template: "+template);
//        logger.error("[CALL] renderPdfFromTemplate with template: "+template);
//        if (!renderedFileInProgress.getParentFile().exists()) {
//            renderedFileInProgress.getParentFile().mkdirs();
//        }
//        try (OutputStream os = Files.newOutputStream(Paths.get(renderedFileInProgress.getAbsolutePath()))) {
//
//            String htmlContent = templateEngine.process(template.getAbsolutePath(),  ctx);
//            File tempFile = File.createTempFile("temp", ".html");
//            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile);             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {            outputStreamWriter.write(htmlContent);        }        pdfPlayRight convertToPDF = new pdfPlayRight(tempFile);        convertToPDF.set(renderedFileInProgress.getAbsolutePath());        convertToPDF.get();                return renderedFileInProgress;    } catch (Exception e) {        throw new RuntimeException(e);    }
//
//
//            /** LIB - openhtmltopdf **/
////            PdfRendererBuilder builder = new PdfRendererBuilder();
////            builder.useFastMode();
////            builder.withHtmlContent(htmlContent, template.getParentFile().getAbsolutePath());
////            builder.useSVGDrawer(new BatikSVGDrawer());
////                /** Test FONT **/
////                /* builder.useFont(new File("C:\\Users\\worrasiri\\Documents\\workspace\\DocMngt.Branch.CG-45\\src\\main\\resources\\fonts\\cour.ttf"), "Couries New");
////                builder.useFont(new File(getClass().getClassLoader().getResource("fonts/cour.ttf").getFile()), "CustomFont");
////                builder.useFont(new File(getClass().getClassLoader().getResource("fonts/NotoSansThai-Bold.ttf").getFile()), "NotoSansThaiBold");
////                builder.useFont(new File(getClass().getClassLoader().getResource("fonts/NotoSansThai-Regular.ttf").getFile()), "NotoSansThaiRegular");*/
////            builder.toStream(os);
////            builder.run();
//
////            PdfPlayRight pdfPlayRight = new PdfPlayRight(template);
////            pdfPlayRight.set(renderedFileInProgress.getAbsolutePath());
////            pdfPlayRight.get();
//
//
//            //            Document doc = Jsoup.parse(htmlContent);
//            //            doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
//
//            //            WriterProperties writerProperties = new WriterProperties();
//            //            writerProperties.addXmpMetadata();
//
//            //            PdfDocument pdf = new PdfDocument();
//            //            PdfWriter pdfWriter = new PdfWriter(pdf, os);
//            //            pdfDoc = createPdfDoc(pdfWriter);
//
//            //            ByteArrayOutputStream target = new ByteArrayOutputStream();
//            //            ConverterProperties converterProperties = new ConverterProperties();
//            //            converterProperties.setBaseUri("http://localhost:8080");
//            //
//            //            FontProvider font = new FontProvider();
//            //            font.addStandardPdfFonts();
//            //            converterProperties.setFontProvider(font);
//            //
//            //            /* Call convert method */
//            //            HtmlConverter.convertToPdf(htmlContent, target, converterProperties);
//            //            /* extract output as bytes */
//            //            target.writeTo(os);
//
//            /** LIB - print PDF **/
//            /*
//            File tempFile = File.createTempFile("temp", ".html");
//            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
//                 OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
//                outputStreamWriter.write(htmlContent);
//            }
//            PrintingPageToPdf convertToPDF = new PrintingPageToPdf(tempFile);
//            convertToPDF.set(renderedFileInProgress.getAbsolutePath());
//            convertToPDF.get();*/
//
//            /** LIB - Itext **/
//            /* final String FONT = templateService.getMessageType(getClass().getClassLoader().getResource("fonts/Sarabun-Regular.ttf").getPath());
//            ITextRenderer renderer = new ITextRenderer();
//            renderer.getFontResolver().addFont(FONT, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
//            renderer.getFontResolver().addFont(getClass().getClassLoader().getResource("fonts/Sarabun-Regular.ttf").toExternalForm(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
//            SharedContext cntxt = renderer.getSharedContext();
//            cntxt.setPrint(true);
//            cntxt.setInteractive(false);
//            renderer.setDocumentFromString(htmlContent);
//            renderer.layout();
//            renderer.createPDF(os);
//            renderer.finishPDF();*/
//
//        }
//
//            return renderedFileInProgress;
//    }

    private String getPendingFolder(String requestId) {
        return MessageFormat.format("{0}_Pending/{1}", renderedFilePath, requestId);
    }

    private String getInProgressFolder(String requestId) {
        return MessageFormat.format("{0}_InProgress/{1}", renderedFilePath, requestId);
    }

    private String getRenderedFolder(String requestId) {
        return MessageFormat.format("{0}/{1}", renderedFilePath, requestId);
    }

    private String getUpdatedFilePath(String requestId) {
        return MessageFormat.format("{0}_Updated/{1}", renderedFilePath, requestId);
    }

    private String getRenderedFilePath(String renderedName, String requestId, Function<String, String> getFolderFn) {
        return MessageFormat.format("{0}/{1}.pdf", getFolderFn.apply(requestId), FilenameUtils.getBaseName(renderedName));
    }

    private String getRenderedFilePathInProgress(String renderedName, String requestId) {
        return getRenderedFilePath(renderedName, requestId, this::getInProgressFolder);
    }

    private String getRenderedFilePathComplete(String renderedName, String requestId) {
        return getRenderedFilePath(renderedName, requestId, this::getRenderedFolder);
    }

    private String getRenderedFilePathPending(String templateName, String requestId) {
        return MessageFormat.format("{0}/{1}.html", getPendingFolder(requestId), FilenameUtils.getBaseName(templateName));
    }
}