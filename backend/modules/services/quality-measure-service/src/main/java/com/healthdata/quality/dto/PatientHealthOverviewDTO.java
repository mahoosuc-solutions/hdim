package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for complete patient health overview
 * Aggregates health score, mental health, care gaps, and risk assessment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientHealthOverviewDTO {

    /**
     * Patient FHIR ID
     */
    private UUID patientId;

    /**
     * Overall health score
     */
    private HealthScoreDTO healthScore;

    /**
     * Recent mental health assessments (last 5)
     */
    private List<MentalHealthAssessmentDTO> recentMentalHealthAssessments;

    /**
     * Open care gaps
     */
    private List<CareGapDTO> openCareGaps;

    /**
     * Most recent risk assessment
     */
    private RiskAssessmentDTO riskAssessment;

    /**
     * Summary statistics
     */
    private SummaryStatsDTO summaryStats;

    /**
     * Summary Statistics nested DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryStatsDTO {
        /**
         * Total number of open care gaps
         */
        private Integer totalOpenCareGaps;

        /**
         * Number of urgent care gaps
         */
        private Integer urgentCareGaps;

        /**
         * Total mental health assessments on record
         */
        private Integer totalMentalHealthAssessments;

        /**
         * Number of positive mental health screens requiring follow-up
         */
        private Integer positiveScreensRequiringFollowup;
    }
}
