package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * PBH - Persistence of Beta-Blocker Treatment After a Heart Attack (HEDIS)
 *
 * Evaluates whether patients who had acute myocardial infarction (AMI) remain on
 * beta-blocker therapy for at least 6 months post-discharge.
 *
 * Beta-blockers reduce mortality and recurrent MI in post-MI patients.
 * Target: Continuous therapy for ≥180 days (6 months)
 */
@Component
public class PBHMeasure extends AbstractHedisMeasure {

    private static final List<String> ACUTE_MI_CODES = Arrays.asList(
        "22298006",  // Acute myocardial infarction (SNOMED)
        "57054005",  // Acute myocardial infarction (SNOMED)
        "70422006",  // Acute subendocardial infarction (SNOMED)
        "233838001", // Acute anterior myocardial infarction (SNOMED)
        "233843008", // Acute inferior myocardial infarction (SNOMED)
        "233845001", // Acute lateral myocardial infarction (SNOMED)
        "233846000", // Acute posterior myocardial infarction (SNOMED)
        "401303003"  // Acute ST segment elevation myocardial infarction (SNOMED)
    );

    private static final List<String> INPATIENT_ADMISSION_CODES = Arrays.asList(
        "32485007",  // Hospital admission (SNOMED)
        "8715000",   // Inpatient admission (SNOMED)
        "183452005", // Emergency hospital admission (SNOMED)
        "305351004"  // Admission to hospital (SNOMED)
    );

    private static final List<String> INPATIENT_DISCHARGE_CODES = Arrays.asList(
        "58000006",  // Hospital discharge (SNOMED)
        "306689006", // Discharge from hospital (SNOMED)
        "306685001"  // Discharge to home (SNOMED)
    );

    private static final List<String> BETA_BLOCKER_CODES = Arrays.asList(
        // Cardioselective Beta-Blockers
        "3616",      // RxNorm - Atenolol
        "6918",      // RxNorm - Metoprolol
        "49276",     // RxNorm - Metoprolol succinate
        "6185",      // RxNorm - Bisoprolol
        "25789",     // RxNorm - Acebutolol
        "31555",     // RxNorm - Betaxolol
        "42331",     // RxNorm - Nebivolol
        // Non-selective Beta-Blockers
        "8787",      // RxNorm - Propranolol
        "7226",      // RxNorm - Nadolol
        "32672",     // RxNorm - Timolol
        "20352",     // RxNorm - Pindolol
        "47898",     // RxNorm - Sotalol
        // Alpha+Beta Blockers
        "3827",      // RxNorm - Carvedilol
        "17128"      // RxNorm - Labetalol
    );

    private static final int PERSISTENCE_DAYS_REQUIRED = 180; // 6 months

    @Override
    public String getMeasureId() {
        return "PBH";
    }

