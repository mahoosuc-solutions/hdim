package com.healthdata.fhir.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
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

import com.healthdata.fhir.persistence.CoverageRepository;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Integration tests for CoverageController.
 * Tests REST API endpoints with full Spring context.
 */
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
@Transactional
@ExtendWith(SpringExtension.class)
@WithMockUser(username = "testuser")
class CoverageControllerIT {

    private static final String TENANT_ID = "tenant-test-1";
    private static final UUID PATIENT_ID = UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    private static final String FHIR_JSON = "application/fhir+json";
    private static final String H2_URL = "jdbc:h2:mem:healthdata_fhir_coverage_controller;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CoverageRepository coverageRepository;

    @Autowired
    private PatientRepository patientRepository;

    private FhirContext fhirContext;
    private IParser jsonParser;

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("jwt.secret", () -> "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        registry.add("jwt.access-token-expiration", () -> "1h");
        registry.add("jwt.refresh-token-expiration", () -> "1d");
        registry.add("jwt.issuer", () -> "test-issuer");
        registry.add("jwt.audience", () -> "test-audience");
    }

    @BeforeEach
    void setUp() {
        coverageRepository.deleteAll();
        patientRepository.deleteAll();

        PatientEntity patient = PatientEntity.builder()
                .id(PATIENT_ID)
                .tenantId(TENANT_ID)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + PATIENT_ID + "\"}")
                .firstName("Test")
                .lastName("Patient")
                .birthDate(LocalDate.of(1980, 1, 1))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
        patientRepository.save(patient);

        fhirContext = FhirContext.forR4();
        jsonParser = fhirContext.newJsonParser();
    }

    @Test
    void createCoverageShouldReturn201() throws Exception {
        Coverage coverage = createValidCoverage();
        String coverageJson = jsonParser.encodeResourceToString(coverage);

        MvcResult result = mockMvc.perform(post("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(coverageJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(FHIR_JSON))
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Coverage created = jsonParser.parseResource(Coverage.class, responseJson);

        assertThat(created.hasId()).isTrue();
        assertThat(created.getStatus()).isEqualTo(Coverage.CoverageStatus.ACTIVE);
        assertThat(created.getSubscriberId()).isEqualTo("MEM123456");
    }

    @Test
    void getCoverageShouldReturn200() throws Exception {
        Coverage coverage = createValidCoverage();
        String coverageJson = jsonParser.encodeResourceToString(coverage);

        MvcResult createResult = mockMvc.perform(post("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(coverageJson))
            .andExpect(status().isCreated())
            .andReturn();

        Coverage created = jsonParser.parseResource(Coverage.class, createResult.getResponse().getContentAsString());
        String coverageId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/fhir/Coverage/{id}", coverageId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(FHIR_JSON))
            .andExpect(jsonPath("$.id").value(coverageId))
            .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void getCoverageNotFoundShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/fhir/Coverage/{id}", nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateCoverageShouldReturn200() throws Exception {
        Coverage coverage = createValidCoverage();
        String coverageJson = jsonParser.encodeResourceToString(coverage);

        MvcResult createResult = mockMvc.perform(post("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(coverageJson))
            .andExpect(status().isCreated())
            .andReturn();

        Coverage created = jsonParser.parseResource(Coverage.class, createResult.getResponse().getContentAsString());
        String coverageId = created.getIdElement().getIdPart();

        created.setStatus(Coverage.CoverageStatus.CANCELLED);
        String updatedJson = jsonParser.encodeResourceToString(created);

        mockMvc.perform(put("/fhir/Coverage/{id}", coverageId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-2")
                .contentType(FHIR_JSON)
                .content(updatedJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("cancelled"));
    }

    @Test
    void deleteCoverageShouldReturn204() throws Exception {
        Coverage coverage = createValidCoverage();
        String coverageJson = jsonParser.encodeResourceToString(coverage);

        MvcResult createResult = mockMvc.perform(post("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(coverageJson))
            .andExpect(status().isCreated())
            .andReturn();

        Coverage created = jsonParser.parseResource(Coverage.class, createResult.getResponse().getContentAsString());
        String coverageId = created.getIdElement().getIdPart();

        mockMvc.perform(delete("/fhir/Coverage/{id}", coverageId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-3"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/fhir/Coverage/{id}", coverageId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void searchByPatientShouldReturnBundle() throws Exception {
        Coverage coverage1 = createValidCoverage();
        Coverage coverage2 = createValidCoverage();
        coverage2.setId(UUID.randomUUID().toString());
        coverage2.setSubscriberId("MEM789012");

        mockMvc.perform(post("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(coverage1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(coverage2)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", PATIENT_ID.toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(FHIR_JSON))
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getEntry()).hasSize(2);
    }

    @Test
    void tenantIsolationShouldPreventCrossTenantAccess() throws Exception {
        Coverage coverage = createValidCoverage();
        String coverageJson = jsonParser.encodeResourceToString(coverage);

        MvcResult createResult = mockMvc.perform(post("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(coverageJson))
            .andExpect(status().isCreated())
            .andReturn();

        Coverage created = jsonParser.parseResource(Coverage.class, createResult.getResponse().getContentAsString());
        String coverageId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/fhir/Coverage/{id}", coverageId)
                .header("X-Tenant-ID", "different-tenant"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getActiveCoveragesShouldFilterByStatus() throws Exception {
        Coverage activeCoverage = createValidCoverage();
        Coverage cancelledCoverage = createValidCoverage();
        cancelledCoverage.setId(UUID.randomUUID().toString());
        cancelledCoverage.setStatus(Coverage.CoverageStatus.CANCELLED);

        mockMvc.perform(post("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(activeCoverage)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(cancelledCoverage)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/Coverage")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", PATIENT_ID.toString())
                .param("status", "active"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getEntry()).hasSize(1);
    }

    private Coverage createValidCoverage() {
        Coverage coverage = new Coverage();
        coverage.setId(UUID.randomUUID().toString());
        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
        coverage.setBeneficiary(new Reference("Patient/" + PATIENT_ID));
        coverage.addPayor(new Reference("Organization/payor-1").setDisplay("Blue Cross"));
        coverage.setSubscriberId("MEM123456");
        coverage.setType(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                        .setCode("HIP")
                        .setDisplay("health insurance plan policy")));
        coverage.setPeriod(new Period()
                .setStart(new Date())
                .setEnd(Date.from(Instant.now().plusSeconds(365 * 24 * 60 * 60))));
        return coverage;
    }
}
