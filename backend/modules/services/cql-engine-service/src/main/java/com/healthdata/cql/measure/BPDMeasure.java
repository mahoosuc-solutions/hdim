package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * BPD - Blood Pressure Control for Patients with Diabetes (HEDIS)
 *
 * Evaluates whether diabetic patients have adequate blood pressure control.
 * Target: BP <140/90 mmHg (ADA guidelines for most diabetic patients)
 *
 * More stringent than general population due to increased cardiovascular risk.
 */
@Component
public class BPDMeasure extends AbstractHedisMeasure {

    private static final List<String> DIABETES_CODES = Arrays.asList(
        "44054006",  // Type 2 diabetes mellitus (SNOMED)
        "46635009",  // Type 1 diabetes mellitus (SNOMED)
        "73211009",  // Diabetes mellitus (SNOMED)
        "199223000", // Pre-existing diabetes mellitus (SNOMED)
        "237599002", // Insulin-dependent diabetes mellitus (SNOMED)
        "237618001"  // Non-insulin-dependent diabetes mellitus (SNOMED)
    );

    private static final List<String> SYSTOLIC_BP_CODES = Arrays.asList(
        "8480-6",    // LOINC - Systolic blood pressure
        "271649006", // SNOMED - Systolic blood pressure
        "8459-0"     // LOINC - Systolic blood pressure - sitting
    );

    private static final List<String> DIASTOLIC_BP_CODES = Arrays.asList(
        "8462-4",    // LOINC - Diastolic blood pressure
        "271650006", // SNOMED - Diastolic blood pressure
        "8453-3"     // LOINC - Diastolic blood pressure - sitting
    );

    private static final List<String> HYPERTENSION_CODES = Arrays.asList(
        "38341003",  // Hypertension (SNOMED)
        "59621000",  // Essential hypertension (SNOMED)
        "48194001",  // Pregnancy-induced hypertension (SNOMED)
        "70272006"   // Malignant hypertension (SNOMED)
    );

    private static final int SYSTOLIC_TARGET = 140;  // mmHg
    private static final int DIASTOLIC_TARGET = 90;  // mmHg

    @Override
    public String getMeasureId() {
        return "BPD";
    }

    @Override
    public String getMeasureName() {
        return "Blood Pressure Control for Patients with Diabetes";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'BPD-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating BPD measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18-75 with diabetes)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Get most recent BP reading in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        JsonNode systolicObs = getObservations(tenantId, patientId,
            String.join(",", SYSTOLIC_BP_CODES), dateFilter);
        JsonNode diastolicObs = getObservations(tenantId, patientId,
            String.join(",", DIASTOLIC_BP_CODES), dateFilter);

        List<JsonNode> systolicEntries = getEntries(systolicObs);
        List<JsonNode> diastolicEntries = getEntries(diastolicObs);

