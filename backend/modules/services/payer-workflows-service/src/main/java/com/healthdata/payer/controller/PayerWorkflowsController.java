package com.healthdata.payer.controller;

import com.healthdata.payer.domain.*;
import com.healthdata.payer.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for Payer-Specific Workflows including:
 * - Medicare Advantage Star Ratings
 * - Medicaid State Compliance
 * - Payer Dashboard Metrics
 */
@RestController
@RequestMapping("/api/v1/payer")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payer Workflows", description = "Medicare Advantage Star Ratings and Medicaid Compliance APIs")
public class PayerWorkflowsController {

    private final StarRatingCalculator starRatingCalculator;
    private final MedicaidComplianceService medicaidComplianceService;
    private final PayerDashboardService dashboardService;

    // ==================== Medicare Advantage Star Rating Endpoints ====================

    @GetMapping("/medicare/star-rating/{planId}")
    @Operation(summary = "Get Star Rating report for Medicare Advantage plan")
    public ResponseEntity<StarRatingReport> getStarRating(@PathVariable String planId) {
        try {
            // In production, this would fetch actual plan data from database
            Map<StarRatingMeasure, StarRatingCalculator.MeasureData> measureData = getSampleMeasureData();

            StarRatingReport report = starRatingCalculator.calculateStarRatingReport(
                planId,
                "Sample MA Plan " + planId,
                planId.substring(0, 5),  // Contract number
                2024,
                measureData,
                null
            );

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error calculating star rating for plan {}: {}", planId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/medicare/star-rating/{planId}/measures")
    @Operation(summary = "Get measure breakdown for Star Rating")
    public ResponseEntity<List<MeasureScore>> getStarRatingMeasures(@PathVariable String planId) {
        try {
            Map<StarRatingMeasure, StarRatingCalculator.MeasureData> measureData = getSampleMeasureData();

            StarRatingReport report = starRatingCalculator.calculateStarRatingReport(
                planId,
                "Sample MA Plan " + planId,
                planId.substring(0, 5),
                2024,
                measureData,
                null
            );

            return ResponseEntity.ok(report.getAllMeasureScores());
        } catch (Exception e) {
            log.error("Error getting measures for plan {}: {}", planId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/medicare/star-rating/{planId}/improvement")
    @Operation(summary = "Get improvement opportunities for Star Rating")
    public ResponseEntity<List<ImprovementOpportunity>> getImprovementOpportunities(@PathVariable String planId) {
        try {
            Map<StarRatingMeasure, StarRatingCalculator.MeasureData> measureData = getSampleMeasureData();

            StarRatingReport report = starRatingCalculator.calculateStarRatingReport(
                planId,
                "Sample MA Plan " + planId,
                planId.substring(0, 5),
                2024,
                measureData,
                null
            );

            return ResponseEntity.ok(report.getImprovementOpportunities());
        } catch (Exception e) {
            log.error("Error getting improvement opportunities for plan {}: {}", planId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/medicare/star-rating/calculate")
    @Operation(summary = "Calculate Star Rating from provided measure data")
    public ResponseEntity<StarRatingReport> calculateStarRating(
        @RequestBody StarRatingCalculationRequest request
    ) {
        try {
            StarRatingReport report = starRatingCalculator.calculateStarRatingReport(
                request.getPlanId(),
                request.getPlanName(),
                request.getContractNumber(),
                request.getReportingYear(),
                request.getMeasureData(),
                request.getPriorYearData()
            );

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error calculating star rating: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== Medicaid Compliance Endpoints ====================

    @GetMapping("/medicaid/{state}/compliance")
    @Operation(summary = "Get Medicaid compliance report for state")
    public ResponseEntity<MedicaidComplianceReport> getStateCompliance(
        @PathVariable String state,
        @RequestParam String mcoId
    ) {
        try {
            MedicaidStateConfig stateConfig = getStateConfig(state);
            Map<String, MedicaidComplianceService.MeasurePerformance> measurePerformance = getSampleMedicaidData();

            MedicaidComplianceReport report = medicaidComplianceService.calculateComplianceReport(
                mcoId,
                "Sample MCO " + mcoId,
                stateConfig,
                "2024",
                2024,
                measurePerformance
            );

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error calculating compliance for state {}: {}", state, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/medicaid/compliance/calculate")
    @Operation(summary = "Calculate Medicaid compliance from provided data")
    public ResponseEntity<MedicaidComplianceReport> calculateCompliance(
        @RequestBody MedicaidComplianceCalculationRequest request
    ) {
        try {
            MedicaidStateConfig stateConfig = getStateConfig(request.getStateCode());

            MedicaidComplianceReport report = medicaidComplianceService.calculateComplianceReport(
                request.getMcoId(),
                request.getMcoName(),
                stateConfig,
                request.getReportingPeriod(),
                request.getMeasurementYear(),
                request.getMeasurePerformance(),
                request.getPriorYearPerformance()
            );

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error calculating compliance: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== Dashboard Endpoints ====================

    @GetMapping("/dashboard/overview")
    @Operation(summary = "Get payer dashboard overview metrics")
    public ResponseEntity<PayerDashboardMetrics> getDashboardOverview(@RequestParam String payerId) {
        try {
            // In production, fetch actual data for this payer
            List<StarRatingReport> maReports = List.of(
                createSampleStarReport(payerId + "-MA-001"),
                createSampleStarReport(payerId + "-MA-002")
            );

            List<MedicaidComplianceReport> medicaidReports = List.of(
                createSampleComplianceReport(payerId + "-MCO-001")
            );

            PayerDashboardMetrics metrics = dashboardService.generateCombinedDashboard(
                payerId,
                "Sample Payer " + payerId,
                maReports,
                medicaidReports
            );

            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error generating dashboard for payer {}: {}", payerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dashboard/medicare")
    @Operation(summary = "Get Medicare-specific dashboard metrics")
    public ResponseEntity<PayerDashboardMetrics> getMedicareDashboard(@RequestParam String payerId) {
        try {
            List<StarRatingReport> reports = List.of(
                createSampleStarReport(payerId + "-MA-001"),
                createSampleStarReport(payerId + "-MA-002")
            );

            PayerDashboardMetrics metrics = dashboardService.generateMedicareDashboard(
                payerId,
                "Sample Payer " + payerId,
                reports
            );

            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error generating Medicare dashboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dashboard/medicaid")
    @Operation(summary = "Get Medicaid-specific dashboard metrics")
    public ResponseEntity<PayerDashboardMetrics> getMedicaidDashboard(@RequestParam String payerId) {
        try {
            List<MedicaidComplianceReport> reports = List.of(
                createSampleComplianceReport(payerId + "-MCO-001"),
                createSampleComplianceReport(payerId + "-MCO-002")
            );

            PayerDashboardMetrics metrics = dashboardService.generateMedicaidDashboard(
                payerId,
                "Sample Payer " + payerId,
                reports
            );

            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error generating Medicaid dashboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dashboard/financial")
    @Operation(summary = "Get financial impact summary")
    public ResponseEntity<PayerDashboardMetrics.FinancialImpactSummary> getFinancialImpact(
        @RequestParam String payerId
    ) {
        try {
            // Calculate financial impact based on quality performance
            PayerDashboardMetrics.FinancialImpactSummary summary =
                PayerDashboardMetrics.FinancialImpactSummary.builder()
                    .totalPotentialRevenue(10000000.0)
                    .currentQualityBonuses(500000.0)
                    .potentialQualityBonuses(750000.0)
                    .currentPenalties(0.0)
                    .riskOfPenalties(50000.0)
                    .netFinancialImpact(700000.0)
                    .build();

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error calculating financial impact: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== Helper Methods ====================

    private Map<StarRatingMeasure, StarRatingCalculator.MeasureData> getSampleMeasureData() {
        // Sample data for demonstration
        return Map.of(
            StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, new StarRatingCalculator.MeasureData(750, 1000),
            StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL, new StarRatingCalculator.MeasureData(850, 1000),
            StarRatingMeasure.BREAST_CANCER_SCREENING, new StarRatingCalculator.MeasureData(720, 1000),
            StarRatingMeasure.COLORECTAL_CANCER_SCREENING, new StarRatingCalculator.MeasureData(680, 1000)
        );
    }

    private Map<String, MedicaidComplianceService.MeasurePerformance> getSampleMedicaidData() {
        // Sample data for demonstration
        return Map.of(
            "CBP", new MedicaidComplianceService.MeasurePerformance(700, 1000),
            "CDC-H9", new MedicaidComplianceService.MeasurePerformance(800, 1000),
            "BCS", new MedicaidComplianceService.MeasurePerformance(750, 1000)
        );
    }

    private MedicaidStateConfig getStateConfig(String stateCode) {
        return switch (stateCode.toUpperCase()) {
            case "NY" -> MedicaidStateConfig.StateConfigs.newYork();
            case "CA" -> MedicaidStateConfig.StateConfigs.california();
            case "TX" -> MedicaidStateConfig.StateConfigs.texas();
            case "FL" -> MedicaidStateConfig.StateConfigs.florida();
            default -> throw new IllegalArgumentException("Unsupported state: " + stateCode);
        };
    }

    private StarRatingReport createSampleStarReport(String planId) {
        Map<StarRatingMeasure, StarRatingCalculator.MeasureData> data = getSampleMeasureData();
        return starRatingCalculator.calculateStarRatingReport(
            planId,
            "Sample MA Plan",
            "H1234",
            2024,
            data,
            null
        );
    }

    private MedicaidComplianceReport createSampleComplianceReport(String mcoId) {
        MedicaidStateConfig config = MedicaidStateConfig.StateConfigs.newYork();
        Map<String, MedicaidComplianceService.MeasurePerformance> data = getSampleMedicaidData();

        return medicaidComplianceService.calculateComplianceReport(
            mcoId,
            "Sample MCO",
            config,
            "2024",
            2024,
            data
        );
    }

    // ==================== Request DTOs ====================

    public static class StarRatingCalculationRequest {
        private String planId;
        private String planName;
        private String contractNumber;
        private int reportingYear;
        private Map<StarRatingMeasure, StarRatingCalculator.MeasureData> measureData;
        private Map<StarRatingMeasure, Double> priorYearData;

        // Getters and setters
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        public String getPlanName() { return planName; }
        public void setPlanName(String planName) { this.planName = planName; }
        public String getContractNumber() { return contractNumber; }
        public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }
        public int getReportingYear() { return reportingYear; }
        public void setReportingYear(int reportingYear) { this.reportingYear = reportingYear; }
        public Map<StarRatingMeasure, StarRatingCalculator.MeasureData> getMeasureData() { return measureData; }
        public void setMeasureData(Map<StarRatingMeasure, StarRatingCalculator.MeasureData> measureData) { this.measureData = measureData; }
        public Map<StarRatingMeasure, Double> getPriorYearData() { return priorYearData; }
        public void setPriorYearData(Map<StarRatingMeasure, Double> priorYearData) { this.priorYearData = priorYearData; }
    }

    public static class MedicaidComplianceCalculationRequest {
        private String mcoId;
        private String mcoName;
        private String stateCode;
        private String reportingPeriod;
        private int measurementYear;
        private Map<String, MedicaidComplianceService.MeasurePerformance> measurePerformance;
        private Map<String, Double> priorYearPerformance;

        // Getters and setters
        public String getMcoId() { return mcoId; }
        public void setMcoId(String mcoId) { this.mcoId = mcoId; }
        public String getMcoName() { return mcoName; }
        public void setMcoName(String mcoName) { this.mcoName = mcoName; }
        public String getStateCode() { return stateCode; }
        public void setStateCode(String stateCode) { this.stateCode = stateCode; }
        public String getReportingPeriod() { return reportingPeriod; }
        public void setReportingPeriod(String reportingPeriod) { this.reportingPeriod = reportingPeriod; }
        public int getMeasurementYear() { return measurementYear; }
        public void setMeasurementYear(int measurementYear) { this.measurementYear = measurementYear; }
        public Map<String, MedicaidComplianceService.MeasurePerformance> getMeasurePerformance() { return measurePerformance; }
        public void setMeasurePerformance(Map<String, MedicaidComplianceService.MeasurePerformance> measurePerformance) { this.measurePerformance = measurePerformance; }
        public Map<String, Double> getPriorYearPerformance() { return priorYearPerformance; }
        public void setPriorYearPerformance(Map<String, Double> priorYearPerformance) { this.priorYearPerformance = priorYearPerformance; }
    }
}
