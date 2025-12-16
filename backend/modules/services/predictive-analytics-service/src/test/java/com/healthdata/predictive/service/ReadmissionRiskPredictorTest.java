package com.healthdata.predictive.service;

import com.healthdata.predictive.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for ReadmissionRiskPredictor
 * Testing 30/90-day hospital readmission risk prediction
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReadmissionRiskPredictor Tests")
class ReadmissionRiskPredictorTest {

    @Mock
    private FeatureExtractor featureExtractor;

    @Mock
    private ReadmissionRiskModel riskModel;

    @InjectMocks
    private ReadmissionRiskPredictor readmissionRiskPredictor;

    private PatientFeatures sampleFeatures;
    private Map<String, Object> samplePatientData;

    @BeforeEach
    void setUp() {
        sampleFeatures = createSampleFeatures();
        samplePatientData = createSamplePatientData();

        // Mock normalizeFeatures to return the feature vector as-is for simplicity
        when(featureExtractor.normalizeFeatures(any(double[].class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ==================== Basic Prediction Tests ====================

    @Test
    @DisplayName("Should predict 30-day readmission risk")
    void shouldPredict30DayReadmissionRisk() {
        // Arrange
        when(featureExtractor.extractFeatures(eq("tenant-1"), eq("patient-123"), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35); // 35% probability

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score);
        assertEquals("patient-123", score.getPatientId());
        assertEquals("tenant-1", score.getTenantId());
        assertEquals(30, score.getPredictionPeriodDays());
        assertTrue(score.getScore() >= 0 && score.getScore() <= 100);
        assertNotNull(score.getRiskTier());
    }

    @Test
    @DisplayName("Should predict 90-day readmission risk")
    void shouldPredict90DayReadmissionRisk() {
        // Arrange
        when(featureExtractor.extractFeatures(eq("tenant-1"), eq("patient-123"), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.45); // 45% probability

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict90DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score);
        assertEquals(90, score.getPredictionPeriodDays());
        assertTrue(score.getScore() >= 0 && score.getScore() <= 100);
    }

    @Test
    @DisplayName("Should calculate LACE index")
    void shouldCalculateLaceIndex() {
        // Arrange
        when(featureExtractor.extractFeatures(eq("tenant-1"), eq("patient-123"), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score.getLaceIndex());
        assertTrue(score.getLaceIndex() >= 0, "LACE index should be non-negative");
    }

    @Test
    @DisplayName("Should include risk factors in prediction")
    void shouldIncludeRiskFactorsInPrediction() {
        // Arrange
        when(featureExtractor.extractFeatures(eq("tenant-1"), eq("patient-123"), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score.getRiskFactors());
        assertNotNull(score.getRiskFactors().getFeatureImportance());
    }

    @Test
    @DisplayName("Should convert probability to score (0-100)")
    void shouldConvertProbabilityToScore() {
        // Arrange
        when(featureExtractor.extractFeatures(eq("tenant-1"), eq("patient-123"), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.75); // 75% probability

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertEquals(75.0, score.getReadmissionProbability() * 100, 1.0);
        assertTrue(score.getScore() >= 70 && score.getScore() <= 80);
    }

    // ==================== Risk Tier Classification Tests ====================

    @Test
    @DisplayName("Should classify as LOW risk for low probability")
    void shouldClassifyAsLowRiskForLowProbability() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.15); // 15% probability

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertEquals(RiskTier.LOW, score.getRiskTier());
    }

    @Test
    @DisplayName("Should classify as MODERATE risk for moderate probability")
    void shouldClassifyAsModerateRiskForModerateProbability() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35); // 35% probability

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertEquals(RiskTier.MODERATE, score.getRiskTier());
    }

    @Test
    @DisplayName("Should classify as HIGH risk for high probability")
    void shouldClassifyAsHighRiskForHighProbability() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.65); // 65% probability

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertEquals(RiskTier.HIGH, score.getRiskTier());
    }

    @Test
    @DisplayName("Should classify as VERY_HIGH risk for very high probability")
    void shouldClassifyAsVeryHighRiskForVeryHighProbability() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.85); // 85% probability

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertEquals(RiskTier.VERY_HIGH, score.getRiskTier());
    }

    // ==================== LACE Index Tests ====================

    @Test
    @DisplayName("Should calculate LACE index with length of stay component")
    void shouldCalculateLaceIndexWithLengthOfStay() {
        // Arrange
        sampleFeatures.setLengthOfStayLastAdmission(7);
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score.getLaceIndex());
        assertTrue(score.getLaceIndex() > 0);
    }

    @Test
    @DisplayName("Should calculate LACE index with acuity component")
    void shouldCalculateLaceIndexWithAcuity() {
        // Arrange
        sampleFeatures.setLastAdmissionAcuity("emergency");
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score.getLaceIndex());
    }

    @Test
    @DisplayName("Should calculate LACE index with comorbidity component")
    void shouldCalculateLaceIndexWithComorbidity() {
        // Arrange
        sampleFeatures.setCharlsonComorbidityIndex(5);
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score.getLaceIndex());
        assertTrue(score.getLaceIndex() > 0);
    }

    @Test
    @DisplayName("Should calculate LACE index with ED visits component")
    void shouldCalculateLaceIndexWithEdVisits() {
        // Arrange
        sampleFeatures.setEdVisitsPast6Months(4);
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.35);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score.getLaceIndex());
        assertTrue(score.getLaceIndex() > 0);
    }

    // ==================== Feature Importance Tests ====================

    @Test
    @DisplayName("Should calculate feature importance scores")
    void shouldCalculateFeatureImportanceScores() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.45);
        when(riskModel.getFeatureImportance())
            .thenReturn(createMockFeatureImportance());

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score.getRiskFactors().getFeatureImportance());
        assertFalse(score.getRiskFactors().getFeatureImportance().isEmpty());
    }

    @Test
    @DisplayName("Should identify most important risk factors")
    void shouldIdentifyMostImportantRiskFactors() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.45);
        when(riskModel.getFeatureImportance())
            .thenReturn(createMockFeatureImportance());

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        Map<String, Double> importance = score.getRiskFactors().getFeatureImportance();
        assertTrue(importance.values().stream().anyMatch(v -> v > 0.0));
    }

    // ==================== Confidence Score Tests ====================

    @Test
    @DisplayName("Should include model confidence score")
    void shouldIncludeModelConfidenceScore() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.45);
        when(riskModel.getConfidence())
            .thenReturn(0.85);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score.getConfidence());
        assertTrue(score.getConfidence() >= 0.0 && score.getConfidence() <= 1.0);
    }

    @Test
    @DisplayName("Should include model version")
    void shouldIncludeModelVersion() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.45);
        when(riskModel.getModelVersion())
            .thenReturn("v1.0.0");

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score.getModelVersion());
        assertEquals("v1.0.0", score.getModelVersion());
    }

    @Test
    @DisplayName("Should include prediction timestamp")
    void shouldIncludePredictionTimestamp() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.45);

        LocalDateTime before = LocalDateTime.now();

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        LocalDateTime after = LocalDateTime.now();

        // Assert
        assertNotNull(score.getPredictedAt());
        assertTrue(score.getPredictedAt().isAfter(before) || score.getPredictedAt().isEqual(before));
        assertTrue(score.getPredictedAt().isBefore(after) || score.getPredictedAt().isEqual(after));
    }

    // ==================== Edge Cases and Validation Tests ====================

    @Test
    @DisplayName("Should throw exception for null tenant ID")
    void shouldThrowExceptionForNullTenantId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            readmissionRiskPredictor.predict30DayRisk(null, "patient-123", samplePatientData)
        );
    }

    @Test
    @DisplayName("Should throw exception for null patient ID")
    void shouldThrowExceptionForNullPatientId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            readmissionRiskPredictor.predict30DayRisk("tenant-1", null, samplePatientData)
        );
    }

    @Test
    @DisplayName("Should throw exception for null patient data")
    void shouldThrowExceptionForNullPatientData() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            readmissionRiskPredictor.predict30DayRisk("tenant-1", "patient-123", null)
        );
    }

    @Test
    @DisplayName("Should handle patients with minimal data")
    void shouldHandlePatientsWithMinimalData() {
        // Arrange
        Map<String, Object> minimalData = new HashMap<>();
        minimalData.put("patientId", "patient-123");
        PatientFeatures minimalFeatures = PatientFeatures.builder()
            .patientId("patient-123")
            .tenantId("tenant-1")
            .featureVector(new double[10])
            .build();

        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(minimalFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.25);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", minimalData
        );

        // Assert
        assertNotNull(score);
        assertTrue(score.getScore() >= 0);
    }

    @Test
    @DisplayName("Should handle extreme age values")
    void shouldHandleExtremeAgeValues() {
        // Arrange
        sampleFeatures.setAge(95);
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.75);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score);
        assertTrue(score.getScore() > 50, "Very old patients should have higher risk");
    }

    @Test
    @DisplayName("Should handle high comorbidity burden")
    void shouldHandleHighComorbidityBurden() {
        // Arrange
        sampleFeatures.setCharlsonComorbidityIndex(12);
        sampleFeatures.setActiveConditionCount(15);
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.85);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score);
        assertEquals(RiskTier.VERY_HIGH, score.getRiskTier());
    }

    @Test
    @DisplayName("Should handle patients with frequent ED visits")
    void shouldHandlePatientsWithFrequentEdVisits() {
        // Arrange
        sampleFeatures.setEdVisitsPast6Months(10);
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(riskModel.predict(any(double[].class)))
            .thenReturn(0.70);

        // Act
        ReadmissionRiskScore score = readmissionRiskPredictor.predict30DayRisk(
            "tenant-1", "patient-123", samplePatientData
        );

        // Assert
        assertNotNull(score);
        assertTrue(score.getScore() > 60, "Frequent ED visits should increase risk");
    }

    // ==================== Helper Methods ====================

    private PatientFeatures createSampleFeatures() {
        return PatientFeatures.builder()
            .patientId("patient-123")
            .tenantId("tenant-1")
            .age(65)
            .gender("male")
            .charlsonComorbidityIndex(4)
            .activeConditionCount(5)
            .hospitalizationsPastYear(1)
            .edVisitsPast6Months(2)
            .outpatientVisitsPast6Months(6)
            .activeMedicationCount(8)
            .medicationChangesPast30Days(1)
            .lengthOfStayLastAdmission(5)
            .lastAdmissionAcuity("emergency")
            .hemoglobinA1c(7.2)
            .serumCreatinine(1.1)
            .egfr(70.0)
            .bloodPressureSystolic(140.0)
            .bloodPressureDiastolic(85.0)
            .bmi(28.0)
            .socialRiskScore(3)
            .featureVector(new double[]{65, 1, 4, 5, 1, 2, 6, 8, 1, 7.2, 1.1, 70, 140, 85, 28, 3, 5})
            .build();
    }

    private Map<String, Object> createSamplePatientData() {
        Map<String, Object> data = new HashMap<>();
        data.put("age", 65);
        data.put("gender", "male");
        data.put("charlsonComorbidityIndex", 4);
        data.put("chronicConditions", Arrays.asList("diabetes", "hypertension", "copd"));
        return data;
    }

    private Map<String, Double> createMockFeatureImportance() {
        Map<String, Double> importance = new HashMap<>();
        importance.put("age", 0.15);
        importance.put("charlson_comorbidity_index", 0.25);
        importance.put("hospitalizations_past_year", 0.20);
        importance.put("ed_visits_past_6m", 0.18);
        importance.put("length_of_stay", 0.12);
        importance.put("active_medications", 0.10);
        return importance;
    }
}
