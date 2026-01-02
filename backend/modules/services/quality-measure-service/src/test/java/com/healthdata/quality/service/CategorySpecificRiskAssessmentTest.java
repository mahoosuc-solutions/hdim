package com.healthdata.quality.service;

import com.healthdata.quality.dto.RiskAssessmentDTO;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for Category-Specific Risk Assessment (Phase 4)
 *
 * Tests risk assessment by category:
 * - CARDIOVASCULAR: Blood pressure, cholesterol, smoking status
 * - DIABETES: HbA1c, medication adherence, care gaps
 * - RESPIRATORY: Oxygen saturation, spirometry, exacerbations
 * - MENTAL_HEALTH: PHQ-9/GAD-7 scores, crisis events
 */
@ExtendWith(MockitoExtension.class)
class CategorySpecificRiskAssessmentTest {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CategorySpecificRiskService categoryRiskService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("33333333-4444-5555-6666-777777777777");

    @BeforeEach
    void setUp() {
        reset(riskAssessmentRepository, kafkaTemplate);
    }

    @Test
    void testCardiovascularRiskCalculation_HighRisk() {
        // Given: Patient with high BP, high cholesterol, smoking
        Map<String, Object> patientData = Map.of(
            "systolicBP", 165.0,
            "diastolicBP", 95.0,
            "ldlCholesterol", 195.0,
            "hdlCholesterol", 35.0,
            "smokingStatus", "current-smoker",
            "bmi", 32.5,
            "age", 62
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Calculate cardiovascular risk
        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "CARDIOVASCULAR", patientData);

        // Then: Should be HIGH or CRITICAL risk
        assertThat(result).isNotNull();
        assertThat(result.getRiskCategory()).isEqualTo("CARDIOVASCULAR");
        assertThat(result.getRiskScore()).isGreaterThan(50);
        assertThat(result.getRiskLevel()).isIn("high", "very-high");

        // Should have risk factors identified
        assertThat(result.getRiskFactors()).isNotEmpty();
        assertThat(result.getRiskFactors())
            .extracting(RiskAssessmentDTO.RiskFactorDTO::getFactor)
            .contains("Uncontrolled Hypertension", "Elevated LDL Cholesterol", "Active Smoking");

        // Verify save was called
        verify(riskAssessmentRepository).save(any(RiskAssessmentEntity.class));
    }

    @Test
    void testDiabetesRiskCalculation_UncontrolledWithGaps() {
        // Given: Patient with uncontrolled diabetes and care gaps
        Map<String, Object> patientData = Map.of(
            "hba1c", 9.5,
            "glucoseControl", "poor",
            "medicationAdherence", 0.45, // 45% adherence
            "openCareGaps", 3,
            "lastRetinalExam", "2021-06-15", // Overdue
            "lastFootExam", "2022-01-10" // Overdue
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Calculate diabetes risk
        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "DIABETES", patientData);

        // Then: Should be HIGH risk due to poor control and gaps
        assertThat(result).isNotNull();
        assertThat(result.getRiskCategory()).isEqualTo("DIABETES");
        assertThat(result.getRiskScore()).isGreaterThan(60);
        assertThat(result.getRiskLevel()).isIn("high", "very-high");

        // Should identify specific risk factors
        assertThat(result.getRiskFactors())
            .extracting(RiskAssessmentDTO.RiskFactorDTO::getCategory)
            .contains("chronic-disease", "medication-adherence", "care-gaps");
    }

