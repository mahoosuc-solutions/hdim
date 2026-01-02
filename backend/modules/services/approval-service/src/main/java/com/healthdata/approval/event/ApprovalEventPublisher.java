package com.healthdata.approval.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.ApprovalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes approval events to Kafka for consumption by Agent Runtime and other services.
 * Enables push-based notifications when approval decisions are made.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalEventPublisher {

    private static final String TOPIC = "approval-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;

    /**
     * Publish event when an approval request is created.
     */
    @Async
    public void publishCreated(ApprovalRequest request) {
        publishEvent(EventType.CREATED, request, null);
    }

    /**
     * Publish event when an approval request is assigned.
     */
    @Async
    public void publishAssigned(ApprovalRequest request, String assignedBy) {
        publishEvent(EventType.ASSIGNED, request, assignedBy);
    }

    /**
     * Publish event when an approval request is approved.
     */
    @Async
    public void publishApproved(ApprovalRequest request, String approvedBy) {
        publishEvent(EventType.APPROVED, request, approvedBy);
    }

    /**
     * Publish event when an approval request is rejected.
     */
    @Async
    public void publishRejected(ApprovalRequest request, String rejectedBy) {
        publishEvent(EventType.REJECTED, request, rejectedBy);
    }

    /**
     * Publish event when an approval request is escalated.
     */
    @Async
    public void publishEscalated(ApprovalRequest request, String escalatedBy) {
        publishEvent(EventType.ESCALATED, request, escalatedBy);
    }

    /**
     * Publish event when an approval request expires.
     */
    @Async
    public void publishExpired(ApprovalRequest request) {
        publishEvent(EventType.EXPIRED, request, "system");
    }

    private void publishEvent(EventType eventType, ApprovalRequest request, String actor) {
        if (request == null) {
            log.warn("Cannot publish event {}: request is null", eventType);
            return;
        }

        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping event: {} for request: {}", eventType, request.getId());
            return;
        }

        try {
            ApprovalEvent event = ApprovalEvent.builder()
                .eventType(eventType)
                .requestId(request.getId().toString())
                .tenantId(request.getTenantId())
                .status(request.getStatus().name())
                .requestType(request.getRequestType().name())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .actionRequested(request.getActionRequested())
                .riskLevel(request.getRiskLevel().name())
                .correlationId(request.getCorrelationId())
                .sourceService(request.getSourceService())
                .actor(actor)
                .decisionBy(request.getDecisionBy())
                .decisionReason(request.getDecisionReason())
                .decisionAt(request.getDecisionAt())
                .timestamp(Instant.now())
                .build();

            String payload = objectMapper.writeValueAsString(event);
            // Use correlation ID or request ID as partition key for ordering
            String key = request.getCorrelationId() != null ?
                request.getCorrelationId() : request.getId().toString();

            CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(TOPIC, key, payload);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish approval event: type={}, request={}, error={}",
                        eventType, request.getId(), ex.getMessage());
                } else {
                    log.info("Published approval event: type={}, request={}, partition={}, offset={}",
                        eventType, request.getId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Error publishing approval event: type={}, request={}, error={}",
                eventType, request.getId(), e.getMessage(), e);
        }
    }

    public enum EventType {
        CREATED,
        ASSIGNED,
        APPROVED,
        REJECTED,
        ESCALATED,
        EXPIRED
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
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
