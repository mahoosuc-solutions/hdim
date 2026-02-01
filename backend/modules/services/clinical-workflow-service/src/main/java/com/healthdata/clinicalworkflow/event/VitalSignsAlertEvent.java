package com.healthdata.clinicalworkflow.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka Event: Vital Signs Alert
 *
 * Published when abnormal vital signs are detected (warning or critical).
 * Consumed by:
 * - Alert Service: Triggers provider notifications
 * - FHIR Service: Creates Flag resources
 * - Analytics Service: Tracks alert trends
 *
 * Topic Routing:
 * - Critical alerts → vitals.alert.critical
 * - Warning alerts → vitals.alert.warning
 *
 * HIPAA Compliance:
 * - PHI included (patient ID, vital values)
 * - Kafka topic encryption required in production
 * - Access control enforced via tenant ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSignsAlertEvent {

    /**
     * Event metadata
     */
    private String eventId;
    private String eventType; // "VITAL_SIGNS_ALERT_CREATED"
    private Instant eventTimestamp;
    private String eventSource; // "clinical-workflow-service"

    /**
     * Tenant and patient identifiers (HIPAA §164.312(d))
     */
    private String tenantId;
    private UUID patientId;
    private String patientName; // For notification display (nullable)

    /**
     * Vital signs record reference
     */
    private UUID vitalsId;
    private String encounterId;
    private Instant recordedAt;
    private String recordedBy;

    /**
     * Alert metadata
     */
    private String alertStatus; // "warning" or "critical"
    private String alertMessage; // Human-readable alert description
    private String[] alertTypes;  // e.g., ["HIGH_BLOOD_PRESSURE", "HIGH_HEART_RATE"]

    /**
     * Vital signs values (PHI - Protected Health Information)
     * Only abnormal values included to minimize PHI exposure
     */
    private VitalSignsValues values;

    /**
     * Room assignment (for emergency response)
     */
    private String roomNumber; // e.g., "EXAM-101" (nullable)

    /**
     * Nested class for vital signs values
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VitalSignsValues {
        private BigDecimal systolicBp;
        private BigDecimal diastolicBp;
        private BigDecimal heartRate;
        private BigDecimal temperatureF;
        private BigDecimal respirationRate;
        private BigDecimal oxygenSaturation;
    }
}
