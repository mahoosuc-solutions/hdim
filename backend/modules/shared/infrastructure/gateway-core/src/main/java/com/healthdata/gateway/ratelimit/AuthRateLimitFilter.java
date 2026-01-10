package com.healthdata.gateway.ratelimit;

import com.healthdata.gateway.config.GatewayAuthProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * IP-based Rate Limiting Filter for Authentication Endpoints
 *
 * Protects auth endpoints from brute-force attacks by limiting requests per IP:
 * - Login: Configurable attempts per minute (default 10)
 * - Token refresh: Configurable attempts per minute (default 20)
 * - Blocks IP for configurable duration after exceeding limits
 *
 * Security Features:
 * - Per-IP rate limiting (not user-based, to prevent lockout attacks)
 * - Separate limits for login vs refresh endpoints
 * - Automatic block expiration
 * - IP extraction handles X-Forwarded-For for load balancers
 *
 * Order: -50 (runs early, before main auth filter but after security filters)
 */
@Slf4j
@Component
@Order(-50)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gateway.auth.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REFRESH_PATH = "/api/v1/auth/refresh";
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String MFA_PATH = "/api/v1/auth/mfa";

    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final GatewayAuthProperties authProperties;

    // IP-based buckets for login attempts
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    // IP-based buckets for refresh attempts
    private final Map<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();

    // Blocked IPs with expiration times
    private final Map<String, Long> blockedIps = new ConcurrentHashMap<>();

    // Cleanup scheduler
    private final ScheduledExecutorService cleanupExecutor =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auth-ratelimit-cleanup");
            t.setDaemon(true);
            return t;
        });

    /**
     * Initialize cleanup task
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        // Clean up expired blocks and old buckets every 5 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
        log.info("Auth rate limit filter initialized: login={}/min, refresh={}/min, block={}s",
            getRateLimitConfig().getLoginAttemptsPerMinute(),
            getRateLimitConfig().getRefreshAttemptsPerMinute(),
            getRateLimitConfig().getBlockDurationSeconds());
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate limit POST requests to auth endpoints
        if (!"POST".equals(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if this is an auth endpoint
        if (!isAuthEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);

        // Check if IP is blocked
        if (isBlocked(clientIp)) {
            long unblockTime = blockedIps.get(clientIp);
            long retryAfter = Math.max(1, (unblockTime - System.currentTimeMillis()) / 1000);
            handleBlocked(response, clientIp, retryAfter);
            return;
        }

        // Apply rate limit based on endpoint type
        RateLimitResult result;
        if (isLoginEndpoint(path)) {
            result = tryConsumeLogin(clientIp);
        } else if (isRefreshEndpoint(path)) {
            result = tryConsumeRefresh(clientIp);
        } else {
            // Registration and MFA use login limits
            result = tryConsumeLogin(clientIp);
        }

        if (!result.allowed) {
            // Block IP after exceeding rate limit
            blockIp(clientIp);
            handleRateLimitExceeded(response, clientIp, result);
            return;
        }

        // Add rate limit headers
        addRateLimitHeaders(response, result);

        filterChain.doFilter(request, response);
    }

    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/v1/auth/");
    }

    private boolean isLoginEndpoint(String path) {
        return path.equals(LOGIN_PATH) || path.equals(REGISTER_PATH) ||
               path.startsWith(MFA_PATH);
    }

    private boolean isRefreshEndpoint(String path) {
        return path.equals(REFRESH_PATH);
    }

    /**
     * Try to consume a login attempt token
     */
    private RateLimitResult tryConsumeLogin(String clientIp) {
        GatewayAuthProperties.RateLimitConfig config = getRateLimitConfig();
        Bucket bucket = loginBuckets.computeIfAbsent(clientIp,
            k -> createBucket(config.getLoginAttemptsPerMinute()));

        if (bucket.tryConsume(1)) {
            return new RateLimitResult(true, bucket.getAvailableTokens(),
                config.getLoginAttemptsPerMinute(), 0);
        }

        return new RateLimitResult(false, 0, config.getLoginAttemptsPerMinute(),
            config.getBlockDurationSeconds());
    }

    /**
     * Try to consume a refresh attempt token
     */
    private RateLimitResult tryConsumeRefresh(String clientIp) {
        GatewayAuthProperties.RateLimitConfig config = getRateLimitConfig();
        Bucket bucket = refreshBuckets.computeIfAbsent(clientIp,
            k -> createBucket(config.getRefreshAttemptsPerMinute()));

        if (bucket.tryConsume(1)) {
            return new RateLimitResult(true, bucket.getAvailableTokens(),
                config.getRefreshAttemptsPerMinute(), 0);
        }

        return new RateLimitResult(false, 0, config.getRefreshAttemptsPerMinute(),
            config.getBlockDurationSeconds());
    }

    /**
     * Create a rate limit bucket
     */
    private Bucket createBucket(int tokensPerMinute) {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(tokensPerMinute,
                Refill.intervally(tokensPerMinute, Duration.ofMinutes(1))))
            .build();
    }

    /**
     * Block an IP address
     */
    private void blockIp(String clientIp) {
        GatewayAuthProperties.RateLimitConfig config = getRateLimitConfig();
        long unblockTime = System.currentTimeMillis() +
            (config.getBlockDurationSeconds() * 1000L);
        blockedIps.put(clientIp, unblockTime);
        log.warn("SECURITY: Blocked IP {} for {} seconds due to rate limit exceeded",
            clientIp, config.getBlockDurationSeconds());
    }

    /**
     * Check if an IP is blocked
     */
    private boolean isBlocked(String clientIp) {
        Long unblockTime = blockedIps.get(clientIp);
        if (unblockTime == null) {
            return false;
        }
        if (System.currentTimeMillis() >= unblockTime) {
            blockedIps.remove(clientIp);
            return false;
        }
        return true;
    }

    /**
     * Extract client IP address, handling X-Forwarded-For header
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    /**
     * Handle rate limit exceeded
     */
    private void handleRateLimitExceeded(
        HttpServletResponse response,
        String clientIp,
        RateLimitResult result
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(result.retryAfterSeconds));
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, "0");
        response.setHeader(RATE_LIMIT_LIMIT_HEADER, String.valueOf(result.limit));

        String message = String.format(
            "{\"error\":\"rate_limit_exceeded\",\"message\":\"Too many authentication attempts. Please try again later.\",\"retry_after\":%d}",
            result.retryAfterSeconds);
        response.getWriter().write(message);

        log.warn("AUTH_RATE_LIMIT: IP {} exceeded rate limit, blocked for {}s",
            clientIp, result.retryAfterSeconds);
    }

    /**
     * Handle blocked IP
     */
    private void handleBlocked(
        HttpServletResponse response,
        String clientIp,
        long retryAfter
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfter));

        String message = String.format(
            "{\"error\":\"ip_blocked\",\"message\":\"IP temporarily blocked due to too many failed attempts.\",\"retry_after\":%d}",
            retryAfter);
        response.getWriter().write(message);

        log.debug("AUTH_BLOCKED: IP {} still blocked, retry in {}s", clientIp, retryAfter);
    }

    /**
     * Add rate limit headers to response
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(result.remaining));
        response.setHeader(RATE_LIMIT_LIMIT_HEADER, String.valueOf(result.limit));
    }

    /**
     * Cleanup expired blocks and old buckets
     */
    private void cleanup() {
        long now = System.currentTimeMillis();

        // Remove expired blocks
        blockedIps.entrySet().removeIf(entry -> now >= entry.getValue());

        // Clear old buckets (tokens naturally replenish, but this prevents memory growth)
        if (loginBuckets.size() > 10000) {
            log.info("Clearing login rate limit buckets (size: {})", loginBuckets.size());
            loginBuckets.clear();
        }
        if (refreshBuckets.size() > 10000) {
            log.info("Clearing refresh rate limit buckets (size: {})", refreshBuckets.size());
            refreshBuckets.clear();
        }
    }

    private GatewayAuthProperties.RateLimitConfig getRateLimitConfig() {
        GatewayAuthProperties.RateLimitConfig config = authProperties.getRateLimit();
        if (config == null) {
            // Return defaults
            config = new GatewayAuthProperties.RateLimitConfig();
        }
        return config;
    }

    /**
     * Rate limit result
     */
    private static class RateLimitResult {
        final boolean allowed;
        final long remaining;
        final long limit;
        final long retryAfterSeconds;

        RateLimitResult(boolean allowed, long remaining, long limit, long retryAfterSeconds) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.limit = limit;
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Skip for non-auth endpoints
        return !request.getRequestURI().startsWith("/api/v1/auth/");
    }
}
