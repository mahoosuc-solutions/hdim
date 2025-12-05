package com.healthdata.cql.measure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of a HEDIS measure evaluation
 *
 * Contains compliance status, scores, and detailed findings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = MeasureResult.MeasureResultBuilder.class)
public class MeasureResult {

    /**
     * Measure identifier (e.g., "CDC", "CBP")
     */
    private String measureId;

    /**
     * Measure name
     */
    private String measureName;

    /**
     * Patient ID
     */
    private String patientId;

    /**
     * Evaluation date
     */
    @Builder.Default
    private LocalDate evaluationDate = LocalDate.now();

    /**
     * Is patient in denominator (eligible for measure)?
     */
    private boolean inDenominator;

    /**
     * Is patient in numerator (compliant with measure)?
     */
    private boolean inNumerator;

    /**
     * Exclusion reason if patient is excluded from measure
     */
    private String exclusionReason;

    /**
     * Compliance rate (0.0 to 1.0)
     */
    private Double complianceRate;

    /**
     * Quality score (0-100)
     */
    private Double score;

    /**
     * Care gaps identified
     */
    @Builder.Default
    private List<CareGap> careGaps = new ArrayList<>();

    /**
     * Supporting evidence
     */
    @Builder.Default
    private Map<String, Object> evidence = new HashMap<>();

    /**
     * Detailed findings
     */
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    /**
     * Represents a care gap identified during evaluation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonDeserialize(builder = CareGap.CareGapBuilder.class)
    public static class CareGap {
        private String gapType;
        private String description;
        private String recommendedAction;
        private String priority; // high, medium, low
        private LocalDate dueDate;

        @JsonPOJOBuilder(withPrefix = "")
        public static class CareGapBuilder {
            // Lombok generates the builder methods
        }
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class MeasureResultBuilder {
        // Lombok generates the builder methods
    }
}
