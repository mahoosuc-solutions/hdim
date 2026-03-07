package com.healthdata.healthixadapter.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

@Slf4j
public class AtnaAuditService {

    private static final String ATNA_AUDIT_TOPIC = "ihe.audit.events";

    private final String serviceName;
    private final String phiLevel;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AtnaAuditService(String serviceName) {
        this(serviceName, "NONE", null);
    }

    public AtnaAuditService(String serviceName, String phiLevel) {
        this(serviceName, phiLevel, null);
    }

    public AtnaAuditService(String serviceName, String phiLevel, KafkaTemplate<String, Object> kafkaTemplate) {
        this.serviceName = serviceName;
        this.phiLevel = phiLevel;
        this.kafkaTemplate = kafkaTemplate;
    }

    public AtnaAuditEvent buildAuditEvent(String tenantId, String eventType, String resourceType,
                                           String resourceId, String patientId,
                                           String correlationId, String status, String errorMessage) {
        return AtnaAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .sourceSystem(serviceName)
                .tenantId(tenantId)
                .eventType(eventType)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .patientId(patientId)
                .phiLevel(phiLevel)
                .correlationId(correlationId)
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

    public void logAudit(AtnaAuditEvent event) {
        if ("FAILURE".equals(event.getStatus())) {
            log.error("ATNA_AUDIT: {} | tenant={} | resource={}:{} | correlationId={} | status=FAILURE | error={}",
                    event.getEventType(), event.getTenantId(), event.getResourceType(),
                    event.getResourceId(), event.getCorrelationId(), event.getErrorMessage());
        } else {
            log.info("ATNA_AUDIT: {} | tenant={} | resource={}:{} | correlationId={} | status=SUCCESS",
                    event.getEventType(), event.getTenantId(), event.getResourceType(),
                    event.getResourceId(), event.getCorrelationId());
        }
        forwardToArr(event);
    }

    private void forwardToArr(AtnaAuditEvent event) {
        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(ATNA_AUDIT_TOPIC, event.getTenantId(), event);
                log.debug("ATNA audit event forwarded to ARR topic: {}", ATNA_AUDIT_TOPIC);
            } catch (Exception e) {
                log.warn("Failed to forward ATNA audit event to ARR: {}", e.getMessage());
            }
        }
    }
}
