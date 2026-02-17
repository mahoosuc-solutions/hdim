package com.healthdata.costanalysis.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "cost_tracking",
    indexes = {
        @Index(name = "idx_cost_tracking_service_time", columnList = "service_id,timestamp_utc"),
        @Index(name = "idx_cost_tracking_tenant_time", columnList = "tenant_id,timestamp_utc"),
        @Index(name = "idx_cost_tracking_metric_type", columnList = "metric_type")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostTrackingEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "metric_type", nullable = false, length = 64)
    private String metricType;

    @Column(name = "metric_value", nullable = false, precision = 16, scale = 4)
    private BigDecimal metricValue;

    @Column(name = "cost_amount", nullable = false, precision = 16, scale = 4)
    private BigDecimal costAmount;

    @Column(name = "timestamp_utc", nullable = false)
    private Instant timestampUtc;

    @Column(name = "service_id", nullable = false, length = 128)
    private String serviceId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "feature_key", length = 128)
    private String featureKey;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (timestampUtc == null) {
            timestampUtc = Instant.now();
        }
    }
}
