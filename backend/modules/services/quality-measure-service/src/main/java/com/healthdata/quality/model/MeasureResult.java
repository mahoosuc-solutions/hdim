package com.healthdata.quality.model;

import lombok.Data;
import lombok.Builder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of a HEDIS measure calculation for a single patient.
 * Follows HEDIS specifications for measure reporting.
 */
@Data
@Builder
public class MeasureResult {

    /**
     * Measure identifier (e.g., "CDC", "CBP", "BCS")
     */
    private String measureId;

    /**
     * Full measure name (e.g., "Comprehensive Diabetes Care")
     */
    private String measureName;

    /**
     * Patient ID (FHIR Patient.id)
     */
    private String patientId;

    /**
     * Is patient eligible for this measure?
     * (e.g., has diabetes diagnosis for CDC measure)
     */
    @Builder.Default
    private boolean isEligible = false;

    /**
     * Is patient in the denominator?
     * (eligible and not excluded)
     */
    @Builder.Default
    private boolean denominatorMembership = false;

    /**
     * Is patient excluded from the denominator?
     * (e.g., hospice, palliative care)
     */
    @Builder.Default
    private boolean denominatorExclusion = false;

    /**
     * Reason for exclusion (if excluded)
     */
    private String exclusionReason;

    /**
     * Sub-measure results
     * Key: Sub-measure name (e.g., "HbA1c Testing", "Eye Exam")
     * Value: Sub-measure result
     */
    @Builder.Default
    private Map<String, SubMeasureResult> subMeasures = new HashMap<>();

    /**
     * Identified care gaps for this patient
     */
    @Builder.Default
    private List<CareGap> careGaps = new ArrayList<>();

    /**
     * Clinical recommendations based on measure results
     */
    @Builder.Default
    private List<Recommendation> recommendations = new ArrayList<>();

    /**
     * When this measure was calculated
     */
    @Builder.Default
    private Instant calculatedAt = Instant.now();
}
