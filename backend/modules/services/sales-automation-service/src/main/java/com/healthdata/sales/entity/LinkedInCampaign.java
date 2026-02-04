package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a LinkedIn outreach campaign for managing bulk connection
 * requests and InMail messages to targeted prospects.
 */
@Entity
@Table(name = "linkedin_campaigns",
    indexes = {
        @Index(name = "idx_linkedin_campaigns_tenant", columnList = "tenant_id"),
        @Index(name = "idx_linkedin_campaigns_status", columnList = "status"),
        @Index(name = "idx_linkedin_campaigns_name", columnList = "name")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_linkedin_campaigns_tenant_name", columnNames = {"tenant_id", "name"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkedInCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "target_criteria", columnDefinition = "TEXT")
    private String targetCriteria;

    @Column(name = "daily_limit", nullable = false)
    @Builder.Default
    private Integer dailyLimit = 25;

    // Metrics (denormalized for performance)
    @Column(name = "total_sent")
    @Builder.Default
    private Integer totalSent = 0;

    @Column(name = "total_accepted")
    @Builder.Default
    private Integer totalAccepted = 0;

    @Column(name = "total_replied")
    @Builder.Default
    private Integer totalReplied = 0;

    @Column(name = "acceptance_rate")
    @Builder.Default
    private Double acceptanceRate = 0.0;

    // Ownership
    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business methods

    public void activate() {
        this.status = CampaignStatus.ACTIVE;
    }

    public void pause() {
        this.status = CampaignStatus.PAUSED;
    }

    public void complete() {
        this.status = CampaignStatus.COMPLETED;
    }

    public void incrementSent() {
        this.totalSent++;
        updateAcceptanceRate();
    }

    public void incrementAccepted() {
        this.totalAccepted++;
        updateAcceptanceRate();
    }

    public void incrementReplied() {
        this.totalReplied++;
    }

    private void updateAcceptanceRate() {
        if (totalSent > 0) {
            this.acceptanceRate = (double) totalAccepted / totalSent * 100;
        }
    }

    public enum CampaignStatus {
        DRAFT,
        ACTIVE,
        PAUSED,
        COMPLETED
    }
}
