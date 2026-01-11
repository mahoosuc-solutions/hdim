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
 * Patient Measure Override Entity
 * Allows patient-specific customization of measure parameters with clinical justification.
 * Maps to the patient_measure_overrides table created in migration 0035.
 *
 * HIPAA Compliance: All overrides require clinical justification and supporting evidence.
 * Supports approval workflow and periodic review requirements.
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "patient_measure_overrides",
    indexes = {
        @Index(name = "idx_pmo_patient_measure", columnList = "patient_id, measure_id, active"),
        @Index(name = "idx_pmo_measure", columnList = "measure_id, active"),
        @Index(name = "idx_pmo_tenant", columnList = "tenant_id"),
        @Index(name = "idx_pmo_review_due", columnList = "next_review_date"),
        @Index(name = "idx_pmo_override_type", columnList = "override_type, active")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientMeasureOverrideEntity implements Serializable {

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

    // Override Configuration
    @Column(name = "override_type", nullable = false, length = 50)
    private String overrideType; // THRESHOLD, PARAMETER, EXCLUSION, INCLUSION_CRITERIA, TARGET_VALUE, FREQUENCY

    @Column(name = "override_field", nullable = false, length = 100)
    private String overrideField;

    @Column(name = "original_value", columnDefinition = "TEXT")
    private String originalValue;

    @Column(name = "override_value", nullable = false, columnDefinition = "TEXT")
    private String overrideValue;

    @Column(name = "value_type", nullable = false, length = 50)
    @Builder.Default
    private String valueType = "TEXT"; // NUMERIC, DATE, BOOLEAN, TEXT, JSON

    // Clinical Justification (REQUIRED for HIPAA)
    @Column(name = "clinical_reason", nullable = false, columnDefinition = "TEXT")
    private String clinicalReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supporting_evidence", columnDefinition = "jsonb")
    private Map<String, Object> supportingEvidence;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    // Status
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    // Periodic Review
    @Column(name = "requires_periodic_review", nullable = false)
    @Builder.Default
    private Boolean requiresPeriodicReview = true;

    @Column(name = "review_frequency_days", nullable = false)
    @Builder.Default
    private Integer reviewFrequencyDays = 90;

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

    @Column(name = "last_reviewed_by")
    private UUID lastReviewedBy;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate; // Auto-calculated by trigger

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
        if (requiresPeriodicReview == null) {
            requiresPeriodicReview = true;
        }
        if (reviewFrequencyDays == null) {
            reviewFrequencyDays = 90;
        }
        if (valueType == null) {
            valueType = "TEXT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
