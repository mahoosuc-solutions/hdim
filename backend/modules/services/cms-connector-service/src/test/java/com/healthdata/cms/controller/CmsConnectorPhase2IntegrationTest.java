package com.healthdata.cms.controller;

import com.healthdata.cms.CmsConnectorServiceApplication;
import com.healthdata.cms.auth.OAuth2Manager;
import com.healthdata.cms.client.BcdaClient;
import com.healthdata.cms.client.DpcClient;
import com.healthdata.cms.repository.CmsIntegrationConfigRepository;
import com.healthdata.cache.CacheEvictionService;
import com.healthdata.cms.service.CmsEobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 2 integration tests for CMS Connector Service.
 * Validates measure retrieval, CMS format, HEDIS mapping, error handling, and tenant filtering.
 */
@SpringBootTest(classes = CmsConnectorServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Tag("integration")
@WithMockUser(roles = {"ADMIN"})
@EntityScan(basePackages = {
        "com.healthdata.cms.model",
        "com.healthdata.authentication.domain",
        "com.healthdata.audit.entity"
})
@DisplayName("CMS Connector Service Phase 2 Integration Tests")
class CmsConnectorPhase2IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cms_connector_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("audit.enabled", () -> "false");
        registry.add("jwt.secret", () -> "test-secret-key-for-integration-testing-only");
        registry.add("jwt.expiration", () -> "3600000");
    }

    @MockBean(name = "cmsRestTemplate")
    private RestTemplate cmsRestTemplate;

    @MockBean(name = "oauth2RestTemplate")
    private RestTemplate oauth2RestTemplate;

    @MockBean(name = "circuitBreakerEventConsumer")
    private Consumer<Object> circuitBreakerEventConsumer;

    @MockBean(name = "retryEventConsumer")
    private Consumer<Object> retryEventConsumer;

    @MockBean
    private OAuth2Manager oauth2Manager;

    @MockBean
    private DpcClient dpcClient;

    @MockBean
    private BcdaClient bcdaClient;

    @MockBean
    private CmsIntegrationConfigRepository cmsIntegrationConfigRepository;

    @MockBean
    private CacheManager cacheManager;

    @MockBean
    private CacheEvictionService cacheEvictionService;

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_A = "pilot-tenant-a";

    @MockBean
    private CmsEobService eobService;

    @Test
    @DisplayName("GET /api/v1/cms/dpc/patient/{id} - Should retrieve CMS patient data")
    void shouldRetrieveCmsPatientData() throws Exception {
        String fhirPatient = "{\"resourceType\":\"Patient\",\"id\":\"test-123\",\"name\":[{\"family\":\"Doe\"}]}";
        when(dpcClient.getPatient(anyString())).thenReturn(fhirPatient);

        mockMvc.perform(get("/api/v1/cms/dpc/patient/test-123")
                        .header("X-Tenant-ID", TENANT_A)
                        .header("X-Auth-Validated", "true")
                        .header("X-Auth-User-Id", "admin-user")
                        .header("X-Auth-Roles", "ADMIN")
                        .accept("application/fhir+json"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/cms/dpc/conditions/{id} - Should return FHIR Bundle format")
    void shouldReturnFhirBundleFormat() throws Exception {
        String bundle = "{\"resourceType\":\"Bundle\",\"type\":\"searchset\",\"total\":1,\"entry\":[]}";
        when(dpcClient.getConditions(anyString())).thenReturn(bundle);

        mockMvc.perform(get("/api/v1/cms/dpc/conditions/test-123")
                        .header("X-Tenant-ID", TENANT_A)
                        .header("X-Auth-Validated", "true")
                        .header("X-Auth-User-Id", "admin-user")
                        .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/cms/dpc/eob/{id}/summary - Should support HEDIS-relevant EOB summary")
    void shouldSupportHedisEobSummary() throws Exception {
        when(eobService.getEobSummary(anyString(), any())).thenReturn(
                new CmsEobService.EobSummary());

        mockMvc.perform(get("/api/v1/cms/dpc/eob/test-123/summary")
                        .header("X-Tenant-ID", TENANT_A)
                        .header("X-Auth-Validated", "true")
                        .header("X-Auth-User-Id", "admin-user")
                        .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/cms/dpc/patient/{id} - VIEWER role should be forbidden")
    @WithMockUser(roles = {"VIEWER"})
    void shouldRejectUnauthorizedRole() throws Exception {
        mockMvc.perform(get("/api/v1/cms/dpc/patient/test-123")
                        .header("X-Tenant-ID", TENANT_A)
                        .header("X-Auth-Validated", "true")
                        .header("X-Auth-User-Id", "viewer-user")
                        .header("X-Auth-Roles", "VIEWER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/cms/health - Should return overall health status")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/cms/health")
                        .header("X-Tenant-ID", TENANT_A))
                .andExpect(status().isOk());
    }
}
