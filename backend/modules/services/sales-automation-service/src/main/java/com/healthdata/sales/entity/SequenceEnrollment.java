package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks enrollment of a lead/contact in an email sequence
 */
@Entity
@Table(name = "sequence_enrollments", indexes = {
    @Index(name = "idx_enrollments_tenant", columnList = "tenant_id"),
    @Index(name = "idx_enrollments_sequence", columnList = "sequence_id"),
    @Index(name = "idx_enrollments_lead", columnList = "lead_id"),
    @Index(name = "idx_enrollments_contact", columnList = "contact_id"),
    @Index(name = "idx_enrollments_status", columnList = "status"),
    @Index(name = "idx_enrollments_next_email", columnList = "next_email_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequenceEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sequence_id", nullable = false)
    private EmailSequence sequence;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "current_step", nullable = false)
    @Builder.Default
    private Integer currentStep = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "next_email_at")
    private LocalDateTime nextEmailAt;

    @Column(name = "last_email_sent_at")
    private LocalDateTime lastEmailSentAt;

    @Column(name = "emails_sent", nullable = false)
    @Builder.Default
    private Integer emailsSent = 0;

    @Column(name = "emails_opened", nullable = false)
    @Builder.Default
    private Integer emailsOpened = 0;

    @Column(name = "emails_clicked", nullable = false)
    @Builder.Default
    private Integer emailsClicked = 0;

    @Column(name = "emails_bounced", nullable = false)
    @Builder.Default
    private Integer emailsBounced = 0;

    @Column(name = "enrolled_by_user_id")
    private UUID enrolledByUserId;

    @Column(name = "pause_reason", length = 255)
    private String pauseReason;

    @Column(name = "completion_reason", length = 255)
    private String completionReason;

    @Column(name = "unsubscribe_token", length = 100)
    private String unsubscribeToken;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if enrollment is active and can receive emails
     */
    public boolean canReceiveEmail() {
        return status == EnrollmentStatus.ACTIVE &&
               email != null &&
               !email.isBlank();
    }

    /**
     * Move to next step
     */
    public void advanceStep() {
        this.currentStep++;
        this.emailsSent++;
        this.lastEmailSentAt = LocalDateTime.now();
    }

    /**
     * Record email open
     */
    public void recordOpen() {
        this.emailsOpened++;
    }

    /**
     * Record email click
     */
    public void recordClick() {
        this.emailsClicked++;
    }

    /**
     * Record email bounce
     */
    public void recordBounce() {
        this.emailsBounced++;
        if (this.emailsBounced >= 2) {
            this.status = EnrollmentStatus.BOUNCED;
        }
    }

    /**
     * Pause the enrollment
     */
    public void pause(String reason) {
        this.status = EnrollmentStatus.PAUSED;
        this.pauseReason = reason;
    }

    /**
     * Resume the enrollment
     */
    public void resume() {
        this.status = EnrollmentStatus.ACTIVE;
        this.pauseReason = null;
    }

    /**
     * Complete the enrollment
     */
    public void complete(String reason) {
        this.status = EnrollmentStatus.COMPLETED;
        this.completionReason = reason;
        this.nextEmailAt = null;
    }

    /**
     * Unsubscribe
     */
    public void unsubscribe() {
        this.status = EnrollmentStatus.UNSUBSCRIBED;
        this.nextEmailAt = null;
    }

    /**
     * Get display name
     */
    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return email;
    }
}
