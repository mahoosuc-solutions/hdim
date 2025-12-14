package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DSF - Depression Screening and Follow-up for Adolescents and Adults
 *
 * CMS Measure ID: CMS2v13 (Depression Screening)
 * APP Plus 2025: Required measure for ACO performance
 *
 * Measure Description:
 * Percentage of patients aged 12 years and older screened for depression
 * on the date of the encounter or up to 14 days prior to the date of the
 * encounter using an age-appropriate standardized depression screening tool,
 * AND if positive, a follow-up plan is documented on the date of the
 * eligible encounter.
 *
 * Eligible Population:
 * - Ages 12 years and older at the start of the measurement period
 * - With at least one qualifying encounter during the measurement period
 *
 * Numerator:
 * - Patients screened for depression AND
 * - If screen is positive, a follow-up plan is documented
 *
 * Denominator Exclusions:
 * - Active diagnosis of depression or bipolar disorder
 * - Patient refusal documented
 *
 * Standardized Screening Tools:
 * - PHQ-2 (Patient Health Questionnaire-2)
 * - PHQ-9 (Patient Health Questionnaire-9)
 * - PHQ-A (Modified for Adolescents)
 * - Beck Depression Inventory (BDI)
 * - CES-D (Center for Epidemiologic Studies Depression Scale)
 * - HADS (Hospital Anxiety and Depression Scale)
 * - Geriatric Depression Scale (GDS)
 */
@Component
public class DSFMeasure extends AbstractHedisMeasure {

    // LOINC codes for PHQ screening instruments
    private static final List<String> PHQ2_CODES = Arrays.asList(
        "55757-9",   // PHQ-2 total score [Reported]
        "58223-8"    // PHQ-2 screening result
    );

    private static final List<String> PHQ9_CODES = Arrays.asList(
        "44261-6",   // PHQ-9 total score [Reported]
        "55758-7"    // PHQ-9 total score [Calculated]
    );

    private static final List<String> PHQ_A_CODES = Arrays.asList(
        "69725-0",   // PHQ-A Modified for Teens total score
        "89204-2"    // PHQ-9 Modified for Adolescents
    );

    // Other validated depression screening tools
    private static final List<String> OTHER_SCREENING_CODES = Arrays.asList(
        "73831-0",   // Beck Depression Inventory II (BDI-II) total score
        "71390-7",   // Center for Epidemiologic Studies Depression Scale (CES-D)
        "89206-7",   // Geriatric Depression Scale (GDS) Short Form
        "70274-6"    // Hospital Anxiety and Depression Scale (HADS)
    );

    // All depression screening LOINC codes
    private static final List<String> ALL_SCREENING_CODES;
    static {
        ALL_SCREENING_CODES = new ArrayList<>();
        ALL_SCREENING_CODES.addAll(PHQ2_CODES);
        ALL_SCREENING_CODES.addAll(PHQ9_CODES);
        ALL_SCREENING_CODES.addAll(PHQ_A_CODES);
        ALL_SCREENING_CODES.addAll(OTHER_SCREENING_CODES);
    }

    // SNOMED CT codes for follow-up plan
    private static final List<String> FOLLOW_UP_PLAN_CODES = Arrays.asList(
        "183524004", // Referral to psychiatry
        "385893007", // Follow-up care arrangement
        "308459004", // Referral to mental health service
        "710914003", // Referral to counseling service
        "225337009", // Suicide risk assessment
        "229065009", // Mental health care plan
        "30346009",  // Psychotherapy
        "87512008",  // Antidepressant therapy
        "406149000"  // Follow-up for depression
    );

    // SNOMED CT codes for depression/bipolar exclusions
    private static final List<String> DEPRESSION_EXCLUSION_CODES = Arrays.asList(
        "35489007",  // Depressive disorder
        "370143000", // Major depressive disorder
        "191616006", // Major depression, recurrent
        "13746004",  // Bipolar disorder
        "5703000",   // Bipolar I disorder
        "83225003"   // Bipolar II disorder
    );

    // Encounter types that qualify for the measure
    private static final List<String> QUALIFYING_ENCOUNTER_CODES = Arrays.asList(
        "99201", "99202", "99203", "99204", "99205", // New patient office
        "99211", "99212", "99213", "99214", "99215", // Established patient office
        "99381", "99382", "99383", "99384", "99385", // Preventive new
        "99391", "99392", "99393", "99394", "99395", // Preventive established
        "99401", "99402", "99403", "99404"           // Preventive counseling
    );

    // Positive screening thresholds
    private static final int PHQ2_POSITIVE_THRESHOLD = 3;  // Score >= 3 is positive
    private static final int PHQ9_POSITIVE_THRESHOLD = 10; // Score >= 10 is moderate depression

    @Override
    public String getMeasureId() {
        return "DSF";
    }

    @Override
    public String getMeasureName() {
        return "Depression Screening and Follow-up for Adolescents and Adults";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'DSF-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating DSF measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        // Check if patient is eligible (age 12+ with qualifying encounter)
        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient does not meet eligibility criteria (age <12 or no qualifying encounter)")
                .build();
        }

