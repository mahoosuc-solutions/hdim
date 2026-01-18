package com.healthdata.eventsourcing.projection.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Condition Projection - CQRS Read Model for Diagnoses
 * Status tracking and diagnosis history
 */
@Entity
@Table(
    name = "condition_projections",
    indexes = {
        @Index(name = "idx_condition_projections_tenant_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_condition_projections_icd_code", columnList = "icd_code"),
        @Index(name = "idx_condition_projections_status", columnList = "status"),
        @Index(name = "idx_condition_projections_tenant_patient_status", columnList = "tenant_id, patient_id, status"),
        @Index(name = "idx_condition_projections_onset_date", columnList = "onset_date")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConditionProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "icd_code", nullable = false)
    private String icdCode;

    @Column(name = "status")
    private String status;

    @Column(name = "verification_status")
    private String verificationStatus;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "clinical_notes")
    private String clinicalNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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
