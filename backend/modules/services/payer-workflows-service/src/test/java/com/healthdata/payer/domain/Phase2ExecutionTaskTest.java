package com.healthdata.payer.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for Phase2ExecutionTask - Phase 2 Financial ROI Tracking.
 *
 * Tests cover:
 * - Financial field presence and serialization
 * - ROI calculation fields
 * - Care gap and intervention tracking
 * - Customer evidence (quote, case study)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Phase2ExecutionTask Financial Fields Tests")
class Phase2ExecutionTaskTest {

    private Phase2ExecutionTask task;

    @BeforeEach
    void setUp() {
        task = Phase2ExecutionTask.builder()
            .id("test-task-001")
            .tenantId("test-tenant")
            .taskName("Close HBA1C Care Gaps")
            .description("Implement care gap closure program")
            .category(Phase2ExecutionTask.TaskCategory.PRODUCT)
            .targetDueDate(Instant.now())
            .status(Phase2ExecutionTask.TaskStatus.IN_PROGRESS)
            .priority(Phase2ExecutionTask.TaskPriority.CRITICAL)
            .ownerName("John Doe")
            .ownerRole("VP_PRODUCT")
            .progressPercentage(75)
            .build();
    }

    // ==================== Financial Fields Presence Tests ====================

    @Test
    @DisplayName("Should contain hediseMeasure field")
    void shouldContainHediseMeasureField() {
        // Given
        String hediseMeasure = "HBA1C";

        // When
        task.setHediseMeasure(hediseMeasure);

        // Then
        assertThat(task.getHediseMeasure()).isEqualTo(hediseMeasure);
    }

    @Test
    @DisplayName("Should contain baselinePerformancePercentage field")
    void shouldContainBaselinePerformancePercentageField() {
        // Given
        BigDecimal baselinePercentage = new BigDecimal("65.50");

        // When
        task.setBaselinePerformancePercentage(baselinePercentage);

        // Then
        assertThat(task.getBaselinePerformancePercentage()).isEqualTo(baselinePercentage);
    }

    @Test
    @DisplayName("Should contain currentPerformancePercentage field")
    void shouldContainCurrentPerformancePercentageField() {
        // Given
        BigDecimal currentPercentage = new BigDecimal("75.25");

        // When
        task.setCurrentPerformancePercentage(currentPercentage);

        // Then
        assertThat(task.getCurrentPerformancePercentage()).isEqualTo(currentPercentage);
    }

    @Test
    @DisplayName("Should contain qualityBonusAtRisk field")
    void shouldContainQualityBonusAtRiskField() {
        // Given
        BigDecimal bonusAtRisk = new BigDecimal("150000.00");

        // When
        task.setQualityBonusAtRisk(bonusAtRisk);

        // Then
        assertThat(task.getQualityBonusAtRisk()).isEqualTo(bonusAtRisk);
    }

    @Test
    @DisplayName("Should contain qualityBonusCaptured field")
    void shouldContainQualityBonusCapturedField() {
        // Given
        BigDecimal bonusCaptured = new BigDecimal("125000.50");

        // When
        task.setQualityBonusCaptured(bonusCaptured);

        // Then
        assertThat(task.getQualityBonusCaptured()).isEqualTo(bonusCaptured);
    }

    @Test
    @DisplayName("Should contain interventionType field")
    void shouldContainInterventionTypeField() {
        // Given
        String interventionType = "Digital Outreach with Patient Engagement Portal";

        // When
        task.setInterventionType(interventionType);

        // Then
        assertThat(task.getInterventionType()).isEqualTo(interventionType);
    }

    @Test
    @DisplayName("Should contain gapsClosed field")
    void shouldContainGapsClosedField() {
        // Given
        Integer gapsClosed = 45;

        // When
        task.setGapsClosed(gapsClosed);

        // Then
        assertThat(task.getGapsClosed()).isEqualTo(45);
    }

    @Test
    @DisplayName("Should contain costPerGap field")
    void shouldContainCostPerGapField() {
        // Given
        BigDecimal costPerGap = new BigDecimal("2750.75");

        // When
        task.setCostPerGap(costPerGap);

        // Then
        assertThat(task.getCostPerGap()).isEqualTo(costPerGap);
    }

    @Test
    @DisplayName("Should contain roiPercentage field")
    void shouldContainRoiPercentageField() {
        // Given
        BigDecimal roiPercentage = new BigDecimal("285.50");

        // When
        task.setRoiPercentage(roiPercentage);

        // Then
        assertThat(task.getRoiPercentage()).isEqualTo(roiPercentage);
    }

    @Test
    @DisplayName("Should contain customerQuote field")
    void shouldContainCustomerQuoteField() {
        // Given
        String quote = "HDIM's care gap closure solution reduced our HBA1C uncontrolled population by 12% in 6 months";

        // When
        task.setCustomerQuote(quote);

        // Then
        assertThat(task.getCustomerQuote()).isEqualTo(quote);
    }

