package com.healthdata.agentbuilder.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration.AgentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes agent configuration events to Kafka for Agent Runtime hot-reload.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentConfigEventPublisher {

    private static final String TOPIC = "agent-config-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;

    /**
     * Publish event when a new agent configuration is created.
     */
    @Async
    public void publishCreated(AgentConfiguration config, String userId) {
        publishEvent(EventType.CREATED, config, null, userId);
    }

    /**
     * Publish event when an agent configuration is updated.
     */
    @Async
    public void publishUpdated(AgentConfiguration config, String userId) {
        publishEvent(EventType.UPDATED, config, null, userId);
    }

    /**
     * Publish event when an agent configuration is deleted/archived.
     */
    @Async
    public void publishDeleted(AgentConfiguration config, String userId) {
        publishEvent(EventType.DELETED, config, null, userId);
    }

    /**
     * Publish event when an agent status changes (e.g., published, deprecated).
     */
    @Async
    public void publishStatusChanged(AgentConfiguration config, AgentStatus previousStatus, String userId) {
        publishEvent(EventType.STATUS_CHANGED, config, previousStatus, userId);
    }

    /**
     * Publish event when an agent is deployed/published.
     */
    @Async
    public void publishDeployed(AgentConfiguration config, String userId) {
        publishEvent(EventType.DEPLOYED, config, null, userId);
    }

    private void publishEvent(EventType eventType, AgentConfiguration config,
                              AgentStatus previousStatus, String userId) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping event: {} for agent: {}", eventType, config.getSlug());
            return;
        }

        try {
            AgentConfigEvent event = AgentConfigEvent.builder()
                .eventType(eventType)
                .configId(config.getId().toString())
                .agentType(config.getSlug())
                .tenantId(config.getTenantId())
                .version(config.getVersion())
                .newStatus(config.getStatus().name())
                .previousStatus(previousStatus != null ? previousStatus.name() : null)
                .modifiedBy(userId)
                .timestamp(Instant.now())
                .build();

            String payload = objectMapper.writeValueAsString(event);
            String key = config.getTenantId() + ":" + config.getSlug();

            CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(TOPIC, key, payload);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish agent config event: type={}, agent={}, error={}",
                        eventType, config.getSlug(), ex.getMessage());
                } else {
                    log.info("Published agent config event: type={}, agent={}, partition={}, offset={}",
                        eventType, config.getSlug(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Error publishing agent config event: type={}, agent={}, error={}",
                eventType, config.getSlug(), e.getMessage(), e);
        }
    }

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED,
        STATUS_CHANGED,
        DEPLOYED
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgentConfigEvent {
        private EventType eventType;
        private String configId;
        private String agentType;
        private String tenantId;
        private String version;
        private String newStatus;
        private String previousStatus;
        private String modifiedBy;
        private Instant timestamp;
    }
}
