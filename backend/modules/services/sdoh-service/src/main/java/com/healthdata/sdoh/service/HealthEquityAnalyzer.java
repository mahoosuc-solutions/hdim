package com.healthdata.sdoh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.repository.SdohAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Health Equity Analytics and Disparity Measurement Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthEquityAnalyzer {

    private final SdohAssessmentRepository assessmentRepository;
    private final ObjectMapper objectMapper;

    public EquityReport generateEquityReport(String tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        List<DisparityMetric> disparityMetrics = new ArrayList<>();

        // Calculate disparities by race
        disparityMetrics.addAll(calculateDisparityMetrics(
                tenantId, DisparityMetric.StratificationType.RACE, startDate, endDate));

        // Calculate disparities by ethnicity
        disparityMetrics.addAll(calculateDisparityMetrics(
                tenantId, DisparityMetric.StratificationType.ETHNICITY, startDate, endDate));

        List<String> keyFindings = generateKeyFindings(disparityMetrics);
        List<String> recommendations = generateRecommendations(keyFindings);

        return EquityReport.builder()
                .reportId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .reportDate(LocalDateTime.now())
                .startDate(startDate)
                .endDate(endDate)
                .disparityMetrics(disparityMetrics)
                .keyFindings(keyFindings)
                .recommendations(recommendations)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public List<DisparityMetric> calculateDisparityMetrics(String tenantId,
                                                           DisparityMetric.StratificationType stratificationType,
                                                           LocalDateTime startDate, LocalDateTime endDate) {
        List<DisparityMetric> metrics = new ArrayList<>();

        // Sample stratification values
        String[] stratificationValues = getStratificationValues(stratificationType);

        for (String value : stratificationValues) {
            DisparityMetric metric = DisparityMetric.builder()
                    .metricId(UUID.randomUUID().toString())
                    .metricName("Food Insecurity Rate")
                    .stratificationType(stratificationType)
                    .stratificationValue(value)
                    .metricValue(0.25) // 25% sample rate
                    .benchmarkValue(0.15) // 15% benchmark
                    .disparityRatio(calculateDisparityRatio(0.25, 0.15))
                    .unit("percentage")
                    .description("Rate of food insecurity in population")
                    .build();

            metrics.add(metric);
        }

        return metrics;
    }

    public double calculateDisparityRatio(double groupValue, double benchmarkValue) {
        if (benchmarkValue == 0) return 0;
        return groupValue / benchmarkValue;
    }

    public Map<String, Map<SdohCategory, Double>> analyzeSdohPrevalence(String tenantId,
                                                                         DisparityMetric.StratificationType stratificationType,
                                                                         LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Map<SdohCategory, Double>> prevalence = new HashMap<>();
        String[] stratificationValues = getStratificationValues(stratificationType);

        for (String value : stratificationValues) {
            Map<SdohCategory, Double> categoryPrevalence = new HashMap<>();
            categoryPrevalence.put(SdohCategory.FOOD_INSECURITY, 0.20);
            categoryPrevalence.put(SdohCategory.HOUSING_INSTABILITY, 0.15);
            prevalence.put(value, categoryPrevalence);
        }

        return prevalence;
    }

    public List<DisparityMetric> identifySignificantDisparities(List<DisparityMetric> metrics, double threshold) {
        return metrics.stream()
                .filter(m -> m.getDisparityRatio() >= threshold)
                .collect(Collectors.toList());
    }

    public List<String> generateKeyFindings(List<DisparityMetric> metrics) {
        List<String> findings = new ArrayList<>();

        List<DisparityMetric> significant = identifySignificantDisparities(metrics, 1.5);
        if (!significant.isEmpty()) {
            findings.add("Significant health disparities identified in " + significant.size() + " metrics");
        }

        return findings;
    }

    public List<String> generateRecommendations(List<String> findings) {
        List<String> recommendations = new ArrayList<>();

        if (!findings.isEmpty()) {
            recommendations.add("Implement targeted interventions for high-disparity populations");
            recommendations.add("Increase SDOH screening in underserved communities");
            recommendations.add("Strengthen community resource partnerships");
        }

        return recommendations;
    }

    public Map<String, Double> analyzeScreeningCompletionRates(String tenantId,
                                                                DisparityMetric.StratificationType stratificationType,
                                                                LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Double> rates = new HashMap<>();
        String[] values = getStratificationValues(stratificationType);

        for (String value : values) {
            rates.put(value, 0.75); // 75% sample completion rate
        }

        return rates;
    }

    public Map<String, Double> analyzeReferralRates(String tenantId,
                                                     DisparityMetric.StratificationType stratificationType,
                                                     LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Double> rates = new HashMap<>();
        String[] values = getStratificationValues(stratificationType);

        for (String value : values) {
            rates.put(value, 0.60); // 60% sample referral rate
        }

        return rates;
    }

    public List<DisparityMetric> calculateHealthOutcomeDisparities(String tenantId,
                                                                    DisparityMetric.StratificationType stratificationType,
                                                                    LocalDateTime startDate, LocalDateTime endDate) {
        return calculateDisparityMetrics(tenantId, stratificationType, startDate, endDate);
    }

    public Map<String, List<Double>> trackDisparityTrends(String tenantId,
                                                           DisparityMetric.StratificationType stratificationType,
                                                           String metricName, int months) {
        Map<String, List<Double>> trends = new HashMap<>();
        String[] values = getStratificationValues(stratificationType);

        for (String value : values) {
            List<Double> monthlyValues = new ArrayList<>();
            for (int i = 0; i < months; i++) {
                monthlyValues.add(0.20 + (i * 0.01)); // Sample trend
            }
            trends.put(value, monthlyValues);
        }

        return trends;
    }

    public String exportReportAsJson(EquityReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            log.error("Error exporting report to JSON", e);
            return "{}";
        }
    }

    public boolean isValidStratificationType(DisparityMetric.StratificationType type) {
        return type != null;
    }

    private String[] getStratificationValues(DisparityMetric.StratificationType type) {
        return switch (type) {
            case RACE -> new String[]{"White", "Black or African American", "Asian", "Other"};
            case ETHNICITY -> new String[]{"Hispanic or Latino", "Not Hispanic or Latino"};
            case LANGUAGE -> new String[]{"English", "Spanish", "Other"};
            case GEOGRAPHY -> new String[]{"Urban", "Suburban", "Rural"};
            case INSURANCE_STATUS -> new String[]{"Private", "Medicare", "Medicaid", "Uninsured"};
            case INCOME_LEVEL -> new String[]{"Low", "Medium", "High"};
            case AGE_GROUP -> new String[]{"18-34", "35-54", "55-64", "65+"};
            case GENDER -> new String[]{"Male", "Female", "Other"};
        };
    }
}
