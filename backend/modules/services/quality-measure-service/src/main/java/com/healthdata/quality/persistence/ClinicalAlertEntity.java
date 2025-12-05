package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Clinical Alert Entity
 *
 * Stores clinical alerts for mental health crises, risk escalations,
 * health score declines, and chronic disease deterioration.
 *
 * Alert Types:
 * - MENTAL_HEALTH_CRISIS: PHQ-9 ≥20, GAD-7 ≥15, suicide risk
 * - RISK_ESCALATION: Patient risk level increased to VERY_HIGH
 * - HEALTH_DECLINE: Overall health score dropped ≥15 points
 * - CHRONIC_DETERIORATION: Chronic disease metrics worsening
 *
 * Severity Levels:
 * - CRITICAL: Immediate action required (suicide risk, severe crisis)
 * - HIGH: Urgent attention needed (risk escalation, severe symptoms)
 * - MEDIUM: Attention required (health decline, moderate deterioration)
 * - LOW: Monitoring recommended
 */
@Entity
@Table(name = "clinical_alerts", indexes = {
    @Index(name = "idx_alerts_patient_status", columnList = "tenant_id, patient_id, status"),
    @Index(name = "idx_alerts_triggered_at", columnList = "triggered_at DESC"),
    @Index(name = "idx_alerts_severity", columnList = "severity, status"),
    @Index(name = "idx_alerts_type", columnList = "alert_type, triggered_at DESC"),
    @Index(name = "idx_alerts_dedup", columnList = "tenant_id, patient_id, alert_type, triggered_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Patient FHIR ID
     */
    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    /**
     * Tenant ID for multi-tenant isolation
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Type of clinical alert
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    /**
     * Alert severity level
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    /**
     * Alert title (brief summary)
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Detailed alert message
     */
    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    /**
     * Source event type that triggered the alert
     * (e.g., "mental-health-assessment", "risk-assessment")
     */
    @Column(name = "source_event_type", length = 100)
    private String sourceEventType;

    /**
     * Source event ID (reference to assessment, score change, etc.)
     */
    @Column(name = "source_event_id", length = 100)
    private String sourceEventId;

    /**
     * When the alert was triggered
     */
    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    /**
     * When the alert was acknowledged by a provider
     */
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    /**
     * Provider who acknowledged the alert
     */
    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    /**
     * Whether alert has been escalated
     */
    @Column(name = "escalated", nullable = false)
    @Builder.Default
    private boolean escalated = false;

    /**
     * When alert was escalated
     */
    @Column(name = "escalated_at")
    private Instant escalatedAt;

    /**
     * Alert status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.ACTIVE;

    /**
     * Audit fields
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (triggeredAt == null) {
            triggeredAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Alert Types
     */
    public enum AlertType {
        /**
         * Mental health crisis (severe depression, anxiety, suicide risk)
         */
        MENTAL_HEALTH_CRISIS,

        /**
         * Patient risk level escalated to HIGH or VERY_HIGH
         */
        RISK_ESCALATION,

        /**
         * Overall health score declined significantly
         */
        HEALTH_DECLINE,

        /**
         * Chronic disease metrics deteriorating
         */
        CHRONIC_DETERIORATION
    }

    /**
     * Alert Severity Levels
     */
    public enum AlertSeverity {
        /**
         * Critical - Immediate action required (suicide risk, life-threatening)
         */
        CRITICAL,

        /**
         * High - Urgent attention needed (severe symptoms, high risk)
         */
        HIGH,

        /**
         * Medium - Attention required (moderate decline, care gap)
         */
        MEDIUM,

        /**
         * Low - Monitoring recommended
         */
        LOW
    }

    /**
     * Alert Status
     */
    public enum AlertStatus {
        /**
         * Alert is active and requires attention
         */
        ACTIVE,

        /**
         * Alert has been acknowledged by a provider
         */
        ACKNOWLEDGED,

        /**
         * Alert has been resolved
         */
        RESOLVED
    }
}
