package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * HBD - Hemoglobin A1c Control for Patients with Diabetes (HEDIS)
 *
 * Evaluates HbA1c control in patients with diabetes.
 * Multiple thresholds:
 * - HbA1c <8.0% (good control)
 * - HbA1c >9.0% (poor control)
 */
@Component
public class HBDMeasure extends AbstractHedisMeasure {

    private static final List<String> DIABETES_CODES = Arrays.asList(
        "44054006",  // Diabetes mellitus type 2 (SNOMED)
        "46635009",  // Diabetes mellitus type 1 (SNOMED)
        "73211009",  // Diabetes mellitus (SNOMED)
        "190372001", // Type 2 diabetes mellitus without complication (SNOMED)
        "313436004", // Type 1 diabetes mellitus without complication (SNOMED)
        "190331003"  // Type 1 diabetes mellitus with complication (SNOMED)
    );

    private static final List<String> HBA1C_CODES = Arrays.asList(
        "4548-4",    // Hemoglobin A1c/Hemoglobin.total in Blood (LOINC)
        "17856-6",   // Hemoglobin A1c/Hemoglobin.total in Blood by HPLC (LOINC)
        "59261-8",   // Hemoglobin A1c/Hemoglobin.total in Blood by calculation (LOINC)
        "62388-4",   // Hemoglobin A1c/Hemoglobin.total in Blood by IFCC protocol (LOINC)
        "71875-9"    // Hemoglobin A1c goal [Percent] (LOINC)
    );

    @Override
    public String getMeasureId() {
        return "HBD";
    }

    @Override
    public String getMeasureName() {
        return "Hemoglobin A1c Control for Patients with Diabetes";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'HBD-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating HBD measure for patient: {}", patientId);

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

        // Check for HbA1c test in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode hba1cObs = getObservations(tenantId, patientId,
            String.join(",", HBA1C_CODES), dateFilter);
        List<JsonNode> hba1cEntries = getEntries(hba1cObs);

        if (hba1cEntries.isEmpty()) {
            // No HbA1c test - care gap
            resultBuilder.inNumerator(false);
            resultBuilder.complianceRate(0.0);
            resultBuilder.score(0.0);

            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_HBA1C_TEST")
                    .description("No HbA1c test in the last 12 months")
                    .recommendedAction("Order HbA1c test")
                    .priority("high")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "hasHbA1cTest", false
            ));

            return resultBuilder.build();
        }

        // Get most recent HbA1c value
        JsonNode mostRecentHbA1c = hba1cEntries.get(0);
        Double hba1cValue = extractHbA1cValue(mostRecentHbA1c);
        String testDate = getEffectiveDate(mostRecentHbA1c);

        if (hba1cValue == null) {
            // Test exists but no value
            resultBuilder.inNumerator(false);
            resultBuilder.complianceRate(0.5); // Partial credit for having test

            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("INVALID_HBA1C_RESULT")
                    .description("HbA1c test result not available or invalid")
                    .recommendedAction("Review HbA1c test result and document value")
                    .priority("medium")
                    .dueDate(LocalDate.now().plusWeeks(1))
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "hasHbA1cTest", true,
                "lastTestDate", testDate,
                "hba1cValue", "Invalid"
            ));

            return resultBuilder.build();
        }

        // Evaluate HbA1c control
        boolean goodControl = hba1cValue < 8.0;  // Good control <8%
        boolean poorControl = hba1cValue > 9.0;  // Poor control >9%

        resultBuilder.inNumerator(goodControl);
        resultBuilder.complianceRate(goodControl ? 1.0 : 0.0);
        resultBuilder.score(goodControl ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (poorControl) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("POOR_HBA1C_CONTROL")
                .description(String.format("HbA1c >9.0%% (current: %.1f%%)", hba1cValue))
                .recommendedAction("Intensify diabetes management - consider medication adjustment, diabetes education, or endocrinology referral")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        } else if (!goodControl) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("SUBOPTIMAL_HBA1C_CONTROL")
                .description(String.format("HbA1c 8.0-9.0%% (current: %.1f%%)", hba1cValue))
                .recommendedAction("Review diabetes management plan - consider medication adjustment or diabetes self-management education")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "hasHbA1cTest", true,
            "lastTestDate", testDate,
            "hba1cValue", hba1cValue,
            "goodControl", goodControl,
            "poorControl", poorControl,
            "controlLevel", goodControl ? "Good (<8%)" : (poorControl ? "Poor (>9%)" : "Fair (8-9%)")
        ));

        resultBuilder.evidence(java.util.Map.of(
            "latestHbA1c", hba1cValue,
            "testDate", testDate,
            "totalTests", hba1cEntries.size()
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

    /**
     * Extract HbA1c value from observation
     */
    private Double extractHbA1cValue(JsonNode observation) {
        try {
            if (observation.has("valueQuantity")) {
                JsonNode valueQuantity = observation.get("valueQuantity");
                if (valueQuantity.has("value")) {
                    return valueQuantity.get("value").asDouble();
                }
            }
        } catch (Exception e) {
            logger.warn("Could not extract HbA1c value: {}", e.getMessage());
        }
        return null;
    }
}
