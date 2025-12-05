package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * FUA - Follow-Up After Emergency Department Visit for Alcohol and Other Drug Abuse or Dependence (HEDIS)
 *
 * Evaluates whether patients with an ED visit for AOD received follow-up care.
 * Two rates:
 * - 7-day follow-up: Within 7 days of ED visit
 * - 30-day follow-up: Within 30 days of ED visit
 */
@Component
public class FUAMeasure extends AbstractHedisMeasure {

    private static final List<String> AOD_CODES = Arrays.asList(
        "191816009", // Alcohol abuse (SNOMED)
        "15167005",  // Alcohol dependence (SNOMED)
        "66590003",  // Drug abuse (SNOMED)
        "191874001", // Drug dependence (SNOMED)
        "231470003", // Opioid dependence (SNOMED)
        "230328006", // Cocaine dependence (SNOMED)
        "230346007", // Cannabis dependence (SNOMED)
        "191819002"  // Alcoholic intoxication (SNOMED)
    );

    private static final List<String> ED_VISIT_CODES = Arrays.asList(
        "50849002",  // Emergency room admission (SNOMED)
        "4525004",   // Emergency department patient visit (SNOMED)
        "183495009", // Emergency hospital admission (SNOMED)
        "308335008"  // Patient encounter procedure (SNOMED)
    );

    private static final List<String> AOD_TREATMENT_CODES = Arrays.asList(
        "56876005",  // Drug rehabilitation therapy (SNOMED)
        "60112009",  // Alcohol rehabilitation therapy (SNOMED)
        "385989002", // Substance abuse counseling (SNOMED)
        "371597004", // Addiction counseling (SNOMED)
        "385990006", // Group substance abuse counseling (SNOMED)
        "313071005"  // Alcohol counseling (SNOMED)
    );

    private static final List<String> BEHAVIORAL_HEALTH_VISIT_CODES = Arrays.asList(
        "76168009",  // Psychiatric diagnostic evaluation (SNOMED)
        "313234004", // Mental health counseling (SNOMED)
        "225337009", // Psychotherapy (SNOMED)
        "390906007"  // Follow-up encounter (SNOMED)
    );

    @Override
    public String getMeasureId() {
        return "FUA";
    }

    @Override
    public String getMeasureName() {
        return "Follow-Up After ED Visit for Alcohol and Other Drug Abuse";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'FUA-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating FUA measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 13+ with ED visit for AOD in last 30 days)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find most recent ED visit for AOD
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

        // Check for AOD treatment follow-up within 7 days
        String sevenDayDateFilter = "ge" + edVisitDate.toString() + "&date=le" + sevenDayWindow.toString();
        JsonNode sevenDayTreatment = getEncounters(tenantId, patientId,
            String.join(",", AOD_TREATMENT_CODES), sevenDayDateFilter);
        boolean hasSevenDayFollowUp = !getEntries(sevenDayTreatment).isEmpty();

        // Check for behavioral health visits
        JsonNode sevenDayBH = getEncounters(tenantId, patientId,
            String.join(",", BEHAVIORAL_HEALTH_VISIT_CODES), sevenDayDateFilter);
        hasSevenDayFollowUp = hasSevenDayFollowUp || !getEntries(sevenDayBH).isEmpty();

        // Check for AOD treatment follow-up within 30 days
        String thirtyDayDateFilter = "ge" + edVisitDate.toString() + "&date=le" + thirtyDayWindow.toString();
        JsonNode thirtyDayTreatment = getEncounters(tenantId, patientId,
            String.join(",", AOD_TREATMENT_CODES), thirtyDayDateFilter);
        boolean hasThirtyDayFollowUp = !getEntries(thirtyDayTreatment).isEmpty();

        // Check for behavioral health visits
        JsonNode thirtyDayBH = getEncounters(tenantId, patientId,
            String.join(",", BEHAVIORAL_HEALTH_VISIT_CODES), thirtyDayDateFilter);
        hasThirtyDayFollowUp = hasThirtyDayFollowUp || !getEntries(thirtyDayBH).isEmpty();

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
                .gapType("MISSING_7DAY_AOD_FOLLOWUP")
                .description(String.format("No AOD treatment follow-up within 7 days of ED visit (%s)", edVisitDateStr))
                .recommendedAction("Schedule substance abuse counseling or behavioral health follow-up within 7 days of ED visit")
                .priority("high")
                .dueDate(isOverdue ? LocalDate.now().plusDays(1) : sevenDayWindow)
                .build());
        }

        if (!hasThirtyDayFollowUp) {
            boolean isOverdue = LocalDate.now().isAfter(thirtyDayWindow);
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_30DAY_AOD_FOLLOWUP")
                .description(String.format("No AOD treatment follow-up within 30 days of ED visit (%s)", edVisitDateStr))
                .recommendedAction("Schedule substance abuse treatment or counseling within 30 days")
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
        if (age == null || age < 13) {
            return false;
        }

        // Must have AOD diagnosis
        JsonNode aodConditions = getConditions(tenantId, patientId,
            String.join(",", AOD_CODES));
        if (getEntries(aodConditions).isEmpty()) {
            return false;
        }

        // Must have ED visit in last 30 days
        String edDateFilter = "ge" + LocalDate.now().minusDays(30).toString();
        JsonNode edVisits = getEncounters(tenantId, patientId,
            String.join(",", ED_VISIT_CODES), edDateFilter);

        return !getEntries(edVisits).isEmpty();
    }
}
