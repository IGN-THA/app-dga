package com.docprocess.pojo;

import com.docprocess.constant.DriverPlan;
import com.docprocess.constant.GarageOption;
import com.docprocess.constant.PaymentFrequency;
import com.docprocess.constant.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class QuoteSlipGenerationContext {
//
    private List<Accessory> accessories;
    private BrokerInfo brokerInfo;
    private List<Drivers> drivers;
    private PhAccount phaccount;
    private Quote quote;
    private List<QuoteLineList> quoteLineList;
    private Coverages coverages;
}
