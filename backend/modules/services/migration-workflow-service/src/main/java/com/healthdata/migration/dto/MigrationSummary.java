package com.healthdata.migration.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Final summary of a completed migration job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationSummary {

    private UUID jobId;
    private String jobName;
    private String tenantId;
    private JobStatus finalStatus;

    // Record counts
    private long totalRecords;
    private long successCount;
    private long failureCount;
    private long skippedCount;

    // Success/failure rates
    private double successRate;
    private double failureRate;

    // Timing
    private Instant startedAt;
    private Instant completedAt;
    private long totalDurationMs;
    private long avgProcessingTimeMs;
    private double recordsPerSecond;

    // FHIR resources created (by type)
    private Map<String, Long> fhirResourcesCreated;

    // Error summary (by category)
    private Map<MigrationErrorCategory, Long> errorsByCategory;

    // Checkpoints
    private int checkpointsSaved;
    private int retriesPerformed;

    /**
     * Format duration as human-readable string
     */
    public String getFormattedDuration() {
        if (totalDurationMs <= 0) {
            return "N/A";
        }

        Duration duration = Duration.ofMillis(totalDurationMs);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return String.format("%d hr %d min %d sec", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d min %d sec", minutes, seconds);
        } else {
            return String.format("%d sec", seconds);
        }
    }

    /**
     * Get total FHIR resources created
     */
    public long getTotalFhirResourcesCreated() {
        if (fhirResourcesCreated == null) {
            return 0;
        }
        return fhirResourcesCreated.values().stream().mapToLong(Long::longValue).sum();
    }
}
