package com.healthdata.sales.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for recording and querying audit events.
 * Provides HIPAA-compliant audit trail for all data modifications.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    private static final String SERVICE_NAME = "sales-automation-service";

    /**
     * Record an audit event asynchronously to not block the main transaction.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAsync(AuditEvent event) {
        try {
            event.setServiceName(SERVICE_NAME);
            if (event.getCreatedAt() == null) {
                event.setCreatedAt(LocalDateTime.now());
            }
            auditEventRepository.save(event);
            log.debug("Recorded audit event: {} {} on {} {}",
                event.getAction(), event.getEntityType(), event.getEntityId(), event.getUserEmail());
        } catch (Exception e) {
            log.error("Failed to record audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Record an audit event synchronously within the current transaction.
     */
    @Transactional
    public void record(AuditEvent event) {
        event.setServiceName(SERVICE_NAME);
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(LocalDateTime.now());
        }
        auditEventRepository.save(event);
    }

    /**
     * Helper to build and record a CREATE audit event.
     */
    public void auditCreate(UUID tenantId, UUID userId, String userEmail,
                            String entityType, UUID entityId, String entityName, Object entity) {
        AuditEvent event = AuditEvent.builder()
            .tenantId(tenantId)
            .userId(userId)
            .userEmail(userEmail)
            .action(AuditEvent.AuditAction.CREATE)
            .entityType(entityType)
            .entityId(entityId)
            .entityName(entityName)
            .newValue(serializeEntity(entity))
            .build();
        recordAsync(event);
    }

    /**
     * Helper to build and record an UPDATE audit event.
     */
    public void auditUpdate(UUID tenantId, UUID userId, String userEmail,
                            String entityType, UUID entityId, String entityName,
                            Object oldEntity, Object newEntity, String changes) {
        AuditEvent event = AuditEvent.builder()
            .tenantId(tenantId)
            .userId(userId)
            .userEmail(userEmail)
            .action(AuditEvent.AuditAction.UPDATE)
            .entityType(entityType)
            .entityId(entityId)
            .entityName(entityName)
            .oldValue(serializeEntity(oldEntity))
            .newValue(serializeEntity(newEntity))
            .changes(changes)
            .build();
        recordAsync(event);
    }

    /**
     * Helper to build and record a DELETE audit event.
     */
    public void auditDelete(UUID tenantId, UUID userId, String userEmail,
                            String entityType, UUID entityId, String entityName, Object entity) {
        AuditEvent event = AuditEvent.builder()
            .tenantId(tenantId)
            .userId(userId)
            .userEmail(userEmail)
            .action(AuditEvent.AuditAction.DELETE)
            .entityType(entityType)
            .entityId(entityId)
            .entityName(entityName)
            .oldValue(serializeEntity(entity))
            .build();
        recordAsync(event);
    }

    /**
     * Helper to build and record a custom action audit event.
     */
    public void auditAction(UUID tenantId, UUID userId, String userEmail,
                            AuditEvent.AuditAction action, String entityType,
                            UUID entityId, String entityName, String details) {
        AuditEvent event = AuditEvent.builder()
            .tenantId(tenantId)
            .userId(userId)
            .userEmail(userEmail)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .entityName(entityName)
            .changes(details)
            .build();
        recordAsync(event);
    }

    // Query methods

    @Transactional(readOnly = true)
    public Page<AuditEvent> findByTenant(UUID tenantId, Pageable pageable) {
        return auditEventRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> findByEntityType(UUID tenantId, String entityType, Pageable pageable) {
        return auditEventRepository.findByTenantIdAndEntityTypeOrderByCreatedAtDesc(
            tenantId, entityType, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> findByEntity(UUID tenantId, String entityType, UUID entityId, Pageable pageable) {
        return auditEventRepository.findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            tenantId, entityType, entityId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> findByUser(UUID tenantId, UUID userId, Pageable pageable) {
        return auditEventRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(
            tenantId, userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> findByDateRange(UUID tenantId, LocalDateTime startDate,
                                             LocalDateTime endDate, Pageable pageable) {
        return auditEventRepository.findByTenantIdAndDateRange(
            tenantId, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> findByActions(UUID tenantId, List<AuditEvent.AuditAction> actions, Pageable pageable) {
        return auditEventRepository.findByTenantIdAndActions(tenantId, actions, pageable);
    }

    @Transactional(readOnly = true)
    public Long countRecentEvents(UUID tenantId, int hours) {
        return auditEventRepository.countByTenantIdAndCreatedAtAfter(
            tenantId, LocalDateTime.now().minusHours(hours));
    }

    private String serializeEntity(Object entity) {
        if (entity == null) return null;
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            log.warn("Failed to serialize entity for audit: {}", e.getMessage());
            return entity.toString();
        }
    }
}
