package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Clinical Alert DTO
 *
 * Response object for clinical alerts with mental health crisis detection,
 * risk escalation, and health decline notifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalAlertDTO {

    /**
     * Alert ID
     */
    private String id;

    /**
     * Patient FHIR ID
     */
    private UUID patientId;

    /**
     * Tenant ID
     */
    private String tenantId;

    /**
     * Alert type (MENTAL_HEALTH_CRISIS, RISK_ESCALATION, HEALTH_DECLINE, CHRONIC_DETERIORATION)
     */
    private String alertType;

    /**
     * Severity level (CRITICAL, HIGH, MEDIUM, LOW)
     */
    private String severity;

    /**
     * Alert title
     */
    private String title;

    /**
     * Detailed alert message
     */
    private String message;

    /**
     * Source event type that triggered the alert
     */
    private String sourceEventType;

    /**
     * Source event ID
     */
    private String sourceEventId;

    /**
     * When the alert was triggered
     */
    private Instant triggeredAt;

    /**
     * When the alert was acknowledged
     */
    private Instant acknowledgedAt;

    /**
     * Who acknowledged the alert
     */
    private String acknowledgedBy;

    /**
     * Whether alert has been escalated
     */
    private boolean escalated;

    /**
     * When alert was escalated
     */
    private Instant escalatedAt;

    /**
     * Alert status (ACTIVE, ACKNOWLEDGED, RESOLVED)
     */
    private String status;

    /**
     * When the record was created
     */
    private Instant createdAt;

    /**
     * When the record was last updated
     */
    private Instant updatedAt;
}
