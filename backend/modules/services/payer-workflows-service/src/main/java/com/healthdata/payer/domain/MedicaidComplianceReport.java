package com.healthdata.payer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Medicaid state-specific compliance report.
 * Shows performance against state-mandated quality measures and thresholds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicaidComplianceReport {

    /**
     * Medicaid Managed Care Organization (MCO) identifier
     */
    private String mcoId;

    /**
     * MCO name
     */
    private String mcoName;

    /**
     * State configuration
     */
    private MedicaidStateConfig stateConfig;

    /**
     * Reporting period (e.g., "Q1 2024", "2024")
     */
    private String reportingPeriod;

    /**
     * Measurement year
     */
    private int measurementYear;

    /**
     * Individual measure results
     */
    private List<MedicaidMeasureResult> measureResults;

    /**
     * Overall compliance status
     */
    private ComplianceStatus overallStatus;

    /**
     * Overall compliance rate (percentage of measures meeting thresholds)
     */
    private double overallComplianceRate;

    /**
     * Total enrolled members
     */
    private int totalEnrollment;

    /**
     * Number of measures meeting state thresholds
     */
    private int measuresMetThreshold;

    /**
     * Number of measures below state thresholds
     */
    private int measuresBelowThreshold;

    /**
     * Measures requiring corrective action
     */
    private List<String> correctiveActionMeasures;

    /**
     * Penalty assessment (if applicable)
     */
    private PenaltyAssessment penaltyAssessment;

    /**
     * Quality bonus eligibility
     */
    private boolean qualityBonusEligible;

    /**
     * Estimated quality bonus amount
     */
    private Double estimatedBonus;

    /**
     * NCQA accreditation status
     */
    private String ncqaAccreditation;

    /**
     * Report generation timestamp
     */
    private LocalDateTime generatedAt;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    public enum ComplianceStatus {
        COMPLIANT,           // All measures meet thresholds
        SUBSTANTIALLY_COMPLIANT,  // >80% of measures meet thresholds
        PARTIALLY_COMPLIANT,      // 50-80% of measures meet thresholds
        NON_COMPLIANT        // <50% of measures meet thresholds
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicaidMeasureResult {
        private String measureCode;
        private String measureName;
        private double performanceRate;
        private double stateThreshold;
        private double stateGoal;
        private boolean meetsThreshold;
        private boolean meetsGoal;
        private int numerator;
        private int denominator;
        private Double priorYearRate;
        private Double improvement;
        private ComplianceLevel complianceLevel;

        public enum ComplianceLevel {
            EXCEEDS_GOAL,      // Above state goal
            MEETS_THRESHOLD,   // Between threshold and goal
            BELOW_THRESHOLD    // Below state threshold
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PenaltyAssessment {
        private boolean penaltyApplied;
        private double penaltyPercentage;
        private double estimatedPenaltyAmount;
        private List<String> penaltyReasons;
        private String correctiveActionPlan;
    }
}
