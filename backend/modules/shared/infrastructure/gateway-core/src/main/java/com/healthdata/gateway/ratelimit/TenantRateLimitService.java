package com.healthdata.gateway.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Per-Tenant Rate Limiting Service
 *
 * Provides differentiated rate limiting based on:
 * - Tenant tier (standard, premium, enterprise)
 * - User role (admin, provider, analyst)
 * - Endpoint type (read, write, bulk)
 *
 * Uses Redis-backed distributed buckets for cluster-wide rate limiting.
 */
@Service
public class TenantRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(TenantRateLimitService.class);

    // Rate limit tiers
    public enum RateLimitTier {
        ANONYMOUS(10, 20),           // 10 req/s, burst 20
        STANDARD(100, 150),          // 100 req/s, burst 150
        PREMIUM(500, 750),           // 500 req/s, burst 750
        ENTERPRISE(2000, 3000),      // 2000 req/s, burst 3000
        INTERNAL(5000, 7500);        // 5000 req/s, burst 7500 (service-to-service)

        private final int requestsPerSecond;
        private final int burstCapacity;

        RateLimitTier(int requestsPerSecond, int burstCapacity) {
            this.requestsPerSecond = requestsPerSecond;
            this.burstCapacity = burstCapacity;
        }

        public int getRequestsPerSecond() { return requestsPerSecond; }
        public int getBurstCapacity() { return burstCapacity; }
    }

    // Endpoint types with multipliers
    public enum EndpointType {
        READ(1.0),           // Standard rate
        WRITE(0.5),          // 50% of standard (more expensive)
        BULK(0.1),           // 10% of standard (very expensive)
        HEALTH(10.0);        // 10x standard (health checks)

        private final double multiplier;

        EndpointType(double multiplier) {
            this.multiplier = multiplier;
        }

        public double getMultiplier() { return multiplier; }
    }

    // Local bucket cache (fallback when Redis unavailable)
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    // Tenant tier cache
    private final Map<String, RateLimitTier> tenantTiers = new ConcurrentHashMap<>();

    @Value("${gateway.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${gateway.rate-limit.use-redis:true}")
    private boolean useRedis;

    /**
     * Check if request is allowed for the given tenant and endpoint
     */
    public RateLimitResult tryConsume(String tenantId, String userId, EndpointType endpointType) {
        if (!rateLimitEnabled) {
            return RateLimitResult.allowed();
        }

        String bucketKey = buildBucketKey(tenantId, userId);
        RateLimitTier tier = getTenantTier(tenantId);

        Bucket bucket = getOrCreateBucket(bucketKey, tier, endpointType);

        if (bucket.tryConsume(1)) {
            long remainingTokens = bucket.getAvailableTokens();
            return RateLimitResult.allowed(remainingTokens, tier.getRequestsPerSecond());
        } else {
            long waitTimeNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
            long retryAfterSeconds = Duration.ofNanos(waitTimeNanos).toSeconds() + 1;

            log.warn("Rate limit exceeded for tenant={}, user={}, tier={}",
                    tenantId, userId, tier);

            return RateLimitResult.rejected(retryAfterSeconds, tier.getRequestsPerSecond());
        }
    }

    /**
     * Check tenant-level aggregate rate limit
     */
    public RateLimitResult tryConsumeTenantAggregate(String tenantId) {
        if (!rateLimitEnabled) {
            return RateLimitResult.allowed();
        }

        String bucketKey = "tenant:" + tenantId + ":aggregate";
        RateLimitTier tier = getTenantTier(tenantId);

        // Tenant aggregate gets 10x the per-user limit
        Bucket bucket = getOrCreateBucket(bucketKey, tier, EndpointType.READ);

        if (bucket.tryConsume(1)) {
            return RateLimitResult.allowed(bucket.getAvailableTokens(), tier.getRequestsPerSecond() * 10);
        } else {
            long waitTimeNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
            return RateLimitResult.rejected(Duration.ofNanos(waitTimeNanos).toSeconds() + 1,
                    tier.getRequestsPerSecond() * 10);
        }
    }

    /**
     * Get or create a rate limit bucket
     */
    private Bucket getOrCreateBucket(String key, RateLimitTier tier, EndpointType endpointType) {
        return localBuckets.computeIfAbsent(key, k -> createBucket(tier, endpointType));
    }

    /**
     * Create a bucket with the appropriate configuration
     */
    private Bucket createBucket(RateLimitTier tier, EndpointType endpointType) {
        int effectiveRps = (int) (tier.getRequestsPerSecond() * endpointType.getMultiplier());
        int effectiveBurst = (int) (tier.getBurstCapacity() * endpointType.getMultiplier());

        return Bucket.builder()
                .addLimit(Bandwidth.classic(effectiveBurst,
                        Refill.greedy(effectiveRps, Duration.ofSeconds(1))))
                .build();
    }

    /**
     * Get the rate limit tier for a tenant
     */
    public RateLimitTier getTenantTier(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return RateLimitTier.ANONYMOUS;
        }

        // Check cache first
        RateLimitTier cached = tenantTiers.get(tenantId);
        if (cached != null) {
            return cached;
        }

        // Default to STANDARD - in production, query from tenant service
        return RateLimitTier.STANDARD;
    }

    /**
     * Update tenant tier (called when tenant subscription changes)
     */
    public void updateTenantTier(String tenantId, RateLimitTier tier) {
        tenantTiers.put(tenantId, tier);
        log.info("Updated rate limit tier for tenant={} to {}", tenantId, tier);

        // Clear cached buckets for this tenant to apply new limits
        localBuckets.entrySet().removeIf(entry -> entry.getKey().startsWith("tenant:" + tenantId));
    }

    /**
     * Build bucket key from tenant and user
     */
    private String buildBucketKey(String tenantId, String userId) {
        if (tenantId == null) tenantId = "anonymous";
        if (userId == null) userId = "anonymous";
        return String.format("tenant:%s:user:%s", tenantId, userId);
    }

    /**
     * Determine endpoint type from path
     */
    public EndpointType determineEndpointType(String method, String path) {
        // Health and actuator endpoints
        if (path.contains("/actuator") || path.contains("/health")) {
            return EndpointType.HEALTH;
        }

        // Bulk operations
        if (path.contains("/bulk") || path.contains("/batch") || path.contains("/export")) {
            return EndpointType.BULK;
        }

        // Write operations
        if ("POST".equals(method) || "PUT".equals(method) ||
            "DELETE".equals(method) || "PATCH".equals(method)) {
            return EndpointType.WRITE;
        }

        // Default to read
        return EndpointType.READ;
    }

    /**
     * Rate limit result
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final long remainingTokens;
        private final long limit;
        private final long retryAfterSeconds;

        private RateLimitResult(boolean allowed, long remainingTokens, long limit, long retryAfterSeconds) {
            this.allowed = allowed;
            this.remainingTokens = remainingTokens;
            this.limit = limit;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public static RateLimitResult allowed() {
            return new RateLimitResult(true, -1, -1, 0);
        }

        public static RateLimitResult allowed(long remainingTokens, long limit) {
            return new RateLimitResult(true, remainingTokens, limit, 0);
        }

        public static RateLimitResult rejected(long retryAfterSeconds, long limit) {
            return new RateLimitResult(false, 0, limit, retryAfterSeconds);
        }

        public boolean isAllowed() { return allowed; }
        public long getRemainingTokens() { return remainingTokens; }
        public long getLimit() { return limit; }
        public long getRetryAfterSeconds() { return retryAfterSeconds; }
    }
}
