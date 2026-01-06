package com.healthdata.caregap.service;

import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provider Care Gap Prioritization Service
 *
 * Issue #138: Provides provider-specific care gap prioritization with a scoring algorithm
 * that considers urgency (40%), due date (30%), and intervention ease (30%).
 *
 * Includes primary care-specific priority rules for A1c, BP, screenings, and
 * recommended actions for each gap.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProviderCareGapPrioritizationService {

    private final CareGapRepository careGapRepository;

    // Scoring weight constants
    private static final double URGENCY_WEIGHT = 0.40;
    private static final double DUE_DATE_WEIGHT = 0.30;
    private static final double INTERVENTION_EASE_WEIGHT = 0.30;

    // Primary care measure IDs for priority boost
    private static final Set<String> PRIMARY_CARE_MEASURES = Set.of(
            // Diabetes care - HbA1c
            "CDC", "CDC-HbA1c", "HBA1C", "DIABETES_POOR_CONTROL",
            // Blood pressure control
            "CBP", "BP_CONTROL", "HYPERTENSION",
            // Cancer screenings
            "COL", "COLORECTAL", "BCS", "BREAST_CANCER_SCREENING",
            "CCS", "CERVICAL_CANCER_SCREENING",
            // Preventive care
            "AWV", "ANNUAL_WELLNESS", "FLU", "INFLUENZA",
            // Medication adherence
            "SPC", "STATIN_THERAPY", "PDC"
    );

    // Measure IDs with high intervention ease (can be addressed in single visit)
    private static final Set<String> HIGH_INTERVENTION_EASE = Set.of(
            "FLU", "INFLUENZA", "AWV", "ANNUAL_WELLNESS",
            "BP_CONTROL", "CBP", "SPC", "STATIN_THERAPY"
    );

    // Measure IDs requiring referrals/specialty care (lower intervention ease)
    private static final Set<String> LOW_INTERVENTION_EASE = Set.of(
            "COL", "COLORECTAL", "BCS", "BREAST_CANCER_SCREENING",
            "CCS", "CERVICAL_CANCER_SCREENING", "RETINAL_EXAM"
    );

    /**
     * Get prioritized care gaps for a provider with scoring.
     *
     * @param tenantId Tenant ID
     * @param providerId Provider ID
     * @param limit Maximum number of gaps to return (default 50)
     * @return List of prioritized care gaps with scores and recommendations
     */
    @Cacheable(value = "providerPrioritizedGaps", key = "#tenantId + '_' + #providerId + '_' + #limit")
    public List<PrioritizedCareGap> getPrioritizedCareGaps(String tenantId, String providerId, int limit) {
        log.info("Getting prioritized care gaps for provider: {} in tenant: {}", providerId, tenantId);

        List<CareGapEntity> openGaps = careGapRepository.findOpenGapsByProviderWithLimit(
                tenantId, providerId, PageRequest.of(0, Math.max(limit * 2, 100)));

        if (openGaps.isEmpty()) {
            log.debug("No open gaps found for provider: {}", providerId);
            return new ArrayList<>();
        }

        List<PrioritizedCareGap> prioritizedGaps = openGaps.stream()
                .map(this::scoreCareGap)
                .sorted(Comparator.comparingDouble(PrioritizedCareGap::getPriorityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Returning {} prioritized gaps for provider: {}", prioritizedGaps.size(), providerId);
        return prioritizedGaps;
    }

    /**
     * Score a single care gap using the weighted algorithm.
     */
    private PrioritizedCareGap scoreCareGap(CareGapEntity gap) {
        double urgencyScore = calculateUrgencyScore(gap);
        double dueDateScore = calculateDueDateScore(gap);
        double interventionEaseScore = calculateInterventionEaseScore(gap);

        // Apply primary care boost if applicable
        double primaryCareBoost = isPrimaryCareRelevant(gap) ? 1.15 : 1.0;

        double totalScore = (urgencyScore * URGENCY_WEIGHT +
                dueDateScore * DUE_DATE_WEIGHT +
                interventionEaseScore * INTERVENTION_EASE_WEIGHT) * primaryCareBoost;

        // Cap at 100
        totalScore = Math.min(totalScore, 100.0);

        String recommendation = generateRecommendation(gap);
        List<String> suggestedActions = generateSuggestedActions(gap);

        return PrioritizedCareGap.builder()
                .gapId(gap.getId().toString())
                .patientId(gap.getPatientId().toString())
                .measureId(gap.getMeasureId())
                .measureName(gap.getMeasureName())
                .gapCategory(gap.getGapCategory())
                .priority(gap.getPriority())
                .dueDate(gap.getDueDate())
                .priorityScore(Math.round(totalScore * 100.0) / 100.0)
                .urgencyScore(Math.round(urgencyScore * 100.0) / 100.0)
                .dueDateScore(Math.round(dueDateScore * 100.0) / 100.0)
                .interventionEaseScore(Math.round(interventionEaseScore * 100.0) / 100.0)
                .isPrimaryCareRelevant(isPrimaryCareRelevant(gap))
                .recommendation(recommendation)
                .suggestedActions(suggestedActions)
                .recommendationType(gap.getRecommendationType())
                .gapDescription(gap.getGapDescription())
                .identifiedDate(gap.getIdentifiedDate())
                .daysUntilDue(calculateDaysUntilDue(gap))
                .build();
    }

    /**
     * Calculate urgency score (0-100) based on priority and severity.
     */
    private double calculateUrgencyScore(CareGapEntity gap) {
        double score = 50.0; // Base score

        // Priority contribution (0-40 points)
        String priority = gap.getPriority() != null ? gap.getPriority().toLowerCase() : "medium";
        switch (priority) {
            case "high" -> score += 40;
            case "medium" -> score += 20;
            case "low" -> score += 5;
        }

        // Severity contribution (0-30 points)
        String severity = gap.getSeverity() != null ? gap.getSeverity().toLowerCase() : "medium";
        switch (severity) {
            case "high" -> score += 30;
            case "medium" -> score += 15;
            case "low" -> score += 5;
        }

        // Star impact contribution (0-20 points)
        if (gap.getStarImpact() != null) {
            score += gap.getStarImpact().doubleValue() * 20;
        }

        // Risk score contribution (0-10 points)
        if (gap.getRiskScore() != null) {
            score += gap.getRiskScore() * 10;
        }

        return Math.min(score, 100.0);
    }

    /**
     * Calculate due date score (0-100) based on proximity to due date.
     */
    private double calculateDueDateScore(CareGapEntity gap) {
        if (gap.getDueDate() == null) {
            return 50.0; // Default score for gaps without due date
        }

        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), gap.getDueDate());

        if (daysUntilDue < 0) {
            // Overdue - maximum urgency
            return 100.0;
        } else if (daysUntilDue <= 7) {
            // Due within 1 week
            return 95.0;
        } else if (daysUntilDue <= 14) {
            // Due within 2 weeks
            return 85.0;
        } else if (daysUntilDue <= 30) {
            // Due within 1 month
            return 70.0;
        } else if (daysUntilDue <= 60) {
            // Due within 2 months
            return 50.0;
        } else if (daysUntilDue <= 90) {
            // Due within 3 months
            return 35.0;
        } else {
            // Due beyond 3 months
            return 20.0;
        }
    }

    /**
     * Calculate intervention ease score (0-100) based on measure type and requirements.
     */
    private double calculateInterventionEaseScore(CareGapEntity gap) {
        String measureId = gap.getMeasureId() != null ? gap.getMeasureId().toUpperCase() : "";
        String recommendationType = gap.getRecommendationType() != null ? gap.getRecommendationType().toLowerCase() : "";

        // High ease measures (can be done in single visit)
        for (String highEase : HIGH_INTERVENTION_EASE) {
            if (measureId.contains(highEase)) {
                return 90.0;
            }
        }

        // Low ease measures (require referrals/specialty)
        for (String lowEase : LOW_INTERVENTION_EASE) {
            if (measureId.contains(lowEase)) {
                return 40.0;
            }
        }

        // Based on recommendation type
        switch (recommendationType) {
            case "immunization" -> { return 85.0; }
            case "medication" -> { return 75.0; }
            case "screening" -> { return 50.0; }
            case "procedure" -> { return 35.0; }
            case "referral" -> { return 30.0; }
        }

        // Default medium ease
        return 60.0;
    }

    /**
     * Check if measure is relevant for primary care.
     */
    private boolean isPrimaryCareRelevant(CareGapEntity gap) {
        if (gap.getMeasureId() == null) {
            return false;
        }

        String measureId = gap.getMeasureId().toUpperCase();
        for (String pcMeasure : PRIMARY_CARE_MEASURES) {
            if (measureId.contains(pcMeasure)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate days until due date.
     */
    private Long calculateDaysUntilDue(CareGapEntity gap) {
        if (gap.getDueDate() == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), gap.getDueDate());
    }

    /**
     * Generate a recommendation based on the care gap.
     */
    private String generateRecommendation(CareGapEntity gap) {
        if (gap.getRecommendation() != null && !gap.getRecommendation().isBlank()) {
            return gap.getRecommendation();
        }

        String measureId = gap.getMeasureId() != null ? gap.getMeasureId().toUpperCase() : "";
        String measureName = gap.getMeasureName() != null ? gap.getMeasureName() : "Care Gap";

        // Generate measure-specific recommendations
        if (measureId.contains("HBA1C") || measureId.contains("CDC")) {
            return "Schedule HbA1c test. Consider medication adjustment if result shows poor control (>9%).";
        }
        if (measureId.contains("BP") || measureId.contains("HYPERTENSION")) {
            return "Check blood pressure at next visit. Review medication compliance and consider dose adjustment.";
        }
        if (measureId.contains("COL") || measureId.contains("COLORECTAL")) {
            return "Refer for colonoscopy or order FIT test. Discuss screening options with patient.";
        }
        if (measureId.contains("BCS") || measureId.contains("BREAST")) {
            return "Order mammogram or refer to imaging center. Ensure patient scheduling assistance.";
        }
        if (measureId.contains("FLU") || measureId.contains("INFLUENZA")) {
            return "Administer influenza vaccine during visit. Document contraindications if applicable.";
        }
        if (measureId.contains("STATIN") || measureId.contains("SPC")) {
            return "Review cardiovascular risk. Consider statin initiation per guidelines if not contraindicated.";
        }
        if (measureId.contains("AWV") || measureId.contains("WELLNESS")) {
            return "Schedule Annual Wellness Visit. Review preventive care checklist with patient.";
        }
        if (measureId.contains("RETINAL") || measureId.contains("EYE")) {
            return "Refer to ophthalmology for dilated eye exam. Confirm appointment scheduled.";
        }

        // Default recommendation
        return String.format("Address %s gap. Review clinical guidelines and discuss options with patient.", measureName);
    }

    /**
     * Generate suggested actions for the care gap.
     */
    private List<String> generateSuggestedActions(CareGapEntity gap) {
        List<String> actions = new ArrayList<>();
        String measureId = gap.getMeasureId() != null ? gap.getMeasureId().toUpperCase() : "";
        String recommendationType = gap.getRecommendationType() != null ? gap.getRecommendationType().toLowerCase() : "";

        // Generic first actions
        actions.add("Review patient chart for contraindications");

        // Measure-specific actions
        if (measureId.contains("HBA1C") || measureId.contains("CDC")) {
            actions.add("Order HbA1c lab test");
            actions.add("Schedule follow-up to review results");
            actions.add("Assess medication adherence");
        } else if (measureId.contains("BP") || measureId.contains("HYPERTENSION")) {
            actions.add("Measure blood pressure");
            actions.add("Review home BP log if available");
            actions.add("Assess antihypertensive compliance");
        } else if (measureId.contains("COL") || measureId.contains("COLORECTAL")) {
            actions.add("Discuss colonoscopy vs FIT test options");
            actions.add("Order appropriate screening test");
            actions.add("Provide patient education materials");
        } else if (measureId.contains("BCS") || measureId.contains("BREAST")) {
            actions.add("Order mammogram");
            actions.add("Provide imaging center contact");
            actions.add("Schedule follow-up for results");
        } else if (measureId.contains("FLU") || measureId.contains("INFLUENZA")) {
            actions.add("Check for vaccine allergies");
            actions.add("Administer flu vaccine");
            actions.add("Document in immunization registry");
        } else if (measureId.contains("STATIN") || measureId.contains("SPC")) {
            actions.add("Calculate ASCVD risk score");
            actions.add("Review lipid panel");
            actions.add("Discuss statin therapy benefits/risks");
        } else {
            // Default actions based on recommendation type
            switch (recommendationType) {
                case "immunization" -> {
                    actions.add("Verify immunization history");
                    actions.add("Administer vaccine");
                }
                case "medication" -> {
                    actions.add("Review current medications");
                    actions.add("Prescribe recommended therapy");
                }
                case "screening" -> {
                    actions.add("Order screening test");
                    actions.add("Schedule results review");
                }
                case "procedure" -> {
                    actions.add("Schedule procedure");
                    actions.add("Provide pre-procedure instructions");
                }
                default -> {
                    actions.add("Document care gap intervention");
                    actions.add("Schedule follow-up as needed");
                }
            }
        }

        return actions;
    }

    /**
     * Get summary statistics for provider's prioritized gaps.
     */
    public ProviderGapSummary getProviderGapSummary(String tenantId, String providerId) {
        log.info("Getting gap summary for provider: {} in tenant: {}", providerId, tenantId);

        List<CareGapEntity> openGaps = careGapRepository.findOpenGapsByProvider(tenantId, providerId);

        long totalOpen = openGaps.size();
        long highPriority = openGaps.stream()
                .filter(g -> "high".equalsIgnoreCase(g.getPriority()))
                .count();
        long overdue = openGaps.stream()
                .filter(g -> g.getDueDate() != null && g.getDueDate().isBefore(LocalDate.now()))
                .count();
        long dueSoon = openGaps.stream()
                .filter(g -> g.getDueDate() != null &&
                        !g.getDueDate().isBefore(LocalDate.now()) &&
                        g.getDueDate().isBefore(LocalDate.now().plusDays(14)))
                .count();
        long primaryCareRelevant = openGaps.stream()
                .filter(this::isPrimaryCareRelevant)
                .count();

        return ProviderGapSummary.builder()
                .providerId(providerId)
                .totalOpenGaps(totalOpen)
                .highPriorityGaps(highPriority)
                .overdueGaps(overdue)
                .gapsDueSoon(dueSoon)
                .primaryCareRelevantGaps(primaryCareRelevant)
                .lastUpdated(java.time.Instant.now())
                .build();
    }

    // ==================== DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrioritizedCareGap {
        private String gapId;
        private String patientId;
        private String measureId;
        private String measureName;
        private String gapCategory;
        private String priority;
        private LocalDate dueDate;
        private Long daysUntilDue;

        // Scoring
        private double priorityScore;
        private double urgencyScore;
        private double dueDateScore;
        private double interventionEaseScore;

        // Primary care relevance
        private boolean isPrimaryCareRelevant;

        // Recommendations
        private String recommendation;
        private List<String> suggestedActions;
        private String recommendationType;
        private String gapDescription;

        private java.time.Instant identifiedDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderGapSummary {
        private String providerId;
        private long totalOpenGaps;
        private long highPriorityGaps;
        private long overdueGaps;
        private long gapsDueSoon;
        private long primaryCareRelevantGaps;
        private java.time.Instant lastUpdated;
    }
}
