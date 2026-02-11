package com.healthdata.investor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing an outreach activity (email, LinkedIn, call, meeting).
 * Tracks all interactions with investor contacts.
 */
@Entity
@Table(name = "outreach_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutreachActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private InvestorContact contact;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;  // linkedin_connect, linkedin_message, email, call, meeting, intro_request

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "pending";  // pending, sent, responded, completed, no_response

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "scheduled_time")
    private Instant scheduledTime;  // For meetings/calls

    @Column(name = "response_received")
    private Instant responseReceived;

    @Column(name = "response_content", columnDefinition = "TEXT")
    private String responseContent;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // LinkedIn-specific fields
    @Column(name = "linkedin_message_id", length = 100)
    private String linkedInMessageId;  // For tracking via API

    @Column(name = "linkedin_connection_status", length = 50)
    private String linkedInConnectionStatus;  // pending, connected, declined

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
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

    public boolean isLinkedInActivity() {
        return activityType != null && activityType.startsWith("linkedin_");
    }

    public boolean hasResponse() {
        return responseReceived != null;
    }

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }
}
