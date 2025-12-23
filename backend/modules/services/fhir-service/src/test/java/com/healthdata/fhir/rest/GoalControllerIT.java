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

import com.healthdata.fhir.persistence.GoalRepository;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Integration tests for GoalController.
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
class GoalControllerIT {

    private static final String TENANT_ID = "tenant-test-1";
    private static final UUID PATIENT_ID = UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    private static final String FHIR_JSON = "application/fhir+json";
    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GoalRepository goalRepository;

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
        goalRepository.deleteAll();
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
    void createGoalShouldReturn201() throws Exception {
        Goal goal = createValidGoal();
        String goalJson = jsonParser.encodeResourceToString(goal);

        MvcResult result = mockMvc.perform(post("/fhir/Goal")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(goalJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(FHIR_JSON))
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Goal created = jsonParser.parseResource(Goal.class, responseJson);

        assertThat(created.hasId()).isTrue();
        assertThat(created.getLifecycleStatus()).isEqualTo(Goal.GoalLifecycleStatus.ACTIVE);
    }

    @Test
    void getGoalShouldReturn200() throws Exception {
        Goal goal = createValidGoal();
        String goalJson = jsonParser.encodeResourceToString(goal);

        MvcResult createResult = mockMvc.perform(post("/fhir/Goal")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(goalJson))
            .andExpect(status().isCreated())
            .andReturn();

        Goal created = jsonParser.parseResource(Goal.class, createResult.getResponse().getContentAsString());
        String goalId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/fhir/Goal/{id}", goalId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(FHIR_JSON))
            .andExpect(jsonPath("$.id").value(goalId))
            .andExpect(jsonPath("$.lifecycleStatus").value("active"));
    }

    @Test
    void getGoalNotFoundShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/fhir/Goal/{id}", nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateGoalShouldReturn200() throws Exception {
        Goal goal = createValidGoal();
        String goalJson = jsonParser.encodeResourceToString(goal);

        MvcResult createResult = mockMvc.perform(post("/fhir/Goal")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(goalJson))
            .andExpect(status().isCreated())
            .andReturn();

        Goal created = jsonParser.parseResource(Goal.class, createResult.getResponse().getContentAsString());
        String goalId = created.getIdElement().getIdPart();

        created.setLifecycleStatus(Goal.GoalLifecycleStatus.COMPLETED);
        String updatedJson = jsonParser.encodeResourceToString(created);

        mockMvc.perform(put("/fhir/Goal/{id}", goalId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-2")
                .contentType(FHIR_JSON)
                .content(updatedJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lifecycleStatus").value("completed"));
    }

    @Test
    void deleteGoalShouldReturn204() throws Exception {
        Goal goal = createValidGoal();
        String goalJson = jsonParser.encodeResourceToString(goal);

        MvcResult createResult = mockMvc.perform(post("/fhir/Goal")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(goalJson))
            .andExpect(status().isCreated())
            .andReturn();

        Goal created = jsonParser.parseResource(Goal.class, createResult.getResponse().getContentAsString());
        String goalId = created.getIdElement().getIdPart();

        mockMvc.perform(delete("/fhir/Goal/{id}", goalId)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-3"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/fhir/Goal/{id}", goalId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void searchByPatientShouldReturnBundle() throws Exception {
        Goal goal1 = createValidGoal();
        Goal goal2 = createValidGoal();
        goal2.setId(UUID.randomUUID().toString());
        goal2.setDescription(new CodeableConcept().setText("Lower blood pressure"));

        mockMvc.perform(post("/fhir/Goal")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(goal1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/Goal")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(goal2)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/Goal")
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
    void filterByLifecycleStatusShouldReturnMatching() throws Exception {
        Goal activeGoal = createValidGoal();
        Goal completedGoal = createValidGoal();
        completedGoal.setId(UUID.randomUUID().toString());
        completedGoal.setLifecycleStatus(Goal.GoalLifecycleStatus.COMPLETED);

        mockMvc.perform(post("/fhir/Goal")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(activeGoal)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/fhir/Goal")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(jsonParser.encodeResourceToString(completedGoal)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/fhir/Goal")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", PATIENT_ID.toString())
                .param("lifecycle-status", "active"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    void tenantIsolationShouldPreventCrossTenantAccess() throws Exception {
        Goal goal = createValidGoal();
        String goalJson = jsonParser.encodeResourceToString(goal);

        MvcResult createResult = mockMvc.perform(post("/fhir/Goal")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", "user-1")
                .contentType(FHIR_JSON)
                .content(goalJson))
            .andExpect(status().isCreated())
            .andReturn();

        Goal created = jsonParser.parseResource(Goal.class, createResult.getResponse().getContentAsString());
        String goalId = created.getIdElement().getIdPart();

        mockMvc.perform(get("/fhir/Goal/{id}", goalId)
                .header("X-Tenant-ID", "different-tenant"))
            .andExpect(status().isNotFound());
    }

    private Goal createValidGoal() {
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID().toString());
        goal.setLifecycleStatus(Goal.GoalLifecycleStatus.ACTIVE);
        goal.setSubject(new Reference("Patient/" + PATIENT_ID));
        goal.setDescription(new CodeableConcept().setText("Reduce A1c to below 7%"));
        goal.setPriority(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/goal-priority")
                        .setCode("high-priority")
                        .setDisplay("High Priority")));
        goal.addCategory(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/goal-category")
                        .setCode("physiologic")
                        .setDisplay("Physiologic")));
        goal.setStart(new DateType(new Date()));
        goal.addTarget()
                .setDue(new DateType(Date.from(Instant.now().plusSeconds(90 * 24 * 60 * 60))));
        return goal;
    }
}
