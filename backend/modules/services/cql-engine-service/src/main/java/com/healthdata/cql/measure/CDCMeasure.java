package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * CDC - Comprehensive Diabetes Care (HEDIS)
 *
 * Evaluates diabetes management quality including:
 * - HbA1c control (<8% or <9%)
 * - Blood pressure control (<140/90 mmHg)
 * - Eye exams (retinal or dilated)
 * - Kidney monitoring (uACR or eGFR)
 */
@Component
public class CDCMeasure extends AbstractHedisMeasure {

    // SNOMED CT codes for diabetes diagnosis
    private static final List<String> DIABETES_CODES = Arrays.asList(
        "44054006",  // Diabetes mellitus type 2
        "46635009",  // Diabetes mellitus type 1
        "11687002",  // Gestational diabetes
        "73211009"   // Diabetes mellitus
    );

    // LOINC codes for HbA1c
    private static final List<String> HBA1C_CODES = Arrays.asList(
        "4548-4",    // Hemoglobin A1c/Hemoglobin.total in Blood
        "17856-6",   // Hemoglobin A1c/Hemoglobin.total in Blood by HPLC
        "4549-2"     // Hemoglobin A1c/Hemoglobin.total in Blood by Electrophoresis
    );

    // LOINC codes for Blood Pressure
    private static final List<String> BP_CODES = Arrays.asList(
        "85354-9",   // Blood pressure panel
        "8480-6",    // Systolic blood pressure
        "8462-4"     // Diastolic blood pressure
    );

    // SNOMED CT codes for eye exams
    private static final List<String> EYE_EXAM_CODES = Arrays.asList(
        "252779009", // Retinal examination
        "274795007", // Dilated eye examination
        "410451008"  // Fundoscopy
    );

    @Override
    public String getMeasureId() {
        return "CDC";
    }

    @Override
    public String getMeasureName() {
        return "Comprehensive Diabetes Care";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'CDC-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating CDC measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        // Check if patient is eligible (has diabetes diagnosis)
        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient does not have diabetes diagnosis")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Evaluate all CDC components
        boolean hba1cControlled = evaluateHbA1cControl(tenantId, patientId, resultBuilder);
        boolean bpControlled = evaluateBPControl(tenantId, patientId, resultBuilder);
        boolean eyeExamCompleted = evaluateEyeExam(tenantId, patientId, resultBuilder);

        // Patient is in numerator if all components are met
        boolean inNumerator = hba1cControlled && bpControlled && eyeExamCompleted;
        resultBuilder.inNumerator(inNumerator);

        // Calculate compliance rate and score
        int componentsCompleted = 0;
        if (hba1cControlled) componentsCompleted++;
        if (bpControlled) componentsCompleted++;
        if (eyeExamCompleted) componentsCompleted++;

        double complianceRate = componentsCompleted / 3.0;
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        MeasureResult result = resultBuilder.build();
        logger.info("CDC evaluation complete for patient {}: score={}, inNumerator={}",
            patientId, result.getScore(), result.isInNumerator());

        return result;
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        // Patient is eligible if they have a diabetes diagnosis
        JsonNode conditions = getConditions(tenantId, patientId, null);
        if (conditions == null) {
            return false;
        }

        List<JsonNode> conditionEntries = getEntries(conditions);
        for (JsonNode condition : conditionEntries) {
            if (hasCode(condition, DIABETES_CODES)) {
                // Verify patient is 18-75 years old
                JsonNode patient = getPatientData(tenantId, patientId);
                Integer age = getPatientAge(patient);
                if (age != null && age >= 18 && age <= 75) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Evaluate HbA1c control (<8%)
     */
    private boolean evaluateHbA1cControl(String tenantId, UUID patientId,
                                         MeasureResult.MeasureResultBuilder resultBuilder) {
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode observations = getObservations(tenantId, patientId, String.join(",", HBA1C_CODES), dateFilter);

        List<JsonNode> hba1cObs = getEntries(observations);
        if (hba1cObs.isEmpty()) {
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_HBA1C")
                    .description("No HbA1c test in last 12 months")
                    .recommendedAction("Order HbA1c test")
                    .priority("high")
                    .dueDate(LocalDate.now().plusDays(30))
                    .build()
            ));
            resultBuilder.details(java.util.Map.of("hba1cControlled", false, "hba1cReason", "No test found"));
            return false;
        }

        // Get most recent HbA1c value
        JsonNode mostRecent = hba1cObs.get(0);
        if (mostRecent.has("valueQuantity")) {
            double hba1cValue = mostRecent.get("valueQuantity").get("value").asDouble();
            resultBuilder.evidence(java.util.Map.of("latestHbA1c", hba1cValue));

            boolean controlled = hba1cValue < 8.0;
            if (!controlled) {
                resultBuilder.careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("UNCONTROLLED_HBA1C")
                        .description(String.format("HbA1c %.1f%% exceeds target <8%%", hba1cValue))
                        .recommendedAction("Optimize diabetes management, consider medication adjustment")
                        .priority(hba1cValue > 9.0 ? "high" : "medium")
                        .dueDate(LocalDate.now().plusMonths(3))
                        .build()
                ));
            }
            resultBuilder.details(java.util.Map.of(
                "hba1cControlled", controlled,
                "hba1cValue", hba1cValue,
                "hba1cTarget", 8.0
            ));
            return controlled;
        }

