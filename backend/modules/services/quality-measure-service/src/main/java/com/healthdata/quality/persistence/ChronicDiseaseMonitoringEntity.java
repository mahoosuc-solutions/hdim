package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Chronic Disease Monitoring Entity
 *
 * Tracks continuous monitoring of chronic diseases for deterioration detection.
 * Supports trend analysis and automated alerting for:
 * - Diabetes (HbA1c monitoring)
 * - Hypertension (Blood pressure monitoring)
 * - Hyperlipidemia (LDL cholesterol monitoring)
 * - Other chronic conditions
 */
@Entity
@Table(name = "chronic_disease_monitoring",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cdm_tenant_patient_disease",
            columnNames = {"tenant_id", "patient_id", "disease_code"}
        )
    },
    indexes = {
        @Index(name = "idx_cdm_tenant", columnList = "tenant_id"),
        @Index(name = "idx_cdm_patient", columnList = "patient_id"),
        @Index(name = "idx_cdm_tenant_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_cdm_alerts", columnList = "tenant_id, alert_triggered, monitored_at DESC"),
        @Index(name = "idx_cdm_trend", columnList = "tenant_id, trend"),
        @Index(name = "idx_cdm_next_monitoring", columnList = "tenant_id, next_monitoring_due")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChronicDiseaseMonitoringEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private UUID patientId;

    /**
     * SNOMED CT code for the chronic disease
     * Examples:
     * - 44054006: Type 2 Diabetes Mellitus
     * - 38341003: Hypertensive disorder
     * - 13644009: Hyperlipidemia
     */
    @Column(name = "disease_code", nullable = false, length = 50)
    private String diseaseCode;

    @Column(name = "disease_name", nullable = false, length = 255)
    private String diseaseName;

    /**
     * Latest measured value (e.g., HbA1c %, BP mmHg, LDL mg/dL)
     */
    @Column(name = "latest_value")
    private Double latestValue;

    /**
     * Previous measured value for trend comparison
     */
    @Column(name = "previous_value")
    private Double previousValue;

    /**
     * Trend direction: IMPROVING, STABLE, or DETERIORATING
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trend", nullable = false, length = 20)
    private Trend trend;

    /**
     * Whether an alert was triggered for this monitoring record
     */
    @Column(name = "alert_triggered", nullable = false)
    private boolean alertTriggered;

    /**
     * When this disease was last monitored
     */
    @Column(name = "monitored_at", nullable = false)
    private Instant monitoredAt;

    /**
     * When the next monitoring is due (for proactive outreach)
     */
    @Column(name = "next_monitoring_due")
    private Instant nextMonitoringDue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (monitoredAt == null) {
            monitoredAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Disease trend direction
     */
    public enum Trend {
        IMPROVING,      // Values moving toward target range
        STABLE,         // Values within acceptable range or not changing significantly
        DETERIORATING   // Values moving away from target range
    }
}
