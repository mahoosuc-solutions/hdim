package com.healthdata.agent.agents.custom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka event listener for agent configuration changes.
 * Enables hot-reload of custom agents when configurations are updated in Agent Builder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentConfigEventListener {

    private static final String TOPIC = "agent-config-events";
    private static final String GROUP_ID = "agent-runtime-config-listener";

    private final CustomAgentProvider customAgentProvider;
    private final ObjectMapper objectMapper;

    /**
     * Handle agent configuration events from Agent Builder service.
     */
    @KafkaListener(
        topics = TOPIC,
        groupId = GROUP_ID,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAgentConfigEvent(String message) {
        try {
            AgentConfigEvent event = objectMapper.readValue(message, AgentConfigEvent.class);
            log.info("Received agent config event: type={}, agentType={}, tenant={}",
                event.getEventType(), event.getAgentType(), event.getTenantId());

            switch (event.getEventType()) {
                case CREATED -> handleConfigCreated(event);
                case UPDATED -> handleConfigUpdated(event);
                case DELETED -> handleConfigDeleted(event);
                case STATUS_CHANGED -> handleStatusChanged(event);
                case DEPLOYED -> handleConfigDeployed(event);
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }

        } catch (Exception e) {
            log.error("Failed to process agent config event: {}", e.getMessage(), e);
        }
    }

    private void handleConfigCreated(AgentConfigEvent event) {
        log.info("New agent configuration created: type={}, tenant={}",
            event.getAgentType(), event.getTenantId());

        // Proactively load the new configuration into cache
        customAgentProvider.reloadAgent(event.getAgentType(), event.getTenantId());
    }

    private void handleConfigUpdated(AgentConfigEvent event) {
        log.info("Agent configuration updated: type={}, tenant={}, version={}",
            event.getAgentType(), event.getTenantId(), event.getVersion());

        // Invalidate and reload the updated configuration
        customAgentProvider.reloadAgent(event.getAgentType(), event.getTenantId());
    }

    private void handleConfigDeleted(AgentConfigEvent event) {
        log.info("Agent configuration deleted: type={}, tenant={}",
            event.getAgentType(), event.getTenantId());

        // Remove from cache
        customAgentProvider.invalidateAgent(event.getAgentType(), event.getTenantId());
    }

    private void handleStatusChanged(AgentConfigEvent event) {
        log.info("Agent status changed: type={}, tenant={}, status={}",
            event.getAgentType(), event.getTenantId(), event.getNewStatus());

        // Reload to pick up new status (might become active or inactive)
        customAgentProvider.reloadAgent(event.getAgentType(), event.getTenantId());
    }

    private void handleConfigDeployed(AgentConfigEvent event) {
        log.info("Agent configuration deployed: type={}, tenant={}",
            event.getAgentType(), event.getTenantId());

        // Reload the deployed configuration
        customAgentProvider.reloadAgent(event.getAgentType(), event.getTenantId());
    }

    /**
     * Agent configuration event from Agent Builder service.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
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

        public enum EventType {
            CREATED,
            UPDATED,
            DELETED,
            STATUS_CHANGED,
            DEPLOYED
        }
    }
}
