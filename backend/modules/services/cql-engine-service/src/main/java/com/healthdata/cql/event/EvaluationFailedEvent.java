package com.healthdata.cql.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a CQL evaluation fails.
 * Used for error monitoring and alerting.
 */
public class EvaluationFailedEvent implements EvaluationEvent {

    private final String eventId;
    private final UUID evaluationId;
    private final String tenantId;
    private final String measureId;
    private final String measureName;
    private final String patientId;
    private final Instant timestamp;
    private final String batchId;
    private final String errorMessage;
    private final String errorType;  // FHIR_FETCH_ERROR, CQL_PARSE_ERROR, RUNTIME_ERROR, etc.
    private final long durationMs;

    @JsonCreator
    public EvaluationFailedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("evaluationId") UUID evaluationId,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("measureId") String measureId,
            @JsonProperty("measureName") String measureName,
            @JsonProperty("patientId") String patientId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("batchId") String batchId,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("errorType") String errorType,
            @JsonProperty("durationMs") long durationMs) {
        this.eventId = eventId;
        this.evaluationId = evaluationId;
        this.tenantId = tenantId;
        this.measureId = measureId;
        this.measureName = measureName;
        this.patientId = patientId;
        this.timestamp = timestamp;
        this.batchId = batchId;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.durationMs = durationMs;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public EventType getEventType() {
        return EventType.EVALUATION_FAILED;
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

    public String getPatientId() {
        return patientId;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public long getDurationMs() {
        return durationMs;
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
        private String patientId;
        private Instant timestamp = Instant.now();
        private String batchId;
        private String errorMessage;
        private String errorType;
        private long durationMs;

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

        public Builder patientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder batchId(String batchId) {
            this.batchId = batchId;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder errorType(String errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public EvaluationFailedEvent build() {
            return new EvaluationFailedEvent(
                    eventId, evaluationId, tenantId, measureId, measureName,
                    patientId, timestamp, batchId, errorMessage, errorType, durationMs);
        }
    }
}
