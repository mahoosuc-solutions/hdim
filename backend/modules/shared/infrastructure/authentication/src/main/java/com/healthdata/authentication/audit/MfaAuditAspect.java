package com.healthdata.authentication.audit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

/**
 * Aspect for auditing MFA operations.
 *
 * <p>HIPAA §164.308(a)(1)(ii)(D) requires audit controls to record and examine
 * information system activity involving ePHI. This aspect logs all MFA operations
 * with user ID, IP address, timestamp, and outcome.</p>
 *
 * <p>Metrics are also recorded to Prometheus for monitoring MFA adoption and
 * detecting potential security issues (e.g., high failure rates).</p>
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class MfaAuditAspect {

    private final MeterRegistry meterRegistry;

    /**
     * Audit all MFA service operations.
     *
     * <p>Captures:</p>
     * <ul>
     *   <li>User ID (from method arguments)</li>
     *   <li>IP address (from request context)</li>
     *   <li>Timestamp (current time)</li>
     *   <li>Event type (mapped from method name)</li>
     *   <li>Outcome (SUCCESS or FAILURE)</li>
     * </ul>
     *
     * @param joinPoint the intercepted method call
     * @return the result of the method execution
     * @throws Throwable if method execution fails
     */
    @Around("execution(* com.healthdata.authentication.service.MfaService.*(..))")
    public Object auditMfaOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // Extract context
        UUID userId = extractUserId(args);
        String ipAddress = extractIpAddress();
        Instant timestamp = Instant.now();
        MfaAuditEvent event = mapMethodToEvent(methodName);

        try {
            // Execute the MFA operation
            Object result = joinPoint.proceed();

            // Log successful operation
            log.info("MFA_AUDIT: event={}, userId={}, ip={}, timestamp={}, outcome=SUCCESS, method={}",
                event.name(), userId, ipAddress, timestamp, methodName);

            // Record success metric
            incrementMfaCounter(event, "success");

            return result;

        } catch (Exception e) {
            // Log failed operation
            log.warn("MFA_AUDIT: event={}, userId={}, ip={}, timestamp={}, outcome=FAILURE, error={}, method={}",
                event.name(), userId, ipAddress, timestamp, e.getMessage(), methodName);

            // Record failure metric
            incrementMfaCounter(event, "failure");

            // Re-throw exception to maintain original behavior
            throw e;
        }
    }

    /**
     * Extract user ID from method arguments.
     * Looks for UUID type arguments.
     *
     * @param args method arguments
     * @return user ID or null if not found
     */
    private UUID extractUserId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
        }
        return null;
    }

    /**
     * Extract IP address from HTTP request context.
     * Checks X-Forwarded-For header for proxy scenarios.
     *
     * @return IP address or "UNKNOWN" if not in request context
     */
    private String extractIpAddress() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // Check X-Forwarded-For header (proxy/load balancer)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // Take first IP in chain (original client)
                return xForwardedFor.split(",")[0].trim();
            }

            // Fall back to remote address
            return request.getRemoteAddr();
        }

        return "UNKNOWN";
    }

    /**
     * Map MfaService method name to audit event type.
     *
     * @param methodName the method being called
     * @return corresponding audit event
     */
    private MfaAuditEvent mapMethodToEvent(String methodName) {
        return switch (methodName) {
            case "initializeMfaSetup" -> MfaAuditEvent.MFA_SETUP_INITIATED;
            case "completeMfaSetup" -> MfaAuditEvent.MFA_ENABLED;
            case "verifyMfaCode" -> MfaAuditEvent.MFA_VERIFICATION_SUCCESS;
            case "verifyRecoveryCode" -> MfaAuditEvent.MFA_RECOVERY_CODE_USED;
            case "disableMfa" -> MfaAuditEvent.MFA_DISABLED;
            case "regenerateRecoveryCodes" -> MfaAuditEvent.MFA_RECOVERY_CODES_REGENERATED;
            default -> MfaAuditEvent.MFA_SETUP_INITIATED; // Default fallback
        };
    }

    /**
     * Increment Prometheus counter for MFA operations.
     *
     * <p>Metrics exposed at /actuator/prometheus</p>
     * <p>Example query: rate(mfa_operations_total[5m])</p>
     *
     * @param event the audit event type
     * @param outcome "success" or "failure"
     */
    private void incrementMfaCounter(MfaAuditEvent event, String outcome) {
        Counter.builder("mfa.operations")
            .description("Count of MFA operations by event type and outcome")
            .tag("event", event.name())
            .tag("outcome", outcome)
            .register(meterRegistry)
            .increment();
    }
}
