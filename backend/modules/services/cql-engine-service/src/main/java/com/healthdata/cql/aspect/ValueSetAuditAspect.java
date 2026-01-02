package com.healthdata.cql.aspect;

import com.healthdata.cql.entity.ValueSet;
import com.healthdata.cql.event.audit.AuditEvent;
import com.healthdata.cql.event.audit.AuditEventProducer;
import com.healthdata.cql.event.audit.ValueSetAuditEvent;
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
 * AOP Aspect for auditing Value Set management operations.
 *
 * Automatically captures audit events for:
 * - Value set creation
 * - Value set updates
 * - Value set deletion
 */
@Aspect
@Component
public class ValueSetAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(ValueSetAuditAspect.class);

    private final AuditEventProducer auditEventProducer;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    public ValueSetAuditAspect(AuditEventProducer auditEventProducer) {
        this.auditEventProducer = auditEventProducer;
    }

    /**
     * Audit value set creation
     */
    @Around("execution(* com.healthdata.cql.service.ValueSetService.createValueSet(..))")
    public Object auditValueSetCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditEnabled) {
            return joinPoint.proceed();
        }

        Instant timestamp = Instant.now();
        String eventId = UUID.randomUUID().toString();

        try {
            Object result = joinPoint.proceed();

            if (result instanceof ValueSet) {
                ValueSet valueSet = (ValueSet) result;
                emitValueSetAuditEvent(valueSet, "CREATE_VALUE_SET", eventId, timestamp,
                        AuditEvent.OperationResult.SUCCESS, null);
            }

            return result;

        } catch (Exception e) {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof ValueSet) {
                ValueSet valueSet = (ValueSet) args[0];
                emitValueSetAuditEvent(valueSet, "CREATE_VALUE_SET", eventId, timestamp,
                        AuditEvent.OperationResult.FAILURE, e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Audit value set updates
     */
    @Around("execution(* com.healthdata.cql.service.ValueSetService.updateValueSet(..))")
    public Object auditValueSetUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditEnabled) {
            return joinPoint.proceed();
        }

        Instant timestamp = Instant.now();
        String eventId = UUID.randomUUID().toString();
        Object[] args = joinPoint.getArgs();
        UUID valueSetId = (UUID) args[0];

        try {
            Object result = joinPoint.proceed();

            if (result instanceof ValueSet) {
                ValueSet valueSet = (ValueSet) result;
                emitValueSetAuditEvent(valueSet, "UPDATE_VALUE_SET", eventId, timestamp,
                        AuditEvent.OperationResult.SUCCESS, null);
            }

            return result;

        } catch (Exception e) {
            emitMinimalValueSetAuditEvent(valueSetId, null, "UPDATE_VALUE_SET", eventId, timestamp,
                    AuditEvent.OperationResult.FAILURE, e.getMessage());
            throw e;
        }
    }

    /**
     * Audit value set deletion
     */
    @Around("execution(* com.healthdata.cql.service.ValueSetService.deleteValueSet(..))")
    public Object auditValueSetDeletion(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditEnabled) {
            return joinPoint.proceed();
        }

        Instant timestamp = Instant.now();
        String eventId = UUID.randomUUID().toString();
        Object[] args = joinPoint.getArgs();
        UUID valueSetId = (UUID) args[0];
        String tenantId = (String) args[1];

        try {
            joinPoint.proceed();

            emitMinimalValueSetAuditEvent(valueSetId, tenantId, "DELETE_VALUE_SET", eventId, timestamp,
                    AuditEvent.OperationResult.SUCCESS, null);

            return null;

        } catch (Exception e) {
            emitMinimalValueSetAuditEvent(valueSetId, tenantId, "DELETE_VALUE_SET", eventId, timestamp,
                    AuditEvent.OperationResult.FAILURE, e.getMessage());
            throw e;
        }
    }

    /**
     * Emit audit event for value set operation
     */
    private void emitValueSetAuditEvent(ValueSet valueSet, String action, String eventId,
                                        Instant timestamp, AuditEvent.OperationResult result,
                                        String errorMessage) {
        try {
            String performedBy = extractPerformedBy();

            ValueSetAuditEvent auditEvent = ValueSetAuditEvent.builder()
                    .eventId(eventId)
                    .timestamp(timestamp)
                    .tenantId(valueSet.getTenantId())
                    .performedBy(performedBy)
                    .action(action)
                    .resourceType("VALUE_SET")
                    .resourceId(valueSet.getId().toString())
                    .result(result)
                    .details(buildDetails(valueSet, errorMessage))
                    .clientIp(null)
                    .requestId(null)
                    .valueSetId(valueSet.getId())
                    .valueSetOid(valueSet.getOid())
                    .valueSetName(valueSet.getName())
                    .valueSetVersion(valueSet.getVersion())
                    .codesCount(valueSet.getCodes() != null ? valueSet.getCodes().length() : 0)
                    .build();

            auditEventProducer.publishValueSetAudit(auditEvent);

            logger.info("Emitted value set audit event: action={}, valueSetId={}, oid={}",
                    action, valueSet.getId(), valueSet.getOid());

        } catch (Exception e) {
            logger.error("Failed to emit value set audit event: valueSetId={}, error={}",
                    valueSet.getId(), e.getMessage(), e);
        }
    }

    /**
     * Emit minimal audit event when full value set object is not available
     */
    private void emitMinimalValueSetAuditEvent(UUID valueSetId, String tenantId, String action,
                                               String eventId, Instant timestamp,
                                               AuditEvent.OperationResult result, String errorMessage) {
        try {
            String performedBy = extractPerformedBy();

            ValueSetAuditEvent auditEvent = ValueSetAuditEvent.builder()
                    .eventId(eventId)
                    .timestamp(timestamp)
                    .tenantId(tenantId != null ? tenantId : "unknown")
                    .performedBy(performedBy)
                    .action(action)
                    .resourceType("VALUE_SET")
                    .resourceId(valueSetId.toString())
                    .result(result)
                    .details(errorMessage != null ? errorMessage : "Operation completed")
                    .clientIp(null)
                    .requestId(null)
                    .valueSetId(valueSetId)
                    .valueSetOid(null)
                    .valueSetName(null)
                    .valueSetVersion(null)
                    .codesCount(null)
                    .build();

            auditEventProducer.publishValueSetAudit(auditEvent);

            logger.info("Emitted minimal value set audit event: action={}, valueSetId={}", action, valueSetId);

        } catch (Exception e) {
            logger.error("Failed to emit minimal value set audit event: valueSetId={}, error={}",
                    valueSetId, e.getMessage(), e);
        }
    }

    /**
     * Build detailed information about the value set operation
     */
    private String buildDetails(ValueSet valueSet, String errorMessage) {
        if (errorMessage != null) {
            return "Error: " + errorMessage;
        }
        return String.format("Value Set: %s (OID: %s, ID: %s, Codes length: %d)",
                valueSet.getName(), valueSet.getOid(), valueSet.getId(),
                valueSet.getCodes() != null ? valueSet.getCodes().length() : 0);
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
