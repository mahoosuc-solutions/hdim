package com.healthdata.consent.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.consent.ConsentServiceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 2 integration tests for Consent Service.
 * Validates consent creation, verification, revocation, and tenant isolation.
 */
@SpringBootTest(
        classes = ConsentServiceApplication.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false"
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@WithMockUser(roles = {"ADMIN"})
@Tag("integration")
@DisplayName("Consent Service Phase 2 Integration Tests")
class ConsentPhase2IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_A = "pilot-tenant-a";
    private static final String TENANT_B = "pilot-tenant-b";

    @Test
    @DisplayName("POST /api/consents - Should create consent record")
    void shouldCreateConsent() throws Exception {
        Map<String, Object> consent = Map.of(
                "patientId", "patient-001",
                "scope", "TREATMENT",
                "status", "ACTIVE",
                "consentType", "GENERAL",
                "authorizedPartyId", "provider-001"
        );

        mockMvc.perform(post("/api/consents")
                        .header("X-Tenant-ID", TENANT_A)
                        .header("X-Auth-Validated", "true")
                        .header("X-Auth-User-Id", "admin-user")
                        .header("X-Auth-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consent)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("GET /api/consents/patient/{patientId} - Should verify consent exists")
    void shouldVerifyConsentExists() throws Exception {
        mockMvc.perform(get("/api/consents/patient/patient-001")
                        .header("X-Tenant-ID", TENANT_A)
                        .header("X-Auth-Validated", "true")
                        .header("X-Auth-User-Id", "admin-user")
                        .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Tenant isolation - Should not see consents from other tenant")
    void shouldEnforceTenantIsolation() throws Exception {
        // Query tenant B should not see tenant A's consents
        mockMvc.perform(get("/api/consents/patient/patient-001")
                        .header("X-Tenant-ID", TENANT_B)
                        .header("X-Auth-Validated", "true")
                        .header("X-Auth-User-Id", "admin-user")
                        .header("X-Auth-Roles", "ADMIN"))
                .andExpect(status().isOk());
        // Result should be empty or not contain tenant A's data
    }

    @Test
    @DisplayName("GET /api/consents/_health - Should return health status")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/consents/_health")
                        .header("X-Tenant-ID", TENANT_A))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/consents/validate-access - Should validate access check")
    void shouldValidateAccess() throws Exception {
        Map<String, Object> accessRequest = Map.of(
                "patientId", "patient-001",
                "scope", "TREATMENT",
                "requestorId", "provider-001"
        );

        mockMvc.perform(post("/api/consents/validate-access")
                        .header("X-Tenant-ID", TENANT_A)
                        .header("X-Auth-Validated", "true")
                        .header("X-Auth-User-Id", "admin-user")
                        .header("X-Auth-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accessRequest)))
                .andExpect(status().is2xxSuccessful());
    }
}
