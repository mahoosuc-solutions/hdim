package com.healthdata.payer.service;

import com.healthdata.payer.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating aggregated payer dashboard metrics.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PayerDashboardService {

    private final StarRatingCalculator starRatingCalculator;
    private final MedicaidComplianceService medicaidComplianceService;

    /**
     * Generate Medicare Advantage dashboard metrics.
     */
    public PayerDashboardMetrics generateMedicareDashboard(
        String payerId,
        String payerName,
        List<StarRatingReport> starReports
    ) {
        if (starReports.isEmpty()) {
            return createEmptyDashboard(payerId, payerName, PayerDashboardMetrics.DashboardType.MEDICARE_ADVANTAGE);
        }

        // Calculate Medicare-specific metrics
        double avgStarRating = starReports.stream()
            .mapToDouble(StarRatingReport::getOverallStarRating)
            .average()
            .orElse(0.0);

        int fourStarsOrMore = (int) starReports.stream()
            .filter(r -> r.getRoundedStarRating() >= 4)
            .count();

        int threeStarsOrLess = (int) starReports.stream()
            .filter(r -> r.getRoundedStarRating() <= 3)
            .count();

        double estimatedBonuses = starReports.stream()
            .filter(StarRatingReport::isQualityBonusPaymentEligible)
            .mapToDouble(r -> r.getTotalEnrollees() * r.getBonusPaymentPercentage() * 10)  // Simplified calculation
            .sum();

        double yoyImprovement = starReports.stream()
            .filter(r -> r.getOverallImprovement() != null)
            .mapToDouble(StarRatingReport::getOverallImprovement)
            .average()
            .orElse(0.0);

        PayerDashboardMetrics.MedicareAdvantageMetrics maMetrics =
            PayerDashboardMetrics.MedicareAdvantageMetrics.builder()
                .averageStarRating(Math.round(avgStarRating * 100.0) / 100.0)
                .plansWithFourStarsOrMore(fourStarsOrMore)
                .plansWithThreeStarsOrLess(threeStarsOrLess)
                .totalMedicarePlans(starReports.size())
                .qualityBonusPaymentEligible(fourStarsOrMore > 0)
                .estimatedBonusPayments(estimatedBonuses)
                .yearOverYearImprovement(yoyImprovement)
                .build();

        // Aggregate measure performance
        Map<String, Double> topMeasures = identifyTopMeasures(starReports);
        Map<String, Double> needsAttention = identifyMeasuresNeedingAttention(starReports);

        int totalEnrollment = starReports.stream()
            .mapToInt(StarRatingReport::getTotalEnrollees)
            .sum();

        return PayerDashboardMetrics.builder()
            .payerId(payerId)
            .payerName(payerName)
            .dashboardType(PayerDashboardMetrics.DashboardType.MEDICARE_ADVANTAGE)
            .totalEnrollment(totalEnrollment)
            .activePlans(starReports.size())
            .medicareMetrics(maMetrics)
            .topPerformingMeasures(topMeasures)
            .measuresNeedingAttention(needsAttention)
            .generatedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Generate Medicaid MCO dashboard metrics.
     */
    public PayerDashboardMetrics generateMedicaidDashboard(
        String payerId,
        String payerName,
        List<MedicaidComplianceReport> complianceReports
    ) {
        if (complianceReports.isEmpty()) {
            return createEmptyDashboard(payerId, payerName, PayerDashboardMetrics.DashboardType.MEDICAID_MCO);
        }

        double avgCompliance = complianceReports.stream()
            .mapToDouble(MedicaidComplianceReport::getOverallComplianceRate)
            .average()
            .orElse(0.0);

        int compliantPlans = (int) complianceReports.stream()
            .filter(r -> r.getOverallStatus() == MedicaidComplianceReport.ComplianceStatus.COMPLIANT ||
                         r.getOverallStatus() == MedicaidComplianceReport.ComplianceStatus.SUBSTANTIALLY_COMPLIANT)
            .count();

        int nonCompliantPlans = (int) complianceReports.stream()
            .filter(r -> r.getOverallStatus() == MedicaidComplianceReport.ComplianceStatus.NON_COMPLIANT)
            .count();

        // Count unique states
        int numberOfStates = (int) complianceReports.stream()
            .map(r -> r.getStateConfig().getStateCode())
            .distinct()
            .count();

        double estimatedPenalties = complianceReports.stream()
            .filter(r -> r.getPenaltyAssessment() != null && r.getPenaltyAssessment().isPenaltyApplied())
            .mapToDouble(r -> r.getPenaltyAssessment().getEstimatedPenaltyAmount())
            .sum();

        double estimatedBonuses = complianceReports.stream()
            .filter(MedicaidComplianceReport::isQualityBonusEligible)
            .mapToDouble(r -> r.getEstimatedBonus() != null ? r.getEstimatedBonus() : 0.0)
            .sum();

        PayerDashboardMetrics.MedicaidMcoMetrics medicaidMetrics =
            PayerDashboardMetrics.MedicaidMcoMetrics.builder()
                .numberOfStates(numberOfStates)
                .averageComplianceRate(Math.round(avgCompliance * 100.0) / 100.0)
                .compliantPlans(compliantPlans)
                .nonCompliantPlans(nonCompliantPlans)
                .estimatedPenalties(estimatedPenalties)
                .estimatedBonuses(estimatedBonuses)
                .build();

        int totalEnrollment = complianceReports.stream()
            .mapToInt(MedicaidComplianceReport::getTotalEnrollment)
            .sum();

        return PayerDashboardMetrics.builder()
            .payerId(payerId)
            .payerName(payerName)
            .dashboardType(PayerDashboardMetrics.DashboardType.MEDICAID_MCO)
            .totalEnrollment(totalEnrollment)
            .activePlans(complianceReports.size())
            .medicaidMetrics(medicaidMetrics)
            .generatedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Generate combined dashboard for multi-line payers.
     */
    public PayerDashboardMetrics generateCombinedDashboard(
        String payerId,
        String payerName,
        List<StarRatingReport> maReports,
        List<MedicaidComplianceReport> medicaidReports
    ) {
        PayerDashboardMetrics maDashboard = generateMedicareDashboard(payerId, payerName, maReports);
        PayerDashboardMetrics medicaidDashboard = generateMedicaidDashboard(payerId, payerName, medicaidReports);

        return PayerDashboardMetrics.builder()
            .payerId(payerId)
            .payerName(payerName)
            .dashboardType(PayerDashboardMetrics.DashboardType.ALL)
            .totalEnrollment(maDashboard.getTotalEnrollment() + medicaidDashboard.getTotalEnrollment())
            .activePlans(maDashboard.getActivePlans() + medicaidDashboard.getActivePlans())
            .medicareMetrics(maDashboard.getMedicareMetrics())
            .medicaidMetrics(medicaidDashboard.getMedicaidMetrics())
            .topPerformingMeasures(maDashboard.getTopPerformingMeasures())
            .measuresNeedingAttention(maDashboard.getMeasuresNeedingAttention())
            .generatedAt(LocalDateTime.now())
            .build();
    }

    private Map<String, Double> identifyTopMeasures(List<StarRatingReport> reports) {
        Map<StarRatingMeasure, List<Double>> measurePerformance = new HashMap<>();

        for (StarRatingReport report : reports) {
            if (report.getAllMeasureScores() != null) {
                for (MeasureScore score : report.getAllMeasureScores()) {
                    measurePerformance.computeIfAbsent(score.getMeasure(), k -> new ArrayList<>())
                        .add(score.getPerformanceRate());
                }
            }
        }

        return measurePerformance.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().getCode(),
                e -> e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, Double> identifyMeasuresNeedingAttention(List<StarRatingReport> reports) {
        Map<StarRatingMeasure, List<Double>> measurePerformance = new HashMap<>();

        for (StarRatingReport report : reports) {
            if (report.getAllMeasureScores() != null) {
                for (MeasureScore score : report.getAllMeasureScores()) {
                    if (score.getStars() < 4) {  // Focus on measures below 4 stars
                        measurePerformance.computeIfAbsent(score.getMeasure(), k -> new ArrayList<>())
                            .add(score.getPerformanceRate());
                    }
                }
            }
        }

        return measurePerformance.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().getCode(),
                e -> e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
            ))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(5)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private PayerDashboardMetrics createEmptyDashboard(
        String payerId,
        String payerName,
        PayerDashboardMetrics.DashboardType type
    ) {
        return PayerDashboardMetrics.builder()
            .payerId(payerId)
            .payerName(payerName)
            .dashboardType(type)
            .totalEnrollment(0)
            .activePlans(0)
            .generatedAt(LocalDateTime.now())
            .build();
    }
}
