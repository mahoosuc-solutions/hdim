package com.healthdata.analytics.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Metric Snapshot Entity
 *
 * Stores point-in-time snapshots of metrics for trend analysis and historical reporting.
 */
@Entity
@Table(name = "metric_snapshots",
       indexes = {
           @Index(name = "idx_snapshot_tenant", columnList = "tenant_id"),
           @Index(name = "idx_snapshot_type", columnList = "metric_type"),
           @Index(name = "idx_snapshot_date", columnList = "snapshot_date"),
           @Index(name = "idx_snapshot_tenant_type_date", columnList = "tenant_id, metric_type, snapshot_date")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricSnapshotEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType; // QUALITY_SCORE, RAF_SCORE, CARE_GAP_RATE, STAR_RATING

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(name = "metric_value", precision = 15, scale = 4)
    private BigDecimal metricValue;

    @Type(JsonBinaryType.class)
    @Column(name = "dimensions", columnDefinition = "jsonb")
    private Map<String, Object> dimensions; // e.g., measureId, populationId, etc.

    @Type(JsonBinaryType.class)
    @Column(name = "breakdown", columnDefinition = "jsonb")
    private Map<String, Object> breakdown; // detailed breakdown of the metric

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "sample_size")
    private Integer sampleSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        if (snapshotDate == null) {
            snapshotDate = LocalDate.now();
        }
    }
}
