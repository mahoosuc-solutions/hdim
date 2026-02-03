package com.healthdata.patient.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for patient risk assessment.
 *
 * Combines HCC-based RAF scores with care gap counts to provide
 * a comprehensive risk assessment view for clinical decision support.
 *
 * HIPAA Note: This response contains PHI aggregations. Ensure proper
 * cache control headers are applied and audit logging is enabled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Patient risk assessment combining HCC RAF scores with care gap data")
public class PatientRiskAssessmentResponse {

    @Schema(description = "Patient identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private String patientId;

    // ========================================================================
    // RAF SCORES
    // ========================================================================

    @Schema(description = "Blended RAF score (weighted combination of V24 and V28)",
            example = "1.234")
    private BigDecimal rafScoreBlended;

    @Schema(description = "RAF score under CMS-HCC V24 model", example = "1.156")
    private BigDecimal rafScoreV24;

    @Schema(description = "RAF score under CMS-HCC V28 model", example = "1.312")
    private BigDecimal rafScoreV28;

    // ========================================================================
    // RISK CLASSIFICATION
    // ========================================================================

    @Schema(description = "Risk level classification based on RAF score",
            example = "HIGH",
            allowableValues = {"LOW", "MODERATE", "HIGH", "VERY_HIGH"})
    private RiskLevel riskLevel;

    @Schema(description = "Numerical risk score (0-100) for UI display", example = "78")
    private Integer riskScore;

    // ========================================================================
    // HCC DETAILS
    // ========================================================================

    @Schema(description = "Total count of captured HCC codes", example = "5")
    private Integer hccCount;

    @Schema(description = "Top HCC codes by RAF impact (up to 5)",
            example = "[\"HCC18\", \"HCC85\", \"HCC96\"]")
    private List<String> topHccs;

    @Schema(description = "Chronic conditions derived from HCC codes",
            example = "[\"Diabetes with Complications\", \"Heart Failure\"]")
    private List<String> chronicConditions;

    // ========================================================================
    // CARE GAP INTEGRATION
    // ========================================================================

    @Schema(description = "Count of open care gaps", example = "3")
    private Integer openCareGaps;

    @Schema(description = "Count of high-priority care gaps", example = "1")
    private Integer highPriorityCareGaps;

    @Schema(description = "Count of overdue care gaps", example = "2")
    private Integer overdueCareGaps;

    // ========================================================================
    // OPPORTUNITY METRICS
    // ========================================================================

    @Schema(description = "Potential RAF uplift if documentation gaps are addressed",
            example = "0.145")
    private BigDecimal potentialRafUplift;

    @Schema(description = "Count of documentation gaps that could improve RAF score",
            example = "2")
    private Integer documentationGapCount;

    @Schema(description = "Count of prior-year HCCs needing recapture", example = "3")
    private Integer recaptureOpportunities;

    // ========================================================================
    // METADATA
    // ========================================================================

    @Schema(description = "Timestamp when risk assessment was calculated",
            example = "2026-02-02T15:30:00Z")
    private Instant calculatedAt;

    @Schema(description = "Profile year for HCC calculations", example = "2026")
    private Integer profileYear;

    @Schema(description = "Data availability indicators")
    private DataAvailability dataAvailability;

    // ========================================================================
    // ENUMS
    // ========================================================================

    /**
     * Risk level classification based on RAF score thresholds.
     *
     * Thresholds:
     * - LOW: RAF < 1.0 (below average expected cost)
     * - MODERATE: 1.0 <= RAF < 2.0 (average to moderately elevated)
     * - HIGH: 2.0 <= RAF < 3.5 (significantly elevated risk)
     * - VERY_HIGH: RAF >= 3.5 (complex, high-cost patients)
     */
    public enum RiskLevel {
        LOW,
        MODERATE,
        HIGH,
        VERY_HIGH
    }

    /**
     * Indicates which data sources were available for the assessment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Indicates which data sources were available")
    public static class DataAvailability {
        @Schema(description = "Whether HCC profile data was available", example = "true")
        private boolean hccDataAvailable;

        @Schema(description = "Whether care gap data was available", example = "true")
        private boolean careGapDataAvailable;

        @Schema(description = "Whether documentation gap data was available", example = "true")
        private boolean documentationGapDataAvailable;
    }

    // ========================================================================
    // FACTORY METHODS
    // ========================================================================

    /**
     * Calculate risk level from RAF score.
     *
     * @param rafScore the blended RAF score
     * @return risk level classification
     */
    public static RiskLevel calculateRiskLevel(BigDecimal rafScore) {
        if (rafScore == null) {
            return RiskLevel.LOW;
        }

        double score = rafScore.doubleValue();
        if (score < 1.0) {
            return RiskLevel.LOW;
        } else if (score < 2.0) {
            return RiskLevel.MODERATE;
        } else if (score < 3.5) {
            return RiskLevel.HIGH;
        } else {
            return RiskLevel.VERY_HIGH;
        }
    }

    /**
     * Convert RAF score to 0-100 scale for UI display.
     *
     * Uses a logarithmic scale to better represent the distribution:
     * - RAF 0.5 → ~25
     * - RAF 1.0 → ~50
     * - RAF 2.0 → ~70
     * - RAF 4.0 → ~90
     *
     * @param rafScore the blended RAF score
     * @return score from 0-100
     */
    public static Integer calculateRiskScore(BigDecimal rafScore) {
        if (rafScore == null || rafScore.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        double raf = rafScore.doubleValue();
        // Logarithmic scaling: score = 50 + 30 * log2(RAF)
        // This gives: RAF 0.5 → 20, RAF 1.0 → 50, RAF 2.0 → 80, RAF 4.0 → 100
        double logScore = 50 + (30 * Math.log(raf) / Math.log(2));
        return Math.max(0, Math.min(100, (int) Math.round(logScore)));
    }
}
