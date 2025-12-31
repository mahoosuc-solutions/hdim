package com.healthdata.quality.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Audit Event Publisher for WebSocket connection events
 *
 * Publishes audit events to Kafka for:
 * - HIPAA compliance tracking (§164.312(b) Audit Controls)
 * - SIEM integration (Splunk, ELK, CloudWatch)
 * - Security monitoring and alerting
 * - Dedicated audit database/service consumption
 *
 * Events are published asynchronously to avoid impacting connection latency.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "websocket.enabled", havingValue = "true", matchIfMissing = true)
public class AuditEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${audit.kafka.topic:websocket-audit-events}")
    private String auditTopic;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    /**
     * Publish a WebSocket audit event to Kafka
     *
     * @param auditEvent The audit event map containing event details
     */
    public void publish(Map<String, Object> auditEvent) {
        if (!auditEnabled) {
            log.debug("Audit publishing disabled, skipping event: {}", auditEvent.get("eventType"));
            return;
        }

        try {
            String eventType = (String) auditEvent.get("eventType");
            String tenantId = (String) auditEvent.getOrDefault("tenantId", "unknown");

            // Use tenantId as key for partitioning (ensures tenant events go to same partition)
            kafkaTemplate.send(auditTopic, tenantId, auditEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish audit event {}: {}", eventType, ex.getMessage());
                        } else {
                            log.debug("Published audit event {} to topic {} partition {}",
                                    eventType, auditTopic, result.getRecordMetadata().partition());
                        }
                    });

        } catch (Exception e) {
            // Never fail the WebSocket connection due to audit publishing errors
            log.error("Error publishing audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish a high-severity security event with immediate attention
     *
     * @param auditEvent The security event details
     */
    public void publishSecurityEvent(Map<String, Object> auditEvent) {
        if (!auditEnabled) {
            return;
        }

        try {
            // Security events go to a separate topic for faster alerting
            String securityTopic = auditTopic + "-security";
            String tenantId = (String) auditEvent.getOrDefault("tenantId", "unknown");

            kafkaTemplate.send(securityTopic, tenantId, auditEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("CRITICAL: Failed to publish security audit event: {}", ex.getMessage());
                        } else {
                            log.warn("SECURITY: Published security event to {} - {}",
                                    securityTopic, auditEvent.get("securityViolation"));
                        }
                    });

        } catch (Exception e) {
            log.error("CRITICAL: Error publishing security audit event: {}", e.getMessage(), e);
        }
    }
}
