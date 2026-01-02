package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * ASF - Unhealthy Alcohol Use Screening and Follow-Up (HEDIS)
 *
 * Evaluates whether adults were screened for unhealthy alcohol use using a
 * validated screening tool, and whether those who screened positive received
 * brief counseling or other follow-up.
 *
 * USPSTF Grade B recommendation for alcohol screening and behavioral counseling
 * interventions in primary care to reduce unhealthy alcohol use.
 *
 * Two components:
 * - Screening: Alcohol use screening with validated tool
 * - Follow-up: Brief counseling or referral if positive screen
 */
@Component
public class ASFMeasure extends AbstractHedisMeasure {

    private static final List<String> ALCOHOL_SCREENING_CODES = Arrays.asList(
        "68518-0",   // LOINC - AUDIT-C (Alcohol Use Disorders Identification Test - Consumption)
        "75626-2",   // LOINC - AUDIT total score
        "88037-7",   // LOINC - AUDIT-C total score
        "68519-8",   // LOINC - How often do you have a drink containing alcohol
        "75889-6",   // LOINC - How often do you have 6 or more drinks on one occasion
        "75855-7",   // LOINC - How many drinks containing alcohol do you have on a typical day
        "11331-6",   // LOINC - Alcohol use assessment
        "74208-4"    // LOINC - Alcohol screening
    );

    private static final List<String> ALCOHOL_USE_DISORDER_CODES = Arrays.asList(
        "7200002",   // SNOMED - Alcoholism
        "66590003",  // SNOMED - Alcohol abuse
        "191816009", // SNOMED - Alcohol dependence
        "15167005",  // SNOMED - Alcohol use disorder
        "361055000"  // SNOMED - Misuse of alcohol
    );

    private static final List<String> BRIEF_COUNSELING_CODES = Arrays.asList(
        "385989002", // SNOMED - Alcohol abuse counseling
        "24165007",  // SNOMED - Alcoholism counseling
        "413473000", // SNOMED - Counseling about alcohol consumption
        "281078001", // SNOMED - Health promotion - alcohol education
        "425014005", // SNOMED - Substance use treatment: alcohol withdrawal
        "56876005"   // SNOMED - Drug rehabilitation and detoxification
    );

    private static final List<String> ALCOHOL_TREATMENT_MEDICATION_CODES = Arrays.asList(
        "1091",      // RxNorm - Disulfiram (Antabuse)
        "7213",      // RxNorm - Naltrexone
        "313",       // RxNorm - Acamprosate
        "42347"      // RxNorm - Naltrexone extended-release injectable
    );

    @Override
    public String getMeasureId() {
        return "ASF";
    }

