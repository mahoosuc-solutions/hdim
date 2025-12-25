package com.healthdata.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for sales pipeline metrics and forecasting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineMetricsDTO {

    // Pipeline Value
    private BigDecimal totalPipelineValue;
    private BigDecimal weightedPipelineValue;

    // Opportunity Counts by Stage
    private Map<String, Long> opportunitiesByStage;
    private Map<String, BigDecimal> valueByStage;

    // Conversion Metrics
    private Long totalLeads;
    private Long convertedLeads;
    private Double leadConversionRate;

    private Long totalOpportunities;
    private Long wonOpportunities;
    private Long lostOpportunities;
    private Double winRate;

    // Activity Metrics
    private Long activitiesThisMonth;
    private Long callsThisMonth;
    private Long emailsThisMonth;
    private Long meetingsThisMonth;
    private Long demosThisMonth;

    // Average Metrics
    private BigDecimal averageDealSize;
    private Integer averageSalesCycleDays;

    // Forecast
    private BigDecimal forecastThisMonth;
    private BigDecimal forecastThisQuarter;
}
