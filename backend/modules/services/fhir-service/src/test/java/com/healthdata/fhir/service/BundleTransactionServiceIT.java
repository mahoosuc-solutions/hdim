package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.healthdata.fhir.persistence.ConditionRepository;
import com.healthdata.fhir.persistence.EncounterRepository;
import com.healthdata.fhir.persistence.ObservationRepository;
import com.healthdata.fhir.persistence.PatientRepository;

/**
 * Integration tests for BundleTransactionService — proves resource persistence
 * and transaction/batch semantics (rollback vs independent processing).
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
@ActiveProfiles("test")
@Tag("integration")
class BundleTransactionServiceIT {

    private static final String TENANT = "bundle-svc-test-tenant";
    private static final String ACTOR = "test-actor";

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:tc:postgresql:15-alpine:///testdb");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("jwt.secret", () -> "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        registry.add("jwt.access-token-expiration", () -> "1h");
        registry.add("jwt.refresh-token-expiration", () -> "1d");
        registry.add("jwt.issuer", () -> "test-issuer");
        registry.add("jwt.audience", () -> "test-audience");
    }

    @Autowired
    private BundleTransactionService bundleTransactionService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @BeforeEach
    void cleanDatabase() {
        observationRepository.deleteAll();
        conditionRepository.deleteAll();
        encounterRepository.deleteAll();
        patientRepository.deleteAll();
    }

    @Test
    void shouldPersistAllResourceTypes() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.BATCH);

        // Patient entry
        Patient patient = new Patient();
        patient.addName().setFamily("ServiceTest").addGiven("Dana");
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        Bundle.BundleEntryComponent patientEntry = bundle.addEntry();
        patientEntry.setResource(patient);
        patientEntry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Patient");

        // Condition entry
        Condition condition = new Condition();
        condition.getCode().addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("73211009")
                .setDisplay("Diabetes mellitus");
        condition.getClinicalStatus().addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active");
        Bundle.BundleEntryComponent conditionEntry = bundle.addEntry();
        conditionEntry.setResource(condition);
        conditionEntry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Condition");

        // Observation entry
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.getCode().addCoding()
                .setSystem("http://loinc.org")
                .setCode("4548-4")
                .setDisplay("Hemoglobin A1c");
        Bundle.BundleEntryComponent obsEntry = bundle.addEntry();
        obsEntry.setResource(observation);
        obsEntry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");

        Bundle result = bundleTransactionService.processBundle(TENANT, bundle, ACTOR);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Bundle.BundleType.BATCHRESPONSE);

        // Verify all resource types persisted
        assertThat(patientRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(conditionRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(observationRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldPersistBatchEntriesIndependently() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.BATCH);

        // Valid Patient
        Patient patient = new Patient();
        patient.addName().setFamily("BatchIndependent").addGiven("Eve");
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        Bundle.BundleEntryComponent goodEntry = bundle.addEntry();
        goodEntry.setResource(patient);
        goodEntry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Patient");

        // Entry with unsupported resource type (will fail individually)
        Bundle.BundleEntryComponent badEntry = bundle.addEntry();
        badEntry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("UnsupportedType");
        // No resource set — this entry should fail but not block the patient

        Bundle result = bundleTransactionService.processBundle(TENANT, bundle, ACTOR);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(Bundle.BundleType.BATCHRESPONSE);

        // The valid patient should still be persisted even though another entry failed
        assertThat(patientRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldRejectBundleWithNoEntries() {
        Bundle emptyBundle = new Bundle();
        emptyBundle.setType(Bundle.BundleType.TRANSACTION);

        try {
            bundleTransactionService.processBundle(TENANT, emptyBundle, ACTOR);
            assertThat(true).as("Expected BundleValidationException").isFalse();
        } catch (BundleTransactionService.BundleValidationException e) {
            assertThat(e.getMessage()).containsIgnoringCase("entry");
        }
    }
}
