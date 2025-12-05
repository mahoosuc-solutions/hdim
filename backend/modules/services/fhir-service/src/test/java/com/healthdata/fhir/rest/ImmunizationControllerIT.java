package com.healthdata.fhir.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.fhir.persistence.ImmunizationEntity;
import com.healthdata.fhir.persistence.ImmunizationRepository;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

/**
 * Integration tests for ImmunizationController REST endpoints.
 * Tests HTTP request/response handling and REST API compliance.
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
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "testuser")
class ImmunizationControllerIT {

    private static final String H2_URL = "jdbc:h2:mem:healthdata_fhir_controller;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
    private static final String TENANT_ID = "tenant-1";
    private static final String USER_ID = "user-test";
    private static final String CVX_CODE_COVID = "213";
    private static final String CVX_CODE_FLU = "141";

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.enabled", () -> "false");
        // JWT configuration for tests
        registry.add("jwt.secret", () -> "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        registry.add("jwt.access-token-expiration", () -> "1h");
        registry.add("jwt.refresh-token-expiration", () -> "1d");
        registry.add("jwt.issuer", () -> "test-issuer");
        registry.add("jwt.audience", () -> "test-audience");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImmunizationRepository immunizationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID patientId;

    @BeforeEach
    void setUp() {
        // Create test patient
        patientId = UUID.randomUUID();
        PatientEntity patient = PatientEntity.builder()
                .id(patientId)
                .tenantId(TENANT_ID)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + patientId + "\"}")
                .firstName("Test")
                .lastName("Patient")
                .gender("male")
                .birthDate(LocalDate.of(1980, 1, 1))
                .build();
        patientRepository.save(patient);
    }

    @Test
    void postImmunizationShouldReturn201WithCreatedResource() throws Exception {
        // Given
        String immunizationJson = createFhirImmunizationJson(patientId.toString(), CVX_CODE_COVID);

        // When / Then
        MvcResult result = mockMvc.perform(post("/fhir/Immunization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType("application/fhir+json")
                        .content(immunizationJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/fhir+json"))
                .andReturn();

        // Verify response contains resource
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).isNotEmpty();
        assertThat(responseBody).contains("resourceType");
        assertThat(responseBody).contains("Immunization");
    }

    @Test
    void getImmunizationByIdShouldReturn200WhenExists() throws Exception {
        // Given
        ImmunizationEntity entity = createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now());

        // When / Then
        mockMvc.perform(get("/fhir/Immunization/" + entity.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"))
                .andExpect(jsonPath("$.resourceType").value("Immunization"))
                .andExpect(jsonPath("$.id").value(entity.getId().toString()))
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void getImmunizationByIdShouldReturn404WhenNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When / Then
        mockMvc.perform(get("/fhir/Immunization/" + nonExistentId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept("application/fhir+json"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateImmunizationShouldReturn200() throws Exception {
        // Given
        ImmunizationEntity entity = createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now());
        String updatedJson = createFhirImmunizationJson(patientId.toString(), CVX_CODE_COVID);

        // When / Then
        mockMvc.perform(put("/fhir/Immunization/" + entity.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType("application/fhir+json")
                        .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"));
    }

    @Test
    void deleteImmunizationShouldReturn204() throws Exception {
        // Given
        ImmunizationEntity entity = createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now());

        // When / Then
        mockMvc.perform(delete("/fhir/Immunization/" + entity.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isNoContent());

        // Verify deletion
        assertThat(immunizationRepository.findById(entity.getId())).isEmpty();
    }

    @Test
    void searchByPatientShouldReturnBundleWithResults() throws Exception {
        // Given
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30));
        createAndSaveImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15));

        // When / Then
        MvcResult result = mockMvc.perform(get("/fhir/Immunization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"))
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.type").value("searchset"))
                .andExpect(jsonPath("$.total").value(2))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode bundle = objectMapper.readTree(responseBody);
        assertThat(bundle.get("entry").size()).isEqualTo(2);
    }

    @Test
    void searchByPatientShouldReturn400WhenPatientParameterMissing() throws Exception {
        // When / Then
        mockMvc.perform(get("/fhir/Immunization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept("application/fhir+json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchByPatientAndVaccineShouldFilterResults() throws Exception {
        // Given
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60));
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30));
        createAndSaveImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15));

        // When / Then
        MvcResult result = mockMvc.perform(get("/fhir/Immunization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("vaccine", CVX_CODE_COVID)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.total").value(2))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode bundle = objectMapper.readTree(responseBody);
        assertThat(bundle.get("entry").size()).isEqualTo(2);
    }

    @Test
    void getCompletedImmunizationsShouldReturnOnlyCompleted() throws Exception {
        // Given
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30));
        createAndSaveImmunization(CVX_CODE_FLU, "not-done", LocalDate.now().minusDays(15));
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(10));

        // When / Then
        mockMvc.perform(get("/fhir/Immunization/completed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void hasImmunizationShouldReturnTrueWhenExists() throws Exception {
        // Given
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now());

        // When / Then
        MvcResult result = mockMvc.perform(get("/fhir/Immunization/has-immunization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("vaccine", CVX_CODE_COVID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasImmunization").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(response.get("hasImmunization").asBoolean()).isTrue();
    }

    @Test
    void hasImmunizationShouldReturnFalseWhenNotExists() throws Exception {
        // When / Then
        mockMvc.perform(get("/fhir/Immunization/has-immunization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("vaccine", CVX_CODE_FLU)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasImmunization").value(false));
    }

    @Test
    void getVaccineSeriesShouldReturnOrderedDoses() throws Exception {
        // Given
        ImmunizationEntity dose1 = createAndSaveImmunization(CVX_CODE_COVID, "completed",
                LocalDate.now().minusDays(60));
        dose1.setDoseNumber(1);
        dose1.setSeriesDoses(2);
        immunizationRepository.save(dose1);

        ImmunizationEntity dose2 = createAndSaveImmunization(CVX_CODE_COVID, "completed",
                LocalDate.now().minusDays(30));
        dose2.setDoseNumber(2);
        dose2.setSeriesDoses(2);
        immunizationRepository.save(dose2);

        // When / Then
        mockMvc.perform(get("/fhir/Immunization/series")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("vaccine", CVX_CODE_COVID)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void isSeriesCompleteShouldReturnTrueWhenComplete() throws Exception {
        // Given
        ImmunizationEntity dose1 = createAndSaveImmunization(CVX_CODE_COVID, "completed",
                LocalDate.now().minusDays(60));
        dose1.setDoseNumber(1);
        immunizationRepository.save(dose1);

        ImmunizationEntity dose2 = createAndSaveImmunization(CVX_CODE_COVID, "completed",
                LocalDate.now().minusDays(30));
        dose2.setDoseNumber(2);
        immunizationRepository.save(dose2);

        // When / Then
        mockMvc.perform(get("/fhir/Immunization/series-complete")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("vaccine", CVX_CODE_COVID)
                        .param("requiredDoses", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isComplete").value(true));
    }

    @Test
    void isSeriesCompleteShouldReturnFalseWhenIncomplete() throws Exception {
        // Given
        ImmunizationEntity dose1 = createAndSaveImmunization(CVX_CODE_COVID, "completed",
                LocalDate.now().minusDays(60));
        dose1.setDoseNumber(1);
        immunizationRepository.save(dose1);

        // When / Then (requires 3 doses, but only 1 completed)
        mockMvc.perform(get("/fhir/Immunization/series-complete")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("vaccine", CVX_CODE_COVID)
                        .param("requiredDoses", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isComplete").value(false));
    }

    @Test
    void getImmunizationsWithReactionsShouldReturnFiltered() throws Exception {
        // Given
        ImmunizationEntity withReaction = createAndSaveImmunization(CVX_CODE_COVID, "completed",
                LocalDate.now().minusDays(30));
        withReaction.setHadReaction(true);
        withReaction.setReactionDetail("Mild fever");
        immunizationRepository.save(withReaction);

        createAndSaveImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15));

        // When / Then
        mockMvc.perform(get("/fhir/Immunization/with-reactions")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void countImmunizationsShouldReturnCorrectCount() throws Exception {
        // Given
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60));
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30));
        createAndSaveImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15));

        // When / Then
        mockMvc.perform(get("/fhir/Immunization/count")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void countByVaccineCodeShouldReturnFilteredCount() throws Exception {
        // Given
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60));
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30));
        createAndSaveImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15));

        // When / Then
        mockMvc.perform(get("/fhir/Immunization/count")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("vaccine", CVX_CODE_COVID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void getComplianceReportShouldReturnReport() throws Exception {
        // Given
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30));
        createAndSaveImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15));

        // When / Then
        mockMvc.perform(get("/fhir/Immunization/compliance")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalImmunizations").value(2))
                .andExpect(jsonPath("$.complianceStatus").value("compliant"));
    }

    @Test
    void healthCheckShouldReturn200() throws Exception {
        // When / Then
        mockMvc.perform(get("/fhir/Immunization/_health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldEnforceTenantIsolation() throws Exception {
        // Given
        String tenant2 = "tenant-2";
        UUID patient2Id = UUID.randomUUID();

        PatientEntity patient2 = PatientEntity.builder()
                .id(patient2Id)
                .tenantId(tenant2)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + patient2Id + "\"}")
                .firstName("Tenant2")
                .lastName("Patient")
                .gender("female")
                .birthDate(LocalDate.of(1985, 1, 1))
                .build();
        patientRepository.save(patient2);

        // Create immunization in tenant-1
        createAndSaveImmunization(CVX_CODE_COVID, "completed", LocalDate.now());

        // Create immunization in tenant-2
        ImmunizationEntity tenant2Imm = ImmunizationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenant2)
                .patientId(patient2Id)
                .vaccineCode(CVX_CODE_FLU)
                .vaccineSystem("http://hl7.org/fhir/sid/cvx")
                .vaccineDisplay("Influenza vaccine")
                .status("completed")
                .occurrenceDate(LocalDate.now())
                .build();
        immunizationRepository.save(tenant2Imm);

        // When / Then - Tenant-1 should only see their data
        mockMvc.perform(get("/fhir/Immunization")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        // When / Then - Tenant-2 should only see their data
        mockMvc.perform(get("/fhir/Immunization")
                        .header("X-Tenant-ID", tenant2)
                        .param("patient", "Patient/" + patient2Id)
                        .accept("application/fhir+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    // Helper methods
    private ImmunizationEntity createAndSaveImmunization(String vaccineCode, String status,
                                                          LocalDate occurrenceDate) {
        ImmunizationEntity entity = ImmunizationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientId)
                .vaccineCode(vaccineCode)
                .vaccineSystem("http://hl7.org/fhir/sid/cvx")
                .vaccineDisplay(vaccineCode.equals(CVX_CODE_COVID) ? "COVID-19 vaccine" : "Influenza vaccine")
                .status(status)
                .occurrenceDate(occurrenceDate)
                .primarySource(true)
                .build();

        return immunizationRepository.save(entity);
    }

    private String createFhirImmunizationJson(String patientId, String vaccineCode) {
        return String.format("""
            {
              "resourceType": "Immunization",
              "status": "completed",
              "vaccineCode": {
                "coding": [{
                  "system": "http://hl7.org/fhir/sid/cvx",
                  "code": "%s",
                  "display": "COVID-19 vaccine"
                }]
              },
              "patient": {
                "reference": "Patient/%s"
              },
              "occurrenceDateTime": "2024-01-15",
              "primarySource": true,
              "lotNumber": "LOT123"
            }
            """, vaccineCode, patientId);
    }
}
