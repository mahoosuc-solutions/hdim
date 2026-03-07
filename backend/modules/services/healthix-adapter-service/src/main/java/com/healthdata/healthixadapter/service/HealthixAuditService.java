package com.healthdata.healthixadapter.service;

import com.healthdata.healthixadapter.model.HealthixAuditLog;
import com.healthdata.healthixadapter.model.HealthixAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * HIPAA audit logging service for all Healthix adapter operations.
 * All PHI access must be logged with patient ID, event type,
 * source system, and correlation ID for compliance tracing.
 *
 * Logging is asynchronous (fire-and-forget) to avoid impacting
 * request latency for clinical operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthixAuditService {

    private final HealthixAuditLogRepository auditLogRepository;

    @Async
    public void logPhiAccess(String tenantId, String eventType, String resourceType,
                             String resourceId, String patientId, String correlationId) {
        try {
            HealthixAuditLog entry = HealthixAuditLog.builder()
                    .tenantId(tenantId)
                    .eventType(eventType)
                    .sourceSystem("HEALTHIX")
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .patientId(patientId)
                    .phiLevel("FULL")
                    .correlationId(correlationId)
                    .status("SUCCESS")
                    .build();

            auditLogRepository.save(entry);
            log.debug("HIPAA audit logged: event={}, resource={}/{}, patient={}",
                    eventType, resourceType, resourceId, patientId);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to log HIPAA audit entry: {}", e.getMessage());
        }
    }

    @Async
    public void logPhiAccessFailure(String tenantId, String eventType, String resourceType,
                                    String patientId, String correlationId, String errorMessage) {
        try {
            HealthixAuditLog entry = HealthixAuditLog.builder()
                    .tenantId(tenantId)
                    .eventType(eventType)
                    .sourceSystem("HEALTHIX")
                    .resourceType(resourceType)
                    .patientId(patientId)
                    .phiLevel("FULL")
                    .correlationId(correlationId)
                    .status("FAILURE")
                    .errorMessage(errorMessage)
                    .build();

            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to log HIPAA audit failure entry: {}", e.getMessage());
        }
    }
}
