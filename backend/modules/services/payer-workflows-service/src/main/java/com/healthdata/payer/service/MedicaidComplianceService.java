package com.healthdata.payer.service;

import com.healthdata.payer.audit.PayerWorkflowsAuditIntegration;
import com.healthdata.payer.domain.MedicaidComplianceReport;
import com.healthdata.payer.domain.MedicaidStateConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for calculating Medicaid state-specific compliance reports.
 *
 * Handles:
 * - State-specific quality thresholds and goals
 * - Compliance status determination
 * - Penalty and bonus calculations
 * - Corrective action identification
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MedicaidComplianceService {

    private final PayerWorkflowsAuditIntegration auditIntegration;

    /**
     * Calculate comprehensive compliance report for a Medicaid MCO.
     */
    public MedicaidComplianceReport calculateComplianceReport(
        String mcoId,
        String mcoName,
        MedicaidStateConfig stateConfig,
        String reportingPeriod,
        int measurementYear,
        Map<String, MeasurePerformance> measurePerformance
    ) {
        return calculateComplianceReport(
            mcoId,
            mcoName,
            stateConfig,
            reportingPeriod,
            measurementYear,
            measurePerformance,
            null
        );
    }

    /**
     * Calculate comprehensive compliance report with prior year data for improvement tracking.
     */
    public MedicaidComplianceReport calculateComplianceReport(
        String mcoId,
        String mcoName,
        MedicaidStateConfig stateConfig,
        String reportingPeriod,
        int measurementYear,
        Map<String, MeasurePerformance> measurePerformance,
        Map<String, Double> priorYearPerformance
    ) {
        // Calculate individual measure results
        List<MedicaidComplianceReport.MedicaidMeasureResult> measureResults = new ArrayList<>();

        for (String measureCode : stateConfig.getRequiredMeasures()) {
            MeasurePerformance perf = measurePerformance.get(measureCode);
            if (perf == null) {
                log.warn("Missing performance data for required measure: {}", measureCode);
                continue;
            }

            double performanceRate = perf.denominator > 0 ?
                (double) perf.numerator / perf.denominator : 0.0;

            double threshold = stateConfig.getQualityThresholds().getOrDefault(measureCode, 0.0);
            double goal = stateConfig.getPerformanceGoals().getOrDefault(measureCode, threshold + 0.05);

            boolean meetsThreshold = performanceRate >= threshold;
            boolean meetsGoal = performanceRate >= goal;

            Double priorYear = priorYearPerformance != null ? priorYearPerformance.get(measureCode) : null;
            Double improvement = priorYear != null ? performanceRate - priorYear : null;

            MedicaidComplianceReport.MedicaidMeasureResult.ComplianceLevel level;
            if (performanceRate >= goal) {
                level = MedicaidComplianceReport.MedicaidMeasureResult.ComplianceLevel.EXCEEDS_GOAL;
            } else if (performanceRate >= threshold) {
                level = MedicaidComplianceReport.MedicaidMeasureResult.ComplianceLevel.MEETS_THRESHOLD;
            } else {
                level = MedicaidComplianceReport.MedicaidMeasureResult.ComplianceLevel.BELOW_THRESHOLD;
            }

            MedicaidComplianceReport.MedicaidMeasureResult result =
                MedicaidComplianceReport.MedicaidMeasureResult.builder()
                    .measureCode(measureCode)
                    .measureName(getMeasureName(measureCode))
                    .performanceRate(performanceRate)
                    .stateThreshold(threshold)
                    .stateGoal(goal)
                    .meetsThreshold(meetsThreshold)
                    .meetsGoal(meetsGoal)
                    .numerator(perf.numerator)
                    .denominator(perf.denominator)
                    .priorYearRate(priorYear)
                    .improvement(improvement)
                    .complianceLevel(level)
                    .build();

            measureResults.add(result);
        }

        // Calculate overall compliance metrics
        int measuresMetThreshold = (int) measureResults.stream()
            .filter(MedicaidComplianceReport.MedicaidMeasureResult::isMeetsThreshold)
            .count();

        int measuresBelowThreshold = measureResults.size() - measuresMetThreshold;

        double overallComplianceRate = measureResults.size() > 0 ?
            (double) measuresMetThreshold / measureResults.size() : 0.0;

        // Determine overall compliance status
        MedicaidComplianceReport.ComplianceStatus overallStatus;
        if (overallComplianceRate >= 1.0) {
            overallStatus = MedicaidComplianceReport.ComplianceStatus.COMPLIANT;
        } else if (overallComplianceRate > 0.80) {
            overallStatus = MedicaidComplianceReport.ComplianceStatus.SUBSTANTIALLY_COMPLIANT;
        } else if (overallComplianceRate >= 0.50) {
            overallStatus = MedicaidComplianceReport.ComplianceStatus.PARTIALLY_COMPLIANT;
        } else {
            overallStatus = MedicaidComplianceReport.ComplianceStatus.NON_COMPLIANT;
        }

        // Identify measures requiring corrective action
        List<String> correctiveActionMeasures = measureResults.stream()
            .filter(r -> !r.isMeetsThreshold())
            .map(MedicaidComplianceReport.MedicaidMeasureResult::getMeasureCode)
            .collect(Collectors.toList());

        // Calculate penalty assessment
        MedicaidComplianceReport.PenaltyAssessment penaltyAssessment =
            calculatePenaltyAssessment(overallStatus, measuresBelowThreshold, correctiveActionMeasures);

        // Determine quality bonus eligibility
        boolean qualityBonusEligible = determineQualityBonusEligibility(
            overallStatus,
            measureResults,
            stateConfig
        );

        Double estimatedBonus = qualityBonusEligible ? calculateEstimatedBonus(measureResults) : null;

        // Build report
        MedicaidComplianceReport report = MedicaidComplianceReport.builder()
            .mcoId(mcoId)
            .mcoName(mcoName)
            .stateConfig(stateConfig)
            .reportingPeriod(reportingPeriod)
            .measurementYear(measurementYear)
            .measureResults(measureResults)
            .overallStatus(overallStatus)
            .overallComplianceRate(overallComplianceRate)
            .measuresMetThreshold(measuresMetThreshold)
            .measuresBelowThreshold(measuresBelowThreshold)
            .correctiveActionMeasures(correctiveActionMeasures)
            .penaltyAssessment(penaltyAssessment)
            .qualityBonusEligible(qualityBonusEligible)
            .estimatedBonus(estimatedBonus)
            .ncqaAccreditation(stateConfig.isNcqaAccreditationRequired() ? "Required" : "Not Required")
            .generatedAt(LocalDateTime.now())
            .build();

        // Publish audit event
        Map<String, Object> metrics = new java.util.HashMap<>();
        metrics.put("measuresMetThreshold", measuresMetThreshold);
        metrics.put("measuresBelowThreshold", measuresBelowThreshold);
        metrics.put("complianceRate", overallComplianceRate);
        
        auditIntegration.publishMedicaidComplianceEvent(
            mcoId, // Using mcoId as tenantId
            stateConfig.getStateCode(),
            reportingPeriod,
            overallStatus == MedicaidComplianceReport.ComplianceStatus.COMPLIANT ||
                overallStatus == MedicaidComplianceReport.ComplianceStatus.SUBSTANTIALLY_COMPLIANT,
            metrics,
            0, // Processing time not tracked
            "system"
        );

        return report;
    }

    /**
     * Calculate penalty assessment based on compliance status.
     */
    private MedicaidComplianceReport.PenaltyAssessment calculatePenaltyAssessment(
        MedicaidComplianceReport.ComplianceStatus status,
        int measuresBelowThreshold,
        List<String> correctiveActionMeasures
    ) {
        boolean penaltyApplied = false;
        double penaltyPercentage = 0.0;
        List<String> penaltyReasons = new ArrayList<>();

        // Apply penalties for non-compliant or partially compliant MCOs
        if (status == MedicaidComplianceReport.ComplianceStatus.NON_COMPLIANT) {
            penaltyApplied = true;
            penaltyPercentage = 5.0;  // 5% penalty
            penaltyReasons.add("Overall compliance rate below 50%");
            penaltyReasons.add("Multiple measures below state thresholds");
        } else if (status == MedicaidComplianceReport.ComplianceStatus.PARTIALLY_COMPLIANT) {
            if (measuresBelowThreshold >= 3) {
                penaltyApplied = true;
                penaltyPercentage = 2.0;  // 2% penalty
                penaltyReasons.add("Three or more measures below state thresholds");
            }
        }

        // Estimate penalty amount (would be based on actual contract amounts in production)
        double estimatedPenaltyAmount = penaltyApplied ? penaltyPercentage * 1000000 : 0.0;

        return MedicaidComplianceReport.PenaltyAssessment.builder()
            .penaltyApplied(penaltyApplied)
            .penaltyPercentage(penaltyPercentage)
            .estimatedPenaltyAmount(estimatedPenaltyAmount)
            .penaltyReasons(penaltyReasons)
            .correctiveActionPlan(penaltyApplied ?
                "MCO must submit corrective action plan within 30 days" : null)
            .build();
    }

    /**
     * Determine if MCO qualifies for quality bonus payment.
     */
    private boolean determineQualityBonusEligibility(
        MedicaidComplianceReport.ComplianceStatus status,
        List<MedicaidComplianceReport.MedicaidMeasureResult> measureResults,
        MedicaidStateConfig stateConfig
    ) {
        // Eligible if compliant or substantially compliant
        if (status != MedicaidComplianceReport.ComplianceStatus.COMPLIANT &&
            status != MedicaidComplianceReport.ComplianceStatus.SUBSTANTIALLY_COMPLIANT) {
            return false;
        }

        // Check if majority of measures exceed goals
        long measuresExceedingGoals = measureResults.stream()
            .filter(MedicaidComplianceReport.MedicaidMeasureResult::isMeetsGoal)
            .count();

        return measuresExceedingGoals > measureResults.size() / 2;
    }

    /**
     * Calculate estimated bonus payment amount.
     */
    private Double calculateEstimatedBonus(List<MedicaidComplianceReport.MedicaidMeasureResult> measureResults) {
        // Simple calculation based on number of measures exceeding goals
        long exceedingGoals = measureResults.stream()
            .filter(MedicaidComplianceReport.MedicaidMeasureResult::isMeetsGoal)
            .count();

        // $10,000 per measure exceeding goal (simplified)
        return exceedingGoals * 10000.0;
    }

    /**
     * Get human-readable measure name from code.
     */
    private String getMeasureName(String measureCode) {
        Map<String, String> measureNames = new java.util.HashMap<>();
        measureNames.put("CBP", "Controlling High Blood Pressure");
        measureNames.put("CDC-H9", "Comprehensive Diabetes Care: HbA1c Poor Control");
        measureNames.put("CDC-E", "Comprehensive Diabetes Care: Eye Exam");
        measureNames.put("BCS", "Breast Cancer Screening");
        measureNames.put("COL", "Colorectal Cancer Screening");
        measureNames.put("CCS", "Cervical Cancer Screening");
        measureNames.put("W30", "Well-Child Visits in the First 30 Months of Life");
        measureNames.put("AAP", "Adults' Access to Preventive/Ambulatory Health Services");
        measureNames.put("CIS", "Childhood Immunization Status");
        measureNames.put("IMA", "Immunizations for Adolescents");
        measureNames.put("WCV", "Child and Adolescent Well-Care Visits");
        measureNames.put("AMR", "Asthma Medication Ratio");
        measureNames.put("FUH-7", "Follow-Up After Hospitalization for Mental Illness (7 days)");
        measureNames.put("FUH-30", "Follow-Up After Hospitalization for Mental Illness (30 days)");
        measureNames.put("ADD", "Follow-Up Care for Children Prescribed ADHD Medication");
        measureNames.put("AMB", "Ambulatory Care: Emergency Department Visits");
        measureNames.put("PCR", "Plan All-Cause Readmissions");

        return measureNames.getOrDefault(measureCode, measureCode);
    }

    /**
     * Helper class for measure performance data
     */
    public static class MeasurePerformance {
        public final int numerator;
        public final int denominator;

        public MeasurePerformance(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }
}
