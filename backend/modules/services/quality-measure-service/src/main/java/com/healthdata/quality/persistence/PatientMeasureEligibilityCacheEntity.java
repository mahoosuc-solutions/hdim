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
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Patient Measure Eligibility Cache Entity
 * Performance cache for patient measure eligibility computations.
 * Maps to the patient_measure_eligibility_cache table created in migration 0040.
 *
 * Optimization: Caches expensive eligibility calculations with TTL-based invalidation.
 * Supports manual invalidation when patient data or measure definitions change.
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "patient_measure_eligibility_cache",
    indexes = {
        @Index(name = "idx_pmec_patient_valid", columnList = "patient_id, invalidated"),
        @Index(name = "idx_pmec_measure_valid", columnList = "measure_id, invalidated"),
        @Index(name = "idx_pmec_valid_until", columnList = "valid_until"),
        @Index(name = "idx_pmec_tenant", columnList = "tenant_id"),
        @Index(name = "idx_pmec_eligible", columnList = "is_eligible, invalidated")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pmec_patient_measure", columnNames = {"patient_id", "measure_id"})
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientMeasureEligibilityCacheEntity implements Serializable {

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

    // Eligibility Result
    @Column(name = "is_eligible", nullable = false)
    private Boolean isEligible;

    @Column(name = "eligibility_reason", columnDefinition = "TEXT")
    private String eligibilityReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligibility_criteria_met", columnDefinition = "jsonb")
    private Map<String, Object> eligibilityCriteriaMet;

    // Cache Metadata
    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @Column(name = "valid_until", nullable = false)
    private OffsetDateTime validUntil;

    @Column(name = "invalidated", nullable = false)
    @Builder.Default
    private Boolean invalidated = false;

    // Data Version Tracking
    @Column(name = "patient_data_version", length = 100)
    private String patientDataVersion;

    @Column(name = "measure_version", length = 50)
    private String measureVersion;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (calculatedAt == null) {
            calculatedAt = OffsetDateTime.now();
        }
        if (invalidated == null) {
            invalidated = false;
        }
    }
}
