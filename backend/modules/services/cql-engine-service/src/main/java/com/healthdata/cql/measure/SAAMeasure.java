package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * SAA - Adherence to Antipsychotic Medications for Individuals with Schizophrenia (HEDIS)
 *
 * Evaluates medication adherence using Proportion of Days Covered (PDC) methodology.
 * Target: PDC ≥80% for at least 12 months of continuous treatment.
 *
 * PDC = Days Covered / Days in Treatment Period
 */
@Component
public class SAAMeasure extends AbstractHedisMeasure {

    private static final List<String> SCHIZOPHRENIA_CODES = Arrays.asList(
        "16990005",  // Schizophrenia (SNOMED)
        "58214004",  // Schizophreniform disorder (SNOMED)
        "26025008",  // Schizoaffective disorder (SNOMED)
        "191618008"  // Psychotic disorder (SNOMED)
    );

    private static final List<String> ANTIPSYCHOTIC_MEDICATION_CODES = Arrays.asList(
        // Typical Antipsychotics
        "3033",      // RxNorm - Chlorpromazine
        "4091",      // RxNorm - Fluphenazine
        "5093",      // RxNorm - Haloperidol
        "8076",      // RxNorm - Perphenazine
        "11046",     // RxNorm - Thioridazine
        "11174",     // RxNorm - Thiothixene
        "11289",     // RxNorm - Trifluoperazine
        // Atypical Antipsychotics
        "46303",     // RxNorm - Aripiprazole
        "115698",    // RxNorm - Asenapine
        "89013",     // RxNorm - Clozapine
        "61381",     // RxNorm - Iloperidone
        "142439",    // RxNorm - Lurasidone
        "41996",     // RxNorm - Olanzapine
        "784649",    // RxNorm - Paliperidone
        "35636",     // RxNorm - Quetiapine
        "35827",     // RxNorm - Risperidone
        "258337",    // RxNorm - Ziprasidone
        "1040028"    // RxNorm - Brexpiprazole
    );

    private static final double ADHERENCE_THRESHOLD = 0.80; // 80% PDC

    @Override
    public String getMeasureId() {
        return "SAA";
    }

