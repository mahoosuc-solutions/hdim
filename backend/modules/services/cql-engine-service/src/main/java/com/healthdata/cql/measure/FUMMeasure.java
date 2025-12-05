package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * FUM - Follow-Up After Emergency Department Visit for Mental Illness (HEDIS)
 *
 * Evaluates whether patients with an ED visit for mental illness received follow-up care.
 * Two rates:
 * - 7-day follow-up: Within 7 days of ED visit
 * - 30-day follow-up: Within 30 days of ED visit
 */
@Component
public class FUMMeasure extends AbstractHedisMeasure {

    private static final List<String> MENTAL_ILLNESS_CODES = Arrays.asList(
        "35489007",  // Depression (SNOMED)
        "13746004",  // Bipolar disorder (SNOMED)
        "191718004", // Major depressive disorder (SNOMED)
        "16990005",  // Schizophrenia (SNOMED)
        "26025008",  // Schizoaffective disorder (SNOMED)
        "47505003",  // Posttraumatic stress disorder (SNOMED)
        "197480006", // Anxiety disorder (SNOMED)
        "191708003", // Psychotic disorder (SNOMED)
        "231504006", // Mixed anxiety and depressive disorder (SNOMED)
        "69322001"   // Generalized anxiety disorder (SNOMED)
    );

    private static final List<String> ED_VISIT_CODES = Arrays.asList(
        "50849002",  // Emergency room admission (SNOMED)
        "4525004",   // Emergency department patient visit (SNOMED)
        "183495009", // Emergency hospital admission (SNOMED)
        "308335008"  // Patient encounter procedure (SNOMED)
    );

    private static final List<String> MENTAL_HEALTH_VISIT_CODES = Arrays.asList(
        "76168009",  // Psychiatric diagnostic evaluation (SNOMED)
        "313234004", // Mental health counseling (SNOMED)
        "225337009", // Psychotherapy (SNOMED)
        "390906007", // Follow-up encounter (SNOMED)
        "183866006", // Follow-up psychiatric assessment (SNOMED)
        "185460008", // Psychiatric consultation (SNOMED)
        "406547006", // Psychiatric follow-up (SNOMED)
        "371797001"  // Psychotherapy counseling (SNOMED)
    );

    private static final List<String> TELEHEALTH_CODES = Arrays.asList(
        "185317003", // Telephone encounter (SNOMED)
        "308720009", // Telehealth consultation (SNOMED)
        "444971000124105" // Telehealth visit (SNOMED)
    );

    @Override
    public String getMeasureId() {
        return "FUM";
    }

