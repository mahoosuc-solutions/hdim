package com.healthdata.quality.service;

import com.healthdata.quality.dto.CareGapDTO;
import com.healthdata.quality.dto.HealthScoreDTO;
import com.healthdata.quality.dto.MentalHealthAssessmentDTO;
import com.healthdata.quality.dto.RiskAssessmentDTO;
import com.healthdata.quality.persistence.*;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for PatientHealthService - Real Data Integration
 *
 * Tests real calculation of health scores from FHIR data:
 * - Physical health score from vital signs
 * - Chronic disease score from conditions and control metrics
 * - Preventive care score from screenings
 * - Health score trend from history
 */
@ExtendWith(MockitoExtension.class)
class PatientHealthServiceTest {

    @Mock
    private MentalHealthAssessmentService mentalHealthService;

    @Mock
    private CareGapService careGapService;

    @Mock
    private RiskStratificationService riskService;

    @Mock
    private CareGapRepository careGapRepository;

    @Mock
    private MentalHealthAssessmentRepository mentalHealthRepository;

    @Mock
    private PatientDataService patientDataService;

    @Mock
    private HealthScoreHistoryRepository healthScoreHistoryRepository;

    @InjectMocks
    private PatientHealthService patientHealthService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        reset(mentalHealthService, careGapService, riskService,
              careGapRepository, mentalHealthRepository, patientDataService,
              healthScoreHistoryRepository);
    }

    // ===== PHYSICAL HEALTH SCORE TESTS =====

    @Test
    void testPhysicalHealthScore_AllVitalsInHealthyRange() {
        // Given: All vitals in healthy range
        List<Observation> observations = Arrays.asList(
            createVitalSignObservation("85714-4", "Heart rate", 72.0, "beats/min"),
            createVitalSignObservation("8480-6", "Systolic BP", 118.0, "mmHg"),
            createVitalSignObservation("8462-4", "Diastolic BP", 78.0, "mmHg"),
            createVitalSignObservation("39156-5", "BMI", 23.5, "kg/m2"),
            createVitalSignObservation("29463-7", "Body Weight", 70.0, "kg")
        );

        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(observations);

        // When: Calculate physical health score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Physical score should be high (all vitals healthy)
        assertThat(score.getPhysicalHealthScore()).isGreaterThanOrEqualTo(90.0);
    }

    @Test
    void testPhysicalHealthScore_SomeVitalsOutOfRange() {
        // Given: Some vitals out of range
        List<Observation> observations = Arrays.asList(
            createVitalSignObservation("85714-4", "Heart rate", 95.0, "beats/min"), // High
            createVitalSignObservation("8480-6", "Systolic BP", 145.0, "mmHg"), // High
            createVitalSignObservation("8462-4", "Diastolic BP", 75.0, "mmHg"), // Normal
            createVitalSignObservation("39156-5", "BMI", 31.0, "kg/m2") // High (obese)
        );

        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(observations);

        // When: Calculate physical health score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Physical score should be moderate (50% in range)
        assertThat(score.getPhysicalHealthScore()).isBetween(40.0, 70.0);
    }

    @Test
    void testPhysicalHealthScore_NoVitals() {
        // Given: No vital signs available
        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(Collections.emptyList());

        // When: Calculate physical health score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Physical score should be default (50 - unknown)
        assertThat(score.getPhysicalHealthScore()).isEqualTo(50.0);
    }

    @Test
    void testPhysicalHealthScore_UsesMostRecentVital() {
        stubDefaultsForScore();
        Observation older = createVitalSignObservation("85714-4", "Heart rate", 70.0, "beats/min");
        older.setEffective(new DateTimeType(Date.from(Instant.now().minus(20, ChronoUnit.DAYS))));
        Observation newer = createVitalSignObservation("85714-4", "Heart rate", 120.0, "beats/min");
        newer.setEffective(new DateTimeType(Date.from(Instant.now().minus(2, ChronoUnit.DAYS))));

        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(older, newer));

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getPhysicalHealthScore()).isEqualTo(0.0);
    }

    @Test
    void testPhysicalHealthScore_IgnoresObservationWithoutLoinc() {
        stubDefaultsForScore();
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setEffective(new DateTimeType(Date.from(Instant.now().minus(5, ChronoUnit.DAYS))));
        observation.setCode(new CodeableConcept()
            .addCoding(new Coding().setSystem("http://example.com").setCode("85714-4")));
        observation.setValue(new Quantity().setValue(80));
        observation.addCategory(new CodeableConcept()
            .addCoding(new Coding().setCode("vital-signs")));

        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(observation));

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getPhysicalHealthScore()).isEqualTo(50.0);
    }

    // ===== CHRONIC DISEASE SCORE TESTS =====

    @Test
    void testChronicDiseaseScore_DiabetesWellControlled() {
        // Given: Diabetes with well-controlled HbA1c
        List<Condition> conditions = Arrays.asList(
            createCondition("44054006", "Type 2 Diabetes Mellitus", "active")
        );

        List<Observation> observations = Arrays.asList(
            createLabObservation("4548-4", "Hemoglobin A1c", 6.8, "%")
        );

        when(patientDataService.fetchPatientConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(conditions);
        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(observations);

        // When: Calculate chronic disease score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Chronic disease score should be high (well controlled)
        assertThat(score.getChronicDiseaseScore()).isGreaterThanOrEqualTo(85.0);
    }

    @Test
    void testChronicDiseaseScore_DiabetesPoorlyControlled() {
        // Given: Diabetes with poorly controlled HbA1c
        List<Condition> conditions = Arrays.asList(
            createCondition("44054006", "Type 2 Diabetes Mellitus", "active")
        );

        List<Observation> observations = Arrays.asList(
            createLabObservation("4548-4", "Hemoglobin A1c", 9.5, "%")
        );

        when(patientDataService.fetchPatientConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(conditions);
        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(observations);

        // When: Calculate chronic disease score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Chronic disease score should be low (poorly controlled)
        assertThat(score.getChronicDiseaseScore()).isLessThanOrEqualTo(50.0);
    }

    @Test
    void testChronicDiseaseScore_HypertensionControlled() {
        // Given: Hypertension with controlled BP
        List<Condition> conditions = Arrays.asList(
            createCondition("38341003", "Hypertensive disorder", "active")
        );

        List<Observation> observations = Arrays.asList(
            createVitalSignObservation("8480-6", "Systolic BP", 125.0, "mmHg"),
            createVitalSignObservation("8462-4", "Diastolic BP", 80.0, "mmHg")
        );

        when(patientDataService.fetchPatientConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(conditions);
        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(observations);

        // When: Calculate chronic disease score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Chronic disease score should be high
        assertThat(score.getChronicDiseaseScore()).isGreaterThanOrEqualTo(85.0);
    }

    @Test
    void testChronicDiseaseScore_MultipleConditionsVariedControl() {
        // Given: Multiple chronic conditions with varied control
        List<Condition> conditions = Arrays.asList(
            createCondition("44054006", "Type 2 Diabetes Mellitus", "active"),
            createCondition("38341003", "Hypertensive disorder", "active"),
            createCondition("13644009", "Hyperlipidemia", "active")
        );

        List<Observation> observations = Arrays.asList(
            createLabObservation("4548-4", "Hemoglobin A1c", 7.2, "%"), // Good control
            createVitalSignObservation("8480-6", "Systolic BP", 150.0, "mmHg"), // Poor control
            createLabObservation("2093-3", "Total Cholesterol", 180.0, "mg/dL") // Good control
        );

        when(patientDataService.fetchPatientConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(conditions);
        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(observations);

        // When: Calculate chronic disease score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Score should reflect mixed control (2 of 3 controlled)
        assertThat(score.getChronicDiseaseScore()).isBetween(60.0, 80.0);
    }

    @Test
    void testChronicDiseaseScore_NoChronicConditions() {
        // Given: No chronic conditions
        when(patientDataService.fetchPatientConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(Collections.emptyList());
        when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            TENANT_ID, PATIENT_ID, CareGapEntity.GapCategory.CHRONIC_DISEASE))
            .thenReturn(Collections.emptyList());

        // When: Calculate chronic disease score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Score should be 100 (no chronic diseases)
        assertThat(score.getChronicDiseaseScore()).isEqualTo(100.0);
    }

    @Test
    void testChronicDiseaseScore_NoConditionsWithOpenGaps() {
        when(patientDataService.fetchPatientConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(Collections.emptyList());
        CareGapEntity gap = CareGapEntity.builder()
            .status(CareGapEntity.Status.OPEN)
            .build();
        when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            TENANT_ID, PATIENT_ID, CareGapEntity.GapCategory.CHRONIC_DISEASE))
            .thenReturn(List.of(gap));

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getChronicDiseaseScore()).isLessThan(100.0);
    }

    // ===== PREVENTIVE CARE SCORE TESTS =====

    @Test
    void testPreventiveCareScore_AllScreeningsComplete() {
        // Given: Patient with all recommended screenings complete
        List<Procedure> procedures = Arrays.asList(
            createScreeningProcedure("73761001", "Colonoscopy", -180), // 6 months ago
            createScreeningProcedure("268547008", "Mammography", -180),
            createScreeningProcedure("310078007", "Cervical cancer screening", -180)
        );

        Patient patient = createPatient(55, Enumerations.AdministrativeGender.FEMALE);

        lenient().when(patientDataService.fetchPatientProcedures(anyString(), any(UUID.class)))
            .thenReturn(procedures);
        lenient().when(patientDataService.fetchPatient(anyString(), any(UUID.class)))
            .thenReturn(patient);
        lenient().when(patientDataService.fetchPatientObservations(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientConditions(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        lenient().when(healthScoreHistoryRepository.findRecentScores(anyString(), any(UUID.class), anyInt()))
            .thenReturn(Collections.emptyList());
        lenient().when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());

        // When: Calculate preventive care score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Score should be high (all screenings complete)
        assertThat(score.getPreventiveCareScore()).isGreaterThanOrEqualTo(90.0);
    }

    @Test
    void testPreventiveCareScore_SomeScreeningsOverdue() {
        // Given: Some screenings overdue (1 up to date, 2 overdue/missing)
        List<Procedure> procedures = Arrays.asList(
            createScreeningProcedure("73761001", "Colonoscopy", -4000), // >10 years ago (overdue)
            createScreeningProcedure("268547008", "Mammography", -180)   // 6 months ago (up to date)
            // Cervical screening is missing
        );

        Patient patient = createPatient(55, Enumerations.AdministrativeGender.FEMALE);

        lenient().when(patientDataService.fetchPatientProcedures(anyString(), any(UUID.class)))
            .thenReturn(procedures);
        lenient().when(patientDataService.fetchPatient(anyString(), any(UUID.class)))
            .thenReturn(patient);
        lenient().when(patientDataService.fetchPatientObservations(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientConditions(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        lenient().when(healthScoreHistoryRepository.findRecentScores(anyString(), any(UUID.class), anyInt()))
            .thenReturn(Collections.emptyList());
        lenient().when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());

        // When: Calculate preventive care score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Score should be moderate (some overdue)
        assertThat(score.getPreventiveCareScore()).isBetween(30.0, 70.0);
    }

    @Test
    void testPreventiveCareScore_NoScreenings() {
        // Given: No screenings completed
        Patient patient = createPatient(55, Enumerations.AdministrativeGender.FEMALE);

        when(patientDataService.fetchPatientProcedures(TENANT_ID, PATIENT_ID))
            .thenReturn(Collections.emptyList());
        when(patientDataService.fetchPatient(TENANT_ID, PATIENT_ID))
            .thenReturn(patient);
        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(Collections.emptyList());
        when(patientDataService.fetchPatientConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(Collections.emptyList());
        when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        when(healthScoreHistoryRepository.findRecentScores(TENANT_ID, PATIENT_ID, 5))
            .thenReturn(Collections.emptyList());
        when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());

        // When: Calculate preventive care score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Score should be low (no screenings)
        assertThat(score.getPreventiveCareScore()).isLessThanOrEqualTo(50.0);
    }

    @Test
    void testPreventiveCareScore_NoRecommendedScreenings() {
        Patient patient = createPatient(20, Enumerations.AdministrativeGender.MALE);

        when(patientDataService.fetchPatient(TENANT_ID, PATIENT_ID))
            .thenReturn(patient);
        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(Collections.emptyList());
        when(patientDataService.fetchPatientConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(Collections.emptyList());
        when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        when(healthScoreHistoryRepository.findRecentScores(TENANT_ID, PATIENT_ID, 5))
            .thenReturn(Collections.emptyList());
        when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getPreventiveCareScore()).isEqualTo(100.0);
    }

    @Test
    void testPreventiveCareScore_NoPatientDemographics() {
        when(patientDataService.fetchPatient(TENANT_ID, PATIENT_ID))
            .thenReturn(null);
        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(Collections.emptyList());
        when(patientDataService.fetchPatientConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(Collections.emptyList());
        when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        when(healthScoreHistoryRepository.findRecentScores(TENANT_ID, PATIENT_ID, 5))
            .thenReturn(Collections.emptyList());
        when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getPreventiveCareScore()).isEqualTo(50.0);
    }

    // ===== MENTAL HEALTH SCORE TESTS =====

    @Test
    void testMentalHealthScore_NoAssessments() {
        when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());
        stubDefaultsForScore();

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getMentalHealthScore()).isEqualTo(100.0);
    }

    @Test
    void testMentalHealthScore_SeverePenaltyApplied() {
        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .score(20)
            .maxScore(27)
            .severity("severe")
            .build();

        when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(List.of(assessment));
        stubDefaultsForScore();

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getMentalHealthScore()).isLessThan(60.0);
    }

    @Test
    void testMentalHealthScore_ModeratePenaltyApplied() {
        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .score(10)
            .maxScore(20)
            .severity("moderate")
            .build();

        when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(List.of(assessment));
        stubDefaultsForScore();

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getMentalHealthScore()).isEqualTo(35.0);
    }

    // ===== SOCIAL SCORE TESTS =====

    @Test
    void testSocialScore_WithUnmetAndAddressedNeeds() {
        List<Observation> observations = List.of(
            createSocialObservationWithBoolean("Food insecurity", true),
            createSocialObservationWithCode("LA31994-9", Observation.ObservationStatus.FINAL)
        );

        stubDefaultsForScore();
        when(patientDataService.fetchSocialHistoryObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(observations);

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getSocialDeterminantsScore()).isBetween(80.0, 100.0);
    }

    @Test
    void testSocialScore_NoObservations() {
        stubDefaultsForScore();

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getSocialDeterminantsScore()).isEqualTo(80.0);
    }

    @Test
    void testSocialScore_UnmetNeedByCode() {
        List<Observation> observations = List.of(
            createSocialObservationWithCode("LA31996-4", Observation.ObservationStatus.FINAL)
        );

        stubDefaultsForScore();
        when(patientDataService.fetchSocialHistoryObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(observations);

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getSocialDeterminantsScore()).isEqualTo(90.0);
    }

    @Test
    void testSocialScore_AddressedNeedByBooleanFalse() {
        List<Observation> observations = List.of(
            createSocialObservationWithBoolean("Housing stability", false)
        );

        stubDefaultsForScore();
        when(patientDataService.fetchSocialHistoryObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(observations);

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getSocialDeterminantsScore()).isEqualTo(100.0);
    }

    @Test
    void testPhysicalHealthScore_ErrorFallsBackToDefault() {
        stubDefaultsForScore();
        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenThrow(new RuntimeException("boom"));

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getPhysicalHealthScore()).isEqualTo(50.0);
    }

    @Test
    void testPhysicalHealthScore_IgnoresOldOrNonVital() {
        stubDefaultsForScore();
        Observation oldObservation = new Observation();
        oldObservation.setStatus(Observation.ObservationStatus.FINAL);
        oldObservation.setEffective(new DateTimeType(
            Date.from(Instant.now().minus(200, ChronoUnit.DAYS))));
        oldObservation.setCode(new CodeableConcept()
            .addCoding(new Coding().setSystem("http://loinc.org").setCode("85714-4")));
        oldObservation.setValue(new Quantity().setValue(80));
        oldObservation.addCategory(new CodeableConcept()
            .addCoding(new Coding().setCode("laboratory")));

        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(oldObservation));

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getPhysicalHealthScore()).isEqualTo(50.0);
    }

    @Test
    void testChronicDiseaseScore_IgnoresInactiveConditions() {
        stubDefaultsForScore();
        List<Condition> conditions = List.of(
            createCondition("44054006", "Type 2 Diabetes Mellitus", "inactive")
        );

        when(patientDataService.fetchPatientConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(conditions);
        when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            TENANT_ID, PATIENT_ID, CareGapEntity.GapCategory.CHRONIC_DISEASE))
            .thenReturn(Collections.emptyList());

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getChronicDiseaseScore()).isEqualTo(100.0);
    }

    @Test
    void testPreventiveCareScore_ErrorFallsBackToDefault() {
        stubDefaultsForScore();
        when(patientDataService.fetchPatient(TENANT_ID, PATIENT_ID))
            .thenThrow(new RuntimeException("patient lookup failed"));

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getPreventiveCareScore()).isEqualTo(50.0);
    }

    @Test
    void testPhysicalHealthScore_NonQuantityValueIgnored() {
        stubDefaultsForScore();
        Observation obs = createVitalSignObservation("85714-4", "Heart rate", 72.0, "beats/min");
        obs.setValue(new StringType("invalid"));

        when(patientDataService.fetchPatientObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(obs));

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getPhysicalHealthScore()).isEqualTo(0.0);
    }

    @Test
    void testIsRecentFalseWhenNoDateTime() {
        Observation obs = new Observation();
        boolean recent = (boolean) ReflectionTestUtils.invokeMethod(
            patientHealthService, "isRecent", obs, Instant.now());

        assertThat(recent).isFalse();
    }

    @Test
    void testIsMoreRecentTrueWhenNewer() {
        Observation older = new Observation();
        older.setEffective(new DateTimeType(Date.from(Instant.now().minus(10, ChronoUnit.DAYS))));
        Observation newer = new Observation();
        newer.setEffective(new DateTimeType(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))));

        boolean isMoreRecent = (boolean) ReflectionTestUtils.invokeMethod(
            patientHealthService, "isMoreRecent", newer, older);

        assertThat(isMoreRecent).isTrue();
    }

    @Test
    void testIsInHealthyRangeUnknownCode() {
        Observation obs = createVitalSignObservation("0000-0", "Unknown", 10.0, "unit");
        boolean inRange = (boolean) ReflectionTestUtils.invokeMethod(
            patientHealthService, "isInHealthyRange", "0000-0", obs);

        assertThat(inRange).isFalse();
    }

    @Test
    void testIsUnmetSdohNeedForInstabilityDisplay() {
        Observation observation = createSocialObservationWithBoolean("Housing instability", true);

        boolean unmet = (boolean) ReflectionTestUtils.invokeMethod(
            patientHealthService, "isUnmetSdohNeed", observation);

        assertThat(unmet).isTrue();
    }

    @Test
    void testIsAddressedSdohNeedWithNeverTrueCode() {
        Observation observation = createSocialObservationWithCode(
            "LA28398-8", Observation.ObservationStatus.FINAL);

        boolean addressed = (boolean) ReflectionTestUtils.invokeMethod(
            patientHealthService, "isAddressedSdohNeed", observation);

        assertThat(addressed).isTrue();
    }

    @Test
    void testIsAddressedSdohNeedFalseWhenStatusNotFinal() {
        Observation observation = createSocialObservationWithCode(
            "LA31994-9", Observation.ObservationStatus.REGISTERED);

        boolean addressed = (boolean) ReflectionTestUtils.invokeMethod(
            patientHealthService, "isAddressedSdohNeed", observation);

        assertThat(addressed).isFalse();
    }

    @Test
    void testHasScreeningCodeReturnsFalseWhenMissingCode() {
        Procedure procedure = new Procedure();

        boolean hasCode = (boolean) ReflectionTestUtils.invokeMethod(
            patientHealthService, "hasScreeningCode", procedure, "73761001");

        assertThat(hasCode).isFalse();
    }

    @Test
    void testAssessDiseaseControlUnknownWhenNoSnomedCode() {
        Condition condition = new Condition();
        condition.setCode(new CodeableConcept()
            .addCoding(new Coding().setSystem("http://example.com").setCode("123")));

        String controlStatus = (String) ReflectionTestUtils.invokeMethod(
            patientHealthService, "assessDiseaseControl", condition, Collections.emptyList());

        assertThat(controlStatus).isEqualTo("unknown");
    }

    @Test
    void testMentalHealthScore_ModeratelySeverePenalty() {
        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .score(15)
            .maxScore(27)
            .severity("moderately-severe")
            .build();

        when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(List.of(assessment));
        stubDefaultsForScore();

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getMentalHealthScore()).isLessThan(60.0);
    }

    @Test
    void testSocialScore_ExceptionFallback() {
        stubDefaultsForScore();
        when(patientDataService.fetchSocialHistoryObservations(TENANT_ID, PATIENT_ID))
            .thenThrow(new RuntimeException("sdoh error"));

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getSocialDeterminantsScore()).isEqualTo(80.0);
    }

    @Test
    void testPreventiveCareScore_MaleWithColonoscopyOverdue() {
        Patient patient = createPatient(60, Enumerations.AdministrativeGender.MALE);
        List<Procedure> procedures = List.of(
            createScreeningProcedure("73761001", "Colonoscopy", -5000)
        );

        stubDefaultsForScore();
        when(patientDataService.fetchPatient(TENANT_ID, PATIENT_ID)).thenReturn(patient);
        when(patientDataService.fetchPatientProcedures(TENANT_ID, PATIENT_ID)).thenReturn(procedures);

        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        assertThat(score.getPreventiveCareScore()).isEqualTo(0.0);
    }

    // ===== HEALTH OVERVIEW TESTS =====

    @Test
    void testGetPatientHealthOverview() {
        List<MentalHealthAssessmentDTO> assessments = List.of(MentalHealthAssessmentDTO.builder().build());
        List<CareGapDTO> gaps = List.of(CareGapDTO.builder().priority("urgent").build());
        RiskAssessmentDTO risk = RiskAssessmentDTO.builder().build();

        when(mentalHealthService.getPatientAssessments(TENANT_ID, PATIENT_ID, null, 5, 0))
            .thenReturn(assessments);
        when(careGapService.getPatientCareGaps(TENANT_ID, PATIENT_ID, null, null))
            .thenReturn(gaps);
        when(riskService.getRiskAssessment(TENANT_ID, PATIENT_ID))
            .thenReturn(risk);
        when(mentalHealthRepository.countByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(3L);
        when(mentalHealthRepository.findPositiveScreensRequiringFollowup(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(MentalHealthAssessmentEntity.builder().build()));
        stubDefaultsForScore();

        var overview = patientHealthService.getPatientHealthOverview(TENANT_ID, PATIENT_ID);

        assertThat(overview.getRecentMentalHealthAssessments()).hasSize(1);
        assertThat(overview.getOpenCareGaps()).hasSize(1);
        assertThat(overview.getSummaryStats().getUrgentCareGaps()).isEqualTo(1);
        assertThat(overview.getSummaryStats().getTotalMentalHealthAssessments()).isEqualTo(3);
        assertThat(overview.getSummaryStats().getPositiveScreensRequiringFollowup()).isEqualTo(1);
    }

    // ===== HEALTH SCORE TREND TESTS =====

    @Test
    void testHealthScoreTrend_Improving() {
        // Given: Historical scores showing improvement (most recent < current calculated score)
        List<HealthScoreHistoryEntity> history = Arrays.asList(
            createHistoryEntry(65.0, -30), // Most recent past: 65
            createHistoryEntry(55.0, -60), // 60 days ago: 55
            createHistoryEntry(45.0, -90)  // 90 days ago: 45
        );

        lenient().when(healthScoreHistoryRepository.findRecentScores(anyString(), any(UUID.class), anyInt()))
            .thenReturn(history);
        lenient().when(patientDataService.fetchPatientObservations(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientConditions(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientProcedures(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatient(anyString(), any(UUID.class)))
            .thenReturn(null);
        lenient().when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        lenient().when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());

        // When: Calculate health score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Trend should be improving
        assertThat(score.getTrend()).isEqualTo("improving");
    }

    @Test
    void testHealthScoreTrend_Declining() {
        // Given: Historical scores showing decline (most recent > current calculated score)
        List<HealthScoreHistoryEntity> history = Arrays.asList(
            createHistoryEntry(85.0, -30), // Most recent past: 85
            createHistoryEntry(90.0, -60), // 60 days ago: 90
            createHistoryEntry(95.0, -90)  // 90 days ago: 95
        );

        lenient().when(healthScoreHistoryRepository.findRecentScores(anyString(), any(UUID.class), anyInt()))
            .thenReturn(history);
        lenient().when(patientDataService.fetchPatientObservations(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientConditions(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientProcedures(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatient(anyString(), any(UUID.class)))
            .thenReturn(null);
        lenient().when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        lenient().when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());

        // When: Calculate health score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Trend should be declining
        assertThat(score.getTrend()).isEqualTo("declining");
    }

    @Test
    void testHealthScoreTrend_Stable() {
        // Given: Historical scores showing stability
        List<HealthScoreHistoryEntity> history = Arrays.asList(
            createHistoryEntry(75.0, -30),
            createHistoryEntry(76.0, -60),
            createHistoryEntry(74.0, -90)
        );

        lenient().when(healthScoreHistoryRepository.findRecentScores(anyString(), any(UUID.class), anyInt()))
            .thenReturn(history);
        lenient().when(patientDataService.fetchPatientObservations(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientConditions(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientProcedures(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatient(anyString(), any(UUID.class)))
            .thenReturn(null);
        lenient().when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        lenient().when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());

        // When: Calculate health score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Trend should be stable
        assertThat(score.getTrend()).isEqualTo("stable");
    }

    @Test
    void testHealthScoreTrend_SignificantChange() {
        // Given: Historical score with >10 point change
        List<HealthScoreHistoryEntity> history = Arrays.asList(
            createHistoryEntry(90.0, -30),  // Current: 90
            createHistoryEntry(75.0, -60)   // Previous: 75 (15 point increase)
        );

        lenient().when(healthScoreHistoryRepository.findRecentScores(anyString(), any(UUID.class), anyInt()))
            .thenReturn(history);
        lenient().when(patientDataService.fetchPatientObservations(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientConditions(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientProcedures(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatient(anyString(), any(UUID.class)))
            .thenReturn(null);
        lenient().when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        lenient().when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());

        // When: Calculate health score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Should detect significant change
        assertThat(score.isSignificantChange()).isTrue();
        assertThat(Math.abs(score.getScoreDelta())).isGreaterThanOrEqualTo(10.0);
    }

    @Test
    void testHealthScoreTrend_NoHistory() {
        // Given: No historical scores
        lenient().when(healthScoreHistoryRepository.findRecentScores(anyString(), any(UUID.class), anyInt()))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientObservations(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientConditions(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientProcedures(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatient(anyString(), any(UUID.class)))
            .thenReturn(null);
        lenient().when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        lenient().when(mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            anyString(), any(UUID.class), any(PageRequest.class)))
            .thenReturn(Collections.emptyList());

        // When: Calculate health score
        HealthScoreDTO score = patientHealthService.calculateHealthScore(TENANT_ID, PATIENT_ID);

        // Then: Trend should be "new" (no history)
        assertThat(score.getTrend()).isEqualTo("new");
        assertThat(score.getScoreDelta()).isNull();
    }

    // ===== HELPER METHODS =====

    private Observation createVitalSignObservation(String loincCode, String display,
                                                   double value, String unit) {
        Observation obs = new Observation();
        obs.setId(UUID.randomUUID().toString());
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setEffective(new DateTimeType(Date.from(Instant.now().minus(7, ChronoUnit.DAYS))));

        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://loinc.org");
        coding.setCode(loincCode);
        coding.setDisplay(display);
        code.addCoding(coding);
        obs.setCode(code);

        Quantity quantity = new Quantity();
        quantity.setValue(value);
        quantity.setUnit(unit);
        obs.setValue(quantity);

        CodeableConcept category = new CodeableConcept();
        category.addCoding(new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
            .setCode("vital-signs"));
        obs.addCategory(category);

        return obs;
    }

    private Observation createLabObservation(String loincCode, String display,
                                            double value, String unit) {
        Observation obs = createVitalSignObservation(loincCode, display, value, unit);

        CodeableConcept category = new CodeableConcept();
        category.addCoding(new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
            .setCode("laboratory"));
        obs.getCategory().clear();
        obs.addCategory(category);

        return obs;
    }

    private Observation createSocialObservationWithBoolean(String display, boolean value) {
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setEffective(new DateTimeType(Date.from(Instant.now().minus(7, ChronoUnit.DAYS))));

        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding().setDisplay(display));
        obs.setCode(code);
        obs.setValue(new BooleanType(value));

        return obs;
    }

    private Observation createSocialObservationWithCode(String codeValue, Observation.ObservationStatus status) {
        Observation obs = new Observation();
        obs.setStatus(status);
        obs.setEffective(new DateTimeType(Date.from(Instant.now().minus(7, ChronoUnit.DAYS))));

        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding().setDisplay("Housing"));
        obs.setCode(code);

        CodeableConcept value = new CodeableConcept();
        value.addCoding(new Coding().setCode(codeValue));
        obs.setValue(value);

        return obs;
    }

    private void stubDefaultsForScore() {
        lenient().when(patientDataService.fetchPatientObservations(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientConditions(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatientProcedures(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(patientDataService.fetchPatient(anyString(), any(UUID.class)))
            .thenReturn(null);
        lenient().when(patientDataService.fetchSocialHistoryObservations(anyString(), any(UUID.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(careGapRepository.findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
            anyString(), any(UUID.class), any()))
            .thenReturn(Collections.emptyList());
        lenient().when(healthScoreHistoryRepository.findRecentScores(anyString(), any(UUID.class), anyInt()))
            .thenReturn(Collections.emptyList());
    }

    private Condition createCondition(String snomedCode, String display, String clinicalStatus) {
        Condition condition = new Condition();
        condition.setId(UUID.randomUUID().toString());

        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://snomed.info/sct");
        coding.setCode(snomedCode);
        coding.setDisplay(display);
        code.addCoding(coding);
        condition.setCode(code);

        CodeableConcept status = new CodeableConcept();
        status.addCoding(new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
            .setCode(clinicalStatus));
        condition.setClinicalStatus(status);

        return condition;
    }

    private Procedure createScreeningProcedure(String snomedCode, String display, int daysAgo) {
        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID().toString());
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        procedure.setPerformed(new DateTimeType(
            Date.from(Instant.now().plus(daysAgo, ChronoUnit.DAYS))));

        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://snomed.info/sct");
        coding.setCode(snomedCode);
        coding.setDisplay(display);
        code.addCoding(coding);
        procedure.setCode(code);

        return procedure;
    }

    private Patient createPatient(int age, Enumerations.AdministrativeGender gender) {
        Patient patient = new Patient();
        patient.setId(PATIENT_ID.toString());
        patient.setGender(gender);

        Date birthDate = Date.from(Instant.now().minus(age * 365L, ChronoUnit.DAYS));
        patient.setBirthDate(birthDate);

        return patient;
    }

    private HealthScoreHistoryEntity createHistoryEntry(double score, int daysAgo) {
        return HealthScoreHistoryEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .overallScore(score)
            .physicalHealthScore(score)
            .mentalHealthScore(score)
            .socialDeterminantsScore(score)
            .preventiveCareScore(score)
            .chronicDiseaseScore(score)
            .calculatedAt(Instant.now().plus(daysAgo, ChronoUnit.DAYS))
            .build();
    }
}
