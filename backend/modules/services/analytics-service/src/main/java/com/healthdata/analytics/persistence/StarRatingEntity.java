package com.healthdata.analytics.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.UUID;

/**
 * Star Rating Entity
 *
 * Stores Medicare STAR rating performance data.
 */
@Entity
@Table(name = "star_ratings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StarRatingEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "measure_id", nullable = false, length = 128)
    private String measureId;

    @Column(name = "measure_name", nullable = false, length = 255)
    private String measureName;

    @Column(name = "measure_domain", nullable = false, length = 100)
    private String measureDomain;

    @Column(name = "rating_year", nullable = false)
    private Integer ratingYear;

    @Column(name = "rating_value", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingValue;

    @Column(name = "performance_score", precision = 6, scale = 2)
    private BigDecimal performanceScore;

    @Column(name = "benchmark", precision = 6, scale = 2)
    private BigDecimal benchmark;

    @Column(name = "percentile_rank", precision = 5, scale = 2)
    private BigDecimal percentileRank;

    @Column(name = "measurement_period_start")
    private LocalDate measurementPeriodStart;

    @Column(name = "measurement_period_end")
    private LocalDate measurementPeriodEnd;

    @Column(name = "data_source", length = 100)
    private String dataSource;

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
    }
}
