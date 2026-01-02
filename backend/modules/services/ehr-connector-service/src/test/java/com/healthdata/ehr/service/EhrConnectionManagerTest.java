package com.healthdata.ehr.service;

import com.healthdata.ehr.connector.EhrConnector;
import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.factory.EhrConnectorFactory;
import com.healthdata.ehr.model.EhrConnectionStatus;
import com.healthdata.ehr.model.EhrVendorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite for EhrConnectionManager.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EHR Connection Manager Tests")
class EhrConnectionManagerTest {

    @Mock
    private EhrConnectorFactory connectorFactory;

    @Mock
    private EhrConnector mockConnector;

    private EhrConnectionManager connectionManager;

    @BeforeEach
    void setUp() {
        connectionManager = new EhrConnectionManager(connectorFactory);
    }

    @Test
    @DisplayName("Should register new connection successfully")
    void shouldRegisterConnection() {
        // Given
        EhrConnectionConfig config = createTestConfig("conn-1", "tenant-1");
        when(connectorFactory.createConnector(any())).thenReturn(mockConnector);
        when(mockConnector.initialize(any())).thenReturn(Mono.empty());

        // When
        Mono<String> result = connectionManager.registerConnection(config);

        // Then
        StepVerifier.create(result)
                .assertNext(connectionId -> {
                    assertThat(connectionId).isNotNull();
                    assertThat(connectionId).isEqualTo("conn-1");
                })
                .verifyComplete();

        verify(connectorFactory).createConnector(config);
        verify(mockConnector).initialize(config);
    }

    @Test
    @DisplayName("Should get connection by ID")
    void shouldGetConnection() {
        // Given
        EhrConnectionConfig config = createTestConfig("conn-1", "tenant-1");
        when(connectorFactory.createConnector(any())).thenReturn(mockConnector);
        when(mockConnector.initialize(any())).thenReturn(Mono.empty());

        connectionManager.registerConnection(config).block();

        // When
        EhrConnector connector = connectionManager.getConnection("conn-1", "tenant-1");

        // Then
        assertThat(connector).isNotNull();
        assertThat(connector).isEqualTo(mockConnector);
    }

    @Test
    @DisplayName("Should return null for non-existent connection")
    void shouldReturnNullForNonExistentConnection() {
        // When
        EhrConnector connector = connectionManager.getConnection("non-existent", "tenant-1");

        // Then
        assertThat(connector).isNull();
    }

    @Test
    @DisplayName("Should get connection status")
    void shouldGetConnectionStatus() {
        // Given
        EhrConnectionConfig config = createTestConfig("conn-1", "tenant-1");
        EhrConnectionStatus status = EhrConnectionStatus.builder()
                .connectionId("conn-1")
                .status(EhrConnectionStatus.Status.CONNECTED)
                .build();

        when(connectorFactory.createConnector(any())).thenReturn(mockConnector);
        when(mockConnector.initialize(any())).thenReturn(Mono.empty());
        when(mockConnector.getConnectionStatus()).thenReturn(Mono.just(status));

        connectionManager.registerConnection(config).block();

        // When
        Mono<EhrConnectionStatus> result = connectionManager.getConnectionStatus("conn-1", "tenant-1");

        // Then
        StepVerifier.create(result)
                .assertNext(s -> {
                    assertThat(s.getConnectionId()).isEqualTo("conn-1");
                    assertThat(s.getStatus()).isEqualTo(EhrConnectionStatus.Status.CONNECTED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should remove connection")
    void shouldRemoveConnection() {
        // Given
        EhrConnectionConfig config = createTestConfig("conn-1", "tenant-1");
        when(connectorFactory.createConnector(any())).thenReturn(mockConnector);
        when(mockConnector.initialize(any())).thenReturn(Mono.empty());
        when(mockConnector.disconnect()).thenReturn(Mono.empty());

        connectionManager.registerConnection(config).block();

        // When
        Mono<Void> result = connectionManager.removeConnection("conn-1", "tenant-1");

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(mockConnector).disconnect();
        assertThat(connectionManager.getConnection("conn-1", "tenant-1")).isNull();
    }

    @Test
    @DisplayName("Should get all connections for tenant")
    void shouldGetAllConnectionsForTenant() {
        // Given
        EhrConnectionConfig config1 = createTestConfig("conn-1", "tenant-1");
        EhrConnectionConfig config2 = createTestConfig("conn-2", "tenant-1");
        EhrConnectionConfig config3 = createTestConfig("conn-3", "tenant-2");

        when(connectorFactory.createConnector(any())).thenReturn(mockConnector);
        when(mockConnector.initialize(any())).thenReturn(Mono.empty());

        connectionManager.registerConnection(config1).block();
        connectionManager.registerConnection(config2).block();
        connectionManager.registerConnection(config3).block();

        // When
        List<String> connectionIds = connectionManager.getConnectionsByTenant("tenant-1");

        // Then
        assertThat(connectionIds).hasSize(2);
        assertThat(connectionIds).contains("conn-1", "conn-2");
    }

    @Test
    @DisplayName("Should prevent duplicate connection IDs")
    void shouldPreventDuplicateConnectionIds() {
        // Given
        EhrConnectionConfig config = createTestConfig("conn-1", "tenant-1");
        when(connectorFactory.createConnector(any())).thenReturn(mockConnector);
        when(mockConnector.initialize(any())).thenReturn(Mono.empty());

        connectionManager.registerConnection(config).block();

        // When
        Mono<String> result = connectionManager.registerConnection(config);

        // Then
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should test connection health")
    void shouldTestConnectionHealth() {
        // Given
        EhrConnectionConfig config = createTestConfig("conn-1", "tenant-1");
        EhrConnectionStatus status = EhrConnectionStatus.builder()
                .connectionId("conn-1")
                .status(EhrConnectionStatus.Status.CONNECTED)
                .build();

        when(connectorFactory.createConnector(any())).thenReturn(mockConnector);
        when(mockConnector.initialize(any())).thenReturn(Mono.empty());
        when(mockConnector.testConnection()).thenReturn(Mono.just(status));

        connectionManager.registerConnection(config).block();

        // When
        Mono<EhrConnectionStatus> result = connectionManager.testConnection("conn-1", "tenant-1");

        // Then
        StepVerifier.create(result)
                .assertNext(s -> {
                    assertThat(s.getStatus()).isEqualTo(EhrConnectionStatus.Status.CONNECTED);
                })
                .verifyComplete();

        verify(mockConnector).testConnection();
    }

    private EhrConnectionConfig createTestConfig(String connectionId, String tenantId) {
        return EhrConnectionConfig.builder()
                .connectionId(connectionId)
                .tenantId(tenantId)
                .vendorType(EhrVendorType.EPIC)
                .baseUrl("https://fhir.epic.com")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();
    }
}
