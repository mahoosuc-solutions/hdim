package com.healthdata.agent.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Context for agent execution containing tenant, user, and session information.
 * Provides isolation and security boundaries for multi-tenant operations.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {

    /**
     * Tenant identifier for multi-tenancy isolation.
     */
    private String tenantId;

    /**
     * User identifier performing the action.
     */
    private String userId;

    /**
     * Session identifier for conversation tracking.
     */
    private String sessionId;

    /**
     * Correlation ID for distributed tracing.
     */
    private String correlationId;

    /**
     * User's role(s) for permission checks.
     */
    @Builder.Default
    private Set<String> roles = Set.of();

    /**
     * User's permissions for fine-grained access control.
     */
    @Builder.Default
    private Set<String> permissions = Set.of();

    /**
     * Patient ID if context is patient-specific.
     */
    private String patientId;

    /**
     * Encounter ID if context is encounter-specific.
     */
    private String encounterId;

    /**
     * Agent configuration being used.
     */
    private String agentId;

    /**
     * Agent type (e.g., clinical-decision, care-gap-optimizer).
     */
    private String agentType;

    /**
     * Timestamp when context was created.
     */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Context expiration time.
     */
    private Instant expiresAt;

    /**
     * Additional metadata.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Request origin (e.g., web, api, scheduled).
     */
    private String origin;

    /**
     * Data residency region for compliance.
     */
    private String dataRegion;

    /**
     * Whether AI data sharing is consented.
     */
    @Builder.Default
    private boolean aiDataSharingConsented = false;

    /**
     * Consented jurisdictions for cross-border data.
     */
    @Builder.Default
    private Set<String> consentedJurisdictions = Set.of();

    /**
     * Check if context has a specific permission.
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || permissions.contains("*");
    }

    /**
     * Check if context has a specific role.
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Check if context has any of the specified roles.
     */
    public boolean hasAnyRole(String... checkRoles) {
        for (String role : checkRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if context is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if context is valid.
     */
    public boolean isValid() {
        return tenantId != null && userId != null && !isExpired();
    }

    /**
     * Check if context is patient-specific.
     */
    public boolean isPatientContext() {
        return patientId != null;
    }

    /**
     * Get metadata value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Set metadata value.
     */
    public AgentContext withMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }

    /**
     * Create a child context for sub-operations.
     */
    public AgentContext createChildContext(String childCorrelationId) {
        return AgentContext.builder()
            .tenantId(tenantId)
            .userId(userId)
            .sessionId(sessionId)
            .correlationId(childCorrelationId)
            .roles(roles)
            .permissions(permissions)
            .patientId(patientId)
            .encounterId(encounterId)
            .agentId(agentId)
            .agentType(agentType)
            .createdAt(Instant.now())
            .expiresAt(expiresAt)
            .metadata(new HashMap<>(metadata))
            .origin(origin)
            .dataRegion(dataRegion)
            .aiDataSharingConsented(aiDataSharingConsented)
            .consentedJurisdictions(consentedJurisdictions)
            .build();
    }

    /**
     * Create builder from security context.
     */
    public static AgentContextBuilder fromSecurityContext(String tenantId, String userId, Set<String> roles) {
        return AgentContext.builder()
            .tenantId(tenantId)
            .userId(userId)
            .roles(roles)
            .correlationId(java.util.UUID.randomUUID().toString());
    }
}
