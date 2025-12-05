package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * HDO - Use of Opioids at High Dosage (HEDIS)
 *
 * Identifies patients receiving high-dose opioid therapy (≥90 morphine milligram equivalents/day).
 * High-dose opioids significantly increase overdose risk.
 *
 * This is an INVERSE measure - LOWER rates are better.
 * Being in the numerator indicates HIGH RISK (high-dose opioid use).
 */
@Component
public class HDOMeasure extends AbstractHedisMeasure {

    private static final List<String> OPIOID_MEDICATION_CODES = Arrays.asList(
        // Short-acting opioids
        "7052",      // RxNorm - Hydrocodone
        "5489",      // RxNorm - Oxycodone
        "3423",      // RxNorm - Codeine
        "7804",      // RxNorm - Methadone
        "6813",      // RxNorm - Morphine
        "5640",      // RxNorm - Hydromorphone (Dilaudid)
        "4337",      // RxNorm - Fentanyl
        "8001",      // RxNorm - Oxymorphone
        "237005",    // RxNorm - Tapentadol
        "787390",    // RxNorm - Tramadol
        // Long-acting/extended-release
        "1049502",   // RxNorm - Oxycodone extended-release
        "1049221",   // RxNorm - Morphine extended-release
        "1666777"    // RxNorm - Buprenorphine (for pain, not OUD)
    );

    private static final List<String> CHRONIC_PAIN_CODES = Arrays.asList(
        "82423001",  // SNOMED - Chronic pain
        "55145008",  // SNOMED - Chronic pain syndrome
        "373503007", // SNOMED - Chronic idiopathic pain disorder
        "161465002", // SNOMED - History of chronic pain
        "161891005"  // SNOMED - Backache (chronic)
    );

    private static final double HIGH_DOSE_MME_THRESHOLD = 90.0;  // CDC guideline threshold

    @Override
    public String getMeasureId() {
        return "HDO";
    }

    @Override
    public String getMeasureName() {
        return "Use of Opioids at High Dosage";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'HDO-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating HDO measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18+ with opioid prescription)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Get all opioid prescriptions in last 60 days
        String dateFilter = "ge" + LocalDate.now().minusDays(60).toString();
        JsonNode opioidMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", OPIOID_MEDICATION_CODES), dateFilter);
        List<JsonNode> opioidEntries = getEntries(opioidMeds);

        if (opioidEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No opioid prescriptions in last 60 days")
                .build();
        }

        // Calculate total daily MME (Morphine Milligram Equivalent)
        // This is a simplified calculation - real implementation would need detailed dosing data
        double estimatedDailyMME = estimateMME(opioidEntries);

        boolean isHighDose = estimatedDailyMME >= HIGH_DOSE_MME_THRESHOLD;

        // For INVERSE measure: being in numerator = high risk (bad outcome)
        resultBuilder.inNumerator(isHighDose);
        // Compliance = NOT being on high dose
        resultBuilder.complianceRate(isHighDose ? 0.0 : 1.0);
        resultBuilder.score(isHighDose ? 0.0 : 100.0);

        // Check for chronic pain diagnosis (context for opioid use)
        JsonNode chronicPain = getConditions(tenantId, patientId,
            String.join(",", CHRONIC_PAIN_CODES));
        boolean hasChronicPain = !getEntries(chronicPain).isEmpty();

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (isHighDose) {
            String priority = estimatedDailyMME >= 120 ? "high" : "medium";
            String doseCategory = estimatedDailyMME >= 120 ? "Very high dose (≥120 MME/day)" :
                                                            "High dose (90-119 MME/day)";

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("HIGH_DOSE_OPIOID_THERAPY")
                .description(String.format("%s opioid therapy - estimated %.0f MME/day (CDC recommends <90 MME/day)",
                    doseCategory, estimatedDailyMME))
                .recommendedAction("Review opioid dosing; consider dose reduction, non-opioid alternatives, or opioid rotation")
                .priority(priority)
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("OPIOID_RISK_MITIGATION")
                .description("High-dose opioids increase overdose risk exponentially")
                .recommendedAction("Prescribe naloxone; discuss risks; consider urine drug screening; check PDMP (prescription drug monitoring)")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());

            if (!hasChronicPain) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("HIGH_DOSE_WITHOUT_CHRONIC_PAIN_DIAGNOSIS")
                    .description("High-dose opioid without documented chronic pain diagnosis")
                    .recommendedAction("Review indication for long-term opioid therapy; document chronic pain diagnosis or taper opioids")
                    .priority("medium")
                    .dueDate(LocalDate.now().plusMonths(1))
                    .build());
            }
        }

        resultBuilder.careGaps(careGaps);

        // Dose risk category
        String riskCategory;
        if (estimatedDailyMME < 50) {
            riskCategory = "Low dose (<50 MME/day) - Lower risk";
        } else if (estimatedDailyMME < 90) {
            riskCategory = "Moderate dose (50-89 MME/day) - Moderate risk";
        } else if (estimatedDailyMME < 120) {
            riskCategory = "High dose (90-119 MME/day) - High risk";
        } else {
            riskCategory = "Very high dose (≥120 MME/day) - Very high risk";
        }

        resultBuilder.details(java.util.Map.of(
            "estimatedDailyMME", String.format("%.0f MME/day", estimatedDailyMME),
            "isHighDose", isHighDose,
            "riskCategory", riskCategory,
            "hasChronicPain", hasChronicPain,
            "totalOpioidPrescriptions", opioidEntries.size()
        ));

        resultBuilder.evidence(java.util.Map.of(
            "dailyMME", estimatedDailyMME,
            "highDoseOpioid", isHighDose,
            "chronicPainPresent", hasChronicPain
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 18) {
            return false;
        }

        // Must have opioid prescription in last 60 days
        String dateFilter = "ge" + LocalDate.now().minusDays(60).toString();
        JsonNode opioidMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", OPIOID_MEDICATION_CODES), dateFilter);

        return !getEntries(opioidMeds).isEmpty();
    }

    /**
     * Estimate daily MME (Morphine Milligram Equivalent) from prescriptions
     * This is a simplified calculation - real implementation would need:
     * - Specific drug formulation
     * - Dosage strength
     * - Frequency of administration
     * - Conversion factors for each opioid
     */
    private double estimateMME(List<JsonNode> opioidEntries) {
        // Simplified estimate: assume average of 60 MME/day for typical opioid prescription
        // In real implementation, would calculate based on:
        // MME = (Opioid dose × MME conversion factor) × doses per day

        // For this implementation, we'll return a reasonable estimate
        // based on the number and recency of prescriptions
        if (opioidEntries.isEmpty()) {
            return 0.0;
        }

        // If multiple concurrent opioid prescriptions, assume cumulative MME
        int concurrentCount = opioidEntries.size();

        if (concurrentCount == 1) {
            return 60.0;  // Typical single opioid
        } else if (concurrentCount == 2) {
            return 95.0;  // Multiple opioids - likely high dose
        } else {
            return 125.0;  // 3+ opioids - very high risk
        }

        // Note: Real implementation would extract:
        // - dosageInstruction.doseAndRate.doseQuantity.value
        // - dosageInstruction.timing.repeat.frequency
        // - medicationCodeableConcept (to determine conversion factor)
    }
}
