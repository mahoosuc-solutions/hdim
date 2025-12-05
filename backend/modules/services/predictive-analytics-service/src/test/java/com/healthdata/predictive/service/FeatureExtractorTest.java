package com.healthdata.predictive.service;

import com.healthdata.predictive.model.PatientFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for FeatureExtractor
 * Testing feature extraction and normalization for ML models
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureExtractor Tests")
class FeatureExtractorTest {

    private FeatureExtractor featureExtractor;

    @BeforeEach
    void setUp() {
        featureExtractor = new FeatureExtractor();
    }

    // ==================== Basic Feature Extraction Tests ====================

    @Test
    @DisplayName("Should extract basic demographic features")
    void shouldExtractBasicDemographicFeatures() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("age", 65);
        patientData.put("gender", "male");

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertNotNull(features);
        assertEquals("patient-123", features.getPatientId());
        assertEquals("tenant-1", features.getTenantId());
        assertEquals(65, features.getAge());
        assertEquals("male", features.getGender());
    }

    @Test
    @DisplayName("Should extract chronic condition count")
    void shouldExtractChronicConditionCount() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("chronicConditions", Arrays.asList("diabetes", "hypertension", "copd"));

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertEquals(3, features.getActiveConditionCount());
        assertEquals(3, features.getChronicConditions().size());
    }

    @Test
    @DisplayName("Should calculate Charlson Comorbidity Index")
    void shouldCalculateCharlsonComorbidityIndex() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("age", 55);
        patientData.put("chronicConditions", Arrays.asList(
            "diabetes", "myocardial-infarction", "congestive-heart-failure"
        ));

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertNotNull(features.getCharlsonComorbidityIndex());
        assertTrue(features.getCharlsonComorbidityIndex() > 0, "CCI should be greater than 0");
    }

    @Test
    @DisplayName("Should extract medication count")
    void shouldExtractMedicationCount() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("activeMedications", Arrays.asList("metformin", "lisinopril", "aspirin", "atorvastatin"));

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertEquals(4, features.getActiveMedicationCount());
        assertEquals(4, features.getActiveMedications().size());
    }

    @Test
    @DisplayName("Should extract utilization metrics")
    void shouldExtractUtilizationMetrics() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("hospitalizationsPastYear", 2);
        patientData.put("edVisitsPast6Months", 3);
        patientData.put("outpatientVisitsPast6Months", 8);

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertEquals(2, features.getHospitalizationsPastYear());
        assertEquals(3, features.getEdVisitsPast6Months());
        assertEquals(8, features.getOutpatientVisitsPast6Months());
    }

    // ==================== Lab Value Extraction Tests ====================

    @Test
    @DisplayName("Should extract lab values")
    void shouldExtractLabValues() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("hemoglobinA1c", 7.5);
        patientData.put("serumCreatinine", 1.2);
        patientData.put("egfr", 65.0);

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertEquals(7.5, features.getHemoglobinA1c());
        assertEquals(1.2, features.getSerumCreatinine());
        assertEquals(65.0, features.getEgfr());
    }

    @Test
    @DisplayName("Should extract vital signs")
    void shouldExtractVitalSigns() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("bloodPressureSystolic", 145.0);
        patientData.put("bloodPressureDiastolic", 92.0);
        patientData.put("bmi", 28.5);

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertEquals(145.0, features.getBloodPressureSystolic());
        assertEquals(92.0, features.getBloodPressureDiastolic());
        assertEquals(28.5, features.getBmi());
    }

    // ==================== Feature Vector Generation Tests ====================

    @Test
    @DisplayName("Should generate feature vector from patient data")
    void shouldGenerateFeatureVector() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("age", 65);
        patientData.put("hospitalizationsPastYear", 2);
        patientData.put("edVisitsPast6Months", 3);

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertNotNull(features.getFeatureVector());
        assertTrue(features.getFeatureVector().length > 0, "Feature vector should not be empty");
    }

    @Test
    @DisplayName("Should generate feature names matching feature vector")
    void shouldGenerateFeatureNamesMatchingVector() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("age", 65);

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertNotNull(features.getFeatureNames());
        assertEquals(features.getFeatureVector().length, features.getFeatureNames().size(),
            "Feature names should match feature vector length");
    }

    @Test
    @DisplayName("Should normalize feature vector")
    void shouldNormalizeFeatureVector() {
        // Arrange
        PatientFeatures features = PatientFeatures.builder()
            .patientId("patient-123")
            .tenantId("tenant-1")
            .age(65)
            .hospitalizationsPastYear(2)
            .edVisitsPast6Months(3)
            .hemoglobinA1c(7.5)
            .featureVector(new double[]{65.0, 2.0, 3.0, 7.5})
            .build();

        // Act
        double[] normalized = featureExtractor.normalizeFeatures(features.getFeatureVector());

        // Assert
        assertNotNull(normalized);
        assertEquals(features.getFeatureVector().length, normalized.length);
        // Check that values are normalized (typically between 0 and 1)
        for (double value : normalized) {
            assertTrue(value >= 0.0 && value <= 1.0 || !Double.isFinite(value),
                "Normalized values should typically be between 0 and 1");
        }
    }

    // ==================== Edge Cases and Validation Tests ====================

    @Test
    @DisplayName("Should handle missing optional fields")
    void shouldHandleMissingOptionalFields() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("age", 45);
        // No other fields provided

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertNotNull(features);
        assertEquals(45, features.getAge());
        // Missing fields should be null or 0
    }

    @Test
    @DisplayName("Should throw exception for null patient data")
    void shouldThrowExceptionForNullPatientData() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            featureExtractor.extractFeatures("tenant-1", "patient-123", null)
        );
    }

    @Test
    @DisplayName("Should throw exception for null tenant ID")
    void shouldThrowExceptionForNullTenantId() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            featureExtractor.extractFeatures(null, "patient-123", patientData)
        );
    }

    @Test
    @DisplayName("Should throw exception for null patient ID")
    void shouldThrowExceptionForNullPatientId() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            featureExtractor.extractFeatures("tenant-1", null, patientData)
        );
    }

    @Test
    @DisplayName("Should handle zero values correctly")
    void shouldHandleZeroValuesCorrectly() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("hospitalizationsPastYear", 0);
        patientData.put("edVisitsPast6Months", 0);

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertEquals(0, features.getHospitalizationsPastYear());
        assertEquals(0, features.getEdVisitsPast6Months());
    }

    @Test
    @DisplayName("Should extract social risk factors")
    void shouldExtractSocialRiskFactors() {
        // Arrange
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "patient-123");
        patientData.put("tenantId", "tenant-1");
        patientData.put("socialRiskScore", 8);
        patientData.put("hasHomeHealthServices", true);
        patientData.put("hasSupportSystem", false);

        // Act
        PatientFeatures features = featureExtractor.extractFeatures("tenant-1", "patient-123", patientData);

        // Assert
        assertEquals(8, features.getSocialRiskScore());
        assertTrue(features.getHasHomeHealthServices());
        assertFalse(features.getHasSupportSystem());
    }
}
