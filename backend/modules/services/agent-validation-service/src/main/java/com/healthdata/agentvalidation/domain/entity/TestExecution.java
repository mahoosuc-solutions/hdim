package com.healthdata.agentvalidation.domain.entity;

import com.healthdata.agentvalidation.domain.enums.HarmLevel;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Records the results of executing a test case.
 * Captures the agent response, evaluation metrics, reflection results,
 * and correlation with Jaeger traces.
 */
@Entity
@Table(name = "test_executions", indexes = {
    @Index(name = "idx_test_executions_test_case", columnList = "test_case_id"),
    @Index(name = "idx_test_executions_tenant", columnList = "tenant_id"),
    @Index(name = "idx_test_executions_trace", columnList = "trace_id"),
    @Index(name = "idx_test_executions_status", columnList = "status"),
    @Index(name = "idx_test_executions_executed_at", columnList = "executed_at"),
    @Index(name = "idx_test_executions_provider", columnList = "llm_provider")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * The full agent response text.
     */
    @Column(name = "agent_response", columnDefinition = "TEXT")
    private String agentResponse;

    /**
     * OpenTelemetry trace ID for Jaeger correlation.
     */
    @Column(name = "trace_id", length = 64)
    private String traceId;

    /**
     * The LLM provider used for this execution.
     */
    @Column(name = "llm_provider")
    private String llmProvider;

    /**
     * Overall evaluation score (0.0 - 1.0).
     */
    @Column(name = "evaluation_score", precision = 3, scale = 2)
    private BigDecimal evaluationScore;

    /**
     * Individual metric results.
     * Key: metric type name, Value: score and metadata.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metric_results", columnDefinition = "jsonb")
    private Map<String, MetricResult> metricResults;

    /**
     * Agent self-reflection results.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reflection_result", columnDefinition = "jsonb")
    private ReflectionResult reflectionResult;

    /**
     * Regression comparison results (if golden response exists).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "regression_result", columnDefinition = "jsonb")
    private RegressionResult regressionResult;

    /**
     * Whether the test passed all thresholds.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean passed = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TestStatus status = TestStatus.PENDING;

    /**
     * QA review status if flagged for review.
     */
    @Column(name = "qa_review_status")
    private String qaReviewStatus;

    /**
     * QA reviewer's comments.
     */
    @Column(name = "qa_review_comments", columnDefinition = "TEXT")
    private String qaReviewComments;

    /**
     * ID of the QA reviewer.
     */
    @Column(name = "qa_reviewer_id")
    private String qaReviewerId;

    /**
     * When QA review was completed.
     */
    @Column(name = "qa_reviewed_at")
    private Instant qaReviewedAt;

    /**
     * Execution duration in milliseconds.
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * Token usage for this execution.
     */
    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    /**
     * Estimated cost in USD.
     */
    @Column(name = "estimated_cost", precision = 10, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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
     * Nested class for individual metric results.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricResult {
        private String metricType;
        private BigDecimal score;
        private BigDecimal threshold;
        private boolean passed;
        private String reason;
        private Map<String, Object> metadata;
    }

    /**
     * Nested class for reflection results.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReflectionResult {
        private BigDecimal confidenceLevel;
        private String toolSelectionJustification;
        private String clinicalSafetyCheck;
        private String informationCompleteness;
        private HarmLevel potentialHarmLevel;
        private BigDecimal calibrationDelta;
        private boolean miscalibrated;
        private Map<String, Object> rawReflection;
    }

    /**
     * Nested class for regression comparison results.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegressionResult {
        private UUID goldenResponseId;
        private BigDecimal semanticSimilarity;
        private BigDecimal qualityDelta;
        private boolean regressionDetected;
        private String comparisonDetails;
    }

    /**
     * Check if this execution should be flagged for QA review.
     */
    public boolean shouldFlagForQaReview(BigDecimal scoreThreshold,
                                          BigDecimal calibrationThreshold) {
        // Flag if overall score is below threshold
        if (evaluationScore != null && evaluationScore.compareTo(scoreThreshold) < 0) {
            return true;
        }

        // Flag if confidence is miscalibrated
        if (reflectionResult != null && reflectionResult.isMiscalibrated()) {
            return true;
        }

        // Flag if potential harm level is MEDIUM or HIGH
        if (reflectionResult != null &&
            (reflectionResult.getPotentialHarmLevel() == HarmLevel.MEDIUM ||
             reflectionResult.getPotentialHarmLevel() == HarmLevel.HIGH)) {
            return true;
        }

        // Flag if regression detected
        if (regressionResult != null && regressionResult.isRegressionDetected()) {
            return true;
        }

        return false;
    }
}
