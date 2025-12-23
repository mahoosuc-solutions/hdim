package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ML Prediction Entity
 *
 * Stores prediction outputs and outcomes for validation.
 */
@Entity
@Table(name = "ml_predictions", indexes = {
    @Index(name = "idx_mlp_patient", columnList = "tenant_id, patient_id, predicted_at"),
    @Index(name = "idx_mlp_type", columnList = "tenant_id, prediction_type, predicted_at"),
    @Index(name = "idx_mlp_model_performance", columnList = "tenant_id, model_name, model_version, actual_outcome"),
    @Index(name = "idx_mlp_high_risk", columnList = "tenant_id, prediction_type, prediction_value"),
    @Index(name = "idx_mlp_outcomes", columnList = "tenant_id, outcome_date, actual_outcome"),
    @Index(name = "idx_mlp_tenant", columnList = "tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlPredictionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private UUID patientId;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 20)
    private String modelVersion;

    @Column(name = "prediction_type", nullable = false, length = 50)
    private String predictionType;

    @Column(name = "prediction_value", nullable = false)
    private Double predictionValue;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features_used", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> featuresUsed;

    @Column(name = "predicted_at", nullable = false)
    private Instant predictedAt;

    @Column(name = "outcome_date")
    private Instant outcomeDate;

    @Column(name = "actual_outcome")
    private Boolean actualOutcome;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (predictedAt == null) {
            predictedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
