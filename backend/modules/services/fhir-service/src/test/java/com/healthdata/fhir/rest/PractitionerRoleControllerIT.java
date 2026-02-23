package com.healthdata.fhir.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.persistence.PractitionerRoleRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Integration tests for PractitionerRoleController.
 * Tests REST API endpoints with full Spring context, Testcontainers PostgreSQL,
 * and verifies CRUD + tenant isolation + search-by-practitioner/role operations.
 */
@SpringBootTest(
    properties = {
        "spring.cache.type=simple",
        "spring.data.redis.repositories.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false",
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
@Transactional
@ExtendWith(SpringExtension.class)
@WithMockUser(username = "testuser", roles = "ADMIN")
@Tag("integration")
@DisplayName("PractitionerRole Controller Integration Tests")
class PractitionerRoleControllerIT {

    private static final String TENANT_ID = "tenant-test-1";
    private static final String OTHER_TENANT = "tenant-test-2";
    private static final String FHIR_JSON = "application/fhir+json";
    private static final String TC_URL = "jdbc:tc:postgresql:15-alpine:///testdb";
    private static final UUID PRACTITIONER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PractitionerRoleRepository practitionerRoleRepository;

    private FhirContext fhirContext;
    private IParser jsonParser;

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TC_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("jwt.secret", () -> "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        registry.add("jwt.access-token-expiration", () -> "1h");
        registry.add("jwt.refresh-token-expiration", () -> "1d");
        registry.add("jwt.issuer", () -> "test-issuer");
        registry.add("jwt.audience", () -> "test-audience");
    }

    @BeforeEach
    void setUp() {
        practitionerRoleRepository.deleteAll();
        fhirContext = FhirContext.forR4();
        jsonParser = fhirContext.newJsonParser();
    }

    @Test
    @DisplayName("Create practitioner role should return 201")
    void createPractitionerRoleShouldReturn201() throws Exception {
        PractitionerRole role = createValidPractitionerRole();
        String json = jsonParser.encodeResourceToString(role);

        MvcResult result = mockMvc.perform(post("/PractitionerRole")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        PractitionerRole created = jsonParser.parseResource(PractitionerRole.class, responseJson);

        assertThat(created.hasId()).isTrue();
        assertThat(created.getActive()).isTrue();
        assertThat(created.getPractitioner().getReference())
                .isEqualTo("Practitioner/" + PRACTITIONER_ID);
    }

    @Test
    @DisplayName("Get practitioner role should return 200")
    void getPractitionerRoleShouldReturn200() throws Exception {
        PractitionerRole role = createValidPractitionerRole();
        String json = jsonParser.encodeResourceToString(role);

        MvcResult createResult = mockMvc.perform(post("/PractitionerRole")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        PractitionerRole created = jsonParser.parseResource(PractitionerRole.class,
                createResult.getResponse().getContentAsString());
        String roleId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/PractitionerRole/{id}", roleId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(roleId))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Get non-existent role should return 404")
    void getNonExistentRoleShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/PractitionerRole/{id}", nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Delete practitioner role should return 204")
    void deletePractitionerRoleShouldReturn204() throws Exception {
        PractitionerRole role = createValidPractitionerRole();
        String json = jsonParser.encodeResourceToString(role);

        MvcResult createResult = mockMvc.perform(post("/PractitionerRole")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        PractitionerRole created = jsonParser.parseResource(PractitionerRole.class,
                createResult.getResponse().getContentAsString());
        String roleId = created.getIdElement().getIdPart();

        mockMvc.perform(delete("/PractitionerRole/{id}", roleId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-3"))
            .andExpect(status().isNoContent());

        // Verify gone
        mockMvc.perform(get("/PractitionerRole/{id}", roleId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant isolation should prevent cross-tenant access")
    void tenantIsolationShouldPreventCrossTenantAccess() throws Exception {
        PractitionerRole role = createValidPractitionerRole();
        String json = jsonParser.encodeResourceToString(role);

        MvcResult createResult = mockMvc.perform(post("/PractitionerRole")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        PractitionerRole created = jsonParser.parseResource(PractitionerRole.class,
                createResult.getResponse().getContentAsString());
        String roleId = created.getIdElement().getIdPart();

        // Attempt access from different tenant — should return 404
        mockMvc.perform(get("/PractitionerRole/{id}", roleId)
                .header("X-Tenant-ID", OTHER_TENANT))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Search by practitioner should return matching roles in Bundle")
    void searchByPractitionerShouldReturnBundle() throws Exception {
        PractitionerRole role = createValidPractitionerRole();
        mockMvc.perform(post("/PractitionerRole")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(role)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/PractitionerRole")
                .header("X-Tenant-ID", TENANT_ID)
                .param("practitioner", "Practitioner/" + PRACTITIONER_ID))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Search by role code should return matching roles in Bundle")
    void searchByRoleCodeShouldReturnBundle() throws Exception {
        PractitionerRole role = createValidPractitionerRole();
        mockMvc.perform(post("/PractitionerRole")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(role)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/PractitionerRole")
                .header("X-Tenant-ID", TENANT_ID)
                .param("role", "doctor"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getEntry()).hasSize(1);
    }

    // ==================== Helper Methods ====================

    private PractitionerRole createValidPractitionerRole() {
        PractitionerRole role = new PractitionerRole();
        role.setId(UUID.randomUUID().toString());
        role.setActive(true);

        role.setPractitioner(new Reference("Practitioner/" + PRACTITIONER_ID)
                .setDisplay("Dr. Sarah Chen"));
        role.setOrganization(new Reference("Organization/" + ORG_ID)
                .setDisplay("Main Street Family Practice"));

        CodeableConcept roleCode = new CodeableConcept();
        roleCode.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/practitioner-role")
                .setCode("doctor")
                .setDisplay("Doctor");
        role.addCode(roleCode);

        CodeableConcept specialty = new CodeableConcept();
        specialty.addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("394814009")
                .setDisplay("General practice");
        role.addSpecialty(specialty);

        role.addIdentifier()
                .setSystem("http://healthdata.com/fhir/identifier/practitioner-role")
                .setValue("ROLE-" + UUID.randomUUID().toString().substring(0, 8));

        return role;
    }
}