    @Test
    void testRespiratoryRiskCalculation_COPDWithExacerbations() {
        // Given: COPD patient with recent exacerbations
        Map<String, Object> patientData = Map.of(
            "diagnosis", "COPD",
            "oxygenSaturation", 88.0, // Low
            "fev1Percent", 45.0, // Moderate-severe COPD
            "exacerbationsLast12Months", 3,
            "recentHospitalization", true,
            "smokingStatus", "former-smoker",
            "packYears", 30
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Calculate respiratory risk
        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "RESPIRATORY", patientData);

        // Then: Should be HIGH risk
        assertThat(result).isNotNull();
        assertThat(result.getRiskCategory()).isEqualTo("RESPIRATORY");
        assertThat(result.getRiskScore()).isGreaterThan(55);

        // Should identify exacerbation risk
        assertThat(result.getRiskFactors())
            .anyMatch(rf -> rf.getFactor().contains("Frequent Exacerbations"));
    }

    @Test
    void testMentalHealthRiskCalculation_SevereDepression() {
        // Given: Patient with severe depression and anxiety
        Map<String, Object> patientData = Map.of(
            "phq9Score", 22, // Severe depression
            "gad7Score", 18, // Severe anxiety
            "recentCrisisEvent", true,
            "medicationCompliance", 0.60,
            "therapyEngagement", "poor",
            "suicidalIdeation", false,
            "supportSystem", "limited"
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Calculate mental health risk
        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "MENTAL_HEALTH", patientData);

        // Then: Should be CRITICAL risk
        assertThat(result).isNotNull();
        assertThat(result.getRiskCategory()).isEqualTo("MENTAL_HEALTH");
        assertThat(result.getRiskScore()).isGreaterThan(70);
        assertThat(result.getRiskLevel()).isEqualTo("very-high");

        // Should include crisis intervention recommendation
        assertThat(result.getRecommendations())
            .anyMatch(rec -> rec.contains("crisis") || rec.contains("intensive"));
    }

    @Test
    void testMentalHealthRiskCalculation_LowRisk() {
        Map<String, Object> patientData = Map.of(
            "phq9Score", 2,
            "gad7Score", 1,
            "recentCrisisEvent", false
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "MENTAL_HEALTH", patientData);

        assertThat(result.getRiskLevel()).isEqualTo("low");
        assertThat(result.getPredictedOutcomes()).hasSize(1);
    }

    @Test
    void testMentalHealthRiskCalculation_HighRiskRecommendations() {
        Map<String, Object> patientData = Map.of(
            "phq9Score", 15,
            "gad7Score", 15,
            "medicationCompliance", 0.60
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "MENTAL_HEALTH", patientData);

        assertThat(result.getRiskLevel()).isEqualTo("high");
        assertThat(result.getRecommendations())
            .anyMatch(rec -> rec.contains("behavioral health"));
    }

    @Test
    void testCardiovascularRiskCalculation_LowRisk() {
        // Given: Patient with good control and healthy lifestyle
        Map<String, Object> patientData = Map.of(
            "systolicBP", 118.0,
            "diastolicBP", 75.0,
            "ldlCholesterol", 85.0,
            "hdlCholesterol", 55.0,
            "smokingStatus", "never-smoker",
            "bmi", 23.5,
            "age", 45,
            "exerciseMinutesPerWeek", 180
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Calculate cardiovascular risk
        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "CARDIOVASCULAR", patientData);

        // Then: Should be LOW risk
        assertThat(result).isNotNull();
        assertThat(result.getRiskScore()).isLessThan(25);
        assertThat(result.getRiskLevel()).isEqualTo("low");
    }

    @Test
    void testCardiovascularRiskCalculation_ModerateRisk() {
        Map<String, Object> patientData = Map.of(
            "systolicBP", 142.0,
            "hdlCholesterol", 38.0,
            "age", 68
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "CARDIOVASCULAR", patientData);

        assertThat(result.getRiskLevel()).isEqualTo("moderate");
        assertThat(result.getPredictedOutcomes()).hasSize(1);
    }

