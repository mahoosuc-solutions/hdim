package com.healthdata.ehr.connector;

import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite for AbstractEhrConnector base functionality.
 * Tests common authentication, connection management, and resilience patterns.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Abstract EHR Connector Tests")
class AbstractEhrConnectorTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    private TestEhrConnector connector;
    private EhrConnectionConfig config;

    @BeforeEach
    void setUp() {
        config = EhrConnectionConfig.builder()
                .connectionId("test-conn-1")
                .tenantId("tenant-1")
                .vendorType(EhrVendorType.EPIC)
                .baseUrl("https://test.fhir.server")
                .clientId("test-client")
                .clientSecret("test-secret")
                .enableCircuitBreaker(false) // Disable for simpler testing
                .maxRetries(3)
                .build();

        // Mock WebClient.Builder chain
        when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(any(), any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        connector = new TestEhrConnector(config, webClientBuilder);
    }

    @Test
    @DisplayName("Should initialize connection successfully")
    void shouldInitializeConnection() {
        // When
        Mono<Void> result = connector.initialize(config);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(connector.getConnectionStatus().block().getStatus())
                .isEqualTo(EhrConnectionStatus.Status.CONNECTED);
    }

    @Test
    @DisplayName("Should handle authentication failure during initialization")
    void shouldHandleAuthenticationFailure() {
        // Given
        TestEhrConnector failingConnector = new TestEhrConnector(config, webClientBuilder, true);

        // When
        Mono<Void> result = failingConnector.initialize(config);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        assertThat(failingConnector.getConnectionStatus().block().getStatus())
                .isEqualTo(EhrConnectionStatus.Status.AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("Should test connection and return status")
    void shouldTestConnection() {
        // When
        Mono<EhrConnectionStatus> result = connector.testConnection();

        // Then
        StepVerifier.create(result)
                .assertNext(status -> {
                    assertThat(status.getConnectionId()).isEqualTo("test-conn-1");
                    assertThat(status.getTenantId()).isEqualTo("tenant-1");
                    assertThat(status.getStatus()).isEqualTo(EhrConnectionStatus.Status.CONNECTED);
                    assertThat(status.getConsecutiveFailures()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle test connection failure")
    void shouldHandleTestConnectionFailure() {
        // Given
        TestEhrConnector failingConnector = new TestEhrConnector(config, webClientBuilder, true);

        // When
        Mono<EhrConnectionStatus> result = failingConnector.testConnection();

        // Then
        StepVerifier.create(result)
                .assertNext(status -> {
                    assertThat(status.getStatus()).isEqualTo(EhrConnectionStatus.Status.ERROR);
                    assertThat(status.getErrorMessage()).contains("Authentication failed");
                    assertThat(status.getConsecutiveFailures()).isGreaterThan(0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should disconnect and clear token")
    void shouldDisconnect() {
        // Given
        connector.initialize(config).block();

        // When
        Mono<Void> result = connector.disconnect();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(connector.getConnectionStatus().block().getStatus())
                .isEqualTo(EhrConnectionStatus.Status.DISCONNECTED);
        assertThat(connector.accessToken).isNull();
        assertThat(connector.tokenExpiryTime).isNull();
    }

    @Test
    @DisplayName("Should return connection status")
    void shouldGetConnectionStatus() {
        // When
        Mono<EhrConnectionStatus> result = connector.getConnectionStatus();

        // Then
        StepVerifier.create(result)
                .assertNext(status -> {
                    assertThat(status.getConnectionId()).isEqualTo("test-conn-1");
                    assertThat(status.getTenantId()).isEqualTo("tenant-1");
                    assertThat(status.getVendorType()).isEqualTo(EhrVendorType.EPIC);
                    assertThat(status.getCircuitBreakerState()).isEqualTo(EhrConnectionStatus.CircuitBreakerState.CLOSED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return configuration")
    void shouldGetConfig() {
        // When
        EhrConnectionConfig result = connector.getConfig();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConnectionId()).isEqualTo("test-conn-1");
        assertThat(result.getTenantId()).isEqualTo("tenant-1");
        assertThat(result.getVendorType()).isEqualTo(EhrVendorType.EPIC);
    }

    @Test
    @DisplayName("Should validate token correctly")
    void shouldValidateToken() {
        // Given
        connector.initialize(config).block();

        // When & Then
        assertThat(connector.isTokenValid()).isTrue();
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        // Given
        connector.accessToken = "expired-token";
        connector.tokenExpiryTime = LocalDateTime.now().minusHours(1);

        // When & Then
        assertThat(connector.isTokenValid()).isFalse();
    }

    @Test
    @DisplayName("Should get valid access token")
    void shouldGetValidAccessToken() {
        // When
        Mono<String> result = connector.getValidAccessToken();

        // Then
        StepVerifier.create(result)
                .assertNext(token -> {
                    assertThat(token).isNotNull();
                    assertThat(token).isEqualTo("test-access-token");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should refresh token if expired")
    void shouldRefreshTokenIfExpired() {
        // Given
        connector.accessToken = "old-token";
        connector.tokenExpiryTime = LocalDateTime.now().minusHours(1);

        // When
        Mono<String> result = connector.getValidAccessToken();

        // Then
        StepVerifier.create(result)
                .assertNext(token -> {
                    assertThat(token).isEqualTo("test-access-token");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should apply resilience patterns to Mono")
    void shouldApplyResilience() {
        // Given
        Mono<String> testMono = Mono.just("test-value");

        // When
        Mono<String> result = connector.applyResilience(testMono);

        // Then
        StepVerifier.create(result)
                .expectNext("test-value")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should retrieve patient data")
    void shouldGetPatient() {
        // When
        Mono<EhrPatient> result = connector.getPatient("patient-1", "tenant-1");

        // Then
        StepVerifier.create(result)
                .assertNext(patient -> {
                    assertThat(patient.getEhrPatientId()).isEqualTo("patient-1");
                    assertThat(patient.getTenantId()).isEqualTo("tenant-1");
                    assertThat(patient.getFamilyName()).isEqualTo("Test");
                    assertThat(patient.getGivenName()).isEqualTo("Patient");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should sync patient data and return results")
    void shouldSyncPatientData() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        Mono<EhrConnector.SyncResult> result = connector.syncPatientData(
                "patient-1", startDate, endDate, "tenant-1");

        // Then
        StepVerifier.create(result)
                .assertNext(syncResult -> {
                    assertThat(syncResult.patientId()).isEqualTo("patient-1");
                    assertThat(syncResult.encountersRetrieved()).isEqualTo(5);
                    assertThat(syncResult.observationsRetrieved()).isEqualTo(20);
                    assertThat(syncResult.success()).isTrue();
                    assertThat(syncResult.errorMessage()).isNull();
                    assertThat(syncResult.syncStartTime()).isNotNull();
                    assertThat(syncResult.syncEndTime()).isNotNull();
                })
                .verifyComplete();
    }

    /**
     * Test implementation of AbstractEhrConnector for testing purposes.
     * Provides mock implementations of all abstract methods.
     */
    static class TestEhrConnector extends AbstractEhrConnector {

        private final boolean shouldFailAuth;

        public TestEhrConnector(EhrConnectionConfig config, WebClient.Builder webClientBuilder) {
            this(config, webClientBuilder, false);
        }

        public TestEhrConnector(EhrConnectionConfig config, WebClient.Builder webClientBuilder,
                                boolean shouldFailAuth) {
            super(config, webClientBuilder);
            this.shouldFailAuth = shouldFailAuth;
        }

        @Override
        protected Mono<String> authenticate() {
            if (shouldFailAuth) {
                return Mono.error(new RuntimeException("Authentication failed"));
            }

            // Simulate successful authentication
            this.accessToken = "test-access-token";
            this.tokenExpiryTime = LocalDateTime.now().plusHours(1);
            return Mono.just(accessToken);
        }

        @Override
        public Mono<EhrPatient> getPatient(String ehrPatientId, String tenantId) {
            return getValidAccessToken()
                    .map(token -> EhrPatient.builder()
                            .ehrPatientId(ehrPatientId)
                            .tenantId(tenantId)
                            .sourceVendor(config.getVendorType())
                            .familyName("Test")
                            .givenName("Patient")
                            .dateOfBirth(java.time.LocalDate.of(1980, 1, 1))
                            .gender("male")
                            .active(true)
                            .deceased(false)
                            .build());
        }

        @Override
        public Flux<EhrPatient> searchPatients(String familyName, String givenName,
                                                LocalDateTime dateOfBirth, String tenantId) {
            return getValidAccessToken()
                    .flatMapMany(token -> Flux.just(
                            EhrPatient.builder()
                                    .ehrPatientId("patient-1")
                                    .tenantId(tenantId)
                                    .familyName(familyName)
                                    .givenName(givenName)
                                    .build()
                    ));
        }

        @Override
        public Mono<EhrEncounter> getEncounter(String ehrEncounterId, String tenantId) {
            return getValidAccessToken()
                    .map(token -> EhrEncounter.builder()
                            .ehrEncounterId(ehrEncounterId)
                            .tenantId(tenantId)
                            .sourceVendor(config.getVendorType())
                            .ehrPatientId("patient-1")
                            .status("finished")
                            .encounterClass("ambulatory")
                            .build());
        }

        @Override
        public Flux<EhrEncounter> getEncounters(String ehrPatientId, LocalDateTime startDate,
                                                 LocalDateTime endDate, String tenantId) {
            return getValidAccessToken()
                    .flatMapMany(token -> Flux.range(1, 5)
                            .map(i -> EhrEncounter.builder()
                                    .ehrEncounterId("encounter-" + i)
                                    .tenantId(tenantId)
                                    .ehrPatientId(ehrPatientId)
                                    .status("finished")
                                    .build()));
        }

        @Override
        public Flux<EhrObservation> getObservations(String ehrPatientId, String category,
                                                     LocalDateTime startDate, LocalDateTime endDate,
                                                     String tenantId) {
            return getValidAccessToken()
                    .flatMapMany(token -> Flux.range(1, 20)
                            .map(i -> EhrObservation.builder()
                                    .ehrObservationId("obs-" + i)
                                    .tenantId(tenantId)
                                    .ehrPatientId(ehrPatientId)
                                    .status("final")
                                    .build()));
        }

        @Override
        public Flux<EhrObservation> getObservationsByEncounter(String ehrEncounterId, String tenantId) {
            return getValidAccessToken()
                    .flatMapMany(token -> Flux.just(
                            EhrObservation.builder()
                                    .ehrObservationId("obs-1")
                                    .tenantId(tenantId)
                                    .ehrEncounterId(ehrEncounterId)
                                    .status("final")
                                    .build()
                    ));
        }

        @Override
        public Mono<SyncResult> syncPatientData(String ehrPatientId, LocalDateTime startDate,
                                                 LocalDateTime endDate, String tenantId) {
            LocalDateTime syncStart = LocalDateTime.now();

            return Mono.zip(
                    getEncounters(ehrPatientId, startDate, endDate, tenantId).collectList(),
                    getObservations(ehrPatientId, null, startDate, endDate, tenantId).collectList()
            ).map(tuple -> new SyncResult(
                    ehrPatientId,
                    tuple.getT1().size(),
                    tuple.getT2().size(),
                    syncStart,
                    LocalDateTime.now(),
                    true,
                    null
            )).onErrorResume(error -> Mono.just(new SyncResult(
                    ehrPatientId,
                    0,
                    0,
                    syncStart,
                    LocalDateTime.now(),
                    false,
                    error.getMessage()
            )));
        }
    }
}
