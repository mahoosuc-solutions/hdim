package com.healthdata.fhir.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
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

import com.healthdata.fhir.persistence.PractitionerRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Integration tests for PractitionerController.
 * Tests REST API endpoints with full Spring context, Testcontainers PostgreSQL,
 * and verifies CRUD + tenant isolation + search operations.
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
@DisplayName("Practitioner Controller Integration Tests")
class PractitionerControllerIT {

    private static final String TENANT_ID = "tenant-test-1";
    private static final String OTHER_TENANT = "tenant-test-2";
    private static final String FHIR_JSON = "application/fhir+json";
    private static final String TC_URL = "jdbc:tc:postgresql:15-alpine:///testdb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PractitionerRepository practitionerRepository;

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
        practitionerRepository.deleteAll();
        fhirContext = FhirContext.forR4();
        jsonParser = fhirContext.newJsonParser();
    }

    @Test
    @DisplayName("Create practitioner should return 201")
    void createPractitionerShouldReturn201() throws Exception {
        Practitioner practitioner = createValidPractitioner();
        String json = jsonParser.encodeResourceToString(practitioner);

        MvcResult result = mockMvc.perform(post("/Practitioner")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Practitioner created = jsonParser.parseResource(Practitioner.class, responseJson);

        assertThat(created.hasId()).isTrue();
        assertThat(created.getNameFirstRep().getFamily()).isEqualTo("Chen");
        assertThat(created.getNameFirstRep().getGivenAsSingleString()).isEqualTo("Sarah");
        assertThat(created.getActive()).isTrue();
    }

    @Test
    @DisplayName("Get practitioner should return 200")
    void getPractitionerShouldReturn200() throws Exception {
        // Create first
        Practitioner practitioner = createValidPractitioner();
        String json = jsonParser.encodeResourceToString(practitioner);

        MvcResult createResult = mockMvc.perform(post("/Practitioner")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        Practitioner created = jsonParser.parseResource(Practitioner.class,
                createResult.getResponse().getContentAsString());
        String practitionerId = created.getIdElement().getIdPart();

        // Then get
        mockMvc.perform(get("/Practitioner/{id}", practitionerId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(practitionerId))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Get non-existent practitioner should return 404")
    void getNonExistentPractitionerShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/Practitioner/{id}", nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update practitioner should return 200")
    void updatePractitionerShouldReturn200() throws Exception {
        // Create first
        Practitioner practitioner = createValidPractitioner();
        String json = jsonParser.encodeResourceToString(practitioner);

        MvcResult createResult = mockMvc.perform(post("/Practitioner")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        Practitioner created = jsonParser.parseResource(Practitioner.class,
                createResult.getResponse().getContentAsString());
        String practitionerId = created.getIdElement().getIdPart();

        // Update
        created.getNameFirstRep().setFamily("Updated");
        String updatedJson = jsonParser.encodeResourceToString(created);

        mockMvc.perform(put("/Practitioner/{id}", practitionerId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-2")
                .contentType(FHIR_JSON)
                .content(updatedJson))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Delete practitioner should return 204")
    void deletePractitionerShouldReturn204() throws Exception {
        // Create first
        Practitioner practitioner = createValidPractitioner();
        String json = jsonParser.encodeResourceToString(practitioner);

        MvcResult createResult = mockMvc.perform(post("/Practitioner")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        Practitioner created = jsonParser.parseResource(Practitioner.class,
                createResult.getResponse().getContentAsString());
        String practitionerId = created.getIdElement().getIdPart();

        // Delete
        mockMvc.perform(delete("/Practitioner/{id}", practitionerId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-3"))
            .andExpect(status().isNoContent());

        // Verify gone
        mockMvc.perform(get("/Practitioner/{id}", practitionerId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant isolation should prevent cross-tenant access")
    void tenantIsolationShouldPreventCrossTenantAccess() throws Exception {
        // Create in tenant-1
        Practitioner practitioner = createValidPractitioner();
        String json = jsonParser.encodeResourceToString(practitioner);

        MvcResult createResult = mockMvc.perform(post("/Practitioner")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andReturn();

        Practitioner created = jsonParser.parseResource(Practitioner.class,
                createResult.getResponse().getContentAsString());
        String practitionerId = created.getIdElement().getIdPart();

        // Attempt access from different tenant — should return 404
        mockMvc.perform(get("/Practitioner/{id}", practitionerId)
                .header("X-Tenant-ID", OTHER_TENANT))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Search by name should return matching practitioners in Bundle")
    void searchByNameShouldReturnBundle() throws Exception {
        // Create two practitioners
        Practitioner p1 = createValidPractitioner();
        Practitioner p2 = createValidPractitioner();
        p2.setId(UUID.randomUUID().toString());
        p2.getNameFirstRep().setFamily("Lopez");
        p2.getNameFirstRep().getGiven().clear();
        p2.getNameFirstRep().addGiven("Maria");

        mockMvc.perform(post("/Practitioner")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(p1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/Practitioner")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(p2)))
            .andExpect(status().isCreated());

        // Search by name "Chen" — should return only 1
        MvcResult result = mockMvc.perform(get("/Practitioner")
                .header("X-Tenant-ID", TENANT_ID)
                .param("name", "Chen"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Search by identifier should return matching practitioner in Bundle")
    void searchByIdentifierShouldReturnBundle() throws Exception {
        Practitioner practitioner = createValidPractitioner();
        mockMvc.perform(post("/Practitioner")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(practitioner)))
            .andExpect(status().isCreated());

        // Search by NPI
        MvcResult result = mockMvc.perform(get("/Practitioner")
                .header("X-Tenant-ID", TENANT_ID)
                .param("identifier", "http://hl7.org/fhir/sid/us-npi|1234567890"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getTotal()).isEqualTo(1);
    }

    // ==================== Helper Methods ====================

    private Practitioner createValidPractitioner() {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(UUID.randomUUID().toString());
        practitioner.setActive(true);

        HumanName name = practitioner.addName();
        name.setFamily("Chen");
        name.addGiven("Sarah");
        name.addPrefix("Dr.");

        practitioner.addIdentifier()
                .setSystem("http://hl7.org/fhir/sid/us-npi")
                .setValue("1234567890");

        Practitioner.PractitionerQualificationComponent qualification = practitioner.addQualification();
        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v2-0360")
                .setCode("MD")
                .setDisplay("Doctor of Medicine");
        qualification.setCode(code);

        return practitioner;
    }
}
