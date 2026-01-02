package com.healthdata.quality.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async Configuration for Parallel Processing
 *
 * Configures optimal thread pool for population calculation:
 * - Core pool size: Max(10, CPU cores × 2)
 * - Max pool size: CPU cores × 4
 * - Queue capacity: 1000
 * - Thread naming: population-calc-
 *
 * Also configures resilience patterns:
 * - Circuit breaker for downstream service protection
 * - Rate limiter to prevent overwhelming downstream services
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Configure thread pool size based on CPU cores with safe defaults
        int availableCores = Runtime.getRuntime().availableProcessors();
        // Ensure minimum values to prevent invalid ThreadPoolExecutor configuration
        int corePoolSize = Math.max(4, availableCores > 0 ? availableCores * 2 : 4);
        int maxPoolSize = Math.max(8, availableCores > 0 ? availableCores * 4 : 8);

        log.info("Configuring async executor: availableCores={}, corePoolSize={}, maxPoolSize={}",
            availableCores, corePoolSize, maxPoolSize);

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("population-calc-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Reject policy: Caller runs (applies backpressure)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("Initialized async executor with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
            corePoolSize, maxPoolSize, 1000);

        return executor;
    }

    /**
     * Circuit Breaker for Measure Calculation Service
     *
     * Configuration:
     * - Failure rate threshold: 50% (if 50% of calls fail, open circuit)
     * - Wait duration in open state: 30 seconds
     * - Sliding window size: 100 calls
     * - Minimum number of calls: 10 (need at least 10 calls before evaluating)
     */
    @Bean
    public CircuitBreaker measureCalculationCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // 50% failure rate threshold
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(100)
            .minimumNumberOfCalls(10)
            .permittedNumberOfCallsInHalfOpenState(5)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker("measureCalculation");

        // Log circuit breaker events
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.warn("Circuit breaker state transition: {}", event))
            .onFailureRateExceeded(event -> log.error("Circuit breaker failure rate exceeded: {}", event))
            .onCallNotPermitted(event -> log.warn("Circuit breaker call not permitted: {}", event));

        return circuitBreaker;
    }

    /**
     * Rate Limiter for Measure Calculation
     *
     * Limits calls to 100 per second to prevent overwhelming downstream services
     */
    @Bean
    public RateLimiter measureCalculationRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(100) // 100 calls
            .limitRefreshPeriod(Duration.ofSeconds(1)) // per second
            .timeoutDuration(Duration.ofSeconds(5)) // wait up to 5s for permission
            .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        RateLimiter rateLimiter = registry.rateLimiter("measureCalculation");

        // Log rate limiter events
        rateLimiter.getEventPublisher()
            .onSuccess(event -> log.debug("Rate limiter permitted call"))
            .onFailure(event -> log.warn("Rate limiter rejected call: {}", event));

        return rateLimiter;
    }

    /**
     * Thread pool for batch processing
     *
     * Separate executor for batch chunks to allow fine-grained control
     */
    @Bean(name = "batchExecutor")
    public Executor batchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        int availableCores = Runtime.getRuntime().availableProcessors();
        // Ensure minimum values to prevent invalid ThreadPoolExecutor configuration
        int corePoolSize = Math.max(2, availableCores > 0 ? availableCores : 2);
        int maxPoolSize = Math.max(4, availableCores > 0 ? availableCores * 2 : 4);

        log.info("Configuring batch executor: availableCores={}, corePoolSize={}, maxPoolSize={}",
            availableCores, corePoolSize, maxPoolSize);

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("batch-chunk-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("Initialized batch executor with corePoolSize={}, maxPoolSize={}",
            corePoolSize, maxPoolSize);

        return executor;
    }
}
