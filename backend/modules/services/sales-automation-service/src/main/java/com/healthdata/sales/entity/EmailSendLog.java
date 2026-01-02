package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Log of individual email sends for tracking and analytics
 */
@Entity
@Table(name = "email_send_logs", indexes = {
    @Index(name = "idx_email_logs_tenant", columnList = "tenant_id"),
    @Index(name = "idx_email_logs_enrollment", columnList = "enrollment_id"),
    @Index(name = "idx_email_logs_email", columnList = "recipient_email"),
    @Index(name = "idx_email_logs_sent_at", columnList = "sent_at"),
    @Index(name = "idx_email_logs_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "enrollment_id")
    private UUID enrollmentId;

    @Column(name = "sequence_id")
    private UUID sequenceId;

    @Column(name = "step_id")
    private UUID stepId;

    @Column(name = "step_number")
    private Integer stepNumber;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "from_email", length = 255)
    private String fromEmail;

    @Column(name = "from_name", length = 100)
    private String fromName;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EmailStatus status = EmailStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(name = "bounced_at")
    private LocalDateTime bouncedAt;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;

    @Column(name = "open_count", nullable = false)
    @Builder.Default
    private Integer openCount = 0;

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Integer clickCount = 0;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "mail_provider_id", length = 255)
    private String mailProviderId;

    @Column(name = "tracking_id", length = 100)
    private String trackingId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum EmailStatus {
        PENDING,        // Queued for sending
        SENT,           // Successfully sent
        DELIVERED,      // Confirmed delivered
        OPENED,         // Email opened
        CLICKED,        // Link clicked
        BOUNCED,        // Email bounced
        FAILED,         // Send failed
        CANCELLED       // Cancelled before sending
    }

    public void markSent(String providerId) {
        this.status = EmailStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.mailProviderId = providerId;
    }

    public void markDelivered() {
        this.status = EmailStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void markOpened() {
        if (this.openedAt == null) {
            this.openedAt = LocalDateTime.now();
        }
        this.openCount++;
        this.status = EmailStatus.OPENED;
    }

    public void markClicked() {
        if (this.clickedAt == null) {
            this.clickedAt = LocalDateTime.now();
        }
        this.clickCount++;
        this.status = EmailStatus.CLICKED;
    }

    public void markBounced(String errorMessage) {
        this.status = EmailStatus.BOUNCED;
        this.bouncedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void markFailed(String errorMessage) {
        this.status = EmailStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
