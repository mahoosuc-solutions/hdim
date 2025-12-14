package com.healthdata.agentbuilder.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent test session entity for sandbox testing.
 */
@Entity
@Table(name = "agent_test_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_configuration_id", nullable = false)
    private AgentConfiguration agentConfiguration;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false)
    private TestType testType;

    @Column(name = "test_scenario", columnDefinition = "TEXT")
    private String testScenario;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<TestMessage> messages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_invocations", columnDefinition = "jsonb")
    private List<ToolInvocation> toolInvocations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private TestMetrics metrics;

    @Column(name = "tester_feedback", columnDefinition = "TEXT")
    private String testerFeedback;

    @Column(name = "tester_rating")
    private Integer testerRating;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        startedAt = Instant.now();
        if (status == null) {
            status = TestStatus.IN_PROGRESS;
        }
        if (testType == null) {
            testType = TestType.INTERACTIVE;
        }
    }

    public enum TestType {
        INTERACTIVE,
        AUTOMATED,
        SCENARIO
    }

    public enum TestStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestMessage {
        private String role;
        private String content;
        private Instant timestamp;
        private Long latencyMs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolInvocation {
        private String toolName;
        private Map<String, Object> arguments;
        private String result;
        private boolean success;
        private Long durationMs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestMetrics {
        private int totalMessages;
        private int toolInvocations;
        private long totalTokens;
        private long avgResponseTimeMs;
        private int guardrailTriggers;
    }
}
