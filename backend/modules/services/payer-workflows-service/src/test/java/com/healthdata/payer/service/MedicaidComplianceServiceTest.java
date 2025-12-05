package com.healthdata.payer.service;

import com.healthdata.payer.domain.MedicaidComplianceReport;
import com.healthdata.payer.domain.MedicaidStateConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for MedicaidComplianceService - State-specific Medicaid compliance reporting.
 *
 * Tests cover:
 * - State-specific compliance calculations
 * - Threshold comparisons
 * - Compliance status determination
 * - Penalty and bonus calculations
 * - Multiple state configurations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Medicaid Compliance Service Tests")
class MedicaidComplianceServiceTest {

    private MedicaidComplianceService service;

    @BeforeEach
    void setUp() {
        service = new MedicaidComplianceService();
    }

    // ==================== State-Specific Compliance Tests ====================

    @Test
    @DisplayName("Should calculate compliance report for New York Medicaid")
    void shouldCalculateNewYorkCompliance() {
        // Given
        String mcoId = "NY-MCO-001";
        String mcoName = "NY Health Plus";
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = createNewYorkMeasureData();

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            mcoId,
            mcoName,
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getMcoId()).isEqualTo(mcoId);
        assertThat(report.getMcoName()).isEqualTo(mcoName);
        assertThat(report.getStateConfig().getStateCode()).isEqualTo("NY");
        assertThat(report.getMeasurementYear()).isEqualTo(2024);
        assertThat(report.getMeasureResults()).isNotEmpty();
    }

    @Test
    @DisplayName("Should calculate compliance report for California Medicaid")
    void shouldCalculateCaliforniaCompliance() {
        // Given
        String mcoId = "CA-MCO-001";
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.california();
        Map<String, MeasurePerformance> measurePerformance = createCaliforniaMeasureData();

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            mcoId,
            "California Care",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report.getStateConfig().getStateCode()).isEqualTo("CA");
        assertThat(report.getStateConfig().getProgramName()).isEqualTo("Medi-Cal Managed Care");
    }

    @Test
    @DisplayName("Should calculate compliance report for Texas Medicaid")
    void shouldCalculateTexasCompliance() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.texas();
        Map<String, MeasurePerformance> measurePerformance = createTexasMeasureData();

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "TX-MCO-001",
            "Texas Star Health",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report.getStateConfig().getStateCode()).isEqualTo("TX");
        assertThat(report.getStateConfig().getProgramName()).isEqualTo("Texas STAR Medicaid");
    }

    @Test
    @DisplayName("Should calculate compliance report for Florida Medicaid")
    void shouldCalculateFloridaCompliance() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.florida();
        Map<String, MeasurePerformance> measurePerformance = createFloridaMeasureData();

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "FL-MCO-001",
            "Florida Health Partners",
            config,
            "Q1 2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report.getStateConfig().getStateCode()).isEqualTo("FL");
        assertThat(report.getReportingPeriod()).isEqualTo("Q1 2024");
    }

    // ==================== Threshold Comparison Tests ====================

    @Test
    @DisplayName("Should identify measures meeting state thresholds")
    void shouldIdentifyMeasuresMeetingThresholds() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        measurePerformance.put("CBP", new MeasurePerformance(700, 1000));  // 70% > 65% threshold

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-002",
            "Test MCO",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        MedicaidComplianceReport.MedicaidMeasureResult cbpResult = report.getMeasureResults().stream()
            .filter(r -> r.getMeasureCode().equals("CBP"))
            .findFirst()
            .orElseThrow();

        assertThat(cbpResult.getMeetsThreshold()).isTrue();
        assertThat(cbpResult.getComplianceLevel())
            .isIn(MedicaidComplianceReport.MedicaidMeasureResult.ComplianceLevel.MEETS_THRESHOLD,
                  MedicaidComplianceReport.MedicaidMeasureResult.ComplianceLevel.EXCEEDS_GOAL);
    }

    @Test
    @DisplayName("Should identify measures below state thresholds")
    void shouldIdentifyMeasuresBelowThresholds() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        measurePerformance.put("CBP", new MeasurePerformance(600, 1000));  // 60% < 65% threshold

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-003",
            "Test MCO",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        MedicaidComplianceReport.MedicaidMeasureResult cbpResult = report.getMeasureResults().stream()
            .filter(r -> r.getMeasureCode().equals("CBP"))
            .findFirst()
            .orElseThrow();

        assertThat(cbpResult.getMeetsThreshold()).isFalse();
        assertThat(cbpResult.getComplianceLevel())
            .isEqualTo(MedicaidComplianceReport.MedicaidMeasureResult.ComplianceLevel.BELOW_THRESHOLD);
    }

    @Test
    @DisplayName("Should identify measures exceeding state goals")
    void shouldIdentifyMeasuresExceedingGoals() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        measurePerformance.put("CBP", new MeasurePerformance(750, 1000));  // 75% > 70% goal

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-004",
            "High Performer",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        MedicaidComplianceReport.MedicaidMeasureResult cbpResult = report.getMeasureResults().stream()
            .filter(r -> r.getMeasureCode().equals("CBP"))
            .findFirst()
            .orElseThrow();

        assertThat(cbpResult.getMeetsGoal()).isTrue();
        assertThat(cbpResult.getComplianceLevel())
            .isEqualTo(MedicaidComplianceReport.MedicaidMeasureResult.ComplianceLevel.EXCEEDS_GOAL);
    }

    // ==================== Compliance Status Tests ====================

    @Test
    @DisplayName("Should mark as COMPLIANT when all measures meet thresholds")
    void shouldMarkAsCompliantWhenAllMeetThresholds() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        // All measures above thresholds
        measurePerformance.put("CBP", new MeasurePerformance(700, 1000));
        measurePerformance.put("CDC-H9", new MeasurePerformance(800, 1000));
        measurePerformance.put("BCS", new MeasurePerformance(750, 1000));

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-005",
            "Compliant MCO",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report.getOverallStatus()).isEqualTo(MedicaidComplianceReport.ComplianceStatus.COMPLIANT);
        assertThat(report.getOverallComplianceRate()).isEqualTo(1.0);
        assertThat(report.getMeasuresBelowThreshold()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should mark as SUBSTANTIALLY_COMPLIANT when >80% meet thresholds")
    void shouldMarkAsSubstantiallyCompliant() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        // 4 out of 5 measures meet thresholds (80%)
        measurePerformance.put("CBP", new MeasurePerformance(700, 1000));
        measurePerformance.put("CDC-H9", new MeasurePerformance(800, 1000));
        measurePerformance.put("BCS", new MeasurePerformance(750, 1000));
        measurePerformance.put("COL", new MeasurePerformance(650, 1000));
        measurePerformance.put("W30", new MeasurePerformance(500, 1000));  // Below threshold

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-006",
            "Mostly Compliant MCO",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report.getOverallStatus()).isEqualTo(MedicaidComplianceReport.ComplianceStatus.SUBSTANTIALLY_COMPLIANT);
        assertThat(report.getOverallComplianceRate()).isGreaterThan(0.80);
        assertThat(report.getMeasuresBelowThreshold()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should mark as PARTIALLY_COMPLIANT when 50-80% meet thresholds")
    void shouldMarkAsPartiallyCompliant() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        // 3 out of 5 measures meet thresholds (60%)
        measurePerformance.put("CBP", new MeasurePerformance(700, 1000));
        measurePerformance.put("CDC-H9", new MeasurePerformance(800, 1000));
        measurePerformance.put("BCS", new MeasurePerformance(750, 1000));
        measurePerformance.put("COL", new MeasurePerformance(500, 1000));
        measurePerformance.put("W30", new MeasurePerformance(450, 1000));

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-007",
            "Partially Compliant MCO",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report.getOverallStatus()).isEqualTo(MedicaidComplianceReport.ComplianceStatus.PARTIALLY_COMPLIANT);
        assertThat(report.getOverallComplianceRate()).isBetween(0.50, 0.80);
    }

    @Test
    @DisplayName("Should mark as NON_COMPLIANT when <50% meet thresholds")
    void shouldMarkAsNonCompliant() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        // Only 1 out of 5 measures meets threshold (20%)
        measurePerformance.put("CBP", new MeasurePerformance(700, 1000));
        measurePerformance.put("CDC-H9", new MeasurePerformance(500, 1000));
        measurePerformance.put("BCS", new MeasurePerformance(400, 1000));
        measurePerformance.put("COL", new MeasurePerformance(350, 1000));
        measurePerformance.put("W30", new MeasurePerformance(300, 1000));

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-008",
            "Non-Compliant MCO",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report.getOverallStatus()).isEqualTo(MedicaidComplianceReport.ComplianceStatus.NON_COMPLIANT);
        assertThat(report.getOverallComplianceRate()).isLessThan(0.50);
        assertThat(report.getMeasuresBelowThreshold()).isGreaterThan(2);
    }

    // ==================== Year-over-Year Improvement Tests ====================

    @Test
    @DisplayName("Should calculate year-over-year improvement")
    void shouldCalculateYearOverYearImprovement() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> currentPerformance = new HashMap<>();
        currentPerformance.put("CBP", new MeasurePerformance(750, 1000));

        Map<String, Double> priorYearPerformance = new HashMap<>();
        priorYearPerformance.put("CBP", 0.70);

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-009",
            "Improving MCO",
            config,
            "2024",
            2024,
            currentPerformance,
            priorYearPerformance
        );

        // Then
        MedicaidComplianceReport.MedicaidMeasureResult cbpResult = report.getMeasureResults().stream()
            .filter(r -> r.getMeasureCode().equals("CBP"))
            .findFirst()
            .orElseThrow();

        assertThat(cbpResult.getPriorYearRate()).isEqualTo(0.70);
        assertThat(cbpResult.getImprovement()).isEqualTo(0.05);  // 75% - 70%
    }

    // ==================== Corrective Action Tests ====================

    @Test
    @DisplayName("Should identify measures requiring corrective action")
    void shouldIdentifyCorrectiveActionMeasures() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        measurePerformance.put("CBP", new MeasurePerformance(500, 1000));  // Well below threshold
        measurePerformance.put("CDC-H9", new MeasurePerformance(800, 1000));  // Above threshold

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-010",
            "Action Needed MCO",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report.getCorrectiveActionMeasures()).contains("CBP");
        assertThat(report.getCorrectiveActionMeasures()).doesNotContain("CDC-H9");
    }

    // ==================== Penalty Assessment Tests ====================

    @Test
    @DisplayName("Should apply penalty for non-compliant MCO")
    void shouldApplyPenaltyForNonCompliance() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        // Mostly below thresholds
        measurePerformance.put("CBP", new MeasurePerformance(500, 1000));
        measurePerformance.put("CDC-H9", new MeasurePerformance(600, 1000));
        measurePerformance.put("BCS", new MeasurePerformance(550, 1000));

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-011",
            "Penalty MCO",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report.getPenaltyAssessment()).isNotNull();
        assertThat(report.getPenaltyAssessment().isPenaltyApplied()).isTrue();
        assertThat(report.getPenaltyAssessment().getPenaltyPercentage()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should not apply penalty for compliant MCO")
    void shouldNotApplyPenaltyForCompliance() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        measurePerformance.put("CBP", new MeasurePerformance(750, 1000));
        measurePerformance.put("CDC-H9", new MeasurePerformance(850, 1000));

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-012",
            "Good MCO",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        if (report.getPenaltyAssessment() != null) {
            assertThat(report.getPenaltyAssessment().isPenaltyApplied()).isFalse();
        }
    }

    // ==================== Quality Bonus Tests ====================

    @Test
    @DisplayName("Should determine quality bonus eligibility")
    void shouldDetermineQualityBonusEligibility() {
        // Given
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MeasurePerformance> measurePerformance = new HashMap<>();
        // All measures exceed goals
        measurePerformance.put("CBP", new MeasurePerformance(750, 1000));
        measurePerformance.put("CDC-H9", new MeasurePerformance(850, 1000));
        measurePerformance.put("BCS", new MeasurePerformance(780, 1000));

        // When
        MedicaidComplianceReport report = service.calculateComplianceReport(
            "NY-MCO-013",
            "Excellent MCO",
            config,
            "2024",
            2024,
            measurePerformance
        );

        // Then
        assertThat(report.isQualityBonusEligible()).isTrue();
    }

    // ==================== Helper Methods ====================

    private Map<String, MeasurePerformance> createNewYorkMeasureData() {
        Map<String, MeasurePerformance> data = new HashMap<>();
        data.put("CBP", new MeasurePerformance(700, 1000));
        data.put("CDC-H9", new MeasurePerformance(800, 1000));
        data.put("BCS", new MeasurePerformance(750, 1000));
        data.put("COL", new MeasurePerformance(650, 1000));
        return data;
    }

    private Map<String, MeasurePerformance> createCaliforniaMeasureData() {
        Map<String, MeasurePerformance> data = new HashMap<>();
        data.put("CBP", new MeasurePerformance(680, 1000));
        data.put("CDC-H9", new MeasurePerformance(780, 1000));
        data.put("BCS", new MeasurePerformance(730, 1000));
        return data;
    }

    private Map<String, MeasurePerformance> createTexasMeasureData() {
        Map<String, MeasurePerformance> data = new HashMap<>();
        data.put("CBP", new MeasurePerformance(660, 1000));
        data.put("CDC-H9", new MeasurePerformance(760, 1000));
        data.put("W30", new MeasurePerformance(720, 1000));
        return data;
    }

    private Map<String, MeasurePerformance> createFloridaMeasureData() {
        Map<String, MeasurePerformance> data = new HashMap<>();
        data.put("CBP", new MeasurePerformance(720, 1000));
        data.put("CDC-H9", new MeasurePerformance(820, 1000));
        data.put("W30", new MeasurePerformance(750, 1000));
        return data;
    }

    /**
     * Helper class for measure performance data
     */
    public static class MeasurePerformance {
        public final int numerator;
        public final int denominator;

        public MeasurePerformance(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }
}
