package com.healthdata.patientevent.event;

import java.time.Instant;

/**
 * PatientActivatedEvent - Fired when a patient is activated
 *
 * Immutable domain event capturing patient activation.
 */
public class PatientActivatedEvent {
    private final String patientId;
    private final String tenantId;
    private final String reason;
    private final Instant timestamp;

    public PatientActivatedEvent(String patientId, String tenantId, String reason) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
}
