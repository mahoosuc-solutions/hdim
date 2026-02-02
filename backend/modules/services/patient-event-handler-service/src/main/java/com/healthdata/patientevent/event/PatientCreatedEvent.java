package com.healthdata.patientevent.event;

import com.healthdata.eventsourcing.event.EventUserContext;
import java.time.Instant;

/**
 * PatientCreatedEvent - Fired when a new patient is created in the system
 *
 * Immutable domain event capturing patient creation details.
 *
 * HIPAA Compliance:
 * - 45 CFR 164.312(b): Audit controls - includes user context for audit trail
 * - 45 CFR 164.312(d): Person or entity authentication - tracks who created the patient
 */
public class PatientCreatedEvent {
    private final String patientId;
    private final String tenantId;
    private final String firstName;
    private final String lastName;
    private final String dateOfBirth;
    private final Instant timestamp;
    private final EventUserContext userContext;

    public PatientCreatedEvent(String patientId, String tenantId, String firstName, String lastName, String dateOfBirth) {
        this(patientId, tenantId, firstName, lastName, dateOfBirth, null);
    }

    public PatientCreatedEvent(String patientId, String tenantId, String firstName, String lastName, String dateOfBirth, EventUserContext userContext) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.timestamp = Instant.now();
        this.userContext = userContext;
    }

    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDateOfBirth() { return dateOfBirth; }
    public Instant getTimestamp() { return timestamp; }
    public EventUserContext getUserContext() { return userContext; }
}
