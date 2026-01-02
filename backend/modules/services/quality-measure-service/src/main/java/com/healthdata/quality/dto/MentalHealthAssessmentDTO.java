package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for mental health assessment results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentalHealthAssessmentDTO {

    /**
     * Assessment ID
     */
    private String id;

    /**
     * Patient FHIR ID
     */
    private UUID patientId;

    /**
     * Assessment type enum value (PHQ_9, GAD_7, PHQ_2, etc.)
     */
    private String type;

    /**
     * Human-readable assessment name
     */
    private String name;

    /**
     * Calculated score
     */
    private Integer score;

    /**
     * Maximum possible score
     */
    private Integer maxScore;

    /**
     * Severity level (minimal, mild, moderate, moderately-severe, severe)
     */
    private String severity;

    /**
     * Clinical interpretation text
     */
    private String interpretation;

    /**
     * Whether this is a positive screen requiring follow-up
     */
    private Boolean positiveScreen;

    /**
     * Score threshold for positive screen
     */
    private Integer thresholdScore;

    /**
     * Whether clinical follow-up is required
     */
    private Boolean requiresFollowup;

    /**
     * Provider who administered the assessment
     */
    private String assessedBy;

    /**
     * Date/time the assessment was performed
     */
    private Instant assessmentDate;

    /**
     * Date/time the record was created
     */
    private Instant createdAt;
}
