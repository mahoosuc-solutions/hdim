package com.healthdata.audit.entity.ai;

import com.healthdata.audit.models.ai.ConfigurationEngineEvent;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity for configuration engine change events.
 * 
 * Stores runtime configuration changes for:
 * - Change tracking and history
 * - Performance impact analysis
 * - Rollback capabilities
 * - Compliance auditing
 */
@Entity
@Table(name = "configuration_engine_events", indexes = {
    @Index(name = "idx_config_change_tenant_timestamp", columnList = "tenant_id,timestamp"),
    @Index(name = "idx_config_change_service", columnList = "service_name,timestamp"),
    @Index(name = "idx_config_change_type", columnList = "change_type,timestamp"),
    @Index(name = "idx_config_change_status", columnList = "execution_status,timestamp"),
    @Index(name = "idx_config_change_source", columnList = "change_source,timestamp"),
    @Index(name = "idx_config_change_correlation", columnList = "correlation_id"),
    @Index(name = "idx_config_change_ai_recommendation", columnList = "ai_recommendation_id"),
    @Index(name = "idx_config_change_environment", columnList = "environment,timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurationEngineEventEntity {

    @Id
    @Column(name = "event_id", columnDefinition = "uuid")
    private UUID eventId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "change_id", columnDefinition = "uuid", nullable = false)
    private UUID changeId;

    // Change Identity
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 50)
    private ConfigurationEngineEvent.ChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_source", length = 50)
    private ConfigurationEngineEvent.ChangeSource changeSource;

    @Column(name = "triggered_by", length = 255)
    private String triggeredBy;

    // Configuration Details
    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "configuration_scope", length = 50)
    private ConfigurationEngineEvent.ConfigurationScope configurationScope;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "config_key", length = 255)
    private String configKey;

    @Column(name = "previous_value", length = 1000)
    private String previousValue;

    @Column(name = "new_value", length = 1000)
    private String newValue;

    @Column(name = "effective_value", length = 1000)
    private String effectiveValue;

    // Change Context
    @Column(columnDefinition = "TEXT")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_metrics", columnDefinition = "jsonb")
    private Map<String, Object> triggerMetrics;

    @Column(name = "expected_impact", columnDefinition = "TEXT")
    private String expectedImpact;

    @Column(name = "ai_recommendation_id", columnDefinition = "uuid")
    private UUID aiRecommendationId;

    // Change Execution
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", length = 30)
    private ConfigurationEngineEvent.ExecutionStatus executionStatus;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", length = 30)
    private ConfigurationEngineEvent.ValidationStatus validationStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_metrics", columnDefinition = "jsonb")
    private Map<String, Object> validationMetrics;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "rollback_required")
    private Boolean rollbackRequired;

    @Column(name = "rolled_back_at")
    private Instant rolledBackAt;

    // Performance Impact
    @Column(name = "latency_change_percent")
    private Double latencyChangePercent;

    @Column(name = "throughput_change_percent")
    private Double throughputChangePercent;

    @Column(name = "error_rate_change_percent")
    private Double errorRateChangePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_severity", length = 30)
    private ConfigurationEngineEvent.ImpactSeverity impactSeverity;

    @Column(name = "observation_window_minutes")
    private Integer observationWindowMinutes;

    // Compliance & Tracing
    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "change_approval_id", columnDefinition = "uuid")
    private UUID changeApprovalId;

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
        if (changeId == null) {
            changeId = UUID.randomUUID();
        }
    }

    /**
     * Convert from domain model to entity.
     */
    public static ConfigurationEngineEventEntity fromDomainModel(ConfigurationEngineEvent event) {
        ConfigurationEngineEventEntityBuilder builder = ConfigurationEngineEventEntity.builder()
            .eventId(event.getEventId())
            .timestamp(event.getTimestamp())
            .tenantId(event.getTenantId())
            .changeId(event.getChangeId())
            .changeType(event.getChangeType())
            .changeSource(event.getChangeSource())
            .triggeredBy(event.getTriggeredBy())
            .serviceName(event.getServiceName())
            .configurationScope(event.getConfigurationScope())
            .resourceType(event.getResourceType())
            .resourceId(event.getResourceId())
            .configKey(event.getConfigKey())
            .previousValue(String.valueOf(event.getPreviousValue()))
            .newValue(String.valueOf(event.getNewValue()))
            .effectiveValue(String.valueOf(event.getEffectiveValue()))
            .reason(event.getReason())
            .triggerMetrics(event.getTriggerMetrics())
            .expectedImpact(event.getExpectedImpact())
            .aiRecommendationId(event.getAiRecommendationId())
            .executionStatus(event.getExecutionStatus())
            .appliedAt(event.getAppliedAt())
            .validationStatus(event.getValidationStatus())
            .validationMetrics(event.getValidationMetrics())
            .errorMessage(event.getErrorMessage())
            .rollbackRequired(event.getRollbackRequired())
            .rolledBackAt(event.getRolledBackAt())
            .requestId(event.getRequestId())
            .correlationId(event.getCorrelationId())
            .changeApprovalId(event.getChangeApprovalId())
            .environment(event.getEnvironment());

        // Performance Impact
        if (event.getPerformanceImpact() != null) {
            builder.latencyChangePercent(event.getPerformanceImpact().getLatencyChangePercent())
                .throughputChangePercent(event.getPerformanceImpact().getThroughputChangePercent())
                .errorRateChangePercent(event.getPerformanceImpact().getErrorRateChangePercent())
                .impactSeverity(event.getPerformanceImpact().getImpactSeverity())
                .observationWindowMinutes(event.getPerformanceImpact().getObservationWindowMinutes());
        }

        return builder.build();
    }
}
