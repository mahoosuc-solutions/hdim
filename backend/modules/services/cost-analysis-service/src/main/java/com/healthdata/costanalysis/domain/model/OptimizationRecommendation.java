package com.healthdata.costanalysis.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "optimization_recommendation",
    indexes = {
        @Index(name = "idx_opt_tenant_status", columnList = "tenant_id,status"),
        @Index(name = "idx_opt_tenant_service", columnList = "tenant_id,service_name")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationRecommendation {

    public enum RecommendationType { RIGHT_SIZING, QUERY_OPTIMIZATION, CACHE_TUNING, INFRA_CONSOLIDATION }
    public enum RecommendationStatus { PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, REJECTED }
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum RiskLevel { LOW, MEDIUM, HIGH }
    public enum ImplementationEffort { LOW, MEDIUM, HIGH }

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false, length = 64)
    private RecommendationType recommendationType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_savings", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedSavings;

    @Column(name = "actual_savings", precision = 14, scale = 2)
    private BigDecimal actualSavings;

    @Column(name = "savings_currency", length = 8)
    private String savingsCurrency;

    @Column(name = "savings_timeframe", length = 32)
    private String savingsTimeframe;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "implementation_effort", nullable = false, length = 16)
    private ImplementationEffort implementationEffort;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RecommendationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Priority priority;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "implementation_date")
    private Instant implementationDate;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
