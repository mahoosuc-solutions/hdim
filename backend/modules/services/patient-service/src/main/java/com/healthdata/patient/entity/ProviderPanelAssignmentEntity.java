package com.healthdata.patient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Provider Panel Assignment Entity
 * Issue #135: Create Provider Panel Assignment API
 *
 * Stores provider-patient panel assignments for primary care workflows.
 */
@Entity
@Table(name = "provider_panel_assignment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPanelAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "assigned_date", nullable = false)
    private Instant assignedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 50)
    @Builder.Default
    private AssignmentType assignmentType = AssignmentType.PRIMARY;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (assignedDate == null) {
            assignedDate = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Assignment type enum for provider-patient relationships
     */
    public enum AssignmentType {
        /** Primary care provider */
        PRIMARY,
        /** Specialist referral */
        SPECIALIST,
        /** Care coordinator */
        CARE_COORDINATOR,
        /** Covering provider (temporary) */
        COVERING,
        /** Consulting provider */
        CONSULTING
    }
}
