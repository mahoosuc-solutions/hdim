package com.healthdata.cql.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a CQL evaluation completes successfully.
 * Contains evaluation results and performance metrics.
 */
public class EvaluationCompletedEvent implements EvaluationEvent {

    private final String eventId;
    private final UUID evaluationId;
    private final String tenantId;
    private final String measureId;
    private final String measureName;
    private final UUID patientId;
    private final Instant timestamp;
    private final String batchId;

    // Evaluation results
    private final boolean inDenominator;
    private final boolean inNumerator;
    private final String exclusionReason;
    private final double complianceRate;
    private final double score;
    private final long durationMs;

    // Additional context
    private final Map<String, Object> evidence;  // Flexible data for visualization
    private final int careGapCount;

    @JsonCreator
    public EvaluationCompletedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("evaluationId") UUID evaluationId,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("measureId") String measureId,
            @JsonProperty("measureName") String measureName,
            @JsonProperty("patientId") UUID patientId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("batchId") String batchId,
            @JsonProperty("inDenominator") boolean inDenominator,
            @JsonProperty("inNumerator") boolean inNumerator,
            @JsonProperty("exclusionReason") String exclusionReason,
            @JsonProperty("complianceRate") double complianceRate,
            @JsonProperty("score") double score,
            @JsonProperty("durationMs") long durationMs,
            @JsonProperty("evidence") Map<String, Object> evidence,
            @JsonProperty("careGapCount") int careGapCount) {
        this.eventId = eventId;
        this.evaluationId = evaluationId;
        this.tenantId = tenantId;
        this.measureId = measureId;
        this.measureName = measureName;
        this.patientId = patientId;
        this.timestamp = timestamp;
        this.batchId = batchId;
        this.inDenominator = inDenominator;
        this.inNumerator = inNumerator;
        this.exclusionReason = exclusionReason;
        this.complianceRate = complianceRate;
        this.score = score;
        this.durationMs = durationMs;
        this.evidence = evidence;
        this.careGapCount = careGapCount;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public EventType getEventType() {
        return EventType.EVALUATION_COMPLETED;
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
        return evaluationId;
    }

    public String getMeasureId() {
        return measureId;
    }

    public String getMeasureName() {
        return measureName;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public String getBatchId() {
        return batchId;
    }

    public boolean isInDenominator() {
        return inDenominator;
    }

    public boolean isInNumerator() {
        return inNumerator;
    }

    public String getExclusionReason() {
        return exclusionReason;
    }

    public double getComplianceRate() {
        return complianceRate;
    }

    public double getScore() {
        return score;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }

    public int getCareGapCount() {
        return careGapCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId = UUID.randomUUID().toString();
        private UUID evaluationId;
        private String tenantId;
        private String measureId;
        private String measureName;
        private UUID patientId;
        private Instant timestamp = Instant.now();
        private String batchId;
        private boolean inDenominator;
        private boolean inNumerator;
        private String exclusionReason;
        private double complianceRate;
        private double score;
        private long durationMs;
        private Map<String, Object> evidence;
        private int careGapCount;

        public Builder evaluationId(UUID evaluationId) {
            this.evaluationId = evaluationId;
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

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder batchId(String batchId) {
            this.batchId = batchId;
            return this;
        }

        public Builder inDenominator(boolean inDenominator) {
            this.inDenominator = inDenominator;
            return this;
        }

        public Builder inNumerator(boolean inNumerator) {
            this.inNumerator = inNumerator;
            return this;
        }

        public Builder exclusionReason(String exclusionReason) {
            this.exclusionReason = exclusionReason;
            return this;
        }

        public Builder complianceRate(double complianceRate) {
            this.complianceRate = complianceRate;
            return this;
        }

        public Builder score(double score) {
            this.score = score;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder evidence(Map<String, Object> evidence) {
            this.evidence = evidence;
            return this;
        }

        public Builder careGapCount(int careGapCount) {
            this.careGapCount = careGapCount;
            return this;
        }

        public EvaluationCompletedEvent build() {
            return new EvaluationCompletedEvent(
                    eventId, evaluationId, tenantId, measureId, measureName,
                    patientId, timestamp, batchId, inDenominator, inNumerator,
                    exclusionReason, complianceRate, score, durationMs, evidence, careGapCount);
        }
    }
}
