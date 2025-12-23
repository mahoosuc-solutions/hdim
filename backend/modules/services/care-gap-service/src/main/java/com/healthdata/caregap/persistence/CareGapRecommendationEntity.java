package com.healthdata.caregap.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Care Gap Recommendation Entity
 *
 * Stores recommended interventions for a care gap.
 */
@Entity
@Table(name = "care_gap_recommendations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareGapRecommendationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "care_gap_id", nullable = false)
    private UUID careGapId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "recommendation_type", nullable = false, length = 50)
    private String recommendationType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "action_required", nullable = false, columnDefinition = "TEXT")
    private String actionRequired;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "evidence_level", length = 20)
    private String evidenceLevel;

    @Column(name = "guideline_reference", columnDefinition = "TEXT")
    private String guidelineReference;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (priority == null) {
            priority = 0;
        }
    }
}
