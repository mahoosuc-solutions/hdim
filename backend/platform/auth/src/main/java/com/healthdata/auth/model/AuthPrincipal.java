package com.healthdata.auth.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents an authenticated actor within the platform.
 */
public record AuthPrincipal(
        UUID subjectId,
        String username,
        String tenantId,
        Set<String> roles,
        Map<String, Object> attributes,
        Instant authenticatedAt) {

    public AuthPrincipal {
        roles = roles == null ? Collections.emptySet() : Set.copyOf(roles);
        attributes = attributes == null ? Collections.emptyMap() : Map.copyOf(attributes);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
