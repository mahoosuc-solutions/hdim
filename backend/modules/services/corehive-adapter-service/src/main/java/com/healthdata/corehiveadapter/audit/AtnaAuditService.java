package com.healthdata.corehiveadapter.audit;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

@Slf4j
public class AtnaAuditService {

    private final String serviceName;
    private final String phiLevel;

    public AtnaAuditService(String serviceName) {
        this(serviceName, "NONE");
    }

    public AtnaAuditService(String serviceName, String phiLevel) {
        this.serviceName = serviceName;
        this.phiLevel = phiLevel;
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
    }
}
