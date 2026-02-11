package com.healthdata.patientevent.event;

import com.healthdata.eventsourcing.event.EventUserContext;
import java.time.Instant;

/**
 * PatientActivatedEvent - Fired when a patient is activated
 *
 * Immutable domain event capturing patient activation.
 *
 * HIPAA Compliance:
 * - 45 CFR 164.312(b): Audit controls - includes user context for audit trail
 * - 45 CFR 164.312(d): Person or entity authentication - tracks who made the change
 */
public class PatientActivatedEvent {
    private final String patientId;
    private final String tenantId;
    private final String reason;
    private final Instant timestamp;
    private final EventUserContext userContext;

    public PatientActivatedEvent(String patientId, String tenantId, String reason) {
        this(patientId, tenantId, reason, null);
    }

    public PatientActivatedEvent(String patientId, String tenantId, String reason, EventUserContext userContext) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.reason = reason;
        this.timestamp = Instant.now();
        this.userContext = userContext;
    }

    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
    public EventUserContext getUserContext() { return userContext; }
}
