package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * TSC - Transitions of Care (HEDIS)
 *
 * Evaluates quality of care transitions after hospital discharge.
 * Four components tracked:
 * 1. Notification of inpatient admission
 * 2. Receipt of discharge information
 * 3. Patient engagement after discharge (within 30 days)
 * 4. Medication reconciliation (within 30 days)
 *
 * Measures successful handoff from hospital to community provider.
 */
@Component
public class TSCMeasure extends AbstractHedisMeasure {

    private static final List<String> INPATIENT_ADMISSION_CODES = Arrays.asList(
        "32485007",  // Hospital admission (SNOMED)
        "8715000",   // Inpatient admission (SNOMED)
        "183452005", // Emergency hospital admission (SNOMED)
        "305351004", // Admission to hospital (SNOMED)
        "447941000124106" // Hospital observation care (SNOMED)
    );

    private static final List<String> INPATIENT_DISCHARGE_CODES = Arrays.asList(
        "58000006",  // Hospital discharge (SNOMED)
        "306689006", // Discharge from hospital (SNOMED)
        "306685001"  // Discharge to home (SNOMED)
    );

    private static final List<String> POST_DISCHARGE_VISIT_CODES = Arrays.asList(
        "390906007", // Follow-up encounter (SNOMED)
        "185463005", // Office visit (SNOMED)
        "185349003", // Outpatient encounter (SNOMED)
        "308335008", // Patient encounter procedure (SNOMED)
        "439708006", // Home visit (SNOMED)
        "185317003"  // Telephone encounter (SNOMED) - includes telehealth
    );

    private static final List<String> MEDICATION_RECONCILIATION_CODES = Arrays.asList(
        "430193006", // SNOMED - Medication reconciliation (procedure)
        "182777000", // SNOMED - Medication review
        "134436002"  // SNOMED - Lifestyle education regarding medication
    );

    private static final List<String> TRANSITIONAL_CARE_CODES = Arrays.asList(
        "185347001", // SNOMED - Encounter for problem
        "77406008",  // SNOMED - Confirmation of medication compliance
        "410155007", // SNOMED - Occupational therapy assessment
        "183856001"  // SNOMED - Assessment of health education needs
    );

    @Override
    public String getMeasureId() {
        return "TSC";
    }

