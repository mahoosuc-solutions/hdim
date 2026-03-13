package com.healthdata.caregap.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "star_rating_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StarRatingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "snapshot_granularity", nullable = false, length = 20)
    private String snapshotGranularity;

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

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @PrePersist
    void onCreate() {
        if (capturedAt == null) {
            capturedAt = Instant.now();
        }
    }
}
