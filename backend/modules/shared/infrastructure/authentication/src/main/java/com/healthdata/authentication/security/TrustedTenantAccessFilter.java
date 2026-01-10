package com.healthdata.authentication.security;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Tenant access filter that validates access using request attributes instead of database lookup.
 *
 * This filter is designed to work with gateway-authenticated requests where the gateway has
 * already validated the JWT and injected trusted headers. The TrustedHeaderAuthFilter extracts
 * user context from these headers and stores tenant IDs in request attributes.
 *
 * SECURITY ARCHITECTURE:
 * ┌────────┐   JWT    ┌─────────────┐   X-Auth-* Headers   ┌─────────────────┐
 * │ Client │─────────▶│   Gateway   │────────────────────▶│ Backend Service │
 * └────────┘          └─────────────┘                      └─────────────────┘
 *                           │                                     │
 *                     Validates JWT                         Trusts gateway
 *                     Strips external                       Reads attributes
 *                     X-Auth-* headers                      No DB lookup
 *                     Injects trusted                       No JWT re-validation
 *                     headers with HMAC
 *
 * Prerequisites:
 * - Request must pass through TrustedHeaderAuthFilter first
 * - TrustedHeaderAuthFilter stores tenant IDs in: request.getAttribute(ATTR_TENANT_IDS)
 * - Gateway must be the only entry point (backend services not publicly accessible)
 *
 * Filter Chain Order:
 * 1. TrustedHeaderAuthFilter - validates gateway headers, sets SecurityContext and attributes
 * 2. TrustedTenantAccessFilter - validates X-Tenant-ID against allowed tenants from attributes
 * 3. Controller - receives validated request
 *
 * This addresses CRITICAL finding: "Complete Bypass of Tenant Isolation"
 * without requiring database access from backend services.
 *
 * @see TrustedHeaderAuthFilter
 * @see AuthHeaderConstants#ATTR_TENANT_IDS
 */
@Slf4j
@RequiredArgsConstructor
public class TrustedTenantAccessFilter extends OncePerRequestFilter {

    /**
     * Metrics registry for collecting tenant isolation metrics.
     */
    private final MeterRegistry meterRegistry;

    /**
     * Metrics counters for tenant isolation.
     */
    private Counter tenantViolationCounter;
    private Counter missingTenantContextCounter;

    /**
     * Public endpoints that don't require tenant validation.
     */
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/actuator/health",
        "/actuator/info",
        "/api/v1/health",
        "/fhir/metadata",
        "/metadata",
        "/swagger-ui",
        "/v3/api-docs",
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh"
    );

    /**
     * Initialize metrics after construction.
     */
    @Override
    public void afterPropertiesSet() throws ServletException {
        super.afterPropertiesSet();
        initializeMetrics();
    }

    /**
     * Initialize Micrometer metrics for tenant isolation tracking.
     */
    private void initializeMetrics() {
        tenantViolationCounter = Counter.builder("tenant_violations_total")
            .description("Total number of tenant isolation violations")
            .tag("filter", "trusted_tenant_access")
            .register(meterRegistry);

        missingTenantContextCounter = Counter.builder("missing_tenant_context_total")
            .description("Total number of requests with missing tenant context")
            .tag("filter", "trusted_tenant_access")
            .register(meterRegistry);

        log.info("Metrics initialized for TrustedTenantAccessFilter");
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String requestedTenantId = request.getHeader("X-Tenant-ID");

        log.debug("Trusted tenant access filter: path={}, tenantId={}", requestPath, requestedTenantId);

        // Skip validation for public endpoints
        if (isPublicPath(requestPath)) {
            log.debug("Public path, skipping tenant validation: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // If no tenant ID in header, allow (controller will handle validation if needed)
        // This allows endpoints that don't require tenant context
        if (requestedTenantId == null || requestedTenantId.trim().isEmpty()) {
            log.debug("No tenant ID in request, allowing");
            filterChain.doFilter(request, response);
            return;
        }

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If no authentication or anonymous, let Spring Security handle it
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            log.debug("No authentication, allowing Spring Security to handle");
            filterChain.doFilter(request, response);
            return;
        }

        // Get tenant IDs from request attributes (set by TrustedHeaderAuthFilter)
        @SuppressWarnings("unchecked")
        Set<String> allowedTenants = (Set<String>) request.getAttribute(AuthHeaderConstants.ATTR_TENANT_IDS);

        if (allowedTenants == null || allowedTenants.isEmpty()) {
            log.warn("No tenant IDs found in request attributes. User: {}. " +
                     "Ensure TrustedHeaderAuthFilter runs before this filter.",
                authentication.getName());
            missingTenantContextCounter.increment();

            // In strict mode, deny access. Otherwise, allow controller to handle.
            // For gateway-authenticated requests, missing tenant IDs indicates a problem.
            sendForbiddenResponse(response, "No tenant access configured for user");
            return;
        }

        // Validate tenant access
        if (!allowedTenants.contains(requestedTenantId)) {
            log.warn("SECURITY: User {} attempted to access unauthorized tenant: {}. " +
                     "Authorized tenants: {}",
                authentication.getName(), requestedTenantId, allowedTenants);
            tenantViolationCounter.increment();

            sendForbiddenResponse(response,
                String.format("Access denied to tenant: %s", requestedTenantId));
            return;
        }

        log.debug("Tenant access validated: user={}, tenant={}, allowedTenants={}",
            authentication.getName(), requestedTenantId, allowedTenants);

        // Access granted, continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request path is public (doesn't require tenant validation).
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Send 403 Forbidden response with JSON error body.
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"Forbidden\",\"message\":\"%s\",\"status\":403}",
            escapeJson(message)
        ));
        response.getWriter().flush();
    }

    /**
     * Simple JSON string escaping.
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip filter for actuator and swagger endpoints (redundant with isPublicPath but faster)
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/favicon.ico");
    }
}
