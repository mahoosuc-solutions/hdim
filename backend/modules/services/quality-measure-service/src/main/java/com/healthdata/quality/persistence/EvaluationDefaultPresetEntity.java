package com.healthdata.quality.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evaluation Default Preset Entity
 *
 * Stores a single default evaluation preset per tenant and user scope.
 */
@Entity
@Table(name = "evaluation_default_presets",
    indexes = {
        @Index(name = "idx_eval_default_preset_tenant", columnList = "tenant_id"),
        @Index(name = "idx_eval_default_preset_user", columnList = "user_id"),
        @Index(name = "idx_eval_default_preset_measure", columnList = "measure_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_eval_default_preset_tenant_user", columnNames = {"tenant_id", "user_id"})
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDefaultPresetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "measure_id", nullable = false, length = 255)
    private String measureId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "use_cql_engine", nullable = false)
    @Builder.Default
    private Boolean useCqlEngine = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (useCqlEngine == null) {
            useCqlEngine = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
