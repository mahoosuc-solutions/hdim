package com.healthdata.patientevent.event;

import java.time.Instant;

/**
 * PatientCreatedEvent - Fired when a new patient is created in the system
 *
 * Immutable domain event capturing patient creation details.
 */
public class PatientCreatedEvent {
    private final String patientId;
    private final String tenantId;
    private final String firstName;
    private final String lastName;
    private final String dateOfBirth;
    private final Instant timestamp;

    public PatientCreatedEvent(String patientId, String tenantId, String firstName, String lastName, String dateOfBirth) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.timestamp = Instant.now();
    }

    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDateOfBirth() { return dateOfBirth; }
    public Instant getTimestamp() { return timestamp; }
}
