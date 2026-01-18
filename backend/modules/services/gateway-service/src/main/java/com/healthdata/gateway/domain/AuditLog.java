package com.healthdata.gateway.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Audit Logging (Phase 2.0 Team 2)
 *
 * Tracks all endpoint access for compliance (HIPAA §164.312(b))
 * PHI is NOT stored - only access metadata and identifiers
 */
@Entity
@Table(name = "audit_logs",
    indexes = {
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_http_status", columnList = "http_status_code"),
        @Index(name = "idx_trace_id", columnList = "trace_id"),
        @Index(name = "idx_request_path", columnList = "request_path")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ============ Request Metadata ============

    /**
     * Request timestamp (precise to millisecond)
     */
    @Column(nullable = false)
    private Instant timestamp;

    /**
     * HTTP method (GET, POST, PUT, DELETE, PATCH)
     */
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    /**
     * Request path (e.g., /api/v1/patients/123)
     */
    @Column(name = "request_path", nullable = false)
    private String requestPath;

    /**
     * Query parameters (JSON, PHI-sanitized)
     * Example: {"patient_id": "123", "status": "ACTIVE"}
     */
    @Column(name = "query_parameters", columnDefinition = "TEXT")
    private String queryParameters;

    // ============ User/Security Context ============

    /**
     * User ID from JWT (or null if unauthenticated)
     */
    @Column(name = "user_id", length = 255)
    private String userId;

    /**
     * Username from JWT (or null if unauthenticated)
     */
    @Column(name = "username", length = 255)
    private String username;

    /**
     * Tenant ID from X-Tenant-ID header
     */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    /**
     * Comma-separated roles (e.g., "ADMIN,EVALUATOR")
     */
    @Column(name = "roles", length = 255)
    private String roles;

    // ============ Network Information ============

    /**
     * Client IP address (IPv4 or IPv6)
     * Extracted from X-Forwarded-For, X-Real-IP, or request.getRemoteAddr()
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /**
     * User-Agent header value
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    // ============ Response Information ============

    /**
     * HTTP status code (200, 401, 403, 404, 429, 500, etc)
     */
    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    /**
     * Response time in milliseconds
     */
    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    /**
     * Whether response was successful (2xx status code)
     */
    @Column(name = "success")
    private Boolean success;

    // ============ Authorization Information ============

    /**
     * Whether request was authorized to access endpoint
     * true = @PreAuthorize passed or no auth required
     * false = 403 Forbidden or similar
     */
    @Column(name = "authorization_allowed")
    private Boolean authorizationAllowed;

    /**
     * Required role for endpoint (e.g., "ADMIN", "EVALUATOR")
     * Null if endpoint is public
     */
    @Column(name = "required_role", length = 255)
    private String requiredRole;

    // ============ Error Information ============

    /**
     * Error message if request failed
     * Examples:
     * - "Invalid or expired JWT token"
     * - "User does not have required role ADMIN"
     * - "Rate limit exceeded"
     * - "Patient not found"
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ============ Tracing Information ============

    /**
     * OpenTelemetry trace ID for distributed tracing
     * Allows correlating logs across multiple services
     */
    @Column(name = "trace_id", length = 255)
    private String traceId;

    /**
     * Span ID for detailed tracing
     */
    @Column(name = "span_id", length = 255)
    private String spanId;

    // ============ Audit Trail ============

    /**
     * When log entry was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

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

    /**
     * Convenience method to get human-readable status
     */
    public String getStatusDescription() {
        if (httpStatusCode == null) {
            return "Unknown";
        }
        return switch (httpStatusCode) {
            case 200, 201, 202, 204 -> "Success";
            case 400, 422 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 429 -> "Rate Limited";
            case 500, 502, 503, 504 -> "Server Error";
            default -> "HTTP " + httpStatusCode;
        };
    }
}
