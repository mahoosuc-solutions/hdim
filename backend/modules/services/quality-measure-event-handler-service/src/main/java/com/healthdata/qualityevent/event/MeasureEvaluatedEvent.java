package com.healthdata.qualityevent.event;

import java.time.Instant;

/**
 * MeasureEvaluatedEvent - Quality measure evaluation initiated
 *
 * Fired when a quality measure is evaluated for a patient.
 * Immutable and idempotent - safe to replay.
 */
public class MeasureEvaluatedEvent {
    private final String tenantId;
    private final String patientId;
    private final String measureCode;
    private final String measureDescription;
    private final Instant timestamp;

    public MeasureEvaluatedEvent(String tenantId, String patientId, String measureCode, String measureDescription) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.measureCode = measureCode;
        this.measureDescription = measureDescription;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getMeasureCode() { return measureCode; }
    public String getMeasureDescription() { return measureDescription; }
    public Instant getTimestamp() { return timestamp; }
}
