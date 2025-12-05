package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * PCR - Plan All-Cause Readmissions (HEDIS)
 *
 * Evaluates unplanned hospital readmissions within 30 days of discharge.
 * This is an inverse measure - LOWER readmission rates are better.
 *
 * Tracks:
 * - Total discharges
 * - 30-day readmissions
 * - Readmission rate (lower is better)
 */
@Component
public class PCRMeasure extends AbstractHedisMeasure {

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

    private static final List<String> PLANNED_ADMISSION_CODES = Arrays.asList(
        "305410009", // Admission to hospital planned (SNOMED)
        "305411008", // Elective admission to hospital (SNOMED)
        "371883000"  // Outpatient surgery (SNOMED)
    );

    @Override
    public String getMeasureId() {
        return "PCR";
    }

    @Override
    public String getMeasureName() {
        return "Plan All-Cause Readmissions";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'PCR-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating PCR measure for patient: {}", patientId);

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

        // Find hospital discharges in last 60 days (allows 30-day follow-up window)
        String dischargeDateFilter = "ge" + LocalDate.now().minusDays(60).toString();
        JsonNode dischargeEncounters = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_DISCHARGE_CODES), dischargeDateFilter);
        List<JsonNode> discharges = getEntries(dischargeEncounters);

        if (discharges.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No hospital discharges found in last 60 days")
                .build();
        }

        // Get most recent discharge
        JsonNode mostRecentDischarge = discharges.get(0);
        String dischargeDateStr = getEffectiveDate(mostRecentDischarge);
        LocalDate dischargeDate = LocalDate.parse(dischargeDateStr);

        // Calculate 30-day readmission window
        LocalDate readmissionWindowEnd = dischargeDate.plusDays(30);

        // Check for admissions within 30 days of discharge
        String readmissionDateFilter = "ge" + dischargeDate.plusDays(1).toString() +
                                      "&date=le" + readmissionWindowEnd.toString();
        JsonNode readmissions = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_ADMISSION_CODES), readmissionDateFilter);
        List<JsonNode> readmissionEntries = getEntries(readmissions);

        // Check if any readmissions were planned (these should be excluded)
        JsonNode plannedAdmissions = getEncounters(tenantId, patientId,
            String.join(",", PLANNED_ADMISSION_CODES), readmissionDateFilter);
        int plannedAdmissionCount = getEntries(plannedAdmissions).size();

        // Calculate unplanned readmissions
        int totalReadmissions = readmissionEntries.size();
        int unplannedReadmissions = Math.max(0, totalReadmissions - plannedAdmissionCount);

        boolean hasUnplannedReadmission = unplannedReadmissions > 0;

        // For PCR, being IN numerator means HAD a readmission (inverse measure - lower is better)
        resultBuilder.inNumerator(hasUnplannedReadmission);

        // Calculate readmission rate per discharge
        double readmissionRate = hasUnplannedReadmission ? 1.0 : 0.0;
        resultBuilder.complianceRate(1.0 - readmissionRate); // Inverse: compliance = NOT readmitted
        resultBuilder.score((1.0 - readmissionRate) * 100);  // Higher score = better (no readmission)

        if (hasUnplannedReadmission) {
            // Readmission occurred - identify care gap
            JsonNode firstReadmission = readmissionEntries.get(0);
            String readmissionDateStr = getEffectiveDate(firstReadmission);
            LocalDate readmissionDate = LocalDate.parse(readmissionDateStr);
            long daysToReadmission = java.time.temporal.ChronoUnit.DAYS.between(dischargeDate, readmissionDate);

            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("UNPLANNED_READMISSION")
                    .description(String.format("Unplanned hospital readmission %d days after discharge (%s)",
                        daysToReadmission, dischargeDateStr))
                    .recommendedAction("Review discharge planning, medication reconciliation, and transitional care processes")
                    .priority("high")
                    .dueDate(LocalDate.now().plusWeeks(1))
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "dischargeDate", dischargeDateStr,
                "readmissionDate", readmissionDateStr,
                "daysToReadmission", daysToReadmission,
                "hasUnplannedReadmission", true,
                "unplannedReadmissions", unplannedReadmissions,
                "plannedAdmissions", plannedAdmissionCount
            ));

            resultBuilder.evidence(java.util.Map.of(
                "totalReadmissions", totalReadmissions,
                "unplannedReadmissions", unplannedReadmissions,
                "firstReadmissionDate", readmissionDateStr
            ));
        } else {
            // No readmission - good outcome
            resultBuilder.details(java.util.Map.of(
                "dischargeDate", dischargeDateStr,
                "hasUnplannedReadmission", false,
                "readmissionWindowEnd", readmissionWindowEnd.toString(),
                "plannedAdmissions", plannedAdmissionCount
            ));

            resultBuilder.evidence(java.util.Map.of(
                "totalReadmissions", totalReadmissions,
                "unplannedReadmissions", 0,
                "noReadmission", true
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

        // Must have hospital discharge in last 60 days (to allow 30-day follow-up)
        String dischargeDateFilter = "ge" + LocalDate.now().minusDays(60).toString();
        JsonNode dischargeEncounters = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_DISCHARGE_CODES), dischargeDateFilter);

        return !getEntries(dischargeEncounters).isEmpty();
    }
}
