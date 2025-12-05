package com.healthdata.quality.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Interceptor for WebSocket Connections
 *
 * Security Benefits:
 * - Prevents brute-force authentication attacks
 * - Mitigates denial-of-service (DoS) attacks
 * - Limits resource exhaustion
 * - Protects against connection flooding
 *
 * Rate Limits:
 * - Per IP address: Configurable connection attempts per minute
 * - Per user: Max concurrent connections limit (enforced in handler)
 *
 * Rate Limit Windows:
 * - Connection attempts: 60 seconds (rolling window)
 * - Cleanup runs every 60 seconds to remove expired entries
 */
@Component
@Slf4j
public class RateLimitingInterceptor implements HandshakeInterceptor {

    @Value("${websocket.security.rate-limit.connections-per-minute:10}")
    private int connectionsPerMinute;

    @Value("${websocket.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    // Map: IP address -> connection attempts in current window
    private final Map<String, ConnectionAttempts> ipConnectionAttempts = new ConcurrentHashMap<>();

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        if (!rateLimitEnabled) {
            return true; // Rate limiting disabled
        }

        // Extract IP address
        String clientIp = extractClientIp(request);

        if (clientIp == null) {
            log.warn("Unable to extract client IP address for rate limiting");
            return true; // Allow connection if IP can't be determined (fail open)
        }

        // Check rate limit
        ConnectionAttempts attempts = ipConnectionAttempts.computeIfAbsent(
                clientIp,
                k -> new ConnectionAttempts()
        );

        // Clean up old attempts
        attempts.cleanup();

        // Check if rate limit exceeded
        if (attempts.incrementAndCheck() > connectionsPerMinute) {
            log.warn("SECURITY: WebSocket rate limit exceeded for IP: {} (limit: {}/min, attempts: {})",
                    clientIp, connectionsPerMinute, attempts.getCount());

            // Store rate limit violation in attributes for audit logging
            attributes.put("rateLimitViolation", true);
            attributes.put("clientIp", clientIp);
            attributes.put("attemptsCount", attempts.getCount());

            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return false;
        }

        log.debug("WebSocket rate limit check passed for IP: {} ({}/{})",
                clientIp, attempts.getCount(), connectionsPerMinute);

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No action needed
    }

    /**
     * Extract client IP address from request
     * Checks X-Forwarded-For header first (for proxy/load balancer scenarios)
     */
    private String extractClientIp(ServerHttpRequest request) {
        // Check X-Forwarded-For header (set by reverse proxies)
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first (original client)
            return forwardedFor.split(",")[0].trim();
        }

        // Fall back to remote address
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return null;
    }

    /**
     * Scheduled cleanup of expired rate limit entries
     * Runs every 60 seconds
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredEntries() {
        int sizeBefore = ipConnectionAttempts.size();

        // Remove entries with no recent attempts
        ipConnectionAttempts.entrySet().removeIf(entry -> {
            entry.getValue().cleanup();
            return entry.getValue().getCount() == 0;
        });

        int sizeAfter = ipConnectionAttempts.size();
        if (sizeBefore > sizeAfter) {
            log.debug("Rate limiting cleanup: removed {} expired entries ({} -> {})",
                    sizeBefore - sizeAfter, sizeBefore, sizeAfter);
        }
    }

    /**
     * Get current rate limit configuration
     */
    public int getConnectionsPerMinute() {
        return connectionsPerMinute;
    }

    /**
     * Check if rate limiting is enabled
     */
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    /**
     * Connection attempts tracking with rolling window
     */
    private static class ConnectionAttempts {
        private final AtomicInteger count = new AtomicInteger(0);
        private long windowStart = System.currentTimeMillis();
        private static final long WINDOW_SIZE_MS = 60000; // 60 seconds

        /**
         * Increment attempt counter and return current count
         */
        public int incrementAndCheck() {
            return count.incrementAndGet();
        }

        /**
         * Get current attempt count
         */
        public int getCount() {
            return count.get();
        }

        /**
         * Clean up old attempts (reset if window expired)
         */
        public void cleanup() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_SIZE_MS) {
                // Window expired, reset
                count.set(0);
                windowStart = now;
            }
        }
    }
}
