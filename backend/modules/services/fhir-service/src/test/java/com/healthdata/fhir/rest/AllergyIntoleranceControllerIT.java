package com.healthdata.fhir.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Reference;
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

import com.healthdata.fhir.persistence.AllergyIntoleranceEntity;
import com.healthdata.fhir.persistence.AllergyIntoleranceRepository;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

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
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class AllergyIntoleranceControllerIT {

    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";
    private static final String TENANT_ID = "tenant-test-1";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String BASE_URL = "/fhir/AllergyIntolerance";

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
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
    private AllergyIntoleranceRepository allergyIntoleranceRepository;

    @Autowired
    private PatientRepository patientRepository;

    private FhirContext fhirContext;
    private IParser jsonParser;

    @BeforeEach
    void setUp() {
        allergyIntoleranceRepository.deleteAll();
        patientRepository.deleteAll();

        // Create test patient
        PatientEntity patient = PatientEntity.builder()
                .id(PATIENT_ID)
                .tenantId(TENANT_ID)
                .resourceType("Patient")
                .resourceJson("{}")
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
    void postAllergyIntoleranceShouldReturn201() throws Exception {
        AllergyIntolerance allergyIntolerance = createValidAllergyIntolerance();
        String allergyJson = jsonParser.encodeResourceToString(allergyIntolerance);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user-1")
                        .contentType("application/fhir+json")
                        .content(allergyJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/fhir+json"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        AllergyIntolerance created = jsonParser.parseResource(AllergyIntolerance.class, responseJson);

        assertThat(created.hasId()).isTrue();
        assertThat(created.getCode().getCodingFirstRep().getCode()).isEqualTo("227037002");
        assertThat(created.getCriticality()).isEqualTo(AllergyIntolerance.AllergyIntoleranceCriticality.HIGH);
    }

    @Test
    void getAllergyIntoleranceShouldReturn200WhenExists() throws Exception {
        UUID allergyId = UUID.randomUUID();
        AllergyIntoleranceEntity entity = createAllergyEntity(allergyId, PATIENT_ID, "active");
        allergyIntoleranceRepository.save(entity);

        mockMvc.perform(get(BASE_URL + "/{id}", allergyId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"))
                .andExpect(jsonPath("$.id").value(allergyId.toString()))
                .andExpect(jsonPath("$.code.coding[0].code").value("227037002"));
    }

    @Test
    void getAllergyIntoleranceShouldReturn404WhenNotExists() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get(BASE_URL + "/{id}", nonExistentId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void putAllergyIntoleranceShouldReturn200() throws Exception {
        UUID allergyId = UUID.randomUUID();
        AllergyIntoleranceEntity entity = createAllergyEntity(allergyId, PATIENT_ID, "active");
        allergyIntoleranceRepository.save(entity);

        AllergyIntolerance updated = createValidAllergyIntolerance();
        updated.setId(allergyId.toString());
        updated.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.LOW);
        String updatedJson = jsonParser.encodeResourceToString(updated);

        mockMvc.perform(put(BASE_URL + "/{id}", allergyId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user-2")
                        .contentType("application/fhir+json")
                        .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"));
    }

    @Test
    void deleteAllergyIntoleranceShouldReturn204() throws Exception {
        UUID allergyId = UUID.randomUUID();
        AllergyIntoleranceEntity entity = createAllergyEntity(allergyId, PATIENT_ID, "active");
        allergyIntoleranceRepository.save(entity);

        mockMvc.perform(delete(BASE_URL + "/{id}", allergyId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user-3"))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        assertThat(allergyIntoleranceRepository.findById(allergyId)).isEmpty();
    }

    @Test
    void searchByPatientShouldReturnBundle() throws Exception {
        // Create multiple allergies for the patient
        allergyIntoleranceRepository.save(createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active"));
        allergyIntoleranceRepository.save(createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active"));
        allergyIntoleranceRepository.save(createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active"));

        MvcResult result = mockMvc.perform(get(BASE_URL)
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getTotal()).isEqualTo(3);
        assertThat(bundle.getEntry()).hasSize(3);
    }

    @Test
    void searchByPatientShouldReturn400WhenPatientMissing() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getActiveAllergiesShouldReturnOnlyActive() throws Exception {
        allergyIntoleranceRepository.save(createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active"));
        allergyIntoleranceRepository.save(createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active"));
        allergyIntoleranceRepository.save(createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "resolved"));

        MvcResult result = mockMvc.perform(get(BASE_URL + "/active")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getTotal()).isEqualTo(2);
    }

    @Test
    void getCriticalAllergiesShouldReturnHighCriticality() throws Exception {
        AllergyIntoleranceEntity critical1 = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        critical1.setCriticality("high");
        allergyIntoleranceRepository.save(critical1);

        AllergyIntoleranceEntity critical2 = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        critical2.setCriticality("high");
        allergyIntoleranceRepository.save(critical2);

        AllergyIntoleranceEntity lowCriticality = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        lowCriticality.setCriticality("low");
        allergyIntoleranceRepository.save(lowCriticality);

        MvcResult result = mockMvc.perform(get(BASE_URL + "/critical")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getTotal()).isEqualTo(2);
    }

    @Test
    void getMedicationAllergiesShouldReturnMedicationCategory() throws Exception {
        AllergyIntoleranceEntity medication1 = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        medication1.setCategory("medication");
        allergyIntoleranceRepository.save(medication1);

        AllergyIntoleranceEntity medication2 = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        medication2.setCategory("medication");
        allergyIntoleranceRepository.save(medication2);

        AllergyIntoleranceEntity food = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        food.setCategory("food");
        allergyIntoleranceRepository.save(food);

        MvcResult result = mockMvc.perform(get(BASE_URL + "/medication")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getTotal()).isEqualTo(2);
    }

    @Test
    void getFoodAllergiesShouldReturnFoodCategory() throws Exception {
        AllergyIntoleranceEntity food1 = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        food1.setCategory("food");
        allergyIntoleranceRepository.save(food1);

        AllergyIntoleranceEntity food2 = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        food2.setCategory("food");
        allergyIntoleranceRepository.save(food2);

        AllergyIntoleranceEntity medication = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        medication.setCategory("medication");
        allergyIntoleranceRepository.save(medication);

        MvcResult result = mockMvc.perform(get(BASE_URL + "/food")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getTotal()).isEqualTo(2);
    }

    @Test
    void getConfirmedAllergiesShouldReturnConfirmedOnly() throws Exception {
        AllergyIntoleranceEntity confirmed1 = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        confirmed1.setVerificationStatus("confirmed");
        allergyIntoleranceRepository.save(confirmed1);

        AllergyIntoleranceEntity confirmed2 = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        confirmed2.setVerificationStatus("confirmed");
        allergyIntoleranceRepository.save(confirmed2);

        AllergyIntoleranceEntity unconfirmed = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        unconfirmed.setVerificationStatus("unconfirmed");
        allergyIntoleranceRepository.save(unconfirmed);

        MvcResult result = mockMvc.perform(get(BASE_URL + "/confirmed")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/fhir+json"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Bundle bundle = jsonParser.parseResource(Bundle.class, responseJson);

        assertThat(bundle.getTotal()).isEqualTo(2);
    }

    @Test
    void hasActiveAllergyShouldReturnTrueWhenExists() throws Exception {
        String allergyCode = "227037002";
        AllergyIntoleranceEntity entity = createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active");
        entity.setCode(allergyCode);
        allergyIntoleranceRepository.save(entity);

        mockMvc.perform(get(BASE_URL + "/has-allergy")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("code", allergyCode))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasAllergy").value(true));
    }

    @Test
    void hasActiveAllergyShouldReturnFalseWhenNotExists() throws Exception {
        mockMvc.perform(get(BASE_URL + "/has-allergy")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString())
                        .param("code", "NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasAllergy").value(false));
    }

    @Test
    void countActiveAllergiesShouldReturnCorrectCount() throws Exception {
        allergyIntoleranceRepository.save(createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active"));
        allergyIntoleranceRepository.save(createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "active"));
        allergyIntoleranceRepository.save(createAllergyEntity(UUID.randomUUID(), PATIENT_ID, "resolved"));

        mockMvc.perform(get(BASE_URL + "/count")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", PATIENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void healthCheckShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/_health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // Helper methods
    private AllergyIntolerance createValidAllergyIntolerance() {
        AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
        allergyIntolerance.setId(UUID.randomUUID().toString());

        allergyIntolerance.setPatient(new Reference("Patient/" + PATIENT_ID));

        allergyIntolerance.setCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://snomed.info/sct")
                        .setCode("227037002")
                        .setDisplay("Fish (substance)")));

        allergyIntolerance.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                        .setCode("active")));

        allergyIntolerance.setVerificationStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                        .setCode("confirmed")));

        allergyIntolerance.addCategory(AllergyIntolerance.AllergyIntoleranceCategory.FOOD);
        allergyIntolerance.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.HIGH);
        allergyIntolerance.setType(AllergyIntolerance.AllergyIntoleranceType.ALLERGY);

        allergyIntolerance.setRecordedDateElement(new DateTimeType(new java.util.Date()));

        return allergyIntolerance;
    }

    private AllergyIntoleranceEntity createAllergyEntity(UUID id, UUID patientId, String clinicalStatus) {
        // Create a complete FHIR resource JSON that includes all fields
        String fhirResourceJson = String.format(
            "{\"resourceType\":\"AllergyIntolerance\",\"id\":\"%s\"," +
            "\"clinicalStatus\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical\",\"code\":\"%s\"}]}," +
            "\"verificationStatus\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/allergyintolerance-verification\",\"code\":\"confirmed\"}]}," +
            "\"type\":\"allergy\"," +
            "\"category\":[\"food\"]," +
            "\"criticality\":\"high\"," +
            "\"code\":{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"227037002\",\"display\":\"Fish (substance)\"}]}," +
            "\"patient\":{\"reference\":\"Patient/%s\"}}",
            id, clinicalStatus, patientId);

        return AllergyIntoleranceEntity.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .patientId(patientId)
                .code("227037002")
                .codeSystem("http://snomed.info/sct")
                .codeDisplay("Fish (substance)")
                .category("food")
                .clinicalStatus(clinicalStatus)
                .verificationStatus("confirmed")
                .criticality("high")
                .type("allergy")
                .recordedDate(LocalDateTime.now())
                .fhirResource(fhirResourceJson)
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .version(0)
                .build();
    }
}
