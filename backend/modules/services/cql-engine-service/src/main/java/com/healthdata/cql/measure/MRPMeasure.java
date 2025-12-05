package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * MRP - Medication Reconciliation Post-Discharge (HEDIS)
 *
 * Evaluates whether medication reconciliation was performed within 30 days of discharge
 * from an inpatient facility.
 *
 * Medication reconciliation: Comparing the patient's current medications to newly prescribed
 * medications and resolving discrepancies.
 */
@Component
public class MRPMeasure extends AbstractHedisMeasure {

    private static final List<String> INPATIENT_DISCHARGE_CODES = Arrays.asList(
        "58000006",  // Hospital discharge (SNOMED)
        "32485007",  // Hospital admission (SNOMED)
        "8715000",   // Inpatient admission (SNOMED)
        "305351004", // Admission to hospital (SNOMED)
        "50849002"   // Emergency room admission (SNOMED)
    );

    private static final List<String> MEDICATION_RECONCILIATION_CODES = Arrays.asList(
        "430193006", // Medication reconciliation (SNOMED)
        "182836005", // Review of medication (SNOMED)
        "416940007", // Past medication use unknown (SNOMED) - inverse indicator
        "308273005"  // Follow-up encounter (SNOMED) with medication review
    );

    private static final List<String> TRANSITIONAL_CARE_CODES = Arrays.asList(
        "185389009", // Follow-up visit (SNOMED)
        "390906007", // Follow-up encounter (SNOMED)
        "439708006", // Home visit by physician (SNOMED)
        "185317003"  // Telephone encounter (SNOMED)
    );

    @Override
    public String getMeasureId() {
        return "MRP";
    }

    @Override
    public String getMeasureName() {
        return "Medication Reconciliation Post-Discharge";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'MRP-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating MRP measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18+ with hospital discharge in last 30 days)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find most recent hospital discharge in last 30 days
        String dischargeDateFilter = "ge" + LocalDate.now().minusDays(30).toString();
        JsonNode dischargeEncounters = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_DISCHARGE_CODES), dischargeDateFilter);
        List<JsonNode> discharges = getEntries(dischargeEncounters);

        if (discharges.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No hospital discharge found in last 30 days")
                .build();
        }

        // Get discharge date
        JsonNode mostRecentDischarge = discharges.get(0);
        String dischargeDateStr = getEffectiveDate(mostRecentDischarge);
        LocalDate dischargeDate = LocalDate.parse(dischargeDateStr);

        // Calculate 30-day reconciliation window
        LocalDate reconciliationWindowEnd = dischargeDate.plusDays(30);

        // Check for medication reconciliation encounter/procedure within 30 days
        String reconciliationDateFilter = "ge" + dischargeDate.toString() +
                                         "&date=le" + reconciliationWindowEnd.toString();

        // Check for explicit medication reconciliation
        JsonNode reconEncounters = getEncounters(tenantId, patientId,
            String.join(",", MEDICATION_RECONCILIATION_CODES), reconciliationDateFilter);
        boolean hasMedicationReconciliation = !getEntries(reconEncounters).isEmpty();

        // Also check for transitional care visits (where reconciliation typically occurs)
        JsonNode transitionalVisits = getEncounters(tenantId, patientId,
            String.join(",", TRANSITIONAL_CARE_CODES), reconciliationDateFilter);
        boolean hasTransitionalCareVisit = !getEntries(transitionalVisits).isEmpty();

        // Patient meets measure if medication reconciliation OR transitional care visit occurred
        boolean hasMedRecon = hasMedicationReconciliation || hasTransitionalCareVisit;

        resultBuilder.inNumerator(hasMedRecon);
        resultBuilder.complianceRate(hasMedRecon ? 1.0 : 0.0);
        resultBuilder.score(hasMedRecon ? 100.0 : 0.0);

        if (hasMedRecon) {
            String reconType = hasMedicationReconciliation ? "Medication Reconciliation" :
                              "Transitional Care Visit";
            String reconDate = hasMedicationReconciliation ?
                              getEffectiveDate(getEntries(reconEncounters).get(0)) :
                              getEffectiveDate(getEntries(transitionalVisits).get(0));

            resultBuilder.evidence(java.util.Map.of(
                "dischargeDate", dischargeDateStr,
                "reconciliationType", reconType,
                "reconciliationDate", reconDate,
                "daysAfterDischarge", java.time.temporal.ChronoUnit.DAYS.between(dischargeDate, LocalDate.parse(reconDate))
            ));

            resultBuilder.details(java.util.Map.of(
                "medicationReconciliationComplete", true,
                "dischargeDate", dischargeDateStr,
                "reconciliationDate", reconDate,
                "reconciliationType", reconType
            ));
        } else {
            // Care gap - no medication reconciliation
            boolean isOverdue = LocalDate.now().isAfter(reconciliationWindowEnd);

            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_MEDICATION_RECONCILIATION")
                    .description(String.format("No medication reconciliation within 30 days of discharge (%s)", dischargeDateStr))
                    .recommendedAction("Schedule post-discharge follow-up visit with medication reconciliation within 30 days")
                    .priority("high")
                    .dueDate(isOverdue ? LocalDate.now().plusDays(2) : reconciliationWindowEnd)
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "medicationReconciliationComplete", false,
                "dischargeDate", dischargeDateStr,
                "reconciliationDueDate", reconciliationWindowEnd.toString(),
                "isOverdue", isOverdue
            ));
        }

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 18) {
            return false;
        }

        // Must have hospital discharge in last 30 days
        String dischargeDateFilter = "ge" + LocalDate.now().minusDays(30).toString();
        JsonNode dischargeEncounters = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_DISCHARGE_CODES), dischargeDateFilter);

        return !getEntries(dischargeEncounters).isEmpty();
    }
}
