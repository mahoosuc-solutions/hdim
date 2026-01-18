package com.healthdata.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Result of rate limit check
 *
 * Contains information about current usage and limits
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResult {

    // Limit configuration
    private int limit;

    // Current usage
    private long current;

    // Remaining requests in window
    private long remaining;

    // Whether request is allowed
    private boolean allowed;

    // When the rate limit window resets
    private Instant resetTime;

    // Seconds to wait before retrying
    private long retryAfterSeconds;

    /**
     * Get X-RateLimit-Limit header value
     */
    public String getLimitHeader() {
        return String.valueOf(limit);
    }

    /**
     * Get X-RateLimit-Remaining header value
     */
    public String getRemainingHeader() {
        return String.valueOf(remaining);
    }

    /**
     * Get X-RateLimit-Reset header value (epoch seconds)
     */
    public String getResetHeader() {
        return String.valueOf(resetTime.getEpochSecond());
    }

    /**
     * Get Retry-After header value (seconds)
     */
    public String getRetryAfterHeader() {
        return String.valueOf(retryAfterSeconds);
    }
}
