package com.healthdata.patientevent.projection;

import java.time.Instant;

/**
 * PatientActiveProjection - Denormalized read model for patient data
 *
 * Built from PatientCreatedEvent and related events.
 * Optimized for fast queries (patient search, status lookups).
 * Version tracked for consistency checks.
 */
public class PatientActiveProjection {
    private final String patientId;
    private final String tenantId;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String status;  // ACTIVE, INACTIVE
    private String enrollmentStatus;  // ACTIVE, INACTIVE, SUSPENDED, etc.
    private String enrollmentReason;
    private long version;
    private Instant lastUpdated;

    public PatientActiveProjection(String patientId, String tenantId, String firstName, String lastName, String status) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.enrollmentStatus = "ACTIVE";
        this.enrollmentReason = "";
        this.version = 1L;
        this.lastUpdated = Instant.now();
    }

    // Getters
    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getStatus() { return status; }
    public String getEnrollmentStatus() { return enrollmentStatus; }
    public String getEnrollmentReason() { return enrollmentReason; }
    public long getVersion() { return version; }
    public Instant getLastUpdated() { return lastUpdated; }

    // Setters
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setStatus(String status) { this.status = status; }
    public void setEnrollmentStatus(String enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }
    public void setEnrollmentReason(String enrollmentReason) { this.enrollmentReason = enrollmentReason; }
    public void setVersion(long version) { this.version = version; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public void incrementVersion() {
        this.version++;
        this.lastUpdated = Instant.now();
    }
}
