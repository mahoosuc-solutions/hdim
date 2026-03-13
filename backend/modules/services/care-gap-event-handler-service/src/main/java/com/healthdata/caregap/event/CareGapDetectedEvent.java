package com.healthdata.caregap.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * CareGapDetectedEvent - Care gap identification for patient
 *
 * Fired when a quality care gap is detected for a patient.
 * Immutable and idempotent - safe to replay.
 */
public class CareGapDetectedEvent {
    private final String tenantId;
    private final String patientId;
    private final String gapCode;
    private final String gapDescription;
    private final String severity;  // CRITICAL, HIGH, MEDIUM, LOW
    private final Instant timestamp;

    public CareGapDetectedEvent(String tenantId, String patientId, String gapCode, String gapDescription, String severity) {
        this(tenantId, patientId, gapCode, gapDescription, severity, Instant.now());
    }

    @JsonCreator
    public CareGapDetectedEvent(
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("patientId") String patientId,
        @JsonProperty("gapCode") String gapCode,
        @JsonProperty("gapDescription") String gapDescription,
        @JsonProperty("severity") String severity,
        @JsonProperty("timestamp") Instant timestamp
    ) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.gapCode = gapCode;
        this.gapDescription = gapDescription;
        this.severity = severity;
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getGapCode() { return gapCode; }
    public String getGapDescription() { return gapDescription; }
    public String getSeverity() { return severity; }
    public Instant getTimestamp() { return timestamp; }
}
