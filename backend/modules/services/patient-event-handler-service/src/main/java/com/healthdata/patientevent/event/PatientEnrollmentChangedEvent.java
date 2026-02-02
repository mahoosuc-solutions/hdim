package com.healthdata.patientevent.event;

import com.healthdata.eventsourcing.event.EventUserContext;
import java.time.Instant;

/**
 * PatientEnrollmentChangedEvent - Fired when a patient's enrollment status changes
 *
 * Immutable domain event capturing enrollment changes.
 *
 * HIPAA Compliance:
 * - 45 CFR 164.312(b): Audit controls - includes user context for audit trail
 * - 45 CFR 164.312(d): Person or entity authentication - tracks who made the change
 */
public class PatientEnrollmentChangedEvent {
    private final String patientId;
    private final String tenantId;
    private final String newStatus;
    private final String reason;
    private final Instant timestamp;
    private final EventUserContext userContext;

    public PatientEnrollmentChangedEvent(String patientId, String tenantId, String newStatus, String reason) {
        this(patientId, tenantId, newStatus, reason, null);
    }

    public PatientEnrollmentChangedEvent(String patientId, String tenantId, String newStatus, String reason, EventUserContext userContext) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.newStatus = newStatus;
        this.reason = reason;
        this.timestamp = Instant.now();
        this.userContext = userContext;
    }

    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public String getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
    public EventUserContext getUserContext() { return userContext; }
}
