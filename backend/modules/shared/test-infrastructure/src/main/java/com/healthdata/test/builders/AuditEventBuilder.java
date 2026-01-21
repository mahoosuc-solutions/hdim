package com.healthdata.test.builders;

import com.healthdata.audit.models.ai.AIAgentDecisionEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builder for creating test AIAgentDecisionEvent instances.
 * 
 * Provides sensible defaults for all required fields.
 * 
 * Usage:
 * <pre>
 * {@code
 * AIAgentDecisionEvent event = AuditEventBuilder.create()
 *     .tenantId("tenant-123")
 *     .agentId("test-agent")
 *     .agentType(AIAgentDecisionEvent.AgentType.CQL_ENGINE)
 *     .decisionType(AIAgentDecisionEvent.DecisionType.MEASURE_MET)
 *     .build();
 * }
 * </pre>
 */
public class AuditEventBuilder {
    
    private UUID eventId = UUID.randomUUID();
    private Instant timestamp = Instant.now();
    private String tenantId = "test-tenant";
    private String agentId = "test-agent";
    private AIAgentDecisionEvent.AgentType agentType = AIAgentDecisionEvent.AgentType.CQL_ENGINE;
    private String agentVersion = "1.0.0";
    private String modelName = "test-model";
    private AIAgentDecisionEvent.DecisionType decisionType = AIAgentDecisionEvent.DecisionType.MEASURE_MET;
    private String resourceType = "Patient";
    private String resourceId = "patient-123";
    private Double confidenceScore = 0.95;
    private String reasoning = "Test reasoning";
    private String correlationId = UUID.randomUUID().toString();
    private Map<String, Object> inputMetrics = new HashMap<>();
    
    private AuditEventBuilder() {
    }
    
    public static AuditEventBuilder create() {
        return new AuditEventBuilder();
    }
    
    public AuditEventBuilder eventId(UUID eventId) {
        this.eventId = eventId;
        return this;
    }
    
    public AuditEventBuilder timestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    public AuditEventBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }
    
    public AuditEventBuilder agentId(String agentId) {
        this.agentId = agentId;
        return this;
    }
    
    public AuditEventBuilder agentType(AIAgentDecisionEvent.AgentType agentType) {
        this.agentType = agentType;
        return this;
    }
    
    public AuditEventBuilder agentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
        return this;
    }
    
    public AuditEventBuilder modelName(String modelName) {
        this.modelName = modelName;
        return this;
    }
    
    public AuditEventBuilder decisionType(AIAgentDecisionEvent.DecisionType decisionType) {
        this.decisionType = decisionType;
        return this;
    }
    
    public AuditEventBuilder resourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }
    
    public AuditEventBuilder resourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }
    
    public AuditEventBuilder confidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
        return this;
    }
    
    public AuditEventBuilder reasoning(String reasoning) {
        this.reasoning = reasoning;
        return this;
    }
    
    public AuditEventBuilder correlationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
    
    public AuditEventBuilder inputMetric(String key, Object value) {
        this.inputMetrics.put(key, value);
        return this;
    }
    
    public AuditEventBuilder inputMetrics(Map<String, Object> inputMetrics) {
        this.inputMetrics = inputMetrics;
        return this;
    }
    
    public AIAgentDecisionEvent build() {
        return AIAgentDecisionEvent.builder()
            .eventId(eventId)
            .timestamp(timestamp)
            .tenantId(tenantId)
            .agentId(agentId)
            .agentType(agentType)
            .agentVersion(agentVersion)
            .modelName(modelName)
            .decisionType(decisionType)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .confidenceScore(confidenceScore)
            .reasoning(reasoning)
            .correlationId(correlationId)
            .inputMetrics(inputMetrics)
            .build();
    }
}

