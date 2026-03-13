package com.healthdata.caregap.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "star_rating_projections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StarRatingProjection {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "overall_rating", nullable = false, precision = 4, scale = 2)
    private BigDecimal overallRating;

    @Column(name = "rounded_rating", nullable = false, precision = 3, scale = 1)
    private BigDecimal roundedRating;

    @Column(name = "measure_count", nullable = false)
    private int measureCount;

    @Column(name = "open_gap_count", nullable = false)
    private int openGapCount;

    @Column(name = "closed_gap_count", nullable = false)
    private int closedGapCount;

    @Column(name = "quality_bonus_eligible", nullable = false)
    private boolean qualityBonusEligible;

    @Column(name = "last_trigger_event", length = 255)
    private String lastTriggerEvent;

    @Column(name = "last_calculated_at")
    private Instant lastCalculatedAt;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private long version = 0L;

    @PrePersist
    @PreUpdate
    void touch() {
        lastCalculatedAt = Instant.now();
    }
}
