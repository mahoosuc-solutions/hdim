package com.healthdata.migration.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing migration job details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationJobResponse {

    private UUID id;
    private String tenantId;
    private String jobName;
    private String description;

    private SourceType sourceType;
    private SourceConfig sourceConfig;
    private DataType dataType;

    private boolean convertToFhir;
    private boolean continueOnError;
    private int batchSize;
    private boolean resumable;

    private JobStatus status;

    // Progress metrics
    private long totalRecords;
    private long processedCount;
    private long successCount;
    private long failureCount;
    private long skippedCount;

    // Timing
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant lastCheckpointAt;

    // Retry info
    private int retryCount;
    private int maxRetries;
    private Instant nextRetryAt;

    // Links
    private String progressUrl;
    private String errorsUrl;
    private String qualityReportUrl;

    /**
     * Calculate completion percentage
     */
    public double getCompletionPercentage() {
        if (totalRecords == 0) {
            return 0.0;
        }
        return (double) processedCount / totalRecords * 100.0;
    }

    /**
     * Calculate success rate
     */
    public double getSuccessRate() {
        if (processedCount == 0) {
            return 0.0;
        }
        return (double) successCount / processedCount * 100.0;
    }
}
