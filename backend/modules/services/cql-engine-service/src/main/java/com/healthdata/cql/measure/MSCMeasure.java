package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * MSC - Medical Assistance with Smoking and Tobacco Use Cessation (HEDIS)
 *
 * Evaluates whether patients identified as tobacco users received cessation interventions.
 * Three components:
 * - Tobacco use screening and identification
 * - Cessation counseling or intervention
 * - Pharmacotherapy (cessation medications)
 *
 * Multiple rates:
 * - Advising smokers to quit
 * - Discussing cessation medications
 * - Discussing cessation strategies
 */
@Component
public class MSCMeasure extends AbstractHedisMeasure {

    private static final List<String> TOBACCO_USE_SCREENING_CODES = Arrays.asList(
        "72166-2",   // LOINC - Tobacco smoking status NHIS
        "68535-4",   // LOINC - Have you used tobacco in the last 30 days
        "68536-2",   // LOINC - Tobacco use and exposure assessment
        "11367-0",   // LOINC - History of tobacco use
        "39240-7"    // LOINC - Tobacco use status
    );

    private static final List<String> TOBACCO_USER_CODES = Arrays.asList(
        "77176-6",   // Current every day smoker
        "428041000124106", // SNOMED - Current tobacco smoker
        "230057008", // SNOMED - Cigarette smoker
        "428061000124105", // SNOMED - Current light tobacco smoker
        "428071000124103"  // SNOMED - Current heavy tobacco smoker
    );

    private static final List<String> CESSATION_COUNSELING_CODES = Arrays.asList(
        "225323000", // SNOMED - Smoking cessation education
        "710081004", // SNOMED - Smoking cessation therapy
        "171055003", // SNOMED - Tobacco cessation counseling
        "225324006", // SNOMED - Stop smoking monitoring
        "385890007", // SNOMED - Smoking cessation education, guidance and counseling
        "702388001", // SNOMED - Tobacco use treatment counseling
        "428081000124100" // SNOMED - Tobacco use treatment plan
    );

    private static final List<String> CESSATION_MEDICATION_CODES = Arrays.asList(
        // Nicotine Replacement Therapy
        "7313",      // RxNorm - Nicotine patch
        "314119",    // RxNorm - Nicotine gum
        "198029",    // RxNorm - Nicotine lozenge
        "250524",    // RxNorm - Nicotine nasal spray
        "198031",    // RxNorm - Nicotine oral inhaler
        // Prescription Medications
        "42347",     // RxNorm - Varenicline (Chantix)
        "4493",      // RxNorm - Bupropion (Zyban/Wellbutrin)
        "993503",    // RxNorm - Bupropion SR for smoking cessation
        "993518"     // RxNorm - Bupropion XL
    );

    private static final List<String> OUTPATIENT_VISIT_CODES = Arrays.asList(
        "185349003", // SNOMED - Outpatient encounter
        "308335008", // SNOMED - Patient encounter procedure
        "390906007", // SNOMED - Follow-up encounter
        "185463005"  // SNOMED - Office visit
    );

    @Override
    public String getMeasureId() {
        return "MSC";
    }

