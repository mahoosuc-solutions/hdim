package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks LinkedIn outreach activities including connection requests,
 * InMail messages, and profile engagements.
 */
@Entity
@Table(name = "linkedin_outreach",
    indexes = {
        @Index(name = "idx_linkedin_outreach_tenant", columnList = "tenant_id"),
        @Index(name = "idx_linkedin_outreach_lead", columnList = "lead_id"),
        @Index(name = "idx_linkedin_outreach_contact", columnList = "contact_id"),
        @Index(name = "idx_linkedin_outreach_status", columnList = "status"),
        @Index(name = "idx_linkedin_outreach_scheduled", columnList = "scheduled_at")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkedInOutreach {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // Target - either a lead or contact
    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "contact_id")
    private UUID contactId;

    // LinkedIn profile info
    @Column(name = "linkedin_profile_url", length = 500)
    private String linkedinProfileUrl;

    @Column(name = "linkedin_profile_id", length = 100)
    private String linkedinProfileId;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Column(name = "target_title", length = 200)
    private String targetTitle;

    @Column(name = "target_company", length = 200)
    private String targetCompany;

    // Outreach details
    @Enumerated(EnumType.STRING)
    @Column(name = "outreach_type", nullable = false, length = 50)
    private OutreachType outreachType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private OutreachStatus status = OutreachStatus.PENDING;

    @Column(name = "message_template", length = 100)
    private String messageTemplate;

    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "connection_note", length = 300)
    private String connectionNote; // LinkedIn limit: 300 chars

    // Scheduling
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // Tracking
    @Column(name = "connection_accepted")
    @Builder.Default
    private Boolean connectionAccepted = false;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "replied")
    @Builder.Default
    private Boolean replied = false;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "profile_viewed")
    @Builder.Default
    private Boolean profileViewed = false;

    @Column(name = "profile_viewed_at")
    private LocalDateTime profileViewedAt;

    // Sequence tracking
    @Column(name = "sequence_id")
    private UUID sequenceId;

    @Column(name = "sequence_step")
    private Integer sequenceStep;

    // Campaign relationship
    @Column(name = "campaign_id")
    private UUID campaignId;

    // Metadata (legacy - kept for backwards compatibility)
    @Column(name = "campaign_name", length = 200)
    private String campaignName;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    // Business methods

    public void markSent() {
        this.status = OutreachStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAccepted() {
        this.connectionAccepted = true;
        this.acceptedAt = LocalDateTime.now();
        this.status = OutreachStatus.ACCEPTED;
    }

    public void markReplied() {
        this.replied = true;
        this.repliedAt = LocalDateTime.now();
        this.status = OutreachStatus.REPLIED;
    }

    public void markProfileViewed() {
        this.profileViewed = true;
        this.profileViewedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.status = OutreachStatus.FAILED;
        this.errorMessage = error;
        this.retryCount++;
    }

    public boolean canRetry() {
        return retryCount < 3 && status == OutreachStatus.FAILED;
    }

    public enum OutreachType {
        CONNECTION_REQUEST,
        INMAIL,
        MESSAGE,           // Direct message to existing connection
        PROFILE_VIEW,
        POST_ENGAGEMENT,   // Like/comment on their post
        FOLLOW
    }

    public enum OutreachStatus {
        PENDING,           // Scheduled but not sent
        SENT,              // Outreach sent
        ACCEPTED,          // Connection accepted
        REPLIED,           // Received a reply
        IGNORED,           // No response after follow-up period
        DECLINED,          // Connection request declined
        FAILED,            // Technical failure
        CANCELLED          // Manually cancelled
    }
}
