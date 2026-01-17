package com.healthdata.clinicalworkflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Vital Signs Record Entity
 *
 * Stores vital signs measurements with abnormal value detection:
 * - BP, HR, Temp, O2, RR, Weight, Height, BMI
 * - Alert status (normal, warning, critical)
 * - Links to FHIR Observation and Encounter resources
 *
 * Multi-tenant with HIPAA-compliant caching (5-minute TTL)
 * Used for MA vitals recording workflow
 */
@Entity
@Table(
    name = "vital_signs_records",
    indexes = {
        @Index(name = "idx_vital_signs_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_vital_signs_patient_id", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_vital_signs_recorded_at", columnList = "tenant_id, patient_id, recorded_at"),
        @Index(name = "idx_vital_signs_alert_status", columnList = "tenant_id, alert_status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSignsRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "encounter_id", length = 255)
    private String encounterId;

    @Column(name = "observation_id", length = 255, columnDefinition = "VARCHAR(255) COMMENT 'FHIR Observation resource ID'")
    private String observationId;

    @Column(name = "recorded_by", nullable = false, length = 255)
    private String recordedBy;

    // Vital Signs Measurements
    @Column(name = "systolic_bp", precision = 5, scale = 1, columnDefinition = "NUMERIC(5,1) COMMENT 'Systolic Blood Pressure in mmHg'")
    private BigDecimal systolicBp;

    @Column(name = "diastolic_bp", precision = 5, scale = 1, columnDefinition = "NUMERIC(5,1) COMMENT 'Diastolic Blood Pressure in mmHg'")
    private BigDecimal diastolicBp;

    @Column(name = "heart_rate", precision = 5, scale = 1, columnDefinition = "NUMERIC(5,1) COMMENT 'Heart Rate in beats per minute'")
    private BigDecimal heartRate;

    @Column(name = "temperature_f", precision = 5, scale = 2, columnDefinition = "NUMERIC(5,2) COMMENT 'Temperature in Fahrenheit'")
    private BigDecimal temperatureF;

    @Column(name = "respiration_rate", precision = 5, scale = 1, columnDefinition = "NUMERIC(5,1) COMMENT 'Respiration Rate in breaths per minute'")
    private BigDecimal respirationRate;

    @Column(name = "oxygen_saturation", precision = 5, scale = 1, columnDefinition = "NUMERIC(5,1) COMMENT 'O2 Saturation in percentage'")
    private BigDecimal oxygenSaturation;

    @Column(name = "weight_kg", precision = 7, scale = 2, columnDefinition = "NUMERIC(7,2) COMMENT 'Weight in kilograms'")
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 1, columnDefinition = "NUMERIC(5,1) COMMENT 'Height in centimeters'")
    private BigDecimal heightCm;

    @Column(name = "bmi", precision = 5, scale = 2, columnDefinition = "NUMERIC(5,2) COMMENT 'Body Mass Index'")
    private BigDecimal bmi;

    // Alert Status
    @Column(name = "alert_status", nullable = false, length = 50)
    @Builder.Default
    private String alertStatus = "normal";  // normal, warning, critical

    @Column(name = "alert_message", columnDefinition = "TEXT")
    private String alertMessage;

    // Workflow
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
