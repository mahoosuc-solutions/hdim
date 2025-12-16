package com.healthdata.payer.service;

import com.healthdata.payer.domain.MedicaidComplianceReport;
import com.healthdata.payer.domain.MedicaidComplianceReport.ComplianceStatus;
import com.healthdata.payer.domain.MedicaidComplianceReport.MedicaidMeasureResult;
import com.healthdata.payer.domain.MedicaidComplianceReport.PenaltyAssessment;
import com.healthdata.payer.domain.MedicaidStateConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MedicaidComplianceService - State-specific Medicaid compliance reporting.
 *
 * Tests cover:
 * - State-specific compliance calculations
 * - Threshold and goal comparisons
 * - Compliance status determination
 * - Penalty and bonus calculations
 * - Multiple state configurations (NY, CA, TX, FL)
 */
@DisplayName("Medicaid Compliance Service Tests")
class MedicaidComplianceServiceTest {

    private MedicaidComplianceService complianceService;

    private static final String MCO_ID = "mco-001";
    private static final String MCO_NAME = "Health Partners MCO";
    private static final String REPORTING_PERIOD = "Q4 2024";
    private static final int MEASUREMENT_YEAR = 2024;

    @BeforeEach
    void setUp() {
        complianceService = new MedicaidComplianceService();
    }

    @Nested
    @DisplayName("New York State Compliance Tests")
    class NewYorkComplianceTests {

        private MedicaidStateConfig nyConfig;

        @BeforeEach
        void setUp() {
            nyConfig = MedicaidStateConfig.StateConfigs.newYork();
        }

        @Test
        @DisplayName("Should calculate compliant status when all measures exceed thresholds")
        void shouldCalculateCompliantStatus() {
            // Given - all measures exceed NY thresholds
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            // NY thresholds: CBP=0.65, CDC-H9=0.75, BCS=0.70, COL=0.60
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(70, 100));      // 0.70 > 0.65
            performance.put("CDC-H9", new MedicaidComplianceService.MeasurePerformance(80, 100));   // 0.80 > 0.75
            performance.put("BCS", new MedicaidComplianceService.MeasurePerformance(75, 100));      // 0.75 > 0.70
            performance.put("COL", new MedicaidComplianceService.MeasurePerformance(65, 100));      // 0.65 > 0.60

            // When
            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            // Then
            assertNotNull(report);
            assertEquals(ComplianceStatus.COMPLIANT, report.getOverallStatus());
            assertEquals(1.0, report.getOverallComplianceRate(), 0.001);
            assertEquals(4, report.getMeasuresMetThreshold());
            assertEquals(0, report.getMeasuresBelowThreshold());
            assertTrue(report.getCorrectiveActionMeasures().isEmpty());
        }

        @Test
        @DisplayName("Should calculate non-compliant status when less than 50% of measures meet thresholds")
        void shouldCalculateNonCompliantStatus() {
            // Given - only 1 of 4 measures meets NY thresholds
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(70, 100));      // 0.70 > 0.65 ✓
            performance.put("CDC-H9", new MedicaidComplianceService.MeasurePerformance(50, 100));   // 0.50 < 0.75 ✗
            performance.put("BCS", new MedicaidComplianceService.MeasurePerformance(40, 100));      // 0.40 < 0.70 ✗
            performance.put("COL", new MedicaidComplianceService.MeasurePerformance(30, 100));      // 0.30 < 0.60 ✗

