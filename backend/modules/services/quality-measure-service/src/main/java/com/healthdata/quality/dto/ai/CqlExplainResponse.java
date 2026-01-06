package com.healthdata.quality.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for AI-assisted CQL explanation.
 *
 * Issue #150: Implement AI-Assisted CQL Generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CqlExplainResponse {

    /**
     * Unique identifier for this explanation request.
     */
    private String id;

    /**
     * Plain English summary of what the CQL does.
     */
    private String summary;

    /**
     * Detailed explanation by section.
     */
    private List<SectionExplanation> sections;

    /**
     * Key clinical concepts used in the CQL.
     */
    private List<ClinicalConcept> clinicalConcepts;

    /**
     * Data elements accessed by the CQL.
     */
    private List<DataElement> dataElements;

    /**
     * Potential issues or concerns with the CQL.
     */
    private List<String> potentialIssues;

    /**
     * Suggestions for improvement.
     */
    private List<CqlGenerationResponse.Suggestion> suggestions;

    /**
     * Complexity rating (1-10).
     */
    private int complexityRating;

    /**
     * Estimated performance impact.
     */
    private PerformanceAssessment performanceAssessment;

    /**
     * Timestamp of explanation.
     */
    private LocalDateTime explainedAt;

    /**
     * Section-by-section explanation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionExplanation {
        private String sectionName;
        private String cqlSnippet;
        private String explanation;
        private String purpose;
        private int lineStart;
        private int lineEnd;
    }

    /**
     * Clinical concept referenced in CQL.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClinicalConcept {
        private String name;
        private String description;
        private String codeSystem;
        private String valueSetOid;
        private String usage; // How it's used in the CQL
    }

    /**
     * Data element accessed by CQL.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataElement {
        private String resourceType; // Patient, Condition, Observation, etc.
        private String element;
        private String purpose;
        private boolean required;
    }

    /**
     * Performance assessment for the CQL.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceAssessment {
        private String rating; // LOW, MEDIUM, HIGH
        private List<String> concerns;
        private List<String> recommendations;
        private int estimatedDataQueries;
    }
}
