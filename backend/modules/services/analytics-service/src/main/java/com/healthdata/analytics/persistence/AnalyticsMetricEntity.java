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
 * Analytics Metric Entity
 *
 * Stores time-series performance metrics.
 */
@Entity
@Table(name = "analytics_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsMetricEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "metric_name", nullable = false, length = 255)
    private String metricName;

    @Column(name = "metric_category", nullable = false, length = 50)
    private String metricCategory;

    @Column(name = "metric_value", nullable = false, precision = 18, scale = 6)
    private BigDecimal metricValue;

    @Column(name = "metric_unit", length = 50)
    private String metricUnit;

    @Column(name = "dimension_1", length = 128)
    private String dimension1;

    @Column(name = "dimension_2", length = 128)
    private String dimension2;

    @Column(name = "dimension_3", length = 128)
    private String dimension3;

    @Column(name = "measurement_date", nullable = false)
    private LocalDate measurementDate;

    @Column(name = "measurement_period", nullable = false, length = 20)
    private String measurementPeriod;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }
}