    @Override
    public String getMeasureName() {
        return "Follow-Up After ED Visit for Mental Illness";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'FUM-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating FUM measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 6+ with ED visit for mental illness in last 30 days)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find most recent ED visit for mental illness
        String edDateFilter = "ge" + LocalDate.now().minusDays(30).toString();
        JsonNode edVisits = getEncounters(tenantId, patientId,
            String.join(",", ED_VISIT_CODES), edDateFilter);
        List<JsonNode> edVisitEntries = getEntries(edVisits);

        if (edVisitEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No ED visit found in last 30 days")
                .build();
        }

        // Get ED visit date
        JsonNode mostRecentEDVisit = edVisitEntries.get(0);
        String edVisitDateStr = getEffectiveDate(mostRecentEDVisit);
        LocalDate edVisitDate = LocalDate.parse(edVisitDateStr);

        // Calculate follow-up windows
        LocalDate sevenDayWindow = edVisitDate.plusDays(7);
        LocalDate thirtyDayWindow = edVisitDate.plusDays(30);

        // Check for mental health follow-up within 7 days
        String sevenDayDateFilter = "ge" + edVisitDate.toString() + "&date=le" + sevenDayWindow.toString();
        JsonNode sevenDayVisits = getEncounters(tenantId, patientId,
            String.join(",", MENTAL_HEALTH_VISIT_CODES), sevenDayDateFilter);
        boolean hasSevenDayFollowUp = !getEntries(sevenDayVisits).isEmpty();

        // Check for telehealth visits (also count as follow-up)
        JsonNode sevenDayTelehealth = getEncounters(tenantId, patientId,
            String.join(",", TELEHEALTH_CODES), sevenDayDateFilter);
        hasSevenDayFollowUp = hasSevenDayFollowUp || !getEntries(sevenDayTelehealth).isEmpty();

        // Check for mental health follow-up within 30 days
        String thirtyDayDateFilter = "ge" + edVisitDate.toString() + "&date=le" + thirtyDayWindow.toString();
        JsonNode thirtyDayVisits = getEncounters(tenantId, patientId,
            String.join(",", MENTAL_HEALTH_VISIT_CODES), thirtyDayDateFilter);
        boolean hasThirtyDayFollowUp = !getEntries(thirtyDayVisits).isEmpty();

        // Check for telehealth visits
        JsonNode thirtyDayTelehealth = getEncounters(tenantId, patientId,
            String.join(",", TELEHEALTH_CODES), thirtyDayDateFilter);
        hasThirtyDayFollowUp = hasThirtyDayFollowUp || !getEntries(thirtyDayTelehealth).isEmpty();

        // Calculate compliance
        int componentsCompleted = 0;
        if (hasSevenDayFollowUp) componentsCompleted++;
        if (hasThirtyDayFollowUp) componentsCompleted++;

        double complianceRate = componentsCompleted / 2.0;
        resultBuilder.inNumerator(hasSevenDayFollowUp); // Primary measure is 7-day
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!hasSevenDayFollowUp) {
            boolean isOverdue = LocalDate.now().isAfter(sevenDayWindow);
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_7DAY_MENTAL_HEALTH_FOLLOWUP")
                .description(String.format("No mental health follow-up within 7 days of ED visit (%s)", edVisitDateStr))
                .recommendedAction("Schedule urgent mental health follow-up within 7 days of ED visit (in-person or telehealth)")
                .priority("high")
                .dueDate(isOverdue ? LocalDate.now().plusDays(1) : sevenDayWindow)
                .build());
        }

        if (!hasThirtyDayFollowUp) {
            boolean isOverdue = LocalDate.now().isAfter(thirtyDayWindow);
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_30DAY_MENTAL_HEALTH_FOLLOWUP")
                .description(String.format("No mental health follow-up within 30 days of ED visit (%s)", edVisitDateStr))
                .recommendedAction("Schedule mental health follow-up within 30 days (psychotherapy, counseling, or psychiatric evaluation)")
                .priority(hasSevenDayFollowUp ? "medium" : "high")
                .dueDate(isOverdue ? LocalDate.now().plusDays(2) : thirtyDayWindow)
                .build());
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "edVisitDate", edVisitDateStr,
            "hasSevenDayFollowUp", hasSevenDayFollowUp,
            "hasThirtyDayFollowUp", hasThirtyDayFollowUp,
            "sevenDayWindow", sevenDayWindow.toString(),
            "thirtyDayWindow", thirtyDayWindow.toString()
        ));

        resultBuilder.evidence(java.util.Map.of(
            "edVisitDate", edVisitDateStr,
            "sevenDayFollowUp", hasSevenDayFollowUp,
            "thirtyDayFollowUp", hasThirtyDayFollowUp
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 6) {
            return false;
        }

        // Must have mental illness diagnosis
        JsonNode mentalIllnessConditions = getConditions(tenantId, patientId,
            String.join(",", MENTAL_ILLNESS_CODES));
        if (getEntries(mentalIllnessConditions).isEmpty()) {
            return false;
        }

        // Must have ED visit in last 30 days
        String edDateFilter = "ge" + LocalDate.now().minusDays(30).toString();
        JsonNode edVisits = getEncounters(tenantId, patientId,
            String.join(",", ED_VISIT_CODES), edDateFilter);

        return !getEntries(edVisits).isEmpty();
    }
}
