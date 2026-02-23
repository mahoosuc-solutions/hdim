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

import com.healthdata.fhir.persistence.OrganizationRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Integration tests for OrganizationController.
 * Tests REST API endpoints with full Spring context, Testcontainers PostgreSQL,
 * and verifies CRUD + tenant isolation + name/identifier search operations.
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
@DisplayName("Organization Controller Integration Tests")
class OrganizationControllerIT {

    private static final String TENANT_ID = "tenant-test-1";
    private static final String OTHER_TENANT = "tenant-test-2";
    private static final String FHIR_JSON = "application/fhir+json";
    private static final String TC_URL = "jdbc:tc:postgresql:15-alpine:///testdb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

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
        organizationRepository.deleteAll();
        fhirContext = FhirContext.forR4();
        jsonParser = fhirContext.newJsonParser();
    }

    @Test
    @DisplayName("Create organization should return 201")
    void createOrganizationShouldReturn201() throws Exception {
        Organization org = createValidOrganization();
        String json = jsonParser.encodeResourceToString(org);

        MvcResult result = mockMvc.perform(post("/Organization")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Organization created = jsonParser.parseResource(Organization.class, responseJson);

        assertThat(created.hasId()).isTrue();
        assertThat(created.getName()).isEqualTo("Main Street Family Practice");
        assertThat(created.getActive()).isTrue();
    }

    @Test
    @DisplayName("Get organization should return 200")
    void getOrganizationShouldReturn200() throws Exception {
        Organization org = createValidOrganization();
        String json = jsonParser.encodeResourceToString(org);

        MvcResult createResult = mockMvc.perform(post("/Organization")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        Organization created = jsonParser.parseResource(Organization.class,
                createResult.getResponse().getContentAsString());
        String orgId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/Organization/{id}", orgId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orgId))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Get non-existent organization should return 404")
    void getNonExistentOrganizationShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/Organization/{id}", nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update organization should return 200")
    void updateOrganizationShouldReturn200() throws Exception {
        Organization org = createValidOrganization();
        String json = jsonParser.encodeResourceToString(org);

        MvcResult createResult = mockMvc.perform(post("/Organization")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        Organization created = jsonParser.parseResource(Organization.class,
                createResult.getResponse().getContentAsString());
        String orgId = created.getIdElement().getIdPart();

        created.setName("Updated Clinic Name");
        String updatedJson = jsonParser.encodeResourceToString(created);

        mockMvc.perform(put("/Organization/{id}", orgId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-2")
                .contentType(FHIR_JSON)
                .content(updatedJson))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Delete organization should return 204")
    void deleteOrganizationShouldReturn204() throws Exception {
        Organization org = createValidOrganization();
        String json = jsonParser.encodeResourceToString(org);

        MvcResult createResult = mockMvc.perform(post("/Organization")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        Organization created = jsonParser.parseResource(Organization.class,
                createResult.getResponse().getContentAsString());
        String orgId = created.getIdElement().getIdPart();

        mockMvc.perform(delete("/Organization/{id}", orgId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-3"))
            .andExpect(status().isNoContent());

        // Verify gone
        mockMvc.perform(get("/Organization/{id}", orgId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant isolation should prevent cross-tenant access")
    void tenantIsolationShouldPreventCrossTenantAccess() throws Exception {
        Organization org = createValidOrganization();
        String json = jsonParser.encodeResourceToString(org);

        MvcResult createResult = mockMvc.perform(post("/Organization")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        Organization created = jsonParser.parseResource(Organization.class,
                createResult.getResponse().getContentAsString());
        String orgId = created.getIdElement().getIdPart();

        // Different tenant — should not see it
        mockMvc.perform(get("/Organization/{id}", orgId)
                .header("X-Tenant-ID", OTHER_TENANT))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Search by name should return matching organizations in Bundle")
    void searchByNameShouldReturnBundle() throws Exception {
        Organization org1 = createValidOrganization();
        Organization org2 = createValidOrganization();
        org2.setId(UUID.randomUUID().toString());
        org2.setName("Riverside Medical Center");

        mockMvc.perform(post("/Organization")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(org1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/Organization")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(org2)))
            .andExpect(status().isCreated());

        // Search by name "Main" — should return only 1
        MvcResult result = mockMvc.perform(get("/Organization")
                .header("X-Tenant-ID", TENANT_ID)
                .param("name", "Main"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Search by identifier should return matching organization in Bundle")
    void searchByIdentifierShouldReturnBundle() throws Exception {
        Organization org = createValidOrganization();
        mockMvc.perform(post("/Organization")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(org)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/Organization")
                .header("X-Tenant-ID", TENANT_ID)
                .param("identifier", "http://healthdata.com|ORG-MAIN-001"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getTotal()).isEqualTo(1);
    }

    // ==================== Helper Methods ====================

    private Organization createValidOrganization() {
        Organization org = new Organization();
        org.setId(UUID.randomUUID().toString());
        org.setActive(true);
        org.setName("Main Street Family Practice");

        org.addIdentifier()
                .setSystem("http://healthdata.com/fhir/identifier/organization")
                .setValue("ORG-MAIN-001");

        CodeableConcept type = new CodeableConcept();
        type.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
                .setCode("prov")
                .setDisplay("Healthcare Provider");
        org.addType(type);

        return org;
    }
}
