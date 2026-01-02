package com.healthdata.quality.service;

import com.healthdata.quality.dto.RiskAssessmentDTO;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategorySpecificRiskServiceTest {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private CategorySpecificRiskService service;

    @BeforeEach
    void setUp() {
        service = new CategorySpecificRiskService(riskAssessmentRepository, kafkaTemplate);
        lenient().when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class))).thenAnswer(invocation -> {
            RiskAssessmentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
    }

    @Test
    void shouldCalculateCardiovascularRiskVeryHigh() {
        UUID patientId = UUID.randomUUID();

        RiskAssessmentDTO dto = service.calculateCategoryRisk(
            "tenant-1",
            patientId,
            "CARDIOVASCULAR",
            Map.of(
                "systolicBP", 170.0,
                "ldlCholesterol", 200.0,
                "hdlCholesterol", 35.0,
                "smokingStatus", "current-smoker",
                "bmi", 32.0,
                "age", 70
            )
        );

        assertThat(dto.getRiskLevel()).isEqualTo("very-high");
        assertThat(dto.getRecommendations()).contains(
            "Refer to cardiology for risk assessment",
            "Optimize antihypertensive therapy",
            "Consider statin therapy or dose adjustment",
            "Enroll in smoking cessation program"
        );
    }

    @Test
    void shouldCalculateDiabetesRiskModerate() {
        UUID patientId = UUID.randomUUID();

        RiskAssessmentDTO dto = service.calculateCategoryRisk(
            "tenant-1",
            patientId,
            "DIABETES",
            Map.of(
                "hba1c", 8.5,
                "medicationAdherence", 0.7,
                "openCareGaps", 2
            )
        );

        assertThat(dto.getRiskLevel()).isEqualTo("moderate");
        assertThat(dto.getRecommendations()).contains(
            "Address medication adherence barriers",
            "Consider simplified medication regimen",
            "Schedule appointments to close care gaps",
            "Ensure annual retinal and foot exams are completed"
        );
    }

    @Test
    void shouldCalculateRespiratoryRiskHigh() {
        UUID patientId = UUID.randomUUID();

        RiskAssessmentDTO dto = service.calculateCategoryRisk(
            "tenant-1",
            patientId,
            "RESPIRATORY",
            Map.of(
                "oxygenSaturation", 88.0,
                "fev1Percent", 45.0,
                "exacerbationsLast12Months", 3,
                "recentHospitalization", true
            )
        );

        assertThat(dto.getRiskLevel()).isEqualTo("high");
        assertThat(dto.getRecommendations()).contains(
            "Refer to pulmonology for evaluation",
            "Develop COPD/asthma action plan",
            "Review and optimize inhaler technique"
        );
    }

    @Test
    void shouldCalculateMentalHealthRiskVeryHigh() {
        UUID patientId = UUID.randomUUID();

        RiskAssessmentDTO dto = service.calculateCategoryRisk(
            "tenant-1",
            patientId,
            "MENTAL_HEALTH",
            Map.of(
                "phq9Score", 22,
                "gad7Score", 16,
                "recentCrisisEvent", true,
                "medicationCompliance", 0.6
            )
        );

        assertThat(dto.getRiskLevel()).isEqualTo("very-high");
        assertThat(dto.getRecommendations()).contains(
            "Urgent psychiatric evaluation required",
            "Develop safety plan with patient",
            "Consider medication adjustment or therapy intensification"
        );
    }

    @Test
    void shouldRecalculateAllRisks() {
        UUID patientId = UUID.randomUUID();

        List<RiskAssessmentDTO> results = service.recalculateAllRisks("tenant-1", patientId);

        assertThat(results).hasSize(4);
        verify(riskAssessmentRepository, times(4)).save(any(RiskAssessmentEntity.class));
    }

    @Test
    void shouldDetectDeteriorationAndPublishEvent() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        RiskAssessmentEntity previous = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .riskCategory("CARDIOVASCULAR")
            .riskLevel(RiskAssessmentEntity.RiskLevel.LOW)
            .riskScore(10)
            .assessmentDate(Instant.now())
            .build();

        when(riskAssessmentRepository.findLatestByCategoryAndPatient(tenantId, patientId, "CARDIOVASCULAR"))
            .thenReturn(Optional.of(previous));

        boolean result = service.detectDeterioration(tenantId, patientId, "CARDIOVASCULAR");

        assertThat(result).isTrue();
        verify(kafkaTemplate).send(eq("patient-risk.escalated"), any());
    }

    @Test
    void shouldReturnFalseWhenNoPreviousAssessment() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(riskAssessmentRepository.findLatestByCategoryAndPatient(tenantId, patientId, "DIABETES"))
            .thenReturn(Optional.empty());

        boolean result = service.detectDeterioration(tenantId, patientId, "DIABETES");

        assertThat(result).isFalse();
        verify(riskAssessmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownCategory() {
        assertThatThrownBy(() -> service.calculateCategoryRisk(
            "tenant-1",
            UUID.randomUUID(),
            "UNKNOWN",
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDetermineRiskLevelsAtThresholds() {
        assertThat((RiskAssessmentEntity.RiskLevel) ReflectionTestUtils.invokeMethod(
            service, "determineRiskLevel", 80)).isEqualTo(RiskAssessmentEntity.RiskLevel.VERY_HIGH);
        assertThat((RiskAssessmentEntity.RiskLevel) ReflectionTestUtils.invokeMethod(
            service, "determineRiskLevel", 60)).isEqualTo(RiskAssessmentEntity.RiskLevel.HIGH);
        assertThat((RiskAssessmentEntity.RiskLevel) ReflectionTestUtils.invokeMethod(
            service, "determineRiskLevel", 30)).isEqualTo(RiskAssessmentEntity.RiskLevel.MODERATE);
        assertThat((RiskAssessmentEntity.RiskLevel) ReflectionTestUtils.invokeMethod(
            service, "determineRiskLevel", 10)).isEqualTo(RiskAssessmentEntity.RiskLevel.LOW);
    }

    @Test
    void shouldGenerateOutcomesForAllLevels() {
        assertThat((List<?>) ReflectionTestUtils.invokeMethod(
            service, "generateCardiovascularOutcomes", RiskAssessmentEntity.RiskLevel.VERY_HIGH))
            .hasSize(2);
        assertThat((List<?>) ReflectionTestUtils.invokeMethod(
            service, "generateCardiovascularOutcomes", RiskAssessmentEntity.RiskLevel.LOW))
            .hasSize(1);

        assertThat((List<?>) ReflectionTestUtils.invokeMethod(
            service, "generateDiabetesOutcomes", RiskAssessmentEntity.RiskLevel.HIGH))
            .hasSize(2);
        assertThat((List<?>) ReflectionTestUtils.invokeMethod(
            service, "generateDiabetesOutcomes", RiskAssessmentEntity.RiskLevel.MODERATE))
            .hasSize(1);

        assertThat((List<?>) ReflectionTestUtils.invokeMethod(
            service, "generateRespiratoryOutcomes", RiskAssessmentEntity.RiskLevel.VERY_HIGH))
            .hasSize(2);
        assertThat((List<?>) ReflectionTestUtils.invokeMethod(
            service, "generateRespiratoryOutcomes", RiskAssessmentEntity.RiskLevel.LOW))
            .hasSize(1);

        assertThat((List<?>) ReflectionTestUtils.invokeMethod(
            service, "generateMentalHealthOutcomes", RiskAssessmentEntity.RiskLevel.HIGH))
            .hasSize(2);
        assertThat((List<?>) ReflectionTestUtils.invokeMethod(
            service, "generateMentalHealthOutcomes", RiskAssessmentEntity.RiskLevel.MODERATE))
            .hasSize(1);
    }

    @Test
    void shouldGenerateRecommendationsBasedOnRiskFactors() {
        List<Object> cardioFactors = new ArrayList<>();
        cardioFactors.add(riskFactor("Hypertension", "cardiovascular", "moderate"));
        cardioFactors.add(riskFactor("Cholesterol", "cardiovascular", "moderate"));
        cardioFactors.add(riskFactor("Smoking", "lifestyle", "high"));

        @SuppressWarnings("unchecked")
        List<String> cardioRecommendations = (List<String>) ReflectionTestUtils.invokeMethod(
            service,
            "generateCardiovascularRecommendations",
            cardioFactors,
            RiskAssessmentEntity.RiskLevel.HIGH
        );

        assertThat(cardioRecommendations).contains(
            "Refer to cardiology for risk assessment",
            "Optimize antihypertensive therapy",
            "Consider statin therapy or dose adjustment",
            "Enroll in smoking cessation program"
        );

        @SuppressWarnings("unchecked")
        List<String> lowCardioRecommendations = (List<String>) ReflectionTestUtils.invokeMethod(
            service,
            "generateCardiovascularRecommendations",
            List.of(),
            RiskAssessmentEntity.RiskLevel.LOW
        );
        assertThat(lowCardioRecommendations).isEmpty();
    }

    @Test
    void shouldGenerateMentalHealthRecommendationsWithCrisis() {
        List<Object> factors = new ArrayList<>();
        factors.add(riskFactor("Recent Crisis Event", "mental-health", "critical"));
        factors.add(riskFactor("Severe Depression", "mental-health", "critical"));

        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) ReflectionTestUtils.invokeMethod(
            service,
            "generateMentalHealthRecommendations",
            factors,
            RiskAssessmentEntity.RiskLevel.VERY_HIGH
        );

        assertThat(recommendations).contains(
            "Urgent psychiatric evaluation required",
            "Develop safety plan with patient",
            "Consider medication adjustment or therapy intensification"
        );
    }

    private Object riskFactor(String factor, String category, String severity) {
        try {
            Class<?> riskFactorClass = Class.forName(
                "com.healthdata.quality.service.CategorySpecificRiskService$RiskFactor");
            var constructor = riskFactorClass
                .getDeclaredConstructor(String.class, String.class, int.class, String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(factor, category, 10, severity, "evidence");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create RiskFactor for test", e);
        }
    }

    @Test
    void shouldCompareRiskLevelsForDeterioration() {
        assertThat((boolean) ReflectionTestUtils.invokeMethod(
            service,
            "hasRiskLevelIncreased",
            RiskAssessmentEntity.RiskLevel.MODERATE,
            RiskAssessmentEntity.RiskLevel.HIGH
        )).isTrue();

        assertThat((boolean) ReflectionTestUtils.invokeMethod(
            service,
            "hasRiskLevelIncreased",
            RiskAssessmentEntity.RiskLevel.VERY_HIGH,
            RiskAssessmentEntity.RiskLevel.HIGH
        )).isFalse();
    }
}
