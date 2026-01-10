package com.healthdata.gateway.auth;

import com.healthdata.gateway.config.GatewayAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing public paths that don't require authentication.
 *
 * This component provides:
 * - Centralized configuration of public paths from application.yml
 * - Runtime path registration for dynamic public paths
 * - Efficient path matching using Ant-style patterns
 * - Service-specific and global public path support
 *
 * Default public paths (always included):
 * - /actuator/health/** - Health checks
 * - /actuator/info - Info endpoint
 * - /api/v1/auth/login - Login endpoint
 * - /api/v1/auth/register - Registration endpoint
 * - /api/v1/auth/refresh - Token refresh endpoint
 * - /v3/api-docs/** - OpenAPI docs
 * - /swagger-ui/** - Swagger UI
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicPathRegistry {

    private final GatewayAuthProperties authProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Runtime-registered public paths (in addition to config).
     */
    private final Set<String> runtimePublicPaths = ConcurrentHashMap.newKeySet();

    /**
     * Default public paths that are always included.
     */
    private static final List<String> DEFAULT_PUBLIC_PATHS = List.of(
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/info",
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
        "/api/v1/auth/mfa/verify",  // MFA verification uses mfaToken, not JWT
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/favicon.ico"
    );

    @PostConstruct
    public void init() {
        log.info("Initializing PublicPathRegistry with {} default paths and {} configured paths",
            DEFAULT_PUBLIC_PATHS.size(),
            authProperties.getGlobalPublicPaths().size());

        if (log.isDebugEnabled()) {
            getAllPublicPaths().forEach(path ->
                log.debug("Registered public path: {}", path));
        }
    }

    /**
     * Check if a request path is public (no authentication required).
     *
     * @param path the request path to check
     * @return true if path is public, false otherwise
     */
    public boolean isPublicPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        // Normalize path
        String normalizedPath = normalizePath(path);

        // Check default paths
        for (String pattern : DEFAULT_PUBLIC_PATHS) {
            if (pathMatcher.match(pattern, normalizedPath)) {
                log.trace("Path {} matches default public pattern {}", normalizedPath, pattern);
                return true;
            }
        }

        // Check configured global paths
        for (String pattern : authProperties.getGlobalPublicPaths()) {
            if (pathMatcher.match(pattern, normalizedPath)) {
                log.trace("Path {} matches configured public pattern {}", normalizedPath, pattern);
                return true;
            }
        }

        // Check runtime-registered paths
        for (String pattern : runtimePublicPaths) {
            if (pathMatcher.match(pattern, normalizedPath)) {
                log.trace("Path {} matches runtime public pattern {}", normalizedPath, pattern);
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a path is public for a specific service.
     *
     * @param path the request path
     * @param serviceName the target service name
     * @return true if path is public for the service
     */
    public boolean isPublicPathForService(String path, String serviceName) {
        if (isPublicPath(path)) {
            return true;
        }

        String normalizedPath = normalizePath(path);

        // Check service-specific paths
        for (String pattern : authProperties.getServicePublicPaths(serviceName)) {
            if (pathMatcher.match(pattern, normalizedPath)) {
                log.trace("Path {} matches service {} public pattern {}", normalizedPath, serviceName, pattern);
                return true;
            }
        }

        return false;
    }

    /**
     * Register a runtime public path.
     * Useful for dynamically adding paths without config changes.
     *
     * @param pattern Ant-style path pattern
     */
    public void registerPublicPath(String pattern) {
        if (pattern != null && !pattern.isBlank()) {
            runtimePublicPaths.add(pattern);
            log.info("Registered runtime public path: {}", pattern);
        }
    }

    /**
     * Unregister a runtime public path.
     *
     * @param pattern path pattern to remove
     */
    public void unregisterPublicPath(String pattern) {
        if (runtimePublicPaths.remove(pattern)) {
            log.info("Unregistered runtime public path: {}", pattern);
        }
    }

    /**
     * Get all registered public paths (default + configured + runtime).
     *
     * @return list of all public path patterns
     */
    public List<String> getAllPublicPaths() {
        List<String> allPaths = new ArrayList<>();
        allPaths.addAll(DEFAULT_PUBLIC_PATHS);
        allPaths.addAll(authProperties.getGlobalPublicPaths());
        allPaths.addAll(runtimePublicPaths);
        return allPaths;
    }

    /**
     * Get public paths for a specific service.
     *
     * @param serviceName service name
     * @return list of public paths for the service
     */
    public List<String> getPublicPathsForService(String serviceName) {
        List<String> paths = new ArrayList<>(getAllPublicPaths());
        paths.addAll(authProperties.getServicePublicPaths(serviceName));
        return paths;
    }

    /**
     * Normalize a path for consistent matching.
     */
    private String normalizePath(String path) {
        // Remove query string
        int queryIndex = path.indexOf('?');
        if (queryIndex > 0) {
            path = path.substring(0, queryIndex);
        }

        // Remove trailing slashes (except for root)
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * Clear all runtime-registered paths.
     * Useful for testing.
     */
    public void clearRuntimePaths() {
        runtimePublicPaths.clear();
        log.info("Cleared all runtime public paths");
    }
}
