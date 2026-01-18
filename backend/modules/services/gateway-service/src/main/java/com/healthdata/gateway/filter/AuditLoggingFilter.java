package com.healthdata.gateway.filter;

import com.healthdata.gateway.dto.AuditLogRequest;
import com.healthdata.gateway.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Audit Logging Filter for Phase 2.0 Team 2
 *
 * Captures all endpoint access for HIPAA compliance (164.312(b))
 * Logs request/response metadata asynchronously
 *
 * Important: PHI is NOT logged - only access metadata and identifiers
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;

    // Endpoints that should be excluded from audit logging
    private static final String[] AUDIT_EXCLUDED_PATHS = {
        "/actuator",
        "/actuator/health",
        "/swagger-ui",
        "/v3/api-docs"
    };

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip audit logging for excluded paths
        if (shouldExcludeFromAudit(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // Extract request metadata
            String httpMethod = request.getMethod();
            String requestPath = request.getRequestURI();
            String queryParameters = sanitizeQueryParameters(request.getQueryString());
            String tenantId = extractTenantId(request);
            String clientIp = extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String traceId = extractTraceId(request);
            String spanId = extractSpanId(request);

            // Extract security context (after authentication)
            String userId = null;
            String username = null;
            String roles = null;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                }
                userId = extractUserId(request);
                roles = extractRoles(auth);
            }

            // Determine authorization requirement and result
            String requiredRole = extractRequiredRole(requestPath);
            Boolean authorizationAllowed = auth != null && auth.isAuthenticated();

            // Continue filter chain and capture response
            try {
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                // Log the exception but don't swallow it
                throw e;
            }

            // Calculate response time
            long responseTime = System.currentTimeMillis() - startTime;
            Integer httpStatusCode = response.getStatus();
            Boolean success = isSuccessful(httpStatusCode);

            // Build audit log request
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                .timestamp(Instant.now())
                .httpMethod(httpMethod)
                .requestPath(requestPath)
                .queryParameters(queryParameters)
                .userId(userId)
                .username(username)
                .tenantId(tenantId)
                .roles(roles)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .httpStatusCode(httpStatusCode)
                .responseTimeMs(Math.toIntExact(responseTime))
                .success(success)
                .authorizationAllowed(authorizationAllowed)
                .requiredRole(requiredRole)
                .traceId(traceId)
                .spanId(spanId)
                .build();

            // Submit audit log asynchronously
            auditLogService.logAccessAsync(auditRequest);

        } catch (Exception e) {
            log.error("Error in audit logging filter", e);
            // Don't rethrow - audit logging failure should not break request processing
        }
    }

    /**
     * Extract client identifier from JWT attribute or header
     */
    private String extractUserId(HttpServletRequest request) {
        // Try to extract user ID from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }

        // Fall back to JWT attribute if set by authentication filter
        Object userAttribute = request.getAttribute("user_id");
        if (userAttribute != null) {
            return userAttribute.toString();
        }

        return null;
    }

    /**
     * Extract tenant ID from header
     */
    private String extractTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }

        // Fall back to tenant attribute if set by security filter
        Object tenantAttribute = request.getAttribute("tenant_id");
        if (tenantAttribute != null) {
            return tenantAttribute.toString();
        }

        return "unknown";
    }

    /**
     * Extract client IP address from request
     * Respects X-Forwarded-For header for proxied requests
     */
    private String extractClientIp(HttpServletRequest request) {
        // Check for X-Forwarded-For header (from load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, use first one
            return xForwardedFor.split(",")[0].trim();
        }

        // Check for X-Real-IP header (alternative)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }

    /**
     * Extract comma-separated roles from authentication
     */
    private String extractRoles(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return null;
        }

        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));
    }

    /**
     * Extract trace ID from request (OpenTelemetry)
     */
    private String extractTraceId(HttpServletRequest request) {
        // Try standard OpenTelemetry header
        String traceId = request.getHeader("traceparent");
        if (traceId != null && !traceId.isEmpty()) {
            // Extract trace ID from W3C format: version-trace_id-parent_id-trace_flags
            String[] parts = traceId.split("-");
            if (parts.length >= 2) {
                return parts[1];
            }
        }

        // Fall back to custom header
        String customTraceId = request.getHeader("X-Trace-ID");
        if (customTraceId != null && !customTraceId.isEmpty()) {
            return customTraceId;
        }

        return null;
    }

    /**
     * Extract span ID from request (OpenTelemetry)
     */
    private String extractSpanId(HttpServletRequest request) {
        // Try standard OpenTelemetry header
        String traceId = request.getHeader("traceparent");
        if (traceId != null && !traceId.isEmpty()) {
            // Extract span ID from W3C format: version-trace_id-parent_id-trace_flags
            String[] parts = traceId.split("-");
            if (parts.length >= 3) {
                return parts[2];
            }
        }

        // Fall back to custom header
        String customSpanId = request.getHeader("X-Span-ID");
        if (customSpanId != null && !customSpanId.isEmpty()) {
            return customSpanId;
        }

        return null;
    }

    /**
     * Sanitize query parameters to remove PHI
     * Removes parameters that commonly contain sensitive data
     */
    private String sanitizeQueryParameters(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }

        // List of parameter names that might contain PHI
        String[] sensitiveParams = {"ssn", "mrn", "dob", "password", "apikey", "secret"};

        String sanitized = queryString;
        for (String param : sensitiveParams) {
            // Replace sensitive values with [REDACTED]
            sanitized = sanitized.replaceAll("(?i)(" + param + "=)[^&]*", "$1[REDACTED]");
        }

        return sanitized;
    }

    /**
     * Determine if HTTP status code indicates success
     */
    private Boolean isSuccessful(Integer statusCode) {
        if (statusCode == null) {
            return null;
        }
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Extract required role for endpoint (simplified logic)
     */
    private String extractRequiredRole(String requestPath) {
        // Admin endpoints
        if (requestPath.contains("/admin/") || requestPath.contains("/actuator")) {
            return "ADMIN";
        }

        // Evaluator endpoints
        if (requestPath.contains("/evaluate/") || requestPath.contains("/measures/")) {
            return "EVALUATOR";
        }

        // Most endpoints are accessible to VIEWER role
        return "VIEWER";
    }

    /**
     * Check if request path should be excluded from audit logging
     */
    private boolean shouldExcludeFromAudit(String path) {
        for (String excludedPath : AUDIT_EXCLUDED_PATHS) {
            if (path.startsWith(excludedPath)) {
                return true;
            }
        }
        return false;
    }
}
