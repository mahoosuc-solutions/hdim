package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * DRR - Depression Remission or Response for Adolescents and Adults (HEDIS)
 *
 * Evaluates whether patients with depression show improvement using standardized tools.
 * Two rates:
 * - Depression Remission: PHQ-9 score <5 at follow-up
 * - Depression Response: ≥50% reduction in PHQ-9 score from baseline
 *
 * Requires baseline assessment and follow-up assessment 4-8 months later.
 */
@Component
public class DRRMeasure extends AbstractHedisMeasure {

    private static final List<String> DEPRESSION_CODES = Arrays.asList(
        "35489007",  // Depression (SNOMED)
        "191718004", // Major depressive disorder (SNOMED)
        "370143000", // Major depressive disorder (SNOMED)
        "192080009", // Chronic depression (SNOMED)
        "310495003", // Mild depression (SNOMED)
        "310496002", // Moderate depression (SNOMED)
        "310497006", // Severe depression (SNOMED)
        "231504006", // Mixed anxiety and depressive disorder (SNOMED)
        "36923009"   // Major depression, recurrent (SNOMED)
    );

    private static final List<String> PHQ9_CODES = Arrays.asList(
        "44261-6",   // LOINC - Patient Health Questionnaire 9 item (PHQ-9) total score
        "44249-1",   // LOINC - PHQ-9 quick depression assessment panel
        "55757-9"    // LOINC - Patient Health Questionnaire 2 item (PHQ-2)
    );

    private static final List<String> DEPRESSION_SCREENING_CODES = Arrays.asList(
        "73831-0",   // LOINC - Generalized anxiety disorder 7 item (GAD-7)
        "71969-0",   // LOINC - Hamilton depression scale
        "71950-0",   // LOINC - Beck depression inventory
        "44261-6"    // LOINC - PHQ-9
    );

    private static final List<String> ANTIDEPRESSANT_CODES = Arrays.asList(
        // SSRIs
        "4493",      // RxNorm - Fluoxetine (Prozac)
        "36437",     // RxNorm - Sertraline (Zoloft)
        "32937",     // RxNorm - Paroxetine (Paxil)
        "42355",     // RxNorm - Citalopram (Celexa)
        "321988",    // RxNorm - Escitalopram (Lexapro)
        "31565",     // RxNorm - Fluvoxamine (Luvox)
        // SNRIs
        "704",       // RxNorm - Venlafaxine (Effexor)
        "39786",     // RxNorm - Duloxetine (Cymbalta)
        "170375",    // RxNorm - Desvenlafaxine (Pristiq)
        // Other Antidepressants
        "42347",     // RxNorm - Bupropion (Wellbutrin)
        "6646",      // RxNorm - Mirtazapine (Remeron)
        "135446",    // RxNorm - Vilazodone (Viibryd)
        "131705"     // RxNorm - Vortioxetine (Trintellix)
    );

    private static final int REMISSION_THRESHOLD = 5;  // PHQ-9 < 5
    private static final double RESPONSE_REDUCTION = 0.50;  // 50% reduction

    @Override
    public String getMeasureId() {
        return "DRR";
    }

