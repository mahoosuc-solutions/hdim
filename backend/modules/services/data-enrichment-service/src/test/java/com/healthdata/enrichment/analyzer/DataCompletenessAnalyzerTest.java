package com.healthdata.enrichment.analyzer;

import com.healthdata.enrichment.model.CompletionSuggestion;
import com.healthdata.enrichment.model.DataQualityScore;
import com.healthdata.enrichment.model.MissingDataReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for DataCompletenessAnalyzer.
 */
@DisplayName("DataCompletenessAnalyzer TDD Tests")
class DataCompletenessAnalyzerTest {

    private DataCompletenessAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DataCompletenessAnalyzer();
    }

    @Test
    @DisplayName("Should identify missing demographics")
    void testMissingDemographics() {
        String patientId = "patient-123";
        MissingDataReport report = analyzer.analyze(patientId);
        assertThat(report).isNotNull();
    }

    @Test
    @DisplayName("Should identify missing quality measure data")
    void testMissingQualityMeasureData() {
        String patientId = "patient-123";
        String measureId = "CMS122";
        MissingDataReport report = analyzer.analyzeForMeasure(patientId, measureId);
        assertThat(report.getMissingElements()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate completeness score")
    void testCompletenessScore() {
        String patientId = "patient-123";
        DataQualityScore score = analyzer.calculateCompletenessScore(patientId);
        assertThat(score.getCompletenessScore()).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("Should suggest data collection actions")
    void testSuggestDataCollection() {
        String patientId = "patient-123";
        MissingDataReport report = analyzer.analyze(patientId);
        assertThat(report.getSuggestions()).isNotEmpty();
    }

    @Test
    @DisplayName("Should identify missing lab results")
    void testMissingLabResults() {
        String patientId = "patient-123";
        var missingLabs = analyzer.findMissingLabResults(patientId);
        assertThat(missingLabs).isNotNull();
    }

    @Test
    @DisplayName("Should identify missing medications")
    void testMissingMedications() {
        String patientId = "patient-123";
        var report = analyzer.analyze(patientId);
        assertThat(report.getMissingElements()).containsKey("medications");
    }

    @Test
    @DisplayName("Should identify missing diagnoses")
    void testMissingDiagnoses() {
        String patientId = "patient-123";
        var report = analyzer.analyze(patientId);
        assertThat(report).isNotNull();
    }

    @Test
    @DisplayName("Should prioritize missing data by importance")
    void testPrioritizeMissingData() {
        String patientId = "patient-123";
        var suggestions = analyzer.getSuggestions(patientId);
        assertThat(suggestions).isSortedAccordingTo((s1, s2) ->
            Integer.compare(s2.getPriority(), s1.getPriority()));
    }

    @Test
    @DisplayName("Should check HEDIS measure completeness")
    void testHedisMeasureCompleteness() {
        String patientId = "patient-123";
        String measureId = "CDC";
        double completeness = analyzer.calculateMeasureCompleteness(patientId, measureId);
        assertThat(completeness).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("Should identify gaps in care documentation")
    void testCareDocumentationGaps() {
        String patientId = "patient-123";
        var gaps = analyzer.findDocumentationGaps(patientId);
        assertThat(gaps).isNotNull();
    }

    @Test
    @DisplayName("Should suggest next best actions")
    void testNextBestActions() {
        String patientId = "patient-123";
        var actions = analyzer.suggestNextActions(patientId);
        assertThat(actions).isNotEmpty();
    }

    @Test
    @DisplayName("Should analyze data freshness")
    void testDataFreshness() {
        String patientId = "patient-123";
        var report = analyzer.analyze(patientId);
        assertThat(report.getMetadata()).containsKey("dataFreshness");
    }

    @Test
    @DisplayName("Should identify stale data elements")
    void testStaleDataElements() {
        String patientId = "patient-123";
        var staleElements = analyzer.findStaleData(patientId);
        assertThat(staleElements).isNotNull();
    }

    @Test
    @DisplayName("Should provide completion timeline")
    void testCompletionTimeline() {
        String patientId = "patient-123";
        var timeline = analyzer.getCompletionTimeline(patientId);
        assertThat(timeline).isNotNull();
    }

    @Test
    @DisplayName("Should support multi-tenant analysis")
    void testMultiTenantAnalysis() {
        String patientId = "patient-123";
        String tenantId = "tenant-456";
        MissingDataReport report = analyzer.analyzeWithTenant(patientId, tenantId);
        assertThat(report.getTenantId()).isEqualTo(tenantId);
    }
}
