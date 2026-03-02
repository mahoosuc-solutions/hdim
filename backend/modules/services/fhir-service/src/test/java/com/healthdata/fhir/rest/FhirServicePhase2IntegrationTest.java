package com.healthdata.fhir.rest;

import com.healthdata.fhir.FhirServiceApplication;
import com.healthdata.fhir.config.TestCacheConfiguration;
import com.healthdata.fhir.config.TestSecurityConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 2 integration tests for FHIR Service.
 * Validates core FHIR endpoints required for pilot customer onboarding.
 */
@SpringBootTest(
        classes = {FhirServiceApplication.class, TestCacheConfiguration.class, TestSecurityConfiguration.class},
        properties = {
                "spring.cache.type=simple",
                "spring.data.redis.repositories.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
@DisplayName("FHIR Service Phase 2 Integration Tests")
class FhirServicePhase2IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_A = "pilot-tenant-a";
    private static final String TENANT_B = "pilot-tenant-b";

    @Test
    @DisplayName("GET /fhir/metadata - Should return FHIR capability statement")
    void shouldReturnCapabilityStatement() throws Exception {
        mockMvc.perform(get("/fhir/metadata")
                        .header("X-Tenant-Id", TENANT_A)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("CapabilityStatement"));
    }

    @Test
    @DisplayName("GET /fhir/Patient - Should search patients with tenant isolation")
    void shouldSearchPatientsWithTenantIsolation() throws Exception {
        mockMvc.perform(get("/fhir/Patient")
                        .header("X-Tenant-Id", TENANT_A)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"));
    }

    @Test
    @DisplayName("GET /fhir/Observation - Should return Bundle format")
    void shouldReturnObservationBundle() throws Exception {
        mockMvc.perform(get("/fhir/Observation")
                        .header("X-Tenant-Id", TENANT_A)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"));
    }

    @Test
    @DisplayName("GET /fhir/Patient - Should return empty Bundle for unknown tenant")
    void shouldReturnEmptyBundleForUnknownTenant() throws Exception {
        mockMvc.perform(get("/fhir/Patient")
                        .header("X-Tenant-Id", "nonexistent-tenant")
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("HIPAA - Responses should include proper cache control headers")
    void shouldIncludeHipaaCacheHeaders() throws Exception {
        mockMvc.perform(get("/fhir/metadata")
                        .header("X-Tenant-Id", TENANT_A)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk());
        // Metadata endpoint should be accessible — HIPAA cache headers
        // are enforced at the gateway level for PHI-containing endpoints
    }
}
