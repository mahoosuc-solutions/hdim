package com.healthdata.predictive.service;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.predictive.dto.*;
import com.healthdata.predictive.entity.InsightDismissalEntity;
import com.healthdata.predictive.model.RiskCohort;
import com.healthdata.predictive.model.RiskTier;
import com.healthdata.predictive.repository.InsightDismissalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Issue #19: Population Health Insights Engine
 *
 * Service for generating AI-powered population health insights.
 * Implements pattern detection algorithms for:
 * - Care Gap Clusters (>10 patients with same gap)
 * - Performance Trends (>5% change in 30 days)
 * - At-Risk Populations (risk score increases)
 * - Intervention Opportunities (batch outreach potential)
 *
 * HIPAA Compliance:
 * - All PHI access is audited
 * - Cache TTL is limited to 5 minutes (300 seconds)
 * - Multi-tenant filtering enforced
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PopulationHealthInsightsService {

    private final InsightDismissalRepository dismissalRepository;
    private final PopulationRiskStratifier riskStratifier;

    // Thresholds for insight generation
    private static final int CARE_GAP_CLUSTER_THRESHOLD = 10;
    private static final double PERFORMANCE_TREND_THRESHOLD = 5.0; // 5% change
    private static final int AT_RISK_THRESHOLD = 5; // Minimum patients for at-risk insight

    /**
     * Generate population health insights for a provider's panel.
     *
     * @param tenantId   Tenant context
     * @param providerId Provider identifier
     * @param panelData  Patient panel data including care gaps, metrics, and risk scores
     * @return Population insights response
     */
    @Audited(action = AuditAction.READ, resourceType = "PopulationInsights",
             description = "Generate population health insights")
    @Cacheable(value = "populationInsights",
               key = "#tenantId + ':' + #providerId",
               unless = "#result == null")
    public PopulationInsightsResponse generateInsights(
            String tenantId,
            String providerId,
            Map<String, Object> panelData) {

        log.info("Generating population health insights for provider {} in tenant {}", providerId, tenantId);

        Instant now = Instant.now();

        // Get dismissed insight keys to filter them out
        Set<String> dismissedKeys = dismissalRepository.findDismissedInsightKeys(tenantId, providerId, now);

        List<PopulationInsight> insights = new ArrayList<>();

        // Generate care gap cluster insights
        insights.addAll(detectCareGapClusters(tenantId, providerId, panelData, dismissedKeys));

        // Generate performance trend insights
        insights.addAll(detectPerformanceTrends(tenantId, providerId, panelData, dismissedKeys));

        // Generate at-risk population insights
        insights.addAll(detectAtRiskPopulations(tenantId, providerId, panelData, dismissedKeys));

        // Generate intervention opportunity insights
        insights.addAll(detectInterventionOpportunities(tenantId, providerId, panelData, dismissedKeys));

        // Sort by impact and affected patient count
        insights.sort((a, b) -> {
            int impactCompare = getImpactWeight(b.getImpact()) - getImpactWeight(a.getImpact());
            if (impactCompare != 0) return impactCompare;
            return b.getAffectedPatients() - a.getAffectedPatients();
        });

        // Build summary
        PopulationInsightsResponse.InsightSummary summary = buildSummary(insights, dismissedKeys.size());

        // Get panel size from data
        int panelSize = extractPanelSize(panelData);

        return PopulationInsightsResponse.builder()
                .providerId(providerId)
                .tenantId(tenantId)
                .generatedAt(now)
                .totalPanelSize(panelSize)
                .insights(insights)
                .summary(summary)
                .build();
    }

    /**
     * Dismiss an insight with reason tracking.
     */
    @Transactional
    @Audited(action = AuditAction.UPDATE, resourceType = "PopulationInsight",
             description = "Dismiss population health insight")
    @CacheEvict(value = "populationInsights", key = "#tenantId + ':' + #providerId")
    public void dismissInsight(
            String tenantId,
            String providerId,
            String userId,
            InsightDismissalRequest request) {

        log.info("Dismissing insight {} for provider {} by user {}", request.getInsightId(), providerId, userId);

        // Create dismissal record
        InsightDismissalEntity dismissal = InsightDismissalEntity.builder()
                .tenantId(tenantId)
                .providerId(providerId)
                .insightId(request.getInsightId())
                .insightKey(generateInsightKey(request.getInsightId().toString())) // Simplified key
                .reason(request.getReason())
                .dismissedBy(userId)
                .dismissedAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS)) // Dismissals expire after 30 days
                .active(true)
                .build();

        dismissalRepository.save(dismissal);
    }

    /**
     * Restore a dismissed insight.
     */
    @Transactional
    @Audited(action = AuditAction.UPDATE, resourceType = "PopulationInsight",
             description = "Restore dismissed insight")
    @CacheEvict(value = "populationInsights", key = "#tenantId + ':' + #providerId")
    public void restoreInsight(String tenantId, String providerId, UUID insightId) {
        log.info("Restoring insight {} for provider {}", insightId, providerId);

        dismissalRepository.findByTenantIdAndProviderIdAndInsightIdAndActiveTrue(tenantId, providerId, insightId)
                .ifPresent(dismissal -> {
                    dismissal.setActive(false);
                    dismissalRepository.save(dismissal);
                });
    }

    /**
     * Get dismissed insights for a provider.
     */
    public List<InsightDismissalEntity> getDismissedInsights(String tenantId, String providerId) {
        return dismissalRepository.findActiveDismissals(tenantId, providerId, Instant.now());
    }

    // ==================== Pattern Detection Algorithms ====================

    /**
     * Detect Care Gap Clusters: >10 patients with the same care gap.
     */
    @SuppressWarnings("unchecked")
    private List<PopulationInsight> detectCareGapClusters(
            String tenantId,
            String providerId,
            Map<String, Object> panelData,
            Set<String> dismissedKeys) {

        List<PopulationInsight> insights = new ArrayList<>();

        // Extract care gaps from panel data
        Map<String, List<String>> gapsByMeasure = extractGapsByMeasure(panelData);

        for (Map.Entry<String, List<String>> entry : gapsByMeasure.entrySet()) {
            String measureId = entry.getKey();
            List<String> patientIds = entry.getValue();

            // Check threshold
            if (patientIds.size() < CARE_GAP_CLUSTER_THRESHOLD) {
                continue;
            }

            // Generate insight key
            String insightKey = generateInsightKey(InsightType.CARE_GAP_CLUSTER.name() + ":" + measureId);
            if (dismissedKeys.contains(insightKey)) {
                continue;
            }

            // Determine impact based on patient count
            InsightImpact impact = determineImpact(patientIds.size());

            // Get measure name
            String measureName = getMeasureName(measureId, panelData);

            // Build insight
            PopulationInsight insight = PopulationInsight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.CARE_GAP_CLUSTER)
                    .title(measureName + " Gap Cluster")
                    .description(String.format(
                            "%d patients have not completed %s screening. Consider batch outreach.",
                            patientIds.size(), measureName))
                    .impact(impact)
                    .affectedPatients(patientIds.size())
                    .affectedPatientIds(patientIds)
                    .suggestedAction(SuggestedAction.builder()
                            .type(SuggestedAction.ActionType.BATCH_OUTREACH)
                            .description(String.format("Send %s screening reminder to all %d patients",
                                    measureName, patientIds.size()))
                            .estimatedEffortMinutes(30)
                            .priority(impact == InsightImpact.HIGH ? 90 : impact == InsightImpact.MEDIUM ? 70 : 50)
                            .build())
                    .metrics(Map.of(
                            "measureId", measureId,
                            "measureName", measureName,
                            "potentialComplianceImprovement", calculateComplianceImprovement(patientIds.size(), panelData)
                    ))
                    .source(PopulationInsight.InsightSource.builder()
                            .dataType("care_gaps")
                            .measureId(measureId)
                            .measureName(measureName)
                            .dataAsOf(Instant.now())
                            .build())
                    .generatedAt(Instant.now())
                    .dismissed(false)
                    .build();

            insights.add(insight);
        }

        return insights;
    }

    /**
     * Detect Performance Trends: >5% change in metrics over 30 days.
     */
    @SuppressWarnings("unchecked")
    private List<PopulationInsight> detectPerformanceTrends(
            String tenantId,
            String providerId,
            Map<String, Object> panelData,
            Set<String> dismissedKeys) {

        List<PopulationInsight> insights = new ArrayList<>();

        // Extract metric trends from panel data
        Map<String, Map<String, Object>> metricTrends = extractMetricTrends(panelData);

        for (Map.Entry<String, Map<String, Object>> entry : metricTrends.entrySet()) {
            String metricId = entry.getKey();
            Map<String, Object> trend = entry.getValue();

            double changePercent = ((Number) trend.getOrDefault("changePercent", 0.0)).doubleValue();

            // Check threshold
            if (Math.abs(changePercent) < PERFORMANCE_TREND_THRESHOLD) {
                continue;
            }

            String insightKey = generateInsightKey(InsightType.PERFORMANCE_TREND.name() + ":" + metricId);
            if (dismissedKeys.contains(insightKey)) {
                continue;
            }

            boolean isImproving = changePercent > 0;
            InsightImpact impact = Math.abs(changePercent) > 15 ? InsightImpact.HIGH :
                                   Math.abs(changePercent) > 8 ? InsightImpact.MEDIUM : InsightImpact.LOW;

            String metricName = (String) trend.getOrDefault("name", metricId);
            int affectedCount = ((Number) trend.getOrDefault("affectedPatients", 0)).intValue();

            PopulationInsight insight = PopulationInsight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.PERFORMANCE_TREND)
                    .title(String.format("%s %s (%+.1f%%)",
                            metricName, isImproving ? "Improving" : "Declining", changePercent))
                    .description(String.format(
                            "%s control has %s %.1f%% over the past 30 days affecting %d patients.",
                            metricName, isImproving ? "improved by" : "declined by",
                            Math.abs(changePercent), affectedCount))
                    .impact(impact)
                    .affectedPatients(affectedCount)
                    .suggestedAction(isImproving ?
                            SuggestedAction.builder()
                                    .type(SuggestedAction.ActionType.PATIENT_EDUCATION)
                                    .description("Continue current interventions and document successful strategies")
                                    .estimatedEffortMinutes(15)
                                    .priority(40)
                                    .build() :
                            SuggestedAction.builder()
                                    .type(SuggestedAction.ActionType.CARE_COORDINATION)
                                    .description(String.format("Review %s management protocols and patient adherence", metricName))
                                    .estimatedEffortMinutes(45)
                                    .priority(80)
                                    .build())
                    .metrics(Map.of(
                            "metricId", metricId,
                            "changePercent", changePercent,
                            "previousValue", trend.getOrDefault("previousValue", 0),
                            "currentValue", trend.getOrDefault("currentValue", 0),
                            "isImproving", isImproving
                    ))
                    .source(PopulationInsight.InsightSource.builder()
                            .dataType("performance_metrics")
                            .measureId(metricId)
                            .measureName(metricName)
                            .dataAsOf(Instant.now())
                            .build())
                    .generatedAt(Instant.now())
                    .dismissed(false)
                    .build();

            insights.add(insight);
        }

        return insights;
    }

    /**
     * Detect At-Risk Populations: Patients with increasing risk scores.
     */
    @SuppressWarnings("unchecked")
    private List<PopulationInsight> detectAtRiskPopulations(
            String tenantId,
            String providerId,
            Map<String, Object> panelData,
            Set<String> dismissedKeys) {

        List<PopulationInsight> insights = new ArrayList<>();

        // Extract risk tier changes
        Map<String, Object> riskData = (Map<String, Object>) panelData.getOrDefault("riskData", new HashMap<>());
        List<String> newHighRiskPatients = (List<String>) riskData.getOrDefault("newHighRiskPatients", new ArrayList<>());
        List<String> newVeryHighRiskPatients = (List<String>) riskData.getOrDefault("newVeryHighRiskPatients", new ArrayList<>());

        // Combine high risk patients
        List<String> elevatedRiskPatients = new ArrayList<>();
        elevatedRiskPatients.addAll(newHighRiskPatients);
        elevatedRiskPatients.addAll(newVeryHighRiskPatients);

        if (elevatedRiskPatients.size() >= AT_RISK_THRESHOLD) {
            String insightKey = generateInsightKey(InsightType.AT_RISK_POPULATION.name() + ":elevated");
            if (!dismissedKeys.contains(insightKey)) {
                InsightImpact impact = elevatedRiskPatients.size() > 20 ? InsightImpact.HIGH :
                                       elevatedRiskPatients.size() > 10 ? InsightImpact.MEDIUM : InsightImpact.LOW;

                PopulationInsight insight = PopulationInsight.builder()
                        .id(UUID.randomUUID())
                        .type(InsightType.AT_RISK_POPULATION)
                        .title("Elevated Risk Population")
                        .description(String.format(
                                "%d patients have moved to high-risk or very high-risk tier in the past 30 days. " +
                                "Proactive outreach may prevent adverse outcomes.",
                                elevatedRiskPatients.size()))
                        .impact(impact)
                        .affectedPatients(elevatedRiskPatients.size())
                        .affectedPatientIds(elevatedRiskPatients)
                        .suggestedAction(SuggestedAction.builder()
                                .type(SuggestedAction.ActionType.RISK_ASSESSMENT)
                                .description(String.format(
                                        "Conduct comprehensive risk assessments for %d high-risk patients",
                                        elevatedRiskPatients.size()))
                                .estimatedEffortMinutes(elevatedRiskPatients.size() * 15)
                                .priority(95)
                                .build())
                        .metrics(Map.of(
                                "newHighRiskCount", newHighRiskPatients.size(),
                                "newVeryHighRiskCount", newVeryHighRiskPatients.size(),
                                "totalElevatedCount", elevatedRiskPatients.size()
                        ))
                        .source(PopulationInsight.InsightSource.builder()
                                .dataType("risk_scores")
                                .dataAsOf(Instant.now())
                                .build())
                        .generatedAt(Instant.now())
                        .dismissed(false)
                        .build();

                insights.add(insight);
            }
        }

        return insights;
    }

    /**
     * Detect Intervention Opportunities: Similar gaps across similar patients.
     */
    @SuppressWarnings("unchecked")
    private List<PopulationInsight> detectInterventionOpportunities(
            String tenantId,
            String providerId,
            Map<String, Object> panelData,
            Set<String> dismissedKeys) {

        List<PopulationInsight> insights = new ArrayList<>();

        // Extract demographic cohorts with common gaps
        Map<String, List<String>> cohortGaps = extractCohortGaps(panelData);

        for (Map.Entry<String, List<String>> entry : cohortGaps.entrySet()) {
            String cohortKey = entry.getKey();
            List<String> patientIds = entry.getValue();

            if (patientIds.size() < 5) {
                continue; // Minimum cohort size for intervention
            }

            String insightKey = generateInsightKey(InsightType.INTERVENTION_OPPORTUNITY.name() + ":" + cohortKey);
            if (dismissedKeys.contains(insightKey)) {
                continue;
            }

            // Parse cohort key (e.g., "age:50-75:gap:COL")
            String[] parts = cohortKey.split(":");
            String ageRange = parts.length > 1 ? parts[1] : "all ages";
            String gapType = parts.length > 3 ? parts[3] : "multiple gaps";

            InsightImpact impact = patientIds.size() > 20 ? InsightImpact.HIGH :
                                   patientIds.size() > 10 ? InsightImpact.MEDIUM : InsightImpact.LOW;

            PopulationInsight insight = PopulationInsight.builder()
                    .id(UUID.randomUUID())
                    .type(InsightType.INTERVENTION_OPPORTUNITY)
                    .title(String.format("Batch Intervention: %s", gapType))
                    .description(String.format(
                            "%d patients aged %s share similar care gaps. A targeted batch intervention " +
                            "could efficiently address this cohort.",
                            patientIds.size(), ageRange))
                    .impact(impact)
                    .affectedPatients(patientIds.size())
                    .affectedPatientIds(patientIds)
                    .suggestedAction(SuggestedAction.builder()
                            .type(SuggestedAction.ActionType.BATCH_OUTREACH)
                            .description(String.format(
                                    "Schedule group health education session or batch appointment scheduling for %d patients",
                                    patientIds.size()))
                            .estimatedEffortMinutes(60)
                            .priority(impact == InsightImpact.HIGH ? 85 : 65)
                            .build())
                    .metrics(Map.of(
                            "cohortKey", cohortKey,
                            "ageRange", ageRange,
                            "gapType", gapType,
                            "efficiencyGain", patientIds.size() * 0.7 // 70% efficiency gain estimate
                    ))
                    .source(PopulationInsight.InsightSource.builder()
                            .dataType("cohort_analysis")
                            .dataAsOf(Instant.now())
                            .build())
                    .generatedAt(Instant.now())
                    .dismissed(false)
                    .build();

            insights.add(insight);
        }

        return insights;
    }

    // ==================== Helper Methods ====================

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> extractGapsByMeasure(Map<String, Object> panelData) {
        Map<String, List<String>> result = new HashMap<>();

        Object careGapsObj = panelData.get("careGaps");
        if (careGapsObj instanceof Map) {
            Map<String, Object> careGaps = (Map<String, Object>) careGapsObj;
            for (Map.Entry<String, Object> entry : careGaps.entrySet()) {
                if (entry.getValue() instanceof List) {
                    result.put(entry.getKey(), (List<String>) entry.getValue());
                }
            }
        } else if (careGapsObj instanceof List) {
            // Handle list format: [{measureId, patientId}, ...]
            List<Map<String, Object>> gapsList = (List<Map<String, Object>>) careGapsObj;
            for (Map<String, Object> gap : gapsList) {
                String measureId = (String) gap.get("measureId");
                String patientId = (String) gap.get("patientId");
                if (measureId != null && patientId != null) {
                    result.computeIfAbsent(measureId, k -> new ArrayList<>()).add(patientId);
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> extractMetricTrends(Map<String, Object> panelData) {
        Object trendsObj = panelData.get("metricTrends");
        if (trendsObj instanceof Map) {
            return (Map<String, Map<String, Object>>) trendsObj;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> extractCohortGaps(Map<String, Object> panelData) {
        Object cohortsObj = panelData.get("cohortGaps");
        if (cohortsObj instanceof Map) {
            return (Map<String, List<String>>) cohortsObj;
        }
        return new HashMap<>();
    }

    private int extractPanelSize(Map<String, Object> panelData) {
        Object panelSize = panelData.get("panelSize");
        if (panelSize instanceof Number) {
            return ((Number) panelSize).intValue();
        }
        return 0;
    }

    private String getMeasureName(String measureId, Map<String, Object> panelData) {
        // Common measure mappings
        Map<String, String> measureNames = Map.of(
                "COL", "Colorectal Cancer",
                "BCS", "Breast Cancer",
                "CCS", "Cervical Cancer",
                "CDC", "Diabetes Care",
                "CBP", "Blood Pressure Control",
                "HBD", "Hemoglobin A1c",
                "OMW", "Osteoporosis Management",
                "SPR", "Statin Therapy"
        );

        return measureNames.getOrDefault(measureId.toUpperCase(), measureId);
    }

    private InsightImpact determineImpact(int patientCount) {
        if (patientCount >= 25) return InsightImpact.HIGH;
        if (patientCount >= 15) return InsightImpact.MEDIUM;
        return InsightImpact.LOW;
    }

    private int getImpactWeight(InsightImpact impact) {
        return switch (impact) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private double calculateComplianceImprovement(int patientCount, Map<String, Object> panelData) {
        int panelSize = extractPanelSize(panelData);
        if (panelSize == 0) return 0.0;
        return (patientCount * 100.0) / panelSize;
    }

    private String generateInsightKey(String source) {
        // Create a stable key for insight deduplication
        return source.toUpperCase().replaceAll("[^A-Z0-9:]", "_");
    }

    private PopulationInsightsResponse.InsightSummary buildSummary(List<PopulationInsight> insights, int dismissedCount) {
        int high = 0, medium = 0, low = 0;

        for (PopulationInsight insight : insights) {
            switch (insight.getImpact()) {
                case HIGH -> high++;
                case MEDIUM -> medium++;
                case LOW -> low++;
            }
        }

        return PopulationInsightsResponse.InsightSummary.builder()
                .highImpact(high)
                .mediumImpact(medium)
                .lowImpact(low)
                .dismissed(dismissedCount)
                .totalInsights(insights.size())
                .build();
    }
}
