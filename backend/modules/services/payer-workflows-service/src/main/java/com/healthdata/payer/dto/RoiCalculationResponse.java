package com.healthdata.payer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoiCalculationResponse {

    @JsonProperty("id")
    private String id;

    // Inputs echoed back
    @JsonProperty("orgType")
    private String orgType;

    @JsonProperty("patientPopulation")
    private Integer patientPopulation;

    @JsonProperty("currentQualityScore")
    private BigDecimal currentQualityScore;

    @JsonProperty("currentStarRating")
    private BigDecimal currentStarRating;

    @JsonProperty("manualReportingHours")
    private Integer manualReportingHours;

    // Calculated results
    @JsonProperty("qualityImprovement")
    private BigDecimal qualityImprovement;

    @JsonProperty("projectedScore")
    private BigDecimal projectedScore;

    @JsonProperty("starImprovement")
    private BigDecimal starImprovement;

    @JsonProperty("projectedStarRating")
    private BigDecimal projectedStarRating;

    @JsonProperty("qualityBonuses")
    private BigDecimal qualityBonuses;

    @JsonProperty("adminSavings")
    private BigDecimal adminSavings;

    @JsonProperty("gapClosureValue")
    private BigDecimal gapClosureValue;

    @JsonProperty("totalYear1Value")
    private BigDecimal totalYear1Value;

    @JsonProperty("year1Investment")
    private BigDecimal year1Investment;

    @JsonProperty("year1ROI")
    private BigDecimal year1ROI;

    @JsonProperty("paybackDays")
    private BigDecimal paybackDays;

    @JsonProperty("threeYearNPV")
    private BigDecimal threeYearNPV;

    // Lead capture
    @JsonProperty("contactName")
    private String contactName;

    @JsonProperty("contactEmail")
    private String contactEmail;

    @JsonProperty("contactCompany")
    private String contactCompany;

    // Shareable link ID (only present if saved)
    @JsonProperty("shareUrl")
    private String shareUrl;

    @JsonProperty("createdAt")
    private Instant createdAt;
}