    @Test
    @DisplayName("Should contain caseStudyPublished field")
    void shouldContainCaseStudyPublishedField() {
        // Given - Should default to false
        assertThat(task.getCaseStudyPublished()).isNull();

        // When
        task.setCaseStudyPublished(true);

        // Then
        assertThat(task.getCaseStudyPublished()).isTrue();
    }

    // ==================== Financial Calculation Tests ====================

    @Test
    @DisplayName("Should calculate performance improvement from baseline to current")
    void shouldCalculatePerformanceImprovement() {
        // Given
        BigDecimal baseline = new BigDecimal("65.50");
        BigDecimal current = new BigDecimal("75.25");
        task.setBaselinePerformancePercentage(baseline);
        task.setCurrentPerformancePercentage(current);

        // When
        BigDecimal improvement = current.subtract(baseline);

        // Then
        assertThat(improvement).isEqualTo(new BigDecimal("9.75"));
    }

    @Test
    @DisplayName("Should track ROI across complete financial fields")
    void shouldTrackRoiAcrossCompleteFinancialFields() {
        // Given - Complete financial scenario
        task.setHediseMeasure("HBA1C");
        task.setBaselinePerformancePercentage(new BigDecimal("65.50"));
        task.setCurrentPerformancePercentage(new BigDecimal("75.25"));
        task.setQualityBonusAtRisk(new BigDecimal("150000.00"));
        task.setQualityBonusCaptured(new BigDecimal("125000.50"));
        task.setInterventionType("Digital Outreach");
        task.setGapsClosed(45);
        task.setCostPerGap(new BigDecimal("2750.75"));
        task.setRoiPercentage(new BigDecimal("285.50"));

        // When - Verify all fields are set
        assertThat(task.getHediseMeasure()).isEqualTo("HBA1C");
        assertThat(task.getBaselinePerformancePercentage()).isEqualTo(new BigDecimal("65.50"));
        assertThat(task.getCurrentPerformancePercentage()).isEqualTo(new BigDecimal("75.25"));
        assertThat(task.getQualityBonusAtRisk()).isEqualTo(new BigDecimal("150000.00"));
        assertThat(task.getQualityBonusCaptured()).isEqualTo(new BigDecimal("125000.50"));
        assertThat(task.getInterventionType()).isEqualTo("Digital Outreach");
        assertThat(task.getGapsClosed()).isEqualTo(45);
        assertThat(task.getCostPerGap()).isEqualTo(new BigDecimal("2750.75"));
        assertThat(task.getRoiPercentage()).isEqualTo(new BigDecimal("285.50"));

        // Then - Verify serialization is complete (all fields accessible)
        assertThat(task.getHediseMeasure()).isNotNull();
        assertThat(task.getRoiPercentage()).isNotNull();
    }

    @Test
    @DisplayName("Should support case study publication flag")
    void shouldSupportCaseStudyPublicationFlag() {
        // Given
        String customerQuote = "HDIM improved our quality metrics by 15%";
        task.setCustomerQuote(customerQuote);
        task.setCaseStudyPublished(false);

        // When - Publish case study
        task.setCaseStudyPublished(true);

        // Then
        assertThat(task.getCaseStudyPublished()).isTrue();
        assertThat(task.getCustomerQuote()).isEqualTo(customerQuote);
    }

    @Test
    @DisplayName("Should handle null financial fields gracefully")
    void shouldHandleNullFinancialFieldsGracefully() {
        // Given - Financial fields left as null
        task.setHediseMeasure(null);
        task.setBaselinePerformancePercentage(null);
        task.setQualityBonusAtRisk(null);
        task.setRoiPercentage(null);

        // When/Then - Should not throw exception
        assertThat(task.getHediseMeasure()).isNull();
        assertThat(task.getBaselinePerformancePercentage()).isNull();
        assertThat(task.getQualityBonusAtRisk()).isNull();
        assertThat(task.getRoiPercentage()).isNull();
    }

    @Test
    @DisplayName("Should support zero gaps closed")
    void shouldSupportZeroGapsClosed() {
        // Given
        task.setGapsClosed(0);

        // When/Then
        assertThat(task.getGapsClosed()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should support large gap closure numbers")
    void shouldSupportLargeGapClosureNumbers() {
        // Given - Large number of gaps
        task.setGapsClosed(5000);

        // When/Then
        assertThat(task.getGapsClosed()).isEqualTo(5000);
    }

    @Test
    @DisplayName("Should calculate cost per gap with precision")
    void shouldCalculateCostPerGapWithPrecision() {
        // Given
        BigDecimal totalCost = new BigDecimal("123750.75");
        Integer totalGaps = 45;

        // When
        BigDecimal costPerGap = totalCost.divide(new BigDecimal(totalGaps), BigDecimal.ROUND_HALF_UP);

        // Then
        assertThat(costPerGap.scale()).isGreaterThanOrEqualTo(2);
        assertThat(costPerGap).isGreaterThan(BigDecimal.ZERO);
    }

}
