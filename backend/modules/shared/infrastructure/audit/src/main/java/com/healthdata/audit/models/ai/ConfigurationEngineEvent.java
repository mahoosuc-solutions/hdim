package com.healthdata.audit.models.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Audit event for configuration engine changes.
 * 
 * Tracks:
 * - Runtime configuration changes
 * - Dynamic pool resizing
 * - Thread pool adjustments
 * - Kafka consumer scaling
 * - Tenant-specific overrides
 * - Auto-scaling triggers
 * 
 * Enables:
 * - Real-time monitoring of configuration changes
 * - Change correlation with performance metrics
 * - Compliance auditing for production changes
 * - Troubleshooting configuration-related incidents
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationEngineEvent {

    @JsonProperty("eventId")
    private UUID eventId;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("eventType")
    @Builder.Default
    private String eventType = "CONFIGURATION_ENGINE_CHANGE";

    // Change Identity
    @JsonProperty("changeId")
    private UUID changeId; // Unique identifier for this configuration change

    @JsonProperty("changeType")
    private ChangeType changeType;

    @JsonProperty("changeSource")
    private ChangeSource changeSource;

    @JsonProperty("triggeredBy")
    private String triggeredBy; // User ID, AI agent ID, or "SYSTEM"

    // Configuration Details
    @JsonProperty("serviceName")
    private String serviceName; // e.g., "quality-measure-service", "patient-service"

    @JsonProperty("configurationScope")
    private ConfigurationScope configurationScope;

    @JsonProperty("resourceType")
    private String resourceType; // e.g., "HikariCP", "ThreadPoolExecutor", "Kafka"

    @JsonProperty("resourceId")
    private String resourceId; // Specific resource instance

    @JsonProperty("configKey")
    private String configKey; // e.g., "spring.datasource.hikari.maximum-pool-size"

    @JsonProperty("previousValue")
    private Object previousValue;

    @JsonProperty("newValue")
    private Object newValue;

    @JsonProperty("effectiveValue")
    private Object effectiveValue; // Actual value after overrides/constraints

    // Change Context
    @JsonProperty("reason")
    private String reason; // Human/AI explanation for the change

    @JsonProperty("triggerMetrics")
    private Map<String, Object> triggerMetrics; // Metrics that triggered the change

    @JsonProperty("expectedImpact")
    private String expectedImpact;

    @JsonProperty("aiRecommendationId")
    private UUID aiRecommendationId; // Link to AIAgentDecisionEvent if AI-driven

    // Change Execution
    @JsonProperty("executionStatus")
    private ExecutionStatus executionStatus;

    @JsonProperty("appliedAt")
    private Instant appliedAt;

    @JsonProperty("validationStatus")
    private ValidationStatus validationStatus;

    @JsonProperty("validationMetrics")
    private Map<String, Object> validationMetrics; // Post-change metrics

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("rollbackRequired")
    private Boolean rollbackRequired;

    @JsonProperty("rolledBackAt")
    private Instant rolledBackAt;

    // Performance Impact
    @JsonProperty("performanceImpact")
    private PerformanceImpact performanceImpact;

    // Compliance & Tracing
    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("changeApprovalId")
    private UUID changeApprovalId; // For production changes requiring approval

    @JsonProperty("environment")
    private String environment; // DEV, STAGING, PROD

    /**
     * Types of configuration changes
     */
    public enum ChangeType {
        POOL_SIZE_CHANGE,
        TIMEOUT_CHANGE,
        THREAD_POOL_CHANGE,
        KAFKA_CONSUMER_SCALING,
        KAFKA_PARTITION_CHANGE,
        QUEUE_SIZE_CHANGE,
        RETRY_POLICY_CHANGE,
        CIRCUIT_BREAKER_CHANGE,
        RATE_LIMIT_CHANGE,
        CACHE_CONFIGURATION_CHANGE,
        TENANT_OVERRIDE_APPLIED,
        TENANT_OVERRIDE_REMOVED,
        FEATURE_FLAG_CHANGE,
        AUTO_SCALING_POLICY_CHANGE
    }

    /**
     * Source of the configuration change
     */
    public enum ChangeSource {
        USER_MANUAL,          // User made the change via UI/API
        AI_RECOMMENDATION,    // AI agent recommended and user approved
        AI_AUTO_APPLIED,      // AI agent auto-applied (low risk changes)
        SYSTEM_AUTO_SCALING,  // Auto-scaling triggered by metrics
        SYSTEM_LOAD_BALANCER, // Load balancer adjusted config
        SCHEDULED_POLICY,     // Scheduled configuration policy
        EMERGENCY_OVERRIDE,   // Emergency manual override
        ROLLBACK              // Automatic rollback
    }

    /**
     * Scope of the configuration change
     */
    public enum ConfigurationScope {
        GLOBAL,              // Affects all tenants
        TENANT_SPECIFIC,     // Affects one tenant
        SERVICE_SPECIFIC,    // Affects one service
        CUSTOMER_TIER,       // Affects all customers in a tier
        ENVIRONMENT_SPECIFIC // Dev/Staging/Prod specific
    }

    /**
     * Execution status of the change
     */
    public enum ExecutionStatus {
        PENDING,
        IN_PROGRESS,
        APPLIED,
        FAILED,
        ROLLED_BACK,
        PARTIALLY_APPLIED
    }

    /**
     * Validation status after applying the change
     */
    public enum ValidationStatus {
        NOT_VALIDATED,
        VALIDATING,
        VALIDATION_PASSED,
        VALIDATION_FAILED,
        VALIDATION_WARNING,
        VALIDATION_TIMEOUT
    }

    /**
     * Performance impact assessment
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PerformanceImpact {
        @JsonProperty("latencyChangePercent")
        private Double latencyChangePercent; // +/- percentage

        @JsonProperty("throughputChangePercent")
        private Double throughputChangePercent;

        @JsonProperty("errorRateChangePercent")
        private Double errorRateChangePercent;

        @JsonProperty("resourceUtilizationChange")
        private Map<String, Double> resourceUtilizationChange; // CPU, memory, etc.

        @JsonProperty("impactSeverity")
        private ImpactSeverity impactSeverity;

        @JsonProperty("observationWindowMinutes")
        private Integer observationWindowMinutes; // How long we observed the impact
    }

    public enum ImpactSeverity {
        POSITIVE_SIGNIFICANT,  // >20% improvement
        POSITIVE_MINOR,        // 5-20% improvement
        NEUTRAL,               // <5% change
        NEGATIVE_MINOR,        // 5-20% degradation
        NEGATIVE_SIGNIFICANT,  // >20% degradation
        CRITICAL               // Service degradation requiring immediate action
    }

    /**
     * Builder initialization
     */
    public static class ConfigurationEngineEventBuilder {
        public ConfigurationEngineEvent build() {
            if (eventId == null) {
                eventId = UUID.randomUUID();
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            if (changeId == null) {
                changeId = UUID.randomUUID();
            }
            return new ConfigurationEngineEvent(
                eventId, timestamp, tenantId, eventType, changeId, changeType,
                changeSource, triggeredBy, serviceName, configurationScope,
                resourceType, resourceId, configKey, previousValue, newValue,
                effectiveValue, reason, triggerMetrics, expectedImpact,
                aiRecommendationId, executionStatus, appliedAt, validationStatus,
                validationMetrics, errorMessage, rollbackRequired, rolledBackAt,
                performanceImpact, requestId, correlationId, changeApprovalId,
                environment
            );
        }
    }
}
