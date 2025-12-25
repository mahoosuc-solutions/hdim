package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Activity entity representing interactions with leads, contacts, and opportunities
 */
@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_activities_tenant", columnList = "tenant_id"),
    @Index(name = "idx_activities_lead", columnList = "lead_id"),
    @Index(name = "idx_activities_contact", columnList = "contact_id"),
    @Index(name = "idx_activities_opportunity", columnList = "opportunity_id"),
    @Index(name = "idx_activities_scheduled", columnList = "scheduled_at"),
    @Index(name = "idx_activities_type", columnList = "activity_type")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "opportunity_id")
    private UUID opportunityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "outcome", length = 500)
    private String outcome;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "zoho_activity_id", length = 100)
    private String zohoActivityId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void markComplete(String outcome) {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
        this.outcome = outcome;
    }
}