    @Override
    public String getMeasureName() {
        return "Medical Assistance with Smoking and Tobacco Use Cessation";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'MSC-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating MSC measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18+ tobacco user with outpatient visit in last 12 months)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for cessation interventions in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        // Check for cessation counseling
        JsonNode cessationCounseling = getEncounters(tenantId, patientId,
            String.join(",", CESSATION_COUNSELING_CODES), dateFilter);
        boolean hasCessationCounseling = !getEntries(cessationCounseling).isEmpty();
        String counselingDate = hasCessationCounseling ?
            getEffectiveDate(getEntries(cessationCounseling).get(0)) : null;

        // Check for cessation medications
        JsonNode cessationMedications = getMedicationRequests(tenantId, patientId,
            String.join(",", CESSATION_MEDICATION_CODES), dateFilter);
        boolean hasCessationMedication = !getEntries(cessationMedications).isEmpty();
        String medicationDate = hasCessationMedication ?
            getEffectiveDate(getEntries(cessationMedications).get(0)) : null;

        // Check for tobacco use screening/documentation
        JsonNode tobaccoScreening = getObservations(tenantId, patientId,
            String.join(",", TOBACCO_USE_SCREENING_CODES), dateFilter);
        boolean hasRecentScreening = !getEntries(tobaccoScreening).isEmpty();

        // Calculate compliance
        int componentsCompleted = 0;
        if (hasRecentScreening) componentsCompleted++;
        if (hasCessationCounseling) componentsCompleted++;
        if (hasCessationMedication) componentsCompleted++;

        // For numerator, need at least counseling OR medication
        boolean hasIntervention = hasCessationCounseling || hasCessationMedication;
        double complianceRate = componentsCompleted / 3.0;

        resultBuilder.inNumerator(hasIntervention);
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasRecentScreening) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_TOBACCO_SCREENING")
                .description("No tobacco use screening documented in last 12 months")
                .recommendedAction("Document current tobacco use status at next visit")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());
        }

        if (!hasCessationCounseling) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_CESSATION_COUNSELING")
                .description("No smoking cessation counseling documented")
                .recommendedAction("Provide tobacco cessation counseling: discuss quit date, behavioral strategies, and support resources")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        if (!hasCessationMedication) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_CESSATION_MEDICATION")
                .description("No cessation pharmacotherapy prescribed or discussed")
                .recommendedAction("Discuss cessation medications: nicotine replacement, varenicline, or bupropion")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        // Add comprehensive intervention recommendation if nothing done
        if (!hasIntervention) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("NO_CESSATION_INTERVENTION")
                .description("No smoking cessation interventions documented (counseling or medication)")
                .recommendedAction("Initiate comprehensive cessation support: counseling + pharmacotherapy shown most effective")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        // Intervention level assessment
        String interventionLevel;
        if (hasCessationCounseling && hasCessationMedication) {
            interventionLevel = "Comprehensive (counseling + medication)";
        } else if (hasCessationCounseling) {
            interventionLevel = "Counseling only";
        } else if (hasCessationMedication) {
            interventionLevel = "Medication only";
        } else {
            interventionLevel = "No intervention";
        }

        resultBuilder.details(java.util.Map.of(
            "hasRecentScreening", hasRecentScreening,
            "hasCessationCounseling", hasCessationCounseling,
            "counselingDate", counselingDate != null ? counselingDate : "Not available",
            "hasCessationMedication", hasCessationMedication,
            "medicationDate", medicationDate != null ? medicationDate : "Not available",
            "interventionLevel", interventionLevel,
            "hasAnyIntervention", hasIntervention
        ));

        resultBuilder.evidence(java.util.Map.of(
            "tobaccoScreeningCurrent", hasRecentScreening,
            "cessationCounselingProvided", hasCessationCounseling,
            "cessationMedicationProvided", hasCessationMedication,
            "interventionComplete", hasIntervention
        ));

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

        // Must have outpatient visit in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode outpatientVisits = getEncounters(tenantId, patientId,
            String.join(",", OUTPATIENT_VISIT_CODES), dateFilter);
        if (getEntries(outpatientVisits).isEmpty()) {
            return false;
        }

        // Must be identified as tobacco user
        JsonNode tobaccoScreening = getObservations(tenantId, patientId,
            String.join(",", TOBACCO_USE_SCREENING_CODES), dateFilter);
        List<JsonNode> screeningEntries = getEntries(tobaccoScreening);

        if (screeningEntries.isEmpty()) {
            // Check for tobacco use condition codes
            JsonNode tobaccoConditions = getConditions(tenantId, patientId,
                String.join(",", TOBACCO_USER_CODES));
            return !getEntries(tobaccoConditions).isEmpty();
        }

        // Check if screening indicates current tobacco use
        for (JsonNode screening : screeningEntries) {
            try {
                if (screening.has("valueCodeableConcept")) {
                    JsonNode valueCode = screening.get("valueCodeableConcept");
                    if (valueCode.has("coding")) {
                        JsonNode codings = valueCode.get("coding");
                        if (codings.isArray()) {
                            for (JsonNode coding : codings) {
                                String display = coding.has("display") ?
                                    coding.get("display").asText().toLowerCase() : "";
                                // Look for indicators of current tobacco use
                                if (display.contains("current") &&
                                    (display.contains("smoker") || display.contains("tobacco"))) {
                                    return true;
                                }
                            }
                        }
                    }
                } else if (screening.has("valueString")) {
                    String value = screening.get("valueString").asText().toLowerCase();
                    if (value.contains("current") || value.contains("yes") || value.contains("smoker")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                logger.debug("Error parsing tobacco screening result: {}", e.getMessage());
            }
        }

        return false;
    }
}
