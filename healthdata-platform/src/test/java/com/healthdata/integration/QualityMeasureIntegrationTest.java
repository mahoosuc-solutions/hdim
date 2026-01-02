package com.healthdata.integration;

import com.healthdata.BaseIntegrationTest;
import com.healthdata.fhir.entity.Observation;
import com.healthdata.fhir.entity.Condition;
import com.healthdata.fhir.entity.MedicationRequest;
import com.healthdata.fhir.repository.ObservationRepository;
import com.healthdata.fhir.repository.ConditionRepository;
import com.healthdata.fhir.repository.MedicationRequestRepository;
import com.healthdata.patient.entity.Patient;
import com.healthdata.patient.repository.PatientRepository;
import com.healthdata.quality.dto.MeasureResult;
import com.healthdata.quality.dto.QualityMeasureCalculationRequest;
import com.healthdata.quality.entity.QualityMeasureResult;
import com.healthdata.quality.repository.QualityMeasureResultRepository;
import com.healthdata.quality.service.QualityMeasureCalculationService;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Quality Measure Integration Tests
 *
 * Tests validate:
 * - HbA1c Control measure calculation
 * - Blood Pressure Control measure
 * - Medication Adherence tracking
 * - Preventive Screening measures
 * - Batch calculation performance
 * - Clinical accuracy validation
 * - Multi-patient scenarios
 *
 * @author TDD Swarm Agent 5B
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class QualityMeasureIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private QualityMeasureCalculationService measureService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private MedicationRequestRepository medicationRepository;

    @Autowired
    private QualityMeasureResultRepository resultRepository;

    private static final String TENANT_ID = "test-tenant-1";
    private static final String MEASURE_HBA1C = "hba1c-control";
    private static final String MEASURE_BP = "bp-control";
    private static final String MEASURE_MEDICATION = "medication-adherence";
    private static final String MEASURE_BREAST_CANCER = "breast-cancer-screening";
    private static final String MEASURE_COLORECTAL = "colorectal-cancer-screening";

    @BeforeEach
    public void setUp() {
        resultRepository.deleteAll();
        observationRepository.deleteAll();
        conditionRepository.deleteAll();
        medicationRepository.deleteAll();
        patientRepository.deleteAll();
    }

    // ==================== HBA1C CONTROL MEASURE ====================

    @Nested
    @DisplayName("HbA1c Control Measure Calculation")
    class HbA1cControlMeasure {

        @Test
        @DisplayName("Compliant patient with HbA1c < 7.0% passes measure")
        void calculateHbA1c_CompliantPatient_ReturnsPass() {
            // Create patient with diabetes
            Patient patient = createPatientWithDiabetes("John", "Doe");

            // Add compliant HbA1c observation (6.8%)
            createHbA1cObservation(patient.getId(), 6.8, LocalDate.now().minusDays(30));

            // Calculate measure
            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);

            assertNotNull(result);
            assertTrue(result.isCompliant(), "Patient should be compliant with HbA1c < 7.0%");
            assertThat(result.getValue()).isEqualTo(6.8);
            assertThat(result.getStatus()).isEqualTo("COMPLIANT");
            assertThat(result.getMeasureName()).isEqualTo("HbA1c Control");
        }

        @Test
        @DisplayName("Non-compliant patient with HbA1c > 7.0% fails measure")
        void calculateHbA1c_NonCompliantPatient_ReturnsFail() {
            Patient patient = createPatientWithDiabetes("Jane", "Smith");

            // Add non-compliant HbA1c observation (7.5%)
            createHbA1cObservation(patient.getId(), 7.5, LocalDate.now().minusDays(30));

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);

            assertFalse(result.isCompliant(), "Patient should be non-compliant with HbA1c > 7.0%");
            assertThat(result.getValue()).isEqualTo(7.5);
            assertThat(result.getStatus()).isEqualTo("NON_COMPLIANT");
            assertThat(result.getGapDescription()).contains("HbA1c above target");
        }

        @Test
        @DisplayName("Uses most recent HbA1c value when multiple exist")
        void calculateHbA1c_MultipleValues_UsesMostRecent() {
            Patient patient = createPatientWithDiabetes("Bob", "Johnson");

            // Add multiple HbA1c observations
            createHbA1cObservation(patient.getId(), 8.0, LocalDate.now().minusDays(90));
            createHbA1cObservation(patient.getId(), 7.2, LocalDate.now().minusDays(60));
            createHbA1cObservation(patient.getId(), 6.5, LocalDate.now().minusDays(30)); // Most recent

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);

            assertTrue(result.isCompliant());
            assertThat(result.getValue()).isEqualTo(6.5); // Should use most recent
        }

        @Test
        @DisplayName("Patient without diabetes excluded from measure")
        void calculateHbA1c_NoDiabetes_ExcludedFromMeasure() {
            Patient patient = createTestPatient("Alice", "Williams");

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);

            assertThat(result.getStatus()).isEqualTo("NOT_APPLICABLE");
            assertThat(result.getReason()).contains("Patient does not have diabetes");
        }

        @Test
        @DisplayName("Missing HbA1c observation creates care gap")
        void calculateHbA1c_MissingObservation_CreatesCareGap() {
            Patient patient = createPatientWithDiabetes("Charlie", "Brown");

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);

            assertFalse(result.isCompliant());
            assertThat(result.getStatus()).isEqualTo("CARE_GAP");
            assertThat(result.getGapDescription()).contains("No recent HbA1c measurement");
        }

        @Test
        @DisplayName("Old HbA1c observation triggers care gap")
        void calculateHbA1c_OldObservation_CreatesCareGap() {
            Patient patient = createPatientWithDiabetes("David", "Miller");

            // Add HbA1c observation from 13 months ago (outside 12-month window)
            createHbA1cObservation(patient.getId(), 6.5, LocalDate.now().minusMonths(13));

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);

            assertFalse(result.isCompliant());
            assertThat(result.getStatus()).isEqualTo("CARE_GAP");
            assertThat(result.getGapDescription()).contains("HbA1c measurement is outdated");
        }
    }

    // ==================== BLOOD PRESSURE CONTROL ====================

    @Nested
    @DisplayName("Blood Pressure Control Measure")
    class BloodPressureControlMeasure {

        @Test
        @DisplayName("Compliant BP (< 140/90) passes measure")
        void calculateBP_CompliantPatient_ReturnsPass() {
            Patient patient = createPatientWithHypertension("Emma", "Davis");

            createBloodPressureObservation(patient.getId(), 130, 80, LocalDate.now().minusDays(30));

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_BP);

            assertTrue(result.isCompliant());
            assertThat(result.getValue()).isEqualTo(130.0); // Systolic
            assertThat(result.getStatus()).isEqualTo("COMPLIANT");
        }

        @Test
        @DisplayName("High systolic BP (>= 140) fails measure")
        void calculateBP_HighSystolic_ReturnsFail() {
            Patient patient = createPatientWithHypertension("Frank", "Wilson");

            createBloodPressureObservation(patient.getId(), 145, 85, LocalDate.now().minusDays(30));

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_BP);

            assertFalse(result.isCompliant());
            assertThat(result.getGapDescription()).contains("Blood pressure above target");
        }

        @Test
        @DisplayName("High diastolic BP (>= 90) fails measure")
        void calculateBP_HighDiastolic_ReturnsFail() {
            Patient patient = createPatientWithHypertension("Grace", "Taylor");

            createBloodPressureObservation(patient.getId(), 135, 95, LocalDate.now().minusDays(30));

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_BP);

            assertFalse(result.isCompliant());
            assertThat(result.getValue()).isGreaterThan(90.0);
        }

        @Test
        @DisplayName("Uses most recent BP reading from multiple measurements")
        void calculateBP_MultipleReadings_UsesMostRecent() {
            Patient patient = createPatientWithHypertension("Henry", "Anderson");

            createBloodPressureObservation(patient.getId(), 150, 95, LocalDate.now().minusDays(90));
            createBloodPressureObservation(patient.getId(), 145, 90, LocalDate.now().minusDays(60));
            createBloodPressureObservation(patient.getId(), 135, 82, LocalDate.now().minusDays(30));

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_BP);

            assertTrue(result.isCompliant());
            assertThat(result.getValue()).isEqualTo(135.0);
        }

        @Test
        @DisplayName("Missing BP creates care gap")
        void calculateBP_MissingObservation_CreatesCareGap() {
            Patient patient = createPatientWithHypertension("Iris", "Thomas");

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_BP);

            assertThat(result.getStatus()).isEqualTo("CARE_GAP");
            assertThat(result.getGapDescription()).contains("No recent blood pressure measurement");
        }
    }

    // ==================== MEDICATION ADHERENCE ====================

    @Nested
    @DisplayName("Medication Adherence Measure")
    class MedicationAdherenceMeasure {

        @Test
        @DisplayName("High adherence (>= 80%) passes measure")
        void calculateMedication_HighAdherence_ReturnsPass() {
            Patient patient = createPatientWithDiabetes("Jack", "Martinez");

            createMedicationRequest(patient.getId(), "Metformin", 90, 30, LocalDate.now().minusDays(30));

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_MEDICATION);

            assertTrue(result.isCompliant());
            assertThat(result.getValue()).isGreaterThanOrEqualTo(80.0);
        }

        @Test
        @DisplayName("Low adherence (< 80%) fails measure")
        void calculateMedication_LowAdherence_ReturnsFail() {
            Patient patient = createPatientWithDiabetes("Kelly", "Garcia");

            // 15 days of medication taken out of 30 days = 50% adherence
            createMedicationRequest(patient.getId(), "Metformin", 15, 30, LocalDate.now().minusDays(30));

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_MEDICATION);

            assertFalse(result.isCompliant());
            assertThat(result.getValue()).isLessThan(80.0);
            assertThat(result.getGapDescription()).contains("medication adherence below target");
        }

        @Test
        @DisplayName("No active medications returns not applicable")
        void calculateMedication_NoMedications_NotApplicable() {
            Patient patient = createTestPatient("Laura", "Rodriguez");

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_MEDICATION);

            assertThat(result.getStatus()).isEqualTo("NOT_APPLICABLE");
        }
    }

    // ==================== PREVENTIVE SCREENING ====================

    @Nested
    @DisplayName("Preventive Screening Measures")
    class PreventiveScreeningMeasures {

        @Test
        @DisplayName("Female 50+ with recent mammogram passes breast cancer screening")
        void calculateBreastCancer_RecentMammogram_ReturnsPass() {
            Patient patient = createFemalePatient("Mary", "Lewis", 55);

            createMammogramObservation(patient.getId(), LocalDate.now().minusMonths(18));

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_BREAST_CANCER);

            assertTrue(result.isCompliant());
            assertThat(result.getStatus()).isEqualTo("COMPLIANT");
        }

        @Test
        @DisplayName("Female 50+ without recent mammogram fails screening")
        void calculateBreastCancer_NoMammogram_ReturnsFail() {
            Patient patient = createFemalePatient("Nancy", "Walker", 58);

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_BREAST_CANCER);

            assertFalse(result.isCompliant());
            assertThat(result.getStatus()).isEqualTo("CARE_GAP");
            assertThat(result.getGapDescription()).contains("mammogram screening");
        }

        @Test
        @DisplayName("Male patient excluded from breast cancer screening")
        void calculateBreastCancer_MalePatient_NotApplicable() {
            Patient patient = createMalePatient("Oliver", "Hall", 55);

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_BREAST_CANCER);

            assertThat(result.getStatus()).isEqualTo("NOT_APPLICABLE");
        }

        @Test
        @DisplayName("Patient 50+ with recent colonoscopy passes colorectal screening")
        void calculateColorectal_RecentColonoscopy_ReturnsPass() {
            Patient patient = createMalePatient("Paul", "Allen", 60);

            createColonoscopyObservation(patient.getId(), LocalDate.now().minusYears(5));

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_COLORECTAL);

            assertTrue(result.isCompliant());
        }

        @Test
        @DisplayName("Patient 50+ without screening fails colorectal measure")
        void calculateColorectal_NoScreening_ReturnsFail() {
            Patient patient = createFemalePatient("Quinn", "Young", 62);

            MeasureResult result = measureService.calculateMeasure(patient.getId(), MEASURE_COLORECTAL);

            assertFalse(result.isCompliant());
            assertThat(result.getGapDescription()).contains("colorectal cancer screening");
        }
    }

    // ==================== BATCH CALCULATION ====================

    @Nested
    @DisplayName("Batch Measure Calculation")
    class BatchMeasureCalculation {

        @Test
        @DisplayName("Batch calculate HbA1c for 100 patients completes successfully")
        void batchCalculate_100Patients_CompletesSuccessfully() {
            List<String> patientIds = new ArrayList<>();

            // Create 100 patients with varying HbA1c levels
            for (int i = 0; i < 100; i++) {
                Patient patient = createPatientWithDiabetes("Patient" + i, "Test");
                patientIds.add(patient.getId());

                // Alternate between compliant and non-compliant
                double hba1cValue = (i % 2 == 0) ? 6.5 : 7.5;
                createHbA1cObservation(patient.getId(), hba1cValue, LocalDate.now().minusDays(30));
            }

            // Batch calculate
            long startTime = System.currentTimeMillis();
            List<MeasureResult> results = measureService.batchCalculate(patientIds, MEASURE_HBA1C);
            long duration = System.currentTimeMillis() - startTime;

            assertThat(results).hasSize(100);
            assertThat(duration).isLessThan(10000); // Should complete in < 10 seconds

            // Verify compliance distribution (50% compliant, 50% non-compliant)
            long compliantCount = results.stream().filter(MeasureResult::isCompliant).count();
            assertThat(compliantCount).isEqualTo(50);
        }

        @Test
        @DisplayName("Batch calculate handles mixed measure eligibility")
        void batchCalculate_MixedEligibility_HandlesCorrectly() {
            List<String> patientIds = new ArrayList<>();

            // 50 patients with diabetes
            for (int i = 0; i < 50; i++) {
                Patient patient = createPatientWithDiabetes("Diabetic" + i, "Test");
                patientIds.add(patient.getId());
                createHbA1cObservation(patient.getId(), 6.5, LocalDate.now().minusDays(30));
            }

            // 50 patients without diabetes
            for (int i = 0; i < 50; i++) {
                Patient patient = createTestPatient("NonDiabetic" + i, "Test");
                patientIds.add(patient.getId());
            }

            List<MeasureResult> results = measureService.batchCalculate(patientIds, MEASURE_HBA1C);

            assertThat(results).hasSize(100);

            long applicableCount = results.stream()
                    .filter(r -> !r.getStatus().equals("NOT_APPLICABLE"))
                    .count();
            assertThat(applicableCount).isEqualTo(50);
        }

        @Test
        @DisplayName("Batch calculation persists results to database")
        void batchCalculate_PersistsResults() {
            List<String> patientIds = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                Patient patient = createPatientWithDiabetes("Patient" + i, "Test");
                patientIds.add(patient.getId());
                createHbA1cObservation(patient.getId(), 6.5, LocalDate.now().minusDays(30));
            }

            measureService.batchCalculate(patientIds, MEASURE_HBA1C);

            // Verify results are persisted
            List<QualityMeasureResult> persistedResults = resultRepository.findAll();
            assertThat(persistedResults).hasSize(10);
            assertThat(persistedResults).allMatch(r -> r.getMeasureName().equals("HbA1c Control"));
        }
    }

    // ==================== MEASURE ACCURACY ====================

    @Nested
    @DisplayName("Measure Accuracy Validation")
    class MeasureAccuracyValidation {

        @Test
        @DisplayName("All 5 HEDIS measures calculate without errors")
        void allMeasures_CalculateWithoutErrors() {
            Patient patient = createCompletePatient("Complete", "Patient", 55);

            // Add comprehensive observation data
            createHbA1cObservation(patient.getId(), 6.8, LocalDate.now().minusDays(30));
            createBloodPressureObservation(patient.getId(), 130, 80, LocalDate.now().minusDays(30));
            createMedicationRequest(patient.getId(), "Metformin", 90, 30, LocalDate.now().minusDays(30));
            createMammogramObservation(patient.getId(), LocalDate.now().minusMonths(18));
            createColonoscopyObservation(patient.getId(), LocalDate.now().minusYears(5));

            List<String> measures = Arrays.asList(
                    MEASURE_HBA1C,
                    MEASURE_BP,
                    MEASURE_MEDICATION,
                    MEASURE_BREAST_CANCER,
                    MEASURE_COLORECTAL
            );

            for (String measure : measures) {
                assertDoesNotThrow(() -> {
                    MeasureResult result = measureService.calculateMeasure(patient.getId(), measure);
                    assertNotNull(result, "Result should not be null for measure: " + measure);
                });
            }
        }

        @Test
        @DisplayName("Measure calculation is idempotent")
        void measureCalculation_Idempotent() {
            Patient patient = createPatientWithDiabetes("Idempotent", "Test");
            createHbA1cObservation(patient.getId(), 6.5, LocalDate.now().minusDays(30));

            MeasureResult result1 = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);
            MeasureResult result2 = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);

            assertThat(result1.isCompliant()).isEqualTo(result2.isCompliant());
            assertThat(result1.getValue()).isEqualTo(result2.getValue());
            assertThat(result1.getStatus()).isEqualTo(result2.getStatus());
        }

        @Test
        @DisplayName("Clinical thresholds are accurate")
        void clinicalThresholds_Accurate() {
            Patient patient = createPatientWithDiabetes("Threshold", "Test");

            // Test exact threshold value (7.0%)
            createHbA1cObservation(patient.getId(), 7.0, LocalDate.now().minusDays(30));
            MeasureResult resultAt = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);
            assertFalse(resultAt.isCompliant(), "HbA1c = 7.0% should be non-compliant");

            // Test just below threshold (6.9%)
            observationRepository.deleteAll();
            createHbA1cObservation(patient.getId(), 6.9, LocalDate.now().minusDays(30));
            MeasureResult resultBelow = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);
            assertTrue(resultBelow.isCompliant(), "HbA1c = 6.9% should be compliant");

            // Test just above threshold (7.1%)
            observationRepository.deleteAll();
            createHbA1cObservation(patient.getId(), 7.1, LocalDate.now().minusDays(30));
            MeasureResult resultAbove = measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);
            assertFalse(resultAbove.isCompliant(), "HbA1c = 7.1% should be non-compliant");
        }
    }

    // ==================== PERFORMANCE BENCHMARKS ====================

    @Nested
    @DisplayName("Performance Benchmarks")
    class PerformanceBenchmarks {

        @Test
        @DisplayName("Single patient calculation completes in < 100ms")
        void singlePatient_FastCalculation() {
            Patient patient = createPatientWithDiabetes("Fast", "Test");
            createHbA1cObservation(patient.getId(), 6.5, LocalDate.now().minusDays(30));

            long startTime = System.nanoTime();
            measureService.calculateMeasure(patient.getId(), MEASURE_HBA1C);
            long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds

            assertThat(duration).isLessThan(100);
        }

        @Test
        @DisplayName("Batch of 500 patients completes in < 60 seconds")
        void batchCalculation_500Patients_PerformanceTarget() {
            List<String> patientIds = new ArrayList<>();

            for (int i = 0; i < 500; i++) {
                Patient patient = createPatientWithDiabetes("Batch" + i, "Test");
                patientIds.add(patient.getId());
                createHbA1cObservation(patient.getId(), 6.5 + (i % 3), LocalDate.now().minusDays(30));
            }

            long startTime = System.currentTimeMillis();
            List<MeasureResult> results = measureService.batchCalculate(patientIds, MEASURE_HBA1C);
            long duration = System.currentTimeMillis() - startTime;

            assertThat(results).hasSize(500);
            assertThat(duration).isLessThan(60000); // < 60 seconds
        }
    }

    // ==================== HELPER METHODS ====================

    private Patient createTestPatient(String firstName, String lastName) {
        Patient patient = Patient.builder()
                .id(UUID.randomUUID().toString())
                .firstName(firstName)
                .lastName(lastName)
                .mrn("MRN-" + UUID.randomUUID())
                .dateOfBirth(LocalDate.of(1970, 1, 1))
                .gender("male")
                .tenantId(TENANT_ID)
                .build();
        return patientRepository.save(patient);
    }

    private Patient createPatientWithDiabetes(String firstName, String lastName) {
        Patient patient = createTestPatient(firstName, lastName);

        Condition diabetes = Condition.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patient.getId())
                .code("E11") // Type 2 Diabetes ICD-10
                .system("http://hl7.org/fhir/sid/icd-10")
                .display("Type 2 Diabetes Mellitus")
                .clinicalStatus("active")
                .tenantId(TENANT_ID)
                .build();
        conditionRepository.save(diabetes);

        return patient;
    }

    private Patient createPatientWithHypertension(String firstName, String lastName) {
        Patient patient = createTestPatient(firstName, lastName);

        Condition hypertension = Condition.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patient.getId())
                .code("I10") // Hypertension ICD-10
                .system("http://hl7.org/fhir/sid/icd-10")
                .display("Essential Hypertension")
                .clinicalStatus("active")
                .tenantId(TENANT_ID)
                .build();
        conditionRepository.save(hypertension);

        return patient;
    }

    private Patient createFemalePatient(String firstName, String lastName, int age) {
        Patient patient = Patient.builder()
                .id(UUID.randomUUID().toString())
                .firstName(firstName)
                .lastName(lastName)
                .mrn("MRN-" + UUID.randomUUID())
                .dateOfBirth(LocalDate.now().minusYears(age))
                .gender("female")
                .tenantId(TENANT_ID)
                .build();
        return patientRepository.save(patient);
    }

    private Patient createMalePatient(String firstName, String lastName, int age) {
        Patient patient = Patient.builder()
                .id(UUID.randomUUID().toString())
                .firstName(firstName)
                .lastName(lastName)
                .mrn("MRN-" + UUID.randomUUID())
                .dateOfBirth(LocalDate.now().minusYears(age))
                .gender("male")
                .tenantId(TENANT_ID)
                .build();
        return patientRepository.save(patient);
    }

    private Patient createCompletePatient(String firstName, String lastName, int age) {
        Patient patient = createFemalePatient(firstName, lastName, age);

        // Add diabetes and hypertension
        Condition diabetes = Condition.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patient.getId())
                .code("E11")
                .system("http://hl7.org/fhir/sid/icd-10")
                .display("Type 2 Diabetes")
                .clinicalStatus("active")
                .tenantId(TENANT_ID)
                .build();
        conditionRepository.save(diabetes);

        Condition hypertension = Condition.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patient.getId())
                .code("I10")
                .system("http://hl7.org/fhir/sid/icd-10")
                .display("Hypertension")
                .clinicalStatus("active")
                .tenantId(TENANT_ID)
                .build();
        conditionRepository.save(hypertension);

        return patient;
    }

    private void createHbA1cObservation(String patientId, double value, LocalDate effectiveDate) {
        Observation obs = Observation.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .code("4548-4") // HbA1c LOINC code
                .system("http://loinc.org")
                .display("Hemoglobin A1c")
                .valueQuantity(value)
                .unit("%")
                .effectiveDateTime(effectiveDate.atStartOfDay())
                .status("final")
                .tenantId(TENANT_ID)
                .build();
        observationRepository.save(obs);
    }

    private void createBloodPressureObservation(String patientId, int systolic, int diastolic, LocalDate effectiveDate) {
        Observation systolicObs = Observation.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .code("8480-6") // Systolic BP LOINC
                .system("http://loinc.org")
                .display("Systolic Blood Pressure")
                .valueQuantity((double) systolic)
                .unit("mmHg")
                .effectiveDateTime(effectiveDate.atStartOfDay())
                .status("final")
                .tenantId(TENANT_ID)
                .build();
        observationRepository.save(systolicObs);

        Observation diastolicObs = Observation.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .code("8462-4") // Diastolic BP LOINC
                .system("http://loinc.org")
                .display("Diastolic Blood Pressure")
                .valueQuantity((double) diastolic)
                .unit("mmHg")
                .effectiveDateTime(effectiveDate.atStartOfDay())
                .status("final")
                .tenantId(TENANT_ID)
                .build();
        observationRepository.save(diastolicObs);
    }

    private void createMedicationRequest(String patientId, String medicationName, int daysTaken, int daysSupplied, LocalDate startDate) {
        MedicationRequest medReq = MedicationRequest.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .medicationCode("METFORMIN")
                .medicationDisplay(medicationName)
                .status("active")
                .authoredOn(startDate.atStartOfDay())
                .dosageInstruction("Take once daily")
                .dispenseRequestValidityPeriodStart(startDate.atStartOfDay())
                .dispenseRequestValidityPeriodEnd(startDate.plusDays(daysSupplied).atStartOfDay())
                .dispenseRequestQuantity((double) daysSupplied)
                .tenantId(TENANT_ID)
                .build();
        medicationRepository.save(medReq);
    }

    private void createMammogramObservation(String patientId, LocalDate effectiveDate) {
        Observation obs = Observation.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .code("24606-6") // Mammography LOINC
                .system("http://loinc.org")
                .display("Mammogram")
                .status("final")
                .effectiveDateTime(effectiveDate.atStartOfDay())
                .tenantId(TENANT_ID)
                .build();
        observationRepository.save(obs);
    }

    private void createColonoscopyObservation(String patientId, LocalDate effectiveDate) {
        Observation obs = Observation.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .code("73761-0") // Colonoscopy LOINC
                .system("http://loinc.org")
                .display("Colonoscopy")
                .status("final")
                .effectiveDateTime(effectiveDate.atStartOfDay())
                .tenantId(TENANT_ID)
                .build();
        observationRepository.save(obs);
    }
}
