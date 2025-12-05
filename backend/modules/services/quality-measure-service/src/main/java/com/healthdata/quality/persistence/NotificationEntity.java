package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Notification Entity
 *
 * Tracks all notifications sent through the system for audit, delivery status,
 * and retry logic.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_patient", columnList = "patient_id"),
    @Index(name = "idx_notifications_status", columnList = "status"),
    @Index(name = "idx_notifications_channel", columnList = "channel"),
    @Index(name = "idx_notifications_created", columnList = "created_at"),
    @Index(name = "idx_notifications_tenant", columnList = "tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Column(name = "notification_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "severity")
    @Enumerated(EnumType.STRING)
    private NotificationSeverity severity;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "recipient", nullable = false)
    private String recipient; // Email address, phone number, or user ID

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(name = "provider")
    private String provider; // SendGrid, AWS SES, SMTP, Twilio, etc.

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
    }

    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP,
        WEBSOCKET
    }

    public enum NotificationType {
        CLINICAL_ALERT,
        CARE_GAP,
        HEALTH_SCORE_UPDATE,
        APPOINTMENT_REMINDER,
        MEDICATION_REMINDER,
        LAB_RESULT,
        PATIENT_MESSAGE,
        SYSTEM_NOTIFICATION
    }

    public enum NotificationSeverity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }

    public enum NotificationStatus {
        PENDING,      // Queued for sending
        SENDING,      // Currently being sent
        SENT,         // Sent to provider
        DELIVERED,    // Confirmed delivered
        FAILED,       // Failed to send
        BOUNCED,      // Email bounced
        REJECTED,     // Rejected by provider
        EXPIRED,      // Expired before delivery
        CANCELLED     // Cancelled by user/system
    }
}
