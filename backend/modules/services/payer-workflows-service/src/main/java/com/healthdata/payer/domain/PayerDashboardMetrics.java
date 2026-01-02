package com.healthdata.payer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Aggregated metrics for payer dashboard overview.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayerDashboardMetrics {

    /**
     * Payer/organization identifier
     */
    private String payerId;

    /**
     * Payer name
     */
    private String payerName;

    /**
     * Dashboard type (MEDICARE_ADVANTAGE, MEDICAID_MCO, COMMERCIAL)
     */
    private DashboardType dashboardType;

    /**
     * Total enrolled members across all plans
     */
    private int totalEnrollment;

    /**
     * Number of active plans
     */
    private int activePlans;

    /**
     * Number of contracted providers
     */
    private int contractedProviders;

    /**
     * Medicare Advantage metrics (if applicable)
     */
    private MedicareAdvantageMetrics medicareMetrics;

    /**
     * Medicaid MCO metrics (if applicable)
     */
    private MedicaidMcoMetrics medicaidMetrics;

    /**
     * Quality measure summary
     */
    private QualityMeasureSummary qualitySummary;

    /**
     * Financial impact summary
     */
    private FinancialImpactSummary financialSummary;

    /**
     * Top performing measures
     */
    private Map<String, Double> topPerformingMeasures;

    /**
     * Measures needing attention
     */
    private Map<String, Double> measuresNeedingAttention;

    /**
     * Metrics generation timestamp
     */
    private LocalDateTime generatedAt;

    public enum DashboardType {
        MEDICARE_ADVANTAGE,
        MEDICAID_MCO,
        COMMERCIAL,
        ALL
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicareAdvantageMetrics {
        private double averageStarRating;
        private int plansWithFourStarsOrMore;
        private int plansWithThreeStarsOrLess;
        private int totalMedicarePlans;
        private boolean qualityBonusPaymentEligible;
        private double estimatedBonusPayments;
        private double yearOverYearImprovement;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicaidMcoMetrics {
        private int numberOfStates;
        private double averageComplianceRate;
        private int compliantPlans;
        private int nonCompliantPlans;
        private double estimatedPenalties;
        private double estimatedBonuses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityMeasureSummary {
        private int totalMeasures;
        private int measuresAboveBenchmark;
        private int measuresBelowBenchmark;
        private double averagePerformanceRate;
        private double averageImprovementRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialImpactSummary {
        private double totalPotentialRevenue;
        private double currentQualityBonuses;
        private double potentialQualityBonuses;
        private double currentPenalties;
        private double riskOfPenalties;
        private double netFinancialImpact;
    }
}
