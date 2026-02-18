package com.healthdata.consent.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.consent.ConsentServiceApplication;
import com.healthdata.consent.persistence.ConsentEntity;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Boot integration tests for ConsentController.
 *
 * Validates:
 * 1. POST /api/consents -> GET /api/consents/{id} round-trip returns an ACTIVE consent
 * 2. Multi-tenant isolation: tenant-b cannot retrieve a consent created by tenant-a
 *
 * Uses Testcontainers PostgreSQL for a real database context.
 * KafkaTemplate is mocked with @MockBean to avoid broker dependency in CI.
 * The "test" profile security config permits all HTTP requests;
 * @WithMockUser populates the SecurityContext for @PreAuthorize method security.
 *
 * Note on URL paths: MockMvc bypasses the embedded servlet container, so requests
 * are dispatched directly against the DispatcherServlet without the context-path
 * prefix. Use /api/consents (not /consent/api/consents) in test requests.
 */
@Tag("integration")
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
class ConsentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("consent_it_db")
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

    /** Mocked to avoid requiring a live Kafka broker. ConsentService autowires KafkaTemplate. */
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";
    private static final UUID PATIENT_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    private ConsentEntity buildConsent(String tenantId) {
        return ConsentEntity.builder()
                .patientId(PATIENT_ID)
                .tenantId(tenantId)
                .scope("read")
                .status("active")
                .category("treatment")
                .purpose("Quality care and treatment")
                .provisionType("permit")
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now().plusYears(1))
                .consentDate(LocalDate.now())
                .verificationMethod("electronic-signature")
                .verifiedBy("test-user")
                .verificationDate(LocalDate.now())
                .createdBy("test-user")
                .lastModifiedBy("test-user")
                .version(0)
                .build();
    }

    /**
     * POST a consent record then GET it back by ID.
     * Asserts the returned record has status "active".
     */
    @Test
    void createConsent_thenRetrieve_returnsActiveConsent() throws Exception {
        String requestBody = objectMapper.writeValueAsString(buildConsent(TENANT_A));

        // Create consent
        MvcResult createResult = mockMvc.perform(post("/api/consents")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT_A)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("active"))
            .andReturn();

        // Extract the created consent ID
        ConsentEntity created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ConsentEntity.class);
        assertThat(created.getId()).isNotNull();

        // Retrieve by ID
        mockMvc.perform(get("/api/consents/{id}", created.getId())
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT_A))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(created.getId().toString()))
            .andExpect(jsonPath("$.status").value("active"))
            .andExpect(jsonPath("$.tenantId").value(TENANT_A));
    }

    /**
     * Verifies multi-tenant isolation:
     * A consent created under tenant-a must NOT be visible to tenant-b.
     * The service filters by tenantId in its repository queries, so GET
     * with a different tenant header returns 404.
     */
    @Test
    void wrongTenant_cannotAccessOtherTenantConsent() throws Exception {
        String requestBody = objectMapper.writeValueAsString(buildConsent(TENANT_A));

        // Create consent under tenant-a
        MvcResult createResult = mockMvc.perform(post("/api/consents")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT_A)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

        ConsentEntity created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ConsentEntity.class);
        assertThat(created.getId()).isNotNull();

        // Attempt to retrieve as tenant-b — must return 404
        mockMvc.perform(get("/api/consents/{id}", created.getId())
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT_B))
            .andExpect(status().isNotFound());
    }
}
