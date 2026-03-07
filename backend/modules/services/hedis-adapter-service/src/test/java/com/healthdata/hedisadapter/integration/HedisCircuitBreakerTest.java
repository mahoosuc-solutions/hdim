package com.healthdata.hedisadapter.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("Circuit Breaker: hedis degradation")
class HedisCircuitBreakerTest {

    private CircuitBreakerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = CircuitBreakerRegistry.ofDefaults();
    }

    @Test
    @DisplayName("All 3 hedis circuit breakers start in CLOSED state")
    void allCircuitBreakers_startClosed() {
        assertThat(registry.circuitBreaker("hedis-measures").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(registry.circuitBreaker("hedis-cql").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(registry.circuitBreaker("hedis-crm").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("hedis-measures circuit breaker opens after repeated failures")
    void measuresCircuitBreaker_opensAfterFailures() {
        CircuitBreaker cb = registry.circuitBreaker("hedis-measures");

        for (int i = 0; i < 100; i++) {
            cb.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("hedis unreachable"));
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Circuit breakers for measures, cql, crm are independent")
    void circuitBreakers_areIndependent() {
        CircuitBreaker measuresCb = registry.circuitBreaker("hedis-measures");
        CircuitBreaker cqlCb = registry.circuitBreaker("hedis-cql");
        CircuitBreaker crmCb = registry.circuitBreaker("hedis-crm");

        for (int i = 0; i < 100; i++) {
            measuresCb.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("down"));
        }

        assertThat(measuresCb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(cqlCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(crmCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Circuit breaker metrics track failures correctly")
    void circuitBreaker_tracksMetrics() {
        CircuitBreaker cb = registry.circuitBreaker("hedis-crm");

        cb.onSuccess(50, TimeUnit.MILLISECONDS);
        cb.onSuccess(30, TimeUnit.MILLISECONDS);
        cb.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("fail"));

        CircuitBreaker.Metrics metrics = cb.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThanOrEqualTo(1);
    }
}
