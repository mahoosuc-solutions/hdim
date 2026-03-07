package com.healthdata.healthixadapter.integration;

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
@DisplayName("Circuit Breaker: Healthix degradation")
class HealthixCircuitBreakerTest {

    private CircuitBreakerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = CircuitBreakerRegistry.ofDefaults();
    }

    @Test
    @DisplayName("All 3 healthix circuit breakers start in CLOSED state")
    void allCircuitBreakers_startClosed() {
        assertThat(registry.circuitBreaker("healthix-fhir").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(registry.circuitBreaker("healthix-mpi").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(registry.circuitBreaker("healthix-documents").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("healthix-fhir circuit breaker opens after repeated failures")
    void fhirCircuitBreaker_opensAfterFailures() {
        CircuitBreaker cb = registry.circuitBreaker("healthix-fhir");

        for (int i = 0; i < 100; i++) {
            cb.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("FHIR unreachable"));
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Circuit breakers for different services are independent")
    void circuitBreakers_areIndependent() {
        CircuitBreaker fhirCb = registry.circuitBreaker("healthix-fhir");
        CircuitBreaker mpiCb = registry.circuitBreaker("healthix-mpi");

        for (int i = 0; i < 100; i++) {
            fhirCb.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("FHIR down"));
        }

        assertThat(fhirCb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(mpiCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
