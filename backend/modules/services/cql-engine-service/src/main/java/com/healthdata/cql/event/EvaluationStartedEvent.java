package com.healthdata.cql.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a CQL evaluation starts.
 * Used for tracking evaluation lifecycle and performance monitoring.
 */
public class EvaluationStartedEvent implements EvaluationEvent {

    private final String eventId;
    private final UUID evaluationId;
    private final String tenantId;
    private final String measureId;
    private final String measureName;
    private final UUID patientId;
    private final Instant timestamp;
    private final String batchId;  // Null for individual evaluations

    @JsonCreator
    public EvaluationStartedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("evaluationId") UUID evaluationId,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("measureId") String measureId,
            @JsonProperty("measureName") String measureName,
            @JsonProperty("patientId") UUID patientId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("batchId") String batchId) {
        this.eventId = eventId;
        this.evaluationId = evaluationId;
        this.tenantId = tenantId;
        this.measureId = measureId;
        this.measureName = measureName;
        this.patientId = patientId;
        this.timestamp = timestamp;
        this.batchId = batchId;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public EventType getEventType() {
        return EventType.EVALUATION_STARTED;
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

        public EvaluationStartedEvent build() {
            return new EvaluationStartedEvent(
                    eventId, evaluationId, tenantId, measureId,
                    measureName, patientId, timestamp, batchId);
        }
    }
}
