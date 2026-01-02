package com.healthdata.cql.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published periodically during batch evaluation.
 * Emitted every 10 patients or every 5 seconds (whichever comes first).
 * This is the primary event for real-time visualization of batch progress.
 */
public class BatchProgressEvent implements EvaluationEvent {

    private final String eventId;
    private final String batchId;
    private final String tenantId;
    private final String measureId;
    private final String measureName;
    private final Instant timestamp;

    // Progress metrics
    private final int totalPatients;
    private final int completedCount;
    private final int successCount;
    private final int failedCount;
    private final int pendingCount;

    // Performance metrics
    private final double avgDurationMs;
    private final double currentThroughput;  // evaluations per second
    private final long elapsedTimeMs;
    private final long estimatedTimeRemainingMs;

    // Quality metrics
    private final int denominatorCount;
    private final int numeratorCount;
    private final double cumulativeComplianceRate;

    @JsonCreator
    public BatchProgressEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("batchId") String batchId,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("measureId") String measureId,
            @JsonProperty("measureName") String measureName,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("totalPatients") int totalPatients,
            @JsonProperty("completedCount") int completedCount,
            @JsonProperty("successCount") int successCount,
            @JsonProperty("failedCount") int failedCount,
            @JsonProperty("pendingCount") int pendingCount,
            @JsonProperty("avgDurationMs") double avgDurationMs,
            @JsonProperty("currentThroughput") double currentThroughput,
            @JsonProperty("elapsedTimeMs") long elapsedTimeMs,
            @JsonProperty("estimatedTimeRemainingMs") long estimatedTimeRemainingMs,
            @JsonProperty("denominatorCount") int denominatorCount,
            @JsonProperty("numeratorCount") int numeratorCount,
            @JsonProperty("cumulativeComplianceRate") double cumulativeComplianceRate) {
        this.eventId = eventId;
        this.batchId = batchId;
        this.tenantId = tenantId;
        this.measureId = measureId;
        this.measureName = measureName;
        this.timestamp = timestamp;
        this.totalPatients = totalPatients;
        this.completedCount = completedCount;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.pendingCount = pendingCount;
        this.avgDurationMs = avgDurationMs;
        this.currentThroughput = currentThroughput;
        this.elapsedTimeMs = elapsedTimeMs;
        this.estimatedTimeRemainingMs = estimatedTimeRemainingMs;
        this.denominatorCount = denominatorCount;
        this.numeratorCount = numeratorCount;
        this.cumulativeComplianceRate = cumulativeComplianceRate;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public EventType getEventType() {
        return EventType.BATCH_PROGRESS;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public UUID getEvaluationId() {
        return null;  // Batch-level event
    }

    public String getBatchId() {
        return batchId;
    }

    public String getMeasureId() {
        return measureId;
    }

    public String getMeasureName() {
        return measureName;
    }

    public int getTotalPatients() {
        return totalPatients;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public double getAvgDurationMs() {
        return avgDurationMs;
    }

    public double getCurrentThroughput() {
        return currentThroughput;
    }

    public long getElapsedTimeMs() {
        return elapsedTimeMs;
    }

    public long getEstimatedTimeRemainingMs() {
        return estimatedTimeRemainingMs;
    }

    public int getDenominatorCount() {
        return denominatorCount;
    }

    public int getNumeratorCount() {
        return numeratorCount;
    }

    public double getCumulativeComplianceRate() {
        return cumulativeComplianceRate;
    }

    public double getPercentComplete() {
        return totalPatients > 0 ? (completedCount * 100.0 / totalPatients) : 0.0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId = UUID.randomUUID().toString();
        private String batchId;
        private String tenantId;
        private String measureId;
        private String measureName;
        private Instant timestamp = Instant.now();
        private int totalPatients;
        private int completedCount;
        private int successCount;
        private int failedCount;
        private int pendingCount;
        private double avgDurationMs;
        private double currentThroughput;
        private long elapsedTimeMs;
        private long estimatedTimeRemainingMs;
        private int denominatorCount;
        private int numeratorCount;
        private double cumulativeComplianceRate;

        public Builder batchId(String batchId) {
            this.batchId = batchId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder measureId(String measureId) {
            this.measureId = measureId;
            return this;
        }

        public Builder measureName(String measureName) {
            this.measureName = measureName;
            return this;
        }

        public Builder totalPatients(int totalPatients) {
            this.totalPatients = totalPatients;
            return this;
        }

        public Builder completedCount(int completedCount) {
            this.completedCount = completedCount;
            return this;
        }

        public Builder successCount(int successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder failedCount(int failedCount) {
            this.failedCount = failedCount;
            return this;
        }

        public Builder pendingCount(int pendingCount) {
            this.pendingCount = pendingCount;
            return this;
        }

        public Builder avgDurationMs(double avgDurationMs) {
            this.avgDurationMs = avgDurationMs;
            return this;
        }

        public Builder currentThroughput(double currentThroughput) {
            this.currentThroughput = currentThroughput;
            return this;
        }

        public Builder elapsedTimeMs(long elapsedTimeMs) {
            this.elapsedTimeMs = elapsedTimeMs;
            return this;
        }

        public Builder estimatedTimeRemainingMs(long estimatedTimeRemainingMs) {
            this.estimatedTimeRemainingMs = estimatedTimeRemainingMs;
            return this;
        }

        public Builder denominatorCount(int denominatorCount) {
            this.denominatorCount = denominatorCount;
            return this;
        }

        public Builder numeratorCount(int numeratorCount) {
            this.numeratorCount = numeratorCount;
            return this;
        }

        public Builder cumulativeComplianceRate(double cumulativeComplianceRate) {
            this.cumulativeComplianceRate = cumulativeComplianceRate;
            return this;
        }

        public BatchProgressEvent build() {
            return new BatchProgressEvent(
                    eventId, batchId, tenantId, measureId, measureName, timestamp,
                    totalPatients, completedCount, successCount, failedCount, pendingCount,
                    avgDurationMs, currentThroughput, elapsedTimeMs, estimatedTimeRemainingMs,
                    denominatorCount, numeratorCount, cumulativeComplianceRate);
        }
    }
}
