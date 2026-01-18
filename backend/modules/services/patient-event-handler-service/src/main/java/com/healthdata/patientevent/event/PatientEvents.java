package com.healthdata.patientevent.event;

import java.time.Instant;

/**
 * Patient Lifecycle Events for Event Sourcing
 *
 * These immutable domain events represent state changes in the patient lifecycle.
 * Each event is append-only and never modified.
 */

// ===== PatientCreatedEvent =====
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

// ===== PatientEnrollmentChangedEvent =====
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

// ===== PatientDemographicsUpdatedEvent =====
public class PatientDemographicsUpdatedEvent {
    private final String patientId;
    private final String tenantId;
    private final String firstName;
    private final String lastName;
    private final String dateOfBirth;
    private final Instant timestamp;

    public PatientDemographicsUpdatedEvent(String patientId, String tenantId, String firstName, String lastName, String dateOfBirth) {
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

// ===== PatientDeactivatedEvent =====
public class PatientDeactivatedEvent {
    private final String patientId;
    private final String tenantId;
    private final String reason;
    private final Instant timestamp;

    public PatientDeactivatedEvent(String patientId, String tenantId, String reason) {
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

// ===== PatientActivatedEvent =====
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
