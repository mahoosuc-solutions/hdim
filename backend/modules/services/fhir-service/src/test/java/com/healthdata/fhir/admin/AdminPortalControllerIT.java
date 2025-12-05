package com.healthdata.fhir.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

@SpringBootTest(
    properties = {
        "spring.cache.type=simple",
        "spring.data.redis.repositories.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
    },
    classes = {
        com.healthdata.fhir.FhirServiceApplication.class,
        com.healthdata.fhir.config.TestCacheConfiguration.class,
        com.healthdata.fhir.config.TestSecurityConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminPortalControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    private static final String H2_URL = "jdbc:h2:mem:admin_portal;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        // Enable auto-creation of database schema in tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        // JWT configuration for tests
        registry.add("jwt.secret", () -> "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        registry.add("jwt.access-token-expiration", () -> "1h");
        registry.add("jwt.refresh-token-expiration", () -> "1d");
        registry.add("jwt.issuer", () -> "test-issuer");
        registry.add("jwt.audience", () -> "test-audience");
    }

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        patientRepository.save(PatientEntity.builder()
                .id(UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95"))
                .tenantId("tenant-1")
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95\"}")
                .firstName("Maya")
                .lastName("Chen")
                .gender("female")
                .birthDate(LocalDate.of(1985, 5, 20))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build());
    }

    @Test
    void dashboardShouldReturnDynamicMetrics() throws Exception {
        String body = mockMvc.perform(get("/api/admin/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        assertThat(root.get("metrics")).isNotNull();
        assertThat(root.get("metrics").isArray()).isTrue();
        assertThat(root.get("metrics").get(0).get("id").asText()).isEqualTo("patient-registry");
    }

    @Test
    void serviceCatalogShouldExposePatientRegistry() throws Exception {
        mockMvc.perform(get("/api/admin/service-catalog")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(root.get("services")).isNotNull();
                    boolean contains = false;
                    for (JsonNode node : root.get("services")) {
                        if ("patient-registry".equals(node.get("serviceId").asText())) {
                            contains = true;
                            break;
                        }
                    }
                    assertThat(contains).isTrue();
                });
    }

    @Test
    void apiPresetsShouldIncludeReadPatient() throws Exception {
        mockMvc.perform(get("/api/admin/api-presets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(root.isArray()).isTrue();
                    assertThat(root).isNotEmpty();
                    boolean hasReadPreset = false;
                    for (JsonNode node : root) {
                        if ("read-patient".equals(node.get("id").asText())) {
                            hasReadPreset = true;
                            break;
                        }
                    }
                    assertThat(hasReadPreset).isTrue();
                });
    }
}
