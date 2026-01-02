package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

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
@ActiveProfiles("test")
@Transactional
class AllergyIntoleranceRepositoryIT {

    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";
    private static final String TENANT_ID = "tenant-test-1";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        // Enable auto-creation of database schema in tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private AllergyIntoleranceRepository allergyIntoleranceRepository;

    @Autowired
    private PatientRepository patientRepository;

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
    }

    @Test
    void shouldPersistAndRetrieveAllergyIntolerance() {
        AllergyIntoleranceEntity entity = AllergyIntoleranceEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .code("227037002")
                .codeSystem("http://snomed.info/sct")
                .codeDisplay("Fish (substance)")
                .category("food")
                .clinicalStatus("active")
                .verificationStatus("confirmed")
                .criticality("high")
                .type("allergy")
                .recordedDate(LocalDateTime.now())
                .fhirResource("{}")
                .build();

        AllergyIntoleranceEntity saved = allergyIntoleranceRepository.save(entity);

        assertThat(saved.getVersion()).isEqualTo(0);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getLastModifiedAt()).isNotNull();

        AllergyIntoleranceEntity found = allergyIntoleranceRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getCode()).isEqualTo("227037002");
        assertThat(found.getCodeDisplay()).isEqualTo("Fish (substance)");
        assertThat(found.getCriticality()).isEqualTo("high");
    }

    @Test
    void shouldFindByPatientOrderedByRecordedDate() {
        // Create multiple allergies for the same patient
        allergyIntoleranceRepository.save(createAllergyEntity(PATIENT_ID, "Food", "food",
                LocalDateTime.now().minusDays(2)));
        allergyIntoleranceRepository.save(createAllergyEntity(PATIENT_ID, "Medication", "medication",
                LocalDateTime.now().minusDays(1)));
        allergyIntoleranceRepository.save(createAllergyEntity(PATIENT_ID, "Environment", "environment",
                LocalDateTime.now()));

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findByTenantIdAndPatientIdOrderByRecordedDateDesc(TENANT_ID, PATIENT_ID);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getCodeDisplay()).isEqualTo("Environment");
        assertThat(results.get(1).getCodeDisplay()).isEqualTo("Medication");
        assertThat(results.get(2).getCodeDisplay()).isEqualTo("Food");
    }

    @Test
    void shouldFindActiveAllergiesByPatient() {
        allergyIntoleranceRepository.save(createAllergyEntityWithStatus(PATIENT_ID, "Active Allergy 1", "active", "high"));
        allergyIntoleranceRepository.save(createAllergyEntityWithStatus(PATIENT_ID, "Active Allergy 2", "active", "low"));
        allergyIntoleranceRepository.save(createAllergyEntityWithStatus(PATIENT_ID, "Resolved Allergy", "resolved", "low"));
        allergyIntoleranceRepository.save(createAllergyEntityWithStatus(PATIENT_ID, "Inactive Allergy", "inactive", "low"));

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findActiveAllergiesByPatient(TENANT_ID, PATIENT_ID);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(a -> a.getClinicalStatus().equals("active"));
        // Should be ordered by criticality DESC then recordedDate DESC
        assertThat(results.get(0).getCriticality()).isEqualTo("high");
    }

    @Test
    void shouldFindCriticalAllergies() {
        allergyIntoleranceRepository.save(createAllergyEntityWithCriticality(PATIENT_ID, "Critical 1", "high", "active"));
        allergyIntoleranceRepository.save(createAllergyEntityWithCriticality(PATIENT_ID, "Critical 2", "high", "active"));
        allergyIntoleranceRepository.save(createAllergyEntityWithCriticality(PATIENT_ID, "Low priority", "low", "active"));
        allergyIntoleranceRepository.save(createAllergyEntityWithCriticality(PATIENT_ID, "Resolved critical", "high", "resolved"));

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findCriticalAllergies(TENANT_ID, PATIENT_ID);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(a -> a.getCriticality().equals("high"));
        assertThat(results).allMatch(a -> a.getClinicalStatus().equals("active"));
    }

    @Test
    void shouldFindFoodAllergies() {
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Peanuts", "food"));
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Shellfish", "food"));
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Penicillin", "medication"));

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findFoodAllergies(TENANT_ID, PATIENT_ID);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(a -> a.getCategory().equals("food"));
    }

    @Test
    void shouldFindMedicationAllergies() {
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Penicillin", "medication"));
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Sulfa", "medication"));
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Peanuts", "food"));

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findMedicationAllergies(TENANT_ID, PATIENT_ID);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(a -> a.getCategory().equals("medication"));
    }

    @Test
    void shouldFindByCategory() {
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Pollen", "environment"));
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Dust", "environment"));
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Peanuts", "food"));

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findByCategory(TENANT_ID, PATIENT_ID, "environment");

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(a -> a.getCategory().equals("environment"));
    }

    @Test
    void shouldFindConfirmedAllergies() {
        allergyIntoleranceRepository.save(createAllergyWithVerification(PATIENT_ID, "Confirmed 1", "confirmed", "high"));
        allergyIntoleranceRepository.save(createAllergyWithVerification(PATIENT_ID, "Confirmed 2", "confirmed", "low"));
        allergyIntoleranceRepository.save(createAllergyWithVerification(PATIENT_ID, "Unconfirmed", "unconfirmed", "high"));

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findConfirmedAllergies(TENANT_ID, PATIENT_ID);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(a -> a.getVerificationStatus().equals("confirmed"));
        assertThat(results).allMatch(a -> a.getClinicalStatus().equals("active"));
    }

    @Test
    void shouldCheckHasActiveAllergy() {
        String allergyCode = "227037002"; // Fish
        allergyIntoleranceRepository.save(createAllergyWithCode(PATIENT_ID, allergyCode, "active"));

        boolean hasAllergy = allergyIntoleranceRepository.hasActiveAllergy(TENANT_ID, PATIENT_ID, allergyCode);

        assertThat(hasAllergy).isTrue();
    }

    @Test
    void shouldReturnFalseWhenAllergyNotFound() {
        boolean hasAllergy = allergyIntoleranceRepository.hasActiveAllergy(TENANT_ID, PATIENT_ID, "NONEXISTENT");

        assertThat(hasAllergy).isFalse();
    }

    @Test
    void shouldReturnFalseWhenAllergyIsResolved() {
        String allergyCode = "227037002";
        allergyIntoleranceRepository.save(createAllergyWithCode(PATIENT_ID, allergyCode, "resolved"));

        boolean hasAllergy = allergyIntoleranceRepository.hasActiveAllergy(TENANT_ID, PATIENT_ID, allergyCode);

        assertThat(hasAllergy).isFalse();
    }

    @Test
    void shouldCountActiveAllergies() {
        allergyIntoleranceRepository.save(createAllergyEntityWithStatus(PATIENT_ID, "Active 1", "active", "low"));
        allergyIntoleranceRepository.save(createAllergyEntityWithStatus(PATIENT_ID, "Active 2", "active", "low"));
        allergyIntoleranceRepository.save(createAllergyEntityWithStatus(PATIENT_ID, "Resolved", "resolved", "low"));

        long count = allergyIntoleranceRepository.countActiveAllergies(TENANT_ID, PATIENT_ID);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCountByCriticality() {
        allergyIntoleranceRepository.save(createAllergyEntityWithCriticality(PATIENT_ID, "High 1", "high", "active"));
        allergyIntoleranceRepository.save(createAllergyEntityWithCriticality(PATIENT_ID, "High 2", "high", "active"));
        allergyIntoleranceRepository.save(createAllergyEntityWithCriticality(PATIENT_ID, "Low", "low", "active"));

        long count = allergyIntoleranceRepository.countByCriticality(TENANT_ID, PATIENT_ID, "high");

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCountByCategory() {
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Food 1", "food"));
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Food 2", "food"));
        allergyIntoleranceRepository.save(createAllergyByCategory(PATIENT_ID, "Med 1", "medication"));

        long count = allergyIntoleranceRepository.countByCategory(TENANT_ID, PATIENT_ID, "food");

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldFindAllergiesWithReactions() {
        AllergyIntoleranceEntity withReaction = createAllergyEntity(PATIENT_ID, "With Reaction", "food", LocalDateTime.now());
        withReaction.setHasReactions(true);
        withReaction.setReactionSeverity("severe");
        allergyIntoleranceRepository.save(withReaction);

        AllergyIntoleranceEntity withoutReaction = createAllergyEntity(PATIENT_ID, "Without Reaction", "food", LocalDateTime.now());
        withoutReaction.setHasReactions(false);
        allergyIntoleranceRepository.save(withoutReaction);

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findAllergiesWithReactions(TENANT_ID, PATIENT_ID);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getHasReactions()).isTrue();
    }

    @Test
    void shouldFindByReactionSeverity() {
        AllergyIntoleranceEntity severe = createAllergyEntity(PATIENT_ID, "Severe", "food", LocalDateTime.now());
        severe.setReactionSeverity("severe");
        severe.setClinicalStatus("active");
        allergyIntoleranceRepository.save(severe);

        AllergyIntoleranceEntity mild = createAllergyEntity(PATIENT_ID, "Mild", "food", LocalDateTime.now());
        mild.setReactionSeverity("mild");
        mild.setClinicalStatus("active");
        allergyIntoleranceRepository.save(mild);

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findByReactionSeverity(TENANT_ID, PATIENT_ID, "severe");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getReactionSeverity()).isEqualTo("severe");
    }

    @Test
    void shouldFindResolvedAllergies() {
        allergyIntoleranceRepository.save(createAllergyEntityWithStatus(PATIENT_ID, "Resolved 1", "resolved", "low"));
        allergyIntoleranceRepository.save(createAllergyEntityWithStatus(PATIENT_ID, "Resolved 2", "resolved", "low"));
        allergyIntoleranceRepository.save(createAllergyEntityWithStatus(PATIENT_ID, "Active", "active", "low"));

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findResolvedAllergies(TENANT_ID, PATIENT_ID);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(a -> a.getClinicalStatus().equals("resolved"));
    }

    @Test
    void shouldFindByType() {
        AllergyIntoleranceEntity allergy = createAllergyEntity(PATIENT_ID, "Allergy", "food", LocalDateTime.now());
        allergy.setType("allergy");
        allergy.setClinicalStatus("active");
        allergyIntoleranceRepository.save(allergy);

        AllergyIntoleranceEntity intolerance = createAllergyEntity(PATIENT_ID, "Intolerance", "food", LocalDateTime.now());
        intolerance.setType("intolerance");
        intolerance.setClinicalStatus("active");
        allergyIntoleranceRepository.save(intolerance);

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findByType(TENANT_ID, PATIENT_ID, "allergy");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo("allergy");
    }

    @Test
    void shouldFindByRecordedDateRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(5);
        LocalDateTime endDate = now.plusDays(1);

        allergyIntoleranceRepository.save(createAllergyEntity(PATIENT_ID, "Within Range 1", "food", now.minusDays(3)));
        allergyIntoleranceRepository.save(createAllergyEntity(PATIENT_ID, "Within Range 2", "food", now.minusDays(1)));
        allergyIntoleranceRepository.save(createAllergyEntity(PATIENT_ID, "Outside Range", "food", now.minusDays(10)));

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findByRecordedDateRange(TENANT_ID, PATIENT_ID, startDate, endDate);

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindByAsserter() {
        String asserterId = "practitioner-123";
        AllergyIntoleranceEntity entity = createAllergyEntity(PATIENT_ID, "Asserted", "food", LocalDateTime.now());
        entity.setAsserterId(asserterId);
        allergyIntoleranceRepository.save(entity);

        List<AllergyIntoleranceEntity> results = allergyIntoleranceRepository
                .findByAsserter(TENANT_ID, asserterId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAsserterId()).isEqualTo(asserterId);
    }

    // Helper methods
    private AllergyIntoleranceEntity createAllergyEntity(UUID patientId, String display,
                                                         String category, LocalDateTime recordedDate) {
        return AllergyIntoleranceEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientId)
                .code(UUID.randomUUID().toString())
                .codeSystem("http://snomed.info/sct")
                .codeDisplay(display)
                .category(category)
                .clinicalStatus("active")
                .verificationStatus("confirmed")
                .criticality("low")
                .recordedDate(recordedDate)
                .fhirResource("{}")
                .build();
    }

    private AllergyIntoleranceEntity createAllergyEntityWithStatus(UUID patientId, String display,
                                                                   String clinicalStatus, String criticality) {
        return AllergyIntoleranceEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientId)
                .code(UUID.randomUUID().toString())
                .codeSystem("http://snomed.info/sct")
                .codeDisplay(display)
                .category("food")
                .clinicalStatus(clinicalStatus)
                .verificationStatus("confirmed")
                .criticality(criticality)
                .recordedDate(LocalDateTime.now())
                .fhirResource("{}")
                .build();
    }

    private AllergyIntoleranceEntity createAllergyEntityWithCriticality(UUID patientId, String display,
                                                                        String criticality, String clinicalStatus) {
        return AllergyIntoleranceEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientId)
                .code(UUID.randomUUID().toString())
                .codeSystem("http://snomed.info/sct")
                .codeDisplay(display)
                .category("food")
                .clinicalStatus(clinicalStatus)
                .verificationStatus("confirmed")
                .criticality(criticality)
                .recordedDate(LocalDateTime.now())
                .fhirResource("{}")
                .build();
    }

    private AllergyIntoleranceEntity createAllergyByCategory(UUID patientId, String display, String category) {
        return AllergyIntoleranceEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientId)
                .code(UUID.randomUUID().toString())
                .codeSystem("http://snomed.info/sct")
                .codeDisplay(display)
                .category(category)
                .clinicalStatus("active")
                .verificationStatus("confirmed")
                .criticality("low")
                .recordedDate(LocalDateTime.now())
                .fhirResource("{}")
                .build();
    }

    private AllergyIntoleranceEntity createAllergyWithVerification(UUID patientId, String display,
                                                                   String verificationStatus, String criticality) {
        return AllergyIntoleranceEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientId)
                .code(UUID.randomUUID().toString())
                .codeSystem("http://snomed.info/sct")
                .codeDisplay(display)
                .category("food")
                .clinicalStatus("active")
                .verificationStatus(verificationStatus)
                .criticality(criticality)
                .recordedDate(LocalDateTime.now())
                .fhirResource("{}")
                .build();
    }

    private AllergyIntoleranceEntity createAllergyWithCode(UUID patientId, String code, String clinicalStatus) {
        return AllergyIntoleranceEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(patientId)
                .code(code)
                .codeSystem("http://snomed.info/sct")
                .codeDisplay("Test Allergen")
                .category("food")
                .clinicalStatus(clinicalStatus)
                .verificationStatus("confirmed")
                .criticality("low")
                .recordedDate(LocalDateTime.now())
                .fhirResource("{}")
                .build();
    }
}
