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

import com.healthdata.fhir.persistence.CarePlanRepository;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Integration tests for CarePlanController.
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
@Tag("integration")
class CarePlanControllerIT {

    private static final String TENANT_ID = "tenant-test-1";
    private static final UUID PATIENT_ID = UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    private static final String FHIR_JSON = "application/fhir+json";
    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarePlanRepository carePlanRepository;

    @Autowired
    private PatientRepository patientRepository;

    private FhirContext fhirContext;
    private IParser jsonParser;

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
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
        carePlanRepository.deleteAll();
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
    void createCarePlanShouldReturn201() throws Exception {
        CarePlan carePlan = createValidCarePlan();
        String carePlanJson = jsonParser.encodeResourceToString(carePlan);

        MvcResult result = mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(carePlanJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(FHIR_JSON))
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        CarePlan created = jsonParser.parseResource(CarePlan.class, responseJson);

        assertThat(created.hasId()).isTrue();
        assertThat(created.getStatus()).isEqualTo(CarePlan.CarePlanStatus.ACTIVE);
        assertThat(created.getIntent()).isEqualTo(CarePlan.CarePlanIntent.PLAN);
    }

    @Test
    void getCarePlanShouldReturn200() throws Exception {
        CarePlan carePlan = createValidCarePlan();
        String carePlanJson = jsonParser.encodeResourceToString(carePlan);

        MvcResult createResult = mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(carePlanJson))
            .andExpect(status().isCreated())
            .andReturn();

        CarePlan created = jsonParser.parseResource(CarePlan.class, createResult.getResponse().getContentAsString());
        String carePlanId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/fhir/CarePlan/{id}", carePlanId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(FHIR_JSON))
            .andExpect(jsonPath("$.id").value(carePlanId))
            .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void getCarePlanNotFoundShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/fhir/CarePlan/{id}", nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateCarePlanShouldReturn200() throws Exception {
        CarePlan carePlan = createValidCarePlan();
        String carePlanJson = jsonParser.encodeResourceToString(carePlan);

        MvcResult createResult = mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(carePlanJson))
            .andExpect(status().isCreated())
            .andReturn();

        CarePlan created = jsonParser.parseResource(CarePlan.class, createResult.getResponse().getContentAsString());
        String carePlanId = created.getIdElement().getIdPart();

        created.setStatus(CarePlan.CarePlanStatus.COMPLETED);
        String updatedJson = jsonParser.encodeResourceToString(created);

        mockMvc.perform(put("/fhir/CarePlan/{id}", carePlanId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-2")
                .contentType(FHIR_JSON)
                .content(updatedJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void deleteCarePlanShouldReturn204() throws Exception {
        CarePlan carePlan = createValidCarePlan();
        String carePlanJson = jsonParser.encodeResourceToString(carePlan);

        MvcResult createResult = mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(carePlanJson))
            .andExpect(status().isCreated())
            .andReturn();

        CarePlan created = jsonParser.parseResource(CarePlan.class, createResult.getResponse().getContentAsString());
        String carePlanId = created.getIdElement().getIdPart();

        mockMvc.perform(delete("/fhir/CarePlan/{id}", carePlanId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-3"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/fhir/CarePlan/{id}", carePlanId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void searchByPatientShouldReturnBundle() throws Exception {
        CarePlan carePlan1 = createValidCarePlan();
        CarePlan carePlan2 = createValidCarePlan();
        carePlan2.setId(UUID.randomUUID().toString());
        carePlan2.setTitle("Hypertension Management Plan");

        mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(carePlan1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(carePlan2)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/CarePlan")
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
    void filterByStatusShouldReturnMatching() throws Exception {
        CarePlan activePlan = createValidCarePlan();
        CarePlan completedPlan = createValidCarePlan();
        completedPlan.setId(UUID.randomUUID().toString());
        completedPlan.setStatus(CarePlan.CarePlanStatus.COMPLETED);

        mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(activePlan)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(completedPlan)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", PATIENT_ID.toString())
                .param("status", "active"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    void filterByIntentShouldReturnMatching() throws Exception {
        CarePlan planIntent = createValidCarePlan();
        CarePlan proposalIntent = createValidCarePlan();
        proposalIntent.setId(UUID.randomUUID().toString());
        proposalIntent.setIntent(CarePlan.CarePlanIntent.PROPOSAL);

        mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(planIntent)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(proposalIntent)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", PATIENT_ID.toString())
                .param("intent", "plan"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    void tenantIsolationShouldPreventCrossTenantAccess() throws Exception {
        CarePlan carePlan = createValidCarePlan();
        String carePlanJson = jsonParser.encodeResourceToString(carePlan);

        MvcResult createResult = mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(carePlanJson))
            .andExpect(status().isCreated())
            .andReturn();

        CarePlan created = jsonParser.parseResource(CarePlan.class, createResult.getResponse().getContentAsString());
        String carePlanId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/fhir/CarePlan/{id}", carePlanId)
                .header("X-Tenant-ID", "different-tenant"))
            .andExpect(status().isNotFound());
    }

    @Test
    void carePlanWithActivitiesShouldPersist() throws Exception {
        CarePlan carePlan = createValidCarePlan();
        carePlan.addActivity()
                .setDetail(new CarePlan.CarePlanActivityDetailComponent()
                        .setStatus(CarePlan.CarePlanActivityStatus.NOTSTARTED)
                        .setDescription("Monitor blood glucose levels daily"));
        carePlan.addActivity()
                .setDetail(new CarePlan.CarePlanActivityDetailComponent()
                        .setStatus(CarePlan.CarePlanActivityStatus.INPROGRESS)
                        .setDescription("Follow diabetic diet plan"));

        String carePlanJson = jsonParser.encodeResourceToString(carePlan);

        MvcResult result = mockMvc.perform(post("/fhir/CarePlan")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(carePlanJson))
            .andExpect(status().isCreated())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        CarePlan created = jsonParser.parseResource(CarePlan.class, responseJson);

        assertThat(created.getActivity()).hasSize(2);
    }

    private CarePlan createValidCarePlan() {
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        carePlan.setIntent(CarePlan.CarePlanIntent.PLAN);
        carePlan.setSubject(new Reference("Patient/" + PATIENT_ID));
        carePlan.setTitle("Diabetes Management Plan");
        carePlan.setDescription("Comprehensive care plan for type 2 diabetes management");
        carePlan.addCategory(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://hl7.org/fhir/us/core/CodeSystem/careplan-category")
                        .setCode("assess-plan")
                        .setDisplay("Assessment and Plan of Treatment")));
        carePlan.setPeriod(new Period()
                .setStart(new Date())
                .setEnd(Date.from(Instant.now().plusSeconds(365 * 24 * 60 * 60))));
        carePlan.setCreated(new Date());
        carePlan.setAuthor(new Reference("Practitioner/dr-smith").setDisplay("Dr. Smith"));
        return carePlan;
    }
}
