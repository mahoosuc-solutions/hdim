package com.healthdata.ehr.connector;

import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.model.EhrConnectionStatus;
import com.healthdata.ehr.model.EhrVendorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for AbstractEhrConnector base functionality.
 */
@DisplayName("Abstract EHR Connector Tests")
class AbstractEhrConnectorTest {

    private TestEhrConnector connector;
    private EhrConnectionConfig config;

    @BeforeEach
    void setUp() {
        config = EhrConnectionConfig.builder()
                .connectionId("test-conn")
                .tenantId("test-tenant")
                .vendorType(EhrVendorType.GENERIC)
                .baseUrl("https://test.example.com")
                .clientId("test-client")
                .clientSecret("test-secret")
                .enableCircuitBreaker(false) // Disable for unit tests
                .maxRetries(2)
                .build();

        connector = new TestEhrConnector(config, WebClient.builder());
    }

    @Test
    @DisplayName("Should initialize with disconnected status")
    void shouldInitializeWithDisconnectedStatus() {
        // When
        Mono<EhrConnectionStatus> statusMono = connector.getConnectionStatus();

        // Then
        StepVerifier.create(statusMono)
                .assertNext(status -> {
                    assertThat(status.getStatus()).isEqualTo(EhrConnectionStatus.Status.DISCONNECTED);
                    assertThat(status.getConnectionId()).isEqualTo("test-conn");
                    assertThat(status.getTenantId()).isEqualTo("test-tenant");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return config")
    void shouldReturnConfig() {
        // When
        EhrConnectionConfig returnedConfig = connector.getConfig();

        // Then
        assertThat(returnedConfig).isEqualTo(config);
        assertThat(returnedConfig.getConnectionId()).isEqualTo("test-conn");
    }

    @Test
    @DisplayName("Should update status on successful initialization")
    void shouldUpdateStatusOnSuccessfulInit() {
        // When
        Mono<Void> initMono = connector.initialize(config);

        // Then
        StepVerifier.create(initMono)
                .verifyComplete();

        StepVerifier.create(connector.getConnectionStatus())
                .assertNext(status -> {
                    assertThat(status.getStatus()).isEqualTo(EhrConnectionStatus.Status.CONNECTED);
                    assertThat(status.getLastSuccessfulConnection()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should disconnect and update status")
    void shouldDisconnectAndUpdateStatus() {
        // Given
        connector.initialize(config).block();

        // When
        Mono<Void> disconnectMono = connector.disconnect();

        // Then
        StepVerifier.create(disconnectMono)
                .verifyComplete();

        StepVerifier.create(connector.getConnectionStatus())
                .assertNext(status -> {
                    assertThat(status.getStatus()).isEqualTo(EhrConnectionStatus.Status.DISCONNECTED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should apply resilience patterns when enabled")
    void shouldApplyResiliencePatterns() {
        // Given - Config with circuit breaker enabled
        EhrConnectionConfig configWithCB = EhrConnectionConfig.builder()
                .connectionId("test-conn-cb")
                .tenantId("test-tenant")
                .vendorType(EhrVendorType.GENERIC)
                .baseUrl("https://test.example.com")
                .clientId("test-client")
                .clientSecret("test-secret")
                .enableCircuitBreaker(true)
                .maxRetries(3)
                .build();

        TestEhrConnector connectorWithCB = new TestEhrConnector(configWithCB, WebClient.builder());

        // When
        Mono<String> result = connectorWithCB.applyResilience(Mono.just("test"));

        // Then
        StepVerifier.create(result)
                .expectNext("test")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle token expiry")
    void shouldHandleTokenExpiry() {
        // Given
        connector.initialize(config).block();
        connector.setTokenExpired(); // Simulate token expiry

        // When
        Mono<String> tokenMono = connector.getValidAccessToken();

        // Then - Should re-authenticate
        StepVerifier.create(tokenMono)
                .assertNext(token -> {
                    assertThat(token).isEqualTo("test-access-token");
                })
                .verifyComplete();
    }

    /**
     * Test implementation of AbstractEhrConnector.
     */
    static class TestEhrConnector extends AbstractEhrConnector {

        TestEhrConnector(EhrConnectionConfig config, WebClient.Builder webClientBuilder) {
            super(config, webClientBuilder);
        }

        @Override
        protected Mono<String> authenticate() {
            // Simulate successful authentication
            return Mono.fromRunnable(() -> {
                        this.accessToken = "test-access-token";
                        this.tokenExpiryTime = java.time.LocalDateTime.now().plusHours(1);
                    })
                    .thenReturn("test-access-token");
        }

        // Test helper to simulate token expiry
        void setTokenExpired() {
            this.tokenExpiryTime = java.time.LocalDateTime.now().minusHours(1);
        }
    }
}