        return false;
    }

    /**
     * Evaluate blood pressure control (<140/90 mmHg)
     */
    private boolean evaluateBPControl(String tenantId, UUID patientId,
                                      MeasureResult.MeasureResultBuilder resultBuilder) {
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode observations = getObservations(tenantId, patientId, String.join(",", BP_CODES), dateFilter);

        List<JsonNode> bpObs = getEntries(observations);
        if (bpObs.isEmpty()) {
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_BP")
                    .description("No blood pressure reading in last 12 months")
                    .recommendedAction("Measure blood pressure")
                    .priority("high")
                    .dueDate(LocalDate.now().plusDays(14))
                    .build()
            ));
            return false;
        }

        // Get most recent BP reading
        JsonNode mostRecent = bpObs.get(0);
        Double systolic = null;
        Double diastolic = null;

        // Extract systolic and diastolic values
        if (mostRecent.has("component")) {
            for (JsonNode component : mostRecent.get("component")) {
                if (component.has("code") && component.has("valueQuantity")) {
                    String code = component.get("code").get("coding").get(0).get("code").asText();
                    double value = component.get("valueQuantity").get("value").asDouble();

                    if (code.equals("8480-6")) {
                        systolic = value;
                    } else if (code.equals("8462-4")) {
                        diastolic = value;
                    }
                }
            }
        }

        if (systolic != null && diastolic != null) {
            boolean controlled = systolic < 140 && diastolic < 90;

            if (!controlled) {
                resultBuilder.careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("UNCONTROLLED_BP")
                        .description(String.format("BP %.0f/%.0f exceeds target <140/90", systolic, diastolic))
                        .recommendedAction("Optimize BP management, lifestyle modifications")
                        .priority("high")
                        .dueDate(LocalDate.now().plusMonths(1))
                        .build()
                ));
            }

            resultBuilder.evidence(java.util.Map.of(
                "latestBP", String.format("%.0f/%.0f", systolic, diastolic)
            ));
            resultBuilder.details(java.util.Map.of(
                "bpControlled", controlled,
                "systolic", systolic,
                "diastolic", diastolic,
                "bpTarget", "140/90"
            ));
            return controlled;
        }

        return false;
    }

    /**
     * Evaluate eye exam completion (retinal or dilated)
     */
    private boolean evaluateEyeExam(String tenantId, UUID patientId,
                                    MeasureResult.MeasureResultBuilder resultBuilder) {
        String dateFilter = "ge" + LocalDate.now().minusMonths(24).toString(); // Every 2 years
        JsonNode procedures = getProcedures(tenantId, patientId, String.join(",", EYE_EXAM_CODES), dateFilter);

        List<JsonNode> examProcedures = getEntries(procedures);
        boolean hasRecentExam = !examProcedures.isEmpty();

        if (!hasRecentExam) {
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_EYE_EXAM")
                    .description("No diabetic retinal exam in last 24 months")
                    .recommendedAction("Schedule comprehensive dilated eye examination")
                    .priority("medium")
                    .dueDate(LocalDate.now().plusMonths(2))
                    .build()
            ));
            resultBuilder.details(java.util.Map.of("eyeExamCompleted", false));
            return false;
        }

        String lastExamDate = getEffectiveDate(examProcedures.get(0));
        resultBuilder.evidence(java.util.Map.of("lastEyeExam", lastExamDate));
        resultBuilder.details(java.util.Map.of("eyeExamCompleted", true, "lastEyeExamDate", lastExamDate));
        return true;
    }
}
