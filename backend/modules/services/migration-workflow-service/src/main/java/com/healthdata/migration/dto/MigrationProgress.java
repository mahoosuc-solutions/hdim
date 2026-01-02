package com.healthdata.migration.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Real-time migration progress update
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationProgress {

    private UUID jobId;
    private JobStatus status;

    // Counts
    private long totalRecords;
    private long processedCount;
    private long successCount;
    private long failureCount;
    private long skippedCount;

    // Performance metrics
    private double recordsPerSecond;
    private long avgProcessingTimeMs;
    private long estimatedTimeRemainingMs;

    // Current position (for file-based sources)
    private String currentFile;
    private long currentOffset;
    private int currentBatch;

    // Timestamp
    private Instant timestamp;

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
     * Format estimated time remaining as human-readable string
     */
    public String getFormattedTimeRemaining() {
        if (estimatedTimeRemainingMs <= 0) {
            return "Calculating...";
        }

        long seconds = estimatedTimeRemainingMs / 1000;
        if (seconds < 60) {
            return seconds + " seconds";
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes < 60) {
            return String.format("%d min %d sec", minutes, seconds);
        }

        long hours = minutes / 60;
        minutes = minutes % 60;
        return String.format("%d hr %d min", hours, minutes);
    }

    /**
     * Create a progress update from job entity
     */
    public static MigrationProgress from(UUID jobId, JobStatus status, long total,
                                         long processed, long success, long failure, long skipped) {
        return MigrationProgress.builder()
                .jobId(jobId)
                .status(status)
                .totalRecords(total)
                .processedCount(processed)
                .successCount(success)
                .failureCount(failure)
                .skippedCount(skipped)
                .timestamp(Instant.now())
                .build();
    }
}