    @Override
    public String getMeasureName() {
        return "Unhealthy Alcohol Use Screening and Follow-Up";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'ASF-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating ASF measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18+)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for alcohol screening in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusYears(1).toString();
        JsonNode screenings = getObservations(tenantId, patientId,
            String.join(",", ALCOHOL_SCREENING_CODES), dateFilter);
        List<JsonNode> screeningEntries = getEntries(screenings);

        boolean hasScreening = !screeningEntries.isEmpty();
        String screeningDate = hasScreening ? getEffectiveDate(screeningEntries.get(0)) : null;

        // Extract screening score if available
        Integer auditScore = null;
        boolean screeningPositive = false;

        if (hasScreening) {
            JsonNode screening = screeningEntries.get(0);
            try {
                if (screening.has("valueQuantity")) {
                    JsonNode valueQuantity = screening.get("valueQuantity");
                    if (valueQuantity.has("value")) {
                        auditScore = valueQuantity.get("value").asInt();
                        // AUDIT-C: ≥4 for men, ≥3 for women = positive screen
                        // AUDIT (full): ≥8 = hazardous/harmful drinking
                        screeningPositive = auditScore >= 4; // Using conservative threshold
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not extract AUDIT score: {}", e.getMessage());
            }
        }

        // Check for follow-up if positive screen
        boolean hasFollowUp = false;
        if (hasScreening && screeningPositive) {
            // Check for brief counseling
            JsonNode counseling = getEncounters(tenantId, patientId,
                String.join(",", BRIEF_COUNSELING_CODES), dateFilter);
            hasFollowUp = !getEntries(counseling).isEmpty();

            // Also check for alcohol treatment medications
            if (!hasFollowUp) {
                JsonNode medications = getMedicationRequests(tenantId, patientId,
                    String.join(",", ALCOHOL_TREATMENT_MEDICATION_CODES), dateFilter);
                hasFollowUp = !getEntries(medications).isEmpty();
            }

            // Check for alcohol use disorder diagnosis (indicates clinical recognition)
            if (!hasFollowUp) {
                JsonNode audDiagnosis = getConditions(tenantId, patientId,
                    String.join(",", ALCOHOL_USE_DISORDER_CODES));
                hasFollowUp = !getEntries(audDiagnosis).isEmpty();
            }
        }

        // Calculate compliance
        int componentsCompleted = 0;
        if (hasScreening) componentsCompleted++;
        if (!screeningPositive || hasFollowUp) componentsCompleted++;  // Follow-up only required if positive

        double complianceRate = componentsCompleted / 2.0;

        // For numerator: screening completed (and follow-up if positive)
        boolean meetsGoal = hasScreening && (!screeningPositive || hasFollowUp);

        resultBuilder.inNumerator(meetsGoal);
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasScreening) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_ALCOHOL_SCREENING")
                .description("No alcohol use screening in last 12 months")
                .recommendedAction("Screen with AUDIT-C (3 questions) or single-item screen during routine visit; USPSTF Grade B recommendation")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());
        } else if (screeningPositive && !hasFollowUp) {
            String severityLevel = auditScore != null && auditScore >= 8 ? "moderate-severe" : "mild-moderate";

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("POSITIVE_ALCOHOL_SCREEN_NO_FOLLOWUP")
                .description(String.format("Positive alcohol screen (AUDIT score %d = %s unhealthy use) without follow-up",
                    auditScore != null ? auditScore : 0, severityLevel))
                .recommendedAction("Provide brief counseling (5-15 min feedback, advice, goal-setting); consider referral to specialty treatment if severe")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());

            if (auditScore != null && auditScore >= 15) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("SEVERE_ALCOHOL_USE_DISORDER")
                    .description(String.format("AUDIT score %d indicates likely alcohol dependence", auditScore))
                    .recommendedAction("Refer to addiction specialist; consider medications (naltrexone, acamprosate); assess for withdrawal risk")
                    .priority("high")
                    .dueDate(LocalDate.now().plusWeeks(1))
                    .build());
            }
        }

        resultBuilder.careGaps(careGaps);

        String screeningResult = "Unknown";
        if (auditScore != null) {
            if (auditScore < 4) {
                screeningResult = String.format("Negative (AUDIT %d - low risk)", auditScore);
            } else if (auditScore < 8) {
                screeningResult = String.format("Positive (AUDIT %d - hazardous use)", auditScore);
            } else if (auditScore < 15) {
                screeningResult = String.format("Positive (AUDIT %d - harmful use)", auditScore);
            } else {
                screeningResult = String.format("Positive (AUDIT %d - likely dependence)", auditScore);
            }
        }

        resultBuilder.details(java.util.Map.of(
            "hasScreening", hasScreening,
            "screeningDate", screeningDate != null ? screeningDate : "None",
            "auditScore", auditScore != null ? auditScore : "Not available",
            "screeningResult", screeningResult,
            "screeningPositive", screeningPositive,
            "hasFollowUp", hasFollowUp,
            "meetsGoal", meetsGoal
        ));

        resultBuilder.evidence(java.util.Map.of(
            "screeningCompleted", hasScreening,
            "screeningPositive", screeningPositive,
            "followUpProvided", screeningPositive ? hasFollowUp : "Not required"
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be age 18+ (adult screening measure)
        Integer age = getPatientAge(patient);
        if (age == null || age < 18) {
            return false;
        }

        return true;
    }
}
