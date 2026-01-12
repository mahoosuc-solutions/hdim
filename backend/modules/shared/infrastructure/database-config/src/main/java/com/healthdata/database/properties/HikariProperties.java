package com.healthdata.database.properties;

import com.healthdata.database.config.TrafficTier;
import lombok.Data;

/**
 * HikariCP configuration properties with sensible defaults from Phase 3 standardization.
 *
 * Configuration Prefix: healthdata.database.hikari
 *
 * Standard Configuration Pattern:
 * <pre>
 * healthdata:
 *   database:
 *     hikari:
 *       traffic-tier: MEDIUM              # Required: Determines pool size
 *       maximum-pool-size: 20             # Optional: Override tier default
 *       minimum-idle: 5                   # Optional: Override tier default
 *       connection-timeout: 20000         # 20 seconds (fail fast)
 *       idle-timeout: 300000              # 5 minutes (matches network timeout)
 *       max-lifetime: 1800000             # 30 minutes (6x safety margin)
 *       keepalive-time: 240000            # 4 minutes (proactive health check)
 *       leak-detection-threshold: 60000   # 60 seconds
 *       validation-timeout: 5000          # 5 seconds (fail fast)
 * </pre>
 *
 * Critical Timing Rules:
 * - max-lifetime MUST be >= 6x idle-timeout (prevents stale connections)
 * - keepalive-time SHOULD be < idle-timeout (proactive health checks)
 * - All timeout values in milliseconds
 */
@Data
public class HikariProperties {

    /**
     * Traffic tier classification - determines pool size defaults.
     * REQUIRED if maximum-pool-size is not explicitly set.
     */
    private TrafficTier trafficTier;

    /**
     * Maximum pool size. If not set, derived from traffic tier.
     * HIGH: 50, MEDIUM: 20, LOW: 10
     */
    private Integer maximumPoolSize;

    /**
     * Minimum idle connections. If not set, derived from traffic tier.
     * HIGH: 10, MEDIUM: 5, LOW: 5
     */
    private Integer minimumIdle;

    /**
     * Connection timeout in milliseconds.
     * Default: 20000ms (20 seconds) - fail fast to prevent request queuing
     */
    private long connectionTimeout = 20000;

    /**
     * Idle timeout in milliseconds.
     * Default: 300000ms (5 minutes) - matches Docker/PostgreSQL TCP timeout
     */
    private long idleTimeout = 300000;

    /**
     * Max connection lifetime in milliseconds.
     * Default: 1800000ms (30 minutes) - provides 6x safety margin over idle timeout
     *
     * CRITICAL: Must be at least 6x idle-timeout to prevent connection pool exhaustion.
     * See Phase 3 bug fixes (agent-builder, demo-seeding, notification services).
     */
    private long maxLifetime = 1800000;

    /**
     * Keepalive time in milliseconds.
     * Default: 240000ms (4 minutes) - proactive health check before 5-minute timeout
     */
    private long keepaliveTime = 240000;

    /**
     * Leak detection threshold in milliseconds.
     * Default: 60000ms (60 seconds) - detect connections held too long
     */
    private long leakDetectionThreshold = 60000;

    /**
     * Connection validation timeout in milliseconds.
     * Default: 5000ms (5 seconds) - fail fast on dead connections
     */
    private long validationTimeout = 5000;

    /**
     * Auto-commit behavior.
     * Default: true (standard JDBC behavior)
     */
    private boolean autoCommit = true;

    /**
     * Get effective maximum pool size based on traffic tier or explicit setting.
     *
     * @return pool size (explicit override or tier default)
     * @throws IllegalStateException if neither trafficTier nor maximumPoolSize is configured
     */
    public int getEffectiveMaximumPoolSize() {
        if (maximumPoolSize != null) {
            return maximumPoolSize;
        }
        if (trafficTier != null) {
            return trafficTier.getPoolSize();
        }
        throw new IllegalStateException(
                "Either trafficTier or maximumPoolSize must be configured");
    }

    /**
     * Get effective minimum idle connections based on traffic tier or explicit setting.
     *
     * @return minimum idle (explicit override or tier default)
     */
    public int getEffectiveMinimumIdle() {
        if (minimumIdle != null) {
            return minimumIdle;
        }
        if (trafficTier != null) {
            return trafficTier.getMinIdle();
        }
        // Default to 20% of max pool size if neither tier nor explicit value set
        return (int) (getEffectiveMaximumPoolSize() * 0.2);
    }
}
