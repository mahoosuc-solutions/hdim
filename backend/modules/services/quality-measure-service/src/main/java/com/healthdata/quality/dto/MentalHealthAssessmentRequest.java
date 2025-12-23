package com.healthdata.quality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for submitting a mental health assessment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentalHealthAssessmentRequest {

    /**
     * Patient FHIR ID
     */
    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    /**
     * Assessment type (phq-9, gad-7, phq-2, etc.)
     */
    @NotBlank(message = "Assessment type is required")
    private String assessmentType;

    /**
     * Question responses as key-value pairs
     * Keys: "q1", "q2", etc.
     * Values: 0-3 for most assessments
     */
    @NotEmpty(message = "Responses are required")
    private Map<String, Integer> responses;

    /**
     * Provider who administered the assessment
     */
    @NotBlank(message = "Assessed by is required")
    private String assessedBy;

    /**
     * Date/time the assessment was performed
     * Defaults to current time if not provided
     */
    private Instant assessmentDate;

    /**
     * Optional clinical notes
     */
    private String clinicalNotes;
}
