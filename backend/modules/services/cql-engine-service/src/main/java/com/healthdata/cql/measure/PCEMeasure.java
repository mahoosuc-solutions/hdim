package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * PCE - Pharmacotherapy for Opioid Use Disorder (HEDIS)
 *
 * Evaluates whether patients with opioid use disorder (OUD) received
 * medication-assisted treatment (MAT) with FDA-approved medications.
 *
 * Three medications approved for OUD:
 * - Buprenorphine (Suboxone, Subutex, Sublocade)
 * - Methadone (for OUD treatment in certified programs)
 * - Naltrexone (Vivitrol injection or oral)
 *
 * Target: Continuous therapy for at least 180 days (6 months)
 */
@Component
public class PCEMeasure extends AbstractHedisMeasure {

    private static final List<String> OPIOID_USE_DISORDER_CODES = Arrays.asList(
        "231470003", // Opioid dependence (SNOMED)
        "191816009", // Opioid abuse (SNOMED)
        "191905009", // Opioid addiction (SNOMED)
        "398068008", // Heroin dependence (SNOMED)
        "703842006", // Opioid use disorder (SNOMED)
        "361055000"  // Opioid poisoning (SNOMED) - includes overdose events
    );

    private static final List<String> BUPRENORPHINE_CODES = Arrays.asList(
        "1819",      // RxNorm - Buprenorphine
        "351265",    // RxNorm - Buprenorphine / Naloxone (Suboxone)
        "1010600",   // RxNorm - Buprenorphine / Naloxone sublingual film
        "1797886",   // RxNorm - Buprenorphine / Naloxone buccal film
        "1666777",   // RxNorm - Buprenorphine extended-release injection (Sublocade)
        "351266",    // RxNorm - Buprenorphine hydrochloride (Subutex)
        "1010603"    // RxNorm - Buprenorphine / Naloxone sublingual tablet
    );

    private static final List<String> METHADONE_CODES = Arrays.asList(
        "7804",      // RxNorm - Methadone
        "864706",    // RxNorm - Methadone hydrochloride
        "993781",    // RxNorm - Methadone oral solution
        "993770",    // RxNorm - Methadone tablet
        "864718"     // RxNorm - Methadone hydrochloride oral solution
    );

    private static final List<String> NALTREXONE_CODES = Arrays.asList(
        "7243",      // RxNorm - Naltrexone
        "1311288",   // RxNorm - Naltrexone extended-release injectable suspension (Vivitrol)
        "1311287",   // RxNorm - Naltrexone hydrochloride
        "835603",    // RxNorm - Naltrexone oral tablet
        "1311291"    // RxNorm - Naltrexone extended-release injection
    );

    private static final List<String> OVERDOSE_CODES = Arrays.asList(
        "55680006",  // SNOMED - Drug overdose
        "292935008", // SNOMED - Opioid overdose
        "242926005", // SNOMED - Heroin overdose
        "86849004"   // SNOMED - Opioid poisoning
    );

    private static final int CONTINUOUS_TREATMENT_DAYS = 180; // 6 months

    @Override
    public String getMeasureId() {
        return "PCE";
    }

