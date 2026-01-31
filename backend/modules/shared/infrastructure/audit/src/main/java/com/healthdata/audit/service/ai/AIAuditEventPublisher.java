package com.healthdata.audit.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.models.ai.ConfigurationEngineEvent;
import com.healthdata.audit.models.ai.UserConfigurationActionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing AI audit events to Kafka topics.
 * 
 * Publishes to three dedicated topics:
 * - ai.agent.decisions - AI agent recommendations and decisions
 * - configuration.engine.changes - Runtime configuration changes
 * - user.configuration.actions - User-initiated configuration actions
 * 
 * Features:
 * - Async publishing for low latency
 * - JSON serialization
 * - Error handling and logging
 * - Tenant-based partitioning for isolation
 * - Correlation ID tracking
 */
@Slf4j
@Service
public class AIAuditEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.topic.ai-decisions:ai.agent.decisions}")
    private String aiDecisionsTopic;

    @Value("${audit.kafka.topic.config-changes:configuration.engine.changes}")
    private String configChangesTopic;

    @Value("${audit.kafka.topic.user-actions:user.configuration.actions}")
    private String userActionsTopic;

    @Value("${audit.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${audit.kafka.sync:false}")
    private boolean kafkaSync;

    @Autowired
    public AIAuditEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish AI agent decision event.
     * 
     * @param event AI decision event to publish
     * @return CompletableFuture with send result
     */
    public CompletableFuture<SendResult<String, String>> publishAIDecision(AIAgentDecisionEvent event) {
        if (!kafkaEnabled) {
            log.debug("Kafka publishing disabled, skipping AI decision event");
            return CompletableFuture.completedFuture(null);
        }

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String key = buildPartitionKey(event.getTenantId(), event.getAgentId());

            log.debug("Publishing AI decision event: {} to topic: {}", event.getEventId(), aiDecisionsTopic);

            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(aiDecisionsTopic, key, eventJson);

            if (kafkaSync) {
                future.join();
            }

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("AI decision event published successfully: {} to partition: {}", 
                        event.getEventId(), result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish AI decision event: {}", event.getEventId(), ex);
                }
            });

            return future;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize AI decision event: {}", event.getEventId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Publish configuration engine change event.
     * 
     * @param event Configuration change event to publish
     * @return CompletableFuture with send result
     */
    public CompletableFuture<SendResult<String, String>> publishConfigurationChange(
            ConfigurationEngineEvent event) {
        if (!kafkaEnabled) {
            log.debug("Kafka publishing disabled, skipping configuration change event");
            return CompletableFuture.completedFuture(null);
        }

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String key = buildPartitionKey(event.getTenantId(), event.getServiceName());

            log.debug("Publishing configuration change event: {} to topic: {}", 
                event.getEventId(), configChangesTopic);

            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(configChangesTopic, key, eventJson);

            if (kafkaSync) {
                future.join();
            }

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Configuration change event published successfully: {} to partition: {}", 
                        event.getEventId(), result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish configuration change event: {}", event.getEventId(), ex);
                }
            });

            return future;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize configuration change event: {}", event.getEventId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Publish user configuration action event.
     * 
     * @param event User action event to publish
     * @return CompletableFuture with send result
     */
    public CompletableFuture<SendResult<String, String>> publishUserAction(
            UserConfigurationActionEvent event) {
        if (!kafkaEnabled) {
            log.debug("Kafka publishing disabled, skipping user action event");
            return CompletableFuture.completedFuture(null);
        }

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String key = buildPartitionKey(event.getTenantId(), event.getUserId());

            log.debug("Publishing user action event: {} to topic: {}", event.getEventId(), userActionsTopic);

            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(userActionsTopic, key, eventJson);

            if (kafkaSync) {
                future.join();
            }

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("User action event published successfully: {} to partition: {}", 
                        event.getEventId(), result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish user action event: {}", event.getEventId(), ex);
                }
            });

            return future;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user action event: {}", event.getEventId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Build partition key for tenant isolation.
     * Uses tenant ID as primary key with optional secondary key.
     * 
     * @param tenantId Tenant identifier
     * @param secondaryKey Optional secondary key (agent ID, user ID, service name)
     * @return Partition key
     */
    private String buildPartitionKey(String tenantId, String secondaryKey) {
        if (tenantId == null) {
            return secondaryKey != null ? secondaryKey : "global";
        }
        if (secondaryKey == null) {
            return tenantId;
        }
        return tenantId + ":" + secondaryKey;
    }

    /**
     * Convenience method to publish AI decision with blocking wait.
     * Use for critical events where you need confirmation.
     * 
     * @param event AI decision event
     * @return true if published successfully, false otherwise
     */
    public boolean publishAIDecisionSync(AIAgentDecisionEvent event) {
        try {
            publishAIDecision(event).get();
            return true;
        } catch (Exception e) {
            log.error("Failed to publish AI decision event synchronously: {}", event.getEventId(), e);
            return false;
        }
    }

    /**
     * Convenience method to publish configuration change with blocking wait.
     * 
     * @param event Configuration change event
     * @return true if published successfully, false otherwise
     */
    public boolean publishConfigurationChangeSync(ConfigurationEngineEvent event) {
        try {
            publishConfigurationChange(event).get();
            return true;
        } catch (Exception e) {
            log.error("Failed to publish configuration change event synchronously: {}", 
                event.getEventId(), e);
            return false;
        }
    }

    /**
     * Convenience method to publish user action with blocking wait.
     * 
     * @param event User action event
     * @return true if published successfully, false otherwise
     */
    public boolean publishUserActionSync(UserConfigurationActionEvent event) {
        try {
            publishUserAction(event).get();
            return true;
        } catch (Exception e) {
            log.error("Failed to publish user action event synchronously: {}", event.getEventId(), e);
            return false;
        }
    }

    /**
     * Enable or disable Kafka publishing at runtime.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setKafkaEnabled(boolean enabled) {
        this.kafkaEnabled = enabled;
        log.info("Kafka publishing {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Check if Kafka publishing is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isKafkaEnabled() {
        return kafkaEnabled;
    }
}
