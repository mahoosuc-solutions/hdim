package com.healthdata.predictive.service;

import com.healthdata.predictive.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiseaseProgressionPredictor Tests")
class DiseaseProgressionPredictorTest {

    @Mock
    private FeatureExtractor featureExtractor;

    @InjectMocks
    private DiseaseProgressionPredictor progressionPredictor;

    private PatientFeatures sampleFeatures;

    @BeforeEach
    void setUp() {
        sampleFeatures = PatientFeatures.builder()
            .patientId("patient-123")
            .tenantId("tenant-1")
            .age(55)
            .charlsonComorbidityIndex(3)
            .hemoglobinA1c(8.5)
            .egfr(55.0)
            .featureVector(new double[]{55, 1, 3, 2, 0, 1, 4, 6, 0, 8.5, 1.3, 55, 145, 88, 30, 2, 3})
            .build();
    }

    @Test
    @DisplayName("Should predict diabetes progression")
    void shouldPredictDiabetesProgression() {
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "diabetes");
        assertNotNull(risk);
        assertEquals("diabetes", risk.getCondition());
        assertTrue(risk.getProgressionProbability() >= 0 && risk.getProgressionProbability() <= 1);
    }

    @Test
    @DisplayName("Should predict CKD progression")
    void shouldPredictCkdProgression() {
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "chronic-kidney-disease");
        assertNotNull(risk);
        assertEquals("chronic-kidney-disease", risk.getCondition());
    }

    @Test
    @DisplayName("Should calculate time to event")
    void shouldCalculateTimeToEvent() {
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "diabetes");
        assertNotNull(risk.getTimeToEvent());
        assertNotNull(risk.getTimeToEvent().getPredictedDays());
    }

    @Test
    @DisplayName("Should classify risk tier")
    void shouldClassifyRiskTier() {
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "diabetes");
        assertNotNull(risk.getRiskTier());
    }

    @Test
    @DisplayName("Should include risk factors")
    void shouldIncludeRiskFactors() {
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "diabetes");
        assertNotNull(risk.getRiskFactors());
        assertFalse(risk.getRiskFactors().isEmpty());
    }

    @Test
    @DisplayName("Should throw exception for null tenant ID")
    void shouldThrowExceptionForNullTenantId() {
        assertThrows(IllegalArgumentException.class, () ->
            progressionPredictor.predictProgression(null, "patient-123", new HashMap<>(), "diabetes"));
    }

    @Test
    @DisplayName("Should throw exception for null condition")
    void shouldThrowExceptionForNullCondition() {
        assertThrows(IllegalArgumentException.class, () ->
            progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), null));
    }

    @Test
    @DisplayName("Should handle high HbA1c")
    void shouldHandleHighHbA1c() {
        sampleFeatures.setHemoglobinA1c(11.0);
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "diabetes");
        assertTrue(risk.getProgressionProbability() > 0.5);
    }

    @Test
    @DisplayName("Should handle low eGFR")
    void shouldHandleLowEgfr() {
        sampleFeatures.setEgfr(25.0);
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "chronic-kidney-disease");
        assertTrue(risk.getProgressionProbability() > 0.4);
    }

    @Test
    @DisplayName("Should predict stage transitions")
    void shouldPredictStageTransitions() {
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "diabetes");
        assertNotNull(risk.getCurrentStage());
        assertNotNull(risk.getPredictedStage());
    }

    @Test
    @DisplayName("Should calculate confidence intervals for time to event")
    void shouldCalculateConfidenceIntervalsForTimeToEvent() {
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "diabetes");
        TimeToEvent tte = risk.getTimeToEvent();
        assertTrue(tte.getConfidenceLowerDays() <= tte.getPredictedDays());
        assertTrue(tte.getConfidenceUpperDays() >= tte.getPredictedDays());
    }

    @Test
    @DisplayName("Should handle patients with multiple comorbidities")
    void shouldHandlePatientsWithMultipleComorbidities() {
        sampleFeatures.setCharlsonComorbidityIndex(8);
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "diabetes");
        assertTrue(risk.getProgressionProbability() > 0.3);
    }

    @Test
    @DisplayName("Should predict heart failure progression")
    void shouldPredictHeartFailureProgression() {
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "heart-failure");
        assertNotNull(risk);
        assertEquals("heart-failure", risk.getCondition());
    }

    @Test
    @DisplayName("Should include prediction timestamp")
    void shouldIncludePredictionTimestamp() {
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "diabetes");
        assertNotNull(risk.getPredictedAt());
    }

    @Test
    @DisplayName("Should include model version")
    void shouldIncludeModelVersion() {
        when(featureExtractor.extractFeatures(any(), any(), any())).thenReturn(sampleFeatures);
        ProgressionRisk risk = progressionPredictor.predictProgression("tenant-1", "patient-123", new HashMap<>(), "diabetes");
        assertNotNull(risk.getModelVersion());
    }
}
