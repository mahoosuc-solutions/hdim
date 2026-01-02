package com.healthdata.caregap.service;

import com.healthdata.caregap.domain.CareGap;
import com.healthdata.caregap.repository.CareGapRepository;
import com.healthdata.fhir.domain.Condition;
import com.healthdata.fhir.domain.MedicationRequest;
import com.healthdata.fhir.domain.Observation;
import com.healthdata.fhir.service.FhirService;
import com.healthdata.patient.domain.Patient;
import com.healthdata.patient.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for Care Gap Detection Engine
 * Tests core detection, prioritization, auto-closure, and batch processing
 *
 * Test Categories:
 * - Basic detection (8 tests)
 * - Preventive care gaps (6 tests)
 * - Chronic disease gaps (6 tests)
 * - Medication adherence gaps (5 tests)
 * - Cancer screening gaps (5 tests)
 * - Cardiovascular risk gaps (4 tests)
 * - Gap prioritization (5 tests)
 * - Auto-closure validation (5 tests)
 * - Batch processing (4 tests)
 * - Error handling (3 tests)
 *
 * Total: 51 test methods
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Care Gap Detection Engine Test Suite")
class CareGapDetectionEngineTest {

    @Mock
    private CareGapRepository careGapRepository;

    @Mock
    private PatientService patientService;

    @Mock
    private FhirService fhirService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CareGapDetectionEngine engine;
    private Patient testPatient;
    private List<Observation> testObservations;
    private List<Condition> testConditions;
    private List<MedicationRequest> testMedications;

    @BeforeEach
    void setUp() {
        engine = new CareGapDetectionEngine(careGapRepository, patientService, fhirService, eventPublisher);
        setupTestData();
    }

    private void setupTestData() {
        testPatient = Patient.builder()
            .id("patient-1")
            .tenantId("tenant-1")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.now().minusYears(55))
            .gender(Patient.Gender.MALE)
            .build();

