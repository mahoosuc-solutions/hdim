package com.healthdata.payer.service;

import com.healthdata.payer.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for PayerDashboardService - Aggregated payer metrics and analytics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payer Dashboard Service Tests")
class PayerDashboardServiceTest {

    @Mock
    private StarRatingCalculator starRatingCalculator;

    @Mock
    private MedicaidComplianceService medicaidComplianceService;

    private PayerDashboardService service;

    @BeforeEach
    void setUp() {
        service = new PayerDashboardService(starRatingCalculator, medicaidComplianceService);
    }

    @Test
    @DisplayName("Should generate Medicare Advantage dashboard metrics")
    void shouldGenerateMedicareDashboardMetrics() {
        // Given
        String payerId = "PAYER-001";
        List<StarRatingReport> starReports = createSampleStarReports();

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard(payerId, "Test Payer", starReports);

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getPayerId()).isEqualTo(payerId);
        assertThat(metrics.getDashboardType()).isEqualTo(PayerDashboardMetrics.DashboardType.MEDICARE_ADVANTAGE);
        assertThat(metrics.getMedicareMetrics()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate average star rating across all plans")
    void shouldCalculateAverageStarRating() {
        // Given
        List<StarRatingReport> reports = List.of(
            createStarReport("H1234-001", 4.5),
            createStarReport("H1234-002", 3.5),
            createStarReport("H1234-003", 5.0)
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getMedicareMetrics().getAverageStarRating()).isEqualTo(4.33, within(0.01));
    }

    @Test
    @DisplayName("Should count plans with 4+ stars")
    void shouldCountHighStarPlans() {
        // Given - Use 3.4 instead of 3.5 since Math.round(3.5) = 4 in Java
        List<StarRatingReport> reports = List.of(
            createStarReport("H1234-001", 4.5),
            createStarReport("H1234-002", 3.4),  // rounds to 3, not 4
            createStarReport("H1234-003", 5.0)
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getMedicareMetrics().getPlansWithFourStarsOrMore()).isEqualTo(2);
        assertThat(metrics.getMedicareMetrics().getPlansWithThreeStarsOrLess()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should calculate estimated bonus payments")
    void shouldCalculateEstimatedBonuses() {
        // Given
        List<StarRatingReport> reports = List.of(
            createStarReportWithEnrollment("H1234-001", 5, 10000),
            createStarReportWithEnrollment("H1234-002", 4, 8000)
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getMedicareMetrics().getEstimatedBonusPayments()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should generate Medicaid MCO dashboard metrics")
    void shouldGenerateMedicaidDashboardMetrics() {
        // Given
        String payerId = "PAYER-002";
        List<MedicaidComplianceReport> complianceReports = createSampleComplianceReports();

        // When
        PayerDashboardMetrics metrics = service.generateMedicaidDashboard(payerId, "Medicaid Payer", complianceReports);

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getDashboardType()).isEqualTo(PayerDashboardMetrics.DashboardType.MEDICAID_MCO);
        assertThat(metrics.getMedicaidMetrics()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate average compliance rate")
    void shouldCalculateAverageComplianceRate() {
        // Given
        List<MedicaidComplianceReport> reports = List.of(
            createComplianceReport("MCO-001", 0.90),
            createComplianceReport("MCO-002", 0.85),
            createComplianceReport("MCO-003", 0.95)
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicaidDashboard("PAYER-002", "Test", reports);

        // Then
        assertThat(metrics.getMedicaidMetrics().getAverageComplianceRate()).isEqualTo(0.90, within(0.01));
    }

    @Test
    @DisplayName("Should count compliant and non-compliant plans")
    void shouldCountCompliancePlans() {
        // Given
        List<MedicaidComplianceReport> reports = List.of(
            createComplianceReportWithStatus("MCO-001", MedicaidComplianceReport.ComplianceStatus.COMPLIANT),
            createComplianceReportWithStatus("MCO-002", MedicaidComplianceReport.ComplianceStatus.NON_COMPLIANT),
            createComplianceReportWithStatus("MCO-003", MedicaidComplianceReport.ComplianceStatus.SUBSTANTIALLY_COMPLIANT)
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicaidDashboard("PAYER-002", "Test", reports);

        // Then
        assertThat(metrics.getMedicaidMetrics().getCompliantPlans()).isEqualTo(2);
        assertThat(metrics.getMedicaidMetrics().getNonCompliantPlans()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should generate combined dashboard for multi-line payer")
    void shouldGenerateCombinedDashboard() {
        // Given
        String payerId = "PAYER-003";
        List<StarRatingReport> maReports = createSampleStarReports();
        List<MedicaidComplianceReport> medicaidReports = createSampleComplianceReports();

        // When
        PayerDashboardMetrics metrics = service.generateCombinedDashboard(
            payerId,
            "Multi-Line Payer",
            maReports,
            medicaidReports
        );

        // Then
        assertThat(metrics.getDashboardType()).isEqualTo(PayerDashboardMetrics.DashboardType.ALL);
        assertThat(metrics.getMedicareMetrics()).isNotNull();
        assertThat(metrics.getMedicaidMetrics()).isNotNull();
    }

    @Test
    @DisplayName("Should identify top performing measures")
    void shouldIdentifyTopPerformingMeasures() {
        // Given
        List<StarRatingReport> reports = createStarReportsWithMeasures();

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getTopPerformingMeasures()).isNotEmpty();
        assertThat(metrics.getTopPerformingMeasures().size()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should identify measures needing attention")
    void shouldIdentifyMeasuresNeedingAttention() {
        // Given
        List<StarRatingReport> reports = createStarReportsWithMeasures();

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getMeasuresNeedingAttention()).isNotEmpty();
    }

    // ==================== Helper Methods ====================

    private List<StarRatingReport> createSampleStarReports() {
        return List.of(
            createStarReport("H1234-001", 4.0),
            createStarReport("H1234-002", 3.5)
        );
    }

    private StarRatingReport createStarReport(String planId, double rating) {
        return StarRatingReport.builder()
            .planId(planId)
            .planName("Test Plan " + planId)
            .overallStarRating(rating)
            .roundedStarRating((int) Math.round(rating))
            .reportingYear(2024)
            .build();
    }

    private StarRatingReport createStarReportWithEnrollment(String planId, int stars, int enrollment) {
        StarRatingReport report = createStarReport(planId, stars);
        report.setTotalEnrollees(enrollment);
        // Set QBP eligibility for 4+ star plans (5% bonus for 5-star, 5% for 4-star)
        if (stars >= 4) {
            report.setQualityBonusPaymentEligible(true);
            report.setBonusPaymentPercentage(stars >= 5 ? 5.0 : 5.0);  // CMS QBP bonus
        }
        return report;
    }

    private List<StarRatingReport> createStarReportsWithMeasures() {
        MeasureScore highScore = MeasureScore.builder()
            .measure(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE)
            .performanceRate(0.85)
            .stars(5)
            .build();

        MeasureScore lowScore = MeasureScore.builder()
            .measure(StarRatingMeasure.BREAST_CANCER_SCREENING)
            .performanceRate(0.55)
            .stars(2)
            .build();

        StarRatingReport report = createStarReport("H1234-001", 4.0);
        report.setAllMeasureScores(List.of(highScore, lowScore));

        return List.of(report);
    }

    private List<MedicaidComplianceReport> createSampleComplianceReports() {
        return List.of(
            createComplianceReport("MCO-001", 0.85),
            createComplianceReport("MCO-002", 0.90)
        );
    }

    private MedicaidComplianceReport createComplianceReport(String mcoId, double complianceRate) {
        return MedicaidComplianceReport.builder()
            .mcoId(mcoId)
            .mcoName("Test MCO " + mcoId)
            .overallComplianceRate(complianceRate)
            .stateConfig(MedicaidStateConfig.StateConfigs.newYork())
            .build();
    }

    private MedicaidComplianceReport createComplianceReportWithStatus(
        String mcoId,
        MedicaidComplianceReport.ComplianceStatus status
    ) {
        return MedicaidComplianceReport.builder()
            .mcoId(mcoId)
            .overallStatus(status)
            .stateConfig(MedicaidStateConfig.StateConfigs.newYork())
            .build();
    }

    // ==================== Edge Cases and Error Handling Tests ====================

    @Test
    @DisplayName("Should handle empty star reports list")
    void shouldHandleEmptyStarReports() {
        // Given
        List<StarRatingReport> emptyReports = List.of();

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", emptyReports);

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalEnrollment()).isEqualTo(0);
        assertThat(metrics.getActivePlans()).isEqualTo(0);
        assertThat(metrics.getMedicareMetrics()).isNull();
    }

    @Test
    @DisplayName("Should handle empty compliance reports list")
    void shouldHandleEmptyComplianceReports() {
        // Given
        List<MedicaidComplianceReport> emptyReports = List.of();

        // When
        PayerDashboardMetrics metrics = service.generateMedicaidDashboard("PAYER-002", "Test", emptyReports);

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalEnrollment()).isEqualTo(0);
        assertThat(metrics.getActivePlans()).isEqualTo(0);
        assertThat(metrics.getMedicaidMetrics()).isNull();
    }

    @Test
    @DisplayName("Should handle null measure scores in star reports")
    void shouldHandleNullMeasureScores() {
        // Given
        StarRatingReport report = createStarReport("H1234-001", 4.0);
        report.setAllMeasureScores(null);
        List<StarRatingReport> reports = List.of(report);

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTopPerformingMeasures()).isEmpty();
        assertThat(metrics.getMeasuresNeedingAttention()).isEmpty();
    }

    @Test
    @DisplayName("Should handle reports with zero enrollees")
    void shouldHandleReportsWithZeroEnrollees() {
        // Given
        List<StarRatingReport> reports = List.of(
            createStarReportWithEnrollment("H1234-001", 5, 0),
            createStarReportWithEnrollment("H1234-002", 4, 0)
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getTotalEnrollment()).isEqualTo(0);
        assertThat(metrics.getMedicareMetrics().getEstimatedBonusPayments()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should handle all plans with 3 stars or less")
    void shouldHandleAllLowPerformingPlans() {
        // Given
        List<StarRatingReport> reports = List.of(
            createStarReport("H1234-001", 2.0),
            createStarReport("H1234-002", 3.0),
            createStarReport("H1234-003", 2.5)
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getMedicareMetrics().getPlansWithFourStarsOrMore()).isEqualTo(0);
        assertThat(metrics.getMedicareMetrics().getPlansWithThreeStarsOrLess()).isEqualTo(3);
        assertThat(metrics.getMedicareMetrics().isQualityBonusPaymentEligible()).isFalse();
    }

    @Test
    @DisplayName("Should handle all plans with 4+ stars")
    void shouldHandleAllHighPerformingPlans() {
        // Given
        List<StarRatingReport> reports = List.of(
            createStarReport("H1234-001", 4.5),
            createStarReport("H1234-002", 5.0),
            createStarReport("H1234-003", 4.0)
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getMedicareMetrics().getPlansWithFourStarsOrMore()).isEqualTo(3);
        assertThat(metrics.getMedicareMetrics().getPlansWithThreeStarsOrLess()).isEqualTo(0);
        assertThat(metrics.getMedicareMetrics().isQualityBonusPaymentEligible()).isTrue();
    }

    @Test
    @DisplayName("Should handle reports without prior year data")
    void shouldHandleReportsWithoutPriorYearData() {
        // Given
        StarRatingReport report1 = createStarReport("H1234-001", 4.0);
        report1.setOverallImprovement(null);
        StarRatingReport report2 = createStarReport("H1234-002", 3.5);
        report2.setOverallImprovement(null);
        List<StarRatingReport> reports = List.of(report1, report2);

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getMedicareMetrics().getYearOverYearImprovement()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate negative year-over-year improvement")
    void shouldCalculateNegativeYearOverYearImprovement() {
        // Given
        StarRatingReport report1 = createStarReport("H1234-001", 3.0);
        report1.setOverallImprovement(-0.5);
        StarRatingReport report2 = createStarReport("H1234-002", 2.5);
        report2.setOverallImprovement(-0.3);
        List<StarRatingReport> reports = List.of(report1, report2);

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getMedicareMetrics().getYearOverYearImprovement()).isLessThan(0);
    }

    @Test
    @DisplayName("Should handle medicaid reports from multiple states")
    void shouldHandleMedicaidReportsFromMultipleStates() {
        // Given
        List<MedicaidComplianceReport> reports = List.of(
            createComplianceReportWithState("MCO-001", "NY"),
            createComplianceReportWithState("MCO-002", "CA"),
            createComplianceReportWithState("MCO-003", "TX"),
            createComplianceReportWithState("MCO-004", "FL")
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicaidDashboard("PAYER-002", "Test", reports);

        // Then
        assertThat(metrics.getMedicaidMetrics().getNumberOfStates()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should handle reports from single state with multiple MCOs")
    void shouldHandleSingleStateWithMultipleMCOs() {
        // Given
        List<MedicaidComplianceReport> reports = List.of(
            createComplianceReportWithState("MCO-001", "NY"),
            createComplianceReportWithState("MCO-002", "NY"),
            createComplianceReportWithState("MCO-003", "NY")
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicaidDashboard("PAYER-002", "Test", reports);

        // Then
        assertThat(metrics.getMedicaidMetrics().getNumberOfStates()).isEqualTo(1);
        assertThat(metrics.getActivePlans()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle reports with penalties")
    void shouldHandleReportsWithPenalties() {
        // Given
        MedicaidComplianceReport report1 = createComplianceReport("MCO-001", 0.90);
        report1.setPenaltyAssessment(
            MedicaidComplianceReport.PenaltyAssessment.builder()
                .penaltyApplied(true)
                .penaltyPercentage(5.0)
                .estimatedPenaltyAmount(50000.0)
                .build()
        );

        MedicaidComplianceReport report2 = createComplianceReport("MCO-002", 0.85);
        report2.setPenaltyAssessment(
            MedicaidComplianceReport.PenaltyAssessment.builder()
                .penaltyApplied(true)
                .penaltyPercentage(2.0)
                .estimatedPenaltyAmount(20000.0)
                .build()
        );

        List<MedicaidComplianceReport> reports = List.of(report1, report2);

        // When
        PayerDashboardMetrics metrics = service.generateMedicaidDashboard("PAYER-002", "Test", reports);

        // Then
        assertThat(metrics.getMedicaidMetrics().getEstimatedPenalties()).isEqualTo(70000.0);
    }

    @Test
    @DisplayName("Should handle reports eligible for bonuses")
    void shouldHandleReportsEligibleForBonuses() {
        // Given
        MedicaidComplianceReport report1 = createComplianceReport("MCO-001", 0.95);
        report1.setQualityBonusEligible(true);
        report1.setEstimatedBonus(30000.0);

        MedicaidComplianceReport report2 = createComplianceReport("MCO-002", 0.92);
        report2.setQualityBonusEligible(true);
        report2.setEstimatedBonus(25000.0);

        List<MedicaidComplianceReport> reports = List.of(report1, report2);

        // When
        PayerDashboardMetrics metrics = service.generateMedicaidDashboard("PAYER-002", "Test", reports);

        // Then
        assertThat(metrics.getMedicaidMetrics().getEstimatedBonuses()).isEqualTo(55000.0);
    }

    @Test
    @DisplayName("Should handle combined dashboard with empty medicare reports")
    void shouldHandleCombinedDashboardWithEmptyMedicareReports() {
        // Given
        List<StarRatingReport> maReports = List.of();
        List<MedicaidComplianceReport> medicaidReports = createSampleComplianceReports();

        // When
        PayerDashboardMetrics metrics = service.generateCombinedDashboard(
            "PAYER-003", "Multi-Line Payer", maReports, medicaidReports
        );

        // Then
        assertThat(metrics.getDashboardType()).isEqualTo(PayerDashboardMetrics.DashboardType.ALL);
        assertThat(metrics.getMedicareMetrics()).isNull();
        assertThat(metrics.getMedicaidMetrics()).isNotNull();
    }

    @Test
    @DisplayName("Should handle combined dashboard with empty medicaid reports")
    void shouldHandleCombinedDashboardWithEmptyMedicaidReports() {
        // Given
        List<StarRatingReport> maReports = createSampleStarReports();
        List<MedicaidComplianceReport> medicaidReports = List.of();

        // When
        PayerDashboardMetrics metrics = service.generateCombinedDashboard(
            "PAYER-003", "Multi-Line Payer", maReports, medicaidReports
        );

        // Then
        assertThat(metrics.getDashboardType()).isEqualTo(PayerDashboardMetrics.DashboardType.ALL);
        assertThat(metrics.getMedicareMetrics()).isNotNull();
        assertThat(metrics.getMedicaidMetrics()).isNull();
    }

    @Test
    @DisplayName("Should handle combined dashboard with both empty")
    void shouldHandleCombinedDashboardWithBothEmpty() {
        // Given
        List<StarRatingReport> maReports = List.of();
        List<MedicaidComplianceReport> medicaidReports = List.of();

        // When
        PayerDashboardMetrics metrics = service.generateCombinedDashboard(
            "PAYER-003", "Multi-Line Payer", maReports, medicaidReports
        );

        // Then
        assertThat(metrics.getDashboardType()).isEqualTo(PayerDashboardMetrics.DashboardType.ALL);
        assertThat(metrics.getTotalEnrollment()).isEqualTo(0);
        assertThat(metrics.getActivePlans()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should correctly aggregate enrollment across reports")
    void shouldCorrectlyAggregateEnrollment() {
        // Given
        List<StarRatingReport> reports = List.of(
            createStarReportWithEnrollment("H1234-001", 4, 10000),
            createStarReportWithEnrollment("H1234-002", 5, 15000),
            createStarReportWithEnrollment("H1234-003", 3, 8000)
        );

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getTotalEnrollment()).isEqualTo(33000);
    }

    @Test
    @DisplayName("Should identify measures with consistent low performance")
    void shouldIdentifyMeasuresWithConsistentLowPerformance() {
        // Given
        MeasureScore lowScore1 = MeasureScore.builder()
            .measure(StarRatingMeasure.COLORECTAL_CANCER_SCREENING)
            .performanceRate(0.50)
            .stars(2)
            .build();

        MeasureScore lowScore2 = MeasureScore.builder()
            .measure(StarRatingMeasure.COLORECTAL_CANCER_SCREENING)
            .performanceRate(0.52)
            .stars(2)
            .build();

        StarRatingReport report1 = createStarReport("H1234-001", 3.0);
        report1.setAllMeasureScores(List.of(lowScore1));

        StarRatingReport report2 = createStarReport("H1234-002", 3.0);
        report2.setAllMeasureScores(List.of(lowScore2));

        List<StarRatingReport> reports = List.of(report1, report2);

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getMeasuresNeedingAttention()).containsKey("COL");
        assertThat(metrics.getMeasuresNeedingAttention().get("COL")).isCloseTo(0.51, within(0.01));
    }

    @Test
    @DisplayName("Should handle reports with no QBP eligible plans")
    void shouldHandleReportsWithNoQBPEligiblePlans() {
        // Given
        StarRatingReport report1 = createStarReportWithEnrollment("H1234-001", 3, 10000);
        report1.setQualityBonusPaymentEligible(false);

        StarRatingReport report2 = createStarReportWithEnrollment("H1234-002", 2, 8000);
        report2.setQualityBonusPaymentEligible(false);

        List<StarRatingReport> reports = List.of(report1, report2);

        // When
        PayerDashboardMetrics metrics = service.generateMedicareDashboard("PAYER-001", "Test", reports);

        // Then
        assertThat(metrics.getMedicareMetrics().getEstimatedBonusPayments()).isEqualTo(0.0);
        assertThat(metrics.getMedicareMetrics().isQualityBonusPaymentEligible()).isFalse();
    }

    // Helper method for creating compliance reports with specific states
    private MedicaidComplianceReport createComplianceReportWithState(String mcoId, String stateCode) {
        MedicaidStateConfig config = switch (stateCode) {
            case "NY" -> MedicaidStateConfig.StateConfigs.newYork();
            case "CA" -> MedicaidStateConfig.StateConfigs.california();
            case "TX" -> MedicaidStateConfig.StateConfigs.texas();
            case "FL" -> MedicaidStateConfig.StateConfigs.florida();
            default -> MedicaidStateConfig.StateConfigs.newYork();
        };

        return MedicaidComplianceReport.builder()
            .mcoId(mcoId)
            .mcoName("Test MCO " + mcoId)
            .overallComplianceRate(0.85)
            .overallStatus(MedicaidComplianceReport.ComplianceStatus.SUBSTANTIALLY_COMPLIANT)
            .stateConfig(config)
            .build();
    }
}
