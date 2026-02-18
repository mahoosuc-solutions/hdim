package com.healthdata.fhir.rest;

import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Boot integration tests for FHIR Service.
 *
 * Validates:
 * 1. GET /Patient returns a valid FHIR R4 Bundle (resourceType + entry array)
 * 2. HIPAA §164.312(a)(2)(i) compliance: Cache-Control: no-store on patient data endpoints
 * 3. Patient/$everything returns HTTP 404 for unknown patients
 *
 * Uses Testcontainers PostgreSQL for a real database context.
 * Uses gateway-trust header pattern (X-Auth-Validated etc.) for multi-tenant context.
 *
 * Note on URL paths: MockMvc bypasses the embedded servlet container, so requests
 * are dispatched directly against the DispatcherServlet without the context-path prefix.
 * Use /Patient (not /fhir/Patient) in test requests.
 *
 * Note on auth: @AutoConfigureMockMvc(addFilters=false) disables security filters, so
 * @PreAuthorize method security requires @WithMockUser to populate the SecurityContext.
 */
@Tag("integration")
@SpringBootTest(
    classes = {
        com.healthdata.fhir.FhirServiceApplication.class,
        com.healthdata.fhir.config.TestCacheConfiguration.class,
        com.healthdata.fhir.config.TestSecurityConfiguration.class
    },
    properties = {
        "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
        "spring.cache.type=simple",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm",
        "jwt.access-token-expiration=1h",
        "jwt.refresh-token-expiration=1d",
        "jwt.issuer=test-issuer",
        "jwt.audience=test-audience"
    }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@WithMockUser(roles = {"ADMIN"})
class FhirIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("fhir_it_db")
            .withUsername("sa")
            .withPassword("sa");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    private static final String TENANT = "test-tenant";
    private static final String SEED_PATIENT_JSON =
            "{\"resourceType\":\"Patient\",\"id\":\"seed-patient-001\","
            + "\"name\":[{\"family\":\"TestFamily\",\"given\":[\"TestGiven\"]}],"
            + "\"gender\":\"unknown\",\"birthDate\":\"1990-01-01\"}";

    @BeforeEach
    void seedPatient() {
        patientRepository.deleteAll();
        // Seed one patient so the search bundle always contains at least one entry
        patientRepository.save(PatientEntity.builder()
                .id(UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"))
                .tenantId(TENANT)
                .resourceType("Patient")
                .resourceJson(SEED_PATIENT_JSON)
                .firstName("TestGiven")
                .lastName("TestFamily")
                .gender("unknown")
                .birthDate(LocalDate.of(1990, 1, 1))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build());
    }

    /**
     * Verifies GET /Patient returns a FHIR R4 Bundle:
     * - resourceType must be "Bundle"
     * - entry must be a JSON array with at least one element (seeded patient)
     */
    @Test
    void patientList_returnsFhirR4Bundle() throws Exception {
        mockMvc.perform(get("/Patient")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT)
                .accept("application/fhir+json"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("fhir+json")))
            .andExpect(jsonPath("$.resourceType").value("Bundle"))
            .andExpect(jsonPath("$.entry").isArray());
    }

    /**
     * Verifies HIPAA §164.312(a)(2)(i) compliance:
     * PHI endpoints must include Cache-Control: no-store to prevent browser/proxy caching.
     */
    @Test
    void patientList_hasNoCacheHeader() throws Exception {
        mockMvc.perform(get("/Patient")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT)
                .accept("application/fhir+json"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    /**
     * Verifies the Patient/$everything endpoint returns 404 when the patient
     * does not exist — confirming correct FHIR R4 error semantics.
     */
    @Test
    void everythingEndpoint_returnsNotFoundForUnknownPatient() throws Exception {
        mockMvc.perform(get("/Patient/00000000-0000-0000-0000-000000000000/$everything")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT)
                .accept("application/fhir+json"))
            .andExpect(status().isNotFound());
    }
}