        // Check for denominator exclusions
        String exclusionReason = checkDenominatorExclusions(tenantId, patientId);
        if (exclusionReason != null) {
            return resultBuilder
                .inDenominator(true)
                .inNumerator(false)
                .exclusionReason(exclusionReason)
                .build();
        }

        resultBuilder.inDenominator(true);

        // Evaluate depression screening
        ScreeningResult screeningResult = evaluateDepressionScreening(tenantId, patientId, resultBuilder);

        if (!screeningResult.wasScreened) {
            // Not screened - not in numerator
            resultBuilder
                .inNumerator(false)
                .complianceRate(0.0)
                .score(0.0)
                .careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("MISSING_DEPRESSION_SCREENING")
                        .description("No depression screening performed in last 12 months")
                        .recommendedAction("Administer PHQ-2 or PHQ-9 screening at next visit")
                        .priority("high")
                        .dueDate(LocalDate.now().plusDays(30))
                        .build()
                ));
        } else if (screeningResult.isPositive && !screeningResult.hasFollowUpPlan) {
            // Screened positive but no follow-up plan - not in numerator
            resultBuilder
                .inNumerator(false)
                .complianceRate(0.5)
                .score(50.0)
                .careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("MISSING_DEPRESSION_FOLLOWUP")
                        .description(String.format("Positive depression screen (score %d) without documented follow-up plan",
                            screeningResult.screeningScore))
                        .recommendedAction("Document follow-up plan: counseling referral, medication initiation, or other treatment plan")
                        .priority("high")
                        .dueDate(LocalDate.now().plusDays(14))
                        .build()
                ));
        } else {
            // Either screened negative, or screened positive with follow-up plan
            resultBuilder
                .inNumerator(true)
                .complianceRate(1.0)
                .score(100.0);
        }

        MeasureResult result = resultBuilder.build();
        logger.info("DSF evaluation complete for patient {}: score={}, inNumerator={}",
            patientId, result.getScore(), result.isInNumerator());

        return result;
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        // Check patient age (must be 12 or older)
        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);
        if (age == null || age < 12) {
            return false;
        }

        // Check for qualifying encounter in measurement period
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode encounters = getEncounters(tenantId, patientId, null, dateFilter);

        List<JsonNode> encounterEntries = getEntries(encounters);
        for (JsonNode encounter : encounterEntries) {
            // Check if encounter type qualifies
            if (encounter.has("type")) {
                for (JsonNode type : encounter.get("type")) {
                    if (type.has("coding")) {
                        for (JsonNode coding : type.get("coding")) {
                            String code = coding.has("code") ? coding.get("code").asText() : null;
                            if (code != null && QUALIFYING_ENCOUNTER_CODES.contains(code)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        // Default to true if we can't verify encounter types (allow broader screening)
        return !encounterEntries.isEmpty();
    }

    /**
     * Check for denominator exclusions (active depression or bipolar diagnosis).
     */
    private String checkDenominatorExclusions(String tenantId, String patientId) {
        JsonNode conditions = getConditions(tenantId, patientId, null);
        if (conditions == null) {
            return null;
        }

        List<JsonNode> conditionEntries = getEntries(conditions);
        for (JsonNode condition : conditionEntries) {
            if (hasCode(condition, DEPRESSION_EXCLUSION_CODES)) {
                // Check if condition is active
                if (condition.has("clinicalStatus") &&
                    condition.get("clinicalStatus").has("coding")) {
                    for (JsonNode coding : condition.get("clinicalStatus").get("coding")) {
                        String status = coding.has("code") ? coding.get("code").asText() : null;
                        if ("active".equals(status)) {
                            return "Patient has active depression or bipolar disorder diagnosis";
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Evaluate depression screening and follow-up plan.
     */
    private ScreeningResult evaluateDepressionScreening(String tenantId, String patientId,
                                                         MeasureResult.MeasureResultBuilder resultBuilder) {
        ScreeningResult result = new ScreeningResult();

        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        // Look for depression screening observations
        JsonNode observations = getObservations(tenantId, patientId,
            String.join(",", ALL_SCREENING_CODES), dateFilter);

        List<JsonNode> screeningObs = getEntries(observations);
        if (screeningObs.isEmpty()) {
            result.wasScreened = false;
            return result;
        }

        result.wasScreened = true;

        // Get most recent screening result
        JsonNode mostRecentScreening = screeningObs.get(0);
        String screeningDate = getEffectiveDate(mostRecentScreening);
        result.screeningDate = screeningDate;

        // Determine screening type and score
        String screeningType = determineScreeningType(mostRecentScreening);
        result.screeningType = screeningType;

        // Extract screening score
        if (mostRecentScreening.has("valueQuantity")) {
            result.screeningScore = (int) mostRecentScreening.get("valueQuantity").get("value").asDouble();
        } else if (mostRecentScreening.has("valueInteger")) {
            result.screeningScore = mostRecentScreening.get("valueInteger").asInt();
        }

        // Determine if positive based on screening tool and score
        result.isPositive = isPositiveScreen(screeningType, result.screeningScore);

        // Record evidence
        resultBuilder.evidence(java.util.Map.of(
            "screeningDate", screeningDate != null ? screeningDate : "unknown",
            "screeningType", screeningType,
            "screeningScore", result.screeningScore,
            "isPositive", result.isPositive
        ));

        resultBuilder.details(java.util.Map.of(
            "wasScreened", true,
            "screeningType", screeningType,
            "screeningScore", result.screeningScore,
            "positiveScreen", result.isPositive
        ));

        // If positive, check for follow-up plan
        if (result.isPositive) {
            result.hasFollowUpPlan = evaluateFollowUpPlan(tenantId, patientId, screeningDate, resultBuilder);
        } else {
            // Negative screen - no follow-up required
            result.hasFollowUpPlan = true;
        }

        return result;
    }

    /**
     * Determine the type of depression screening based on LOINC codes.
     */
    private String determineScreeningType(JsonNode observation) {
        if (observation.has("code") && observation.get("code").has("coding")) {
            for (JsonNode coding : observation.get("code").get("coding")) {
                String code = coding.has("code") ? coding.get("code").asText() : null;
                if (code != null) {
                    if (PHQ2_CODES.contains(code)) return "PHQ-2";
                    if (PHQ9_CODES.contains(code)) return "PHQ-9";
                    if (PHQ_A_CODES.contains(code)) return "PHQ-A";
                    if (code.equals("73831-0")) return "BDI-II";
                    if (code.equals("71390-7")) return "CES-D";
                    if (code.equals("89206-7")) return "GDS";
                    if (code.equals("70274-6")) return "HADS";
                }
            }
        }
        return "Unknown";
    }

    /**
     * Determine if the screening result is positive based on tool-specific thresholds.
     */
    private boolean isPositiveScreen(String screeningType, int score) {
        return switch (screeningType) {
            case "PHQ-2" -> score >= PHQ2_POSITIVE_THRESHOLD;
            case "PHQ-9", "PHQ-A" -> score >= PHQ9_POSITIVE_THRESHOLD;
            case "BDI-II" -> score >= 14;    // 14+ is mild depression
            case "CES-D" -> score >= 16;     // 16+ is possible depression
            case "GDS" -> score >= 5;        // 5+ suggests depression (short form)
            case "HADS" -> score >= 8;       // 8+ is borderline abnormal
            default -> score > 0;            // Conservative default
        };
    }

    /**
     * Check for documented follow-up plan after positive screen.
     */
    private boolean evaluateFollowUpPlan(String tenantId, String patientId, String screeningDate,
                                         MeasureResult.MeasureResultBuilder resultBuilder) {
        // Look for follow-up procedures or care plans documented on or after screening date
        String dateFilter = screeningDate != null ? "ge" + screeningDate.substring(0, 10) : null;

        // Check procedures for follow-up activities
        JsonNode procedures = getProcedures(tenantId, patientId,
            String.join(",", FOLLOW_UP_PLAN_CODES), dateFilter);

        List<JsonNode> followUpProcedures = getEntries(procedures);
        if (!followUpProcedures.isEmpty()) {
            String followUpDate = getEffectiveDate(followUpProcedures.get(0));
            resultBuilder.evidence(java.util.Map.of(
                "followUpPlanDocumented", true,
                "followUpDate", followUpDate != null ? followUpDate : "unknown"
            ));
            resultBuilder.details(java.util.Map.of(
                "hasFollowUpPlan", true,
                "followUpType", "procedure"
            ));
            return true;
        }

        // Also check for medication orders (antidepressants) as follow-up
        JsonNode medications = getMedicationRequests(tenantId, patientId);
        List<JsonNode> medicationEntries = getEntries(medications);
        for (JsonNode medication : medicationEntries) {
            // Check if medication is for depression treatment
            if (medication.has("reasonCode")) {
                for (JsonNode reason : medication.get("reasonCode")) {
                    if (reason.has("coding")) {
                        for (JsonNode coding : reason.get("coding")) {
                            String code = coding.has("code") ? coding.get("code").asText() : null;
                            if (code != null && DEPRESSION_EXCLUSION_CODES.contains(code)) {
                                resultBuilder.evidence(java.util.Map.of(
                                    "followUpPlanDocumented", true,
                                    "followUpType", "antidepressant medication"
                                ));
                                resultBuilder.details(java.util.Map.of(
                                    "hasFollowUpPlan", true,
                                    "followUpType", "medication"
                                ));
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Internal class to hold screening evaluation results.
     */
    private static class ScreeningResult {
        boolean wasScreened = false;
        boolean isPositive = false;
        boolean hasFollowUpPlan = false;
        String screeningDate;
        String screeningType;
        int screeningScore = 0;
    }
}
