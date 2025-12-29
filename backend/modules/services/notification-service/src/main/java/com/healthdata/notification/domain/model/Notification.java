package com.healthdata.notification.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a notification to be delivered to a recipient.
 *
 * HIPAA Compliance:
 * - PHI data is NOT stored in notifications (only references)
 * - Audit logging enabled for all state changes
 * - TTL enforced at application layer
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_notifications_recipient_id", columnList = "recipient_id"),
    @Index(name = "idx_notifications_status", columnList = "status"),
    @Index(name = "idx_notifications_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "recipient_id", nullable = false, length = 255)
    private String recipientId;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 50)
    private String recipientPhone;

    @Column(name = "device_token", length = 500)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

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

    public boolean canRetry() {
        return !status.isTerminal() && retryCount < maxRetries;
    }

    public void markSent(String externalId) {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
        this.externalId = externalId;
    }

    public void markFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}
