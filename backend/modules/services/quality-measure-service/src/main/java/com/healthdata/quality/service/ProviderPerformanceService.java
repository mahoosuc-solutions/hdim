package com.healthdata.quality.service;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.quality.dto.MeasurePerformanceDTO;
import com.healthdata.quality.dto.MonthlyTrendDTO;
import com.healthdata.quality.dto.ProviderPerformanceResponse;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating provider performance metrics.
 * Aggregates quality measure results by provider and calculates
 * compliance rates, practice averages, trends, and percentile rankings.
 *
 * Issue #146: Create Provider Performance Metrics API
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProviderPerformanceService {

    private final QualityMeasureResultRepository resultRepository;

    // National benchmark thresholds (would come from configuration in production)
    private static final Map<String, Double> NATIONAL_BENCHMARKS = Map.of(
        "CMS122v11", 85.0,  // Diabetes HbA1c Poor Control
        "CMS125v11", 78.0,  // Breast Cancer Screening
        "CMS130v11", 72.0,  // Colorectal Cancer Screening
        "CMS165v11", 80.0,  // Controlling High Blood Pressure
        "CMS347v5", 75.0    // Statin Therapy
    );

    // Performance thresholds
    private static final double BELOW_AVERAGE_THRESHOLD = 0.9; // 90% of practice average
    private static final double ABOVE_AVERAGE_THRESHOLD = 1.1; // 110% of practice average
    private static final double ATTENTION_THRESHOLD = 70.0;    // Below 70% requires attention

    /**
     * Calculate provider performance metrics for a given period.
     *
     * @param tenantId   Tenant identifier
     * @param providerId Provider identifier
     * @param measureIds Optional list of measure IDs to filter
     * @param period     Time period: YTD, LAST_12_MONTHS, LAST_QUARTER
     * @return ProviderPerformanceResponse with calculated metrics
     */
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ProviderPerformanceResponse getProviderPerformance(
            String tenantId,
            UUID providerId,
            List<String> measureIds,
            String period) {

        log.info("Calculating performance metrics for provider {} in tenant {}", providerId, tenantId);

        // Calculate date range based on period
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(period, endDate);

        // Get all results for the provider within the date range
        List<QualityMeasureResultEntity> providerResults = getProviderResults(
            tenantId, providerId, startDate, endDate, measureIds);

        // Get all results for practice (for comparison)
        List<QualityMeasureResultEntity> practiceResults = getPracticeResults(
            tenantId, startDate, endDate, measureIds);

        // Group results by measure
        Map<String, List<QualityMeasureResultEntity>> providerByMeasure = groupByMeasure(providerResults);
        Map<String, List<QualityMeasureResultEntity>> practiceByMeasure = groupByMeasure(practiceResults);

        // Calculate performance for each measure
        List<MeasurePerformanceDTO> measurePerformances = new ArrayList<>();
        List<String> improvementAreas = new ArrayList<>();
        List<String> strengthAreas = new ArrayList<>();

        for (String measureId : providerByMeasure.keySet()) {
            MeasurePerformanceDTO measurePerf = calculateMeasurePerformance(
                measureId,
                providerByMeasure.get(measureId),
                practiceByMeasure.getOrDefault(measureId, Collections.emptyList()),
                startDate,
                endDate
            );
            measurePerformances.add(measurePerf);

            // Categorize measure
            if ("below_average".equals(measurePerf.getPerformanceStatus())) {
                improvementAreas.add(measurePerf.getMeasureName());
            } else if (measurePerf.getPercentile() != null && measurePerf.getPercentile() >= 75) {
                strengthAreas.add(measurePerf.getMeasureName());
            }
        }

        // Calculate overall metrics
        double overallScore = calculateOverallScore(measurePerformances);
        int overallPercentile = calculateOverallPercentile(providerResults, practiceResults);
        int totalEligible = measurePerformances.stream()
            .mapToInt(m -> m.getDenominator() != null ? m.getDenominator() : 0)
            .sum();
        int totalCompliant = measurePerformances.stream()
            .mapToInt(m -> m.getNumerator() != null ? m.getNumerator() : 0)
            .sum();

        return ProviderPerformanceResponse.builder()
            .providerId(providerId)
            .providerName(getProviderName(providerId))
            .period(period)
            .measures(measurePerformances)
            .overallScore(Math.round(overallScore * 10.0) / 10.0)
            .overallPercentile(overallPercentile)
            .totalPatients(providerResults.size())
            .totalEligible(totalEligible)
            .totalCompliant(totalCompliant)
            .improvementAreas(improvementAreas)
            .strengthAreas(strengthAreas)
            .calculatedAt(Instant.now().toString())
            .build();
    }

    /**
     * Calculate start date based on period.
     */
    private LocalDate calculateStartDate(String period, LocalDate endDate) {
        return switch (period) {
            case "YTD" -> LocalDate.of(endDate.getYear(), 1, 1);
            case "LAST_QUARTER" -> endDate.minusMonths(3);
            case "LAST_12_MONTHS" -> endDate.minusMonths(12);
            default -> endDate.minusMonths(12);
        };
    }

    /**
     * Get results for a specific provider.
     */
    private List<QualityMeasureResultEntity> getProviderResults(
            String tenantId,
            UUID providerId,
            LocalDate startDate,
            LocalDate endDate,
            List<String> measureIds) {

        // In production, this would filter by provider ID
        // For now, we get all results and simulate provider filtering
        List<QualityMeasureResultEntity> results = resultRepository.findByTenantId(tenantId);

        return results.stream()
            .filter(r -> isWithinDateRange(r.getCalculationDate(), startDate, endDate))
            .filter(r -> measureIds == null || measureIds.isEmpty() ||
                        measureIds.contains(r.getMeasureId()))
            // Simulate provider filtering by using consistent hash
            .filter(r -> isForProvider(r, providerId))
            .collect(Collectors.toList());
    }

    /**
     * Get all results for practice (for comparison).
     */
    private List<QualityMeasureResultEntity> getPracticeResults(
            String tenantId,
            LocalDate startDate,
            LocalDate endDate,
            List<String> measureIds) {

        List<QualityMeasureResultEntity> results = resultRepository.findByTenantId(tenantId);

        return results.stream()
            .filter(r -> isWithinDateRange(r.getCalculationDate(), startDate, endDate))
            .filter(r -> measureIds == null || measureIds.isEmpty() ||
                        measureIds.contains(r.getMeasureId()))
            .collect(Collectors.toList());
    }

    /**
     * Check if calculation date is within range.
     */
    private boolean isWithinDateRange(LocalDate calculationDate, LocalDate startDate, LocalDate endDate) {
        if (calculationDate == null) return false;
        return !calculationDate.isBefore(startDate) && !calculationDate.isAfter(endDate);
    }

    /**
     * Simulate provider filtering (in production, would use actual provider assignment).
     */
    private boolean isForProvider(QualityMeasureResultEntity result, UUID providerId) {
        // Simple hash-based distribution for demo
        int hash = Math.abs(result.getPatientId().hashCode() ^ providerId.hashCode());
        return hash % 5 == 0; // ~20% of results per provider
    }

    /**
     * Group results by measure ID.
     */
    private Map<String, List<QualityMeasureResultEntity>> groupByMeasure(
            List<QualityMeasureResultEntity> results) {
        return results.stream()
            .collect(Collectors.groupingBy(QualityMeasureResultEntity::getMeasureId));
    }

    /**
     * Calculate performance metrics for a single measure.
     */
    private MeasurePerformanceDTO calculateMeasurePerformance(
            String measureId,
            List<QualityMeasureResultEntity> providerResults,
            List<QualityMeasureResultEntity> practiceResults,
            LocalDate startDate,
            LocalDate endDate) {

        // Calculate provider rate
        long providerEligible = providerResults.stream()
            .filter(QualityMeasureResultEntity::getDenominatorEligible)
            .count();
        long providerCompliant = providerResults.stream()
            .filter(r -> r.getDenominatorEligible() && r.getNumeratorCompliant())
            .count();
        double providerRate = providerEligible > 0 ?
            (providerCompliant * 100.0) / providerEligible : 0;

        // Calculate practice average
        long practiceEligible = practiceResults.stream()
            .filter(QualityMeasureResultEntity::getDenominatorEligible)
            .count();
        long practiceCompliant = practiceResults.stream()
            .filter(r -> r.getDenominatorEligible() && r.getNumeratorCompliant())
            .count();
        double practiceAverage = practiceEligible > 0 ?
            (practiceCompliant * 100.0) / practiceEligible : 0;

        // Calculate percentile
        int percentile = calculatePercentile(providerRate, practiceResults);

        // Determine performance status
        String performanceStatus;
        if (providerRate >= practiceAverage * ABOVE_AVERAGE_THRESHOLD) {
            performanceStatus = "above_average";
        } else if (providerRate < practiceAverage * BELOW_AVERAGE_THRESHOLD) {
            performanceStatus = "below_average";
        } else {
            performanceStatus = "average";
        }

        // Calculate trend
        List<MonthlyTrendDTO> trend = calculateMonthlyTrend(providerResults, practiceResults, startDate, endDate);

        // Calculate change from previous period
        double changeFromPrevious = calculateChangeFromPrevious(trend);
        String trendDirection = changeFromPrevious > 2 ? "improving" :
                               changeFromPrevious < -2 ? "declining" : "stable";

        // Get measure name (would come from measure registry in production)
        String measureName = getMeasureName(measureId);

        return MeasurePerformanceDTO.builder()
            .measureId(measureId)
            .measureName(measureName)
            .category(getCategoryFromMeasureId(measureId))
            .providerRate(Math.round(providerRate * 10.0) / 10.0)
            .practiceAverage(Math.round(practiceAverage * 10.0) / 10.0)
            .nationalBenchmark(NATIONAL_BENCHMARKS.get(measureId))
            .percentile(percentile)
            .numerator((int) providerCompliant)
            .denominator((int) providerEligible)
            .trend(trend)
            .changeFromPrevious(Math.round(changeFromPrevious * 10.0) / 10.0)
            .trendDirection(trendDirection)
            .performanceStatus(performanceStatus)
            .requiresAttention(providerRate < ATTENTION_THRESHOLD)
            .gapToNextTier(calculateGapToNextTier(providerRate))
            .build();
    }

    /**
     * Calculate monthly trend data.
     */
    private List<MonthlyTrendDTO> calculateMonthlyTrend(
            List<QualityMeasureResultEntity> providerResults,
            List<QualityMeasureResultEntity> practiceResults,
            LocalDate startDate,
            LocalDate endDate) {

        List<MonthlyTrendDTO> trend = new ArrayList<>();
        YearMonth current = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);

        while (!current.isAfter(end)) {
            YearMonth month = current;

            // Filter results for this month
            List<QualityMeasureResultEntity> monthProviderResults = providerResults.stream()
                .filter(r -> r.getCalculationDate() != null &&
                    YearMonth.from(r.getCalculationDate()).equals(month))
                .collect(Collectors.toList());

            List<QualityMeasureResultEntity> monthPracticeResults = practiceResults.stream()
                .filter(r -> r.getCalculationDate() != null &&
                    YearMonth.from(r.getCalculationDate()).equals(month))
                .collect(Collectors.toList());

            // Calculate rates
            long providerEligible = monthProviderResults.stream()
                .filter(QualityMeasureResultEntity::getDenominatorEligible)
                .count();
            long providerCompliant = monthProviderResults.stream()
                .filter(r -> r.getDenominatorEligible() && r.getNumeratorCompliant())
                .count();
            double providerRate = providerEligible > 0 ?
                (providerCompliant * 100.0) / providerEligible : 0;

            long practiceEligible = monthPracticeResults.stream()
                .filter(QualityMeasureResultEntity::getDenominatorEligible)
                .count();
            long practiceCompliant = monthPracticeResults.stream()
                .filter(r -> r.getDenominatorEligible() && r.getNumeratorCompliant())
                .count();
            double practiceAverage = practiceEligible > 0 ?
                (practiceCompliant * 100.0) / practiceEligible : 0;

            if (providerEligible > 0) {
                trend.add(MonthlyTrendDTO.builder()
                    .month(month.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .rate(Math.round(providerRate * 10.0) / 10.0)
                    .numerator((int) providerCompliant)
                    .denominator((int) providerEligible)
                    .practiceAverage(Math.round(practiceAverage * 10.0) / 10.0)
                    .build());
            }

            current = current.plusMonths(1);
        }

        return trend;
    }

    /**
     * Calculate percentile ranking within practice.
     */
    private int calculatePercentile(double providerRate, List<QualityMeasureResultEntity> practiceResults) {
        // Simplified percentile calculation
        // In production, would aggregate by provider and calculate actual percentile
        if (providerRate >= 90) return 95;
        if (providerRate >= 80) return 75;
        if (providerRate >= 70) return 50;
        if (providerRate >= 60) return 25;
        return 10;
    }

    /**
     * Calculate change from previous period.
     */
    private double calculateChangeFromPrevious(List<MonthlyTrendDTO> trend) {
        if (trend == null || trend.size() < 2) return 0;

        // Compare last month to average of previous months
        Double lastRate = trend.get(trend.size() - 1).getRate();
        Double previousRate = trend.get(trend.size() - 2).getRate();

        if (lastRate == null || previousRate == null) return 0;
        return lastRate - previousRate;
    }

    /**
     * Calculate gap to next performance tier.
     */
    private int calculateGapToNextTier(double currentRate) {
        if (currentRate >= 90) return 0;
        if (currentRate >= 80) return (int) Math.ceil(90 - currentRate);
        if (currentRate >= 70) return (int) Math.ceil(80 - currentRate);
        return (int) Math.ceil(70 - currentRate);
    }

    /**
     * Calculate overall score from measure performances.
     */
    private double calculateOverallScore(List<MeasurePerformanceDTO> measures) {
        if (measures.isEmpty()) return 0;

        double sum = measures.stream()
            .mapToDouble(m -> m.getProviderRate() != null ? m.getProviderRate() : 0)
            .sum();

        return sum / measures.size();
    }

    /**
     * Calculate overall percentile.
     */
    private int calculateOverallPercentile(
            List<QualityMeasureResultEntity> providerResults,
            List<QualityMeasureResultEntity> practiceResults) {

        long providerCompliant = providerResults.stream()
            .filter(r -> r.getDenominatorEligible() && r.getNumeratorCompliant())
            .count();
        long providerEligible = providerResults.stream()
            .filter(QualityMeasureResultEntity::getDenominatorEligible)
            .count();

        double providerRate = providerEligible > 0 ?
            (providerCompliant * 100.0) / providerEligible : 0;

        return calculatePercentile(providerRate, practiceResults);
    }

    /**
     * Get provider name (mock implementation).
     */
    private String getProviderName(UUID providerId) {
        // In production, would look up from provider service
        return "Dr. Provider " + providerId.toString().substring(0, 8);
    }

    /**
     * Get measure name from ID.
     */
    private String getMeasureName(String measureId) {
        return switch (measureId) {
            case "CMS122v11" -> "Diabetes: HbA1c Poor Control";
            case "CMS125v11" -> "Breast Cancer Screening";
            case "CMS130v11" -> "Colorectal Cancer Screening";
            case "CMS165v11" -> "Controlling High Blood Pressure";
            case "CMS347v5" -> "Statin Therapy for Cardiovascular Disease";
            default -> measureId;
        };
    }

    /**
     * Get category from measure ID.
     */
    private String getCategoryFromMeasureId(String measureId) {
        if (measureId.startsWith("CMS")) return "CMS";
        if (measureId.contains("HEDIS")) return "HEDIS";
        return "CUSTOM";
    }
}
