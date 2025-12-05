package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * ABA - Adult BMI Assessment (HEDIS)
 *
 * Evaluates whether adults had BMI documented in the measurement year with
 * a follow-up plan documented if BMI is outside normal parameters.
 *
 * Two rates:
 * - BMI Documentation: BMI recorded in measurement year
 * - Follow-Up Plan: Plan documented if BMI <18.5 or ≥25
 */
@Component
public class ABAMeasure extends AbstractHedisMeasure {

    private static final List<String> BMI_CODES = Arrays.asList(
        "39156-5",   // LOINC - Body mass index (BMI) [Ratio]
        "59574-4",   // LOINC - Body mass index (BMI) [Percentile]
        "89270-3"    // LOINC - Body mass index (BMI) [Calculated]
    );

    private static final List<String> WEIGHT_CODES = Arrays.asList(
        "29463-7",   // LOINC - Body weight
        "3141-9",    // LOINC - Body weight Measured
        "8350-1"     // LOINC - Body weight [Mass]
    );

    private static final List<String> HEIGHT_CODES = Arrays.asList(
        "8302-2",    // LOINC - Body height
        "8306-3",    // LOINC - Body height - lying
        "8308-9"     // LOINC - Body height - standing
    );

    private static final List<String> FOLLOW_UP_PLAN_CODES = Arrays.asList(
        "304549008", // SNOMED - Refer for dietary consultation
        "410200000", // SNOMED - Weight management counseling
        "281085002", // SNOMED - Weight loss diet education
        "386291006", // SNOMED - Exercise counseling
        "413315001", // SNOMED - Nutrition assessment
        "410177006", // SNOMED - Nutritional care
        "304549008"  // SNOMED - Referral to dietitian
    );

    private static final List<String> AMBULATORY_VISIT_CODES = Arrays.asList(
        "185349003", // SNOMED - Outpatient encounter
        "308335008", // SNOMED - Patient encounter procedure
        "390906007", // SNOMED - Follow-up encounter
        "185463005"  // SNOMED - Office visit
    );

    @Override
    public String getMeasureId() {
        return "ABA";
    }

