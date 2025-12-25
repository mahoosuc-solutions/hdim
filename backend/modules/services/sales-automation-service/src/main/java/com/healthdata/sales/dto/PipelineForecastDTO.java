package com.healthdata.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * DTO for sales pipeline forecasting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineForecastDTO {

    // Current Period Summary
    private ForecastPeriod currentMonth;
    private ForecastPeriod currentQuarter;
    private ForecastPeriod currentYear;

    // Monthly Forecast Breakdown
    private List<MonthlyForecast> monthlyForecasts;

    // Conversion Funnel
    private ConversionFunnel funnel;

    // Historical Comparison
    private HistoricalComparison historical;

    // At-Risk Deals
    private List<AtRiskDeal> atRiskDeals;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastPeriod {
        private String periodName;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal committedValue;      // High probability (>70%)
        private BigDecimal bestCaseValue;       // Medium probability (30-70%)
        private BigDecimal pipelineValue;       // Low probability (<30%)
        private BigDecimal weightedForecast;    // Probability-weighted total
        private Long dealCount;
        private BigDecimal targetValue;
        private Double percentToTarget;
        private BigDecimal closedWonValue;      // Already closed this period
        private Long closedWonCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyForecast {
        private YearMonth month;
        private String monthName;
        private BigDecimal expectedValue;
        private BigDecimal weightedValue;
        private Long dealCount;
        private List<OpportunityDTO> topOpportunities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversionFunnel {
        private Long totalLeads;
        private Long qualifiedLeads;
        private Long opportunities;
        private Long demos;
        private Long proposals;
        private Long negotiations;
        private Long closedWon;

        // Conversion rates between stages
        private Double leadToQualifiedRate;
        private Double qualifiedToOpportunityRate;
        private Double opportunityToDemoRate;
        private Double demoToProposalRate;
        private Double proposalToNegotiationRate;
        private Double negotiationToCloseRate;
        private Double overallWinRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalComparison {
        private BigDecimal lastMonthClosed;
        private BigDecimal lastQuarterClosed;
        private BigDecimal lastYearClosed;
        private Double monthOverMonthGrowth;
        private Double quarterOverQuarterGrowth;
        private Double yearOverYearGrowth;
        private BigDecimal averageMonthlyRevenue;
        private BigDecimal averageQuarterlyRevenue;
        private Integer averageSalesCycleDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtRiskDeal {
        private OpportunityDTO opportunity;
        private String riskReason;
        private Integer riskScore;  // 1-100, higher = more risk
        private Integer daysSinceActivity;
        private Integer daysOverdue;
        private Boolean stagnant;
        private String recommendedAction;
    }
}
