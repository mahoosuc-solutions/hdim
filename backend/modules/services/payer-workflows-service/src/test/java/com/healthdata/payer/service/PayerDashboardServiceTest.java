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
        // Given
        List<StarRatingReport> reports = List.of(
            createStarReport("H1234-001", 4.5),
            createStarReport("H1234-002", 3.5),
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
}
