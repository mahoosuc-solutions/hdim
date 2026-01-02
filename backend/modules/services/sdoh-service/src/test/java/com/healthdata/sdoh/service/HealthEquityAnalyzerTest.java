package com.healthdata.sdoh.service;

import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.repository.SdohAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for HealthEquityAnalyzer
 *
 * Testing health equity analytics and disparity measurement
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Health Equity Analyzer Tests")
class HealthEquityAnalyzerTest {

    @Mock
    private SdohAssessmentRepository assessmentRepository;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private HealthEquityAnalyzer equityAnalyzer;

    private String tenantId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        tenantId = "tenant-001";
        startDate = LocalDateTime.now().minusMonths(6);
        endDate = LocalDateTime.now();
    }

    @Test
    @DisplayName("Should generate health equity report")
    void testGenerateEquityReport() {
        // When
        EquityReport report = equityAnalyzer.generateEquityReport(tenantId, startDate, endDate);

        // Then
        assertNotNull(report);
        assertEquals(tenantId, report.getTenantId());
        assertNotNull(report.getReportDate());
    }

    @Test
    @DisplayName("Should calculate disparity metrics by race")
    void testCalculateDisparityByRace() {
        // When
        List<DisparityMetric> metrics = equityAnalyzer.calculateDisparityMetrics(
                tenantId, DisparityMetric.StratificationType.RACE, startDate, endDate);

        // Then
        assertNotNull(metrics);
        for (DisparityMetric metric : metrics) {
            assertEquals(DisparityMetric.StratificationType.RACE, metric.getStratificationType());
        }
    }

    @Test
    @DisplayName("Should calculate disparity metrics by ethnicity")
    void testCalculateDisparityByEthnicity() {
        // When
        List<DisparityMetric> metrics = equityAnalyzer.calculateDisparityMetrics(
                tenantId, DisparityMetric.StratificationType.ETHNICITY, startDate, endDate);

        // Then
        assertNotNull(metrics);
        for (DisparityMetric metric : metrics) {
            assertEquals(DisparityMetric.StratificationType.ETHNICITY, metric.getStratificationType());
        }
    }

    @Test
    @DisplayName("Should calculate disparity metrics by geography")
    void testCalculateDisparityByGeography() {
        // When
        List<DisparityMetric> metrics = equityAnalyzer.calculateDisparityMetrics(
                tenantId, DisparityMetric.StratificationType.GEOGRAPHY, startDate, endDate);

        // Then
        assertNotNull(metrics);
        for (DisparityMetric metric : metrics) {
            assertEquals(DisparityMetric.StratificationType.GEOGRAPHY, metric.getStratificationType());
        }
    }

    @Test
    @DisplayName("Should calculate disparity ratio")
    void testCalculateDisparityRatio() {
        // Given
        double groupValue = 0.25; // 25% prevalence in group
        double benchmarkValue = 0.15; // 15% prevalence in benchmark

        // When
        double ratio = equityAnalyzer.calculateDisparityRatio(groupValue, benchmarkValue);

        // Then
        assertEquals(1.67, ratio, 0.01);
    }

    @Test
    @DisplayName("Should analyze SDOH prevalence by stratification")
    void testAnalyzeSdohPrevalence() {
        // When
        Map<String, Map<SdohCategory, Double>> prevalence = equityAnalyzer.analyzeSdohPrevalence(
                tenantId, DisparityMetric.StratificationType.RACE, startDate, endDate);

        // Then
        assertNotNull(prevalence);
    }

    @Test
    @DisplayName("Should identify significant disparities")
    void testIdentifySignificantDisparities() {
        // Given
        List<DisparityMetric> metrics = Arrays.asList(
                DisparityMetric.builder()
                        .metricName("Food Insecurity Rate")
                        .disparityRatio(1.8)
                        .build(),
                DisparityMetric.builder()
                        .metricName("Housing Instability Rate")
                        .disparityRatio(1.2)
                        .build()
        );

        double threshold = 1.5;

        // When
        List<DisparityMetric> significant = equityAnalyzer.identifySignificantDisparities(metrics, threshold);

        // Then
        assertNotNull(significant);
        assertEquals(1, significant.size());
        assertTrue(significant.get(0).getDisparityRatio() >= threshold);
    }

    @Test
    @DisplayName("Should generate key findings from metrics")
    void testGenerateKeyFindings() {
        // Given
        List<DisparityMetric> metrics = Arrays.asList(
                DisparityMetric.builder()
                        .metricName("Food Insecurity")
                        .stratificationType(DisparityMetric.StratificationType.RACE)
                        .disparityRatio(2.0)
                        .build()
        );

        // When
        List<String> findings = equityAnalyzer.generateKeyFindings(metrics);

        // Then
        assertNotNull(findings);
        assertFalse(findings.isEmpty());
    }

    @Test
    @DisplayName("Should generate recommendations from findings")
    void testGenerateRecommendations() {
        // Given
        List<String> findings = Arrays.asList(
                "High disparity in food insecurity rates",
                "Geographic clustering of housing instability"
        );

        // When
        List<String> recommendations = equityAnalyzer.generateRecommendations(findings);

        // Then
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }

    @Test
    @DisplayName("Should analyze screening completion rates by stratification")
    void testAnalyzeScreeningCompletionRates() {
        // When
        Map<String, Double> rates = equityAnalyzer.analyzeScreeningCompletionRates(
                tenantId, DisparityMetric.StratificationType.LANGUAGE, startDate, endDate);

        // Then
        assertNotNull(rates);
    }

    @Test
    @DisplayName("Should analyze resource referral rates by stratification")
    void testAnalyzeReferralRates() {
        // When
        Map<String, Double> rates = equityAnalyzer.analyzeReferralRates(
                tenantId, DisparityMetric.StratificationType.RACE, startDate, endDate);

        // Then
        assertNotNull(rates);
    }

    @Test
    @DisplayName("Should calculate health outcome disparities")
    void testCalculateHealthOutcomeDisparities() {
        // When
        List<DisparityMetric> outcomes = equityAnalyzer.calculateHealthOutcomeDisparities(
                tenantId, DisparityMetric.StratificationType.INCOME_LEVEL, startDate, endDate);

        // Then
        assertNotNull(outcomes);
    }

    @Test
    @DisplayName("Should track disparity trends over time")
    void testTrackDisparityTrends() {
        // When
        Map<String, List<Double>> trends = equityAnalyzer.trackDisparityTrends(
                tenantId, DisparityMetric.StratificationType.RACE, "Food Insecurity", 12);

        // Then
        assertNotNull(trends);
    }

    @Test
    @DisplayName("Should export equity report as JSON")
    void testExportEquityReportAsJson() throws Exception {
        // Given
        EquityReport report = EquityReport.builder()
                .reportId("report-001")
                .tenantId(tenantId)
                .build();

        String expectedJson = "{\"reportId\":\"report-001\",\"tenantId\":\"tenant-001\"}";
        when(objectMapper.writeValueAsString(any(EquityReport.class))).thenReturn(expectedJson);

        // When
        String json = equityAnalyzer.exportReportAsJson(report);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("reportId"));
    }

    @Test
    @DisplayName("Should handle empty dataset gracefully")
    void testHandleEmptyDataset() {
        // When - call with valid parameters, service should handle gracefully if no data
        EquityReport report = equityAnalyzer.generateEquityReport(tenantId, startDate, endDate);

        // Then - should return a valid report structure even without data
        assertNotNull(report);
        assertNotNull(report.getDisparityMetrics());
    }

    @Test
    @DisplayName("Should validate stratification type")
    void testValidateStratificationType() {
        // When
        boolean validRace = equityAnalyzer.isValidStratificationType(DisparityMetric.StratificationType.RACE);
        boolean validNull = equityAnalyzer.isValidStratificationType(null);

        // Then
        assertTrue(validRace);
        assertFalse(validNull);
    }
}