        if (systolicEntries.isEmpty() || diastolicEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(true)
                .inNumerator(false)
                .exclusionReason("No blood pressure readings in last 12 months")
                .careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("MISSING_BP_MEASUREMENT")
                        .description("No blood pressure measurement documented in last 12 months for diabetic patient")
                        .recommendedAction("Measure blood pressure at next visit - critical for diabetes management")
                        .priority("high")
                        .dueDate(LocalDate.now().plusWeeks(2))
                        .build()
                ))
                .build();
        }

        // Get most recent readings
        JsonNode mostRecentSystolic = systolicEntries.get(0);
        JsonNode mostRecentDiastolic = diastolicEntries.get(0);

        String systolicDate = getEffectiveDate(mostRecentSystolic);
        String diastolicDate = getEffectiveDate(mostRecentDiastolic);

        // Extract BP values
        Integer systolicValue = null;
        Integer diastolicValue = null;

        try {
            if (mostRecentSystolic.has("valueQuantity")) {
                JsonNode valueQuantity = mostRecentSystolic.get("valueQuantity");
                if (valueQuantity.has("value")) {
                    systolicValue = valueQuantity.get("value").asInt();
                }
            }
        } catch (Exception e) {
            logger.warn("Could not extract systolic BP value: {}", e.getMessage());
        }

        try {
            if (mostRecentDiastolic.has("valueQuantity")) {
                JsonNode valueQuantity = mostRecentDiastolic.get("valueQuantity");
                if (valueQuantity.has("value")) {
                    diastolicValue = valueQuantity.get("value").asInt();
                }
            }
        } catch (Exception e) {
            logger.warn("Could not extract diastolic BP value: {}", e.getMessage());
        }

        // Check BP control
        boolean bpControlled = false;
        String bpStatus = "Unknown";

        if (systolicValue != null && diastolicValue != null) {
            bpControlled = systolicValue < SYSTOLIC_TARGET && diastolicValue < DIASTOLIC_TARGET;

            // Categorize BP level (for diabetics)
            if (systolicValue < 120 && diastolicValue < 80) {
                bpStatus = "Normal (<120/80) - Optimal";
            } else if (systolicValue < 130 && diastolicValue < 80) {
                bpStatus = "Elevated (120-129/<80) - Good control";
            } else if (systolicValue < 140 && diastolicValue < 90) {
                bpStatus = "Stage 1 Hypertension (130-139/80-89) - Acceptable for diabetes";
            } else if (systolicValue < 180 && diastolicValue < 120) {
                bpStatus = "Stage 2 Hypertension (≥140/90) - Uncontrolled";
            } else {
                bpStatus = "Hypertensive Crisis (≥180/120) - Emergency";
            }
        }

        resultBuilder.inNumerator(bpControlled);
        resultBuilder.complianceRate(bpControlled ? 1.0 : 0.0);
        resultBuilder.score(bpControlled ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!bpControlled && systolicValue != null && diastolicValue != null) {
            String priority = "high";
            String recommendedAction = "Intensify antihypertensive therapy per diabetes/hypertension guidelines";

            // Hypertensive crisis
            if (systolicValue >= 180 || diastolicValue >= 120) {
                priority = "high";
                recommendedAction = "URGENT: Hypertensive crisis - immediate evaluation and treatment required";
            }
            // Stage 2 hypertension
            else if (systolicValue >= 140 || diastolicValue >= 90) {
                priority = "high";
                recommendedAction = "Uncontrolled BP in diabetic - consider adding/adjusting antihypertensive medications (ACE-I/ARB preferred)";
            }

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("UNCONTROLLED_BP_DIABETES")
                .description(String.format("Blood pressure not at goal for diabetes (%d/%d mmHg, target <140/90)",
                    systolicValue, diastolicValue))
                .recommendedAction(recommendedAction)
                .priority(priority)
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());

            // Check if on ACE-I/ARB (preferred for diabetics)
            // This would require medication data - add as additional recommendation
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("BP_MEDICATION_OPTIMIZATION")
                .description("Diabetic patients with hypertension benefit from ACE inhibitor or ARB therapy")
                .recommendedAction("Ensure patient on ACE inhibitor or ARB unless contraindicated (renal protection + BP control)")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "systolicBP", systolicValue != null ? systolicValue : "Not available",
            "diastolicBP", diastolicValue != null ? diastolicValue : "Not available",
            "bpMeasurementDate", systolicDate,
            "bpStatus", bpStatus,
            "bpControlled", bpControlled,
            "target", SYSTOLIC_TARGET + "/" + DIASTOLIC_TARGET + " mmHg"
        ));

        resultBuilder.evidence(java.util.Map.of(
            "systolicBP", systolicValue != null ? systolicValue : "Not available",
            "diastolicBP", diastolicValue != null ? diastolicValue : "Not available",
            "bpAtGoal", bpControlled
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 18 || age > 75) {
            return false;
        }

        // Must have diabetes diagnosis
        JsonNode diabetesConditions = getConditions(tenantId, patientId,
            String.join(",", DIABETES_CODES));

        return !getEntries(diabetesConditions).isEmpty();
    }
}
