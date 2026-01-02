package com.healthdata.quality.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Represents a care gap identified during measure calculation.
 * Care gaps are opportunities to improve patient care and quality scores.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CareGap {

    /**
     * Type of care gap
     * Example: "missing-hba1c-test", "uncontrolled-blood-pressure"
     */
    private String type;

    /**
     * Human-readable description of the gap
     */
    private String description;

    /**
     * Severity level: "critical", "high", "medium", "low"
     */
    private String severity;

    /**
     * Recommended action to close the gap
     */
    private String action;

    /**
     * Which measure component this gap relates to
     * Example: "HbA1c Testing", "Eye Exam"
     */
    private String measureComponent;
}
