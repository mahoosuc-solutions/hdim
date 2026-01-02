package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * FUH - Follow-Up After Hospitalization for Mental Illness (HEDIS)
 *
 * Evaluates whether patients who were hospitalized for mental illness received
 * follow-up care with a mental health practitioner.
 *
 * Two rates:
 * - 7-day follow-up: Within 7 days of discharge
 * - 30-day follow-up: Within 30 days of discharge
 */
@Component
public class FUHMeasure extends AbstractHedisMeasure {

    private static final List<String> MENTAL_ILLNESS_CODES = Arrays.asList(
        "35489007",  // Major depression (SNOMED)
        "36923009",  // Major depressive disorder (SNOMED)
        "48589004",  // Depression disorder (SNOMED)
        "371596008", // Bipolar I disorder (SNOMED)
        "191620000", // Bipolar II disorder (SNOMED)
        "191618008", // Anxiety disorder (SNOMED)
        "231504006", // Psychotic disorder (SNOMED)
        "16990005",  // Schizophrenia (SNOMED)
        "1912002",   // Acute psychosis (SNOMED)
        "58214004"   // Schizophreniform disorder (SNOMED)
    );

    private static final List<String> INPATIENT_STAY_CODES = Arrays.asList(
        "32485007",  // Hospital admission (SNOMED)
        "8715000",   // Inpatient admission (SNOMED)
        "183452005", // Emergency hospital admission (SNOMED)
        "432621000124105" // Inpatient psychiatric admission (SNOMED)
    );

    private static final List<String> MENTAL_HEALTH_VISIT_CODES = Arrays.asList(
        "76168009",  // Psychiatric interview and evaluation (SNOMED)
        "313234004", // Mental health counseling (SNOMED)
        "183515006", // Mental health service (SNOMED)
        "225337009", // Psychotherapy (SNOMED)
        "40701008",  // Individual psychotherapy (SNOMED)
        "76168009",  // Psychiatric diagnostic evaluation (SNOMED)
        "10197000"   // Psychiatric interview with mental status examination (SNOMED)
    );

    private static final List<String> TELEHEALTH_MENTAL_HEALTH_CODES = Arrays.asList(
        "185317003", // Telephone encounter (SNOMED)
        "448337001"  // Telemedicine consultation (SNOMED)
    );

    @Override
    public String getMeasureId() {
        return "FUH";
    }

    @Override
    public String getMeasureName() {
        return "Follow-Up After Hospitalization for Mental Illness";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'FUH-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating FUH measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 6+ with mental health hospitalization in last 30 days)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find most recent mental health hospitalization discharge
        String hospitalizationDateFilter = "ge" + LocalDate.now().minusDays(30).toString();
        JsonNode hospitalizations = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_STAY_CODES), hospitalizationDateFilter);
        List<JsonNode> hospitalizationEntries = getEntries(hospitalizations);

        if (hospitalizationEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No mental health hospitalization found in last 30 days")
                .build();
        }

        // Get discharge date
        JsonNode mostRecentHospitalization = hospitalizationEntries.get(0);
        String dischargeDateStr = getEffectiveDate(mostRecentHospitalization);
        LocalDate dischargeDate = LocalDate.parse(dischargeDateStr);

        // Calculate follow-up windows
        LocalDate sevenDayWindow = dischargeDate.plusDays(7);
        LocalDate thirtyDayWindow = dischargeDate.plusDays(30);

        // Check for follow-up visits within 7 days
        String sevenDayDateFilter = "ge" + dischargeDate.toString() + "&date=le" + sevenDayWindow.toString();
        JsonNode sevenDayVisits = getEncounters(tenantId, patientId,
            String.join(",", MENTAL_HEALTH_VISIT_CODES), sevenDayDateFilter);
        boolean hasSevenDayFollowUp = !getEntries(sevenDayVisits).isEmpty();

        // Check for follow-up visits within 30 days
        String thirtyDayDateFilter = "ge" + dischargeDate.toString() + "&date=le" + thirtyDayWindow.toString();
        JsonNode thirtyDayVisits = getEncounters(tenantId, patientId,
            String.join(",", MENTAL_HEALTH_VISIT_CODES), thirtyDayDateFilter);
        boolean hasThirtyDayFollowUp = !getEntries(thirtyDayVisits).isEmpty();

        // Check for telehealth visits (also count)
        JsonNode telehealthVisits = getEncounters(tenantId, patientId,
            String.join(",", TELEHEALTH_MENTAL_HEALTH_CODES), thirtyDayDateFilter);
        boolean hasTelehealthFollowUp = !getEntries(telehealthVisits).isEmpty();

        hasThirtyDayFollowUp = hasThirtyDayFollowUp || hasTelehealthFollowUp;

        // Calculate compliance - both rates matter
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
                .gapType("MISSING_7DAY_FOLLOWUP")
                .description(String.format("No mental health follow-up within 7 days of discharge (%s)", dischargeDateStr))
                .recommendedAction("Schedule mental health follow-up visit within 7 days of discharge")
                .priority("high")
                .dueDate(isOverdue ? LocalDate.now().plusDays(1) : sevenDayWindow)
                .build());
        }

        if (!hasThirtyDayFollowUp) {
            boolean isOverdue = LocalDate.now().isAfter(thirtyDayWindow);
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_30DAY_FOLLOWUP")
                .description(String.format("No mental health follow-up within 30 days of discharge (%s)", dischargeDateStr))
                .recommendedAction("Schedule mental health follow-up visit within 30 days of discharge")
                .priority(hasSevenDayFollowUp ? "medium" : "high")
                .dueDate(isOverdue ? LocalDate.now().plusDays(2) : thirtyDayWindow)
                .build());
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "dischargeDate", dischargeDateStr,
            "hasSevenDayFollowUp", hasSevenDayFollowUp,
            "hasThirtyDayFollowUp", hasThirtyDayFollowUp,
            "sevenDayWindow", sevenDayWindow.toString(),
            "thirtyDayWindow", thirtyDayWindow.toString()
        ));

        resultBuilder.evidence(java.util.Map.of(
            "dischargeDate", dischargeDateStr,
            "sevenDayFollowUp", hasSevenDayFollowUp,
            "thirtyDayFollowUp", hasThirtyDayFollowUp,
            "telehealthUsed", hasTelehealthFollowUp
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
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

        // Must have mental health hospitalization in last 30 days
        String hospitalizationDateFilter = "ge" + LocalDate.now().minusDays(30).toString();
        JsonNode hospitalizations = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_STAY_CODES), hospitalizationDateFilter);

        return !getEntries(hospitalizations).isEmpty();
    }
}
