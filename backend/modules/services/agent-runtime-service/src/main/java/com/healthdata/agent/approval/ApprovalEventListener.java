package com.healthdata.agent.approval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka event listener for approval decision notifications.
 * Enables push-based updates when approval requests are decided.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "hdim.approval.kafka-listener.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ApprovalEventListener {

    private static final String TOPIC = "approval-events";
    private static final String GROUP_ID = "agent-runtime-approval-listener";

    private final ObjectMapper objectMapper;

    /**
     * Cache of pending approval callbacks.
     * Key: correlationId or requestId
     * Value: callback to invoke when decision is received
     */
    private final Map<String, ApprovalCallback> pendingCallbacks = new ConcurrentHashMap<>();

    /**
     * Handle approval events from the Approval Service.
     */
    @KafkaListener(
        topics = TOPIC,
        groupId = GROUP_ID,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleApprovalEvent(String message) {
        try {
            ApprovalEvent event = objectMapper.readValue(message, ApprovalEvent.class);
            log.info("Received approval event: type={}, request={}, status={}",
                event.getEventType(), event.getRequestId(), event.getStatus());

            switch (event.getEventType()) {
                case APPROVED -> handleApproved(event);
                case REJECTED -> handleRejected(event);
                case EXPIRED -> handleExpired(event);
                case ESCALATED -> handleEscalated(event);
                case ASSIGNED -> handleAssigned(event);
                case CREATED -> handleCreated(event);
                default -> log.debug("Ignoring event type: {}", event.getEventType());
            }

        } catch (Exception e) {
            log.error("Failed to process approval event: {}", e.getMessage(), e);
        }
    }

    /**
     * Register a callback to be invoked when an approval decision is made.
     *
     * @param correlationId The correlation ID to match on
     * @param callback      The callback to invoke
     */
    public void registerCallback(String correlationId, ApprovalCallback callback) {
        pendingCallbacks.put(correlationId, callback);
        log.debug("Registered approval callback for: {}", correlationId);
    }

    /**
     * Unregister a callback (e.g., on timeout or cancellation).
     */
    public void unregisterCallback(String correlationId) {
        pendingCallbacks.remove(correlationId);
        log.debug("Unregistered approval callback for: {}", correlationId);
    }

    private void handleApproved(ApprovalEvent event) {
        log.info("Approval request {} was approved by {}", event.getRequestId(), event.getDecisionBy());

        ApprovalCallback callback = findCallback(event);
        if (callback != null) {
            try {
                callback.onApproved(event);
                removeCallback(event);
            } catch (Exception e) {
                log.error("Error in approval callback: {}", e.getMessage(), e);
            }
        }
    }

    private void handleRejected(ApprovalEvent event) {
        log.info("Approval request {} was rejected by {}: {}",
            event.getRequestId(), event.getDecisionBy(), event.getDecisionReason());

        ApprovalCallback callback = findCallback(event);
        if (callback != null) {
            try {
                callback.onRejected(event);
                removeCallback(event);
            } catch (Exception e) {
                log.error("Error in rejection callback: {}", e.getMessage(), e);
            }
        }
    }

    private void handleExpired(ApprovalEvent event) {
        log.warn("Approval request {} expired", event.getRequestId());

        ApprovalCallback callback = findCallback(event);
        if (callback != null) {
            try {
                callback.onExpired(event);
                removeCallback(event);
            } catch (Exception e) {
                log.error("Error in expiration callback: {}", e.getMessage(), e);
            }
        }
    }

    private void handleEscalated(ApprovalEvent event) {
        log.info("Approval request {} was escalated", event.getRequestId());
        // Escalation is informational - the request is still pending
    }

    private void handleAssigned(ApprovalEvent event) {
        log.debug("Approval request {} was assigned", event.getRequestId());
        // Assignment is informational - the request is still pending
    }

    private void handleCreated(ApprovalEvent event) {
        log.debug("Approval request {} was created", event.getRequestId());
        // Creation is informational
    }

    private ApprovalCallback findCallback(ApprovalEvent event) {
        // Try correlation ID first, then request ID
        ApprovalCallback callback = null;
        if (event.getCorrelationId() != null) {
            callback = pendingCallbacks.get(event.getCorrelationId());
        }
        if (callback == null && event.getRequestId() != null) {
            callback = pendingCallbacks.get(event.getRequestId());
        }
        return callback;
    }

    private void removeCallback(ApprovalEvent event) {
        if (event.getCorrelationId() != null) {
            pendingCallbacks.remove(event.getCorrelationId());
        }
        if (event.getRequestId() != null) {
            pendingCallbacks.remove(event.getRequestId());
        }
    }

    /**
     * Get the number of pending callbacks (for monitoring).
     */
    public int getPendingCallbackCount() {
        return pendingCallbacks.size();
    }

    /**
     * Callback interface for approval decision notifications.
     */
    public interface ApprovalCallback {
        void onApproved(ApprovalEvent event);
        void onRejected(ApprovalEvent event);
        void onExpired(ApprovalEvent event);
    }

    public enum EventType {
        CREATED,
        ASSIGNED,
        APPROVED,
        REJECTED,
        ESCALATED,
        EXPIRED
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApprovalEvent {
        private EventType eventType;
        private String requestId;
        private String tenantId;
        private String status;
        private String requestType;
        private String entityType;
        private String entityId;
        private String actionRequested;
        private String riskLevel;
        private String correlationId;
        private String sourceService;
        private String actor;
        private String decisionBy;
        private String decisionReason;
        private Instant decisionAt;
        private Instant timestamp;
    }
}
