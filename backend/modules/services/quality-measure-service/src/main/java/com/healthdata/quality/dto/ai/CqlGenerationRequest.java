package com.healthdata.quality.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for AI-assisted CQL generation.
 *
 * Issue #150: Implement AI-Assisted CQL Generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CqlGenerationRequest {

    /**
     * Natural language description of the measure criteria.
     * Example: "Patients aged 18-75 with type 2 diabetes who have not had an HbA1c test in the last 6 months"
     */
    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    /**
     * Type of measure being created.
     * PROCESS - Actions taken for patient care
     * OUTCOME - Results of care processes
     * STRUCTURE - Healthcare system attributes
     */
    @NotBlank(message = "Measure type is required")
    private String measureType;

    /**
     * Optional measure name for the library.
     */
    private String measureName;

    /**
     * Optional context to improve generation accuracy.
     */
    private GenerationContext context;

    /**
     * Whether to run validation on generated CQL.
     */
    @Builder.Default
    private boolean validateCql = true;

    /**
     * Whether to run test execution against sample patients.
     */
    @Builder.Default
    private boolean runTests = false;

    /**
     * Number of sample patients to test against.
     */
    @Builder.Default
    private int sampleSize = 10;

    /**
     * Context for CQL generation to improve accuracy.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationContext {
        /**
         * Existing conditions to consider.
         * Example: ["diabetes", "hypertension"]
         */
        private List<String> existingConditions;

        /**
         * Relevant value set OIDs.
         * Example: ["2.16.840.1.113883.3.464.1003.103.12.1001"]
         */
        private List<String> relevantValueSets;

        /**
         * Target population characteristics.
         */
        private PopulationCriteria population;

        /**
         * Additional metadata for the measure.
         */
        private Map<String, Object> metadata;
    }

    /**
     * Population criteria for the measure.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopulationCriteria {
        private Integer minAge;
        private Integer maxAge;
        private String gender; // "male", "female", "any"
        private List<String> includedConditions;
        private List<String> excludedConditions;
    }
}
