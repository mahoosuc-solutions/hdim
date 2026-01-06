package com.healthdata.quality.persistence;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Result Signature Entity - Immutable audit trail for signed results.
 *
 * HIPAA Compliance:
 * - All fields are immutable after creation (no update methods)
 * - Captures full context at signing time
 * - Provides complete audit trail for regulatory compliance
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "result_signatures",
       indexes = {
           @Index(name = "idx_result_signatures_result_id", columnList = "result_id"),
           @Index(name = "idx_result_signatures_tenant_signed", columnList = "tenant_id, signed_by, signed_at"),
           @Index(name = "idx_result_signatures_patient", columnList = "tenant_id, patient_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultSignatureEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "result_id", nullable = false, updatable = false)
    private UUID resultId;

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "measure_id", nullable = false, updatable = false, length = 100)
    private String measureId;

    // Signing details
    @Column(name = "signed_by", nullable = false, updatable = false, length = 100)
    private String signedBy;

    @Column(name = "signed_by_username", nullable = false, updatable = false, length = 100)
    private String signedByUsername;

    @Column(name = "signed_at", nullable = false, updatable = false)
    private LocalDateTime signedAt;

    @Column(name = "signature_type", nullable = false, updatable = false, length = 50)
    private String signatureType;

    @Column(name = "notes", updatable = false, length = 1000)
    private String notes;

    // Snapshot of result at signing time (for audit)
    @Column(name = "numerator_compliant", updatable = false)
    private Boolean numeratorCompliant;

    @Column(name = "compliance_rate", updatable = false)
    private Double complianceRate;

    // Record creation timestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