    @Override
    public String getMeasureName() {
        return "Transitions of Care";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'TSC-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating TSC measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18+ with hospital discharge in last 60 days)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find most recent hospital discharge
        String dischargeDateFilter = "ge" + LocalDate.now().minusDays(60).toString();
        JsonNode dischargeEncounters = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_DISCHARGE_CODES), dischargeDateFilter);
        List<JsonNode> discharges = getEntries(dischargeEncounters);

        if (discharges.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No hospital discharge found in last 60 days")
                .build();
        }

        // Get discharge date
        JsonNode mostRecentDischarge = discharges.get(0);
        String dischargeDateStr = getEffectiveDate(mostRecentDischarge);
        LocalDate dischargeDate = LocalDate.parse(dischargeDateStr);

        // Calculate 30-day follow-up window
        LocalDate followUpWindowEnd = dischargeDate.plusDays(30);

        // Component 1: Notification of inpatient admission (documented in records)
        // This would typically be in hospital records - checking for admission documentation
        String admissionDateFilter = "ge" + dischargeDate.minusDays(14).toString() +
                                    "&date=le" + dischargeDate.toString();
        JsonNode admissionRecords = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_ADMISSION_CODES), admissionDateFilter);
        boolean hasAdmissionNotification = !getEntries(admissionRecords).isEmpty();

        // Component 2: Receipt of discharge information (documented discharge summary)
        // In real implementation, this would check for discharge summary document
        // For now, we assume discharge encounter implies discharge information
        boolean hasDischargeInformation = true; // Discharge record exists

        // Component 3: Patient engagement (follow-up visit within 30 days)
        String followUpDateFilter = "ge" + dischargeDate.toString() +
                                   "&date=le" + followUpWindowEnd.toString();
        JsonNode followUpVisits = getEncounters(tenantId, patientId,
            String.join(",", POST_DISCHARGE_VISIT_CODES), followUpDateFilter);
        boolean hasPatientEngagement = !getEntries(followUpVisits).isEmpty();
        String followUpVisitDate = hasPatientEngagement ?
            getEffectiveDate(getEntries(followUpVisits).get(0)) : null;

        // Component 4: Medication reconciliation (within 30 days)
        JsonNode medReconciliation = getEncounters(tenantId, patientId,
            String.join(",", MEDICATION_RECONCILIATION_CODES), followUpDateFilter);
        JsonNode transitionalCare = getEncounters(tenantId, patientId,
            String.join(",", TRANSITIONAL_CARE_CODES), followUpDateFilter);
        boolean hasMedicationReconciliation = !getEntries(medReconciliation).isEmpty() ||
                                             !getEntries(transitionalCare).isEmpty();

        // Calculate compliance (all four components)
        int componentsCompleted = 0;
        if (hasAdmissionNotification) componentsCompleted++;
        if (hasDischargeInformation) componentsCompleted++;
        if (hasPatientEngagement) componentsCompleted++;
        if (hasMedicationReconciliation) componentsCompleted++;

        double complianceRate = componentsCompleted / 4.0;

        // For numerator: need patient engagement (critical component)
        boolean meetsTransitionGoal = hasPatientEngagement;

        resultBuilder.inNumerator(meetsTransitionGoal);
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasPatientEngagement) {
            boolean isOverdue = LocalDate.now().isAfter(followUpWindowEnd);
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_POST_DISCHARGE_FOLLOWUP")
                .description(String.format("No follow-up visit within 30 days of hospital discharge (%s)", dischargeDateStr))
                .recommendedAction("Schedule post-discharge follow-up visit within 30 days - critical for preventing readmissions")
                .priority("high")
                .dueDate(isOverdue ? LocalDate.now().plusDays(3) : followUpWindowEnd)
                .build());
        }

        if (!hasMedicationReconciliation) {
            boolean isOverdue = LocalDate.now().isAfter(followUpWindowEnd);
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_POST_DISCHARGE_MED_RECONCILIATION")
                .description("No medication reconciliation documented after hospital discharge")
                .recommendedAction("Perform medication reconciliation at follow-up visit - verify all medications and resolve discrepancies")
                .priority("high")
                .dueDate(isOverdue ? LocalDate.now().plusWeeks(1) : followUpWindowEnd)
                .build());
        }

        if (!hasAdmissionNotification) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_ADMISSION_NOTIFICATION")
                .description("No documentation of provider notification for inpatient admission")
                .recommendedAction("Implement admission notification system to primary care provider")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "dischargeDate", dischargeDateStr,
            "followUpWindowEnd", followUpWindowEnd.toString(),
            "hasAdmissionNotification", hasAdmissionNotification,
            "hasDischargeInformation", hasDischargeInformation,
            "hasPatientEngagement", hasPatientEngagement,
            "followUpVisitDate", followUpVisitDate != null ? followUpVisitDate : "None",
            "hasMedicationReconciliation", hasMedicationReconciliation,
            "componentsCompleted", String.format("%d of 4", componentsCompleted)
        ));

        resultBuilder.evidence(java.util.Map.of(
            "admissionNotified", hasAdmissionNotification,
            "dischargeInformationProvided", hasDischargeInformation,
            "patientEngaged", hasPatientEngagement,
            "medicationReconciled", hasMedicationReconciliation,
            "transitionComplete", meetsTransitionGoal
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 18) {
            return false;
        }

        // Must have hospital discharge in last 60 days (allows 30-day follow-up window)
        String dischargeDateFilter = "ge" + LocalDate.now().minusDays(60).toString();
        JsonNode dischargeEncounters = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_DISCHARGE_CODES), dischargeDateFilter);

        return !getEntries(dischargeEncounters).isEmpty();
    }
}