    @Override
    public String getMeasureName() {
        return "Adherence to Antipsychotic Medications";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'SAA-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating SAA measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 19-64 with schizophrenia and on antipsychotic medication)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Get all antipsychotic medication fills in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode medications = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIPSYCHOTIC_MEDICATION_CODES), dateFilter);
        List<JsonNode> medicationEntries = getEntries(medications);

        if (medicationEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No antipsychotic medication fills found in measurement period")
                .build();
        }

        // Calculate PDC (Proportion of Days Covered)
        AdherenceData adherenceData = calculatePDC(medicationEntries);
        double pdc = adherenceData.pdc;
        boolean meetsThreshold = pdc >= ADHERENCE_THRESHOLD;

        resultBuilder.inNumerator(meetsThreshold);
        resultBuilder.complianceRate(pdc);
        resultBuilder.score(pdc * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!meetsThreshold) {
            String priority = pdc < 0.50 ? "high" : "medium";
            String gapDescription = String.format(
                "Low adherence to antipsychotic medication (%.1f%% coverage, target ≥80%%)",
                pdc * 100
            );

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("LOW_ANTIPSYCHOTIC_ADHERENCE")
                .description(gapDescription)
                .recommendedAction("Address adherence barriers: medication side effects, cost, understanding of treatment")
                .priority(priority)
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());

            // Add specific recommendations based on gap size
            if (pdc < 0.50) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("CRITICAL_MEDICATION_NONADHERENCE")
                    .description("Critical non-adherence (<50% coverage) increases relapse risk")
                    .recommendedAction("Urgent psychiatric follow-up; consider long-acting injectable (LAI) antipsychotic")
                    .priority("high")
                    .dueDate(LocalDate.now().plusDays(7))
                    .build());
            }
        }
        resultBuilder.careGaps(careGaps);

        // Adherence category
        String adherenceCategory;
        if (pdc >= 0.80) {
            adherenceCategory = "Good adherence (≥80%)";
        } else if (pdc >= 0.60) {
            adherenceCategory = "Moderate adherence (60-79%)";
        } else if (pdc >= 0.40) {
            adherenceCategory = "Poor adherence (40-59%)";
        } else {
            adherenceCategory = "Very poor adherence (<40%)";
        }

        resultBuilder.details(java.util.Map.of(
            "pdc", String.format("%.1f%%", pdc * 100),
            "adherenceCategory", adherenceCategory,
            "daysCovered", adherenceData.daysCovered,
            "daysInPeriod", adherenceData.daysInPeriod,
            "totalFills", adherenceData.fillCount,
            "meetsThreshold", meetsThreshold
        ));

        resultBuilder.evidence(java.util.Map.of(
            "pdcPercentage", pdc * 100,
            "meetsAdherenceThreshold", meetsThreshold,
            "totalMedicationFills", adherenceData.fillCount
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 19 || age > 64) {
            return false;
        }

        // Must have schizophrenia diagnosis
        JsonNode schizophreniaConditions = getConditions(tenantId, patientId,
            String.join(",", SCHIZOPHRENIA_CODES));
        if (getEntries(schizophreniaConditions).isEmpty()) {
            return false;
        }

        // Must have at least 2 antipsychotic fills in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode medications = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIPSYCHOTIC_MEDICATION_CODES), dateFilter);
        List<JsonNode> medicationEntries = getEntries(medications);

        return medicationEntries.size() >= 2;
    }

    /**
     * Calculate Proportion of Days Covered (PDC)
     * PDC = Number of days covered / Total days in measurement period
     */
    private AdherenceData calculatePDC(List<JsonNode> medicationEntries) {
        LocalDate measurementStart = LocalDate.now().minusMonths(12);
        LocalDate measurementEnd = LocalDate.now();
        long totalDaysInPeriod = ChronoUnit.DAYS.between(measurementStart, measurementEnd);

        // Track covered days (accounting for overlapping fills)
        boolean[] coveredDays = new boolean[(int) totalDaysInPeriod];
        int fillCount = 0;

        for (JsonNode medication : medicationEntries) {
            fillCount++;

            // Get dispense date
            LocalDate dispenseDate = LocalDate.parse(getEffectiveDate(medication));

            // Get days supply (default to 30 if not specified)
            int daysSupply = 30;
            try {
                if (medication.has("dispenseRequest")) {
                    JsonNode dispenseRequest = medication.get("dispenseRequest");
                    if (dispenseRequest.has("expectedSupplyDuration")) {
                        JsonNode duration = dispenseRequest.get("expectedSupplyDuration");
                        if (duration.has("value")) {
                            daysSupply = duration.get("value").asInt();
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not extract days supply, using default 30: {}", e.getMessage());
            }

            // Mark covered days
            LocalDate endDate = dispenseDate.plusDays(daysSupply);
            LocalDate currentDate = dispenseDate;

            while (currentDate.isBefore(endDate) && currentDate.isBefore(measurementEnd)) {
                if (!currentDate.isBefore(measurementStart)) {
                    long dayIndex = ChronoUnit.DAYS.between(measurementStart, currentDate);
                    if (dayIndex >= 0 && dayIndex < coveredDays.length) {
                        coveredDays[(int) dayIndex] = true;
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
        }

        // Count total covered days
        int daysCovered = 0;
        for (boolean covered : coveredDays) {
            if (covered) daysCovered++;
        }

        double pdc = totalDaysInPeriod > 0 ? (double) daysCovered / totalDaysInPeriod : 0.0;

        return new AdherenceData(pdc, daysCovered, (int) totalDaysInPeriod, fillCount);
    }

    /**
     * Helper class to hold adherence calculation results
     */
    private static class AdherenceData {
        final double pdc;
        final int daysCovered;
        final int daysInPeriod;
        final int fillCount;

        AdherenceData(double pdc, int daysCovered, int daysInPeriod, int fillCount) {
            this.pdc = pdc;
            this.daysCovered = daysCovered;
            this.daysInPeriod = daysInPeriod;
            this.fillCount = fillCount;
        }
    }
}
