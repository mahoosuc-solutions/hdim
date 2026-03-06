package com.healthdata.events.intelligence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Aggregated tenant-level trust projection for dashboards.
 */
@Entity
@Table(name = "intelligence_tenant_trust_projection")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "tenantId")
public class IntelligenceTenantTrustProjectionEntity {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "total_open_findings", nullable = false)
    private long totalOpenFindings;

    @Column(name = "high_severity_open_findings", nullable = false)
    private long highSeverityOpenFindings;

    @Column(name = "consistency_open_findings", nullable = false)
    private long consistencyOpenFindings;

    @Column(name = "data_completeness_open_findings", nullable = false)
    private long dataCompletenessOpenFindings;

    @Column(name = "temporal_open_findings", nullable = false)
    private long temporalOpenFindings;

    @Column(name = "trust_score", nullable = false)
    private int trustScore;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @PrePersist
    void onCreate() {
        if (lastUpdatedAt == null) {
            lastUpdatedAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        lastUpdatedAt = Instant.now();
    }
}
