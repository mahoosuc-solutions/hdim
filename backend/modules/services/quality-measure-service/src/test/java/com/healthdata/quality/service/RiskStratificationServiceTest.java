package com.healthdata.quality.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import com.healthdata.quality.dto.RiskAssessmentDTO;
import com.healthdata.quality.persistence.*;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RiskStratificationServiceTest {

    @Mock
    private RiskAssessmentRepository repository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IGenericClient fhirClient;

    @Mock
    private CareGapRepository careGapRepository;

    @Mock
    private MentalHealthAssessmentRepository mentalHealthAssessmentRepository;

    @Mock
    private ChronicDiseaseMonitoringRepository chronicDiseaseMonitoringRepository;

    private RiskStratificationService service;

    @BeforeEach
    void setUp() {
        service = new RiskStratificationService(
            repository,
            fhirClient,
            careGapRepository,
            mentalHealthAssessmentRepository,
            chronicDiseaseMonitoringRepository
        );
    }

    @Test
    void shouldCalculateRiskAssessmentWithMultipleSignals() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        Bundle conditionBundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(
                condition("Diabetes", "severe", Instant.now().minus(30, ChronoUnit.DAYS))
            ))
            .addEntry(new Bundle.BundleEntryComponent().setResource(
                condition("Hypertension", "moderate", Instant.now().minus(10, ChronoUnit.DAYS))
            ));

        Bundle observationBundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(
                observation("4548-4", 9.5, "%", "HbA1c", Instant.now())
            ))
            .addEntry(new Bundle.BundleEntryComponent().setResource(
                observation("8480-6", 190, "mmHg", "Systolic BP", Instant.now())
            ))
            .addEntry(new Bundle.BundleEntryComponent().setResource(
                observation("2093-3", 250, "mg/dL", "Total Cholesterol", Instant.now())
            ))
            .addEntry(new Bundle.BundleEntryComponent().setResource(
                observation("18262-6", 150, "mg/dL", "LDL", Instant.now())
            ));

        Bundle medicationBundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(new MedicationStatement()))
            .addEntry(new Bundle.BundleEntryComponent().setResource(new MedicationStatement()))
            .addEntry(new Bundle.BundleEntryComponent().setResource(new MedicationStatement()));

        Bundle encounterBundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(
                encounter("EMER", Instant.now().minus(10, ChronoUnit.DAYS))
            ))
            .addEntry(new Bundle.BundleEntryComponent().setResource(
                encounter("emergency", Instant.now().minus(20, ChronoUnit.DAYS))
            ))
            .addEntry(new Bundle.BundleEntryComponent().setResource(
                encounter("IMP", Instant.now().minus(5, ChronoUnit.DAYS))
            ));

        when(fhirClient.search().forResource(Condition.class)
            .where(anyCriterion())
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(conditionBundle);

        when(fhirClient.search().forResource(Observation.class)
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(observationBundle);

        when(fhirClient.search().forResource(MedicationStatement.class)
            .where(anyCriterion())
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(medicationBundle);

        when(fhirClient.search().forResource(Encounter.class)
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(encounterBundle);

        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .type(MentalHealthAssessmentEntity.AssessmentType.PHQ_9)
            .severity("Severe")
            .score(18)
            .maxScore(27)
            .assessmentDate(Instant.now().minus(20, ChronoUnit.DAYS))
            .build();

        when(mentalHealthAssessmentRepository.findPositiveScreensRequiringFollowup(tenantId, patientId))
            .thenReturn(List.of(assessment));

        when(careGapRepository.countUrgentCareGaps(tenantId, patientId)).thenReturn(2L);
        when(careGapRepository.countOpenCareGaps(tenantId, patientId)).thenReturn(5L);

        ChronicDiseaseMonitoringEntity monitoring = ChronicDiseaseMonitoringEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .diseaseName("Diabetes")
            .previousValue(7.2)
            .latestValue(9.1)
            .trend(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING)
            .build();
        when(chronicDiseaseMonitoringRepository.findByTenantIdAndPatientIdOrderByMonitoredAtDesc(tenantId, patientId))
            .thenReturn(List.of(monitoring));

        when(repository.save(any(RiskAssessmentEntity.class))).thenAnswer(invocation -> {
            RiskAssessmentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            return entity;
        });

        RiskAssessmentDTO result = service.calculateRiskAssessment(tenantId, patientId);

        assertThat(result).isNotNull();
        assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(75);
        assertThat(result.getRiskLevel()).isEqualTo("very-high");
        assertThat(result.getRiskFactors()).isNotEmpty();
        assertThat(result.getPredictedOutcomes()).hasSize(3);
        assertThat(result.getRecommendations())
            .contains("Enroll in care coordination program", "Refer to behavioral health specialist");

        verify(repository).save(any(RiskAssessmentEntity.class));
    }

    @Test
    void shouldCalculateLowRiskWhenNoSignals() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-2";

        Bundle emptyBundle = new Bundle();

        when(fhirClient.search().forResource(Condition.class)
            .where(anyCriterion())
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);

        when(fhirClient.search().forResource(Observation.class)
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);

        when(fhirClient.search().forResource(MedicationStatement.class)
            .where(anyCriterion())
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);

        when(fhirClient.search().forResource(Encounter.class)
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);

        when(mentalHealthAssessmentRepository.findPositiveScreensRequiringFollowup(tenantId, patientId))
            .thenReturn(List.of());
        when(careGapRepository.countUrgentCareGaps(tenantId, patientId)).thenReturn(0L);
        when(careGapRepository.countOpenCareGaps(tenantId, patientId)).thenReturn(0L);
        when(chronicDiseaseMonitoringRepository.findByTenantIdAndPatientIdOrderByMonitoredAtDesc(tenantId, patientId))
            .thenReturn(List.of());

        when(repository.save(any(RiskAssessmentEntity.class))).thenAnswer(invocation -> {
            RiskAssessmentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            return entity;
        });

        RiskAssessmentDTO result = service.calculateRiskAssessment(tenantId, patientId);

        assertThat(result).isNotNull();
        assertThat(result.getRiskScore()).isEqualTo(0);
        assertThat(result.getRiskLevel()).isEqualTo("low");
        assertThat(result.getPredictedOutcomes()).hasSize(2);
        assertThat(result.getRecommendations()).isEmpty();
    }

    @Test
    void shouldReturnMostRecentAssessment() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-3";

        RiskAssessmentEntity entity = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .riskScore(35)
            .riskLevel(RiskAssessmentEntity.RiskLevel.MODERATE)
            .riskFactors(List.of(Map.of(
                "factor", "Moderate anxiety",
                "category", "mental-health",
                "weight", 15,
                "severity", "moderate",
                "evidence", "GAD-7 score: 12/21"
            )))
            .predictedOutcomes(List.of(Map.of(
                "outcome", "ED visit",
                "probability", 0.2,
                "timeframe", "next 90 days"
            )))
            .recommendations(List.of("Schedule care plan review within 30 days"))
            .assessmentDate(Instant.now())
            .createdAt(Instant.now())
            .build();

        when(repository.findMostRecent(tenantId, patientId)).thenReturn(Optional.of(entity));

        RiskAssessmentDTO result = service.getRiskAssessment(tenantId, patientId);

        assertThat(result).isNotNull();
        assertThat(result.getRiskLevel()).isEqualTo("moderate");
        assertThat(result.getRiskFactors()).hasSize(1);
        assertThat(result.getPredictedOutcomes()).hasSize(1);
    }

    @Test
    void shouldHandleFhirErrorsAndReturnLowRisk() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-4";

        when(fhirClient.search().forResource(Condition.class)
            .where(anyCriterion())
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenThrow(new RuntimeException("fhir down"));

        Bundle emptyBundle = new Bundle();
        when(fhirClient.search().forResource(Observation.class)
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);
        when(fhirClient.search().forResource(MedicationStatement.class)
            .where(anyCriterion())
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);
        when(fhirClient.search().forResource(Encounter.class)
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);

        when(mentalHealthAssessmentRepository.findPositiveScreensRequiringFollowup(tenantId, patientId))
            .thenReturn(List.of());
        when(careGapRepository.countUrgentCareGaps(tenantId, patientId)).thenReturn(0L);
        when(careGapRepository.countOpenCareGaps(tenantId, patientId)).thenReturn(0L);
        when(chronicDiseaseMonitoringRepository.findByTenantIdAndPatientIdOrderByMonitoredAtDesc(tenantId, patientId))
            .thenReturn(List.of());

        when(repository.save(any(RiskAssessmentEntity.class))).thenAnswer(invocation -> {
            RiskAssessmentEntity saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        RiskAssessmentDTO result = service.calculateRiskAssessment(tenantId, patientId);

        assertThat(result.getRiskScore()).isEqualTo(0);
        assertThat(result.getRiskLevel()).isEqualTo("low");
        assertThat(result.getPredictedOutcomes()).hasSize(2);
    }

    @Test
    void shouldCalculateModerateRiskWithMentalHealthRecommendation() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-5";

        Bundle conditionBundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(
                condition("Hypertension", "moderate", Instant.now().minus(15, ChronoUnit.DAYS))
            ));

        Bundle emptyBundle = new Bundle();
        when(fhirClient.search().forResource(Condition.class)
            .where(anyCriterion())
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(conditionBundle);
        when(fhirClient.search().forResource(Observation.class)
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);
        when(fhirClient.search().forResource(MedicationStatement.class)
            .where(anyCriterion())
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);
        when(fhirClient.search().forResource(Encounter.class)
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);

        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .type(MentalHealthAssessmentEntity.AssessmentType.GAD_7)
            .severity("Moderate")
            .score(12)
            .maxScore(21)
            .assessmentDate(Instant.now().minus(10, ChronoUnit.DAYS))
            .build();

        when(mentalHealthAssessmentRepository.findPositiveScreensRequiringFollowup(tenantId, patientId))
            .thenReturn(List.of(assessment));
        when(careGapRepository.countUrgentCareGaps(tenantId, patientId)).thenReturn(0L);
        when(careGapRepository.countOpenCareGaps(tenantId, patientId)).thenReturn(0L);
        when(chronicDiseaseMonitoringRepository.findByTenantIdAndPatientIdOrderByMonitoredAtDesc(tenantId, patientId))
            .thenReturn(List.of());

        when(repository.save(any(RiskAssessmentEntity.class))).thenAnswer(invocation -> {
            RiskAssessmentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            return entity;
        });

        RiskAssessmentDTO result = service.calculateRiskAssessment(tenantId, patientId);

        assertThat(result.getRiskLevel()).isEqualTo("moderate");
        assertThat(result.getPredictedOutcomes()).hasSize(3);
        assertThat(result.getRecommendations())
            .contains("Schedule care plan review within 30 days",
                      "Implement monthly check-ins",
                      "Refer to behavioral health specialist");
    }

    @Test
    void shouldCaptureMedicationNonAdherenceRisk() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-6";

        Bundle emptyBundle = new Bundle();
        Bundle medicationBundle = new Bundle()
            .addEntry(new Bundle.BundleEntryComponent().setResource(new MedicationStatement()))
            .addEntry(new Bundle.BundleEntryComponent().setResource(new MedicationStatement()))
            .addEntry(new Bundle.BundleEntryComponent().setResource(new MedicationStatement()))
            .addEntry(new Bundle.BundleEntryComponent().setResource(new MedicationStatement()));

        when(fhirClient.search().forResource(Condition.class)
            .where(anyCriterion())
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);
        when(fhirClient.search().forResource(Observation.class)
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);
        when(fhirClient.search().forResource(MedicationStatement.class)
            .where(anyCriterion())
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(medicationBundle);
        when(fhirClient.search().forResource(Encounter.class)
            .where(anyCriterion())
            .returnBundle(Bundle.class)
            .execute()).thenReturn(emptyBundle);

        when(mentalHealthAssessmentRepository.findPositiveScreensRequiringFollowup(tenantId, patientId))
            .thenReturn(List.of());
        when(careGapRepository.countUrgentCareGaps(tenantId, patientId)).thenReturn(0L);
        when(careGapRepository.countOpenCareGaps(tenantId, patientId)).thenReturn(0L);
        when(chronicDiseaseMonitoringRepository.findByTenantIdAndPatientIdOrderByMonitoredAtDesc(tenantId, patientId))
            .thenReturn(List.of());

        when(repository.save(any(RiskAssessmentEntity.class))).thenAnswer(invocation -> {
            RiskAssessmentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            return entity;
        });

        RiskAssessmentDTO result = service.calculateRiskAssessment(tenantId, patientId);

        assertThat(result.getRiskFactors())
            .anyMatch(factor -> "Medication Non-Adherence".equals(factor.getFactor())
                && factor.getWeight() == 20
                && "high".equals(factor.getSeverity()));
    }

    @Test
    void shouldComputeConditionWeightWithSeverityCap() {
        Condition condition = condition("Diabetes", "severe", Instant.now());

        int weight = ReflectionTestUtils.invokeMethod(
            service, "calculateConditionWeight", "Diabetes", condition);

        assertThat(weight).isEqualTo(30);
    }

    @Test
    void shouldReturnRiskFactorFromConditionText() {
        Condition condition = new Condition();
        condition.setCode(new CodeableConcept()
            .setText("Hypertension")
            .addCoding(new Coding().setDisplay("Hypertension")));
        condition.setSeverity(new CodeableConcept().addCoding(new Coding().setCode("moderate")));

        Object riskFactor = ReflectionTestUtils.invokeMethod(
            service, "extractRiskFactorFromCondition", condition);

        assertThat(riskFactor).isNotNull();
    }

    @Test
    void shouldReturnNullWhenConditionMissingCode() {
        Condition condition = new Condition();

        Object riskFactor = ReflectionTestUtils.invokeMethod(
            service, "extractRiskFactorFromCondition", condition);

        assertThat(riskFactor).isNull();
    }

    @Test
    void shouldReturnNullWhenConditionNameMissing() {
        Condition condition = new Condition();
        condition.setCode(new CodeableConcept().addCoding(new Coding()));

        Object riskFactor = ReflectionTestUtils.invokeMethod(
            service, "extractRiskFactorFromCondition", condition);

        assertThat(riskFactor).isNull();
    }

    @Test
    void shouldSkipObservationOutsideRecencyWindow() {
        Observation observation = observation("4548-4", 9.0, "%", "HbA1c",
            Instant.now().minus(365, ChronoUnit.DAYS));

        Object riskFactor = ReflectionTestUtils.invokeMethod(
            service, "extractRiskFactorFromObservation", observation);

        assertThat(riskFactor).isNull();
    }

    @Test
    void shouldSkipObservationWithNonQuantityValue() {
        Observation observation = new Observation();
        observation.setCode(new CodeableConcept()
            .setText("HbA1c")
            .addCoding(new Coding().setSystem("http://loinc.org").setCode("4548-4")));
        observation.setValue(new StringType("invalid"));

        Object riskFactor = ReflectionTestUtils.invokeMethod(
            service, "extractRiskFactorFromObservation", observation);

        assertThat(riskFactor).isNull();
    }

    @Test
    void shouldEvaluateVeryHighLdlRisk() {
        Object riskFactor = ReflectionTestUtils.invokeMethod(
            service,
            "evaluateObservationRisk",
            "18262-6",
            200.0,
            "mg/dL",
            "LDL"
        );

        assertThat(riskFactor).isNotNull();
    }

    @Test
    void shouldReturnSuboptimalDiabetesRiskForModerateHbA1c() throws Exception {
        Object riskFactor = ReflectionTestUtils.invokeMethod(
            service,
            "evaluateObservationRisk",
            "4548-4",
            8.0,
            "%",
            "HbA1c"
        );

        assertThat(riskFactor).isNotNull();
        assertThat(getRiskFactorValue(riskFactor, "factor")).isEqualTo("Suboptimal Diabetes Control");
        assertThat(getRiskFactorValue(riskFactor, "severity")).isEqualTo("moderate");
    }

    @Test
    void shouldReturnUncontrolledHypertensionForElevatedSystolic() throws Exception {
        Object riskFactor = ReflectionTestUtils.invokeMethod(
            service,
            "evaluateObservationRisk",
            "8480-6",
            160.0,
            "mmHg",
            "Systolic BP"
        );

        assertThat(riskFactor).isNotNull();
        assertThat(getRiskFactorValue(riskFactor, "factor")).isEqualTo("Uncontrolled Hypertension");
    }

    @Test
    void shouldReturnNullForNormalObservationValue() {
        Object riskFactor = ReflectionTestUtils.invokeMethod(
            service,
            "evaluateObservationRisk",
            "4548-4",
            6.5,
            "%",
            "HbA1c"
        );

        assertThat(riskFactor).isNull();
    }

    @Test
    void shouldIncludeSocialDeterminantsRecommendations() throws Exception {
        Class<?> riskFactorClass = Class.forName(
            "com.healthdata.quality.service.RiskStratificationService$RiskFactor");
        var constructor = riskFactorClass
            .getDeclaredConstructor(String.class, String.class, int.class, String.class, String.class);
        constructor.setAccessible(true);
        Object riskFactor = constructor.newInstance(
            "Housing Instability",
            "social-determinants",
            10,
            "high",
            "SDOH");

        List<Object> riskFactors = new ArrayList<>();
        riskFactors.add(riskFactor);

        List<String> recommendations = ReflectionTestUtils.invokeMethod(
            service,
            "generateRecommendations",
            riskFactors,
            RiskAssessmentEntity.RiskLevel.MODERATE
        );

        assertThat(recommendations)
            .contains("Connect with community health worker", "Screen for additional social needs");
    }

    @Test
    void shouldCalculateConditionWeightForModerateSeverity() {
        Condition condition = new Condition();
        condition.setSeverity(new CodeableConcept().addCoding(new Coding().setCode("moderate")));

        int weight = ReflectionTestUtils.invokeMethod(
            service, "calculateConditionWeight", "Asthma", condition);

        assertThat(weight).isEqualTo(20);
    }

    @Test
    void shouldDefaultSeverityWhenMissing() {
        Condition condition = new Condition();

        String severity = ReflectionTestUtils.invokeMethod(
            service, "extractSeverityFromCondition", condition);

        assertThat(severity).isEqualTo("moderate");
    }

    @Test
    void shouldSkipOldMentalHealthAssessments() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-old";

        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .type(MentalHealthAssessmentEntity.AssessmentType.PHQ_9)
            .severity("Severe")
            .score(20)
            .maxScore(27)
            .assessmentDate(Instant.now().minus(200, ChronoUnit.DAYS))
            .build();

        when(mentalHealthAssessmentRepository.findPositiveScreensRequiringFollowup(tenantId, patientId))
            .thenReturn(List.of(assessment));

        @SuppressWarnings("unchecked")
        List<?> factors = (List<?>) ReflectionTestUtils.invokeMethod(
            service, "analyzeMentalHealthScreenings", tenantId, patientId);

        assertThat(factors).isEmpty();
    }

    @Test
    void shouldDetermineRiskLevelThresholds() {
        assertThat((RiskAssessmentEntity.RiskLevel) ReflectionTestUtils.invokeMethod(
            service, "determineRiskLevel", 80))
            .isEqualTo(RiskAssessmentEntity.RiskLevel.VERY_HIGH);
        assertThat((RiskAssessmentEntity.RiskLevel) ReflectionTestUtils.invokeMethod(
            service, "determineRiskLevel", 60))
            .isEqualTo(RiskAssessmentEntity.RiskLevel.HIGH);
        assertThat((RiskAssessmentEntity.RiskLevel) ReflectionTestUtils.invokeMethod(
            service, "determineRiskLevel", 30))
            .isEqualTo(RiskAssessmentEntity.RiskLevel.MODERATE);
        assertThat((RiskAssessmentEntity.RiskLevel) ReflectionTestUtils.invokeMethod(
            service, "determineRiskLevel", 10))
            .isEqualTo(RiskAssessmentEntity.RiskLevel.LOW);
    }

    @Test
    void shouldCapTotalRiskScore() throws Exception {
        List<Object> factors = new ArrayList<>();
        factors.add(newRiskFactor("A", "mental-health", 60));
        factors.add(newRiskFactor("B", "chronic-disease", 60));

        int total = (int) ReflectionTestUtils.invokeMethod(
            service, "calculateTotalRiskScore", factors);

        assertThat(total).isEqualTo(100);
    }

    @Test
    void shouldGeneratePredictedOutcomesForLowRisk() {
        List<?> outcomes = (List<?>) ReflectionTestUtils.invokeMethod(
            service, "generatePredictedOutcomes", List.of(), RiskAssessmentEntity.RiskLevel.LOW);

        assertThat(outcomes).hasSize(2);
    }

    @Test
    void shouldGenerateRecommendationsForModerateAndCategories() throws Exception {
        List<Object> factors = new ArrayList<>();
        factors.add(newRiskFactor("Risk", "mental-health", 10));
        factors.add(newRiskFactor("Risk2", "chronic-disease", 10));
        factors.add(newRiskFactor("Risk3", "chronic-disease", 10));

        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) ReflectionTestUtils.invokeMethod(
            service, "generateRecommendations", factors, RiskAssessmentEntity.RiskLevel.MODERATE);

        assertThat(recommendations).contains(
            "Schedule care plan review within 30 days",
            "Implement monthly check-ins",
            "Address all urgent and high-priority care gaps",
            "Refer to behavioral health specialist",
            "Implement mental health monitoring protocol",
            "Refer to chronic disease management program"
        );
    }

    @Test
    void shouldMapEntityToDto() {
        RiskAssessmentEntity entity = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .patientId(UUID.randomUUID())
            .riskScore(42)
            .riskLevel(RiskAssessmentEntity.RiskLevel.HIGH)
            .riskFactors(List.of(Map.of(
                "factor", "Test",
                "category", "mental-health",
                "weight", 10,
                "severity", "high",
                "evidence", "evidence"
            )))
            .predictedOutcomes(List.of(Map.of(
                "outcome", "Hospital admission",
                "probability", 0.25,
                "timeframe", "next 90 days"
            )))
            .recommendations(List.of("Do something"))
            .assessmentDate(Instant.now())
            .createdAt(Instant.now())
            .build();

        RiskAssessmentDTO dto = ReflectionTestUtils.invokeMethod(service, "mapToDTO", entity);

        assertThat(dto.getRiskLevel()).isEqualTo("high");
        assertThat(dto.getRiskFactors()).hasSize(1);
        assertThat(dto.getPredictedOutcomes()).hasSize(1);
    }

    private Condition condition(String name, String severity, Instant onset) {
        Condition condition = new Condition();
        condition.setCode(new CodeableConcept()
            .setText(name)
            .addCoding(new Coding().setDisplay(name)));
        if (severity != null) {
            condition.setSeverity(new CodeableConcept()
                .addCoding(new Coding().setCode(severity)));
        }
        condition.setOnset(new DateTimeType(java.util.Date.from(onset)));
        return condition;
    }

    private Observation observation(String loinc, double value, String unit, String display, Instant effective) {
        Observation observation = new Observation();
        observation.setCode(new CodeableConcept()
            .setText(display)
            .addCoding(new Coding().setSystem("http://loinc.org").setCode(loinc).setDisplay(display)));
        observation.setValue(new Quantity().setValue(value).setUnit(unit));
        observation.setEffective(new DateTimeType(java.util.Date.from(effective)));
        return observation;
    }

    private Encounter encounter(String encounterClass, Instant start) {
        Encounter encounter = new Encounter();
        encounter.setClass_(new Coding().setCode(encounterClass));
        encounter.setPeriod(new Period().setStart(java.util.Date.from(start)));
        return encounter;
    }

    private Object newRiskFactor(String factor, String category, int weight) throws Exception {
        Class<?> riskFactorClass = Class.forName(
            "com.healthdata.quality.service.RiskStratificationService$RiskFactor");
        var constructor = riskFactorClass
            .getDeclaredConstructor(String.class, String.class, int.class, String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(factor, category, weight, "moderate", "evidence");
    }

    private static String getRiskFactorValue(Object riskFactor, String accessor) throws Exception {
        var method = riskFactor.getClass().getDeclaredMethod(accessor);
        method.setAccessible(true);
        return (String) method.invoke(riskFactor);
    }

    private static ICriterion<?> anyCriterion() {
        return any(ICriterion.class);
    }
}
