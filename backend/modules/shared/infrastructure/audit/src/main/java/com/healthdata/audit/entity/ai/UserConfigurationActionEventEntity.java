package com.healthdata.audit.entity.ai;

import com.healthdata.audit.models.ai.UserConfigurationActionEvent;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity for user configuration action events.
 * 
 * Stores user-initiated configuration actions for:
 * - User activity auditing
 * - Compliance tracking
 * - UX analytics
 * - AI training data
 */
@Entity
@Table(name = "user_configuration_action_events", indexes = {
    @Index(name = "idx_user_action_tenant_timestamp", columnList = "tenant_id,timestamp"),
    @Index(name = "idx_user_action_user", columnList = "user_id,timestamp"),
    @Index(name = "idx_user_action_type", columnList = "action_type,timestamp"),
    @Index(name = "idx_user_action_status", columnList = "action_status,timestamp"),
    @Index(name = "idx_user_action_service", columnList = "service_name,timestamp"),
    @Index(name = "idx_user_action_ai_recommendation", columnList = "ai_recommendation_id"),
    @Index(name = "idx_user_action_correlation", columnList = "correlation_id"),
    @Index(name = "idx_user_action_environment", columnList = "environment,timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConfigurationActionEventEntity {

    @Id
    @Column(name = "event_id", columnDefinition = "uuid")
    private UUID eventId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    // User Identity
    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(length = 255)
    private String username;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    // Action Context
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50)
    private UserConfigurationActionEvent.ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_source", length = 50)
    private UserConfigurationActionEvent.ActionSource actionSource;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // Configuration Details
    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "configuration_key", length = 255)
    private String configurationKey;

    @Column(name = "requested_value", length = 1000)
    private String requestedValue;

    @Column(name = "applied_value", length = 1000)
    private String appliedValue;

    // AI Interaction
    @Column(name = "ai_recommendation_id", columnDefinition = "uuid")
    private UUID aiRecommendationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_recommendation_action", length = 50)
    private UserConfigurationActionEvent.AIRecommendationAction aiRecommendationAction;

    @Column(name = "natural_language_query", columnDefinition = "TEXT")
    private String naturalLanguageQuery;

    @Column(name = "ai_response_id", columnDefinition = "uuid")
    private UUID aiResponseId;

    @Column(name = "user_feedback_rating")
    private Integer userFeedbackRating;

    @Column(name = "user_feedback_comment", columnDefinition = "TEXT")
    private String userFeedbackComment;

    // Action Outcome
    @Enumerated(EnumType.STRING)
    @Column(name = "action_status", length = 30)
    private UserConfigurationActionEvent.ActionStatus actionStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "success_message", columnDefinition = "TEXT")
    private String successMessage;

    // Impact Assessment (stored as JSONB)
    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "affected_tenants")
    private Integer affectedTenants;

    @Column(name = "affected_users")
    private Integer affectedUsers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "estimated_impact", columnDefinition = "jsonb")
    private Map<String, String> estimatedImpact;

    @Column(name = "requires_approval")
    private Boolean requiresApproval;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30)
    private UserConfigurationActionEvent.ApprovalStatus approvalStatus;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    // Compliance & Tracing
    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "change_ticket_id", length = 100)
    private String changeTicketId;

    @Column(length = 20)
    private String environment;

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
    public static UserConfigurationActionEventEntity fromDomainModel(
            UserConfigurationActionEvent event) {
        UserConfigurationActionEventEntityBuilder builder = UserConfigurationActionEventEntity.builder()
            .eventId(event.getEventId())
            .timestamp(event.getTimestamp())
            .tenantId(event.getTenantId())
            .userId(event.getUserId())
            .username(event.getUsername())
            .userRole(event.getUserRole())
            .userEmail(event.getUserEmail())
            .actionType(event.getActionType())
            .actionSource(event.getActionSource())
            .sessionId(event.getSessionId())
            .ipAddress(event.getIpAddress())
            .userAgent(event.getUserAgent())
            .serviceName(event.getServiceName())
            .resourceType(event.getResourceType())
            .resourceId(event.getResourceId())
            .configurationKey(event.getConfigurationKey())
            .requestedValue(String.valueOf(event.getRequestedValue()))
            .appliedValue(String.valueOf(event.getAppliedValue()))
            .aiRecommendationId(event.getAiRecommendationId())
            .aiRecommendationAction(event.getAiRecommendationAction())
            .naturalLanguageQuery(event.getNaturalLanguageQuery())
            .aiResponseId(event.getAiResponseId())
            .userFeedbackRating(event.getUserFeedbackRating())
            .userFeedbackComment(event.getUserFeedbackComment())
            .actionStatus(event.getActionStatus())
            .errorMessage(event.getErrorMessage())
            .successMessage(event.getSuccessMessage())
            .requiresApproval(event.getRequiresApproval())
            .approvalStatus(event.getApprovalStatus())
            .approvedBy(event.getApprovedBy())
            .approvedAt(event.getApprovedAt())
            .requestId(event.getRequestId())
            .correlationId(event.getCorrelationId())
            .changeTicketId(event.getChangeTicketId())
            .environment(event.getEnvironment());

        // Impact Assessment
        if (event.getImpactAssessment() != null) {
            builder.riskLevel(event.getImpactAssessment().getRiskLevel())
                .affectedTenants(event.getImpactAssessment().getAffectedTenants())
                .affectedUsers(event.getImpactAssessment().getAffectedUsers())
                .estimatedImpact(event.getImpactAssessment().getEstimatedImpact());
        }

        return builder.build();
    }
}
