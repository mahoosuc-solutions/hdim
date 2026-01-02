package com.healthdata.integration;

import com.healthdata.BaseIntegrationTest;
import com.healthdata.fhir.dto.FhirValidationResult;
import com.healthdata.fhir.entity.*;
import com.healthdata.fhir.repository.*;
import com.healthdata.fhir.service.FhirIntegrationService;
import com.healthdata.fhir.service.FhirValidationService;
import com.healthdata.patient.entity.Patient;
import com.healthdata.patient.repository.PatientRepository;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * FHIR Resource Integration Tests
 *
 * Tests validate:
 * - FHIR resource validation
 * - Code system mapping (LOINC, SNOMED, ICD-10)
 * - Resource transformation
 * - Bundle processing
 * - Terminology validation
 *
 * @author TDD Swarm Agent 5B
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FhirResourceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FhirIntegrationService fhirService;

    @Autowired
    private FhirValidationService validationService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    private static final String TENANT_ID = "test-tenant-1";

    @BeforeEach
    public void setUp() {
        observationRepository.deleteAll();
        conditionRepository.deleteAll();
        patientRepository.deleteAll();
    }

    // ==================== FHIR VALIDATION ====================

    @Nested
    @DisplayName("FHIR Resource Validation")
    class FhirResourceValidation {

        @Test
        @DisplayName("Valid FHIR Observation passes validation")
        void validateObservation_ValidFhir_PassesValidation() {
            org.hl7.fhir.r4.model.Observation fhirObs = createValidFhirObservation();

            FhirValidationResult result = validationService.validateObservation(fhirObs);

            assertTrue(result.isValid(), "Valid observation should pass validation");
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Observation with invalid LOINC code fails validation")
        void validateObservation_InvalidLoincCode_FailsValidation() {
            org.hl7.fhir.r4.model.Observation obs = new org.hl7.fhir.r4.model.Observation();
            obs.setCode(new CodeableConcept().addCoding(
                    new Coding()
                            .setSystem("http://loinc.org")
                            .setCode("INVALID-CODE")
                            .setDisplay("Invalid Code")));
            obs.setStatus(org.hl7.fhir.r4.model.Observation.ObservationStatus.FINAL);
            obs.setSubject(new Reference("Patient/123"));

            FhirValidationResult result = validationService.validateObservation(obs);

            assertFalse(result.isValid());
            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Observation missing required fields fails validation")
        void validateObservation_MissingRequiredFields_FailsValidation() {
            org.hl7.fhir.r4.model.Observation obs = new org.hl7.fhir.r4.model.Observation();
            // Missing code, status, and subject

            FhirValidationResult result = validationService.validateObservation(obs);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().size() >= 3, "Should have errors for missing required fields");
        }

        @Test
        @DisplayName("Valid FHIR Condition passes validation")
        void validateCondition_ValidFhir_PassesValidation() {
            org.hl7.fhir.r4.model.Condition fhirCondition = createValidFhirCondition();

            FhirValidationResult result = validationService.validateCondition(fhirCondition);

            assertTrue(result.isValid());
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Condition with invalid ICD-10 code fails validation")
        void validateCondition_InvalidIcd10Code_FailsValidation() {
            org.hl7.fhir.r4.model.Condition condition = new org.hl7.fhir.r4.model.Condition();
            condition.setCode(new CodeableConcept().addCoding(
                    new Coding()
                            .setSystem("http://hl7.org/fhir/sid/icd-10")
                            .setCode("INVALID")
                            .setDisplay("Invalid ICD-10")));
            condition.setSubject(new Reference("Patient/123"));

            FhirValidationResult result = validationService.validateCondition(condition);

            assertFalse(result.isValid());
        }
    }

    // ==================== CODE SYSTEM MAPPING ====================

    @Nested
    @DisplayName("Code System Mapping")
    class CodeSystemMapping {

        @Test
        @DisplayName("Maps LOINC HbA1c code correctly")
        void mapLoincCode_HbA1c_ReturnsMapped() {
            String mapped = fhirService.mapLoincCode("4548-4");

            assertThat(mapped).isEqualToIgnoringCase("Hemoglobin A1c");
        }

        @Test
        @DisplayName("Maps LOINC Blood Pressure codes correctly")
        void mapLoincCode_BloodPressure_ReturnsMapped() {
            String systolicMapped = fhirService.mapLoincCode("8480-6");
            String diastolicMapped = fhirService.mapLoincCode("8462-4");

            assertThat(systolicMapped).containsIgnoringCase("Systolic");
            assertThat(diastolicMapped).containsIgnoringCase("Diastolic");
        }

        @Test
        @DisplayName("Maps SNOMED Diabetes code correctly")
        void mapSnomedCode_Diabetes_ReturnsMapped() {
            String mapped = fhirService.mapSnomedCode("44054006");

            assertThat(mapped).containsIgnoringCase("Diabetes");
        }

        @Test
        @DisplayName("Maps ICD-10 Diabetes code correctly")
        void mapIcd10Code_Diabetes_ReturnsMapped() {
            String mapped = fhirService.mapIcd10Code("E11");

            assertThat(mapped).containsIgnoringCase("Type 2 Diabetes");
        }

        @Test
        @DisplayName("Maps ICD-10 Hypertension code correctly")
        void mapIcd10Code_Hypertension_ReturnsMapped() {
            String mapped = fhirService.mapIcd10Code("I10");

            assertThat(mapped).containsIgnoringCase("Hypertension");
        }

        @Test
        @DisplayName("Unknown code returns null or default")
        void mapCode_Unknown_ReturnsNullOrDefault() {
            String mapped = fhirService.mapLoincCode("99999-9");

            // Should either return null or a default value
            assertTrue(mapped == null || mapped.equals("Unknown"));
        }
    }

    // ==================== RESOURCE TRANSFORMATION ====================

    @Nested
    @DisplayName("FHIR Resource Transformation")
    class ResourceTransformation {

        @Test
        @DisplayName("Transforms FHIR Observation to entity")
        void transformObservation_FhirToEntity_Successful() {
            org.hl7.fhir.r4.model.Observation fhirObs = createValidFhirObservation();
            fhirObs.setId("test-obs-1");

            com.healthdata.fhir.entity.Observation entity = fhirService.transformToEntity(fhirObs);

            assertNotNull(entity);
            assertThat(entity.getCode()).isEqualTo("4548-4");
            assertThat(entity.getValueQuantity()).isEqualTo(6.8);
            assertThat(entity.getUnit()).isEqualTo("%");
        }

        @Test
        @DisplayName("Transforms FHIR Condition to entity")
        void transformCondition_FhirToEntity_Successful() {
            org.hl7.fhir.r4.model.Condition fhirCondition = createValidFhirCondition();
            fhirCondition.setId("test-condition-1");

            com.healthdata.fhir.entity.Condition entity = fhirService.transformToEntity(fhirCondition);

            assertNotNull(entity);
            assertThat(entity.getCode()).isEqualTo("E11");
            assertThat(entity.getDisplay()).contains("Diabetes");
        }

        @Test
        @DisplayName("Transforms entity Observation to FHIR")
        void transformObservation_EntityToFhir_Successful() {
            com.healthdata.fhir.entity.Observation entity = com.healthdata.fhir.entity.Observation.builder()
                    .id(UUID.randomUUID().toString())
                    .patientId("patient-123")
                    .code("4548-4")
                    .system("http://loinc.org")
                    .display("Hemoglobin A1c")
                    .valueQuantity(6.8)
                    .unit("%")
                    .status("final")
                    .effectiveDateTime(LocalDateTime.now())
                    .tenantId(TENANT_ID)
                    .build();

            org.hl7.fhir.r4.model.Observation fhirObs = fhirService.transformToFhir(entity);

            assertNotNull(fhirObs);
            assertThat(fhirObs.getCode().getCodingFirstRep().getCode()).isEqualTo("4548-4");
            assertThat(fhirObs.getValueQuantity().getValue().doubleValue()).isEqualTo(6.8);
        }
    }

    // ==================== BUNDLE PROCESSING ====================

    @Nested
    @DisplayName("FHIR Bundle Processing")
    class BundleProcessing {

        @Test
        @DisplayName("Processes bundle with multiple resources")
        void processBundle_MultipleResources_AllProcessed() {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.TRANSACTION);

            // Add Patient
            org.hl7.fhir.r4.model.Patient fhirPatient = createValidFhirPatient();
            bundle.addEntry()
                    .setResource(fhirPatient)
                    .getRequest()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setUrl("Patient");

            // Add Observation
            org.hl7.fhir.r4.model.Observation fhirObs = createValidFhirObservation();
            bundle.addEntry()
                    .setResource(fhirObs)
                    .getRequest()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setUrl("Observation");

            // Add Condition
            org.hl7.fhir.r4.model.Condition fhirCondition = createValidFhirCondition();
            bundle.addEntry()
                    .setResource(fhirCondition)
                    .getRequest()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setUrl("Condition");

            Bundle resultBundle = fhirService.processBundle(bundle);

            assertNotNull(resultBundle);
            assertThat(resultBundle.getEntry()).hasSize(3);
        }

        @Test
        @DisplayName("Bundle processing handles errors gracefully")
        void processBundle_WithErrors_HandlesGracefully() {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.TRANSACTION);

            // Add invalid observation
            org.hl7.fhir.r4.model.Observation invalidObs = new org.hl7.fhir.r4.model.Observation();
            // Missing required fields
            bundle.addEntry()
                    .setResource(invalidObs)
                    .getRequest()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setUrl("Observation");

            assertDoesNotThrow(() -> fhirService.processBundle(bundle));
        }
    }

    // ==================== TERMINOLOGY VALIDATION ====================

    @Nested
    @DisplayName("Terminology Validation")
    class TerminologyValidation {

        @Test
        @DisplayName("Validates LOINC codes against CodeSystem")
        void validateTerminology_ValidLoincCode_Passes() {
            boolean isValid = validationService.isValidLoincCode("4548-4");

            assertTrue(isValid, "4548-4 is a valid LOINC code for HbA1c");
        }

        @Test
        @DisplayName("Rejects invalid LOINC codes")
        void validateTerminology_InvalidLoincCode_Fails() {
            boolean isValid = validationService.isValidLoincCode("99999-9");

            assertFalse(isValid);
        }

        @Test
        @DisplayName("Validates SNOMED CT codes")
        void validateTerminology_ValidSnomedCode_Passes() {
            boolean isValid = validationService.isValidSnomedCode("44054006");

            assertTrue(isValid, "44054006 is valid SNOMED code for Diabetes");
        }

        @Test
        @DisplayName("Validates ICD-10 codes")
        void validateTerminology_ValidIcd10Code_Passes() {
            boolean isValid = validationService.isValidIcd10Code("E11");

            assertTrue(isValid, "E11 is valid ICD-10 code for Type 2 Diabetes");
        }
    }

    // ==================== PERFORMANCE TESTS ====================

    @Nested
    @DisplayName("FHIR Processing Performance")
    class PerformanceTests {

        @Test
        @DisplayName("Transforms 100 observations efficiently")
        void transform_100Observations_EfficientProcessing() {
            List<org.hl7.fhir.r4.model.Observation> fhirObservations = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                fhirObservations.add(createValidFhirObservation());
            }

            long startTime = System.currentTimeMillis();
            List<com.healthdata.fhir.entity.Observation> entities = fhirObservations.stream()
                    .map(fhirService::transformToEntity)
                    .toList();
            long duration = System.currentTimeMillis() - startTime;

            assertThat(entities).hasSize(100);
            assertThat(duration).isLessThan(1000); // < 1 second for 100 transformations
        }

        @Test
        @DisplayName("Validates 100 resources efficiently")
        void validate_100Resources_EfficientProcessing() {
            List<org.hl7.fhir.r4.model.Observation> observations = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                observations.add(createValidFhirObservation());
            }

            long startTime = System.currentTimeMillis();
            List<FhirValidationResult> results = observations.stream()
                    .map(validationService::validateObservation)
                    .toList();
            long duration = System.currentTimeMillis() - startTime;

            assertThat(results).hasSize(100);
            assertThat(duration).isLessThan(2000); // < 2 seconds for 100 validations
        }
    }

    // ==================== HELPER METHODS ====================

    private org.hl7.fhir.r4.model.Observation createValidFhirObservation() {
        org.hl7.fhir.r4.model.Observation obs = new org.hl7.fhir.r4.model.Observation();
        obs.setStatus(org.hl7.fhir.r4.model.Observation.ObservationStatus.FINAL);

        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setSystem("http://loinc.org")
                .setCode("4548-4")
                .setDisplay("Hemoglobin A1c");
        obs.setCode(code);

        Quantity value = new Quantity();
        value.setValue(6.8);
        value.setUnit("%");
        value.setSystem("http://unitsofmeasure.org");
        value.setCode("%");
        obs.setValue(value);

        obs.setSubject(new Reference("Patient/test-patient-1"));
        obs.setEffective(new DateTimeType(new Date()));

        return obs;
    }

    private org.hl7.fhir.r4.model.Condition createValidFhirCondition() {
        org.hl7.fhir.r4.model.Condition condition = new org.hl7.fhir.r4.model.Condition();

        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("E11")
                .setDisplay("Type 2 Diabetes Mellitus");
        condition.setCode(code);

        condition.setSubject(new Reference("Patient/test-patient-1"));

        CodeableConcept clinicalStatus = new CodeableConcept();
        clinicalStatus.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active");
        condition.setClinicalStatus(clinicalStatus);

        return condition;
    }

    private org.hl7.fhir.r4.model.Patient createValidFhirPatient() {
        org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();

        patient.addIdentifier()
                .setSystem("http://hospital.example.org/mrn")
                .setValue("MRN-123456");

        patient.addName()
                .setFamily("Doe")
                .addGiven("John");

        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setBirthDate(Date.from(LocalDate.of(1980, 1, 15)
                .atStartOfDay()
                .toInstant(java.time.ZoneOffset.UTC)));

        return patient;
    }
}
