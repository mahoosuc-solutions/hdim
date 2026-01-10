package com.healthdata.patient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Patient Risk Score Entity
 *
 * Stores risk stratification outputs for care management.
 */
@Entity
@Table(name = "patient_risk_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRiskScoreEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "score_type", nullable = false, length = 50)
    private String scoreType;

    @Column(name = "score_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal scoreValue;

    @Column(name = "risk_category", nullable = false, length = 20)
    private String riskCategory;

    @Column(name = "calculation_date", nullable = false)
    private Instant calculationDate;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "factors")
    @JdbcTypeCode(SqlTypes.JSON)
    private String factors;

    @Column(name = "comorbidities")
    @JdbcTypeCode(SqlTypes.JSON)
    private String comorbidities;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (calculationDate == null) {
            calculationDate = Instant.now();
        }
    }
}
