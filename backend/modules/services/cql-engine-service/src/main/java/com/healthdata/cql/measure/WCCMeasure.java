package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * WCC - Weight Assessment and Counseling for Children (HEDIS)
 *
 * Evaluates BMI documentation and nutrition/physical activity counseling
 * for children and adolescents aged 3-17.
 */
@Component
public class WCCMeasure extends AbstractHedisMeasure {

    private static final List<String> BMI_CODES = Arrays.asList(
        "39156-5",   // BMI
        "59574-4",   // BMI percentile
        "59576-9"    // BMI for age percentile
    );

    private static final List<String> NUTRITION_COUNSELING_CODES = Arrays.asList(
        "61310006",  // Nutrition counseling
        "281085002", // Dietary education
        "410177006"  // Healthy eating education
    );

    private static final List<String> PHYSICAL_ACTIVITY_COUNSELING_CODES = Arrays.asList(
        "409063005", // Physical activity counseling
        "304549008", // Exercise counseling
        "390893007"  // Physical activity education
    );

    @Override
    public String getMeasureId() {
        return "WCC";
    }

    @Override
    public String getMeasureName() {
        return "Weight Assessment and Counseling for Children";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'WCC-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating WCC measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 3-17)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for BMI documentation in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode bmiObs = getObservations(tenantId, patientId, String.join(",", BMI_CODES), dateFilter);
        boolean hasBMI = !getEntries(bmiObs).isEmpty();

        // Check for nutrition counseling in last 12 months
        JsonNode nutritionProcs = getProcedures(tenantId, patientId, String.join(",", NUTRITION_COUNSELING_CODES), dateFilter);
        boolean hasNutritionCounseling = !getEntries(nutritionProcs).isEmpty();

        // Check for physical activity counseling in last 12 months
        JsonNode activityProcs = getProcedures(tenantId, patientId, String.join(",", PHYSICAL_ACTIVITY_COUNSELING_CODES), dateFilter);
        boolean hasActivityCounseling = !getEntries(activityProcs).isEmpty();

        // Patient is in numerator if all three components are met
        boolean inNumerator = hasBMI && hasNutritionCounseling && hasActivityCounseling;
        resultBuilder.inNumerator(inNumerator);

        // Calculate compliance
        int componentsCompleted = 0;
        if (hasBMI) componentsCompleted++;
        if (hasNutritionCounseling) componentsCompleted++;
        if (hasActivityCounseling) componentsCompleted++;

        double complianceRate = componentsCompleted / 3.0;
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Track evidence
        if (hasBMI) {
            JsonNode bmiEntry = getEntries(bmiObs).get(0);
            if (bmiEntry.has("valueQuantity")) {
                double bmiValue = bmiEntry.get("valueQuantity").get("value").asDouble();
                resultBuilder.evidence(java.util.Map.of("latestBMI", bmiValue));
            }
        }

        // Identify care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!hasBMI) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_BMI")
                .description("No BMI documentation in last 12 months")
                .recommendedAction("Document BMI percentile")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }
        if (!hasNutritionCounseling) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_NUTRITION_COUNSELING")
                .description("No nutrition counseling in last 12 months")
                .recommendedAction("Provide nutrition education and counseling")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }
        if (!hasActivityCounseling) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_ACTIVITY_COUNSELING")
                .description("No physical activity counseling in last 12 months")
                .recommendedAction("Provide physical activity guidance")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "hasBMI", hasBMI,
            "hasNutritionCounseling", hasNutritionCounseling,
            "hasActivityCounseling", hasActivityCounseling
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        return age != null && age >= 3 && age <= 17;
    }
}
