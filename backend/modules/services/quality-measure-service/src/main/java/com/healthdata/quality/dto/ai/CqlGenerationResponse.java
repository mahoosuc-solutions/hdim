package com.healthdata.quality.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for AI-assisted CQL generation.
 *
 * Issue #150: Implement AI-Assisted CQL Generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CqlGenerationResponse {

    /**
     * Unique identifier for this generation request.
     */
    private String id;

    /**
     * Generated CQL code.
     */
    private String generatedCql;

    /**
     * Plain English explanation of what the CQL does.
     */
    private String explanation;

    /**
     * AI model's confidence in the generation (0.0 - 1.0).
     */
    private double confidence;

    /**
     * Validation status of the generated CQL.
     */
    private ValidationStatus validationStatus;

    /**
     * Detailed validation results.
     */
    private ValidationResult validationResult;

    /**
     * Test execution results (if tests were requested).
     */
    private TestResults testResults;

    /**
     * Warnings or suggestions for improvement.
     */
    private List<String> warnings;

    /**
     * Suggested improvements to the CQL.
     */
    private List<Suggestion> suggestions;

    /**
     * Metadata about the generation.
     */
    private GenerationMetadata metadata;

    /**
     * Timestamp of generation.
     */
    private LocalDateTime generatedAt;

    /**
     * Model version used for generation.
     */
    private String modelVersion;

    /**
     * Validation status enum.
     */
    public enum ValidationStatus {
        VALID,
        INVALID,
        WARNINGS,
        NOT_VALIDATED,
        ERROR
    }

    /**
     * Detailed validation results.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResult {
        private boolean syntaxValid;
        private boolean semanticValid;
        private List<ValidationError> errors;
        private List<String> warnings;
        private int errorCount;
        private int warningCount;
    }

    /**
     * Validation error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String severity; // ERROR, WARNING
        private String message;
        private int line;
        private int column;
        private String code;
    }

    /**
     * Test execution results.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResults {
        private boolean executed;
        private int sampleSize;
        private int numerator;
        private int denominator;
        private double complianceRate;
        private List<PatientTestResult> patientResults;
        private String executionError;
    }

    /**
     * Individual patient test result.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientTestResult {
        private String patientId;
        private boolean inDenominator;
        private boolean inNumerator;
        private String reason;
    }

    /**
     * Suggestion for CQL improvement.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String type; // OPTIMIZATION, CLARITY, STANDARD_COMPLIANCE
        private String description;
        private String suggestedCode;
        private double impact; // 0.0 - 1.0
    }

    /**
     * Generation metadata.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationMetadata {
        private int inputTokens;
        private int outputTokens;
        private long generationTimeMs;
        private String promptTemplate;
        private Map<String, Object> additionalInfo;
    }
}
