package com.healthdata.cql.event.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing audit events to Kafka asynchronously.
 *
 * All audit events are published to a single topic "healthdata.audit.events"
 * with the tenantId as the partition key to ensure events from the same
 * tenant are processed in order.
 *
 * This approach decouples audit logging from business logic, ensuring:
 * - Zero performance impact on CQL evaluation
 * - Guaranteed event delivery (Kafka guarantees)
 * - Scalable audit processing
 * - Long-term retention in TimescaleDB
 */
@Service
public class AuditEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${audit.kafka.topic:healthdata.audit.events}")
    private String auditTopic;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    public AuditEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish a CQL library audit event
     */
    public void publishLibraryAudit(CqlLibraryAuditEvent event) {
        publishAudit(event);
    }

    /**
     * Publish a value set audit event
     */
    public void publishValueSetAudit(ValueSetAuditEvent event) {
        publishAudit(event);
    }

    /**
     * Publish a CQL evaluation audit event
     */
    public void publishEvaluationAudit(CqlEvaluationAuditEvent event) {
        publishAudit(event);
    }

    /**
     * Publish a data access audit event
     */
    public void publishDataAccessAudit(DataAccessAuditEvent event) {
        publishAudit(event);
    }

    /**
     * Publish a security audit event
     */
    public void publishSecurityAudit(SecurityAuditEvent event) {
        publishAudit(event);
    }

    /**
     * Generic method to publish any audit event to Kafka.
     *
     * Events are published asynchronously with fire-and-forget semantics.
     * Failures are logged but do not block the calling thread.
     *
     * @param event The audit event to publish
     */
    private void publishAudit(AuditEvent event) {
        if (!auditEnabled) {
            logger.debug("Audit logging disabled, skipping event: {}", event.getEventId());
            return;
        }

        try {
            // Use tenantId as partition key to ensure ordering of events per tenant
            String partitionKey = event.getTenantId();

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(auditTopic, partitionKey, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.debug("Published audit event: type={}, id={}, tenant={}, partition={}, offset={}",
                            event.getAuditType(),
                            event.getEventId(),
                            event.getTenantId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish audit event: type={}, id={}, tenant={}, error={}",
                            event.getAuditType(),
                            event.getEventId(),
                            event.getTenantId(),
                            ex.getMessage(),
                            ex);
                }
            });
        } catch (Exception e) {
            // Never let audit failures affect business logic
            logger.error("Exception while publishing audit event: type={}, id={}, error={}",
                    event.getAuditType(),
                    event.getEventId(),
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Get the configured audit topic name
     */
    public String getAuditTopic() {
        return auditTopic;
    }

    /**
     * Check if audit logging is enabled
     */
    public boolean isAuditEnabled() {
        return auditEnabled;
    }
}