    @Test
    void testDiabetesRiskCalculation_WellControlled() {
        // Given: Patient with well-controlled diabetes
        Map<String, Object> patientData = Map.of(
            "hba1c", 6.8,
            "glucoseControl", "good",
            "medicationAdherence", 0.95,
            "openCareGaps", 0,
            "lastRetinalExam", "2024-10-15",
            "lastFootExam", "2024-11-01"
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Calculate diabetes risk
        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "DIABETES", patientData);

        // Then: Should be LOW or MODERATE risk
        assertThat(result).isNotNull();
        assertThat(result.getRiskScore()).isLessThan(50);
        assertThat(result.getRiskLevel()).isIn("low", "moderate");
    }

    @Test
    void testDiabetesRiskCalculation_ModerateRisk() {
        Map<String, Object> patientData = Map.of(
            "hba1c", 7.4,
            "medicationAdherence", 0.75,
            "openCareGaps", 1
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "DIABETES", patientData);

        assertThat(result.getRiskLevel()).isEqualTo("moderate");
        assertThat(result.getPredictedOutcomes()).hasSize(1);
    }

    @Test
    void testDiabetesRiskCalculation_WithTwoCareGaps() {
        Map<String, Object> patientData = Map.of(
            "hba1c", 7.8,
            "openCareGaps", 2
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "DIABETES", patientData);

        assertThat(result.getRiskFactors())
            .anyMatch(rf -> rf.getFactor().contains("Open Care Gaps"));
    }

    @Test
    void testDiabetesRiskCalculation_RecommendationsForAdherenceAndGaps() {
        Map<String, Object> patientData = Map.of(
            "hba1c", 9.2,
            "medicationAdherence", 0.55,
            "openCareGaps", 3
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "DIABETES", patientData);

        assertThat(result.getRecommendations())
            .anyMatch(rec -> rec.contains("adherence"));
        assertThat(result.getRecommendations())
            .anyMatch(rec -> rec.contains("care gaps"));
    }

    @Test
    void testRespiratoryRiskCalculation_RecommendationsForExacerbations() {
        Map<String, Object> patientData = Map.of(
            "oxygenSaturation", 92.0,
            "fev1Percent", 60.0,
            "exacerbationsLast12Months", 2
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "RESPIRATORY", patientData);

        assertThat(result.getRecommendations())
            .anyMatch(rec -> rec.contains("inhaler"));
    }

    @Test
    void testCardiovascularRiskCalculation_WithNonNumericValues() {
        Map<String, Object> patientData = Map.of(
            "systolicBP", "bad",
            "age", "bad"
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        RiskAssessmentDTO result = categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "CARDIOVASCULAR", patientData);

        assertThat(result.getRiskScore()).isEqualTo(0);
        assertThat(result.getRiskFactors()).isEmpty();
    }

    @Test
    void testRecalculateAllRisks_MultipleCategories() {
        // Given: Patient with data for multiple risk categories
        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Recalculate all risks
        List<RiskAssessmentDTO> results = categoryRiskService.recalculateAllRisks(
            TENANT_ID, PATIENT_ID);

        // Then: Should return assessments for all categories
        assertThat(results).hasSize(4);
        assertThat(results)
            .extracting(RiskAssessmentDTO::getRiskCategory)
            .containsExactlyInAnyOrder("CARDIOVASCULAR", "DIABETES", "RESPIRATORY", "MENTAL_HEALTH");

        // Should have saved all assessments
        verify(riskAssessmentRepository, times(4)).save(any(RiskAssessmentEntity.class));
    }

