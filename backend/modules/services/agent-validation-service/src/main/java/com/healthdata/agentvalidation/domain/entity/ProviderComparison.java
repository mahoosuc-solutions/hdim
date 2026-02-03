package com.healthdata.agentvalidation.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records A/B testing results comparing multiple LLM providers
 * for the same test case.
 */
@Entity
@Table(name = "provider_comparisons", indexes = {
    @Index(name = "idx_provider_comparisons_test_case", columnList = "test_case_id"),
    @Index(name = "idx_provider_comparisons_best_quality", columnList = "best_quality_provider"),
    @Index(name = "idx_provider_comparisons_fastest", columnList = "fastest_provider"),
    @Index(name = "idx_provider_comparisons_executed_at", columnList = "executed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderComparison {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Results from each provider.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_results", columnDefinition = "jsonb")
    private List<ProviderResult> executionResults;

    /**
     * Provider with highest quality score.
     */
    @Column(name = "best_quality_provider")
    private String bestQualityProvider;

    /**
     * Best quality score achieved.
     */
    @Column(name = "best_quality_score", precision = 3, scale = 2)
    private BigDecimal bestQualityScore;

    /**
     * Provider with fastest response time.
     */
    @Column(name = "fastest_provider")
    private String fastestProvider;

    /**
     * Fastest response time in milliseconds.
     */
    @Column(name = "fastest_latency_ms")
    private Long fastestLatencyMs;

    /**
     * Provider with lowest cost.
     */
    @Column(name = "cheapest_provider")
    private String cheapestProvider;

    /**
     * Lowest cost achieved.
     */
    @Column(name = "cheapest_cost", precision = 10, scale = 6)
    private BigDecimal cheapestCost;

    /**
     * Cost estimates per provider.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cost_estimates", columnDefinition = "jsonb")
    private Map<String, BigDecimal> costEstimates;

    /**
     * Generated recommendation based on comparison.
     */
    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    /**
     * Statistical analysis of the comparison.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "statistical_analysis", columnDefinition = "jsonb")
    private Map<String, Object> statisticalAnalysis;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "version")
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = Instant.now();
        }
    }

    /**
     * Result from a single provider.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProviderResult {
        private String provider;
        private String modelVersion;
        private String response;
        private BigDecimal evaluationScore;
        private Map<String, BigDecimal> metricScores;
        private Long latencyMs;
        private Integer inputTokens;
        private Integer outputTokens;
        private BigDecimal estimatedCost;
        private boolean success;
        private String errorMessage;
        private String traceId;
    }

    /**
     * Determine the best overall provider based on weighted criteria.
     */
    public String determineBestOverallProvider(
            BigDecimal qualityWeight,
            BigDecimal latencyWeight,
            BigDecimal costWeight) {

        if (executionResults == null || executionResults.isEmpty()) {
            return null;
        }

        String bestProvider = null;
        BigDecimal bestScore = BigDecimal.ZERO;

        for (ProviderResult result : executionResults) {
            if (!result.isSuccess()) continue;

            // Normalize scores (0-1)
            BigDecimal normalizedQuality = result.getEvaluationScore();
            BigDecimal normalizedLatency = normalizeLatency(result.getLatencyMs());
            BigDecimal normalizedCost = normalizeCost(result.getEstimatedCost());

            // Calculate weighted score
            BigDecimal weightedScore = normalizedQuality.multiply(qualityWeight)
                .add(normalizedLatency.multiply(latencyWeight))
                .add(normalizedCost.multiply(costWeight));

            if (weightedScore.compareTo(bestScore) > 0) {
                bestScore = weightedScore;
                bestProvider = result.getProvider();
            }
        }

        return bestProvider;
    }

    private BigDecimal normalizeLatency(Long latencyMs) {
        // Lower latency = higher score (inverse normalization)
        // Assuming max acceptable latency is 10000ms
        if (latencyMs == null || latencyMs <= 0) return BigDecimal.ZERO;
        BigDecimal maxLatency = new BigDecimal("10000");
        BigDecimal normalized = BigDecimal.ONE.subtract(
            new BigDecimal(latencyMs).divide(maxLatency, 2, java.math.RoundingMode.HALF_UP)
        );
        return normalized.max(BigDecimal.ZERO);
    }

    private BigDecimal normalizeCost(BigDecimal cost) {
        // Lower cost = higher score (inverse normalization)
        // Assuming max acceptable cost is $0.10
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ONE;
        BigDecimal maxCost = new BigDecimal("0.10");
        BigDecimal normalized = BigDecimal.ONE.subtract(
            cost.divide(maxCost, 2, java.math.RoundingMode.HALF_UP)
        );
        return normalized.max(BigDecimal.ZERO);
    }
}
