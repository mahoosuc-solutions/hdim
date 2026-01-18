package com.healthdata.gateway.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limiting Configuration for Phase 2.0
 *
 * Configures Redis-based rate limiting with:
 * - Per-endpoint limits
 * - Per-role multipliers
 * - Per-tenant overrides
 * - Sliding window algorithm
 */
@Configuration
@ConfigurationProperties(prefix = "security.rate-limiting")
@Data
public class RateLimitConfiguration {

    private boolean enabled = true;
    private String backend = "redis";

    // Default limit (requests per minute)
    private int defaultLimitPerMinute = 1000;

    // Endpoint-specific limits
    private Map<String, EndpointRateLimit> endpoints = new HashMap<>();

    // Role-based multipliers
    private Map<String, Double> roleMultipliers = new HashMap<>();

    // Tenant overrides
    private Map<String, Integer> tenantOverrides = new HashMap<>();

    /**
     * Get rate limit configuration for specific endpoint
     */
    public EndpointRateLimit getConfigForEndpoint(String path) {
        // Exact match first
        if (endpoints.containsKey(path)) {
            return endpoints.get(path);
        }

        // Try pattern matching (e.g., /api/v1/** matches /api/v1/patients/123)
        for (Map.Entry<String, EndpointRateLimit> entry : endpoints.entrySet()) {
            if (matchesPattern(path, entry.getKey())) {
                return entry.getValue();
            }
        }

        // Return default
        return EndpointRateLimit.builder()
            .path(path)
            .limitPerMinute(defaultLimitPerMinute)
            .description("Default limit")
            .build();
    }

    /**
     * Check if endpoint path matches pattern (supports wildcards)
     */
    public boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }

    /**
     * Get role multiplier (e.g., ADMIN gets 2x limit)
     */
    public double getRoleMultiplier(String role) {
        return roleMultipliers.getOrDefault(role, 1.0);
    }

    /**
     * Check if tenant has override limit
     */
    public boolean hasTenantOverride(String tenantId) {
        return tenantOverrides.containsKey(tenantId);
    }

    /**
     * Get tenant-specific limit
     */
    public int getTenantLimit(String tenantId) {
        return tenantOverrides.getOrDefault(tenantId, defaultLimitPerMinute);
    }

    /**
     * Endpoint-specific rate limit configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointRateLimit {
        private String path;
        private int limitPerMinute;
        private String description;
    }
}
