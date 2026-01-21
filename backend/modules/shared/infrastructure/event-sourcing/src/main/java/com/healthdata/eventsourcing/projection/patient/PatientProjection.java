package com.healthdata.eventsourcing.projection.patient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Patient Projection - CQRS Read Model
 *
 * Denormalized view of patient data materialized from PatientCreatedEvent.
 * Optimized for query performance with indexes on common search patterns.
 *
 * Schema:
 * - id: UUID primary key (auto-generated)
 * - patient_id: Aggregate root ID from event
 * - tenant_id: Multi-tenant isolation (indexed)
 * - mrn: Medical Record Number (indexed)
 * - first_name: First name with prefix search support
 * - last_name: Last name
 * - date_of_birth: DOB for age calculations
 * - gender: FHIR gender code (MALE, FEMALE, OTHER, UNKNOWN)
 * - insurance_member_id: Member ID from insurance system
 * - created_at: Projection creation timestamp
 * - updated_at: Last update timestamp
 */
@Entity
@Table(
    name = "patient_projections",
    indexes = {
        @Index(name = "idx_patient_projections_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_patient_projections_tenant_mrn", columnList = "tenant_id, mrn", unique = true),
        @Index(name = "idx_patient_projections_first_name", columnList = "first_name"),
        @Index(name = "idx_patient_projections_tenant_first_name", columnList = "tenant_id, first_name"),
        @Index(name = "idx_patient_projections_patient_id", columnList = "patient_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProjection {

    /**
     * UUID primary key for this projection
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Patient aggregate ID from PatientCreatedEvent
     * Format: "patient-{tenantId}-{mrn}"
     */
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    /**
     * Tenant ID for multi-tenant isolation
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Medical Record Number - unique per tenant
     */
    @Column(name = "mrn", nullable = false)
    private String mrn;

    /**
     * Patient's first name
     */
    @Column(name = "first_name")
    private String firstName;

    /**
     * Patient's last name
     */
    @Column(name = "last_name")
    private String lastName;

    /**
     * Patient's date of birth
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Gender code (MALE, FEMALE, OTHER, UNKNOWN)
     */
    @Column(name = "gender")
    private String gender;

    /**
     * Insurance member ID
     */
    @Column(name = "insurance_member_id")
    private String insuranceMemberId;

    /**
     * When this projection was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When this projection was last updated
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
