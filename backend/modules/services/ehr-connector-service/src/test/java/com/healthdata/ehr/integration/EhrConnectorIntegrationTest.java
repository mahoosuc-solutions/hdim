package com.healthdata.ehr.integration;

import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import com.healthdata.ehr.config.TestSecurityConfiguration;
import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.factory.EhrConnectorFactory;
import com.healthdata.ehr.model.EhrConnectionStatus;
import com.healthdata.ehr.model.EhrVendorType;
import com.healthdata.ehr.service.EhrConnectionManager;
import com.healthdata.ehr.service.EhrSyncService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.io.IOException;

/**
 * Integration tests for EHR Connector Service.
 * Tests the complete flow from connection registration to data retrieval.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfiguration.class)
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@DisplayName("EHR Connector Integration Tests")
@Tag("integration")
class EhrConnectorIntegrationTest {

    @Autowired
    private EhrConnectionManager connectionManager;

    @Autowired
    private EhrSyncService syncService;

    @Autowired
    private EhrConnectorFactory connectorFactory;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setupMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDownMockServer() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should register connection and retrieve status")
    void shouldRegisterAndGetStatus() {
        // Given - Mock OAuth2 token response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"access_token\":\"test-token\",\"expires_in\":3600}"));

        String baseUrl = mockWebServer.url("/").toString();
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .connectionId("integration-test-conn")
                .tenantId("integration-tenant")
                .vendorType(EhrVendorType.GENERIC)
                .baseUrl(baseUrl)
                .clientId("test-client")
                .clientSecret("test-secret")
                .tokenUrl(baseUrl + "oauth2/token")
                .enableCircuitBreaker(false) // Disable for test
                .build();

        // When - Register connection
        StepVerifier.create(connectionManager.registerConnection(config))
                .assertNext(connectionId -> {
                    Assertions.assertNotNull(connectionId);
                    Assertions.assertEquals("integration-test-conn", connectionId);
                })
                .verifyComplete();

        // Then - Get status
        StepVerifier.create(connectionManager.getConnectionStatus("integration-test-conn", "integration-tenant"))
                .assertNext(status -> {
                    Assertions.assertEquals(EhrConnectionStatus.Status.CONNECTED, status.getStatus());
                    Assertions.assertEquals(EhrVendorType.GENERIC, status.getVendorType());
                })
                .verifyComplete();

        // Cleanup
        connectionManager.removeConnection("integration-test-conn", "integration-tenant").block();
    }

    @Test
    @DisplayName("Should handle connection authentication failure")
    void shouldHandleAuthenticationFailure() {
        // Given - Mock failed OAuth2 response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":\"invalid_client\"}"));

        String baseUrl = mockWebServer.url("/").toString();
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .connectionId("auth-fail-conn")
                .tenantId("test-tenant")
                .vendorType(EhrVendorType.GENERIC)
                .baseUrl(baseUrl)
                .clientId("invalid-client")
                .clientSecret("invalid-secret")
                .tokenUrl(baseUrl + "oauth2/token")
                .enableCircuitBreaker(false)
                .build();

        // When/Then - Registration should fail
        StepVerifier.create(connectionManager.registerConnection(config))
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Should manage multiple connections for same tenant")
    void shouldManageMultipleConnections() {
        // Given - Two different connections
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"access_token\":\"token1\",\"expires_in\":3600}"));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"access_token\":\"token2\",\"expires_in\":3600}"));

        String baseUrl = mockWebServer.url("/").toString();

        EhrConnectionConfig config1 = createTestConfig("multi-conn-1", baseUrl);
        EhrConnectionConfig config2 = createTestConfig("multi-conn-2", baseUrl);

        // When - Register both
        connectionManager.registerConnection(config1).block();
        connectionManager.registerConnection(config2).block();

        // Then - Both should be listed for tenant
        var connections = connectionManager.getConnectionsByTenant("multi-tenant");
        Assertions.assertEquals(2, connections.size());
        Assertions.assertTrue(connections.contains("multi-conn-1"));
        Assertions.assertTrue(connections.contains("multi-conn-2"));

        // Cleanup
        connectionManager.removeConnection("multi-conn-1", "multi-tenant").block();
        connectionManager.removeConnection("multi-conn-2", "multi-tenant").block();
    }

    private EhrConnectionConfig createTestConfig(String connectionId, String baseUrl) {
        return EhrConnectionConfig.builder()
                .connectionId(connectionId)
                .tenantId("multi-tenant")
                .vendorType(EhrVendorType.GENERIC)
                .baseUrl(baseUrl)
                .clientId("test-client")
                .clientSecret("test-secret")
                .tokenUrl(baseUrl + "oauth2/token")
                .enableCircuitBreaker(false)
                .build();
    }
}
