package com.healthdata.caregap.event;

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
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.gapCode = gapCode;
        this.gapDescription = gapDescription;
        this.severity = severity;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getGapCode() { return gapCode; }
    public String getGapDescription() { return gapDescription; }
    public String getSeverity() { return severity; }
    public Instant getTimestamp() { return timestamp; }
}
