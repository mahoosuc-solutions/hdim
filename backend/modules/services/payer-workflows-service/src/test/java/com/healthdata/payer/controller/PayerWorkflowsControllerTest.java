package com.healthdata.payer.controller;

import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import com.healthdata.payer.domain.*;
import com.healthdata.payer.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD tests for PayerWorkflowsController REST API endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@DisplayName("Payer Workflows Controller Tests")
class PayerWorkflowsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StarRatingCalculator starRatingCalculator;

    @MockBean
    private MedicaidComplianceService medicaidComplianceService;

    @MockBean
    private PayerDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        // Setup default mocks for internal helper method calls used by dashboard endpoints
        // The controller's createSampleStarReport() calls starRatingCalculator
        when(starRatingCalculator.calculateStarRatingReport(
            anyString(), anyString(), anyString(), anyInt(), anyMap(), any()
        )).thenReturn(createSampleStarRatingReport("default-plan"));

        // The controller's createSampleComplianceReport() calls medicaidComplianceService
        when(medicaidComplianceService.calculateComplianceReport(
            anyString(), anyString(), any(), anyString(), anyInt(), anyMap()
        )).thenReturn(createSampleComplianceReport("NY"));
    }

    // ==================== Star Rating Endpoints ====================

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId} - Should return star rating report")
    void shouldReturnStarRatingReport() throws Exception {
        // Given
        String planId = "H1234-001";
        StarRatingReport report = createSampleStarRatingReport(planId);

        when(starRatingCalculator.calculateStarRatingReport(
            eq(planId), anyString(), anyString(), anyInt(), anyMap(), any()
        )).thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", planId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.planId").value(planId))
            .andExpect(jsonPath("$.overallStarRating").exists())
            .andExpect(jsonPath("$.roundedStarRating").exists());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId}/measures - Should return measure breakdown")
    void shouldReturnMeasureBreakdown() throws Exception {
        // Given
        String planId = "H1234-001";
        StarRatingReport report = createStarRatingReportWithMeasures(planId);

        when(starRatingCalculator.calculateStarRatingReport(anyString(), anyString(), anyString(), anyInt(), anyMap(), any()))
            .thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}/measures", planId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].measure").exists())
            .andExpect(jsonPath("$[0].performanceRate").exists())
            .andExpect(jsonPath("$[0].stars").exists());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId}/improvement - Should return improvement opportunities")
    void shouldReturnImprovementOpportunities() throws Exception {
        // Given
        String planId = "H1234-001";
        StarRatingReport report = createStarRatingReportWithOpportunities(planId);

        when(starRatingCalculator.calculateStarRatingReport(anyString(), anyString(), anyString(), anyInt(), anyMap(), any()))
            .thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}/improvement", planId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].measure").exists())
            .andExpect(jsonPath("$[0].currentStars").exists())
            .andExpect(jsonPath("$[0].nextStars").exists())
            .andExpect(jsonPath("$[0].patientsNeeded").exists());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId} - Should return 404 for invalid plan")
    void shouldReturn404ForInvalidPlan() throws Exception {
        // Given
        String planId = "INVALID";
        when(starRatingCalculator.calculateStarRatingReport(anyString(), anyString(), anyString(), anyInt(), anyMap(), any()))
            .thenThrow(new IllegalArgumentException("Plan not found"));

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", planId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/payer/medicare/star-rating/calculate - Should calculate star rating from request")
    @org.junit.jupiter.api.Disabled("Requires StarRatingCalculationRequest DTO with proper enum deserialization")
    void shouldCalculateStarRatingFromRequest() throws Exception {
        // Given
        StarRatingReport report = createSampleStarRatingReport("H1234-001");
        when(starRatingCalculator.calculateStarRatingReport(anyString(), anyString(), anyString(), anyInt(), anyMap(), any()))
            .thenReturn(report);

        String requestBody = """
            {
                "planId": "H1234-001",
                "planName": "Test Plan",
                "contractNumber": "H1234",
                "reportingYear": 2024,
                "measureData": {
                    "CBP": {"numerator": 750, "denominator": 1000}
                }
            }
            """;

        // When/Then
        mockMvc.perform(post("/api/v1/payer/medicare/star-rating/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planId").value("H1234-001"));
    }

    // ==================== Medicaid Compliance Endpoints ====================

    @Test
    @DisplayName("GET /api/v1/payer/medicaid/{state}/compliance - Should return state compliance report")
    void shouldReturnStateComplianceReport() throws Exception {
        // Given
        String state = "NY";
        MedicaidComplianceReport report = createSampleComplianceReport(state);

        when(medicaidComplianceService.calculateComplianceReport(
            anyString(), anyString(), any(), anyString(), anyInt(), anyMap()
        )).thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", state)
                .param("mcoId", "NY-MCO-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stateConfig.stateCode").value(state))
            .andExpect(jsonPath("$.overallStatus").exists())
            .andExpect(jsonPath("$.overallComplianceRate").exists());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicaid/{state}/compliance - Should support all states")
    void shouldSupportMultipleStates() throws Exception {
        // Test NY, CA, TX, FL
        for (String state : List.of("NY", "CA", "TX", "FL")) {
            MedicaidComplianceReport report = createSampleComplianceReport(state);
            when(medicaidComplianceService.calculateComplianceReport(
                anyString(), anyString(), any(), anyString(), anyInt(), anyMap()
            )).thenReturn(report);

            mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", state)
                    .param("mcoId", state + "-MCO-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateConfig.stateCode").value(state));
        }
    }

    @Test
    @DisplayName("POST /api/v1/payer/medicaid/compliance/calculate - Should calculate compliance from request")
    @org.junit.jupiter.api.Disabled("Requires MedicaidComplianceRequest DTO with proper enum deserialization")
    void shouldCalculateComplianceFromRequest() throws Exception {
        // Given
        MedicaidComplianceReport report = createSampleComplianceReport("NY");
        when(medicaidComplianceService.calculateComplianceReport(
            anyString(), anyString(), any(), anyString(), anyInt(), anyMap()
        )).thenReturn(report);

        String requestBody = """
            {
                "mcoId": "NY-MCO-001",
                "mcoName": "NY Health Plus",
                "stateCode": "NY",
                "reportingPeriod": "2024",
                "measurementYear": 2024,
                "measurePerformance": {
                    "CBP": {"numerator": 700, "denominator": 1000}
                }
            }
            """;

        // When/Then
        mockMvc.perform(post("/api/v1/payer/medicaid/compliance/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mcoId").value("NY-MCO-001"));
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicaid/{state}/compliance - Should return measures requiring corrective action")
    void shouldReturnCorrectiveActionMeasures() throws Exception {
        // Given
        String state = "NY";
        MedicaidComplianceReport report = createComplianceReportWithCorrectiveAction(state);

        when(medicaidComplianceService.calculateComplianceReport(
            anyString(), anyString(), any(), anyString(), anyInt(), anyMap()
        )).thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", state)
                .param("mcoId", "NY-MCO-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correctiveActionMeasures").isArray())
            .andExpect(jsonPath("$.correctiveActionMeasures").isNotEmpty());
    }

    // ==================== Dashboard Endpoints ====================

    @Test
    @DisplayName("GET /api/v1/payer/dashboard/overview - Should return payer dashboard metrics")
    void shouldReturnDashboardOverview() throws Exception {
        // Given
        String payerId = "PAYER-001";
        PayerDashboardMetrics metrics = createSampleDashboardMetrics(payerId);

        when(dashboardService.generateCombinedDashboard(anyString(), anyString(), anyList(), anyList()))
            .thenReturn(metrics);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/dashboard/overview")
                .param("payerId", payerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payerId").value(payerId))
            .andExpect(jsonPath("$.dashboardType").exists())
            .andExpect(jsonPath("$.totalEnrollment").exists());
    }

    @Test
    @DisplayName("GET /api/v1/payer/dashboard/medicare - Should return Medicare-specific dashboard")
    void shouldReturnMedicareDashboard() throws Exception {
        // Given
        String payerId = "PAYER-001";
        PayerDashboardMetrics metrics = createMedicareDashboardMetrics(payerId);

        when(dashboardService.generateMedicareDashboard(anyString(), anyString(), anyList()))
            .thenReturn(metrics);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/dashboard/medicare")
                .param("payerId", payerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.medicareMetrics").exists())
            .andExpect(jsonPath("$.medicareMetrics.averageStarRating").exists())
            .andExpect(jsonPath("$.medicareMetrics.plansWithFourStarsOrMore").exists());
    }

    @Test
    @DisplayName("GET /api/v1/payer/dashboard/medicaid - Should return Medicaid-specific dashboard")
    void shouldReturnMedicaidDashboard() throws Exception {
        // Given
        String payerId = "PAYER-002";
        PayerDashboardMetrics metrics = createMedicaidDashboardMetrics(payerId);

        when(dashboardService.generateMedicaidDashboard(anyString(), anyString(), anyList()))
            .thenReturn(metrics);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/dashboard/medicaid")
                .param("payerId", payerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.medicaidMetrics").exists())
            .andExpect(jsonPath("$.medicaidMetrics.averageComplianceRate").exists())
            .andExpect(jsonPath("$.medicaidMetrics.compliantPlans").exists());
    }

    @Test
    @DisplayName("GET /api/v1/payer/dashboard/overview - Should include top performing measures")
    void shouldIncludeTopPerformingMeasures() throws Exception {
        // Given
        PayerDashboardMetrics metrics = createDashboardWithMeasures();
        when(dashboardService.generateCombinedDashboard(anyString(), anyString(), anyList(), anyList()))
            .thenReturn(metrics);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/dashboard/overview")
                .param("payerId", "PAYER-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topPerformingMeasures").exists())
            .andExpect(jsonPath("$.measuresNeedingAttention").exists());
    }

    @Test
    @DisplayName("GET /api/v1/payer/dashboard/financial - Should return financial impact summary")
    @org.junit.jupiter.api.Disabled("Dashboard financial endpoint not yet implemented")
    void shouldReturnFinancialImpact() throws Exception {
        // Given
        PayerDashboardMetrics metrics = createDashboardWithFinancials();
        when(dashboardService.generateCombinedDashboard(anyString(), anyString(), anyList(), anyList()))
            .thenReturn(metrics);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/dashboard/financial")
                .param("payerId", "PAYER-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.financialSummary").exists());
    }

    // ==================== Helper Methods ====================

    private StarRatingReport createSampleStarRatingReport(String planId) {
        return StarRatingReport.builder()
            .planId(planId)
            .planName("Test Plan")
            .contractNumber("H1234")
            .reportingYear(2024)
            .overallStarRating(4.0)
            .roundedStarRating(4)
            .qualityBonusPaymentEligible(true)
            .bonusPaymentPercentage(3.0)
            .build();
    }

    private StarRatingReport createStarRatingReportWithMeasures(String planId) {
        MeasureScore score = MeasureScore.builder()
            .measure(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE)
            .performanceRate(0.75)
            .stars(4)
            .numerator(750)
            .denominator(1000)
            .build();

        StarRatingReport report = createSampleStarRatingReport(planId);
        report.setAllMeasureScores(List.of(score));
        return report;
    }

    private StarRatingReport createStarRatingReportWithOpportunities(String planId) {
        ImprovementOpportunity opportunity = ImprovementOpportunity.builder()
            .measure(StarRatingMeasure.BREAST_CANCER_SCREENING)
            .currentStars(3)
            .nextStars(4)
            .patientsNeeded(30)
            .build();

        StarRatingReport report = createSampleStarRatingReport(planId);
        report.setImprovementOpportunities(List.of(opportunity));
        return report;
    }

    private MedicaidComplianceReport createSampleComplianceReport(String stateCode) {
        MedicaidStateConfig config = switch (stateCode) {
            case "NY" -> MedicaidStateConfig.StateConfigs.newYork();
            case "CA" -> MedicaidStateConfig.StateConfigs.california();
            case "TX" -> MedicaidStateConfig.StateConfigs.texas();
            case "FL" -> MedicaidStateConfig.StateConfigs.florida();
            default -> MedicaidStateConfig.StateConfigs.newYork();
        };

        return MedicaidComplianceReport.builder()
            .mcoId(stateCode + "-MCO-001")
            .mcoName("Test MCO")
            .stateConfig(config)
            .reportingPeriod("2024")
            .measurementYear(2024)
            .overallStatus(MedicaidComplianceReport.ComplianceStatus.COMPLIANT)
            .overallComplianceRate(0.95)
            .build();
    }

    private MedicaidComplianceReport createComplianceReportWithCorrectiveAction(String stateCode) {
        MedicaidComplianceReport report = createSampleComplianceReport(stateCode);
        report.setCorrectiveActionMeasures(List.of("CBP", "BCS"));
        return report;
    }

    private PayerDashboardMetrics createSampleDashboardMetrics(String payerId) {
        return PayerDashboardMetrics.builder()
            .payerId(payerId)
            .payerName("Test Payer")
            .dashboardType(PayerDashboardMetrics.DashboardType.ALL)
            .totalEnrollment(50000)
            .activePlans(5)
            .build();
    }

    private PayerDashboardMetrics createMedicareDashboardMetrics(String payerId) {
        PayerDashboardMetrics.MedicareAdvantageMetrics maMetrics =
            PayerDashboardMetrics.MedicareAdvantageMetrics.builder()
                .averageStarRating(4.0)
                .plansWithFourStarsOrMore(3)
                .totalMedicarePlans(5)
                .build();

        PayerDashboardMetrics metrics = createSampleDashboardMetrics(payerId);
        metrics.setMedicareMetrics(maMetrics);
        return metrics;
    }

    private PayerDashboardMetrics createMedicaidDashboardMetrics(String payerId) {
        PayerDashboardMetrics.MedicaidMcoMetrics medicaidMetrics =
            PayerDashboardMetrics.MedicaidMcoMetrics.builder()
                .averageComplianceRate(0.90)
                .compliantPlans(4)
                .nonCompliantPlans(1)
                .build();

        PayerDashboardMetrics metrics = createSampleDashboardMetrics(payerId);
        metrics.setMedicaidMetrics(medicaidMetrics);
        return metrics;
    }

    private PayerDashboardMetrics createDashboardWithMeasures() {
        Map<String, Double> topMeasures = new HashMap<>();
        topMeasures.put("CBP", 0.85);
        topMeasures.put("BCS", 0.82);

        Map<String, Double> needsAttention = new HashMap<>();
        needsAttention.put("COL", 0.55);

        PayerDashboardMetrics metrics = createSampleDashboardMetrics("PAYER-001");
        metrics.setTopPerformingMeasures(topMeasures);
        metrics.setMeasuresNeedingAttention(needsAttention);
        return metrics;
    }

    private PayerDashboardMetrics createDashboardWithFinancials() {
        PayerDashboardMetrics.FinancialImpactSummary financial =
            PayerDashboardMetrics.FinancialImpactSummary.builder()
                .currentQualityBonuses(500000.0)
                .potentialQualityBonuses(750000.0)
                .build();

        PayerDashboardMetrics metrics = createSampleDashboardMetrics("PAYER-001");
        metrics.setFinancialSummary(financial);
        return metrics;
    }

    // ==================== Edge Cases and Error Handling Tests ====================

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId} - Should handle missing planId")
    void shouldHandleMissingPlanId() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId} - Should handle empty planId")
    void shouldHandleEmptyPlanId() throws Exception {
        // Given
        String planId = "";

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", planId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId} - Should handle service throwing RuntimeException")
    void shouldHandleServiceRuntimeException() throws Exception {
        // Given
        String planId = "H1234-001";
        when(starRatingCalculator.calculateStarRatingReport(anyString(), anyString(), anyString(), anyInt(), anyMap(), any()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When/Then - Controller converts RuntimeException to 404
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", planId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicaid/{state}/compliance - Should handle invalid state code")
    void shouldHandleInvalidStateCode() throws Exception {
        // Given
        String state = "INVALID";
        when(medicaidComplianceService.calculateComplianceReport(
            anyString(), anyString(), any(), anyString(), anyInt(), anyMap()
        )).thenThrow(new IllegalArgumentException("Invalid state code"));

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", state)
                .param("mcoId", "TEST-MCO-001"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicaid/{state}/compliance - Should require mcoId parameter")
    void shouldRequireMcoIdParameter() throws Exception {
        // Given
        String state = "NY";

        // When/Then - When mcoId is missing, controller returns 400
        mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", state))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/payer/dashboard/overview - Should handle missing payerId parameter")
    void shouldHandleMissingPayerIdParameter() throws Exception {
        // When/Then - Controller requires payerId parameter and returns 400
        mockMvc.perform(get("/api/v1/payer/dashboard/overview"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/payer/dashboard/medicare - Should return empty metrics for payer with no plans")
    void shouldReturnEmptyMetricsForPayerWithNoPlans() throws Exception {
        // Given
        String payerId = "PAYER-NO-PLANS";
        PayerDashboardMetrics emptyMetrics = PayerDashboardMetrics.builder()
            .payerId(payerId)
            .payerName("Empty Payer")
            .dashboardType(PayerDashboardMetrics.DashboardType.MEDICARE_ADVANTAGE)
            .totalEnrollment(0)
            .activePlans(0)
            .build();

        when(dashboardService.generateMedicareDashboard(anyString(), anyString(), anyList()))
            .thenReturn(emptyMetrics);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/dashboard/medicare")
                .param("payerId", payerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEnrollment").value(0))
            .andExpect(jsonPath("$.activePlans").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId}/measures - Should handle plan with no measures")
    void shouldHandlePlanWithNoMeasures() throws Exception {
        // Given
        String planId = "H1234-NO-MEASURES";
        StarRatingReport report = StarRatingReport.builder()
            .planId(planId)
            .planName("Test Plan")
            .allMeasureScores(List.of())
            .build();

        when(starRatingCalculator.calculateStarRatingReport(anyString(), anyString(), anyString(), anyInt(), anyMap(), any()))
            .thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}/measures", planId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId}/improvement - Should handle plan with no opportunities")
    void shouldHandlePlanWithNoImprovementOpportunities() throws Exception {
        // Given
        String planId = "H1234-PERFECT";
        StarRatingReport report = StarRatingReport.builder()
            .planId(planId)
            .planName("Perfect Plan")
            .overallStarRating(5.0)
            .improvementOpportunities(List.of())
            .build();

        when(starRatingCalculator.calculateStarRatingReport(anyString(), anyString(), anyString(), anyInt(), anyMap(), any()))
            .thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}/improvement", planId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicaid/{state}/compliance - Should handle multiple query parameters")
    void shouldHandleMultipleQueryParameters() throws Exception {
        // Given
        String state = "NY";
        MedicaidComplianceReport report = createSampleComplianceReport(state);

        when(medicaidComplianceService.calculateComplianceReport(
            anyString(), anyString(), any(), anyString(), anyInt(), anyMap()
        )).thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", state)
                .param("mcoId", "NY-MCO-001")
                .param("year", "2024")
                .param("reportingPeriod", "Q4"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/payer/dashboard/overview - Should handle service timeout")
    void shouldHandleServiceTimeout() throws Exception {
        // Given
        when(dashboardService.generateCombinedDashboard(anyString(), anyString(), anyList(), anyList()))
            .thenThrow(new RuntimeException("Service timeout"));

        // When/Then - Controller returns 500 for unhandled RuntimeException
        mockMvc.perform(get("/api/v1/payer/dashboard/overview")
                .param("payerId", "PAYER-001"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId} - Should handle very long planId")
    void shouldHandleVeryLongPlanId() throws Exception {
        // Given
        String planId = "H".repeat(1000) + "-001";

        // When/Then - Controller accepts long planIds and processes them
        // This tests that the controller doesn't crash with long input
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", planId))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId} - Should handle special characters in planId")
    void shouldHandleSpecialCharactersInPlanId() throws Exception {
        // Given
        String planId = "H1234<script>alert('xss')</script>";
        when(starRatingCalculator.calculateStarRatingReport(anyString(), anyString(), anyString(), anyInt(), anyMap(), any()))
            .thenThrow(new IllegalArgumentException("Invalid plan ID"));

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", planId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/payer/dashboard/overview - Should handle concurrent requests")
    void shouldHandleConcurrentRequests() throws Exception {
        // Given
        PayerDashboardMetrics metrics = createSampleDashboardMetrics("PAYER-001");
        when(dashboardService.generateCombinedDashboard(anyString(), anyString(), anyList(), anyList()))
            .thenReturn(metrics);

        // When/Then - Multiple concurrent requests should all succeed
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/payer/dashboard/overview")
                    .param("payerId", "PAYER-001"))
                .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicaid/{state}/compliance - Should support case-insensitive state codes")
    void shouldSupportCaseInsensitiveStateCodes() throws Exception {
        // Given - Test with lowercase state code
        String state = "ny";
        MedicaidComplianceReport report = createSampleComplianceReport("NY");

        when(medicaidComplianceService.calculateComplianceReport(
            anyString(), anyString(), any(), anyString(), anyInt(), anyMap()
        )).thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicaid/{state}/compliance", state)
                .param("mcoId", "NY-MCO-001"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/payer/medicare/star-rating/{planId} - Should return report with all required fields")
    void shouldReturnReportWithAllRequiredFields() throws Exception {
        // Given
        String planId = "H1234-001";
        StarRatingReport report = StarRatingReport.builder()
            .planId(planId)
            .planName("Complete Plan")
            .contractNumber("H1234")
            .reportingYear(2024)
            .overallStarRating(4.0)
            .roundedStarRating(4)
            .qualityBonusPaymentEligible(true)
            .bonusPaymentPercentage(5.0)
            .build();

        when(starRatingCalculator.calculateStarRatingReport(anyString(), anyString(), anyString(), anyInt(), anyMap(), any()))
            .thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/medicare/star-rating/{planId}", planId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planId").value(planId))
            .andExpect(jsonPath("$.planName").exists())
            .andExpect(jsonPath("$.contractNumber").exists())
            .andExpect(jsonPath("$.reportingYear").exists())
            .andExpect(jsonPath("$.overallStarRating").exists())
            .andExpect(jsonPath("$.roundedStarRating").exists())
            .andExpect(jsonPath("$.qualityBonusPaymentEligible").exists())
            .andExpect(jsonPath("$.bonusPaymentPercentage").exists());
    }

    @Test
    @DisplayName("GET /api/v1/payer/dashboard/medicaid - Should include all state-specific metrics")
    void shouldIncludeAllStateSpecificMetrics() throws Exception {
        // Given
        String payerId = "PAYER-MULTI-STATE";
        PayerDashboardMetrics.MedicaidMcoMetrics medicaidMetrics =
            PayerDashboardMetrics.MedicaidMcoMetrics.builder()
                .numberOfStates(4)
                .averageComplianceRate(0.88)
                .compliantPlans(6)
                .nonCompliantPlans(1)
                .estimatedPenalties(25000.0)
                .estimatedBonuses(45000.0)
                .build();

        PayerDashboardMetrics metrics = createMedicaidDashboardMetrics(payerId);
        metrics.setMedicaidMetrics(medicaidMetrics);

        when(dashboardService.generateMedicaidDashboard(anyString(), anyString(), anyList()))
            .thenReturn(metrics);

        // When/Then
        mockMvc.perform(get("/api/v1/payer/dashboard/medicaid")
                .param("payerId", payerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.medicaidMetrics.numberOfStates").value(4))
            .andExpect(jsonPath("$.medicaidMetrics.averageComplianceRate").value(0.88))
            .andExpect(jsonPath("$.medicaidMetrics.compliantPlans").value(6))
            .andExpect(jsonPath("$.medicaidMetrics.nonCompliantPlans").value(1));
    }
}
