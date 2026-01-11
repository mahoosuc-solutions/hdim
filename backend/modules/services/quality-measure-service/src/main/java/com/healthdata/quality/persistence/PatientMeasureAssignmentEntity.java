package com.healthdata.quality.persistence;


import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Patient Measure Assignment Entity
 * Tracks which quality measures are assigned to which patients.
 * Maps to the patient_measure_assignments table created in migration 0034.
 *
 * Supports both manual assignments (by care coordinators) and automatic
 * assignments (based on eligibility rules).
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "patient_measure_assignments",
    indexes = {
        @Index(name = "idx_pma_patient_measure", columnList = "patient_id, measure_id, active"),
        @Index(name = "idx_pma_measure", columnList = "measure_id, active"),
        @Index(name = "idx_pma_tenant", columnList = "tenant_id"),
        @Index(name = "idx_pma_auto_assigned", columnList = "auto_assigned, active"),
        @Index(name = "idx_pma_effective_dates", columnList = "effective_from, effective_until")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientMeasureAssignmentEntity implements Serializable {

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

    @Column(name = "measure_id", nullable = false)
    private UUID measureId;

    @Column(name = "measure_type", length = 20)
    private String measureType; // HEDIS, CMS, CUSTOM

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

    // Eligibility Rules (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligibility_criteria", columnDefinition = "jsonb")
    private Map<String, Object> eligibilityCriteria;

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
