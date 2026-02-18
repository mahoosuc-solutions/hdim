package com.healthdata.cms.controller;

import com.healthdata.cache.CacheEvictionService;
import com.healthdata.cms.auth.OAuth2Manager;
import com.healthdata.cms.client.BcdaClient;
import com.healthdata.cms.client.DpcClient;
import com.healthdata.cms.repository.CmsIntegrationConfigRepository;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for CmsConnectorController.
 *
 * Tests the full API stack for DPC and BCDA endpoints with mocked external CMS clients.
 * External HTTP clients (DpcClient, BcdaClient) are mocked to prevent real CMS API calls.
 *
 * Uses Testcontainers PostgreSQL for a real database with Liquibase migrations applied.
 *
 * The @EntityScan override is required because AuthenticationAutoConfiguration restricts
 * entity scanning to com.healthdata.authentication.domain only. We must broaden it to
 * include the CMS service entities and shared audit entities.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@WithMockUser(roles = {"ADMIN"})
// Override the restricted @EntityScan from AuthenticationAutoConfiguration, which otherwise
// limits entity scanning to com.healthdata.authentication.domain only.
@EntityScan(basePackages = {
    "com.healthdata.cms.model",
    "com.healthdata.authentication.domain",
    "com.healthdata.audit.entity"
})
@DisplayName("CmsConnectorController Integration Tests")
class CmsConnectorControllerIntegrationTest {

    // Mock RestTemplate beans by name to prevent HttpClient5 API conflicts in test classpath
    @MockBean(name = "cmsRestTemplate")
    private RestTemplate cmsRestTemplate;

    @MockBean(name = "oauth2RestTemplate")
    private RestTemplate oauth2RestTemplate;

    // Mock Resilience4j event consumer beans to break circular dependency in RestTemplateConfig
    @MockBean(name = "circuitBreakerEventConsumer")
    @SuppressWarnings("rawtypes")
    private RegistryEventConsumer circuitBreakerEventConsumer;

    @MockBean(name = "retryEventConsumer")
    @SuppressWarnings("rawtypes")
    private RegistryEventConsumer retryEventConsumer;

    // Mock external CMS HTTP clients -- these call real CMS APIs and must not fire in tests
    @MockBean
    private OAuth2Manager oAuth2Manager;

    @MockBean
    private DpcClient dpcClient;

    @MockBean
    private BcdaClient bcdaClient;

    // Mock JPQL-incompatible repository (uses TIMESTAMPDIFF syntax not supported by PostgreSQL)
    @MockBean
    private CmsIntegrationConfigRepository cmsIntegrationConfigRepository;

    // Mock shared cache beans not auto-configured under test profile (caffeine, not redis)
    @MockBean
    private CacheManager cacheManager;

    @MockBean
    private CacheEvictionService cacheEvictionService;