    @Test
    void testDetectDeterioration_CardiovascularWorsening() {
        // Given: Previous cardiovascular risk was MODERATE, now HIGH
        RiskAssessmentEntity previousAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskCategory("CARDIOVASCULAR")
            .riskLevel(RiskAssessmentEntity.RiskLevel.MODERATE)
            .riskScore(40)
            .assessmentDate(Instant.now().minusSeconds(86400 * 90))
            .riskFactors(List.of())
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .build();

        when(riskAssessmentRepository.findLatestByCategoryAndPatient(
            TENANT_ID, PATIENT_ID, "CARDIOVASCULAR"))
            .thenReturn(Optional.of(previousAssessment));

        RiskAssessmentEntity currentAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskCategory("CARDIOVASCULAR")
            .riskLevel(RiskAssessmentEntity.RiskLevel.HIGH)
            .riskScore(65)
            .assessmentDate(Instant.now())
            .riskFactors(List.of())
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .build();

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenReturn(currentAssessment);

        // When: Detect deterioration
        boolean hasDeterioration = categoryRiskService.detectDeterioration(
            TENANT_ID, PATIENT_ID, "CARDIOVASCULAR");

        // Then: Should detect deterioration
        assertThat(hasDeterioration).isTrue();

        // Should publish deterioration event
        verify(kafkaTemplate).send(eq("patient-risk.escalated"), argThat(event -> {
            if (event instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> eventMap = (Map<String, Object>) event;
                return eventMap.get("patientId").equals(PATIENT_ID) &&
                       eventMap.get("riskCategory").equals("CARDIOVASCULAR") &&
                       eventMap.get("previousLevel").equals("MODERATE") &&
                       eventMap.get("newLevel").equals("HIGH");
            }
            return false;
        }));
    }

    @Test
    void testDetectDeterioration_NoPreviousAssessment() {
        when(riskAssessmentRepository.findLatestByCategoryAndPatient(
            TENANT_ID, PATIENT_ID, "MENTAL_HEALTH"))
            .thenReturn(Optional.empty());

        boolean hasDeterioration = categoryRiskService.detectDeterioration(
            TENANT_ID, PATIENT_ID, "MENTAL_HEALTH");

        assertThat(hasDeterioration).isFalse();
        verify(kafkaTemplate, never()).send(eq("patient-risk.escalated"), any());
    }

    @Test
    void testDetectDeterioration_NoIncrease() {
        RiskAssessmentEntity previousAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskCategory("RESPIRATORY")
            .riskLevel(RiskAssessmentEntity.RiskLevel.VERY_HIGH)
            .riskScore(90)
            .assessmentDate(Instant.now().minusSeconds(86400))
            .riskFactors(List.of())
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .build();

        when(riskAssessmentRepository.findLatestByCategoryAndPatient(
            TENANT_ID, PATIENT_ID, "RESPIRATORY"))
            .thenReturn(Optional.of(previousAssessment));
        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        boolean hasDeterioration = categoryRiskService.detectDeterioration(
            TENANT_ID, PATIENT_ID, "RESPIRATORY");

        assertThat(hasDeterioration).isFalse();
        verify(kafkaTemplate, never()).send(eq("patient-risk.escalated"), any());
    }

    @Test
    void testCalculateCategoryRisk_UnknownCategory() {
        assertThatThrownBy(() -> categoryRiskService.calculateCategoryRisk(
            TENANT_ID, PATIENT_ID, "UNKNOWN", Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown risk category");
    }

    @Test
    void testMultiTenantIsolation_CategoryRisk() {
        // Given: Two different tenants
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";

        Map<String, Object> patientData = Map.of(
            "hba1c", 8.5,
            "medicationAdherence", 0.75,
            "openCareGaps", 2
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Calculate risk for both tenants
        RiskAssessmentDTO result1 = categoryRiskService.calculateCategoryRisk(
            tenant1, PATIENT_ID, "DIABETES", patientData);
        RiskAssessmentDTO result2 = categoryRiskService.calculateCategoryRisk(
            tenant2, PATIENT_ID, "DIABETES", patientData);

        // Then: Both should have assessments but isolated
        ArgumentCaptor<RiskAssessmentEntity> captor = ArgumentCaptor.forClass(RiskAssessmentEntity.class);
        verify(riskAssessmentRepository, times(2)).save(captor.capture());

        List<RiskAssessmentEntity> saved = captor.getAllValues();
        assertThat(saved).extracting(RiskAssessmentEntity::getTenantId)
            .containsExactly(tenant1, tenant2);
    }
}