    @Override
    public String getMeasureName() {
        return "Persistence of Beta-Blocker Treatment After Heart Attack";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'PBH-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating PBH measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18+ with AMI hospital discharge in last 12 months)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find AMI discharge date (index event)
        String dischargeDateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode dischargeEncounters = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_DISCHARGE_CODES), dischargeDateFilter);
        List<JsonNode> discharges = getEntries(dischargeEncounters);

        if (discharges.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No hospital discharge found in last 12 months")
                .build();
        }

        // Get discharge date (index discharge)
        JsonNode mostRecentDischarge = discharges.get(0);
        String dischargeDateStr = getEffectiveDate(mostRecentDischarge);
        LocalDate dischargeDate = LocalDate.parse(dischargeDateStr);

        // Calculate 180-day persistence window
        LocalDate persistenceWindowEnd = dischargeDate.plusDays(PERSISTENCE_DAYS_REQUIRED);

        // Check if enough time has passed for evaluation (need at least 180 days post-discharge)
        if (LocalDate.now().isBefore(persistenceWindowEnd)) {
            long daysElapsed = ChronoUnit.DAYS.between(dischargeDate, LocalDate.now());
            return resultBuilder
                .inDenominator(true)
                .inNumerator(false)
                .exclusionReason(String.format("Not enough time elapsed for 180-day persistence evaluation (%d days elapsed)", daysElapsed))
                .build();
        }

        // Get all beta-blocker fills in the 180-day window
        String medDateFilter = "ge" + dischargeDate.toString() + "&date=le" + persistenceWindowEnd.toString();
        JsonNode betaBlockers = getMedicationRequests(tenantId, patientId,
            String.join(",", BETA_BLOCKER_CODES), medDateFilter);
        List<JsonNode> betaBlockerEntries = getEntries(betaBlockers);

        if (betaBlockerEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(true)
                .inNumerator(false)
                .careGaps(List.of(
                    MeasureResult.CareGap.builder()
                        .gapType("NO_BETA_BLOCKER_POST_MI")
                        .description(String.format("No beta-blocker therapy documented after MI (discharge %s)", dischargeDateStr))
                        .recommendedAction("Initiate beta-blocker therapy - proven to reduce mortality and recurrent MI")
                        .priority("high")
                        .dueDate(LocalDate.now().plusWeeks(1))
                        .build()
                ))
                .details(java.util.Map.of(
                    "dischargeDate", dischargeDateStr,
                    "persistenceWindowEnd", persistenceWindowEnd.toString(),
                    "hasBetaBlockerTherapy", false
                ))
                .build();
        }

        // Calculate persistence (Proportion of Days Covered - PDC)
        AdherenceData adherenceData = calculatePDC(betaBlockerEntries, dischargeDate, persistenceWindowEnd);
        double pdc = adherenceData.pdc;
        int daysCovered = adherenceData.daysCovered;

        // For this measure, typically require ≥80% PDC
        boolean meetsPersistenceThreshold = pdc >= 0.80;

        resultBuilder.inNumerator(meetsPersistenceThreshold);
        resultBuilder.complianceRate(pdc);
        resultBuilder.score(pdc * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!meetsPersistenceThreshold) {
            String priority = pdc < 0.50 ? "high" : "medium";
            String gapDescription = String.format(
                "Low persistence with beta-blocker therapy post-MI (%.1f%% of days covered, target ≥80%%)",
                pdc * 100
            );

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("LOW_BETA_BLOCKER_PERSISTENCE")
                .description(gapDescription)
                .recommendedAction("Address adherence barriers; emphasize importance of continuous beta-blocker therapy post-MI")
                .priority(priority)
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());

            // If very poor adherence, suggest intervention
            if (pdc < 0.50) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("CRITICAL_BETA_BLOCKER_NONADHERENCE")
                    .description("Critical non-adherence to beta-blocker increases risk of recurrent MI and death")
                    .recommendedAction("Urgent cardiology follow-up; consider medication reconciliation and adherence counseling")
                    .priority("high")
                    .dueDate(LocalDate.now().plusDays(7))
                    .build());
            }
        }

        resultBuilder.careGaps(careGaps);

        // Persistence category
        String persistenceCategory;
        if (pdc >= 0.80) {
            persistenceCategory = "Good persistence (≥80%)";
        } else if (pdc >= 0.60) {
            persistenceCategory = "Moderate persistence (60-79%)";
        } else if (pdc >= 0.40) {
            persistenceCategory = "Poor persistence (40-59%)";
        } else {
            persistenceCategory = "Very poor persistence (<40%)";
        }

        resultBuilder.details(java.util.Map.of(
            "dischargeDate", dischargeDateStr,
            "persistenceWindowEnd", persistenceWindowEnd.toString(),
            "pdc", String.format("%.1f%%", pdc * 100),
            "persistenceCategory", persistenceCategory,
            "daysCovered", daysCovered,
            "daysInWindow", PERSISTENCE_DAYS_REQUIRED,
            "totalFills", adherenceData.fillCount,
            "meetsPersistenceThreshold", meetsPersistenceThreshold
        ));

        resultBuilder.evidence(java.util.Map.of(
            "pdcPercentage", pdc * 100,
            "meetsPersistenceThreshold", meetsPersistenceThreshold,
            "totalBetaBlockerFills", adherenceData.fillCount
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

        // Must have AMI diagnosis
        JsonNode amiConditions = getConditions(tenantId, patientId,
            String.join(",", ACUTE_MI_CODES));
        if (getEntries(amiConditions).isEmpty()) {
            return false;
        }

        // Must have hospital discharge in last 12 months
        String dischargeDateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode dischargeEncounters = getEncounters(tenantId, patientId,
            String.join(",", INPATIENT_DISCHARGE_CODES), dischargeDateFilter);

        return !getEntries(dischargeEncounters).isEmpty();
    }

    /**
     * Calculate Proportion of Days Covered (PDC) for beta-blocker therapy
     */
    private AdherenceData calculatePDC(List<JsonNode> medicationEntries, LocalDate startDate, LocalDate endDate) {
        long totalDaysInWindow = ChronoUnit.DAYS.between(startDate, endDate);
        boolean[] coveredDays = new boolean[(int) totalDaysInWindow];
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
            LocalDate medicationEndDate = dispenseDate.plusDays(daysSupply);
            LocalDate currentDate = dispenseDate;

            while (currentDate.isBefore(medicationEndDate) && currentDate.isBefore(endDate)) {
                if (!currentDate.isBefore(startDate)) {
                    long dayIndex = ChronoUnit.DAYS.between(startDate, currentDate);
                    if (dayIndex >= 0 && dayIndex < coveredDays.length) {
                        coveredDays[(int) dayIndex] = true;
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
        }

        // Count covered days
        int daysCovered = 0;
        for (boolean covered : coveredDays) {
            if (covered) daysCovered++;
        }

        double pdc = totalDaysInWindow > 0 ? (double) daysCovered / totalDaysInWindow : 0.0;

        return new AdherenceData(pdc, daysCovered, (int) totalDaysInWindow, fillCount);
    }

    /**
     * Helper class to hold adherence calculation results
     */
    private static class AdherenceData {
        final double pdc;
        final int daysCovered;
        final int daysInWindow;
        final int fillCount;

        AdherenceData(double pdc, int daysCovered, int daysInWindow, int fillCount) {
            this.pdc = pdc;
            this.daysCovered = daysCovered;
            this.daysInWindow = daysInWindow;
            this.fillCount = fillCount;
        }
    }
}