    @Autowired
    private MockMvc mockMvc;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cms_connector_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.xml");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        // Disable audit module entity scanning restriction
        registry.add("audit.enabled", () -> "false");
        // JWT stub config required by AuthenticationAutoConfiguration
        registry.add("jwt.secret", () -> "test-secret-key-for-cms-controller-integration-test-only");
        registry.add("jwt.expiration", () -> "3600000");
    }

    // ============ DPC Patient Endpoint Tests ============

    @Test
    @DisplayName("getDpcPatient_withValidId_returns200: mocked DpcClient returns FHIR Patient JSON")
    void getDpcPatient_withValidId_returns200() throws Exception {
        String patientId = "patient-dpc-001";
        String fhirPatientJson = "{"
                + "\"resourceType\": \"Patient\","
                + "\"id\": \"patient-dpc-001\","
                + "\"name\": [{\"family\": \"Doe\", \"given\": [\"John\"]}],"
                + "\"birthDate\": \"1960-05-15\""
                + "}";

        when(dpcClient.getPatient(patientId)).thenReturn(fhirPatientJson);

        mockMvc.perform(get("/api/v1/cms/dpc/patient/{id}", patientId)
                        .header("X-Auth-Validated", "gateway-test-token")
                        .header("X-Auth-User-Id", UUID.randomUUID().toString())
                        .header("X-Auth-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Patient"))
                .andExpect(jsonPath("$.id").value(patientId));
    }

    @Test
    @DisplayName("getDpcConditions_withValidId_returns200: mocked DpcClient returns FHIR Bundle")
    void getDpcConditions_withValidId_returns200() throws Exception {
        String patientId = "patient-dpc-002";
        String fhirConditionsJson = "{"
                + "\"resourceType\": \"Bundle\","
                + "\"type\": \"searchset\","
                + "\"entry\": [{"
                + "  \"resource\": {"
                + "    \"resourceType\": \"Condition\","
                + "    \"id\": \"condition-001\","
                + "    \"code\": {\"coding\": [{\"system\": \"http://hl7.org/fhir/sid/icd-10\", \"code\": \"E11.9\"}]}"
                + "  }"
                + "}]}";

        when(dpcClient.getConditions(patientId)).thenReturn(fhirConditionsJson);

        mockMvc.perform(get("/api/v1/cms/dpc/conditions/{id}", patientId)
                        .header("X-Auth-Validated", "gateway-test-token")
                        .header("X-Auth-User-Id", UUID.randomUUID().toString())
                        .header("X-Auth-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.type").value("searchset"));
    }

    @Test
    @DisplayName("getDpcPatient_withEvaluatorRole_returns200: EVALUATOR role has access")
    @WithMockUser(roles = {"EVALUATOR"})
    void getDpcPatient_withEvaluatorRole_returns200() throws Exception {
        String patientId = "patient-dpc-005";
        String fhirPatientJson = "{"
                + "\"resourceType\": \"Patient\","
                + "\"id\": \"patient-dpc-005\""
                + "}";

        when(dpcClient.getPatient(patientId)).thenReturn(fhirPatientJson);

        mockMvc.perform(get("/api/v1/cms/dpc/patient/{id}", patientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Patient"));
    }

    @Test
    @DisplayName("getDpcPatient_withViewerRole_returns403: VIEWER role is denied")
    @WithMockUser(roles = {"VIEWER"})
    void getDpcPatient_withViewerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/cms/dpc/patient/{id}", "patient-dpc-006")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getDpcPatientEverything_withValidId_returns200: returns full FHIR collection Bundle")
    void getDpcPatientEverything_withValidId_returns200() throws Exception {
        String patientId = "patient-dpc-004";
        String fhirEverythingJson = "{"
                + "\"resourceType\": \"Bundle\","
                + "\"type\": \"collection\","
                + "\"total\": 2,"
                + "\"entry\": ["
                + "  {\"resource\": {\"resourceType\": \"Patient\", \"id\": \"patient-dpc-004\"}},"
                + "  {\"resource\": {\"resourceType\": \"Condition\", \"id\": \"condition-001\"}}"
                + "]}";

        when(dpcClient.getPatientEverything(patientId)).thenReturn(fhirEverythingJson);

        mockMvc.perform(get("/api/v1/cms/dpc/patient/{id}/everything", patientId)
                        .header("X-Auth-Validated", "gateway-test-token")
                        .header("X-Auth-User-Id", UUID.randomUUID().toString())
                        .header("X-Auth-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.type").value("collection"))
                .andExpect(jsonPath("$.total").value(2));
    }

    // ============ Health Check Tests ============

    @Test
    @DisplayName("getOverallHealth_returns200: combined DPC and BCDA status shows UP")
    void getOverallHealth_returns200WithUpStatus() throws Exception {
        DpcClient.DpcHealthStatus dpcHealth =
                new DpcClient.DpcHealthStatus("healthy", "v1", "https://sandbox.dpc.cms.gov");
        BcdaClient.BcdaHealthStatus bcdaHealth =
                new BcdaClient.BcdaHealthStatus("healthy", "v2", "https://sandbox.bcda.cms.gov");

        when(dpcClient.getHealthStatus()).thenReturn(dpcHealth);
        when(bcdaClient.getHealthStatus()).thenReturn(bcdaHealth);

        mockMvc.perform(get("/api/v1/cms/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.dpc.status").value("healthy"))
                .andExpect(jsonPath("$.bcda.status").value("healthy"));
    }

    @Test
    @DisplayName("getDpcHealth_returns200: DPC health endpoint returns status details")
    void getDpcHealth_returns200WithHealthDetails() throws Exception {
        DpcClient.DpcHealthStatus dpcHealth =
                new DpcClient.DpcHealthStatus("UP", "v1", "https://sandbox.dpc.cms.gov");
        when(dpcClient.getHealthStatus()).thenReturn(dpcHealth);

        mockMvc.perform(get("/api/v1/cms/health/dpc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.endpoint").value("https://sandbox.dpc.cms.gov"));
    }
}
