package com.healthdata.clinicalworkflow.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration
 *
 * Configures Resilience4j circuit breakers for fault tolerance when calling external services.
 * Prevents cascading failures by failing fast when downstream services are unavailable.
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * Create circuit breaker registry with default configuration
     *
     * Configuration:
     * - Failure rate threshold: 50% (opens circuit if >50% of calls fail)
     * - Wait duration in open state: 10 seconds
     * - Sliding window size: 10 calls
     * - Minimum number of calls: 5 (before calculating failure rate)
     * - Permitted calls in half-open state: 3
     *
     * @return configured circuit breaker registry
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config =
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build();

        return CircuitBreakerRegistry.of(config);
    }
}
