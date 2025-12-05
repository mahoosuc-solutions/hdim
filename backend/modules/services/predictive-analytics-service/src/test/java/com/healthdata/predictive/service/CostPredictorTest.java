package com.healthdata.predictive.service;

import com.healthdata.predictive.model.CostBreakdown;
import com.healthdata.predictive.model.PatientFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for CostPredictor
 * Testing per-patient cost forecasting
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CostPredictor Tests")
class CostPredictorTest {

    @Mock
    private FeatureExtractor featureExtractor;

    @Mock
    private CostPredictionModel costModel;

    @InjectMocks
    private CostPredictor costPredictor;

    private PatientFeatures sampleFeatures;
    private Map<String, Object> samplePatientData;

    @BeforeEach
    void setUp() {
        sampleFeatures = createSampleFeatures();
        samplePatientData = createSamplePatientData();
    }

    // ==================== Basic Prediction Tests ====================

    @Test
    @DisplayName("Should predict patient costs")
    void shouldPredictPatientCosts() {
        // Arrange
        when(featureExtractor.extractFeatures(eq("tenant-1"), eq("patient-123"), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictTotalCost(any(double[].class)))
            .thenReturn(25000.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12
        );

        // Assert
        assertNotNull(costBreakdown);
        assertEquals("patient-123", costBreakdown.getPatientId());
        assertEquals("tenant-1", costBreakdown.getTenantId());
        assertTrue(costBreakdown.getTotalPredictedCost() > 0);
    }

    @Test
    @DisplayName("Should predict costs by category")
    void shouldPredictCostsByCategory() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictTotalCost(any(double[].class)))
            .thenReturn(25000.0);
        when(costModel.predictInpatientCost(any(double[].class)))
            .thenReturn(12000.0);
        when(costModel.predictOutpatientCost(any(double[].class)))
            .thenReturn(6000.0);
        when(costModel.predictPharmacyCost(any(double[].class)))
            .thenReturn(4000.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12
        );

