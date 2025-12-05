package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification History Entity
 *
 * Tracks all notifications sent through the system for HIPAA compliance audit trail.
 * Records when notifications were sent, to whom, via which channel, and the delivery status.
 *
 * Use Cases:
 * - HIPAA audit trail: Track all patient communications
 * - Delivery verification: Confirm notifications were sent
 * - Analytics: Understand notification patterns and effectiveness
 * - Troubleshooting: Debug notification delivery issues
 */
@Entity
@Table(
    name = "notification_history",
    indexes = {
        @Index(name = "idx_notification_patient_id", columnList = "patient_id"),
        @Index(name = "idx_notification_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_notification_sent_at", columnList = "sent_at"),
        @Index(name = "idx_notification_type", columnList = "notification_type"),
        @Index(name = "idx_notification_status", columnList = "status"),
        @Index(name = "idx_notification_channel", columnList = "channel")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistoryEntity {

    /**
     * Unique identifier for this notification history record
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Tenant ID for multi-tenancy isolation
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Type of notification sent
     * Examples: CRITICAL_ALERT, HEALTH_SCORE_UPDATE, APPOINTMENT_REMINDER,
     *           MEDICATION_REMINDER, LAB_RESULT, DAILY_DIGEST
     */
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    /**
     * Channel used to send the notification
     * Values: EMAIL, SMS, WEBSOCKET, PUSH_NOTIFICATION
     */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    /**
     * Template ID used to render the notification
     * Examples: critical-alert, health-score, appointment-reminder, etc.
     */
    @Column(name = "template_id", nullable = false, length = 100)
    private String templateId;

    /**
     * Patient FHIR ID (if notification is related to a specific patient)
     */
    @Column(name = "patient_id", length = 255)
    private String patientId;

    /**
     * Recipient identifier (email address, phone number, user ID, etc.)
     * Stored in encrypted form for PHI protection
     */
    @Column(name = "recipient_id", nullable = false, length = 255)
    private String recipientId;

    /**
     * Subject line (for emails) or title (for other notifications)
     */
    @Column(name = "subject", length = 500)
    private String subject;

    /**
     * Rendered notification content (for audit purposes)
     * Stored as text for HTML emails or plain text for SMS
     *
     * NOTE: Contains PHI - must be encrypted at rest
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Delivery status
     * Values: SENT, DELIVERED, FAILED, BOUNCED, PENDING
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Error message if delivery failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * External provider message ID (e.g., Twilio message SID, email provider message ID)
     */
    @Column(name = "external_message_id", length = 255)
    private String externalMessageId;

    /**
     * Alert ID that triggered this notification (if applicable)
     */
    @Column(name = "alert_id", length = 255)
    private String alertId;

    /**
     * Severity of the notification (for alerts)
     * Values: CRITICAL, HIGH, MEDIUM, LOW
     */
    @Column(name = "severity", length = 20)
    private String severity;

    /**
     * When the notification was sent
     */
    @CreationTimestamp
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    /**
     * When the notification was delivered (if delivery confirmation is available)
     */
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    /**
     * When the recipient opened/read the notification (if read receipts are available)
     */
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * Metadata in JSON format for additional context
     * Examples: {"retry_count": 2, "template_version": "1.2", "locale": "en-US"}
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * When the record was created (audit timestamp)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When the record was last updated (audit timestamp)
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