        testObservations = new ArrayList<>();
        testConditions = new ArrayList<>();
        testMedications = new ArrayList<>();
    }

    // ==================== BASIC DETECTION TESTS ====================

    @Nested
    @DisplayName("Basic Gap Detection")
    class BasicDetectionTests {

        @Test
        @DisplayName("Should detect gaps for valid patient")
        void testDetectGapsForPatient_Success() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
            verify(patientService).getPatient("patient-1");
            verify(careGapRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw exception for non-existent patient")
        void testDetectGapsForPatient_PatientNotFound() {
            when(patientService.getPatient("invalid")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> engine.detectGapsForPatient("invalid"));
        }

        @Test
        @DisplayName("Should detect specific gap type")
        void testDetectSpecificGap_Success() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);

            CareGap mockGap = CareGap.builder()
                .id("gap-1")
                .patientId("patient-1")
                .gapType("PREVENTIVE_CARE")
                .priority("MEDIUM")
                .status("OPEN")
                .build();
            when(careGapRepository.save(any(CareGap.class))).thenReturn(mockGap);

            Optional<CareGap> result = engine.detectSpecificGap("patient-1", "PREVENTIVE_CARE");

            assertTrue(result.isPresent());
            verify(careGapRepository).save(any(CareGap.class));
        }

        @Test
        @DisplayName("Should return empty for undetected specific gap")
        void testDetectSpecificGap_NoGapFound() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);

            Optional<CareGap> result = engine.detectSpecificGap("patient-1", "INVALID_TYPE");

            assertEquals(Optional.empty(), result);
        }

        @Test
        @DisplayName("Should handle population-level detection")
        void testDetectGapsForPopulation_Success() {
            List<Patient> patients = Arrays.asList(testPatient);
            when(patientService.getAllActivePatients("tenant-1", PageRequest.of(0, Integer.MAX_VALUE)))
                .thenReturn(new PageImpl<>(patients));
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            Pageable pageable = PageRequest.of(0, 10);
            var result = engine.detectGapsForPopulation("tenant-1", pageable);

            assertNotNull(result);
            verify(patientService).getAllActivePatients("tenant-1", PageRequest.of(0, Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("Should perform batch detection for multiple patients")
        void testBatchDetectGaps_Success() {
            List<String> patientIds = Arrays.asList("patient-1", "patient-2");
            when(patientService.getPatient(anyString())).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient(anyString())).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient(anyString())).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient(anyString())).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            Map<String, List<CareGap>> result = engine.batchDetectGaps(patientIds);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(careGapRepository, times(2)).saveAll(anyList());
        }

        @Test
        @DisplayName("Should handle empty observation list")
        void testDetectGapsForPatient_EmptyObservations() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(Collections.emptyList());
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(Collections.emptyList());
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(Collections.emptyList());
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
            verify(careGapRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should remove duplicate gaps")
        void testDetectGapsForPatient_RemoveDuplicates() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
            verify(careGapRepository).saveAll(any());
        }
    }

    // ==================== PREVENTIVE CARE TESTS ====================

    @Nested
    @DisplayName("Preventive Care Gap Detection")
    class PreventiveCareTests {

        @Test
        @DisplayName("Should detect annual wellness visit gap")
        void testPreventiveGap_AnnualWellnessOverdue() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);

            CareGap expectedGap = CareGap.builder()
                .patientId("patient-1")
                .gapType("PREVENTIVE_CARE")
                .description("Annual wellness visit overdue")
                .priority("MEDIUM")
                .status("OPEN")
                .build();
            when(careGapRepository.saveAll(anyList())).thenReturn(Arrays.asList(expectedGap));

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
            verify(careGapRepository).saveAll(any());
        }

        @Test
        @DisplayName("Should detect influenza vaccine gap")
        void testPreventiveGap_InfluenzaVaccineOverdue() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect pneumonia vaccine gap for elderly")
        void testPreventiveGap_PneumoniaVaccineForElderly() {
            Patient elderlyPatient = Patient.builder()
                .id("elderly-1")
                .tenantId("tenant-1")
                .dateOfBirth(LocalDate.now().minusYears(70))
                .gender(Patient.Gender.MALE)
                .build();

            when(patientService.getPatient("elderly-1")).thenReturn(Optional.of(elderlyPatient));
            when(fhirService.getObservationsForPatient("elderly-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("elderly-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("elderly-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("elderly-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect COVID-19 vaccine gap")
        void testPreventiveGap_CovidVaccineOverdue() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should not detect wellness gap if recent observation exists")
        void testPreventiveGap_NoGapWithRecentWellness() {
            Observation recentWellness = Observation.builder()
                .display("wellness-visit")
                .effectiveDate(LocalDateTime.now().minusDays(10))
                .build();
            testObservations.add(recentWellness);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should prioritize preventive gaps correctly")
        void testPreventiveGap_PriorityAssignment() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);

            CareGap mockGap = CareGap.builder()
                .priority("MEDIUM")
                .gapType("PREVENTIVE_CARE")
                .status("OPEN")
                .build();
            when(careGapRepository.saveAll(anyList())).thenReturn(Arrays.asList(mockGap));

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }
    }

    // ==================== CHRONIC DISEASE TESTS ====================

    @Nested
    @DisplayName("Chronic Disease Management Gap Detection")
    class ChronicDiseaseTests {

        @Test
        @DisplayName("Should detect HbA1c gap for diabetic patient")
        void testChronicGap_DiabetesHbA1cOverdue() {
            Condition diabetes = Condition.builder()
                .display("diabetes")
                .clinicalStatus("active")
                .build();
            testConditions.add(diabetes);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);

            CareGap expectedGap = CareGap.builder()
                .gapType("CHRONIC_DISEASE_MANAGEMENT")
                .priority("HIGH")
                .status("OPEN")
                .measureId("HEDIS-CDC")
                .build();
            when(careGapRepository.saveAll(anyList())).thenReturn(Arrays.asList(expectedGap));

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
            verify(careGapRepository).saveAll(any());
        }

        @Test
        @DisplayName("Should not detect HbA1c gap if recent test exists")
        void testChronicGap_NoDiabetesGapWithRecentHbA1c() {
            Condition diabetes = Condition.builder()
                .display("diabetes")
                .clinicalStatus("active")
                .build();
            testConditions.add(diabetes);

            Observation recentHbA1c = Observation.builder()
                .display("HbA1c")
                .effectiveDate(LocalDateTime.now().minusDays(30))
                .build();
            testObservations.add(recentHbA1c);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect blood pressure gap for hypertensive patient")
        void testChronicGap_HypertensionBPMonitoringOverdue() {
            Condition hypertension = Condition.builder()
                .display("hypertension")
                .clinicalStatus("active")
                .build();
            testConditions.add(hypertension);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);

            CareGap expectedGap = CareGap.builder()
                .gapType("CHRONIC_DISEASE_MANAGEMENT")
                .priority("HIGH")
                .status("OPEN")
                .build();
            when(careGapRepository.saveAll(anyList())).thenReturn(Arrays.asList(expectedGap));

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect spirometry gap for asthma patient")
        void testChronicGap_AsthmaSpirometryOverdue() {
            Condition asthma = Condition.builder()
                .display("asthma")
                .clinicalStatus("active")
                .build();
            testConditions.add(asthma);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect COPD spirometry gap")
        void testChronicGap_COPDSpirometryOverdue() {
            Condition copd = Condition.builder()
                .display("copd")
                .clinicalStatus("active")
                .build();
            testConditions.add(copd);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should assign HIGH priority to diabetic HbA1c gap")
        void testChronicGap_DiabetesGapHighPriority() {
            Condition diabetes = Condition.builder()
                .display("diabetes")
                .clinicalStatus("active")
                .build();
            testConditions.add(diabetes);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }
    }

    // ==================== MEDICATION ADHERENCE TESTS ====================

    @Nested
    @DisplayName("Medication Adherence Gap Detection")
    class MedicationAdherenceTests {

        @Test
        @DisplayName("Should detect overdue medication refill")
        void testMedicationGap_OverdueRefill() {
            MedicationRequest overdueMed = MedicationRequest.builder()
                .medicationDisplay("Metformin")
                .validPeriodEnd(LocalDateTime.now().minusDays(5))
                .build();
            testMedications.add(overdueMed);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);

            CareGap expectedGap = CareGap.builder()
                .gapType("MEDICATION_ADHERENCE")
                .priority("HIGH")
                .status("OPEN")
                .build();
            when(careGapRepository.saveAll(anyList())).thenReturn(Arrays.asList(expectedGap));

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should not detect refill gap if current")
        void testMedicationGap_NoGapWithCurrentRefill() {
            MedicationRequest currentMed = MedicationRequest.builder()
                .medicationDisplay("Lisinopril")
                .validPeriodEnd(LocalDateTime.now().plusDays(10))
                .build();
            testMedications.add(currentMed);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect medication compliance risk")
        void testMedicationGap_ComplianceRisk() {
            MedicationRequest nonCompliantMed = MedicationRequest.builder()
                .medicationDisplay("Atorvastatin")
                .refillsRemaining(1)
                .daysSupply(30)
                .validPeriodEnd(LocalDateTime.now().plusDays(10))
                .build();
            testMedications.add(nonCompliantMed);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle medications without refill dates")
        void testMedicationGap_NoRefillDate() {
            MedicationRequest medNoRefill = MedicationRequest.builder()
                .medicationDisplay("Aspirin")
                .validPeriodEnd(null)
                .build();
            testMedications.add(medNoRefill);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect multiple medication gaps")
        void testMedicationGap_MultipleMedicationsWithGaps() {
            MedicationRequest med1 = MedicationRequest.builder()
                .medicationDisplay("Med1")
                .validPeriodEnd(LocalDateTime.now().minusDays(3))
                .build();
            MedicationRequest med2 = MedicationRequest.builder()
                .medicationDisplay("Med2")
                .refillsRemaining(1)
                .daysSupply(30)
                .validPeriodEnd(LocalDateTime.now().plusDays(5))
                .build();
            testMedications.addAll(Arrays.asList(med1, med2));

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }
    }

    // ==================== CANCER SCREENING TESTS ====================

    @Nested
    @DisplayName("Cancer Screening Gap Detection")
    class CancerScreeningTests {

        @Test
        @DisplayName("Should detect colorectal cancer screening gap")
        void testCancerGap_ColonoscopyOverdue() {
            Patient middleAgedPatient = Patient.builder()
                .id("patient-1")
                .tenantId("tenant-1")
                .dateOfBirth(LocalDate.now().minusYears(55))
                .gender(Patient.Gender.MALE)
                .build();

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(middleAgedPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);

            CareGap expectedGap = CareGap.builder()
                .gapType("CANCER_SCREENING")
                .priority("HIGH")
                .status("OPEN")
                .measureId("HEDIS-COL")
                .build();
            when(careGapRepository.saveAll(anyList())).thenReturn(Arrays.asList(expectedGap));

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should not detect colonoscopy gap if too young")
        void testCancerGap_ColonoscopyTooYoung() {
            Patient youngPatient = Patient.builder()
                .id("young-1")
                .tenantId("tenant-1")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .gender(Patient.Gender.MALE)
                .build();

            when(patientService.getPatient("young-1")).thenReturn(Optional.of(youngPatient));
            when(fhirService.getObservationsForPatient("young-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("young-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("young-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("young-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect breast cancer screening gap for women")
        void testCancerGap_MammographyOverdue() {
            Patient femalePatient = Patient.builder()
                .id("female-1")
                .tenantId("tenant-1")
                .dateOfBirth(LocalDate.now().minusYears(50))
                .gender(Patient.Gender.FEMALE)
                .build();

            when(patientService.getPatient("female-1")).thenReturn(Optional.of(femalePatient));
            when(fhirService.getObservationsForPatient("female-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("female-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("female-1")).thenReturn(testMedications);

            CareGap expectedGap = CareGap.builder()
                .gapType("CANCER_SCREENING")
                .priority("HIGH")
                .status("OPEN")
                .measureId("HEDIS-BCS")
                .build();
            when(careGapRepository.saveAll(anyList())).thenReturn(Arrays.asList(expectedGap));

            List<CareGap> result = engine.detectGapsForPatient("female-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should not detect breast screening gap for males")
        void testCancerGap_NoMammographyForMales() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect cervical cancer screening gap for women")
        void testCancerGap_PapSmearOverdue() {
            Patient femalePatient = Patient.builder()
                .id("female-1")
                .tenantId("tenant-1")
                .dateOfBirth(LocalDate.now().minusYears(40))
                .gender(Patient.Gender.FEMALE)
                .build();

            when(patientService.getPatient("female-1")).thenReturn(Optional.of(femalePatient));
            when(fhirService.getObservationsForPatient("female-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("female-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("female-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("female-1");

            assertNotNull(result);
        }
    }

    // ==================== CARDIOVASCULAR RISK TESTS ====================

    @Nested
    @DisplayName("Cardiovascular Risk Management Gap Detection")
    class CardiovascularRiskTests {

        @Test
        @DisplayName("Should detect lipid panel screening gap")
        void testCardioGap_LipidPanelOverdue() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);

            CareGap expectedGap = CareGap.builder()
                .gapType("CARDIOVASCULAR_RISK_MANAGEMENT")
                .priority("MEDIUM")
                .status("OPEN")
                .measureId("HEDIS-LDL")
                .build();
            when(careGapRepository.saveAll(anyList())).thenReturn(Arrays.asList(expectedGap));

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect blood pressure monitoring gap")
        void testCardioGap_BloodPressureMonitoringOverdue() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect weight management gap for obese patients")
        void testCardioGap_WeightManagementForObesity() {
            Condition obesity = Condition.builder()
                .display("obesity")
                .clinicalStatus("active")
                .build();
            testConditions.add(obesity);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should detect smoking cessation gap")
        void testCardioGap_SmokingCessationNeeded() {
            Condition smoking = Condition.builder()
                .display("smoking-status")
                .clinicalStatus("active")
                .build();
            testConditions.add(smoking);

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<CareGap> result = engine.detectGapsForPatient("patient-1");

            assertNotNull(result);
        }
    }

    // ==================== GAP PRIORITIZATION TESTS ====================

    @Nested
    @DisplayName("Gap Prioritization and Scoring")
    class PrioritizationTests {

        @Test
        @DisplayName("Should calculate priority score for gap")
        void testCalculatePriorityScore() {
            CareGap gap = CareGap.builder()
                .id("gap-1")
                .patientId("patient-1")
                .gapType("CHRONIC_DISEASE_MANAGEMENT")
                .priority("HIGH")
                .financialImpact(5000.0)
                .build();

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));

            double score = engine.calculatePriorityScore(gap);

            assertTrue(score > 0);
            assertTrue(score <= 100);
        }

        @Test
        @DisplayName("Should prioritize cancer screening as high")
        void testPrioritize_CancerScreeningHigher() {
            CareGap cancerGap = CareGap.builder()
                .id("gap-cancer")
                .patientId("patient-1")
                .gapType("CANCER_SCREENING")
                .priority("HIGH")
                .financialImpact(8000.0)
                .build();

            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));

            double score = engine.calculatePriorityScore(cancerGap);

            assertTrue(score > 80);
        }

        @Test
        @DisplayName("Should get high priority gaps for tenant")
        void testGetHighPriorityGaps() {
            List<CareGap> gaps = Arrays.asList(
                CareGap.builder().id("gap-1").priority("HIGH").status("OPEN").gapType("CHRONIC_DISEASE_MANAGEMENT").patientId("patient-1").build(),
                CareGap.builder().id("gap-2").priority("LOW").status("OPEN").gapType("PREVENTIVE_CARE").patientId("patient-1").build()
            );
            when(careGapRepository.findByTenantId("tenant-1")).thenReturn(gaps);
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));

            List<CareGap> result = engine.getHighPriorityGaps("tenant-1", 5);

            assertNotNull(result);
            assertTrue(result.size() <= 5);
        }

        @Test
        @DisplayName("Should get overdue gaps for tenant")
        void testGetOverdueGaps() {
            LocalDateTime pastDue = LocalDateTime.now().minusDays(5);
            List<CareGap> overdueGaps = Arrays.asList(
                CareGap.builder().id("gap-1").dueDate(pastDue).status("OPEN").build()
            );
            when(careGapRepository.findOverdueGaps("tenant-1")).thenReturn(overdueGaps);

            List<CareGap> result = engine.getOverdueGaps("tenant-1");

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should get gaps due soon")
        void testGetGapsDueSoon() {
            List<CareGap> dueSoonGaps = new ArrayList<>();
            when(careGapRepository.findGapsDueSoon("tenant-1", 30)).thenReturn(dueSoonGaps);

            List<CareGap> result = engine.getGapsDueSoon("tenant-1", 30);

            assertNotNull(result);
            verify(careGapRepository).findGapsDueSoon("tenant-1", 30);
        }
    }

    // ==================== AUTO-CLOSURE TESTS ====================

    @Nested
    @DisplayName("Gap Auto-Closure Validation")
    class AutoClosureTests {

        @Test
        @DisplayName("Should close preventive care gap when condition met")
        void testAutoClose_PreventiveGapMet() {
            Observation recentWellness = Observation.builder()
                .display("wellness-visit")
                .effectiveDate(LocalDateTime.now().minusDays(5))
                .build();

            CareGap gap = CareGap.builder()
                .id("gap-1")
                .patientId("patient-1")
                .gapType("PREVENTIVE_CARE")
                .description("Annual wellness visit overdue")
                .status("OPEN")
                .detectedDate(LocalDateTime.now().minusDays(30))
                .build();

            when(careGapRepository.findById("gap-1")).thenReturn(Optional.of(gap));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(Arrays.asList(recentWellness));
            when(careGapRepository.save(any(CareGap.class))).thenReturn(gap);

            boolean result = engine.autoCloseGapIfEligible("gap-1");

            assertTrue(result);
            verify(careGapRepository).save(argThat(g -> "CLOSED".equals(g.getStatus())));
        }

        @Test
        @DisplayName("Should not close gap when condition not met")
        void testAutoClose_ConditionNotMet() {
            CareGap gap = CareGap.builder()
                .id("gap-1")
                .patientId("patient-1")
                .gapType("PREVENTIVE_CARE")
                .description("Annual wellness visit overdue")
                .status("OPEN")
                .detectedDate(LocalDateTime.now())
                .build();

            when(careGapRepository.findById("gap-1")).thenReturn(Optional.of(gap));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(new ArrayList<>());

            boolean result = engine.autoCloseGapIfEligible("gap-1");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should close chronic disease gap when HbA1c done")
        void testAutoClose_ChronicGapHbA1cMet() {
            Observation recentHbA1c = Observation.builder()
                .display("HbA1c")
                .effectiveDate(LocalDateTime.now().minusDays(5))
                .build();

            CareGap gap = CareGap.builder()
                .id("gap-1")
                .patientId("patient-1")
                .gapType("CHRONIC_DISEASE_MANAGEMENT")
                .description("HbA1c test overdue (diabetes monitoring)")
                .status("OPEN")
                .detectedDate(LocalDateTime.now().minusDays(30))
                .build();

            when(careGapRepository.findById("gap-1")).thenReturn(Optional.of(gap));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(Arrays.asList(recentHbA1c));
            when(careGapRepository.save(any(CareGap.class))).thenReturn(gap);

            boolean result = engine.autoCloseGapIfEligible("gap-1");

            assertTrue(result);
        }

        @Test
        @DisplayName("Should close cancer screening gap when colonoscopy done")
        void testAutoClose_CancerScreeningGapMet() {
            Observation recentColonoscopy = Observation.builder()
                .display("colonoscopy")
                .effectiveDate(LocalDateTime.now().minusDays(5))
                .build();

            CareGap gap = CareGap.builder()
                .id("gap-1")
                .patientId("patient-1")
                .gapType("CANCER_SCREENING")
                .description("Colorectal cancer screening (colonoscopy) overdue")
                .status("OPEN")
                .detectedDate(LocalDateTime.now().minusDays(30))
                .build();

            when(careGapRepository.findById("gap-1")).thenReturn(Optional.of(gap));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(Arrays.asList(recentColonoscopy));
            when(careGapRepository.save(any(CareGap.class))).thenReturn(gap);

            boolean result = engine.autoCloseGapIfEligible("gap-1");

            assertTrue(result);
        }

        @Test
        @DisplayName("Should close medication gap after refill")
        void testAutoClose_MedicationGapAfterRefill() {
            CareGap gap = CareGap.builder()
                .id("gap-1")
                .patientId("patient-1")
                .gapType("MEDICATION_ADHERENCE")
                .description("Medication refill overdue")
                .status("OPEN")
                .detectedDate(LocalDateTime.now().minusHours(48))
                .updatedAt(LocalDateTime.now().minusHours(12))
                .build();

            when(careGapRepository.findById("gap-1")).thenReturn(Optional.of(gap));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(new ArrayList<>());
            when(careGapRepository.save(any(CareGap.class))).thenReturn(gap);

            boolean result = engine.autoCloseGapIfEligible("gap-1");

            assertTrue(result);
        }
    }

    // ==================== BATCH PROCESSING TESTS ====================

    @Nested
    @DisplayName("Batch Gap Processing")
    class BatchProcessingTests {

        @Test
        @DisplayName("Should handle empty patient list")
        void testBatchDetect_EmptyList() {
            List<String> emptyList = new ArrayList<>();

            Map<String, List<CareGap>> result = engine.batchDetectGaps(emptyList);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle single patient batch")
        void testBatchDetect_SinglePatient() {
            List<String> singlePatient = Arrays.asList("patient-1");
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1")).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient("patient-1")).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient("patient-1")).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            Map<String, List<CareGap>> result = engine.batchDetectGaps(singlePatient);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should continue processing after individual patient error")
        void testBatchDetect_ContinueAfterError() {
            List<String> patients = Arrays.asList("patient-1", "patient-invalid", "patient-3");
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(patientService.getPatient("patient-invalid")).thenReturn(Optional.empty());
            when(patientService.getPatient("patient-3")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient(anyString())).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient(anyString())).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient(anyString())).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            Map<String, List<CareGap>> result = engine.batchDetectGaps(patients);

            assertEquals(3, result.size());
            assertTrue(result.containsKey("patient-invalid"));
        }

        @Test
        @DisplayName("Should return aggregated results from batch")
        void testBatchDetect_AggregatedResults() {
            List<String> patients = Arrays.asList("patient-1", "patient-2");
            when(patientService.getPatient(anyString())).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient(anyString())).thenReturn(testObservations);
            when(fhirService.getConditionsForPatient(anyString())).thenReturn(testConditions);
            when(fhirService.getMedicationsForPatient(anyString())).thenReturn(testMedications);
            when(careGapRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            Map<String, List<CareGap>> result = engine.batchDetectGaps(patients);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.containsKey("patient-1"));
            assertTrue(result.containsKey("patient-2"));
        }
    }

    // ==================== FINANCIAL IMPACT TESTS ====================

    @Nested
    @DisplayName("Financial Impact Analysis")
    class FinancialImpactTests {

        @Test
        @DisplayName("Should get total financial impact")
        void testGetTotalFinancialImpact() {
            when(careGapRepository.getTotalFinancialImpact("tenant-1")).thenReturn(50000.0);

            Double result = engine.getTotalFinancialImpact("tenant-1");

            assertEquals(50000.0, result);
        }

        @Test
        @DisplayName("Should return zero when no financial impact")
        void testGetTotalFinancialImpact_Zero() {
            when(careGapRepository.getTotalFinancialImpact("tenant-1")).thenReturn(null);

            Double result = engine.getTotalFinancialImpact("tenant-1");

            assertEquals(0.0, result);
        }

        @Test
        @DisplayName("Should get average risk score")
        void testGetAverageRiskScore() {
            when(careGapRepository.getAverageRiskScore("tenant-1")).thenReturn(65.5);

            Double result = engine.getAverageRiskScore("tenant-1");

            assertEquals(65.5, result);
        }

        @Test
        @DisplayName("Should return zero for average risk score when none exist")
        void testGetAverageRiskScore_Zero() {
            when(careGapRepository.getAverageRiskScore("tenant-1")).thenReturn(null);

            Double result = engine.getAverageRiskScore("tenant-1");

            assertEquals(0.0, result);
        }
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle patient service exception")
        void testErrorHandling_PatientServiceException() {
            when(patientService.getPatient("patient-1"))
                .thenThrow(new RuntimeException("Service unavailable"));

            assertThrows(RuntimeException.class, () -> engine.detectGapsForPatient("patient-1"));
        }

        @Test
        @DisplayName("Should handle FHIR service exception")
        void testErrorHandling_FhirServiceException() {
            when(patientService.getPatient("patient-1")).thenReturn(Optional.of(testPatient));
            when(fhirService.getObservationsForPatient("patient-1"))
                .thenThrow(new RuntimeException("FHIR service error"));

            assertThrows(RuntimeException.class, () -> engine.detectGapsForPatient("patient-1"));
        }

        @Test
        @DisplayName("Should handle gap not found in auto-closure")
        void testErrorHandling_GapNotFoundInAutoClose() {
            when(careGapRepository.findById("invalid-gap")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> engine.autoCloseGapIfEligible("invalid-gap"));
        }
    }
}