        // Assert
        assertTrue(costBreakdown.getInpatientCost() > 0);
        assertTrue(costBreakdown.getOutpatientCost() > 0);
        assertTrue(costBreakdown.getPharmacyCost() > 0);
    }

    @Test
    @DisplayName("Should set prediction period")
    void shouldSetPredictionPeriod() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictTotalCost(any(double[].class)))
            .thenReturn(25000.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 6
        );

        // Assert
        assertEquals(6, costBreakdown.getPredictionPeriodMonths());
    }

    @Test
    @DisplayName("Should calculate total cost from components")
    void shouldCalculateTotalCostFromComponents() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictInpatientCost(any(double[].class)))
            .thenReturn(10000.0);
        when(costModel.predictOutpatientCost(any(double[].class)))
            .thenReturn(5000.0);
        when(costModel.predictPharmacyCost(any(double[].class)))
            .thenReturn(3000.0);
        when(costModel.predictEmergencyCost(any(double[].class)))
            .thenReturn(2000.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12
        );

        // Assert
        double expectedTotal = 10000.0 + 5000.0 + 3000.0 + 2000.0;
        assertTrue(costBreakdown.calculateTotalCost() >= expectedTotal * 0.9);
    }

    // ==================== Cost Category Tests ====================

    @Test
    @DisplayName("Should predict inpatient costs")
    void shouldPredictInpatientCosts() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictInpatientCost(any(double[].class)))
            .thenReturn(15000.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12
        );

        // Assert
        assertEquals(15000.0, costBreakdown.getInpatientCost());
    }

    @Test
    @DisplayName("Should predict pharmacy costs")
    void shouldPredictPharmacyCosts() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictPharmacyCost(any(double[].class)))
            .thenReturn(4500.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12
        );

        // Assert
        assertEquals(4500.0, costBreakdown.getPharmacyCost());
    }

    @Test
    @DisplayName("Should predict emergency costs")
    void shouldPredictEmergencyCosts() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictEmergencyCost(any(double[].class)))
            .thenReturn(3000.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12
        );

        // Assert
        assertEquals(3000.0, costBreakdown.getEmergencyCost());
    }

    // ==================== Confidence Interval Tests ====================

    @Test
    @DisplayName("Should include confidence intervals")
    void shouldIncludeConfidenceIntervals() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictTotalCost(any(double[].class)))
            .thenReturn(25000.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12
        );

        // Assert
        assertTrue(costBreakdown.getConfidenceLower() > 0);
        assertTrue(costBreakdown.getConfidenceUpper() > costBreakdown.getConfidenceLower());
    }

    @Test
    @DisplayName("Should include confidence score")
    void shouldIncludeConfidenceScore() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.getConfidence())
            .thenReturn(0.75);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12
        );

        // Assert
        assertEquals(0.75, costBreakdown.getConfidence());
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("Should throw exception for null tenant ID")
    void shouldThrowExceptionForNullTenantId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            costPredictor.predictCosts(null, "patient-123", samplePatientData, 12)
        );
    }

    @Test
    @DisplayName("Should throw exception for null patient ID")
    void shouldThrowExceptionForNullPatientId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            costPredictor.predictCosts("tenant-1", null, samplePatientData, 12)
        );
    }

    @Test
    @DisplayName("Should throw exception for null patient data")
    void shouldThrowExceptionForNullPatientData() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            costPredictor.predictCosts("tenant-1", "patient-123", null, 12)
        );
    }

    @Test
    @DisplayName("Should throw exception for invalid prediction period")
    void shouldThrowExceptionForInvalidPredictionPeriod() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            costPredictor.predictCosts("tenant-1", "patient-123", samplePatientData, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
            costPredictor.predictCosts("tenant-1", "patient-123", samplePatientData, -1)
        );
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle high-cost patients")
    void shouldHandleHighCostPatients() {
        // Arrange
        sampleFeatures.setCharlsonComorbidityIndex(12);
        sampleFeatures.setHospitalizationsPastYear(5);
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);
        when(costModel.predictTotalCost(any(double[].class)))
            .thenReturn(100000.0);

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12
        );

        // Assert
        assertTrue(costBreakdown.getTotalPredictedCost() > 50000.0);
    }

    @Test
    @DisplayName("Should include timestamp")
    void shouldIncludeTimestamp() {
        // Arrange
        when(featureExtractor.extractFeatures(any(), any(), any()))
            .thenReturn(sampleFeatures);

        LocalDateTime before = LocalDateTime.now();

        // Act
        CostBreakdown costBreakdown = costPredictor.predictCosts(
            "tenant-1", "patient-123", samplePatientData, 12
        );

        LocalDateTime after = LocalDateTime.now();

        // Assert
        assertNotNull(costBreakdown.getPredictedAt());
        assertTrue(costBreakdown.getPredictedAt().isAfter(before) ||
                  costBreakdown.getPredictedAt().isEqual(before));
        assertTrue(costBreakdown.getPredictedAt().isBefore(after) ||
                  costBreakdown.getPredictedAt().isEqual(after));
    }

    // ==================== Helper Methods ====================

    private PatientFeatures createSampleFeatures() {
        return PatientFeatures.builder()
            .patientId("patient-123")
            .tenantId("tenant-1")
            .age(65)
            .charlsonComorbidityIndex(4)
            .activeConditionCount(5)
            .hospitalizationsPastYear(1)
            .edVisitsPast6Months(2)
            .activeMedicationCount(8)
            .featureVector(new double[]{65, 1, 4, 5, 1, 2, 6, 8, 1, 7.2, 1.1, 70, 140, 85, 28, 3, 5})
            .build();
    }

    private Map<String, Object> createSamplePatientData() {
        Map<String, Object> data = new HashMap<>();
        data.put("age", 65);
        data.put("charlsonComorbidityIndex", 4);
        return data;
    }
}
