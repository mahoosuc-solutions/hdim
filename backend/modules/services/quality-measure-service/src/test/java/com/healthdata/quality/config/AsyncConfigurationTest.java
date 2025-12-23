package com.healthdata.quality.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@DisplayName("Async Configuration Tests")
class AsyncConfigurationTest {

    @Test
    @DisplayName("Should configure async executor with safe defaults")
    void shouldConfigureAsyncExecutor() {
        AsyncConfiguration config = new AsyncConfiguration();

        Executor executor = config.getAsyncExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isGreaterThanOrEqualTo(4);
        assertThat(taskExecutor.getMaxPoolSize()).isGreaterThanOrEqualTo(8);
        assertThat(taskExecutor.getQueueCapacity()).isEqualTo(1000);
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("population-calc-");
        assertThat(taskExecutor.getThreadPoolExecutor())
            .extracting(ThreadPoolExecutor::getRejectedExecutionHandler)
            .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
    }

    @Test
    @DisplayName("Should configure batch executor")
    void shouldConfigureBatchExecutor() {
        AsyncConfiguration config = new AsyncConfiguration();

        Executor executor = config.batchExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isGreaterThanOrEqualTo(2);
        assertThat(taskExecutor.getMaxPoolSize()).isGreaterThanOrEqualTo(4);
        assertThat(taskExecutor.getQueueCapacity()).isEqualTo(500);
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("batch-chunk-");
    }

    @Test
    @DisplayName("Should configure circuit breaker and rate limiter")
    void shouldConfigureCircuitBreakerAndRateLimiter() {
        AsyncConfiguration config = new AsyncConfiguration();

        CircuitBreaker circuitBreaker = config.measureCalculationCircuitBreaker();
        RateLimiter rateLimiter = config.measureCalculationRateLimiter();

        assertThat(circuitBreaker.getName()).isEqualTo("measureCalculation");
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(rateLimiter.getName()).isEqualTo("measureCalculation");
        assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should publish circuit breaker and rate limiter events")
    void shouldPublishResilienceEvents() {
        AsyncConfiguration config = new AsyncConfiguration();

        CircuitBreaker circuitBreaker = config.measureCalculationCircuitBreaker();
        RateLimiter rateLimiter = config.measureCalculationRateLimiter();

        for (int i = 0; i < 10; i++) {
            if (i < 6) {
                circuitBreaker.onError(0, TimeUnit.MILLISECONDS, new RuntimeException("boom"));
            } else {
                circuitBreaker.onSuccess(0, TimeUnit.MILLISECONDS);
            }
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();

        rateLimiter.onSuccess();
        rateLimiter.onError(new RuntimeException("rate-limit"));
    }
}
