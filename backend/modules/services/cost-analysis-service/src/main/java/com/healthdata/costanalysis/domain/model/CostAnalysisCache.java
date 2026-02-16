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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "cost_analysis_cache",
    indexes = {
        @Index(name = "idx_cache_tenant_type_period", columnList = "tenant_id,analysis_type,analysis_period"),
        @Index(name = "idx_cache_expires_at", columnList = "expires_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostAnalysisCache {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "analysis_type", nullable = false, length = 64)
    private String analysisType;

    @Column(name = "analysis_period", nullable = false, length = 32)
    private String analysisPeriod;

    @Column(name = "service_name", length = 128)
    private String serviceName;

    @Column(name = "result_data", nullable = false, columnDefinition = "TEXT")
    private String resultData;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "cache_hits", nullable = false)
    private Integer cacheHits = 0;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plusSeconds(300);
        }
        if (cacheHits == null) {
            cacheHits = 0;
        }
    }
}
