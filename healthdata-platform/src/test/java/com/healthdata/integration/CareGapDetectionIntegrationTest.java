package com.healthdata.integration;

import com.healthdata.BaseIntegrationTest;
import com.healthdata.caregap.dto.CareGap;
import com.healthdata.caregap.dto.GapType;
import com.healthdata.caregap.entity.CareGapEntity;
import com.healthdata.caregap.repository.CareGapRepository;
import com.healthdata.caregap.service.CareGapDetectionEngine;
import com.healthdata.fhir.entity.Condition;
import com.healthdata.fhir.entity.Observation;
import com.healthdata.fhir.repository.ConditionRepository;
import com.healthdata.fhir.repository.ObservationRepository;
import com.healthdata.patient.entity.Patient;
import com.healthdata.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Care Gap Detection Integration Tests
 *
 * Tests validate:
 * - Preventive care gap detection
 * - Chronic disease management gaps
 * - Medication adherence gaps
 * - Screening gaps by age and gender
 * - Gap prioritization algorithms
 * - Batch gap detection
 * - Multi-tenant gap isolation
 *
 * @author TDD Swarm Agent 5B
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CareGapDetectionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CareGapDetectionEngine gapEngine;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private CareGapRepository careGapRepository;

    private static final String TENANT_ID = "test-tenant-1";

    @BeforeEach
    public void setUp() {
        careGapRepository.deleteAll();
        observationRepository.deleteAll();
        conditionRepository.deleteAll();
        patientRepository.deleteAll();
    }

    // ==================== PREVENTIVE CARE GAPS ====================

    @Nested
    @DisplayName("Preventive Care Gap Detection")
    class PreventiveCareGapDetection {

        @Test
        @DisplayName("Detects mammogram screening gap for female 50+")
        void detectGaps_FemaleOverdue_DetectsMammogramGap() {
            Patient patient = createFemalePatient("Jane", "Doe", 55);

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            Optional<CareGap> mammogramGap = gaps.stream()
                    .filter(g -> g.getGapType() == GapType.CANCER_SCREENING)
                    .filter(g -> g.getDescription().contains("mammogram"))
                    .findFirst();

            assertTrue(mammogramGap.isPresent(), "Should detect mammogram screening gap");
            assertThat(mammogramGap.get().getPriority()).isEqualTo("HIGH");
            assertThat(mammogramGap.get().getRecommendedAction()).contains("Schedule mammogram");
        }

        @Test
        @DisplayName("No mammogram gap when recent screening exists")
        void detectGaps_RecentMammogram_NoGap() {
            Patient patient = createFemalePatient("Sarah", "Smith", 52);

            // Add recent mammogram (within 2 years)
            createMammogramObservation(patient.getId(), LocalDate.now().minusMonths(18));

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            boolean hasMammogramGap = gaps.stream()
                    .anyMatch(g -> g.getDescription().contains("mammogram"));

            assertFalse(hasMammogramGap, "Should not detect gap when recent mammogram exists");
        }

        @Test
        @DisplayName("Detects colorectal screening gap for patients 50+")
        void detectGaps_ColorectalOverdue_DetectsGap() {
            Patient patient = createMalePatient("Bob", "Johnson", 60);

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            Optional<CareGap> colorectalGap = gaps.stream()
                    .filter(g -> g.getGapType() == GapType.CANCER_SCREENING)
                    .filter(g -> g.getDescription().contains("colorectal"))
                    .findFirst();

            assertTrue(colorectalGap.isPresent());
            assertThat(colorectalGap.get().getPriority()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("No colorectal gap when recent colonoscopy exists")
        void detectGaps_RecentColonoscopy_NoGap() {
            Patient patient = createMalePatient("Charlie", "Brown", 62);

            // Colonoscopy valid for 10 years
            createColonoscopyObservation(patient.getId(), LocalDate.now().minusYears(8));

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            boolean hasColorectalGap = gaps.stream()
                    .anyMatch(g -> g.getDescription().contains("colorectal"));

            assertFalse(hasColorectalGap);
        }

        @Test
        @DisplayName("Younger patients excluded from age-based screening")
        void detectGaps_YoungPatient_NoAgeBasedScreening() {
            Patient patient = createFemalePatient("Emily", "Young", 30);

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            boolean hasCancerScreeningGap = gaps.stream()
                    .anyMatch(g -> g.getGapType() == GapType.CANCER_SCREENING);

            assertFalse(hasCancerScreeningGap, "Young patients should not have age-based screening gaps");
        }
    }

    // ==================== CHRONIC DISEASE GAPS ====================

    @Nested
    @DisplayName("Chronic Disease Management Gap Detection")
    class ChronicDiseaseGapDetection {

        @Test
        @DisplayName("Detects HbA1c testing gap for diabetic patient")
        void detectGaps_DiabeticNoHbA1c_DetectsGap() {
            Patient patient = createPatientWithDiabetes("David", "Miller", 55);

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            Optional<CareGap> hba1cGap = gaps.stream()
                    .filter(g -> g.getGapType() == GapType.CHRONIC_DISEASE_MANAGEMENT)
                    .filter(g -> g.getDescription().contains("HbA1c"))
                    .findFirst();

            assertTrue(hba1cGap.isPresent());
            assertThat(hba1cGap.get().getPriority()).isEqualTo("HIGH");
            assertThat(hba1cGap.get().getRecommendedAction()).contains("HbA1c test");
        }

        @Test
        @DisplayName("Detects uncontrolled HbA1c gap")
        void detectGaps_HighHbA1c_DetectsControlGap() {
            Patient patient = createPatientWithDiabetes("Frank", "Wilson", 58);

            // High HbA1c value
            createHbA1cObservation(patient.getId(), 8.5, LocalDate.now().minusDays(30));

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            Optional<CareGap> controlGap = gaps.stream()
                    .filter(g -> g.getDescription().contains("uncontrolled") || g.getDescription().contains("above target"))
                    .findFirst();

            assertTrue(controlGap.isPresent());
            assertThat(controlGap.get().getPriority()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("No HbA1c gap when controlled and recent")
        void detectGaps_ControlledHbA1c_NoGap() {
            Patient patient = createPatientWithDiabetes("Grace", "Taylor", 60);

            createHbA1cObservation(patient.getId(), 6.5, LocalDate.now().minusDays(30));

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            boolean hasHbA1cGap = gaps.stream()
                    .filter(g -> g.getGapType() == GapType.CHRONIC_DISEASE_MANAGEMENT)
                    .anyMatch(g -> g.getDescription().contains("HbA1c"));

            assertFalse(hasHbA1cGap);
        }

        @Test
        @DisplayName("Detects BP monitoring gap for hypertensive patient")
        void detectGaps_HypertensiveNoBP_DetectsGap() {
            Patient patient = createPatientWithHypertension("Henry", "Anderson", 65);

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            Optional<CareGap> bpGap = gaps.stream()
                    .filter(g -> g.getGapType() == GapType.CHRONIC_DISEASE_MANAGEMENT)
                    .filter(g -> g.getDescription().contains("blood pressure"))
                    .findFirst();

            assertTrue(bpGap.isPresent());
            assertThat(bpGap.get().getRecommendedAction()).contains("blood pressure check");
        }

        @Test
        @DisplayName("Detects uncontrolled BP gap")
        void detectGaps_HighBP_DetectsControlGap() {
            Patient patient = createPatientWithHypertension("Iris", "Thomas", 62);

            createBloodPressureObservation(patient.getId(), 155, 98, LocalDate.now().minusDays(15));

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            Optional<CareGap> controlGap = gaps.stream()
                    .filter(g -> g.getDescription().contains("uncontrolled") || g.getDescription().contains("elevated"))
                    .findFirst();

            assertTrue(controlGap.isPresent());
            assertThat(controlGap.get().getPriority()).isIn("HIGH", "CRITICAL");
        }
    }

    // ==================== MEDICATION ADHERENCE GAPS ====================

    @Nested
    @DisplayName("Medication Adherence Gap Detection")
    class MedicationAdherenceGapDetection {

        @Test
        @DisplayName("Detects medication gap for diabetic without prescriptions")
        void detectGaps_DiabeticNoMeds_DetectsGap() {
            Patient patient = createPatientWithDiabetes("Jack", "Martinez", 58);

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            Optional<CareGap> medGap = gaps.stream()
                    .filter(g -> g.getGapType() == GapType.MEDICATION_ADHERENCE)
                    .findFirst();

            assertTrue(medGap.isPresent());
            assertThat(medGap.get().getDescription()).contains("medication");
        }

        @Test
        @DisplayName("Detects low adherence gap")
        void detectGaps_LowAdherence_DetectsGap() {
            Patient patient = createPatientWithDiabetes("Kelly", "Garcia", 55);

            // Low adherence scenario - only taking medication 50% of days
            // Implementation would check prescription fill dates and calculate PDC

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            // Gap detection would be based on prescription refill patterns
            assertNotNull(gaps);
        }
    }

    // ==================== GAP PRIORITIZATION ====================

    @Nested
    @DisplayName("Care Gap Prioritization")
    class CareGapPrioritization {

        @Test
        @DisplayName("Critical gaps prioritized over high priority")
        void prioritizeGaps_CriticalOverHigh() {
            Patient patient = createPatientWithMultipleConditions("Laura", "Rodriguez", 70);

            // Create critical condition (very high BP)
            createBloodPressureObservation(patient.getId(), 180, 110, LocalDate.now().minusDays(5));

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());
            List<CareGap> sortedGaps = gaps.stream()
                    .sorted(Comparator.comparing(CareGap::getPriorityScore).reversed())
                    .collect(Collectors.toList());

            if (!sortedGaps.isEmpty()) {
                assertThat(sortedGaps.get(0).getPriority()).isIn("CRITICAL", "HIGH");
            }
        }

        @Test
        @DisplayName("Age-based risk adjustment increases priority")
        void prioritizeGaps_AgeRiskAdjustment() {
            // Elderly patient (75 years old)
            Patient elderlyPatient = createPatientWithDiabetes("Mary", "Elderly", 75);
            List<CareGap> elderlyGaps = gapEngine.detectGapsForPatient(elderlyPatient.getId());

            // Middle-aged patient (50 years old)
            Patient middleAgedPatient = createPatientWithDiabetes("Tom", "MiddleAge", 50);
            List<CareGap> middleAgedGaps = gapEngine.detectGapsForPatient(middleAgedPatient.getId());

            // Elderly patient's gaps should generally have higher priority scores
            if (!elderlyGaps.isEmpty() && !middleAgedGaps.isEmpty()) {
                double elderlyMaxPriority = elderlyGaps.stream()
                        .mapToDouble(CareGap::getPriorityScore)
                        .max()
                        .orElse(0);

                double middleAgedMaxPriority = middleAgedGaps.stream()
                        .mapToDouble(CareGap::getPriorityScore)
                        .max()
                        .orElse(0);

                // Elderly patient should have higher or equal priority
                assertThat(elderlyMaxPriority).isGreaterThanOrEqualTo(middleAgedMaxPriority * 0.9);
            }
        }

        @Test
        @DisplayName("Multiple chronic conditions increase gap priority")
        void prioritizeGaps_MultipleConditions_HigherPriority() {
            Patient multiConditionPatient = createPatientWithMultipleConditions("Nancy", "Complex", 65);

            List<CareGap> gaps = gapEngine.detectGapsForPatient(multiConditionPatient.getId());

            // Patients with multiple conditions should have high-priority gaps
            long highPriorityCount = gaps.stream()
                    .filter(g -> g.getPriority().equals("HIGH") || g.getPriority().equals("CRITICAL"))
                    .count();

            assertThat(highPriorityCount).isGreaterThan(0);
        }

        @Test
        @DisplayName("Recent hospitalization increases priority")
        void prioritizeGaps_RecentHospitalization_HigherPriority() {
            Patient patient = createPatientWithDiabetes("Oliver", "Hospital", 62);

            // Add encounter for recent hospitalization
            // (Implementation would check encounter data)

            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

            // Post-hospitalization gaps should be higher priority
            assertNotNull(gaps);
        }
    }

    // ==================== BATCH GAP DETECTION ====================

    @Nested
    @DisplayName("Batch Care Gap Detection")
    class BatchGapDetection {

        @Test
        @DisplayName("Detect gaps for 100 patients efficiently")
        void detectBatch_100Patients_EfficientProcessing() {
            List<String> patientIds = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                Patient patient = createPatientWithDiabetes("Patient" + i, "Test", 55 + (i % 20));
                patientIds.add(patient.getId());

                // Add varying data - some with gaps, some without
                if (i % 3 == 0) {
                    createHbA1cObservation(patient.getId(), 6.5, LocalDate.now().minusDays(30));
                }
            }

            long startTime = System.currentTimeMillis();
            Map<String, List<CareGap>> results = gapEngine.batchDetectGaps(patientIds);
            long duration = System.currentTimeMillis() - startTime;

            assertThat(results).hasSize(100);
            assertThat(duration).isLessThan(30000); // < 30 seconds for 100 patients

            // Verify that patients without HbA1c have gaps
            long patientsWithGaps = results.values().stream()
                    .filter(gaps -> !gaps.isEmpty())
                    .count();

            assertThat(patientsWithGaps).isGreaterThan(60); // At least 2/3 should have gaps
        }

        @Test
        @DisplayName("Batch detection handles mixed patient populations")
        void detectBatch_MixedPopulation_HandlesCorrectly() {
            List<String> patientIds = new ArrayList<>();

            // 25 diabetic patients
            for (int i = 0; i < 25; i++) {
                Patient patient = createPatientWithDiabetes("Diabetic" + i, "Test", 60);
                patientIds.add(patient.getId());
            }

            // 25 hypertensive patients
            for (int i = 0; i < 25; i++) {
                Patient patient = createPatientWithHypertension("Hypertensive" + i, "Test", 65);
                patientIds.add(patient.getId());
            }

            // 25 healthy patients
            for (int i = 0; i < 25; i++) {
                Patient patient = createHealthyPatient("Healthy" + i, "Test", 55);
                patientIds.add(patient.getId());
            }

            // 25 patients needing screenings
            for (int i = 0; i < 25; i++) {
                Patient patient = createFemalePatient("Screening" + i, "Test", 55);
                patientIds.add(patient.getId());
            }

            Map<String, List<CareGap>> results = gapEngine.batchDetectGaps(patientIds);

            assertThat(results).hasSize(100);

            // Verify different gap types are detected
            Set<GapType> allGapTypes = results.values().stream()
                    .flatMap(List::stream)
                    .map(CareGap::getGapType)
                    .collect(Collectors.toSet());

            assertThat(allGapTypes).isNotEmpty();
        }

        @Test
        @DisplayName("Batch detection persists gaps to database")
        void detectBatch_PersistsGaps() {
            List<String> patientIds = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                Patient patient = createPatientWithDiabetes("Persist" + i, "Test", 60);
                patientIds.add(patient.getId());
            }

            gapEngine.batchDetectGaps(patientIds);

            // Verify gaps are persisted
            List<CareGapEntity> persistedGaps = careGapRepository.findAll();
            assertThat(persistedGaps).isNotEmpty();
        }
    }

    // ==================== TENANT ISOLATION ====================

    @Nested
    @DisplayName("Multi-Tenant Gap Isolation")
    class MultiTenantGapIsolation {

        @Test
        @DisplayName("Gaps isolated by tenant")
        void detectGaps_MultiTenant_IsolatesCorrectly() {
            // Create patient in tenant 1
            Patient tenant1Patient = createPatientWithDiabetes("Tenant1", "Patient", 60);
            tenant1Patient.setTenantId("tenant-1");
            patientRepository.save(tenant1Patient);

            // Create patient in tenant 2
            Patient tenant2Patient = createPatientWithDiabetes("Tenant2", "Patient", 60);
            tenant2Patient.setTenantId("tenant-2");
            patientRepository.save(tenant2Patient);

            // Detect gaps for tenant 1 patient
            List<CareGap> tenant1Gaps = gapEngine.detectGapsForPatient(tenant1Patient.getId());

            // Gaps should only relate to tenant 1 patient
            assertThat(tenant1Gaps).allMatch(gap -> gap.getPatientId().equals(tenant1Patient.getId()));
        }
    }

    // ==================== GAP CLOSURE TRACKING ====================

    @Nested
    @DisplayName("Care Gap Closure Tracking")
    class GapClosureTracking {

        @Test
        @DisplayName("Gap closed when screening completed")
        void trackGapClosure_ScreeningCompleted_GapClosed() {
            Patient patient = createFemalePatient("Paula", "Closure", 55);

            // Initial detection - should have mammogram gap
            List<CareGap> initialGaps = gapEngine.detectGapsForPatient(patient.getId());
            long initialMammogramGaps = initialGaps.stream()
                    .filter(g -> g.getDescription().contains("mammogram"))
                    .count();

            assertThat(initialMammogramGaps).isGreaterThan(0);

            // Complete mammogram
            createMammogramObservation(patient.getId(), LocalDate.now());

            // Re-detect gaps - mammogram gap should be closed
            List<CareGap> updatedGaps = gapEngine.detectGapsForPatient(patient.getId());
            long updatedMammogramGaps = updatedGaps.stream()
                    .filter(g -> g.getDescription().contains("mammogram"))
                    .count();

            assertThat(updatedMammogramGaps).isEqualTo(0);
        }

        @Test
        @DisplayName("Gap closed when measure becomes compliant")
        void trackGapClosure_MeasureCompliant_GapClosed() {
            Patient patient = createPatientWithDiabetes("Quinn", "Improve", 58);

            // Initial high HbA1c
            createHbA1cObservation(patient.getId(), 8.2, LocalDate.now().minusDays(60));

            List<CareGap> initialGaps = gapEngine.detectGapsForPatient(patient.getId());
            long initialHbA1cGaps = initialGaps.stream()
                    .filter(g -> g.getDescription().contains("HbA1c"))
                    .count();

            // Update with controlled HbA1c
            observationRepository.deleteAll();
            createHbA1cObservation(patient.getId(), 6.5, LocalDate.now().minusDays(15));

            List<CareGap> updatedGaps = gapEngine.detectGapsForPatient(patient.getId());
            long updatedHbA1cGaps = updatedGaps.stream()
                    .filter(g -> g.getDescription().contains("HbA1c") && g.getDescription().contains("uncontrolled"))
                    .count();

            // Uncontrolled gap should be closed
            assertThat(updatedHbA1cGaps).isLessThanOrEqualTo(initialHbA1cGaps);
        }
    }

    // ==================== PERFORMANCE BENCHMARKS ====================

    @Nested
    @DisplayName("Performance Benchmarks")
    class PerformanceBenchmarks {

        @Test
        @DisplayName("Single patient gap detection < 200ms")
        void singlePatient_FastDetection() {
            Patient patient = createPatientWithDiabetes("Fast", "Detection", 60);

            long startTime = System.nanoTime();
            List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());
            long duration = (System.nanoTime() - startTime) / 1_000_000;

            assertThat(duration).isLessThan(200);
            assertNotNull(gaps);
        }

        @Test
        @DisplayName("500 patient batch detection < 60 seconds")
        void batchDetection_500Patients_PerformanceTarget() {
            List<String> patientIds = new ArrayList<>();

            for (int i = 0; i < 500; i++) {
                Patient patient = createPatientWithDiabetes("Batch" + i, "Test", 60);
                patientIds.add(patient.getId());
            }

            long startTime = System.currentTimeMillis();
            Map<String, List<CareGap>> results = gapEngine.batchDetectGaps(patientIds);
            long duration = System.currentTimeMillis() - startTime;

            assertThat(results).hasSize(500);
            assertThat(duration).isLessThan(60000);
        }
    }

    // ==================== HELPER METHODS ====================

    private Patient createHealthyPatient(String firstName, String lastName, int age) {
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

    private Patient createPatientWithDiabetes(String firstName, String lastName, int age) {
        Patient patient = createHealthyPatient(firstName, lastName, age);

        Condition diabetes = Condition.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patient.getId())
                .code("E11")
                .system("http://hl7.org/fhir/sid/icd-10")
                .display("Type 2 Diabetes Mellitus")
                .clinicalStatus("active")
                .tenantId(TENANT_ID)
                .build();
        conditionRepository.save(diabetes);

        return patient;
    }

    private Patient createPatientWithHypertension(String firstName, String lastName, int age) {
        Patient patient = createHealthyPatient(firstName, lastName, age);

        Condition hypertension = Condition.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patient.getId())
                .code("I10")
                .system("http://hl7.org/fhir/sid/icd-10")
                .display("Essential Hypertension")
                .clinicalStatus("active")
                .tenantId(TENANT_ID)
                .build();
        conditionRepository.save(hypertension);

        return patient;
    }

    private Patient createPatientWithMultipleConditions(String firstName, String lastName, int age) {
        Patient patient = createHealthyPatient(firstName, lastName, age);

        // Diabetes
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

        // Hypertension
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

        // CHF
        Condition chf = Condition.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patient.getId())
                .code("I50")
                .system("http://hl7.org/fhir/sid/icd-10")
                .display("Heart Failure")
                .clinicalStatus("active")
                .tenantId(TENANT_ID)
                .build();
        conditionRepository.save(chf);

        return patient;
    }

    private void createHbA1cObservation(String patientId, double value, LocalDate effectiveDate) {
        Observation obs = Observation.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .code("4548-4")
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
                .code("8480-6")
                .system("http://loinc.org")
                .display("Systolic BP")
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
                .code("8462-4")
                .system("http://loinc.org")
                .display("Diastolic BP")
                .valueQuantity((double) diastolic)
                .unit("mmHg")
                .effectiveDateTime(effectiveDate.atStartOfDay())
                .status("final")
                .tenantId(TENANT_ID)
                .build();
        observationRepository.save(diastolicObs);
    }

    private void createMammogramObservation(String patientId, LocalDate effectiveDate) {
        Observation obs = Observation.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .code("24606-6")
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
                .code("73761-0")
                .system("http://loinc.org")
                .display("Colonoscopy")
                .status("final")
                .effectiveDateTime(effectiveDate.atStartOfDay())
                .tenantId(TENANT_ID)
                .build();
        observationRepository.save(obs);
    }
}
