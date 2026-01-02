package com.healthdata.cql.event.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Audit event for security-related operations.
 *
 * Tracks:
 * - Authentication attempts
 * - Authorization failures
 * - Suspicious activity
 * - Security policy violations
 */
@Value
@Builder
@Jacksonized
public class SecurityAuditEvent implements AuditEvent {

    @JsonProperty("eventId")
    String eventId;

    @JsonProperty("timestamp")
    Instant timestamp;

    @JsonProperty("tenantId")
    String tenantId;

    @JsonProperty("performedBy")
    String performedBy;

    @JsonProperty("action")
    String action; // AUTHENTICATION_ATTEMPT, AUTHORIZATION_FAILURE, RATE_LIMIT_EXCEEDED, etc.

    @JsonProperty("resourceType")
    String resourceType; // "SECURITY_EVENT"

    @JsonProperty("resourceId")
    String resourceId; // Event ID or user ID

    @JsonProperty("result")
    OperationResult result;

    @JsonProperty("details")
    String details;

    @JsonProperty("clientIp")
    String clientIp;

    @JsonProperty("requestId")
    String requestId;

    // Security specific fields

    @JsonProperty("securityEventType")
    String securityEventType; // AUTH_FAILURE, AUTHZ_FAILURE, SUSPICIOUS_ACTIVITY, etc.

    @JsonProperty("username")
    String username; // User involved

    @JsonProperty("targetResource")
    String targetResource; // Resource being accessed

    @JsonProperty("denialReason")
    String denialReason; // Why access was denied

    @JsonProperty("severity")
    String severity; // INFO, WARNING, ERROR, CRITICAL

    @Override
    public AuditEventType getAuditType() {
        return AuditEventType.SECURITY;
    }
}
