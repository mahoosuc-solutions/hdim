package com.healthdata.gateway.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Per-Tenant Rate Limiting Filter
 *
 * Applies differentiated rate limits based on tenant tier and endpoint type.
 * Runs after authentication to have access to tenant context.
 */
@Component
@Order(10) // Run after authentication filters
public class TenantRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantRateLimitFilter.class);

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    // Paths that bypass rate limiting
    private static final List<String> BYPASS_PATHS = Arrays.asList(
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/health",
            "/ready",
            "/live"
    );

    @Autowired
    private TenantRateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Bypass rate limiting for health checks
        if (shouldBypass(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract tenant and user from request
        String tenantId = extractTenantId(request);
        String userId = extractUserId();

        // Determine endpoint type
        TenantRateLimitService.EndpointType endpointType =
                rateLimitService.determineEndpointType(request.getMethod(), path);

        // Check per-user rate limit
        TenantRateLimitService.RateLimitResult userResult =
                rateLimitService.tryConsume(tenantId, userId, endpointType);

        if (!userResult.isAllowed()) {
            handleRateLimitExceeded(response, userResult, "user");
            return;
        }

        // Check tenant aggregate rate limit
        if (tenantId != null && !tenantId.isEmpty()) {
            TenantRateLimitService.RateLimitResult tenantResult =
                    rateLimitService.tryConsumeTenantAggregate(tenantId);

            if (!tenantResult.isAllowed()) {
                handleRateLimitExceeded(response, tenantResult, "tenant");
                return;
            }
        }

        // Add rate limit headers to response
        addRateLimitHeaders(response, userResult);

        // Continue with request
        filterChain.doFilter(request, response);
    }

    private boolean shouldBypass(String path) {
        return BYPASS_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractTenantId(HttpServletRequest request) {
        // First try header
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }

        // Try to extract from JWT claims via security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> details = (java.util.Map<String, Object>) auth.getDetails();
            Object tenant = details.get("tenant_id");
            if (tenant != null) {
                return tenant.toString();
            }
        }

        return null;
    }

    private String extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return null;
    }

    private void handleRateLimitExceeded(HttpServletResponse response,
                                         TenantRateLimitService.RateLimitResult result,
                                         String limitType) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(result.getRetryAfterSeconds()));
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, "0");
        response.setHeader(RATE_LIMIT_LIMIT_HEADER, String.valueOf(result.getLimit()));

        String errorMessage = String.format(
                "{\"error\":\"rate_limit_exceeded\",\"message\":\"%s rate limit exceeded. Retry after %d seconds.\",\"retry_after\":%d,\"limit\":%d}",
                limitType, result.getRetryAfterSeconds(), result.getRetryAfterSeconds(), result.getLimit());

        response.getWriter().write(errorMessage);
        response.getWriter().flush();

        log.warn("Rate limit exceeded: type={}, retryAfter={}s, limit={}",
                limitType, result.getRetryAfterSeconds(), result.getLimit());
    }

    private void addRateLimitHeaders(HttpServletResponse response,
                                     TenantRateLimitService.RateLimitResult result) {
        if (result.getRemainingTokens() >= 0) {
            response.setHeader(RATE_LIMIT_REMAINING_HEADER,
                    String.valueOf(result.getRemainingTokens()));
        }
        if (result.getLimit() > 0) {
            response.setHeader(RATE_LIMIT_LIMIT_HEADER,
                    String.valueOf(result.getLimit()));
        }
        // Reset time is approximately 1 second from now
        response.setHeader(RATE_LIMIT_RESET_HEADER,
                String.valueOf(System.currentTimeMillis() / 1000 + 1));
    }
}
