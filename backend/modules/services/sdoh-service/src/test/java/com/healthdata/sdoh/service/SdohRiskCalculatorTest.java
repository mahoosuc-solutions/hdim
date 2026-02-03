package com.healthdata.sdoh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.sdoh.entity.SdohRiskScoreEntity;
import com.healthdata.sdoh.model.SdohAssessment;
import com.healthdata.sdoh.model.SdohCategory;
import com.healthdata.sdoh.model.SdohRiskScore;
import com.healthdata.sdoh.repository.SdohRiskScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for SdohRiskCalculator
 *
 * Testing SDOH risk scoring, category weighting, risk predictions, and trend analysis.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("SDOH Risk Calculator Tests")
class SdohRiskCalculatorTest {

    @Mock
    private SdohRiskScoreRepository riskScoreRepository;

    private SdohRiskCalculator riskCalculator;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-001";
    private static final String PATIENT_ID = "patient-001";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        riskCalculator = new SdohRiskCalculator(riskScoreRepository, objectMapper);
    }

    @Nested
    @DisplayName("Category Score Calculation Tests")
    class CategoryScoreTests {

        @Test
        @DisplayName("Should calculate score for single category")
        void shouldCalculateScoreForSingleCategory() {
            // Given - single need identified
            Map<SdohCategory, Boolean> needs = new HashMap<>();
            needs.put(SdohCategory.FOOD_INSECURITY, true);

            // When
            Map<SdohCategory, Double> scores = riskCalculator.calculateCategoryScores(needs);

            // Then
            assertEquals(1, scores.size());
            assertEquals(0.15, scores.get(SdohCategory.FOOD_INSECURITY), 0.001);
        }

        @Test
        @DisplayName("Should calculate scores for multiple categories")
        void shouldCalculateScoresForMultipleCategories() {
            // Given - multiple needs identified
            Map<SdohCategory, Boolean> needs = new HashMap<>();
            needs.put(SdohCategory.FOOD_INSECURITY, true);      // 0.15
            needs.put(SdohCategory.HOUSING_INSTABILITY, true);  // 0.20
            needs.put(SdohCategory.TRANSPORTATION, true);       // 0.10

            // When
            Map<SdohCategory, Double> scores = riskCalculator.calculateCategoryScores(needs);

            // Then
            assertEquals(3, scores.size());
            assertEquals(0.15, scores.get(SdohCategory.FOOD_INSECURITY), 0.001);
            assertEquals(0.20, scores.get(SdohCategory.HOUSING_INSTABILITY), 0.001);
            assertEquals(0.10, scores.get(SdohCategory.TRANSPORTATION), 0.001);
        }

        @Test
        @DisplayName("Should ignore false needs")
        void shouldIgnoreFalseNeeds() {
            // Given - some needs false
            Map<SdohCategory, Boolean> needs = new HashMap<>();
            needs.put(SdohCategory.FOOD_INSECURITY, true);
            needs.put(SdohCategory.HOUSING_INSTABILITY, false);

            // When
            Map<SdohCategory, Double> scores = riskCalculator.calculateCategoryScores(needs);

            // Then
            assertEquals(1, scores.size());
            assertTrue(scores.containsKey(SdohCategory.FOOD_INSECURITY));
            assertFalse(scores.containsKey(SdohCategory.HOUSING_INSTABILITY));
        }

        @Test
        @DisplayName("Should return correct category weights")
        void shouldReturnCorrectCategoryWeights() {
            // Verify key weights from the static configuration
            assertEquals(0.15, riskCalculator.getCategoryWeight(SdohCategory.FOOD_INSECURITY), 0.001);
            assertEquals(0.20, riskCalculator.getCategoryWeight(SdohCategory.HOUSING_INSTABILITY), 0.001);
            assertEquals(0.10, riskCalculator.getCategoryWeight(SdohCategory.TRANSPORTATION), 0.001);
            assertEquals(0.15, riskCalculator.getCategoryWeight(SdohCategory.FINANCIAL_STRAIN), 0.001);
            assertEquals(0.12, riskCalculator.getCategoryWeight(SdohCategory.EMPLOYMENT), 0.001);
            assertEquals(0.08, riskCalculator.getCategoryWeight(SdohCategory.EDUCATION), 0.001);
            assertEquals(0.05, riskCalculator.getCategoryWeight(SdohCategory.UTILITIES), 0.001);
            assertEquals(0.07, riskCalculator.getCategoryWeight(SdohCategory.SOCIAL_ISOLATION), 0.001);
            assertEquals(0.08, riskCalculator.getCategoryWeight(SdohCategory.INTERPERSONAL_VIOLENCE), 0.001);
        }
    }

    @Nested
    @DisplayName("Risk Level Assignment Tests")
    class RiskLevelTests {

        @Test
        @DisplayName("Should assign LOW risk level for score 0-25")
        void shouldAssignLowRiskLevel() {
            // Risk level is determined by SdohRiskScore.RiskLevel.fromScore
            assertEquals(SdohRiskScore.RiskLevel.LOW, SdohRiskScore.RiskLevel.fromScore(0));
            assertEquals(SdohRiskScore.RiskLevel.LOW, SdohRiskScore.RiskLevel.fromScore(15));
            assertEquals(SdohRiskScore.RiskLevel.LOW, SdohRiskScore.RiskLevel.fromScore(25));
        }

        @Test
        @DisplayName("Should assign MODERATE risk level for score 26-50")
        void shouldAssignModerateRiskLevel() {
            assertEquals(SdohRiskScore.RiskLevel.MODERATE, SdohRiskScore.RiskLevel.fromScore(26));
            assertEquals(SdohRiskScore.RiskLevel.MODERATE, SdohRiskScore.RiskLevel.fromScore(38));
            assertEquals(SdohRiskScore.RiskLevel.MODERATE, SdohRiskScore.RiskLevel.fromScore(50));
        }

        @Test
        @DisplayName("Should assign HIGH risk level for score 51-75")
        void shouldAssignHighRiskLevel() {
            assertEquals(SdohRiskScore.RiskLevel.HIGH, SdohRiskScore.RiskLevel.fromScore(51));
            assertEquals(SdohRiskScore.RiskLevel.HIGH, SdohRiskScore.RiskLevel.fromScore(63));
            assertEquals(SdohRiskScore.RiskLevel.HIGH, SdohRiskScore.RiskLevel.fromScore(75));
        }

        @Test
        @DisplayName("Should assign CRITICAL risk level for score 76-100")
        void shouldAssignCriticalRiskLevel() {
            assertEquals(SdohRiskScore.RiskLevel.CRITICAL, SdohRiskScore.RiskLevel.fromScore(76));
            assertEquals(SdohRiskScore.RiskLevel.CRITICAL, SdohRiskScore.RiskLevel.fromScore(88));
            assertEquals(SdohRiskScore.RiskLevel.CRITICAL, SdohRiskScore.RiskLevel.fromScore(100));
        }
    }

    @Nested
    @DisplayName("Risk Prediction Tests")
    class RiskPredictionTests {

        @Test
        @DisplayName("Should predict hospitalization risk based on score")
        void shouldPredictHospitalizationRisk() {
            // Given - risk score of 50 → (50/100) * 0.3 = 0.15
            SdohRiskScore riskScore = SdohRiskScore.builder()
                    .totalScore(50.0)
                    .build();

            // When
            double risk = riskCalculator.predictHospitalizationRisk(riskScore);

            // Then
            assertEquals(0.15, risk, 0.001);
        }

        @Test
        @DisplayName("Should cap hospitalization risk at 1.0")
        void shouldCapHospitalizationRiskAtMax() {
            // Given - very high score
            SdohRiskScore riskScore = SdohRiskScore.builder()
                    .totalScore(100.0)
                    .build();

            // When
            double risk = riskCalculator.predictHospitalizationRisk(riskScore);

            // Then - should be capped at 0.3 (100/100 * 0.3)
            assertEquals(0.30, risk, 0.001);
        }

        @Test
        @DisplayName("Should predict emergency visit risk based on score")
        void shouldPredictEmergencyVisitRisk() {
            // Given - risk score of 50 → (50/100) * 0.4 = 0.20
            SdohRiskScore riskScore = SdohRiskScore.builder()
                    .totalScore(50.0)
                    .build();

            // When
            double risk = riskCalculator.predictEmergencyVisitRisk(riskScore);

            // Then
            assertEquals(0.20, risk, 0.001);
        }

        @Test
        @DisplayName("Should predict negative medication adherence impact")
        void shouldPredictMedicationAdherenceImpact() {
            // Given - risk score of 50 → -(50/100) * 0.3 = -0.15
            SdohRiskScore riskScore = SdohRiskScore.builder()
                    .totalScore(50.0)
                    .build();

            // When
            double impact = riskCalculator.predictMedicationAdherenceImpact(riskScore);

            // Then - negative impact
            assertEquals(-0.15, impact, 0.001);
        }
    }

    @Nested
    @DisplayName("Trend Identification Tests")
    class TrendIdentificationTests {

        @Test
        @DisplayName("Should identify INCREASING trend when score increases >5 points")
        void shouldIdentifyIncreasingTrend() {
            // Given - first score 30, latest score 40 (increase of 10)
            List<SdohRiskScore> history = Arrays.asList(
                    SdohRiskScore.builder().totalScore(40.0).calculatedAt(LocalDateTime.now()).build(),
                    SdohRiskScore.builder().totalScore(30.0).calculatedAt(LocalDateTime.now().minusDays(1)).build()
            );

            // When
            String trend = riskCalculator.identifyTrend(history);

            // Then
            assertEquals("INCREASING", trend);
        }

        @Test
        @DisplayName("Should identify DECREASING trend when score decreases >5 points")
        void shouldIdentifyDecreasingTrend() {
            // Given - first score 40, latest score 30 (decrease of 10)
            List<SdohRiskScore> history = Arrays.asList(
                    SdohRiskScore.builder().totalScore(30.0).calculatedAt(LocalDateTime.now()).build(),
                    SdohRiskScore.builder().totalScore(40.0).calculatedAt(LocalDateTime.now().minusDays(1)).build()
            );

            // When
            String trend = riskCalculator.identifyTrend(history);

            // Then
            assertEquals("DECREASING", trend);
        }

        @Test
        @DisplayName("Should identify STABLE trend when score change is within ±5 points")
        void shouldIdentifyStableTrend() {
            // Given - first score 50, latest score 52 (change of 2)
            List<SdohRiskScore> history = Arrays.asList(
                    SdohRiskScore.builder().totalScore(52.0).calculatedAt(LocalDateTime.now()).build(),
                    SdohRiskScore.builder().totalScore(50.0).calculatedAt(LocalDateTime.now().minusDays(1)).build()
            );

            // When
            String trend = riskCalculator.identifyTrend(history);

            // Then
            assertEquals("STABLE", trend);
        }

        @Test
        @DisplayName("Should return INSUFFICIENT_DATA when history has less than 2 entries")
        void shouldReturnInsufficientDataForShortHistory() {
            // Given - only one record
            List<SdohRiskScore> history = Arrays.asList(
                    SdohRiskScore.builder().totalScore(50.0).calculatedAt(LocalDateTime.now()).build()
            );

            // When
            String trend = riskCalculator.identifyTrend(history);

            // Then
            assertEquals("INSUFFICIENT_DATA", trend);
        }
    }

    @Nested
    @DisplayName("Full Risk Score Calculation Tests")
    class FullCalculationTests {

        @Test
        @DisplayName("Should calculate and persist risk score from assessment")
        void shouldCalculateAndPersistRiskScore() {
            // Given
            Map<SdohCategory, Boolean> needs = new HashMap<>();
            needs.put(SdohCategory.FOOD_INSECURITY, true);      // 0.15
            needs.put(SdohCategory.HOUSING_INSTABILITY, true);  // 0.20
            // Total: 0.35 * 100 = 35 (MODERATE)

            SdohAssessment assessment = SdohAssessment.builder()
                    .assessmentId("assessment-001")
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_ID)
                    .identifiedNeeds(needs)
                    .build();

            when(riskScoreRepository.save(any(SdohRiskScoreEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            SdohRiskScore result = riskCalculator.calculateRiskScore(assessment);

            // Then
            assertNotNull(result);
            assertEquals(TENANT_ID, result.getTenantId());
            assertEquals(PATIENT_ID, result.getPatientId());
            assertEquals(35.0, result.getTotalScore(), 0.001);
            assertEquals(SdohRiskScore.RiskLevel.MODERATE, result.getRiskLevel());
            assertEquals("assessment-001", result.getAssessmentId());
            assertNotNull(result.getScoreId());
            assertNotNull(result.getCalculatedAt());

            // Verify persistence
            ArgumentCaptor<SdohRiskScoreEntity> entityCaptor = ArgumentCaptor.forClass(SdohRiskScoreEntity.class);
            verify(riskScoreRepository).save(entityCaptor.capture());

            SdohRiskScoreEntity savedEntity = entityCaptor.getValue();
            assertEquals(TENANT_ID, savedEntity.getTenantId());
            assertEquals(PATIENT_ID, savedEntity.getPatientId());
        }
    }
}
