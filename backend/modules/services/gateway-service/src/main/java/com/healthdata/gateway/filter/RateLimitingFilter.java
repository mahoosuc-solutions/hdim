package com.healthdata.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.gateway.config.RateLimitConfiguration;
import com.healthdata.gateway.dto.RateLimitResult;
import com.healthdata.gateway.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limiting Filter for Phase 2.0
 *
 * Implements per-endpoint, per-tenant, and per-role rate limiting
 * using Redis as the backend storage.
 *
 * Returns 429 (Too Many Requests) when limit is exceeded.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitConfiguration config;
    private final ObjectMapper objectMapper;

    // Endpoints that should be excluded from rate limiting
    private static final String[] RATE_LIMIT_EXCLUDED_PATHS = {
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

        // Skip rate limiting if disabled
        if (!config.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for certain paths
        if (shouldExcludeFromRateLimit(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract client identifier
            String clientId = extractClientId(request);
            String endpoint = request.getRequestURI();
            String tenantId = request.getHeader("X-Tenant-ID");

            // Check rate limit
            RateLimitResult result = rateLimitService.checkLimit(
                clientId, endpoint, tenantId);

            // Add rate limit headers to response
            addRateLimitHeaders(response, result);

            // Check if request is allowed
            if (!result.isAllowed()) {
                handleRateLimitExceeded(response, result);
                return;
            }

            // Request is allowed, continue filter chain
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error in rate limiting filter", e);
            // Fail open - allow request if rate limiter fails
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Extract client identifier for rate limiting
     * Priority: User ID > IP address
     */
    private String extractClientId(HttpServletRequest request) {
        // Try to extract user ID from JWT (added by JwtAuthenticationFilter)
        Object userAttribute = request.getAttribute("user_id");
        if (userAttribute != null) {
            return "user:" + userAttribute.toString();
        }

        // Fall back to IP address for unauthenticated requests
        String clientIp = extractClientIp(request);
        return "ip:" + clientIp;
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
     * Check if request path should be excluded from rate limiting
     */
    private boolean shouldExcludeFromRateLimit(String path) {
        for (String excludedPath : RATE_LIMIT_EXCLUDED_PATHS) {
            if (path.startsWith(excludedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add rate limit headers to response
     */
    private void addRateLimitHeaders(
            HttpServletResponse response,
            RateLimitResult result) {

        response.setHeader("X-RateLimit-Limit",
            result.getLimitHeader());
        response.setHeader("X-RateLimit-Remaining",
            result.getRemainingHeader());
        response.setHeader("X-RateLimit-Reset",
            result.getResetHeader());
    }

    /**
     * Handle rate limit exceeded response
     */
    private void handleRateLimitExceeded(
            HttpServletResponse response,
            RateLimitResult result) throws IOException {

        // Set HTTP status 429 Too Many Requests
        response.setStatus(429);

        // Set retry-after header
        response.setHeader("Retry-After",
            result.getRetryAfterHeader());

        // Set content type
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Rate Limit Exceeded");
        errorResponse.put("message",
            "Too many requests. Please retry after " +
            result.getRetryAfterSeconds() + " seconds.");
        errorResponse.put("retryAfter",
            result.getRetryAfterSeconds());
        errorResponse.put("limit", result.getLimit());
        errorResponse.put("remaining", result.getRemaining());

        // Write error response
        response.getWriter().write(
            objectMapper.writeValueAsString(errorResponse));

        log.info("Rate limit exceeded - limit: {}, " +
                "remaining: {}, retry-after: {} seconds",
            result.getLimit(), result.getRemaining(),
            result.getRetryAfterSeconds());
    }
}
