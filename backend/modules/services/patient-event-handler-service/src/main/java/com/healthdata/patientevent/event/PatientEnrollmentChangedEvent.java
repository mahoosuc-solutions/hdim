package com.healthdata.patientevent.event;

import java.time.Instant;

/**
 * PatientEnrollmentChangedEvent - Fired when a patient's enrollment status changes
 *
 * Immutable domain event capturing enrollment changes.
 */
public class PatientEnrollmentChangedEvent {
    private final String patientId;
    private final String tenantId;
    private final String newStatus;
    private final String reason;
    private final Instant timestamp;

    public PatientEnrollmentChangedEvent(String patientId, String tenantId, String newStatus, String reason) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.newStatus = newStatus;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public String getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
}
