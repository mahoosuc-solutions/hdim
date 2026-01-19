package com.healthdata.patientevent.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PatientActiveProjection - Denormalized read model for patient data
 *
 * Built from PatientCreatedEvent and related events.
 * Optimized for fast queries (patient search, status lookups).
 * Version tracked for consistency checks.
 *
 * ★ Insight ─────────────────────────────────────
 * - Multi-identifier support: identifiers[] tracks MRN, SSN, Enterprise IDs
 * - FHIR integration: fhirResourceId links to FHIR Patient resource
 * - Merge chain tracking: mergedIntoPatientId enables following merge chains
 * - Identity status: tracks ACTIVE, MERGED, DEPRECATED states
 * - JSON type for identifiers: denormalized for fast search/sort
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "patient_projections")
public class PatientActiveProjection {
    @Id
    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "status")
    private String status;  // ACTIVE, INACTIVE

    @Column(name = "enrollment_status")
    private String enrollmentStatus;  // ACTIVE, INACTIVE, SUSPENDED, etc.

    @Column(name = "enrollment_reason")
    private String enrollmentReason;

    @Column(name = "version")
    private long version;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /**
     * FHIR Patient Resource ID (UUID from fhir-service)
     * Links event-sourced patient to FHIR resource for interoperability
     */
    @Column(name = "fhir_resource_id")
    private UUID fhirResourceId;

    /**
     * Patient identifiers in JSON format
     * Stores list of FHIR-compliant identifiers (MRN, SSN, Enterprise ID, etc.)
     * Enables fast filtering and search by identifier type/value
     */
    @Column(name = "identifiers", columnDefinition = "TEXT")
    private String identifiers;  // JSON stored as TEXT for now

    /**
     * Identity status tracking
     * Values: ACTIVE (current patient), MERGED (incorporated into another), DEPRECATED
     * Used for filtering active patient records
     */
    @Column(name = "identity_status")
    private String identityStatus;  // ACTIVE, MERGED, DEPRECATED

    /**
     * If this patient was merged, the target patient ID
     * Enables "merge chain following" - find current patient after merge
     * Example: Patient A merged into B, then B merged into C
     *          mergedIntoPatientId chain: A→B→C
     */
    @Column(name = "merged_into_patient_id")
    private String mergedIntoPatientId;

    /**
     * Timestamp when patient was merged (if applicable)
     */
    @Column(name = "merged_at")
    private Instant mergedAt;

    // No-arg constructor for JPA
    public PatientActiveProjection() {
    }

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
    public UUID getFhirResourceId() { return fhirResourceId; }
    public String getIdentifiers() { return identifiers; }
    public String getIdentityStatus() { return identityStatus; }
    public String getMergedIntoPatientId() { return mergedIntoPatientId; }
    public Instant getMergedAt() { return mergedAt; }

    // Setters
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setStatus(String status) { this.status = status; }
    public void setEnrollmentStatus(String enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }
    public void setEnrollmentReason(String enrollmentReason) { this.enrollmentReason = enrollmentReason; }
    public void setVersion(long version) { this.version = version; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setFhirResourceId(UUID fhirResourceId) { this.fhirResourceId = fhirResourceId; }
    public void setIdentifiers(String identifiers) { this.identifiers = identifiers; }
    public void setIdentityStatus(String identityStatus) { this.identityStatus = identityStatus; }
    public void setMergedIntoPatientId(String mergedIntoPatientId) { this.mergedIntoPatientId = mergedIntoPatientId; }
    public void setMergedAt(Instant mergedAt) { this.mergedAt = mergedAt; }

    public void incrementVersion() {
        this.version++;
        this.lastUpdated = Instant.now();
    }

    /**
     * Mark patient as merged into another patient
     * Used when processing PatientMergedEvent
     * @param targetPatientId The patient this was merged into
     * @param mergedAtTime Timestamp of merge
     */
    public void markAsMerged(String targetPatientId, Instant mergedAtTime) {
        this.identityStatus = "MERGED";
        this.mergedIntoPatientId = targetPatientId;
        this.mergedAt = mergedAtTime;
        incrementVersion();
    }

    /**
     * Check if this is an active patient record
     * @return true if ACTIVE, false if MERGED or DEPRECATED
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.identityStatus);
    }
}
