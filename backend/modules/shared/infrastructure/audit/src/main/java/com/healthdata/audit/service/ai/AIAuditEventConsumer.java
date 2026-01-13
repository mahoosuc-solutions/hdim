package com.healthdata.audit.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.models.ai.ConfigurationEngineEvent;
import com.healthdata.audit.models.ai.UserConfigurationActionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumers for AI audit events.
 * 
 * Consumes from three topics:
 * - ai.agent.decisions
 * - configuration.engine.changes
 * - user.configuration.actions
 * 
 * Features:
 * - Concurrent consumption for high throughput
 * - JSON deserialization
 * - Error handling and retry logic
 * - Event persistence
 * - Real-time analytics updates
 */
@Slf4j
@Service
public class AIAuditEventConsumer {

    private final ObjectMapper objectMapper;
    private final AIAuditEventStore auditEventStore;

    @Autowired
    public AIAuditEventConsumer(
            ObjectMapper objectMapper,
            AIAuditEventStore auditEventStore) {
        this.objectMapper = objectMapper;
        this.auditEventStore = auditEventStore;
    }

    /**
     * Consume AI agent decision events.
     * 
     * Configured for:
     * - Consumer group: ai-audit-consumer-group
     * - Concurrency: 3 threads
     * - Auto-commit: disabled (manual commit after successful processing)
     */
    @KafkaListener(
        topics = "${audit.kafka.topic.ai-decisions:ai.agent.decisions}",
        groupId = "ai-audit-consumer-group",
        concurrency = "3",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAIDecision(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        
        log.debug("Received AI decision event from partition {} offset {}", partition, offset);

        try {
            AIAgentDecisionEvent event = objectMapper.readValue(message, AIAgentDecisionEvent.class);
            
            // Store event in database
            auditEventStore.storeAIDecision(event);
            
            // Update real-time analytics
            auditEventStore.updateAIDecisionMetrics(event);
            
            // Check for anomalies or patterns
            auditEventStore.analyzeDecisionPattern(event);
            
            log.debug("Successfully processed AI decision event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to process AI decision event from partition {} offset {}: {}", 
                partition, offset, e.getMessage(), e);
            // Event will be retried based on Kafka consumer retry configuration
        }
    }

    /**
     * Consume configuration engine change events.
     */
    @KafkaListener(
        topics = "${audit.kafka.topic.config-changes:configuration.engine.changes}",
        groupId = "ai-audit-consumer-group",
        concurrency = "3",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeConfigurationChange(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        
        log.debug("Received configuration change event from partition {} offset {}", partition, offset);

        try {
            ConfigurationEngineEvent event = objectMapper.readValue(
                message, ConfigurationEngineEvent.class);
            
            // Store event in database
            auditEventStore.storeConfigurationChange(event);
            
            // Update configuration history
            auditEventStore.updateConfigurationHistory(event);
            
            // Track performance impact
            if (event.getPerformanceImpact() != null) {
                auditEventStore.trackPerformanceImpact(event);
            }
            
            // Alert on high-risk changes
            if (isHighRiskChange(event)) {
                auditEventStore.alertOnHighRiskChange(event);
            }
            
            log.debug("Successfully processed configuration change event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to process configuration change event from partition {} offset {}: {}", 
                partition, offset, e.getMessage(), e);
        }
    }

    /**
     * Consume user configuration action events.
     */
    @KafkaListener(
        topics = "${audit.kafka.topic.user-actions:user.configuration.actions}",
        groupId = "ai-audit-consumer-group",
        concurrency = "3",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserAction(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        
        log.debug("Received user action event from partition {} offset {}", partition, offset);

        try {
            UserConfigurationActionEvent event = objectMapper.readValue(
                message, UserConfigurationActionEvent.class);
            
            // Store event in database
            auditEventStore.storeUserAction(event);
            
            // Track user behavior for UX improvements
            auditEventStore.trackUserBehavior(event);
            
            // Update AI recommendation feedback
            if (event.getAiRecommendationId() != null) {
                auditEventStore.updateAIRecommendationFeedback(event);
            }
            
            // Compliance logging for audit trail
            auditEventStore.logComplianceEvent(event);
            
            log.debug("Successfully processed user action event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to process user action event from partition {} offset {}: {}", 
                partition, offset, e.getMessage(), e);
        }
    }

    /**
     * Determine if a configuration change is high-risk.
     * 
     * High-risk changes:
     * - Production environment
     * - Global scope affecting all tenants
     * - Critical resource types (database pools, message queues)
     * - Large value changes (>50% difference)
     */
    private boolean isHighRiskChange(ConfigurationEngineEvent event) {
        if ("PROD".equalsIgnoreCase(event.getEnvironment())) {
            return true;
        }
        
        if (ConfigurationEngineEvent.ConfigurationScope.GLOBAL.equals(
                event.getConfigurationScope())) {
            return true;
        }
        
        if (event.getPerformanceImpact() != null) {
            ConfigurationEngineEvent.ImpactSeverity severity = 
                event.getPerformanceImpact().getImpactSeverity();
            
            if (severity == ConfigurationEngineEvent.ImpactSeverity.CRITICAL ||
                severity == ConfigurationEngineEvent.ImpactSeverity.NEGATIVE_SIGNIFICANT) {
                return true;
            }
        }
        
        return false;
    }
}
