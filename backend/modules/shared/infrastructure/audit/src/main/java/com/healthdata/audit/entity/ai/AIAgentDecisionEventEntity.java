package com.healthdata.audit.entity.ai;

import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity for AI agent decision events.
 * 
 * Stores AI recommendations, reasoning, and outcomes for:
 * - Compliance auditing
 * - Decision replay and debugging
 * - AI model performance tracking
 * - Trust and transparency
 */
@Entity
@Table(name = "ai_agent_decision_events", indexes = {
    @Index(name = "idx_ai_decision_tenant_timestamp", columnList = "tenant_id,timestamp"),
    @Index(name = "idx_ai_decision_agent_type", columnList = "agent_type,timestamp"),
    @Index(name = "idx_ai_decision_type", columnList = "decision_type,timestamp"),
    @Index(name = "idx_ai_decision_outcome", columnList = "outcome,timestamp"),
    @Index(name = "idx_ai_decision_correlation", columnList = "correlation_id"),
    @Index(name = "idx_ai_decision_customer_tier", columnList = "customer_tier,timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIAgentDecisionEventEntity {

    @Id
    @Column(name = "event_id", columnDefinition = "uuid")
    private UUID eventId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    // AI Agent Identity
    @Column(name = "agent_id", length = 255)
    private String agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", length = 50)
    private AIAgentDecisionEvent.AgentType agentType;

    @Column(name = "agent_version", length = 50)
    private String agentVersion;

    @Column(name = "model_name", length = 100)
    private String modelName;

    // Decision Context
    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", length = 50)
    private AIAgentDecisionEvent.DecisionType decisionType;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    // Customer Profile
    @Column(name = "customer_tier", length = 50)
    private String customerTier;

    @Column(name = "patient_count")
    private Long patientCount;

    @Column(name = "provider_count")
    private Integer providerCount;

    @Column(name = "average_daily_messages")
    private Long averageDailyMessages;

    @Column(name = "traffic_tier", length = 20)
    private String trafficTier;

    // Decision Input (stored as JSONB for flexibility)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_metrics", columnDefinition = "jsonb")
    private Map<String, Object> inputMetrics;

    @Column(name = "user_query", columnDefinition = "TEXT")
    private String userQuery;

    // Decision Output
    @Column(name = "config_type", length = 255)
    private String configType;

    @Column(name = "current_value", length = 500)
    private String currentValue;

    @Column(name = "recommended_value", length = 500)
    private String recommendedValue;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "reasoning", columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "expected_impact", columnDefinition = "TEXT")
    private String expectedImpact;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private AIAgentDecisionEvent.RiskLevel riskLevel;

    // Decision Outcome
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AIAgentDecisionEvent.DecisionOutcome outcome;

    @Column(name = "applied_by_user")
    private Boolean appliedByUser;

    @Column(name = "user_feedback_rating")
    private Integer userFeedbackRating;

    @Column(name = "user_feedback_comment", columnDefinition = "TEXT")
    private String userFeedbackComment;

    @Column(name = "user_feedback_timestamp")
    private Instant userFeedbackTimestamp;

    // QA Review Tracking
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "review_status", length = 50)
    private String reviewStatus; // PENDING, APPROVED, REJECTED, FLAGGED

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    // Performance Metrics
    @Column(name = "inference_time_ms")
    private Long inferenceTimeMs;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "cost_estimate")
    private Double costEstimate;

    // Tracing
    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "previous_event_id", columnDefinition = "uuid")
    private UUID previousEventId;

    // Audit metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Convert from domain model to entity.
     */
    public static AIAgentDecisionEventEntity fromDomainModel(AIAgentDecisionEvent event) {
        AIAgentDecisionEventEntityBuilder builder = AIAgentDecisionEventEntity.builder()
            .eventId(event.getEventId())
            .timestamp(event.getTimestamp())
            .tenantId(event.getTenantId())
            .agentId(event.getAgentId())
            .agentType(event.getAgentType())
            .agentVersion(event.getAgentVersion())
            .modelName(event.getModelName())
            .decisionType(event.getDecisionType())
            .resourceType(event.getResourceType())
            .resourceId(event.getResourceId())
            .inputMetrics(event.getInputMetrics())
            .userQuery(event.getUserQuery())
            .confidenceScore(event.getConfidenceScore())
            .reasoning(event.getReasoning())
            .outcome(event.getOutcome())
            .appliedByUser(event.getAppliedByUser())
            .inferenceTimeMs(event.getInferenceTimeMs())
            .tokenCount(event.getTokenCount())
            .costEstimate(event.getCostEstimate())
            .requestId(event.getRequestId())
            .correlationId(event.getCorrelationId())
            .previousEventId(event.getPreviousEventId());

        // Customer profile
        if (event.getCustomerProfile() != null) {
            builder.customerTier(event.getCustomerProfile().getCustomerTier())
                .patientCount(event.getCustomerProfile().getPatientCount())
                .providerCount(event.getCustomerProfile().getProviderCount())
                .averageDailyMessages(event.getCustomerProfile().getAverageDailyMessages())
                .trafficTier(event.getCustomerProfile().getTrafficTier());
        }

        // Recommendation
        if (event.getRecommendation() != null) {
            builder.configType(event.getRecommendation().getConfigType())
                .currentValue(String.valueOf(event.getRecommendation().getCurrentValue()))
                .recommendedValue(String.valueOf(event.getRecommendation().getRecommendedValue()))
                .expectedImpact(event.getRecommendation().getExpectedImpact())
                .riskLevel(event.getRecommendation().getRiskLevel());
        }

        // User feedback
        if (event.getUserFeedback() != null) {
            builder.userFeedbackRating(event.getUserFeedback().getRating())
                .userFeedbackComment(event.getUserFeedback().getComment())
                .userFeedbackTimestamp(event.getUserFeedback().getTimestamp());
        }

        return builder.build();
    }
}
