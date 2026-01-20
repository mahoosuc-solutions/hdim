package com.healthdata.qualityevent.projection;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Measure Evaluation Projection Entity (CQRS Read Model)
 *
 * Denormalized measure evaluation view optimized for fast queries.
 * Updated through Kafka event stream.
 *
 * FIX: Added @Entity annotation (was missing, causing "not a managed type" error)
 */
@Entity
@Table(name = "measure_evaluation_projections", indexes = {
    @Index(name = "idx_mep_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_mep_tenant_measure", columnList = "tenant_id, measure_id"),
    @Index(name = "idx_mep_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_mep_tenant_measure_patient", columnList = "tenant_id, measure_id, patient_id", unique = true),
    @Index(name = "idx_mep_updated_at", columnList = "last_updated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasureEvaluationProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tenant Isolation (CRITICAL)
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    // Measure & Patient Identifiers
    @Column(name = "measure_id", nullable = false, length = 100)
    private String measureId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private UUID patientId;

    // Evaluation Results
    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "compliance_status", length = 50)
    private String complianceStatus;  // COMPLIANT, NON_COMPLIANT, EXCLUDED

    @Column(name = "compliance_rate")
    private Double complianceRate;

    // Evaluation Details
    @Column(name = "numerator", nullable = false)
    @Builder.Default
    private Integer numerator = 0;

    @Column(name = "denominator", nullable = false)
    @Builder.Default
    private Integer denominator = 0;

    @Column(name = "exclusions", nullable = false)
    @Builder.Default
    private Integer exclusions = 0;

    // Measure Metadata
    @Column(name = "measure_name", length = 255)
    private String measureName;

    @Column(name = "measure_version", length = 50)
    private String measureVersion;

    @Column(name = "measure_type", length = 50)
    private String measureType;  // PROCESS, OUTCOME, STRUCTURAL, etc.

    // Evaluation Metadata
    @Column(name = "evaluation_period_start")
    private Instant evaluationPeriodStart;

    @Column(name = "evaluation_period_end")
    private Instant evaluationPeriodEnd;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    // Status & Flags
    @Column(name = "is_compliant", nullable = false)
    @Builder.Default
    private Boolean isCompliant = false;

    @Column(name = "meets_threshold", nullable = false)
    @Builder.Default
    private Boolean meetsThreshold = false;

    // Additional Details
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "event_version", nullable = false)
    @Builder.Default
    private Long eventVersion = 0L;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        lastUpdatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = Instant.now();
    }
}
