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
 * Correlates Jaeger trace data with test execution results.
 * Enables trace-based debugging for failed tests.
 */
@Entity
@Table(name = "trace_outcomes", indexes = {
    @Index(name = "idx_trace_outcomes_trace_id", columnList = "trace_id"),
    @Index(name = "idx_trace_outcomes_execution", columnList = "test_execution_id"),
    @Index(name = "idx_trace_outcomes_passed", columnList = "evaluation_passed"),
    @Index(name = "idx_trace_outcomes_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * OpenTelemetry trace ID from Jaeger.
     */
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_execution_id", nullable = false)
    private TestExecution testExecution;

    /**
     * Total trace duration in milliseconds.
     */
    @Column(name = "total_duration_ms")
    private Long totalDurationMs;

    /**
     * Number of spans with errors.
     */
    @Column(name = "error_count")
    @Builder.Default
    private int errorCount = 0;

    /**
     * Number of tool invocation spans.
     */
    @Column(name = "tool_invocation_count")
    @Builder.Default
    private int toolInvocationCount = 0;

    /**
     * Number of LLM API call spans.
     */
    @Column(name = "llm_call_count")
    @Builder.Default
    private int llmCallCount = 0;

    /**
     * Whether the associated evaluation passed.
     */
    @Column(name = "evaluation_passed")
    private Boolean evaluationPassed;

    /**
     * Overall evaluation score.
     */
    @Column(name = "evaluation_score", precision = 3, scale = 2)
    private BigDecimal evaluationScore;

    /**
     * Summary of key spans for quick reference.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "span_summary", columnDefinition = "jsonb")
    private List<SpanInfo> spanSummary;

    /**
     * Tool invocations extracted from trace.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_invocations", columnDefinition = "jsonb")
    private List<ToolInvocation> toolInvocations;

    /**
     * LLM calls extracted from trace.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "llm_calls", columnDefinition = "jsonb")
    private List<LlmCall> llmCalls;

    /**
     * Errors extracted from trace spans.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trace_errors", columnDefinition = "jsonb")
    private List<TraceError> traceErrors;

    /**
     * URL to view full trace in Jaeger UI.
     */
    @Column(name = "jaeger_url")
    private String jaegerUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Information about a span in the trace.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpanInfo {
        private String spanId;
        private String operationName;
        private String serviceName;
        private Long durationMs;
        private boolean hasError;
        private Map<String, String> tags;
    }

    /**
     * Information about a tool invocation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolInvocation {
        private String toolName;
        private Long durationMs;
        private boolean success;
        private String inputSummary;
        private String outputSummary;
        private Map<String, Object> parameters;
    }

    /**
     * Information about an LLM API call.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LlmCall {
        private String provider;
        private String model;
        private Long durationMs;
        private Integer inputTokens;
        private Integer outputTokens;
        private boolean success;
        private String errorMessage;
    }

    /**
     * Information about an error in the trace.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TraceError {
        private String spanId;
        private String operationName;
        private String errorType;
        private String errorMessage;
        private String stackTrace;
    }
}
