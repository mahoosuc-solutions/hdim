package com.healthdata.gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate Limiting Filter for API Gateway
 *
 * Implements per-client rate limiting to prevent abuse and ensure fair usage.
 * Rate limits are applied based on:
 * 1. Client IP address (for unauthenticated requests)
 * 2. User ID (for authenticated requests)
 * 3. Tenant ID (for tenant-level limits)
 *
 * HIPAA Compliance: Rate limiting helps protect against DoS attacks and
 * ensures system availability for legitimate healthcare operations.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterRegistry rateLimiterRegistry;

    @Value("${gateway.rate-limit.requests-per-second:100}")
    private int requestsPerSecond;

    @Value("${gateway.rate-limit.burst-capacity:150}")
    private int burstCapacity;

    public RateLimitFilter(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip rate limiting for health checks
        if (isHealthCheck(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rateLimitKey = determineRateLimitKey(request);
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimitKey);

        if (rateLimiter.acquirePermission()) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for key: {} on path: {}", rateLimitKey, request.getRequestURI());
            sendRateLimitResponse(response, rateLimitKey);
        }
    }

    /**
     * Determine the rate limit key based on authentication state.
     * Priority: User ID > Tenant ID > IP Address
     */
    private String determineRateLimitKey(HttpServletRequest request) {
        // Try to get user from JWT (set by JwtAuthenticationFilter)
        String userId = (String) request.getAttribute("userId");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // Try tenant ID from header
        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId != null && !tenantId.isEmpty()) {
            return "tenant:" + tenantId;
        }

        // Fall back to IP address
        String clientIp = getClientIp(request);
        return "ip:" + clientIp;
    }

    /**
     * Extract client IP, handling proxy headers.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Check if request is a health check endpoint.
     */
    private boolean isHealthCheck(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator/health") ||
               path.contains("/health") ||
               path.equals("/");
    }

    /**
     * Send 429 Too Many Requests response.
     */
    private void sendRateLimitResponse(HttpServletResponse response, String key) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "1");
        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerSecond));
        response.setHeader("X-RateLimit-Remaining", "0");

        response.getWriter().write(String.format(
            "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please retry after 1 second.\",\"status\":429,\"key\":\"%s\"}",
            key.split(":")[0] // Only expose key type, not value
        ));
        response.getWriter().flush();
    }
}
