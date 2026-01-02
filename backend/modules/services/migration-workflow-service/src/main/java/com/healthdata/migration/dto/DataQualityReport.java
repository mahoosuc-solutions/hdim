package com.healthdata.migration.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data quality report for a migration job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityReport {

    private UUID jobId;
    private String jobName;
    private Instant generatedAt;

    // Summary statistics
    private QualitySummary summary;

    // Error breakdown by category
    private Map<MigrationErrorCategory, Long> errorsByCategory;

    // FHIR resources created by type
    private Map<String, Long> fhirResourcesCreated;

    // Data quality issues detected
    private List<DataQualityIssue> dataQualityIssues;

    // Top errors (for quick review)
    private List<ErrorSample> topErrors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualitySummary {
        private long totalRecords;
        private long successCount;
        private long failureCount;
        private long skippedCount;
        private double successRate;
        private double failureRate;
        private long processingTimeMs;
        private double avgProcessingTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataQualityIssue {
        private String field;
        private String issue;
        private long count;
        private Severity severity;
        private String recommendation;

        public enum Severity {
            INFO, WARNING, ERROR, CRITICAL
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorSample {
        private MigrationErrorCategory category;
        private String errorMessage;
        private long count;
        private String sampleRecordId;
        private String sampleSourceFile;
    }

    /**
     * Calculate overall quality score (0-100)
     */
    public double getQualityScore() {
        if (summary == null || summary.getTotalRecords() == 0) {
            return 0.0;
        }

        // Base score from success rate
        double score = summary.getSuccessRate();

        // Penalty for critical issues
        if (dataQualityIssues != null) {
            long criticalCount = dataQualityIssues.stream()
                    .filter(i -> i.getSeverity() == DataQualityIssue.Severity.CRITICAL)
                    .count();
            score -= criticalCount * 5; // 5 points penalty per critical issue
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Get quality grade (A, B, C, D, F)
     */
    public String getQualityGrade() {
        double score = getQualityScore();
        if (score >= 95) return "A";
        if (score >= 85) return "B";
        if (score >= 75) return "C";
        if (score >= 65) return "D";
        return "F";
    }
}
