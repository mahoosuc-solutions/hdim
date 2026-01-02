package com.healthdata.audit.aspects;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditEvent;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.audit.service.AuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * AOP Aspect that intercepts methods annotated with @Audited
 * and automatically logs HIPAA-compliant audit events.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Intercept methods annotated with @Audited.
     */
    @Around("@annotation(com.healthdata.audit.annotations.Audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Get the @Audited annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audited auditedAnnotation = method.getAnnotation(Audited.class);

        // Build audit event
        AuditEvent.Builder eventBuilder = AuditEvent.builder()
            .action(auditedAnnotation.action())
            .resourceType(auditedAnnotation.resourceType())
            .purposeOfUse(auditedAnnotation.purposeOfUse())
            .encrypted(auditedAnnotation.encryptPayload())
            .serviceName(joinPoint.getTarget().getClass().getSimpleName())
            .methodName(method.getName());

        // Extract user context from Spring Security
        extractUserContext(eventBuilder);

        // Extract HTTP request context
        extractHttpContext(eventBuilder);

        // Extract resource ID from method parameters if available
        extractResourceId(joinPoint, eventBuilder);

        // Include request payload if requested
        if (auditedAnnotation.includeRequestPayload()) {
            try {
                JsonNode requestPayload = objectMapper.valueToTree(joinPoint.getArgs());
                eventBuilder.requestPayload(requestPayload);
            } catch (Exception e) {
                logger.warn("Failed to serialize request payload for audit", e);
            }
        }

        Object result = null;
        Throwable exception = null;

        try {
            // Proceed with the method execution
            result = joinPoint.proceed();

            // Success outcome
            eventBuilder.outcome(AuditOutcome.SUCCESS);

            // Include response payload if requested
            if (auditedAnnotation.includeResponsePayload() && result != null) {
                try {
                    JsonNode responsePayload = objectMapper.valueToTree(result);
                    eventBuilder.responsePayload(responsePayload);
                } catch (Exception e) {
                    logger.warn("Failed to serialize response payload for audit", e);
                }
            }

        } catch (Throwable t) {
            exception = t;

            // Failure outcome
            eventBuilder.outcome(AuditOutcome.SERIOUS_FAILURE);
            eventBuilder.errorMessage(t.getMessage());
        } finally {
            // Calculate duration
            long duration = System.currentTimeMillis() - startTime;
            eventBuilder.durationMs(duration);

            // Log the audit event
            try {
                AuditEvent auditEvent = eventBuilder.build();
                auditService.logAuditEvent(auditEvent);
            } catch (Exception e) {
                // CRITICAL: Never let audit logging break the application
                logger.error("CRITICAL: Failed to log audit event for method: {}.{}",
                    joinPoint.getTarget().getClass().getName(),
                    method.getName(), e);
            }
        }

        // Re-throw exception if one occurred
        if (exception != null) {
            throw exception;
        }

        return result;
    }

    /**
     * Extract user context from Spring Security.
     */
    private void extractUserContext(AuditEvent.Builder eventBuilder) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                eventBuilder.username(authentication.getName());

                // Extract user ID if available (implementation-specific)
                // You may need to cast to your custom UserDetails implementation
                if (authentication.getPrincipal() instanceof String) {
                    eventBuilder.userId((String) authentication.getPrincipal());
                }

                // Extract role
                if (!authentication.getAuthorities().isEmpty()) {
                    String role = authentication.getAuthorities().iterator().next().getAuthority();
                    eventBuilder.role(role);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract user context", e);
        }
    }

    /**
     * Extract HTTP request context.
     */
    private void extractHttpContext(AuditEvent.Builder eventBuilder) {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // IP address
                String ipAddress = request.getRemoteAddr();
                String forwardedFor = request.getHeader("X-Forwarded-For");
                if (forwardedFor != null && !forwardedFor.isBlank()) {
                    ipAddress = forwardedFor.split(",")[0].trim();
                }
                eventBuilder.ipAddress(ipAddress);

                // User agent
                String userAgent = request.getHeader("User-Agent");
                eventBuilder.userAgent(userAgent);

                // Request path
                String requestPath = request.getRequestURI();
                eventBuilder.requestPath(requestPath);

                // Tenant ID from header (if multi-tenant)
                String tenantId = request.getHeader("X-Tenant-Id");
                if (tenantId != null) {
                    eventBuilder.tenantId(tenantId);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract HTTP context", e);
        }
    }

    /**
     * Extract resource ID from method parameters.
     * Looks for parameters named "id", "resourceId", or annotated with @PathVariable("id").
     */
    private void extractResourceId(ProceedingJoinPoint joinPoint, AuditEvent.Builder eventBuilder) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < parameterNames.length; i++) {
                String paramName = parameterNames[i];
                if ("id".equals(paramName) || "resourceId".equals(paramName) || "patientId".equals(paramName)) {
                    if (args[i] != null) {
                        eventBuilder.resourceId(args[i].toString());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract resource ID", e);
        }
    }
}
