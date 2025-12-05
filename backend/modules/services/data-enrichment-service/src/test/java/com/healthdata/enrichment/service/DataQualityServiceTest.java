package com.healthdata.enrichment.service;

import com.healthdata.enrichment.model.QualityDimension;
import com.healthdata.enrichment.model.QualityIssue;
import com.healthdata.enrichment.model.RemediationAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for DataQualityService.
 */
@DisplayName("DataQualityService TDD Tests")
class DataQualityServiceTest {

    private DataQualityService service;

    @BeforeEach
    void setUp() {
        service = new DataQualityService();
    }

    @Test
    @DisplayName("Should assess data accuracy")
    void testDataAccuracy() {
        String patientId = "patient-123";
        var assessment = service.assessQuality(patientId);
        assertThat(assessment.getDimensions()).containsKey(QualityDimension.ACCURACY);
    }

    @Test
    @DisplayName("Should assess data completeness")
    void testDataCompleteness() {
        String patientId = "patient-123";
        var assessment = service.assessQuality(patientId);
        assertThat(assessment.getDimensions()).containsKey(QualityDimension.COMPLETENESS);
    }

    @Test
    @DisplayName("Should assess data consistency")
    void testDataConsistency() {
        String patientId = "patient-123";
        var assessment = service.assessQuality(patientId);
        assertThat(assessment.getDimensions()).containsKey(QualityDimension.CONSISTENCY);
    }

    @Test
    @DisplayName("Should assess data timeliness")
    void testDataTimeliness() {
        String patientId = "patient-123";
        var assessment = service.assessQuality(patientId);
        assertThat(assessment.getDimensions()).containsKey(QualityDimension.TIMELINESS);
    }

    @Test
    @DisplayName("Should identify data quality issues")
    void testIdentifyIssues() {
        String patientId = "patient-123";
        List<QualityIssue> issues = service.identifyIssues(patientId);
        assertThat(issues).isNotNull();
    }

    @Test
    @DisplayName("Should suggest remediation actions")
    void testRemediationActions() {
        String patientId = "patient-123";
        List<RemediationAction> actions = service.suggestRemediationActions(patientId);
        assertThat(actions).isNotNull();
    }

    @Test
    @DisplayName("Should detect duplicate records")
    void testDetectDuplicates() {
        String patientId = "patient-123";
        var duplicates = service.detectDuplicates(patientId);
        assertThat(duplicates).isNotNull();
    }

    @Test
    @DisplayName("Should validate data formats")
    void testDataFormatValidation() {
        String patientId = "patient-123";
        var formatIssues = service.validateDataFormats(patientId);
        assertThat(formatIssues).isNotNull();
    }

    @Test
    @DisplayName("Should check referential integrity")
    void testReferentialIntegrity() {
        String patientId = "patient-123";
        var integrityIssues = service.checkReferentialIntegrity(patientId);
        assertThat(integrityIssues).isNotNull();
    }

    @Test
    @DisplayName("Should generate quality report")
    void testQualityReport() {
        String patientId = "patient-123";
        var report = service.generateQualityReport(patientId);
        assertThat(report).isNotNull();
        assertThat(report.getOverallScore()).isBetween(0.0, 100.0);
    }
}
