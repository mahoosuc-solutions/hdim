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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "cost_daily_summary",
    indexes = {
        @Index(name = "idx_cost_daily_summary_tenant_date", columnList = "tenant_id,summary_date"),
        @Index(name = "idx_cost_daily_summary_service_date", columnList = "service_id,summary_date")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostDailySummaryEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "service_id", nullable = false, length = 128)
    private String serviceId;

    @Column(name = "feature_key", length = 128)
    private String featureKey;

    @Column(name = "total_cost", nullable = false, precision = 16, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "sample_count", nullable = false)
    private Long sampleCount;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
