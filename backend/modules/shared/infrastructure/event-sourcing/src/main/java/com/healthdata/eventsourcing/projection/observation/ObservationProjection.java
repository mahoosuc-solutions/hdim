package com.healthdata.eventsourcing.projection.observation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Observation Projection - CQRS Read Model for Vital Signs
 *
 * Time-series data for vital signs (observations) materialized from ObservationRecordedEvent.
 * Optimized for temporal queries and trend analysis.
 */
@Entity
@Table(
    name = "observation_projections",
    indexes = {
        @Index(name = "idx_observation_projections_tenant_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_observation_projections_loinc", columnList = "loinc_code"),
        @Index(name = "idx_observation_projections_date", columnList = "observation_date"),
        @Index(name = "idx_observation_projections_tenant_patient_loinc", columnList = "tenant_id, patient_id, loinc_code"),
        @Index(name = "idx_observation_projections_tenant_patient_date", columnList = "tenant_id, patient_id, observation_date")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservationProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "loinc_code", nullable = false)
    private String loincCode;

    @Column(name = "value", nullable = false)
    private BigDecimal value;

    @Column(name = "unit")
    private String unit;

    @Column(name = "observation_date", nullable = false)
    private Instant observationDate;

    @Column(name = "notes")
    private String notes;

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