    @Override
    public String getMeasureName() {
        return "Adult BMI Assessment";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'ABA-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating ABA measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18-74 with outpatient visit in measurement year)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for BMI documentation in measurement year
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode bmiObservations = getObservations(tenantId, patientId,
            String.join(",", BMI_CODES), dateFilter);
        List<JsonNode> bmiEntries = getEntries(bmiObservations);

        boolean hasBMI = !bmiEntries.isEmpty();
        Double bmiValue = null;
        String bmiDate = null;

        if (hasBMI) {
            JsonNode mostRecentBMI = bmiEntries.get(0);
            bmiDate = getEffectiveDate(mostRecentBMI);

            // Extract BMI value
            try {
                if (mostRecentBMI.has("valueQuantity")) {
                    JsonNode valueQuantity = mostRecentBMI.get("valueQuantity");
                    if (valueQuantity.has("value")) {
                        bmiValue = valueQuantity.get("value").asDouble();
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not extract BMI value: {}", e.getMessage());
            }

            // If no direct BMI, try to calculate from weight and height
            if (bmiValue == null) {
                bmiValue = calculateBMI(tenantId, patientId, dateFilter);
            }
        }

        // Determine BMI category
        String bmiCategory = "Unknown";
        boolean requiresFollowUpPlan = false;
        if (bmiValue != null) {
            if (bmiValue < 18.5) {
                bmiCategory = "Underweight (BMI <18.5)";
                requiresFollowUpPlan = true;
            } else if (bmiValue < 25.0) {
                bmiCategory = "Normal weight (BMI 18.5-24.9)";
                requiresFollowUpPlan = false;
            } else if (bmiValue < 30.0) {
                bmiCategory = "Overweight (BMI 25.0-29.9)";
                requiresFollowUpPlan = true;
            } else if (bmiValue < 35.0) {
                bmiCategory = "Obesity Class I (BMI 30.0-34.9)";
                requiresFollowUpPlan = true;
            } else if (bmiValue < 40.0) {
                bmiCategory = "Obesity Class II (BMI 35.0-39.9)";
                requiresFollowUpPlan = true;
            } else {
                bmiCategory = "Obesity Class III (BMI ≥40.0)";
                requiresFollowUpPlan = true;
            }
        }

        // Check for follow-up plan if BMI is abnormal
        boolean hasFollowUpPlan = false;
        if (requiresFollowUpPlan) {
            JsonNode followUpPlans = getEncounters(tenantId, patientId,
                String.join(",", FOLLOW_UP_PLAN_CODES), dateFilter);
            hasFollowUpPlan = !getEntries(followUpPlans).isEmpty();
        }

        // Calculate compliance
        int componentsCompleted = 0;
        if (hasBMI) componentsCompleted++;
        if (!requiresFollowUpPlan || hasFollowUpPlan) componentsCompleted++;

        double complianceRate = componentsCompleted / 2.0;
        resultBuilder.inNumerator(hasBMI && (!requiresFollowUpPlan || hasFollowUpPlan));
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!hasBMI) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_BMI_ASSESSMENT")
                .description("No BMI documented in measurement year")
                .recommendedAction("Document BMI at next visit (height and weight required)")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());
        } else if (requiresFollowUpPlan && !hasFollowUpPlan) {
            String priority = bmiValue >= 30 ? "high" : "medium";
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_BMI_FOLLOWUP_PLAN")
                .description(String.format("BMI %.1f is outside normal range but no follow-up plan documented", bmiValue))
                .recommendedAction("Document follow-up plan: nutrition counseling, exercise plan, or referral to dietitian")
                .priority(priority)
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "hasBMI", hasBMI,
            "bmiValue", bmiValue != null ? bmiValue : "Not available",
            "bmiCategory", bmiCategory,
            "requiresFollowUpPlan", requiresFollowUpPlan,
            "hasFollowUpPlan", hasFollowUpPlan,
            "bmiDate", bmiDate != null ? bmiDate : "Not available"
        ));

        resultBuilder.evidence(java.util.Map.of(
            "bmiDocumented", hasBMI,
            "bmiValue", bmiValue != null ? bmiValue : "Not available",
            "followUpPlanDocumented", requiresFollowUpPlan ? hasFollowUpPlan : "Not required"
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 18 || age > 74) {
            return false;
        }

        // Must have outpatient visit in measurement year
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode ambulatoryVisits = getEncounters(tenantId, patientId,
            String.join(",", AMBULATORY_VISIT_CODES), dateFilter);

        return !getEntries(ambulatoryVisits).isEmpty();
    }

    /**
     * Calculate BMI from weight and height observations if direct BMI not available
     */
    private Double calculateBMI(String tenantId, String patientId, String dateFilter) {
        try {
            // Get weight in kg
            JsonNode weightObs = getObservations(tenantId, patientId,
                String.join(",", WEIGHT_CODES), dateFilter);
            List<JsonNode> weightEntries = getEntries(weightObs);

            // Get height in meters
            JsonNode heightObs = getObservations(tenantId, patientId,
                String.join(",", HEIGHT_CODES), dateFilter);
            List<JsonNode> heightEntries = getEntries(heightObs);

            if (weightEntries.isEmpty() || heightEntries.isEmpty()) {
                return null;
            }

            JsonNode weightNode = weightEntries.get(0).get("valueQuantity");
            JsonNode heightNode = heightEntries.get(0).get("valueQuantity");

            if (weightNode == null || heightNode == null) {
                return null;
            }

            double weight = weightNode.get("value").asDouble();
            double height = heightNode.get("value").asDouble();

            // Convert to metric if needed
            String weightUnit = weightNode.has("unit") ? weightNode.get("unit").asText() : "kg";
            String heightUnit = heightNode.has("unit") ? heightNode.get("unit").asText() : "m";

            if (weightUnit.contains("lb")) {
                weight = weight * 0.453592; // pounds to kg
            }
            if (heightUnit.contains("cm")) {
                height = height / 100; // cm to m
            } else if (heightUnit.contains("in")) {
                height = height * 0.0254; // inches to m
            }

            // BMI = weight (kg) / height (m)^2
            return weight / (height * height);

        } catch (Exception e) {
            logger.warn("Could not calculate BMI from weight/height: {}", e.getMessage());
            return null;
        }
    }
}