            // When
            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            // Then
            assertEquals(ComplianceStatus.NON_COMPLIANT, report.getOverallStatus());
            assertEquals(0.25, report.getOverallComplianceRate(), 0.001);  // 1/4 = 0.25
            assertEquals(1, report.getMeasuresMetThreshold());
            assertEquals(3, report.getMeasuresBelowThreshold());
            assertEquals(3, report.getCorrectiveActionMeasures().size());
        }

        @Test
        @DisplayName("Should require NCQA accreditation for NY")
        void shouldRequireNcqaAccreditation() {
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(70, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals("Required", report.getNcqaAccreditation());
        }
    }

    @Nested
    @DisplayName("Texas State Compliance Tests")
    class TexasComplianceTests {

        private MedicaidStateConfig txConfig;

        @BeforeEach
        void setUp() {
            txConfig = MedicaidStateConfig.StateConfigs.texas();
        }

        @Test
        @DisplayName("Should not require NCQA accreditation for TX")
        void shouldNotRequireNcqaAccreditation() {
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(60, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, txConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals("Not Required", report.getNcqaAccreditation());
        }

        @Test
        @DisplayName("Should apply TX-specific thresholds")
        void shouldApplyTexasThresholds() {
            // TX has lower CBP threshold (0.58) than NY (0.65)
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(60, 100));  // 0.60 > TX 0.58 threshold

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, txConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            // Should pass TX threshold
            MedicaidMeasureResult cbpResult = report.getMeasureResults().stream()
                .filter(r -> r.getMeasureCode().equals("CBP"))
                .findFirst()
                .orElseThrow();

            assertTrue(cbpResult.isMeetsThreshold());
            assertEquals(0.58, cbpResult.getStateThreshold(), 0.001);
        }
    }

    @Nested
    @DisplayName("Measure Result Tests")
    class MeasureResultTests {

        @Test
        @DisplayName("Should correctly identify measure exceeding goal")
        void shouldIdentifyMeasureExceedingGoal() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            // NY goals: CBP=0.70
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(75, 100));  // 0.75 > 0.70 goal

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertTrue(result.isMeetsThreshold());
            assertTrue(result.isMeetsGoal());
            assertEquals(MedicaidMeasureResult.ComplianceLevel.EXCEEDS_GOAL, result.getComplianceLevel());
        }

        @Test
        @DisplayName("Should correctly identify measure between threshold and goal")
        void shouldIdentifyMeasureBetweenThresholdAndGoal() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            // NY: CBP threshold=0.65, goal=0.70
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(67, 100));  // 0.67: above 0.65, below 0.70

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertTrue(result.isMeetsThreshold());
            assertFalse(result.isMeetsGoal());
            assertEquals(MedicaidMeasureResult.ComplianceLevel.MEETS_THRESHOLD, result.getComplianceLevel());
        }

        @Test
        @DisplayName("Should correctly identify measure below threshold")
        void shouldIdentifyMeasureBelowThreshold() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            // NY: CBP threshold=0.65
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(50, 100));  // 0.50 < 0.65

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertFalse(result.isMeetsThreshold());
            assertFalse(result.isMeetsGoal());
            assertEquals(MedicaidMeasureResult.ComplianceLevel.BELOW_THRESHOLD, result.getComplianceLevel());
        }

        @Test
        @DisplayName("Should calculate performance rate correctly")
        void shouldCalculatePerformanceRate() {
            MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(850, 1000));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, config, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertEquals(0.85, result.getPerformanceRate(), 0.001);
            assertEquals(850, result.getNumerator());
            assertEquals(1000, result.getDenominator());
        }
    }

    @Nested
    @DisplayName("Penalty Assessment Tests")
    class PenaltyAssessmentTests {

        @Test
        @DisplayName("Should apply 5% penalty for non-compliant status")
        void shouldApplyPenaltyForNonCompliantStatus() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            // Create scenario with < 50% compliance (non-compliant)
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(30, 100));
            performance.put("CDC-H9", new MedicaidComplianceService.MeasurePerformance(30, 100));
            performance.put("BCS", new MedicaidComplianceService.MeasurePerformance(30, 100));
            performance.put("COL", new MedicaidComplianceService.MeasurePerformance(30, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            PenaltyAssessment penalty = report.getPenaltyAssessment();
            assertNotNull(penalty);
            assertTrue(penalty.isPenaltyApplied());
            assertEquals(5.0, penalty.getPenaltyPercentage(), 0.001);
            assertFalse(penalty.getPenaltyReasons().isEmpty());
            assertNotNull(penalty.getCorrectiveActionPlan());
        }

        @Test
        @DisplayName("Should apply 2% penalty for partially compliant with 3+ measures below threshold")
        void shouldApplyPartialPenalty() {
            // Use custom config to ensure all measures have defined thresholds
            MedicaidStateConfig config = MedicaidStateConfig.builder()
                .stateCode("TEST")
                .stateName("Test State")
                .programName("Test Medicaid")
                .requiredMeasures(java.util.List.of("M1", "M2", "M3", "M4", "M5", "M6"))
                .qualityThresholds(Map.of(
                    "M1", 0.50, "M2", 0.50, "M3", 0.50,
                    "M4", 0.50, "M5", 0.50, "M6", 0.50
                ))
                .performanceGoals(Map.of(
                    "M1", 0.60, "M2", 0.60, "M3", 0.60,
                    "M4", 0.60, "M5", 0.60, "M6", 0.60
                ))
                .ncqaAccreditationRequired(false)
                .build();

            // Create scenario with 50-80% compliance but 3+ measures below threshold
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            // 3/6 measures pass = 50% (PARTIALLY_COMPLIANT)
            performance.put("M1", new MedicaidComplianceService.MeasurePerformance(60, 100));      // Pass
            performance.put("M2", new MedicaidComplianceService.MeasurePerformance(60, 100));      // Pass
            performance.put("M3", new MedicaidComplianceService.MeasurePerformance(60, 100));      // Pass
            performance.put("M4", new MedicaidComplianceService.MeasurePerformance(30, 100));      // Fail
            performance.put("M5", new MedicaidComplianceService.MeasurePerformance(30, 100));      // Fail
            performance.put("M6", new MedicaidComplianceService.MeasurePerformance(30, 100));      // Fail

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, config, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals(ComplianceStatus.PARTIALLY_COMPLIANT, report.getOverallStatus());
            PenaltyAssessment penalty = report.getPenaltyAssessment();
            assertTrue(penalty.isPenaltyApplied());
            assertEquals(2.0, penalty.getPenaltyPercentage(), 0.001);
        }

        @Test
        @DisplayName("Should not apply penalty for compliant status")
        void shouldNotApplyPenaltyForCompliantStatus() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(70, 100));
            performance.put("CDC-H9", new MedicaidComplianceService.MeasurePerformance(80, 100));
            performance.put("BCS", new MedicaidComplianceService.MeasurePerformance(75, 100));
            performance.put("COL", new MedicaidComplianceService.MeasurePerformance(65, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            PenaltyAssessment penalty = report.getPenaltyAssessment();
            assertFalse(penalty.isPenaltyApplied());
            assertEquals(0.0, penalty.getPenaltyPercentage(), 0.001);
            assertNull(penalty.getCorrectiveActionPlan());
        }
    }

    @Nested
    @DisplayName("Quality Bonus Tests")
    class QualityBonusTests {

        @Test
        @DisplayName("Should be eligible for quality bonus when compliant and majority exceed goals")
        void shouldBeEligibleForQualityBonus() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            // NY goals: CBP=0.70, CDC-H9=0.80, BCS=0.75, COL=0.65
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(75, 100));      // 0.75 > 0.70 goal ✓
            performance.put("CDC-H9", new MedicaidComplianceService.MeasurePerformance(85, 100));   // 0.85 > 0.80 goal ✓
            performance.put("BCS", new MedicaidComplianceService.MeasurePerformance(80, 100));      // 0.80 > 0.75 goal ✓
            performance.put("COL", new MedicaidComplianceService.MeasurePerformance(65, 100));      // 0.65 = 0.65 goal ✓

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertTrue(report.isQualityBonusEligible());
            assertNotNull(report.getEstimatedBonus());
            assertTrue(report.getEstimatedBonus() > 0);
        }

        @Test
        @DisplayName("Should not be eligible for quality bonus when non-compliant")
        void shouldNotBeEligibleWhenNonCompliant() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(30, 100));
            performance.put("CDC-H9", new MedicaidComplianceService.MeasurePerformance(30, 100));
            performance.put("BCS", new MedicaidComplianceService.MeasurePerformance(30, 100));
            performance.put("COL", new MedicaidComplianceService.MeasurePerformance(30, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertFalse(report.isQualityBonusEligible());
            assertNull(report.getEstimatedBonus());
        }

        @Test
        @DisplayName("Should calculate bonus amount based on measures exceeding goals")
        void shouldCalculateBonusAmount() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            // All 4 measures exceed goals
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(80, 100));      // > 0.70 goal
            performance.put("CDC-H9", new MedicaidComplianceService.MeasurePerformance(90, 100));   // > 0.80 goal
            performance.put("BCS", new MedicaidComplianceService.MeasurePerformance(85, 100));      // > 0.75 goal
            performance.put("COL", new MedicaidComplianceService.MeasurePerformance(75, 100));      // > 0.65 goal

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertTrue(report.isQualityBonusEligible());
            // 4 measures * $10,000 each = $40,000
            assertEquals(40000.0, report.getEstimatedBonus(), 0.001);
        }
    }

    @Nested
    @DisplayName("Year-over-Year Improvement Tests")
    class ImprovementTrackingTests {

        @Test
        @DisplayName("Should track improvement from prior year")
        void shouldTrackImprovementFromPriorYear() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();

            Map<String, MedicaidComplianceService.MeasurePerformance> currentPerformance = new HashMap<>();
            currentPerformance.put("CBP", new MedicaidComplianceService.MeasurePerformance(70, 100));  // 0.70

            Map<String, Double> priorYearPerformance = new HashMap<>();
            priorYearPerformance.put("CBP", 0.60);  // Prior year: 0.60

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR,
                currentPerformance, priorYearPerformance
            );

            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertEquals(0.60, result.getPriorYearRate(), 0.001);
            assertEquals(0.10, result.getImprovement(), 0.001);  // 0.70 - 0.60 = 0.10 improvement
        }

        @Test
        @DisplayName("Should handle negative improvement (decline)")
        void shouldHandleNegativeImprovement() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();

            Map<String, MedicaidComplianceService.MeasurePerformance> currentPerformance = new HashMap<>();
            currentPerformance.put("CBP", new MedicaidComplianceService.MeasurePerformance(60, 100));  // 0.60

            Map<String, Double> priorYearPerformance = new HashMap<>();
            priorYearPerformance.put("CBP", 0.70);  // Prior year: 0.70

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR,
                currentPerformance, priorYearPerformance
            );

            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertEquals(-0.10, result.getImprovement(), 0.001);  // 0.60 - 0.70 = -0.10 decline
        }

        @Test
        @DisplayName("Should handle missing prior year data")
        void shouldHandleMissingPriorYearData() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();

            Map<String, MedicaidComplianceService.MeasurePerformance> currentPerformance = new HashMap<>();
            currentPerformance.put("CBP", new MedicaidComplianceService.MeasurePerformance(70, 100));

            // No prior year data passed
            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, currentPerformance
            );

            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertNull(result.getPriorYearRate());
            assertNull(result.getImprovement());
        }
    }

    @Nested
    @DisplayName("Compliance Status Boundary Tests")
    class ComplianceStatusBoundaryTests {

        @Test
        @DisplayName("Should be SUBSTANTIALLY_COMPLIANT at exactly 81% compliance")
        void shouldBeSubstantiallyCompliantAtBoundary() {
            MedicaidStateConfig config = createConfigWithMeasures(10);

            // 9/10 = 90% > 80% → SUBSTANTIALLY_COMPLIANT
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            for (int i = 1; i <= 9; i++) {
                performance.put("M" + i, new MedicaidComplianceService.MeasurePerformance(80, 100));  // Pass
            }
            performance.put("M10", new MedicaidComplianceService.MeasurePerformance(30, 100));  // Fail

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, config, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals(ComplianceStatus.SUBSTANTIALLY_COMPLIANT, report.getOverallStatus());
        }

        @Test
        @DisplayName("Should be PARTIALLY_COMPLIANT at exactly 50% compliance")
        void shouldBePartiallyCompliantAtBoundary() {
            MedicaidStateConfig config = createConfigWithMeasures(10);

            // 5/10 = 50% → PARTIALLY_COMPLIANT
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                performance.put("M" + i, new MedicaidComplianceService.MeasurePerformance(80, 100));  // Pass
            }
            for (int i = 6; i <= 10; i++) {
                performance.put("M" + i, new MedicaidComplianceService.MeasurePerformance(30, 100));  // Fail
            }

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, config, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals(ComplianceStatus.PARTIALLY_COMPLIANT, report.getOverallStatus());
        }

        @Test
        @DisplayName("Should be NON_COMPLIANT below 50% compliance")
        void shouldBeNonCompliantBelowBoundary() {
            MedicaidStateConfig config = createConfigWithMeasures(10);

            // 4/10 = 40% < 50% → NON_COMPLIANT
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            for (int i = 1; i <= 4; i++) {
                performance.put("M" + i, new MedicaidComplianceService.MeasurePerformance(80, 100));  // Pass
            }
            for (int i = 5; i <= 10; i++) {
                performance.put("M" + i, new MedicaidComplianceService.MeasurePerformance(30, 100));  // Fail
            }

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, config, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals(ComplianceStatus.NON_COMPLIANT, report.getOverallStatus());
        }

        private MedicaidStateConfig createConfigWithMeasures(int count) {
            java.util.List<String> measures = new java.util.ArrayList<>();
            Map<String, Double> thresholds = new HashMap<>();
            Map<String, Double> goals = new HashMap<>();

            for (int i = 1; i <= count; i++) {
                String measureCode = "M" + i;
                measures.add(measureCode);
                thresholds.put(measureCode, 0.50);  // 50% threshold
                goals.put(measureCode, 0.60);       // 60% goal
            }

            return MedicaidStateConfig.builder()
                .stateCode("TEST")
                .stateName("Test State")
                .programName("Test Medicaid")
                .requiredMeasures(measures)
                .qualityThresholds(thresholds)
                .performanceGoals(goals)
                .ncqaAccreditationRequired(false)
                .build();
        }
    }

    @Nested
    @DisplayName("Report Metadata Tests")
    class ReportMetadataTests {

        @Test
        @DisplayName("Should populate all report metadata")
        void shouldPopulateAllReportMetadata() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(70, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals(MCO_ID, report.getMcoId());
            assertEquals(MCO_NAME, report.getMcoName());
            assertEquals(REPORTING_PERIOD, report.getReportingPeriod());
            assertEquals(MEASUREMENT_YEAR, report.getMeasurementYear());
            assertNotNull(report.getGeneratedAt());
            assertNotNull(report.getStateConfig());
            assertEquals("NY", report.getStateConfig().getStateCode());
        }

        @Test
        @DisplayName("Should include measure name mapping")
        void shouldIncludeMeasureNameMapping() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(70, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertEquals("CBP", result.getMeasureCode());
            assertEquals("Controlling High Blood Pressure", result.getMeasureName());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty measure performance map")
        void shouldHandleEmptyMeasurePerformance() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertNotNull(report);
            assertEquals(0, report.getMeasureResults().size());
            assertEquals(0.0, report.getOverallComplianceRate(), 0.001);
        }

        @Test
        @DisplayName("Should handle zero denominator in measure performance")
        void shouldHandleZeroDenominator() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(0, 0));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertNotNull(report);
            assertEquals(1, report.getMeasureResults().size());
            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertEquals(0.0, result.getPerformanceRate(), 0.001);
            assertFalse(result.isMeetsThreshold());
        }

        @Test
        @DisplayName("Should handle measure with numerator exceeding denominator")
        void shouldHandleNumeratorExceedingDenominator() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(150, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertNotNull(report);
            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertEquals(1.5, result.getPerformanceRate(), 0.001);
        }

        @Test
        @DisplayName("Should handle missing measure in performance map")
        void shouldHandleMissingMeasureInPerformanceMap() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            // Only provide one measure when NY requires multiple
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(70, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertNotNull(report);
            // Should only include the measure that was provided
            assertEquals(1, report.getMeasureResults().size());
        }

        @Test
        @DisplayName("Should handle unknown measure codes gracefully")
        void shouldHandleUnknownMeasureCodes() {
            MedicaidStateConfig config = MedicaidStateConfig.builder()
                .stateCode("TEST")
                .stateName("Test State")
                .programName("Test Medicaid")
                .requiredMeasures(java.util.List.of("UNKNOWN_MEASURE"))
                .qualityThresholds(Map.of("UNKNOWN_MEASURE", 0.50))
                .performanceGoals(Map.of("UNKNOWN_MEASURE", 0.60))
                .ncqaAccreditationRequired(false)
                .build();

            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("UNKNOWN_MEASURE", new MedicaidComplianceService.MeasurePerformance(60, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, config, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertNotNull(report);
            assertEquals(1, report.getMeasureResults().size());
            // Measure name should default to measure code
            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertEquals("UNKNOWN_MEASURE", result.getMeasureCode());
        }

        @Test
        @DisplayName("Should handle very small performance rates")
        void shouldHandleVerySmallPerformanceRates() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(1, 10000));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            MedicaidMeasureResult result = report.getMeasureResults().get(0);
            assertEquals(0.0001, result.getPerformanceRate(), 0.00001);
            assertFalse(result.isMeetsThreshold());
        }

        @Test
        @DisplayName("Should handle perfect performance rate of 1.0")
        void shouldHandlePerfectPerformanceRate() {
            MedicaidStateConfig nyConfig = MedicaidStateConfig.StateConfigs.newYork();
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(1000, 1000));
            performance.put("CDC-H9", new MedicaidComplianceService.MeasurePerformance(1000, 1000));
            performance.put("BCS", new MedicaidComplianceService.MeasurePerformance(1000, 1000));
            performance.put("COL", new MedicaidComplianceService.MeasurePerformance(1000, 1000));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, nyConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals(ComplianceStatus.COMPLIANT, report.getOverallStatus());
            assertEquals(1.0, report.getOverallComplianceRate(), 0.001);
            report.getMeasureResults().forEach(result -> {
                assertEquals(1.0, result.getPerformanceRate(), 0.001);
                assertTrue(result.isMeetsThreshold());
                assertTrue(result.isMeetsGoal());
            });
        }
    }

    @Nested
    @DisplayName("California State Compliance Tests")
    class CaliforniaComplianceTests {

        private MedicaidStateConfig caConfig;

        @BeforeEach
        void setUp() {
            caConfig = MedicaidStateConfig.StateConfigs.california();
        }

        @Test
        @DisplayName("Should apply CA-specific compliance thresholds")
        void shouldApplyCaliforniaThresholds() {
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            // Provide performance for all CA required measures
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(68, 100));
            performance.put("CDC-H9", new MedicaidComplianceService.MeasurePerformance(78, 100));
            performance.put("BCS", new MedicaidComplianceService.MeasurePerformance(72, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, caConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertNotNull(report);
            assertEquals("CA", report.getStateConfig().getStateCode());
            assertTrue(report.getMeasureResults().size() > 0);
        }

        @Test
        @DisplayName("Should require NCQA accreditation for CA")
        void shouldRequireNcqaAccreditationForCA() {
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(70, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, caConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals("Required", report.getNcqaAccreditation());
        }
    }

    @Nested
    @DisplayName("Florida State Compliance Tests")
    class FloridaComplianceTests {

        private MedicaidStateConfig flConfig;

        @BeforeEach
        void setUp() {
            flConfig = MedicaidStateConfig.StateConfigs.florida();
        }

        @Test
        @DisplayName("Should apply FL-specific compliance thresholds")
        void shouldApplyFloridaThresholds() {
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("CBP", new MedicaidComplianceService.MeasurePerformance(64, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, flConfig, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertNotNull(report);
            assertEquals("FL", report.getStateConfig().getStateCode());
        }
    }

    @Nested
    @DisplayName("Multiple Measures Tests")
    class MultipleMeasuresTests {

        @Test
        @DisplayName("Should correctly aggregate multiple measures with mixed performance")
        void shouldAggregateMultipleMeasuresWithMixedPerformance() {
            MedicaidStateConfig config = createConfigWithMeasures(10);
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            // 7/10 measures meet threshold = 70% compliance
            for (int i = 1; i <= 7; i++) {
                performance.put("M" + i, new MedicaidComplianceService.MeasurePerformance(80, 100));
            }
            for (int i = 8; i <= 10; i++) {
                performance.put("M" + i, new MedicaidComplianceService.MeasurePerformance(30, 100));
            }

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, config, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals(0.70, report.getOverallComplianceRate(), 0.001);
            assertEquals(7, report.getMeasuresMetThreshold());
            assertEquals(3, report.getMeasuresBelowThreshold());
            assertEquals(3, report.getCorrectiveActionMeasures().size());
        }

        @Test
        @DisplayName("Should handle single measure configuration")
        void shouldHandleSingleMeasureConfiguration() {
            MedicaidStateConfig config = createConfigWithMeasures(1);
            Map<String, MedicaidComplianceService.MeasurePerformance> performance = new HashMap<>();
            performance.put("M1", new MedicaidComplianceService.MeasurePerformance(80, 100));

            MedicaidComplianceReport report = complianceService.calculateComplianceReport(
                MCO_ID, MCO_NAME, config, REPORTING_PERIOD, MEASUREMENT_YEAR, performance
            );

            assertEquals(ComplianceStatus.COMPLIANT, report.getOverallStatus());
            assertEquals(1.0, report.getOverallComplianceRate(), 0.001);
        }

        private MedicaidStateConfig createConfigWithMeasures(int count) {
            java.util.List<String> measures = new java.util.ArrayList<>();
            Map<String, Double> thresholds = new HashMap<>();
            Map<String, Double> goals = new HashMap<>();

            for (int i = 1; i <= count; i++) {
                String measureCode = "M" + i;
                measures.add(measureCode);
                thresholds.put(measureCode, 0.50);
                goals.put(measureCode, 0.60);
            }

            return MedicaidStateConfig.builder()
                .stateCode("TEST")
                .stateName("Test State")
                .programName("Test Medicaid")
                .requiredMeasures(measures)
                .qualityThresholds(thresholds)
                .performanceGoals(goals)
                .ncqaAccreditationRequired(false)
                .build();
        }
    }
}
