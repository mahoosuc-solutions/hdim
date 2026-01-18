package com.healthdata.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for audit log requests (Phase 2.0 Team 2)
 *
 * Transferred from AuditLoggingFilter to AuditLogService
 * Contains request/response metadata for compliance logging
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequest {

    // ============ Request Metadata ============

    /**
     * Request timestamp (precise to millisecond)
     */
    private Instant timestamp;

    /**
     * HTTP method (GET, POST, PUT, DELETE, PATCH)
     */
    private String httpMethod;

    /**
     * Request path (e.g., /api/v1/patients/123)
     */
    private String requestPath;

    /**
     * Query parameters (JSON, PHI-sanitized)
     */
    private String queryParameters;

    // ============ User/Security Context ============

    /**
     * User ID from JWT or security context
     */
    private String userId;

    /**
     * Username from JWT or security context
     */
    private String username;

    /**
     * Tenant ID from X-Tenant-ID header
     */
    private String tenantId;

    /**
     * Comma-separated roles (e.g., "ADMIN,EVALUATOR")
     */
    private String roles;

    // ============ Network Information ============

    /**
     * Client IP address (IPv4 or IPv6)
     * Extracted from X-Forwarded-For, X-Real-IP, or request.getRemoteAddr()
     */
    private String clientIp;

    /**
     * User-Agent header value
     */
    private String userAgent;

    // ============ Response Information ============

    /**
     * HTTP status code (200, 401, 403, 404, 429, 500, etc)
     */
    private Integer httpStatusCode;

    /**
     * Response time in milliseconds
     */
    private Integer responseTimeMs;

    /**
     * Whether response was successful (2xx status code)
     */
    private Boolean success;

    // ============ Authorization Information ============

    /**
     * Whether request was authorized to access endpoint
     * true = authenticated and authorized
     * false = not authenticated or unauthorized
     */
    private Boolean authorizationAllowed;

    /**
     * Required role for endpoint (e.g., "ADMIN", "EVALUATOR")
     * Null if endpoint is public
     */
    private String requiredRole;

    // ============ Tracing Information ============

    /**
     * OpenTelemetry trace ID for distributed tracing
     * Allows correlating logs across multiple services
     */
    private String traceId;

    /**
     * Span ID for detailed tracing
     */
    private String spanId;

    /**
     * Convenience method for checking if request was successful
     */
    public boolean isSuccessful() {
        return success != null && success;
    }

    /**
     * Convenience method for checking if request was authorized
     */
    public boolean wasAuthorized() {
        return authorizationAllowed != null && authorizationAllowed;
    }
}