    @Override
    public String getMeasureName() {
        return "Pharmacotherapy for Opioid Use Disorder";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'PCE-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating PCE measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18+ with opioid use disorder)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for MAT medications in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        // Check for buprenorphine
        JsonNode buprenorphineMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", BUPRENORPHINE_CODES), dateFilter);
        List<JsonNode> buprenorphineEntries = getEntries(buprenorphineMeds);

        // Check for methadone
        JsonNode methadoneMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", METHADONE_CODES), dateFilter);
        List<JsonNode> methadoneEntries = getEntries(methadoneMeds);

        // Check for naltrexone
        JsonNode naltrexoneMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", NALTREXONE_CODES), dateFilter);
        List<JsonNode> naltrexoneEntries = getEntries(naltrexoneMeds);

        // Combine all MAT medications
        int totalMATFills = buprenorphineEntries.size() + methadoneEntries.size() + naltrexoneEntries.size();

        boolean hasMAT = totalMATFills > 0;

        // Determine primary MAT medication
        String matMedication = "None";
        List<JsonNode> primaryMATEntries = null;

        if (!buprenorphineEntries.isEmpty()) {
            matMedication = "Buprenorphine";
            primaryMATEntries = buprenorphineEntries;
        } else if (!methadoneEntries.isEmpty()) {
            matMedication = "Methadone";
            primaryMATEntries = methadoneEntries;
        } else if (!naltrexoneEntries.isEmpty()) {
            matMedication = "Naltrexone";
            primaryMATEntries = naltrexoneEntries;
        }

        // Calculate treatment persistence if on MAT
        Double treatmentPersistence = null;
        int daysCovered = 0;

        if (hasMAT && primaryMATEntries != null) {
            // Calculate PDC for primary medication
            LocalDate startDate = LocalDate.now().minusMonths(12);
            LocalDate endDate = LocalDate.now();

            treatmentPersistence = calculatePDC(primaryMATEntries, startDate, endDate);
            daysCovered = (int) (treatmentPersistence * 365);
        }

        // Check for overdose events (high-risk indicator)
        JsonNode overdoseEvents = getConditions(tenantId, patientId,
            String.join(",", OVERDOSE_CODES));
        boolean hasOverdoseHistory = !getEntries(overdoseEvents).isEmpty();

        // For numerator: need MAT with ≥180 days persistence
        boolean meetsTreatmentGoal = hasMAT && treatmentPersistence != null &&
            (treatmentPersistence * 365) >= CONTINUOUS_TREATMENT_DAYS;

        resultBuilder.inNumerator(meetsTreatmentGoal);
        resultBuilder.complianceRate(hasMAT && treatmentPersistence != null ? treatmentPersistence : 0.0);
        resultBuilder.score(hasMAT && treatmentPersistence != null ? treatmentPersistence * 100 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasMAT) {
            String priority = hasOverdoseHistory ? "high" : "high"; // Always high priority for OUD
            String description = hasOverdoseHistory ?
                "No MAT for opioid use disorder - patient has overdose history (very high risk)" :
                "No medication-assisted treatment for opioid use disorder";

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("NO_OUD_PHARMACOTHERAPY")
                .description(description)
                .recommendedAction("Initiate MAT (buprenorphine, methadone, or naltrexone) - MAT reduces overdose death by 50%")
                .priority(priority)
                .dueDate(LocalDate.now().plusDays(7))
                .build());

            // If overdose history, add urgent recommendation
            if (hasOverdoseHistory) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("HIGH_RISK_OUD_NO_TREATMENT")
                    .description("Overdose history without MAT - extremely high risk for fatal overdose")
                    .recommendedAction("URGENT: Initiate buprenorphine or refer to MAT program immediately; prescribe naloxone")
                    .priority("high")
                    .dueDate(LocalDate.now().plusDays(2))
                    .build());
            }
        } else if (!meetsTreatmentGoal) {
            // On MAT but insufficient persistence
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INSUFFICIENT_OUD_TREATMENT_PERSISTENCE")
                .description(String.format("MAT persistence insufficient (%d days covered, need ≥180 days continuous)", daysCovered))
                .recommendedAction("Address barriers to MAT adherence; consider long-acting formulations (Sublocade, Vivitrol)")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }

        // Always recommend naloxone for overdose reversal
        careGaps.add(MeasureResult.CareGap.builder()
            .gapType("NALOXONE_COPRESCRIPTION")
            .description("Naloxone (Narcan) should be co-prescribed for all patients with OUD")
            .recommendedAction("Prescribe naloxone nasal spray or auto-injector for overdose reversal; educate patient/family")
            .priority("medium")
            .dueDate(LocalDate.now().plusMonths(1))
            .build());

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "hasMAT", hasMAT,
            "matMedication", matMedication,
            "totalMATFills", totalMATFills,
            "treatmentPersistence", treatmentPersistence != null ? String.format("%.1f%%", treatmentPersistence * 100) : "N/A",
            "daysCovered", daysCovered,
            "meetsTreatmentGoal", meetsTreatmentGoal,
            "hasOverdoseHistory", hasOverdoseHistory
        ));

        resultBuilder.evidence(java.util.Map.of(
            "matInitiated", hasMAT,
            "matType", matMedication,
            "sufficientPersistence", meetsTreatmentGoal,
            "overdoseRisk", hasOverdoseHistory
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

        // Must have opioid use disorder diagnosis
        JsonNode oudConditions = getConditions(tenantId, patientId,
            String.join(",", OPIOID_USE_DISORDER_CODES));

        return !getEntries(oudConditions).isEmpty();
    }

    /**
     * Calculate Proportion of Days Covered (PDC) for MAT medication
     */
    private double calculatePDC(List<JsonNode> medicationEntries, LocalDate startDate, LocalDate endDate) {
        long totalDaysInWindow = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        boolean[] coveredDays = new boolean[(int) totalDaysInWindow];

        for (JsonNode medication : medicationEntries) {
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

            // For long-acting injectables (Sublocade, Vivitrol), use extended duration
            try {
                if (medication.has("medicationCodeableConcept")) {
                    JsonNode medCode = medication.get("medicationCodeableConcept");
                    String display = medCode.has("text") ? medCode.get("text").asText().toLowerCase() : "";
                    if (display.contains("sublocade") || display.contains("vivitrol")) {
                        daysSupply = 28; // Monthly injection
                    }
                }
            } catch (Exception e) {
                // Continue with default days supply
            }

            // Mark covered days
            LocalDate medicationEndDate = dispenseDate.plusDays(daysSupply);
            LocalDate currentDate = dispenseDate;

            while (currentDate.isBefore(medicationEndDate) && currentDate.isBefore(endDate)) {
                if (!currentDate.isBefore(startDate)) {
                    long dayIndex = java.time.temporal.ChronoUnit.DAYS.between(startDate, currentDate);
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

        return totalDaysInWindow > 0 ? (double) daysCovered / totalDaysInWindow : 0.0;
    }
}
