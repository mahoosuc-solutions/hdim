package com.healthdata.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for gateway authentication.
 *
 * Configuration example in application.yml:
 * <pre>
 * gateway:
 *   auth:
 *     enabled: true
 *     enforced: true
 *     header-signing-secret: ${GATEWAY_SIGNING_SECRET}
 *     signature-validity-seconds: 300
 *     public-paths:
 *       global:
 *         - /api/v1/auth/**
 *         - /actuator/health/**
 *       fhir-service:
 *         - /fhir/metadata
 *       cql-engine-service:
 *         - /cql-engine/health
 * </pre>
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    /**
     * Enable/disable gateway authentication.
     * When disabled, requests pass through without authentication.
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Enforce authentication for all requests.
     * When false, authentication is optional (demo mode - NOT FOR PRODUCTION).
     */
    @NotNull
    private Boolean enforced = true;

    /**
     * Secret key for signing gateway validation headers.
     * Used to create HMAC signature in X-Auth-Validated header.
     * MUST be at least 32 characters for security.
     */
    @NotBlank(message = "Header signing secret is required for production")
    private String headerSigningSecret;

    /**
     * Validity period for gateway signature in seconds.
     * Default: 300 seconds (5 minutes).
     */
    @Positive
    private Integer signatureValiditySeconds = 300;

    /**
     * Strip external auth headers from incoming requests.
     * SECURITY: Should always be true in production.
     */
    @NotNull
    private Boolean stripExternalAuthHeaders = true;

    /**
     * Log authentication events for audit.
     */
    @NotNull
    private Boolean auditLogging = true;

    /**
     * Public paths configuration.
     * Key: service name or "global" for all services.
     * Value: list of path patterns (Ant-style).
     */
    private Map<String, List<String>> publicPaths = new HashMap<>();

    /**
     * CORS configuration.
     */
    private CorsConfig cors = new CorsConfig();

    /**
     * Rate limiting for authentication endpoints.
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * Get all global public paths.
     */
    public List<String> getGlobalPublicPaths() {
        return publicPaths.getOrDefault("global", new ArrayList<>());
    }

    /**
     * Get public paths for a specific service.
     */
    public List<String> getServicePublicPaths(String serviceName) {
        List<String> paths = new ArrayList<>(getGlobalPublicPaths());
        paths.addAll(publicPaths.getOrDefault(serviceName, new ArrayList<>()));
        return paths;
    }

    /**
     * Check if a path is public (no auth required).
     */
    public boolean isPublicPath(String path) {
        return getGlobalPublicPaths().stream()
            .anyMatch(pattern -> matchesPattern(path, pattern));
    }

    /**
     * Check if a path matches an Ant-style pattern.
     */
    private boolean matchesPattern(String path, String pattern) {
        // Simple pattern matching (supports ** for any, * for single segment)
        String regex = pattern
            .replace("**", "@@DOUBLE@@")
            .replace("*", "[^/]*")
            .replace("@@DOUBLE@@", ".*");
        return path.matches(regex);
    }

    /**
     * Validate configuration for production readiness.
     */
    public List<String> validateForProduction() {
        List<String> errors = new ArrayList<>();

        if (!enforced) {
            errors.add("gateway.auth.enforced=false is not recommended for production");
        }

        if (headerSigningSecret == null || headerSigningSecret.length() < 32) {
            errors.add("gateway.auth.header-signing-secret must be at least 32 characters");
        }

        if (!stripExternalAuthHeaders) {
            errors.add("gateway.auth.strip-external-auth-headers=false is a security risk");
        }

        return errors;
    }

    @Data
    public static class CorsConfig {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private Boolean allowCredentials = true;
        private Long maxAge = 3600L;
    }

    @Data
    public static class RateLimitConfig {
        /**
         * Enable rate limiting for auth endpoints.
         */
        private Boolean enabled = true;

        /**
         * Maximum login attempts per minute per IP.
         */
        private Integer loginAttemptsPerMinute = 10;

        /**
         * Maximum token refresh attempts per minute per IP.
         */
        private Integer refreshAttemptsPerMinute = 20;

        /**
         * Block duration after exceeding rate limit (seconds).
         */
        private Integer blockDurationSeconds = 300;
    }
}