    @Override
    public String getMeasureName() {
        return "Depression Remission or Response";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'DRR-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating DRR measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 12+ with depression and baseline PHQ-9 assessment)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find baseline PHQ-9 assessment (4-8 months ago)
        String baselineDateFilter = "ge" + LocalDate.now().minusMonths(8).toString() +
                                   "&date=le" + LocalDate.now().minusMonths(4).toString();
        JsonNode baselineAssessments = getObservations(tenantId, patientId,
            String.join(",", PHQ9_CODES), baselineDateFilter);
        List<JsonNode> baselineEntries = getEntries(baselineAssessments);

        if (baselineEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No baseline PHQ-9 assessment 4-8 months ago")
                .careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("MISSING_DEPRESSION_BASELINE")
                        .description("No baseline depression assessment (PHQ-9) documented")
                        .recommendedAction("Administer PHQ-9 at next visit to establish baseline for treatment monitoring")
                        .priority("medium")
                        .dueDate(LocalDate.now().plusWeeks(2))
                        .build()
                ))
                .build();
        }

        // Get baseline PHQ-9 score
        JsonNode baselineAssessment = baselineEntries.get(0);
        String baselineDate = getEffectiveDate(baselineAssessment);
        Integer baselineScore = null;

        try {
            if (baselineAssessment.has("valueQuantity")) {
                JsonNode valueQuantity = baselineAssessment.get("valueQuantity");
                if (valueQuantity.has("value")) {
                    baselineScore = valueQuantity.get("value").asInt();
                }
            } else if (baselineAssessment.has("valueInteger")) {
                baselineScore = baselineAssessment.get("valueInteger").asInt();
            }
        } catch (Exception e) {
            logger.warn("Could not extract baseline PHQ-9 score: {}", e.getMessage());
        }

        if (baselineScore == null) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Baseline PHQ-9 score not available")
                .build();
        }

        // Find follow-up PHQ-9 assessment (within last 60 days)
        String followUpDateFilter = "ge" + LocalDate.now().minusDays(60).toString();
        JsonNode followUpAssessments = getObservations(tenantId, patientId,
            String.join(",", PHQ9_CODES), followUpDateFilter);
        List<JsonNode> followUpEntries = getEntries(followUpAssessments);

        if (followUpEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(true)
                .inNumerator(false)
                .careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("MISSING_DEPRESSION_FOLLOWUP")
                        .description(String.format("No follow-up PHQ-9 assessment (baseline score %d on %s)", baselineScore, baselineDate))
                        .recommendedAction("Administer follow-up PHQ-9 to assess treatment response (4-8 months after baseline)")
                        .priority("high")
                        .dueDate(LocalDate.now().plusWeeks(2))
                        .build()
                ))
                .details(java.util.Map.of(
                    "baselineScore", baselineScore,
                    "baselineDate", baselineDate,
                    "hasFollowUpAssessment", false
                ))
                .build();
        }

        // Get follow-up PHQ-9 score
        JsonNode followUpAssessment = followUpEntries.get(0);
        String followUpDate = getEffectiveDate(followUpAssessment);
        Integer followUpScore = null;

        try {
            if (followUpAssessment.has("valueQuantity")) {
                JsonNode valueQuantity = followUpAssessment.get("valueQuantity");
                if (valueQuantity.has("value")) {
                    followUpScore = valueQuantity.get("value").asInt();
                }
            } else if (followUpAssessment.has("valueInteger")) {
                followUpScore = followUpAssessment.get("valueInteger").asInt();
            }
        } catch (Exception e) {
            logger.warn("Could not extract follow-up PHQ-9 score: {}", e.getMessage());
        }

        if (followUpScore == null) {
            return resultBuilder
                .inDenominator(true)
                .inNumerator(false)
                .exclusionReason("Follow-up PHQ-9 score not available")
                .build();
        }

        // Calculate remission and response
        boolean hasRemission = followUpScore < REMISSION_THRESHOLD;
        int scoreReduction = baselineScore - followUpScore;
        double reductionPercentage = (double) scoreReduction / baselineScore;
        boolean hasResponse = reductionPercentage >= RESPONSE_REDUCTION;

        // For numerator: remission OR response
        boolean meetsOutcome = hasRemission || hasResponse;

        resultBuilder.inNumerator(meetsOutcome);
        resultBuilder.complianceRate(meetsOutcome ? 1.0 : reductionPercentage);
        resultBuilder.score(meetsOutcome ? 100.0 : reductionPercentage * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!meetsOutcome) {
            // Check if on antidepressant medication
            String medDateFilter = "ge" + LocalDate.now().minusMonths(3).toString();
            JsonNode antidepressants = getMedicationRequests(tenantId, patientId,
                String.join(",", ANTIDEPRESSANT_CODES), medDateFilter);
            boolean onAntidepressant = !getEntries(antidepressants).isEmpty();

            String gapDescription = String.format(
                "Inadequate depression treatment response (PHQ-9: %d → %d, %.0f%% reduction, need ≥50%% or score <5)",
                baselineScore, followUpScore, reductionPercentage * 100
            );

            String recommendedAction = onAntidepressant ?
                "Consider antidepressant dose adjustment, medication change, or add psychotherapy" :
                "Initiate antidepressant medication and/or psychotherapy for depression treatment";

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INADEQUATE_DEPRESSION_RESPONSE")
                .description(gapDescription)
                .recommendedAction(recommendedAction)
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());

            // If worsening, urgent action
            if (followUpScore > baselineScore) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("WORSENING_DEPRESSION")
                    .description(String.format("Depression symptoms worsening (PHQ-9: %d → %d)", baselineScore, followUpScore))
                    .recommendedAction("Urgent psychiatric evaluation; assess suicide risk; intensify treatment immediately")
                    .priority("high")
                    .dueDate(LocalDate.now().plusDays(3))
                    .build());
            }
        }

        resultBuilder.careGaps(careGaps);

        // Outcome category
        String outcomeCategory;
        if (hasRemission) {
            outcomeCategory = "Remission (PHQ-9 <5)";
        } else if (hasResponse) {
            outcomeCategory = "Response (≥50% reduction)";
        } else if (followUpScore < baselineScore) {
            outcomeCategory = "Partial response (<50% reduction)";
        } else if (followUpScore == baselineScore) {
            outcomeCategory = "No change";
        } else {
            outcomeCategory = "Worsening";
        }

        resultBuilder.details(java.util.Map.of(
            "baselineScore", baselineScore,
            "baselineDate", baselineDate,
            "followUpScore", followUpScore,
            "followUpDate", followUpDate,
            "scoreReduction", scoreReduction,
            "reductionPercentage", String.format("%.1f%%", reductionPercentage * 100),
            "hasRemission", hasRemission,
            "hasResponse", hasResponse,
            "outcomeCategory", outcomeCategory
        ));

        resultBuilder.evidence(java.util.Map.of(
            "baselinePHQ9", baselineScore,
            "followUpPHQ9", followUpScore,
            "remissionAchieved", hasRemission,
            "responseAchieved", hasResponse
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 12) {
            return false;
        }

        // Must have depression diagnosis
        JsonNode depressionConditions = getConditions(tenantId, patientId,
            String.join(",", DEPRESSION_CODES));
        if (getEntries(depressionConditions).isEmpty()) {
            return false;
        }

        // Must have baseline PHQ-9 assessment (4-8 months ago)
        String baselineDateFilter = "ge" + LocalDate.now().minusMonths(8).toString() +
                                   "&date=le" + LocalDate.now().minusMonths(4).toString();
        JsonNode baselineAssessments = getObservations(tenantId, patientId,
            String.join(",", PHQ9_CODES), baselineDateFilter);

        return !getEntries(baselineAssessments).isEmpty();
    }
}
