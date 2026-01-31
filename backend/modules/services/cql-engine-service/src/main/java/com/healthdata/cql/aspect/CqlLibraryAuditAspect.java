package com.healthdata.cql.aspect;

import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.event.audit.AuditEvent;
import com.healthdata.cql.event.audit.AuditEventProducer;
import com.healthdata.cql.event.audit.CqlLibraryAuditEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * AOP Aspect for auditing CQL Library management operations.
 *
 * Automatically captures audit events for:
 * - Library creation
 * - Library updates
 * - Library deletion
 */
@Aspect
@Component
public class CqlLibraryAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(CqlLibraryAuditAspect.class);

    private final AuditEventProducer auditEventProducer;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    public CqlLibraryAuditAspect(AuditEventProducer auditEventProducer) {
        this.auditEventProducer = auditEventProducer;
    }

    /**
     * Audit CQL library creation
     */
    @Around("execution(* com.healthdata.cql.service.CqlLibraryService.createLibrary(..))")
    public Object auditLibraryCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditEnabled) {
            return joinPoint.proceed();
        }

        Instant timestamp = Instant.now();
        String eventId = UUID.randomUUID().toString();

        try {
            Object result = joinPoint.proceed();

            if (result instanceof CqlLibrary) {
                CqlLibrary library = (CqlLibrary) result;
                emitLibraryAuditEvent(library, "CREATE_LIBRARY", eventId, timestamp,
                        AuditEvent.OperationResult.SUCCESS, null);
            }

            return result;

        } catch (Exception e) {
            // Extract library from args for failure event
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof CqlLibrary) {
                CqlLibrary library = (CqlLibrary) args[0];
                emitLibraryAuditEvent(library, "CREATE_LIBRARY", eventId, timestamp,
                        AuditEvent.OperationResult.FAILURE, e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Audit CQL library updates
     */
    @Around("execution(* com.healthdata.cql.service.CqlLibraryService.updateLibrary(..))")
    public Object auditLibraryUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditEnabled) {
            return joinPoint.proceed();
        }

        Instant timestamp = Instant.now();
        String eventId = UUID.randomUUID().toString();
        Object[] args = joinPoint.getArgs();
        UUID libraryId = (UUID) args[0];

        try {
            Object result = joinPoint.proceed();

            if (result instanceof CqlLibrary) {
                CqlLibrary library = (CqlLibrary) result;
                emitLibraryAuditEvent(library, "UPDATE_LIBRARY", eventId, timestamp,
                        AuditEvent.OperationResult.SUCCESS, null);
            }

            return result;

        } catch (Exception e) {
            // Create minimal audit event with available info
            emitMinimalLibraryAuditEvent(libraryId, null, "UPDATE_LIBRARY", eventId, timestamp,
                    AuditEvent.OperationResult.FAILURE, e.getMessage());
            throw e;
        }
    }

    /**
     * Audit CQL library deletion
     */
    @Around("execution(* com.healthdata.cql.service.CqlLibraryService.deleteLibrary(..))")
    public Object auditLibraryDeletion(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditEnabled) {
            return joinPoint.proceed();
        }

        Instant timestamp = Instant.now();
        String eventId = UUID.randomUUID().toString();
        Object[] args = joinPoint.getArgs();
        UUID libraryId = (UUID) args[0];
        String tenantId = (String) args[1];

        try {
            joinPoint.proceed();

            emitMinimalLibraryAuditEvent(libraryId, tenantId, "DELETE_LIBRARY", eventId, timestamp,
                    AuditEvent.OperationResult.SUCCESS, null);

            return null;

        } catch (Exception e) {
            emitMinimalLibraryAuditEvent(libraryId, tenantId, "DELETE_LIBRARY", eventId, timestamp,
                    AuditEvent.OperationResult.FAILURE, e.getMessage());
            throw e;
        }
    }

    /**
     * Emit audit event for library operation
     */
    private void emitLibraryAuditEvent(CqlLibrary library, String action, String eventId,
                                       Instant timestamp, AuditEvent.OperationResult result,
                                       String errorMessage) {
        try {
            if (library == null || library.getId() == null) {
                logger.warn("Skipping library audit event: missing library id (action={})", action);
                return;
            }
            String performedBy = extractPerformedBy();

            CqlLibraryAuditEvent auditEvent = CqlLibraryAuditEvent.builder()
                    .eventId(eventId)
                    .timestamp(timestamp)
                    .tenantId(library.getTenantId())
                    .performedBy(performedBy)
                    .action(action)
                    .resourceType("CQL_LIBRARY")
                    .resourceId(library.getId().toString())
                    .result(result)
                    .details(buildDetails(library, errorMessage))
                    .clientIp(null)
                    .requestId(null)
                    .libraryId(library.getId())
                    .libraryName(library.getLibraryName())
                    .libraryVersion(library.getVersion())
                    .libraryContentLength(library.getCqlContent() != null ? library.getCqlContent().length() : 0)
                    .previousVersion(null) // Note: Previous version tracking would require fetching before update
                    .build();

            auditEventProducer.publishLibraryAudit(auditEvent);

            logger.info("Emitted library audit event: action={}, libraryId={}, name={}",
                    action, library.getId(), library.getLibraryName());

        } catch (Exception e) {
            logger.error("Failed to emit library audit event: libraryId={}, error={}",
                    library.getId(), e.getMessage(), e);
        }
    }

    /**
     * Emit minimal audit event when full library object is not available
     */
    private void emitMinimalLibraryAuditEvent(UUID libraryId, String tenantId, String action,
                                              String eventId, Instant timestamp,
                                              AuditEvent.OperationResult result, String errorMessage) {
        try {
            if (libraryId == null) {
                logger.warn("Skipping minimal library audit event: missing library id (action={})", action);
                return;
            }
            String performedBy = extractPerformedBy();

            CqlLibraryAuditEvent auditEvent = CqlLibraryAuditEvent.builder()
                    .eventId(eventId)
                    .timestamp(timestamp)
                    .tenantId(tenantId != null ? tenantId : "unknown")
                    .performedBy(performedBy)
                    .action(action)
                    .resourceType("CQL_LIBRARY")
                    .resourceId(libraryId.toString())
                    .result(result)
                    .details(errorMessage != null ? errorMessage : "Operation completed")
                    .clientIp(null)
                    .requestId(null)
                    .libraryId(libraryId)
                    .libraryName(null)
                    .libraryVersion(null)
                    .libraryContentLength(null)
                    .previousVersion(null)
                    .build();

            auditEventProducer.publishLibraryAudit(auditEvent);

            logger.info("Emitted minimal library audit event: action={}, libraryId={}", action, libraryId);

        } catch (Exception e) {
            logger.error("Failed to emit minimal library audit event: libraryId={}, error={}",
                    libraryId, e.getMessage(), e);
        }
    }

    /**
     * Build detailed information about the library operation
     */
    private String buildDetails(CqlLibrary library, String errorMessage) {
        if (errorMessage != null) {
            return "Error: " + errorMessage;
        }
        return String.format("Library: %s v%s (ID: %s)",
                library.getLibraryName(), library.getVersion(), library.getId());
    }

    /**
     * Extract the authenticated user from the security context.
     * Falls back to "system" if no authentication is present.
     */
    private String extractPerformedBy() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return auth.getName();
            }
        } catch (Exception e) {
            logger.debug("Could not extract user context", e);
        }
        return "system";
    }
}
