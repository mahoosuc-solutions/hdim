package com.healthdata.agentvalidation.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.healthdata.agentvalidation.domain.enums.EvaluationMetricType;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Represents an individual test scenario within a test suite.
 * Each test case defines a specific user message, context data,
 * and expected evaluation metrics.
 */
@Entity
@Table(name = "test_cases", indexes = {
    @Index(name = "idx_test_cases_suite", columnList = "test_suite_id"),
    @Index(name = "idx_test_cases_status", columnList = "status"),
    @Index(name = "idx_test_cases_golden_response", columnList = "golden_response_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_suite_id", nullable = false)
    @JsonIgnore  // Prevent lazy loading issues during serialization
    private TestSuite testSuite;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    /**
     * The user message that initiates the agent interaction.
     */
    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    /**
     * Context data for the test scenario (patient ID, tenant ID, etc.)
     * Stored as JSONB for flexible nested data.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", columnDefinition = "jsonb")
    private Map<String, Object> contextData;

    /**
     * Expected behavior descriptions for validation.
     * Can include expected tool calls, response patterns, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_behavior", columnDefinition = "jsonb")
    private Map<String, Object> expectedBehavior;

    /**
     * Required metrics to evaluate for this test case.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "test_case_required_metrics",
        joinColumns = @JoinColumn(name = "test_case_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type")
    @Builder.Default
    private Set<EvaluationMetricType> requiredMetrics = new HashSet<>();

    /**
     * Threshold values for each metric.
     * Key: metric type name, Value: minimum acceptable score.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metric_thresholds", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, BigDecimal> metricThresholds = new HashMap<>();

    /**
     * Reference to the golden response for regression testing.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golden_response_id")
    @JsonIgnore  // Prevent lazy loading issues during serialization; use dedicated endpoint
    private GoldenResponse goldenResponse;

    /**
     * Whether clinical safety checks should be performed.
     */
    @Column(name = "clinical_safety_check")
    @Builder.Default
    private boolean clinicalSafetyCheck = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TestStatus status = TestStatus.PENDING;

    /**
     * Priority for test execution order (lower = higher priority).
     */
    @Column(name = "execution_priority")
    @Builder.Default
    private int executionPriority = 100;

    /**
     * Tags for categorization and filtering.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "test_case_tags",
        joinColumns = @JoinColumn(name = "test_case_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "version")
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Get the threshold for a specific metric, defaulting to 0.70 if not set.
     */
    public BigDecimal getThresholdForMetric(EvaluationMetricType metricType) {
        return metricThresholds.getOrDefault(metricType.name(), new BigDecimal("0.70"));
    }

    /**
     * Check if a metric score passes its threshold.
     */
    public boolean passesThreshold(EvaluationMetricType metricType, BigDecimal score) {
        BigDecimal threshold = getThresholdForMetric(metricType);
        return score.compareTo(threshold) >= 0;
    }
}
