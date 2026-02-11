package com.healthdata.eventsourcing.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * User context embedded in domain events for HIPAA audit trail.
 *
 * This class captures WHO performed an action, WHY they accessed data,
 * and WHEN it occurred. It's embedded in domain events to ensure
 * complete audit trails for all PHI-related operations.
 *
 * HIPAA Requirements Met:
 * - 45 CFR 164.312(b): Audit controls - implements user identification and action tracking
 * - 45 CFR 164.312(d): Person or entity authentication - captures authenticated user info
 * - 45 CFR 164.308(a)(5): Security awareness - enables access pattern analysis
 *
 * Usage:
 * <pre>
 * PatientCreatedEvent event = PatientCreatedEvent.builder()
 *     .tenantId(tenantId)
 *     .patientId(patient.getId())
 *     .userContext(EventUserContext.builder()
 *         .userId(UserContextHolder.getCurrentUserId())
 *         .username(UserContextHolder.getCurrentUsername())
 *         .activeTenantId(tenantId)
 *         .purposeOfUse("TREATMENT")
 *         .initiatedAt(Instant.now())
 *         .build())
 *     .build();
 * </pre>
 *
 * Purpose of Use Codes (from HIPAA):
 * - TREATMENT: Treatment, Payment, Health Care Operations
 * - PAYMENT: Payment operations
 * - OPERATIONS: Healthcare operations
 * - EMERGENCY: Emergency access (break-the-glass)
 * - RESEARCH: De-identified research
 * - PUBLIC_HEALTH: Public health activities
 * - QUALITY: Quality assessment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventUserContext {

    /**
     * User ID who initiated the action (from JWT/trusted headers).
     * This should be the UUID string format.
     */
    @JsonProperty("user_id")
    private String userId;

    /**
     * Username who initiated the action.
     */
    @JsonProperty("username")
    private String username;

    /**
     * The tenant context in which the action was performed.
     * Critical for multi-tenant audit segregation.
     */
    @JsonProperty("tenant_id")
    private String activeTenantId;

    /**
     * Comma-separated list of roles the user had when performing the action.
     */
    @JsonProperty("roles")
    private String roles;

    /**
     * Client IP address (for audit and security analysis).
     */
    @JsonProperty("ip_address")
    private String ipAddress;

    /**
     * HIPAA Purpose of Use - Why is PHI being accessed?
     * See class javadoc for valid codes.
     */
    @JsonProperty("purpose_of_use")
    private String purposeOfUse;

    /**
     * Timestamp when the action was initiated (before processing).
     * Stored as ISO-8601 string for JSON compatibility.
     */
    @JsonProperty("initiated_at")
    private String initiatedAt;

    /**
     * JWT token ID (jti claim) for session correlation.
     */
    @JsonProperty("token_id")
    private String tokenId;

    /**
     * Correlation ID for distributed tracing.
     */
    @JsonProperty("correlation_id")
    private String correlationId;

    /**
     * User agent string from the original request.
     */
    @JsonProperty("user_agent")
    private String userAgent;

    /**
     * Create an EventUserContext with the current timestamp.
     */
    public static EventUserContext now(String userId, String username, String tenantId) {
        return EventUserContext.builder()
            .userId(userId)
            .username(username)
            .activeTenantId(tenantId)
            .initiatedAt(Instant.now().toString())
            .build();
    }

    /**
     * Create an anonymous context for system-initiated events.
     */
    public static EventUserContext system(String tenantId, String purposeOfUse) {
        return EventUserContext.builder()
            .userId("system")
            .username("system")
            .activeTenantId(tenantId)
            .purposeOfUse(purposeOfUse)
            .initiatedAt(Instant.now().toString())
            .build();
    }

    /**
     * Create a context for scheduled job events.
     */
    public static EventUserContext scheduledJob(String jobName, String tenantId) {
        return EventUserContext.builder()
            .userId("scheduled-job")
            .username("scheduled:" + jobName)
            .activeTenantId(tenantId)
            .purposeOfUse("OPERATIONS")
            .initiatedAt(Instant.now().toString())
            .build();
    }

    /**
     * Check if this context represents a system/automated action.
     */
    public boolean isSystemContext() {
        return "system".equals(userId) || "scheduled-job".equals(userId);
    }

    /**
     * Check if this context has valid user identification.
     */
    public boolean hasUserIdentification() {
        return userId != null && !userId.isBlank() &&
               username != null && !username.isBlank();
    }
}
