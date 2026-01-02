package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * CBP - Controlling High Blood Pressure (HEDIS)
 *
 * Evaluates blood pressure control for patients with hypertension.
 * Target: <140/90 mmHg
 */
@Component
public class CBPMeasure extends AbstractHedisMeasure {

    private static final List<String> HYPERTENSION_CODES = Arrays.asList(
        "38341003",  // Hypertensive disorder
        "59621000"   // Essential hypertension
    );

    private static final List<String> BP_CODES = Arrays.asList(
        "85354-9", "8480-6", "8462-4"
    );

    @Override
    public String getMeasureId() {
        return "CBP";
    }

    @Override
    public String getMeasureName() {
        return "Controlling High Blood Pressure";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'CBP-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating CBP measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient does not have hypertension diagnosis")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Get most recent BP reading
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode observations = getObservations(tenantId, patientId, String.join(",", BP_CODES), dateFilter);
        List<JsonNode> bpObs = getEntries(observations);

        if (bpObs.isEmpty()) {
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_BP")
                    .description("No BP reading in last 12 months")
                    .recommendedAction("Measure blood pressure")
                    .priority("high")
                    .dueDate(LocalDate.now().plusDays(14))
                    .build()
            ));
            return resultBuilder.inNumerator(false).score(0.0).build();
        }

        // Parse BP components
        JsonNode mostRecent = bpObs.get(0);
        Double systolic = null, diastolic = null;

        if (mostRecent.has("component")) {
            for (JsonNode component : mostRecent.get("component")) {
                if (component.has("code") && component.has("valueQuantity")) {
                    String code = component.get("code").get("coding").get(0).get("code").asText();
                    double value = component.get("valueQuantity").get("value").asDouble();
                    if (code.equals("8480-6")) systolic = value;
                    else if (code.equals("8462-4")) diastolic = value;
                }
            }
        }

        boolean controlled = systolic != null && diastolic != null && systolic < 140 && diastolic < 90;
        resultBuilder.inNumerator(controlled);
        resultBuilder.complianceRate(controlled ? 1.0 : 0.0);
        resultBuilder.score(controlled ? 100.0 : 0.0);

        if (systolic != null && diastolic != null) {
            resultBuilder.evidence(java.util.Map.of("latestBP", String.format("%.0f/%.0f", systolic, diastolic)));
            resultBuilder.details(java.util.Map.of("systolic", systolic, "diastolic", diastolic, "controlled", controlled));

            if (!controlled) {
                resultBuilder.careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("UNCONTROLLED_BP")
                        .description(String.format("BP %.0f/%.0f exceeds target <140/90", systolic, diastolic))
                        .recommendedAction("Optimize BP management")
                        .priority("high")
                        .dueDate(LocalDate.now().plusMonths(1))
                        .build()
                ));
            }
        }

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode conditions = getConditions(tenantId, patientId, null);
        if (conditions == null) return false;

        List<JsonNode> conditionEntries = getEntries(conditions);
        for (JsonNode condition : conditionEntries) {
            if (hasCode(condition, HYPERTENSION_CODES)) {
                JsonNode patient = getPatientData(tenantId, patientId);
                Integer age = getPatientAge(patient);
                return age != null && age >= 18 && age <= 85;
            }
        }
        return false;
    }
}
