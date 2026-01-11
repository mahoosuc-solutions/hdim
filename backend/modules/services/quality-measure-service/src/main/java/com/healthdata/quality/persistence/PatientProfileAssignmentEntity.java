package com.healthdata.quality.persistence;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Patient Profile Assignment Entity
 * Links patients to configuration profiles for measure parameter customization.
 * Maps to the patient_profile_assignments table created in migration 0037.
 *
 * Supports both manual and automatic profile assignments based on patient characteristics.
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "patient_profile_assignments",
    indexes = {
        @Index(name = "idx_ppa_patient_profile", columnList = "patient_id, profile_id, active"),
        @Index(name = "idx_ppa_profile", columnList = "profile_id, active"),
        @Index(name = "idx_ppa_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ppa_auto_assigned", columnList = "auto_assigned, active")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileAssignmentEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    // Core References
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    // Assignment Metadata
    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assignment_reason", columnDefinition = "TEXT")
    private String assignmentReason;

    // Status
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    @Column(name = "auto_assigned", nullable = false)
    @Builder.Default
    private Boolean autoAssigned = false;

    // Audit Fields
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (autoAssigned == null) {
            autoAssigned = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
