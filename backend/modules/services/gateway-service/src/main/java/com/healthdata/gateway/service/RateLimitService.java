package com.healthdata.gateway.service;

import com.healthdata.gateway.config.RateLimitConfiguration;
import com.healthdata.gateway.dto.RateLimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Rate Limiting Service for Phase 2.0
 *
 * Implements Redis-based token bucket algorithm for rate limiting
 * with support for:
 * - Per-tenant rate limits
 * - Per-role multipliers
 * - Endpoint-specific limits
 * - Sliding window (60-second windows)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitConfiguration config;

    private static final long WINDOW_DURATION_SECONDS = 60L;
    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit";

    /**
     * Check if request should be allowed based on rate limit
     */
    public RateLimitResult checkLimit(
            String clientId, String endpoint, String tenantId) {

        // Get base limit for endpoint
        RateLimitConfiguration.EndpointRateLimit endpointConfig =
            config.getConfigForEndpoint(endpoint);
        int baseLimit = endpointConfig.getLimitPerMinute();

        // Apply role multiplier if authenticated
        int adjustedLimit = applyRoleMultiplier(baseLimit);

        // Apply tenant override if exists
        if (config.hasTenantOverride(tenantId)) {
            adjustedLimit = config.getTenantLimit(tenantId);
        }

        // Build Redis key: ratelimit:{clientId}:{endpoint}:{window}
        String currentWindow = getCurrentWindow();
        String key = String.format("%s:%s:%s:%s",
            RATE_LIMIT_KEY_PREFIX, clientId, endpoint, currentWindow);

        try {
            // Atomic increment
            long current = redisTemplate.opsForValue().increment(key);

            // Set expiration on first request (window + buffer)
            if (current == 1) {
                redisTemplate.expire(key,
                    Duration.ofSeconds(WINDOW_DURATION_SECONDS + 1));
                log.debug("Created rate limit key: {} with limit: {}",
                    key, adjustedLimit);
            }

            // Calculate reset time
            Instant resetTime = getWindowResetTime();

            // Build result
            boolean allowed = current <= adjustedLimit;
            long remaining = Math.max(0, adjustedLimit - current);

            log.debug("Rate limit check - clientId: {}, endpoint: {}, " +
                    "current: {}/{}, allowed: {}, tenant: {}",
                clientId, endpoint, current, adjustedLimit, allowed, tenantId);

            return RateLimitResult.builder()
                .limit(adjustedLimit)
                .current(current)
                .remaining(remaining)
                .allowed(allowed)
                .resetTime(resetTime)
                .retryAfterSeconds(
                    allowed ? 0 : calculateRetryAfterSeconds(resetTime))
                .build();

        } catch (Exception e) {
            log.error("Error checking rate limit for clientId: {}, " +
                    "endpoint: {}, tenant: {}",
                clientId, endpoint, tenantId, e);

            // Fail open - allow request if Redis is down
            return RateLimitResult.builder()
                .limit(adjustedLimit)
                .current(0)
                .remaining(adjustedLimit)
                .allowed(true)  // Allow if check fails
                .resetTime(getWindowResetTime())
                .retryAfterSeconds(0)
                .build();
        }
    }

    /**
     * Apply role-based multiplier to limit
     */
    private int applyRoleMultiplier(int baseLimit) {
        try {
            Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()) {
                Collection<GrantedAuthority> authorities =
                    auth.getAuthorities();

                for (GrantedAuthority authority : authorities) {
                    String role = authority.getAuthority();

                    // Remove ROLE_ prefix if present
                    if (role.startsWith("ROLE_")) {
                        role = role.substring(5);
                    }

                    double multiplier =
                        config.getRoleMultiplier(role);

                    if (multiplier != 1.0) {
                        int adjusted = (int) (baseLimit * multiplier);
                        log.debug("Applied role multiplier: {} with {}: " +
                                "{} -> {}",
                            role, multiplier, baseLimit, adjusted);
                        return adjusted;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract role for multiplier", e);
        }

        return baseLimit;
    }

    /**
     * Get current window identifier (for sliding window)
     * Window is based on current minute: minute * 60 seconds
     */
    private String getCurrentWindow() {
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long windowNumber = currentTimeSeconds / WINDOW_DURATION_SECONDS;
        return String.valueOf(windowNumber);
    }

    /**
     * Get when current window resets (next minute)
     */
    private Instant getWindowResetTime() {
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long windowNumber = currentTimeSeconds / WINDOW_DURATION_SECONDS;
        long resetTimeSeconds = (windowNumber + 1) * WINDOW_DURATION_SECONDS;
        return Instant.ofEpochSecond(resetTimeSeconds);
    }

    /**
     * Calculate seconds until retry should happen
     */
    private long calculateRetryAfterSeconds(Instant resetTime) {
        long secondsUntilReset =
            resetTime.getEpochSecond() -
            System.currentTimeMillis() / 1000;
        return Math.max(1, secondsUntilReset);
    }

    /**
     * Reset rate limit for specific client/endpoint (for testing)
     */
    public void resetLimit(String clientId, String endpoint) {
        String pattern = RATE_LIMIT_KEY_PREFIX + ":" + clientId +
            ":" + endpoint + ":*";

        try {
            redisTemplate.getConnectionFactory()
                .getConnection()
                .del(pattern.getBytes());
            log.info("Reset rate limit for clientId: {}, endpoint: {}",
                clientId, endpoint);
        } catch (Exception e) {
            log.error("Error resetting rate limit", e);
        }
    }
}
