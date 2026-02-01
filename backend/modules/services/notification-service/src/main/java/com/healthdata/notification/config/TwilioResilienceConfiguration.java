package com.healthdata.notification.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Configuration for Twilio SMS Provider
 *
 * Circuit Breaker:
 * - Opens after 5 failures in 60 seconds
 * - Half-open state after 30 seconds
 * - Minimum 10 calls required before evaluation
 *
 * Retry:
 * - Max 3 attempts
 * - Exponential backoff: 1s, 2s, 4s
 * - Retries only on transient errors
 */
@Configuration
public class TwilioResilienceConfiguration {

    @Bean
    public CircuitBreakerConfig twilioSmsCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .slowCallRateThreshold(50) // Slow calls threshold
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before half-open
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // Call is slow if > 5s
                .permittedNumberOfCallsInHalfOpenState(5) // 5 calls allowed in half-open
                .minimumNumberOfCalls(10) // Minimum calls before evaluation
                .slidingWindowSize(10) // Last 10 calls for failure rate
                .build();
    }

    @Bean
    public RetryConfig twilioSmsRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3) // Max 3 attempts (1 initial + 2 retries)
                .waitDuration(Duration.ofSeconds(1)) // Initial wait 1s
                .intervalFunction(attempt -> Duration.ofSeconds(1L << (attempt - 1)).toMillis()) // Exponential backoff: 1s, 2s, 4s
                .retryOnException(throwable -> {
                    // Retry on transient errors
                    String message = throwable.getMessage();
                    if (message == null) {
                        return false;
                    }

                    // Retry on rate limit (429)
                    if (message.contains("429") || message.contains("rate limit")) {
                        return true;
                    }

                    // Retry on network errors
                    if (message.contains("timeout") || message.contains("connection")) {
                        return true;
                    }

                    // Retry on server errors (500, 502, 503, 504)
                    if (message.contains("500") || message.contains("502") ||
                        message.contains("503") || message.contains("504")) {
                        return true;
                    }

                    // Don't retry on client errors (400, 401, 403, 404)
                    return false;
                })
                .build();
    }
}
