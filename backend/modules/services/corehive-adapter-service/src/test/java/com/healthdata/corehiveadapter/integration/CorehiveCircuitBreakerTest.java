package com.healthdata.corehiveadapter.integration;

import com.healthdata.corehiveadapter.client.CorehiveApiClient;
import com.healthdata.corehiveadapter.model.CareGapScoringRequest;
import com.healthdata.corehiveadapter.model.CareGapScoringResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Validates circuit breaker behavior when the CoreHive external service
 * is unavailable. After enough failures, the circuit should open and
 * fail fast without attempting further calls.
 */
@Tag("unit")
@DisplayName("Circuit Breaker: CoreHive degradation")
class CorehiveCircuitBreakerTest {

    private CircuitBreakerRegistry registry;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        registry = CircuitBreakerRegistry.ofDefaults();
        circuitBreaker = registry.circuitBreaker("corehive-api");
    }

    @Test
    @DisplayName("Circuit breaker starts in CLOSED state")
    void circuitBreaker_startsInClosedState() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Circuit breaker opens after repeated failures")
    void circuitBreaker_opensAfterRepeatedFailures() {
        // Simulate failures to trip the circuit breaker
        for (int i = 0; i < 100; i++) {
            circuitBreaker.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS,
                    new ResourceAccessException("Connection refused"));
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Circuit breaker metrics track failure count")
    void circuitBreaker_tracksFailureMetrics() {
        circuitBreaker.onSuccess(50, java.util.concurrent.TimeUnit.MILLISECONDS);
        circuitBreaker.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS,
                new ResourceAccessException("Connection refused"));

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(1);
    }
}
