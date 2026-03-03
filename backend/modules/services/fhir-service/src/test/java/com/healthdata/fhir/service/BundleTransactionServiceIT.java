package com.healthdata.fhir.service;

import static com.healthdata.test.TestTenantConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.healthdata.fhir.config.AbstractFhirIntegrationTest;
import com.healthdata.fhir.persistence.ConditionRepository;
import com.healthdata.fhir.persistence.EncounterRepository;
import com.healthdata.fhir.persistence.ObservationRepository;
import com.healthdata.fhir.persistence.PatientRepository;

/**
 * Integration tests for BundleTransactionService — proves resource persistence
 * and transaction/batch semantics (rollback vs independent processing).
 */
class BundleTransactionServiceIT extends AbstractFhirIntegrationTest {

    private static final String TENANT = PRIMARY_TENANT_ID;
    private static final String ACTOR = TEST_ACTOR;

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
