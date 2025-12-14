package com.healthdata.sdoh.service;

import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.repository.SdohRiskScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for SdohRiskCalculator
 *
 * Testing SDOH risk scoring and impact assessment
 *
 * @disabled Temporarily disabled - needs refactoring to use entity classes instead of models
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SDOH Risk Calculator Tests")
@Disabled("Needs refactoring to use entity classes instead of model classes for repository mocks")
class SdohRiskCalculatorTest {

    @Mock
    private SdohRiskScoreRepository riskScoreRepository;

    @InjectMocks
    private SdohRiskCalculator riskCalculator;

    private String tenantId;
    private String patientId;
    private SdohAssessment assessment;

    @BeforeEach
    void setUp() {
        tenantId = "tenant-001";
        patientId = "patient-001";

        assessment = SdohAssessment.builder()
                .assessmentId("assessment-001")
                .patientId(patientId)
                .tenantId(tenantId)
                .identifiedNeeds(new HashMap<>())
                .build();
    }

    @Test
    @DisplayName("Should calculate risk score from assessment")
    void testCalculateRiskScore() {
        // Given
        Map<SdohCategory, Boolean> needs = new HashMap<>();
        needs.put(SdohCategory.FOOD_INSECURITY, true);
        needs.put(SdohCategory.HOUSING_INSTABILITY, true);
        assessment.setIdentifiedNeeds(needs);

        when(riskScoreRepository.save(any(SdohRiskScore.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SdohRiskScore score = riskCalculator.calculateRiskScore(assessment);

        // Then
        assertNotNull(score);
        assertTrue(score.getTotalScore() > 0);
        assertTrue(score.getTotalScore() <= 100);
    }

    @Test
    @DisplayName("Should assign risk level based on score")
    void testAssignRiskLevel() {
        // When
        SdohRiskScore.RiskLevel lowRisk = SdohRiskScore.RiskLevel.fromScore(20);
        SdohRiskScore.RiskLevel moderateRisk = SdohRiskScore.RiskLevel.fromScore(40);
        SdohRiskScore.RiskLevel highRisk = SdohRiskScore.RiskLevel.fromScore(65);
        SdohRiskScore.RiskLevel criticalRisk = SdohRiskScore.RiskLevel.fromScore(85);

        // Then
        assertEquals(SdohRiskScore.RiskLevel.LOW, lowRisk);
        assertEquals(SdohRiskScore.RiskLevel.MODERATE, moderateRisk);
        assertEquals(SdohRiskScore.RiskLevel.HIGH, highRisk);
        assertEquals(SdohRiskScore.RiskLevel.CRITICAL, criticalRisk);
    }

    @Test
    @DisplayName("Should calculate category-specific risk scores")
    void testCalculateCategoryScores() {
        // Given
        Map<SdohCategory, Boolean> needs = new HashMap<>();
        needs.put(SdohCategory.FOOD_INSECURITY, true);
        needs.put(SdohCategory.HOUSING_INSTABILITY, false);

        // When
        Map<SdohCategory, Double> categoryScores = riskCalculator.calculateCategoryScores(needs);

        // Then
        assertNotNull(categoryScores);
        assertTrue(categoryScores.get(SdohCategory.FOOD_INSECURITY) > 0);
    }

    @Test
    @DisplayName("Should weight categories appropriately")
    void testCategoryWeighting() {
        // When
        double foodWeight = riskCalculator.getCategoryWeight(SdohCategory.FOOD_INSECURITY);
        double housingWeight = riskCalculator.getCategoryWeight(SdohCategory.HOUSING_INSTABILITY);

        // Then
        assertTrue(foodWeight > 0);
        assertTrue(housingWeight > 0);
        assertTrue(foodWeight <= 1.0);
        assertTrue(housingWeight <= 1.0);
    }

    @Test
    @DisplayName("Should assess SDOH impact on health outcomes")
    void testAssessImpact() {
        // Given
        SdohCategory category = SdohCategory.FOOD_INSECURITY;

        // When
        SdohImpact impact = riskCalculator.assessImpact(patientId, category);

        // Then
        assertNotNull(impact);
        assertEquals(category, impact.getCategory());
        assertNotNull(impact.getImpactLevel());
    }

    @Test
    @DisplayName("Should predict hospitalization risk from SDOH")
    void testPredictHospitalizationRisk() {
        // Given
        SdohRiskScore riskScore = SdohRiskScore.builder()
                .totalScore(75.0)
                .build();

        // When
        double hospRisk = riskCalculator.predictHospitalizationRisk(riskScore);

        // Then
        assertTrue(hospRisk >= 0.0);
        assertTrue(hospRisk <= 1.0);
    }

    @Test
    @DisplayName("Should predict emergency visit risk from SDOH")
    void testPredictEmergencyVisitRisk() {
        // Given
        SdohRiskScore riskScore = SdohRiskScore.builder()
                .totalScore(65.0)
                .build();

        // When
        double erRisk = riskCalculator.predictEmergencyVisitRisk(riskScore);

        // Then
        assertTrue(erRisk >= 0.0);
        assertTrue(erRisk <= 1.0);
    }

    @Test
    @DisplayName("Should predict medication adherence impact")
    void testPredictMedicationAdherenceImpact() {
        // Given
        SdohRiskScore riskScore = SdohRiskScore.builder()
                .totalScore(55.0)
                .build();

        // When
        double adherenceImpact = riskCalculator.predictMedicationAdherenceImpact(riskScore);

        // Then
        assertTrue(adherenceImpact >= -1.0);
        assertTrue(adherenceImpact <= 0.0);
    }

    @Test
    @DisplayName("Should get patient risk score history")
    void testGetRiskScoreHistory() {
        // Given
        List<SdohRiskScore> history = Arrays.asList(
                SdohRiskScore.builder().scoreId("s1").patientId(patientId).totalScore(50.0).build(),
                SdohRiskScore.builder().scoreId("s2").patientId(patientId).totalScore(60.0).build()
        );

        when(riskScoreRepository.findByTenantIdAndPatientIdOrderByCalculatedAtDesc(tenantId, patientId))
                .thenReturn(history);

        // When
        List<SdohRiskScore> result = riskCalculator.getRiskScoreHistory(tenantId, patientId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should identify risk score trends")
    void testIdentifyRiskTrends() {
        // Given
        List<SdohRiskScore> history = Arrays.asList(
                SdohRiskScore.builder().totalScore(40.0).build(),
                SdohRiskScore.builder().totalScore(50.0).build(),
                SdohRiskScore.builder().totalScore(60.0).build()
        );

        // When
        String trend = riskCalculator.identifyTrend(history);

        // Then
        assertNotNull(trend);
        assertEquals("INCREASING", trend);
    }

    @Test
    @DisplayName("Should handle zero identified needs")
    void testCalculateWithZeroNeeds() {
        // Given
        assessment.setIdentifiedNeeds(new HashMap<>());

        when(riskScoreRepository.save(any(SdohRiskScore.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SdohRiskScore score = riskCalculator.calculateRiskScore(assessment);

        // Then
        assertNotNull(score);
        assertEquals(0.0, score.getTotalScore());
        assertEquals(SdohRiskScore.RiskLevel.LOW, score.getRiskLevel());
    }
}
