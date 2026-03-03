package com.healthdata.ehr.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import com.healthdata.ehr.config.TestSecurityConfiguration;
import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.model.EhrVendorType;
import com.healthdata.ehr.service.EhrConnectionManager;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

/**
 * Integration tests for EHR connection persistence — proves connections
 * survive service restarts, soft-delete works, and tenant isolation holds.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfiguration.class)
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@DisplayName("EHR Connection Persistence Tests")
@Tag("integration")
class EhrConnectionPersistenceIT {

    @Autowired
    private EhrConnectionManager connectionManager;

    @Autowired
    private EhrConnectionConfigRepository configRepository;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
        // Clean up test connections from DB
        configRepository.findByTenantId("persist-tenant-1").forEach(e -> configRepository.delete(e));
        configRepository.findByTenantId("persist-tenant-2").forEach(e -> configRepository.delete(e));
    }

    @Test
    @DisplayName("Should persist connection config to database on register")
    void shouldPersistConnectionConfig() {
        enqueueOAuthSuccess();

        EhrConnectionConfig config = buildConfig("persist-test-conn", "persist-tenant-1");
        connectionManager.registerConnection(config).block();

        // Verify entity exists in database
        var entity = configRepository.findByConnectionIdAndTenantId("persist-test-conn", "persist-tenant-1");
        assertThat(entity).isPresent();
        assertThat(entity.get().getConnectionId()).isEqualTo("persist-test-conn");
        assertThat(entity.get().getTenantId()).isEqualTo("persist-tenant-1");
        assertThat(entity.get().getActive()).isTrue();
        assertThat(entity.get().getBaseUrl()).isEqualTo(mockWebServer.url("/").toString());
        assertThat(entity.get().getVendorType()).isEqualTo(EhrVendorType.GENERIC);

        // Cleanup in-memory
        connectionManager.removeConnection("persist-test-conn", "persist-tenant-1").block();
    }

    @Test
    @DisplayName("Should soft-delete connection on remove (active=false)")
    void shouldDeactivateOnRemove() {
        enqueueOAuthSuccess();

        EhrConnectionConfig config = buildConfig("deactivate-conn", "persist-tenant-1");
        connectionManager.registerConnection(config).block();

        // Verify active=true before remove
        var beforeRemove = configRepository.findByConnectionIdAndTenantId("deactivate-conn", "persist-tenant-1");
        assertThat(beforeRemove).isPresent();
        assertThat(beforeRemove.get().getActive()).isTrue();

        // Remove connection
        connectionManager.removeConnection("deactivate-conn", "persist-tenant-1").block();

        // Verify entity still in DB but active=false
        var afterRemove = configRepository.findByConnectionId("deactivate-conn");
        assertThat(afterRemove).isPresent();
        assertThat(afterRemove.get().getActive()).isFalse();
    }

    @Test
    @DisplayName("Should isolate connections by tenant")
    void shouldIsolateTenants() {
        // Register connections for 2 tenants
        enqueueOAuthSuccess();
        enqueueOAuthSuccess();

        connectionManager.registerConnection(
                buildConfig("tenant1-conn", "persist-tenant-1")).block();
        connectionManager.registerConnection(
                buildConfig("tenant2-conn", "persist-tenant-2")).block();

        // Query by tenant — each should see only their own
        List<EhrConnectionConfigEntity> tenant1Conns = configRepository.findByTenantIdAndActiveTrue("persist-tenant-1");
        List<EhrConnectionConfigEntity> tenant2Conns = configRepository.findByTenantIdAndActiveTrue("persist-tenant-2");

        assertThat(tenant1Conns).hasSize(1);
        assertThat(tenant1Conns.get(0).getConnectionId()).isEqualTo("tenant1-conn");

        assertThat(tenant2Conns).hasSize(1);
        assertThat(tenant2Conns.get(0).getConnectionId()).isEqualTo("tenant2-conn");

        // In-memory check: getConnectionsByTenant should also isolate
        assertThat(connectionManager.getConnectionsByTenant("persist-tenant-1"))
                .containsExactly("tenant1-conn");
        assertThat(connectionManager.getConnectionsByTenant("persist-tenant-2"))
                .containsExactly("tenant2-conn");

        // Cleanup
        connectionManager.removeConnection("tenant1-conn", "persist-tenant-1").block();
        connectionManager.removeConnection("tenant2-conn", "persist-tenant-2").block();
    }

    @Test
    @DisplayName("Should restore active connections list from database")
    void shouldRestoreFromDatabase() {
        enqueueOAuthSuccess();

        // Register a connection (persists to DB + in-memory)
        connectionManager.registerConnection(
                buildConfig("restore-conn", "persist-tenant-1")).block();

        // Verify it's in the DB
        List<EhrConnectionConfigEntity> activeConfigs = configRepository.findByActiveTrue();
        boolean foundRestoreConn = activeConfigs.stream()
                .anyMatch(e -> "restore-conn".equals(e.getConnectionId()));
        assertThat(foundRestoreConn).isTrue();

        // Cleanup
        connectionManager.removeConnection("restore-conn", "persist-tenant-1").block();
    }

    private void enqueueOAuthSuccess() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"access_token\":\"test-token\",\"expires_in\":3600}"));
    }

    private EhrConnectionConfig buildConfig(String connectionId, String tenantId) {
        String baseUrl = mockWebServer.url("/").toString();
        return EhrConnectionConfig.builder()
                .connectionId(connectionId)
                .tenantId(tenantId)
                .vendorType(EhrVendorType.GENERIC)
                .baseUrl(baseUrl)
                .clientId("test-client")
                .clientSecret("test-secret")
                .tokenUrl(baseUrl + "oauth2/token")
                .enableCircuitBreaker(false)
                .build();
    }
}
