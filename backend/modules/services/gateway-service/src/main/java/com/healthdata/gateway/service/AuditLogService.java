package com.healthdata.gateway.service;

import com.healthdata.gateway.domain.AuditLog;
import com.healthdata.gateway.domain.AuditLogRepository;
import com.healthdata.gateway.dto.AuditLogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit Log Service (Phase 2.0 Team 2)
 *
 * Handles asynchronous audit logging for HIPAA compliance
 * Converts audit requests to persistent entities
 * Implements sanitization and error handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log endpoint access asynchronously
     *
     * Non-blocking method that saves audit records without blocking request processing
     * Failures are logged but not rethrown to prevent impacting client requests
     */
    @Async("auditLogExecutor")
    @Transactional
    public void logAccessAsync(AuditLogRequest request) {
        try {
            AuditLog auditLog = convertRequestToEntity(request);
            auditLogRepository.save(auditLog);

            if (isSecurityEvent(auditLog)) {
                logSecurityEvent(auditLog);
            }

        } catch (Exception e) {
            log.error("Failed to persist audit log: {}", e.getMessage(), e);
            // Log to fallback location (file, external service, etc.) if needed
            logAuditFailure(request, e);
        }
    }

    /**
     * Convert audit log request to JPA entity
     */
    private AuditLog convertRequestToEntity(AuditLogRequest request) {
        return AuditLog.builder()
            .timestamp(request.getTimestamp())
            .httpMethod(request.getHttpMethod())
            .requestPath(request.getRequestPath())
            .queryParameters(request.getQueryParameters())
            .userId(request.getUserId())
            .username(request.getUsername())
            .tenantId(request.getTenantId())
            .roles(request.getRoles())
            .clientIp(request.getClientIp())
            .userAgent(request.getUserAgent())
            .httpStatusCode(request.getHttpStatusCode())
            .responseTimeMs(request.getResponseTimeMs())
            .success(request.getSuccess())
            .authorizationAllowed(request.getAuthorizationAllowed())
            .requiredRole(request.getRequiredRole())
            .traceId(request.getTraceId())
            .spanId(request.getSpanId())
            .createdAt(Instant.now())
            .build();
    }

    /**
     * Determine if audit log entry represents a security event requiring immediate attention
     *
     * Security events include:
     * - Failed authentication (401 status)
     * - Unauthorized access attempts (403 status)
     * - Rate limit violations (429 status)
     * - Failed authorization checks
     */
    private boolean isSecurityEvent(AuditLog auditLog) {
        // Authentication failures
        if (auditLog.getHttpStatusCode() == 401) {
            return true;
        }

        // Authorization failures
        if (auditLog.getHttpStatusCode() == 403 || !auditLog.wasAuthorized()) {
            return true;
        }

        // Rate limit exceeded
        if (auditLog.getHttpStatusCode() == 429) {
            return true;
        }

        return false;
    }

    /**
     * Log security events at WARN level for monitoring
     */
    private void logSecurityEvent(AuditLog auditLog) {
        String eventType = determineEventType(auditLog);
        log.warn(
            "SECURITY EVENT: {} - User: {}, IP: {}, Path: {}, Status: {}, Tenant: {}",
            eventType,
            auditLog.getUserId() != null ? auditLog.getUserId() : "ANONYMOUS",
            auditLog.getClientIp(),
            auditLog.getRequestPath(),
            auditLog.getHttpStatusCode(),
            auditLog.getTenantId()
        );
    }

    /**
     * Determine type of security event
     */
    private String determineEventType(AuditLog auditLog) {
        if (auditLog.getHttpStatusCode() == 401) {
            if ("/api/v1/auth/login".equals(auditLog.getRequestPath())) {
                return "FAILED_LOGIN";
            }
            return "AUTHENTICATION_FAILURE";
        }

        if (auditLog.getHttpStatusCode() == 403 || !auditLog.wasAuthorized()) {
            return "UNAUTHORIZED_ACCESS";
        }

        if (auditLog.getHttpStatusCode() == 429) {
            return "RATE_LIMIT_EXCEEDED";
        }

        return "SECURITY_EVENT";
    }

    /**
     * Log audit logging failure to fallback location
     *
     * In production, this could write to:
     * - File-based audit journal
     * - External syslog server
     * - Secondary database
     * - Message queue for retry
     */
    private void logAuditFailure(AuditLogRequest request, Exception e) {
        log.error(
            "AUDIT LOG FAILURE - Path: {}, User: {}, Tenant: {}, Error: {}",
            request.getRequestPath(),
            request.getUserId() != null ? request.getUserId() : "UNKNOWN",
            request.getTenantId(),
            e.getMessage()
        );

        // TODO: Implement fallback mechanism
        // Examples:
        // - Write to dedicated audit failure queue
        // - Write to separate audit log file
        // - Send to external audit service
    }

    /**
     * Query recent failed login attempts for brute force detection
     *
     * Can be called by security monitoring service
     */
    @Transactional(readOnly = true)
    public long countFailedLoginsFromIp(String clientIp, String tenantId, Instant since) {
        // This would require custom query method on repository
        // For now, use basic count logic
        return auditLogRepository.findFailedLoginAttemptsFromIp(clientIp, since, null).getTotalElements();
    }

    /**
     * Query recent unauthorized access attempts
     *
     * Can be called by anomaly detection service
     */
    @Transactional(readOnly = true)
    public long countUnauthorizedAttempts(String tenantId, Instant since) {
        return auditLogRepository.findUnauthorizedAttempts(since, null).getTotalElements();
    }

    /**
     * Query recent rate limit violations
     *
     * Can be called by security monitoring service
     */
    @Transactional(readOnly = true)
    public long countRateLimitViolations(Instant since) {
        return auditLogRepository.findRateLimitEvents(since, null).getTotalElements();
    }

    /**
     * Log token revocation event (Phase 2.0 Team 3.2)
     *
     * @param userId User ID performing the revocation
     * @param tenantId Tenant ID
     * @param tokenIdentifier Token identifier (JTI or token value)
     * @param reason Revocation reason
     */
    @Async("auditLogExecutor")
    @Transactional
    public void logTokenRevocation(String userId, String tenantId, String tokenIdentifier, String reason) {
        try {
            AuditLogRequest request = AuditLogRequest.builder()
                .timestamp(Instant.now())
                .httpMethod("POST")
                .requestPath("/api/v1/auth/revoke")
                .userId(userId)
                .username(userId)
                .tenantId(tenantId)
                .roles("USER")
                .httpStatusCode(200)
                .success(true)
                .authorizationAllowed(true)
                .build();

            // Add token revocation details to query parameters
            request.setQueryParameters("token=" + tokenIdentifier + "&reason=" + reason);

            AuditLog auditLog = convertRequestToEntity(request);
            auditLogRepository.save(auditLog);

            log.info("Token revocation logged: {} for token: {} (reason: {})", userId, tokenIdentifier, reason);

        } catch (Exception e) {
            log.error("Failed to log token revocation: {}", e.getMessage(), e);
        }
    }

    /**
     * Log token refresh operation
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param status Refresh status (SUCCESS, FAILURE)
     */
    @Async("auditLogExecutor")
    @Transactional
    public void logTokenRefresh(String userId, String tenantId, String status) {
        try {
            AuditLogRequest request = AuditLogRequest.builder()
                .timestamp(Instant.now())
                .userId(userId)
                .tenantId(tenantId)
                .requestPath("/api/v1/auth/refresh")
                .httpMethod("POST")
                .roles("USER")
                .httpStatusCode("SUCCESS".equals(status) ? 200 : 400)
                .success("SUCCESS".equals(status))
                .authorizationAllowed(true)
                .build();

            request.setQueryParameters("status=" + status);

            AuditLog auditLog = convertRequestToEntity(request);
            auditLogRepository.save(auditLog);

            log.info("Token refresh logged: {} - Status: {}", userId, status);

        } catch (Exception e) {
            log.error("Failed to log token refresh: {}", e.getMessage(), e);
        }
    }

    /**
     * Get most recent audit log entry
     *
     * Useful for health checks and monitoring
     */
    @Transactional(readOnly = true)
    public AuditLog getLastAuditEntry() {
        return auditLogRepository.findFirstByOrderByTimestampDesc();
    }
}
