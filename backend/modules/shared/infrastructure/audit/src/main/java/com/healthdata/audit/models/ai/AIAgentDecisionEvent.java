package com.healthdata.audit.models.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit event for AI agent decisions and recommendations.
 * 
 * Tracks:
 * - Configuration recommendations from AI agents
 * - Pool sizing decisions
 * - Thread pool adjustments
 * - Kafka partition recommendations
 * - Customer tier classifications
 * - CQL measure evaluations
 * - Care gap identification decisions
 * - AI reasoning and confidence scores
 * 
 * Enables:
 * - Real-time monitoring of AI decisions
 * - Decision replay for troubleshooting
 * - Compliance auditing (SOC 2, HIPAA)
 * - Trust and transparency for AI recommendations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIAgentDecisionEvent {

    @JsonProperty("eventId")
    private UUID eventId;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("eventType")
    @Builder.Default
    private String eventType = "AI_AGENT_DECISION";

    // AI Agent Identity
    @JsonProperty("agentId")
    private String agentId; // e.g., "config-advisor-gpt4", "pool-optimizer-claude"

    @JsonProperty("agentType")
    private AgentType agentType;

    @JsonProperty("agentVersion")
    private String agentVersion;

    @JsonProperty("modelName")
    private String modelName; // e.g., "gpt-4-turbo", "claude-3.5-sonnet"

    // Decision Context
    @JsonProperty("decisionType")
    private DecisionType decisionType;

    @JsonProperty("resourceType")
    private String resourceType; // e.g., "HikariCP Pool", "Thread Pool Executor", "Kafka Partitions"

    @JsonProperty("resourceId")
    private String resourceId; // Specific resource being configured

    @JsonProperty("customerProfile")
    private CustomerProfile customerProfile;

    // Decision Input
    @JsonProperty("inputMetrics")
    private Map<String, Object> inputMetrics; // Current state: pool size, thread count, message rate, etc.

    @JsonProperty("historicalData")
    private JsonNode historicalData; // Time-series data fed to the model

    @JsonProperty("userQuery")
    private String userQuery; // Natural language query if applicable

    // Decision Output
    @JsonProperty("recommendation")
    private ConfigurationRecommendation recommendation;

    @JsonProperty("confidenceScore")
    private Double confidenceScore; // 0.0 - 1.0

    @JsonProperty("reasoning")
    private String reasoning; // AI-generated explanation

    @JsonProperty("alternativeOptions")
    private List<ConfigurationRecommendation> alternativeOptions;

    // Decision Outcome
    @JsonProperty("outcome")
    private DecisionOutcome outcome;

    @JsonProperty("appliedByUser")
    private Boolean appliedByUser;

    @JsonProperty("userFeedback")
    private UserFeedback userFeedback;

    // Performance Metrics
    @JsonProperty("inferenceTimeMs")
    private Long inferenceTimeMs;

    @JsonProperty("tokenCount")
    private Integer tokenCount; // For LLM calls

    @JsonProperty("costEstimate")
    private Double costEstimate; // USD cost of the AI call

    // Compliance & Tracing
    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("correlationId")
    private String correlationId; // For tracking multi-step decisions

    @JsonProperty("previousEventId")
    private UUID previousEventId; // For decision chains

    /**
     * Agent types supported by the system
     */
    public enum AgentType {
        CONFIGURATION_ADVISOR,    // General config recommendations
        POOL_OPTIMIZER,          // HikariCP pool sizing
        THREAD_POOL_OPTIMIZER,   // Thread pool configuration
        KAFKA_OPTIMIZER,         // Kafka partition/consumer tuning
        CUSTOMER_CLASSIFIER,     // Customer tier classification
        LOAD_PREDICTOR,         // Traffic prediction
        COST_OPTIMIZER,         // Cost vs performance optimization
        ANOMALY_DETECTOR,       // Detect configuration anomalies
        CQL_ENGINE,             // CQL clinical quality language engine
        CARE_GAP_IDENTIFIER,    // Care gap identification agent
        AI_AGENT,               // Generic AI agent (LLM-powered)
        TOOL_EXECUTION,         // AI agent tool execution
        GUARDRAIL_BLOCK,        // AI guardrail blocking response
        PHI_ACCESS,             // PHI data access by AI
        AGENT_EXECUTION,        // Full AI agent execution
        PREDICTIVE_ANALYTICS,   // Predictive analytics/ML models
        CONSENT_VALIDATOR,      // Consent validation and filtering
        CLINICAL_WORKFLOW       // Clinical workflow orchestration
    }

    /**
     * Types of decisions the AI can make
     */
    public enum DecisionType {
        POOL_SIZE_RECOMMENDATION,
        THREAD_POOL_ADJUSTMENT,
        KAFKA_PARTITION_RECOMMENDATION,
        CUSTOMER_TIER_CLASSIFICATION,
        TIMEOUT_CONFIGURATION,
        QUEUE_SIZE_RECOMMENDATION,
        CONSUMER_CONCURRENCY_RECOMMENDATION,
        RETENTION_POLICY_RECOMMENDATION,
        COMPRESSION_TYPE_RECOMMENDATION,
        BATCH_SIZE_RECOMMENDATION,
        AUTO_SCALING_POLICY,
        MEASURE_MET,              // CQL evaluation result - measure met
        MEASURE_NOT_MET,          // CQL evaluation result - measure not met
        BATCH_EVALUATION,         // Batch CQL evaluation
        CARE_GAP_IDENTIFICATION,  // Care gap identification
        AI_RECOMMENDATION,        // AI agent recommendation/decision
        TOOL_EXECUTION,           // AI agent tool execution
        GUARDRAIL_BLOCK,          // AI response blocked by guardrails
        PHI_ACCESS,               // PHI accessed by AI agent
        FHIR_QUERY,               // FHIR resource query
        AI_DECISION_FAILED,       // AI decision failed/errored
        AGENT_EXECUTION,          // AI agent execution event
        HOSPITALIZATION_PREDICTION,  // Readmission/hospitalization risk prediction
        RISK_STRATIFICATION,      // Patient population risk stratification
        CLINICAL_DECISION,        // Clinical decision support
        HCC_CODING,               // HCC code assignment
        RAF_CALCULATION,          // RAF score calculation
        CARE_GAP_CLOSURE,         // Care gap closure
        CDS_RECOMMENDATION,       // Clinical decision support recommendation
        QUALITY_MEASURE_RESULT,   // Quality measure evaluation result
        PATIENT_RISK_SCORE,       // Patient health risk score
        CONSENT_GRANT,            // Patient consent granted
        CONSENT_REVOKE,           // Patient consent revoked
        CONSENT_UPDATE,           // Patient consent updated
        EHR_DATA_FETCH,           // EHR data fetch from external system
        EHR_DATA_PUSH,            // EHR data push to local system
        CDR_INGEST,               // CDR data ingestion (HL7/CDA)
        CDR_TRANSFORM,            // CDR data transformation
        PRIOR_AUTH_REQUEST,       // Prior authorization request
        PRIOR_AUTH_DECISION,      // Prior authorization decision
        APPROVAL_REQUEST,         // General approval workflow request
        APPROVAL_DECISION,        // General approval workflow decision
        PAYER_WORKFLOW_STEP       // Payer-specific workflow step
    }

    /**
     * Customer profile information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerProfile {
        @JsonProperty("customerTier")
        private String customerTier; // MICRO, SMALL, MEDIUM, LARGE, ENTERPRISE

        @JsonProperty("patientCount")
        private Long patientCount;

        @JsonProperty("providerCount")
        private Integer providerCount;

        @JsonProperty("practiceCount")
        private Integer practiceCount;

        @JsonProperty("averageDailyMessages")
        private Long averageDailyMessages;

        @JsonProperty("peakConcurrentUsers")
        private Integer peakConcurrentUsers;

        @JsonProperty("trafficTier")
        private String trafficTier; // LOW, MEDIUM, HIGH
    }

    /**
     * Configuration recommendation from the AI
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConfigurationRecommendation {
        @JsonProperty("configType")
        private String configType; // e.g., "hikari.maximum-pool-size"

        @JsonProperty("currentValue")
        private Object currentValue;

        @JsonProperty("recommendedValue")
        private Object recommendedValue;

        @JsonProperty("expectedImpact")
        private String expectedImpact; // Human-readable impact description

        @JsonProperty("implementationComplexity")
        private String implementationComplexity; // Low, Medium, High

        @JsonProperty("riskLevel")
        private RiskLevel riskLevel;

        @JsonProperty("rollbackPlan")
        private String rollbackPlan;

        @JsonProperty("rollbackProcedure")
        private String rollbackProcedure; // Alternative name for rollback plan

        @JsonProperty("validationCriteria")
        private Map<String, Object> validationCriteria; // How to verify the change worked

        @JsonProperty("validationSteps")
        private String validationSteps; // Steps to validate the change

        @JsonProperty("costImpact")
        private String costImpact; // Cost estimate

        @JsonProperty("timeToImplement")
        private String timeToImplement; // Time estimate

        @JsonProperty("approvalRequired")
        private Boolean approvalRequired; // Whether approval is needed
    }

    public enum RiskLevel {
        LOW,      // Minor change, safe to auto-apply
        MEDIUM,   // Requires user confirmation
        HIGH,     // Requires approval + testing
        CRITICAL  // Production impact, multi-stage rollout required
    }

    /**
     * Outcome of the decision
     */
    public enum DecisionOutcome {
        PENDING,           // Decision made, awaiting user action
        APPLIED,          // Configuration applied
        REJECTED_BY_USER, // User rejected the recommendation
        AUTO_APPLIED,     // System auto-applied (low risk)
        VALIDATION_FAILED, // Applied but validation failed
        ROLLED_BACK,      // Applied then rolled back
        APPROVED,         // Decision approved/completed successfully
        BLOCKED           // Decision blocked (e.g., by guardrails)
    }

    /**
     * User feedback on AI decisions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserFeedback {
        @JsonProperty("rating")
        private Integer rating; // 1-5 stars

        @JsonProperty("helpful")
        private Boolean helpful;

        @JsonProperty("comment")
        private String comment;

        @JsonProperty("timestamp")
        private Instant timestamp;

        @JsonProperty("userId")
        private String userId;
    }

}
