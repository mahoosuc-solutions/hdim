package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Care Team Assignment Entity
 *
 * Stores patient-specific care team member assignments including:
 * - Patient ID
 * - Provider/care team member ID
 * - Role (primary care provider, specialist, care manager, etc.)
 * - Active status and effective dates
 *
 * This enables patient-specific alert routing to actual assigned
 * care team members instead of generic roles.
 */
@Entity
@Table(name = "care_team_assignments", indexes = {
    @Index(name = "idx_care_team_patient_active", columnList = "tenant_id, patient_id, active"),
    @Index(name = "idx_care_team_patient_role", columnList = "tenant_id, patient_id, role, active"),
    @Index(name = "idx_care_team_provider", columnList = "provider_id, active"),
    @Index(name = "idx_care_team_effective_dates", columnList = "effective_from, effective_to")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTeamAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Tenant ID for multi-tenant isolation
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Patient FHIR ID
     */
    @Column(name = "patient_id", nullable = false, length = 100)
    private UUID patientId;

    /**
     * Provider/care team member ID
     */
    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    /**
     * Provider/care team member name
     */
    @Column(name = "provider_name", length = 200)
    private String providerName;

    /**
     * Care team role
     * Examples: primary-care-provider, specialist, care-coordinator,
     *           care-manager, social-worker, nutritionist, psychiatrist,
     *           on-call-provider, ordering-provider
     */
    @Column(name = "role", nullable = false, length = 100)
    private String role;

    /**
     * Specialty (if applicable)
     * Examples: cardiology, endocrinology, psychiatry, etc.
     */
    @Column(name = "specialty", length = 100)
    private String specialty;

    /**
     * Whether this assignment is currently active
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Effective start date for this assignment
     */
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    /**
     * Effective end date for this assignment
     * NULL means indefinite
     */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /**
     * Whether this is the primary provider for this role
     * (e.g., primary among multiple care coordinators)
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = false;

    /**
     * Contact priority order (lower number = higher priority)
     */
    @Column(name = "contact_priority", nullable = false)
    @Builder.Default
    private int contactPriority = 10;

    /**
     * Email address for notifications
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Phone number for notifications
     */
    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Notes about this assignment
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Audit fields
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (effectiveFrom == null) {
            effectiveFrom = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
