package com.healthdata.predictive.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Issue #19: Population Health Insights Engine
 *
 * Tracks dismissed insights for provider panels.
 * Enables insight dismissal with reason tracking.
 */
@Entity
@Table(name = "insight_dismissals",
    indexes = {
        @Index(name = "idx_insight_dismissals_tenant_provider", columnList = "tenant_id, provider_id"),
        @Index(name = "idx_insight_dismissals_insight_key", columnList = "insight_key")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsightDismissalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    /**
     * The unique key for this insight type (e.g., "CARE_GAP_CLUSTER:COL")
     * Used to prevent showing the same insight again after dismissal
     */
    @Column(name = "insight_key", nullable = false)
    private String insightKey;

    /**
     * The original insight ID that was dismissed
     */
    @Column(name = "insight_id", nullable = false)
    private UUID insightId;

    /**
     * Reason for dismissal
     */
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    /**
     * User who dismissed the insight
     */
    @Column(name = "dismissed_by")
    private String dismissedBy;

    /**
     * When the insight was dismissed
     */
    @Column(name = "dismissed_at", nullable = false)
    private Instant dismissedAt;

    /**
     * How long the dismissal should last (null = permanent)
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Active flag (for soft delete)
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        if (dismissedAt == null) {
            dismissedAt = Instant.now();
        }
    }
}
