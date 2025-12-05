package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * VLS - Viral Load Suppression for Patients with HIV (HEDIS)
 *
 * Evaluates whether patients with HIV achieved viral load suppression
 * (<200 copies/mL) during the measurement year.
 *
 * Viral suppression is the primary goal of HIV treatment - it prevents
 * disease progression, transmission to others (U=U: Undetectable = Untransmittable),
 * and improves long-term health outcomes.
 */
@Component
public class VLSMeasure extends AbstractHedisMeasure {

    private static final List<String> HIV_DIAGNOSIS_CODES = Arrays.asList(
        "86406008",  // SNOMED - Human immunodeficiency virus infection
        "165816005", // SNOMED - HIV positive
        "402916007", // SNOMED - Acquired immune deficiency syndrome
        "62479008",  // SNOMED - Acquired immunodeficiency syndrome (disorder)
        "235009000"  // SNOMED - HIV disease
    );

    private static final List<String> HIV_VIRAL_LOAD_CODES = Arrays.asList(
        "20447-9",   // LOINC - HIV 1 RNA [#/volume] (viral load) in Serum or Plasma
        "48551-2",   // LOINC - HIV 1 RNA [Log #/volume] in Serum or Plasma
        "62469-2",   // LOINC - HIV 1 RNA [Presence] in Serum or Plasma
        "10351-5",   // LOINC - HIV 1 RNA [Units/volume] in Serum or Plasma
        "23876-6",   // LOINC - HIV 1 RNA [#/volume] in Plasma
        "25836-8"    // LOINC - HIV 1 RNA [#/volume] (viral load) in Specimen
    );

    private static final List<String> ANTIRETROVIRAL_MEDICATION_CODES = Arrays.asList(
        // NRTIs
        "19552",     // RxNorm - Emtricitabine
        "30321",     // RxNorm - Tenofovir
        "114740",    // RxNorm - Abacavir
        // NNRTIs
        "114698",    // RxNorm - Efavirenz
        "358761",    // RxNorm - Rilpivirine
        // PIs
        "195085",    // RxNorm - Darunavir
        "195088",    // RxNorm - Atazanavir
        // Integrase inhibitors
        "593044",    // RxNorm - Dolutegravir
        "860095",    // RxNorm - Raltegravir
        "1014579",   // RxNorm - Bictegravir
        // Combination products
        "847187",    // RxNorm - Biktarvy (bictegravir + emtricitabine + tenofovir)
        "1594234",   // RxNorm - Descovy (emtricitabine + tenofovir)
        "217059"     // RxNorm - Truvada (emtricitabine + tenofovir)
    );

    private static final double VIRAL_SUPPRESSION_THRESHOLD = 200.0; // copies/mL

    @Override
    public String getMeasureId() {
        return "VLS";
    }

    @Override
    public String getMeasureName() {
        return "Viral Load Suppression for Patients with HIV";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'VLS-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating VLS measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must have HIV diagnosis)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for viral load tests in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusYears(1).toString();
        JsonNode viralLoads = getObservations(tenantId, patientId,
            String.join(",", HIV_VIRAL_LOAD_CODES), dateFilter);
        List<JsonNode> viralLoadEntries = getEntries(viralLoads);

        if (viralLoadEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(true)
                .inNumerator(false)
                .exclusionReason("No viral load test in last 12 months - cannot assess suppression")
                .build();
        }

        // Get most recent viral load
        JsonNode mostRecentVL = viralLoadEntries.get(0);
        String vlDate = getEffectiveDate(mostRecentVL);
        Double viralLoadValue = null;
        boolean isSuppressed = false;

        // Extract viral load value
        try {
            if (mostRecentVL.has("valueQuantity")) {
                JsonNode valueQuantity = mostRecentVL.get("valueQuantity");
                if (valueQuantity.has("value")) {
                    viralLoadValue = valueQuantity.get("value").asDouble();
                    isSuppressed = viralLoadValue < VIRAL_SUPPRESSION_THRESHOLD;
                }
            } else if (mostRecentVL.has("valueCodeableConcept")) {
                // Handle "undetectable" or "below quantification limit" results
                JsonNode valueCodeable = mostRecentVL.get("valueCodeableConcept");
                if (valueCodeable.has("coding")) {
                    String code = valueCodeable.get("coding").get(0).get("code").asText();
                    if (code.contains("260415000") || code.contains("undetectable")) {
                        isSuppressed = true;
                        viralLoadValue = 0.0; // Treat as undetectable
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not extract viral load value: {}", e.getMessage());
        }

        // Check for antiretroviral medication (adherence indicator)
        JsonNode arvMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIRETROVIRAL_MEDICATION_CODES), dateFilter);
        boolean hasARVTherapy = !getEntries(arvMeds).isEmpty();

        // Count viral load tests in last year (should be at least 2)
        int vlTestCount = viralLoadEntries.size();

        resultBuilder.inNumerator(isSuppressed);
        resultBuilder.complianceRate(isSuppressed ? 1.0 : 0.0);
        resultBuilder.score(isSuppressed ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!isSuppressed && viralLoadValue != null) {
            String vlCategory;
            String priority;
            String action;

            if (viralLoadValue >= 100000) {
                vlCategory = "Very high viral load (≥100,000 copies/mL)";
                priority = "high";
                action = "URGENT: Assess adherence, check for resistance, consider regimen change; risk of rapid disease progression and transmission";
            } else if (viralLoadValue >= 1000) {
                vlCategory = "High viral load (1,000-99,999 copies/mL)";
                priority = "high";
                action = "Assess medication adherence; check for drug interactions; consider resistance testing; intensify monitoring";
            } else if (viralLoadValue >= 200) {
                vlCategory = "Detectable but low viral load (200-999 copies/mL)";
                priority = "medium";
                action = "Assess adherence barriers; provide adherence counseling; repeat viral load in 1-2 months";
            } else {
                vlCategory = "Viral load <200 but not fully suppressed";
                priority = "low";
                action = "Continue current regimen; monitor for sustained suppression";
            }

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("HIV_VIROLOGIC_FAILURE")
                .description(String.format("%s - viral load %.0f copies/mL (goal <200)", vlCategory, viralLoadValue))
                .recommendedAction(action)
                .priority(priority)
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }

        if (!hasARVTherapy) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_ANTIRETROVIRAL_THERAPY")
                .description("HIV diagnosis without documented antiretroviral therapy")
                .recommendedAction("Initiate ART immediately (current guidelines recommend treatment for all HIV+ patients regardless of CD4 count)")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(1))
                .build());
        }

        if (vlTestCount < 2) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INSUFFICIENT_VIRAL_LOAD_MONITORING")
                .description(String.format("Only %d viral load test(s) in last year (recommend at least 2)", vlTestCount))
                .recommendedAction("Schedule viral load testing every 3-6 months for stable patients, more frequently for new patients or treatment changes")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        String vlStatus = "Unknown";
        if (viralLoadValue != null) {
            if (viralLoadValue == 0.0) {
                vlStatus = "Undetectable (<20 copies/mL)";
            } else if (viralLoadValue < 200) {
                vlStatus = String.format("Suppressed (%.0f copies/mL)", viralLoadValue);
            } else if (viralLoadValue < 1000) {
                vlStatus = String.format("Low-level viremia (%.0f copies/mL)", viralLoadValue);
            } else if (viralLoadValue < 100000) {
                vlStatus = String.format("Virologic failure (%.0f copies/mL)", viralLoadValue);
            } else {
                vlStatus = String.format("High viremia (%.0f copies/mL)", viralLoadValue);
            }
        }

        resultBuilder.details(java.util.Map.of(
            "viralLoadDate", vlDate,
            "viralLoadValue", viralLoadValue != null ? viralLoadValue : "Not available",
            "viralLoadStatus", vlStatus,
            "isSuppressed", isSuppressed,
            "hasARVTherapy", hasARVTherapy,
            "vlTestCount", vlTestCount,
            "suppressionGoal", "Viral load <200 copies/mL"
        ));

        resultBuilder.evidence(java.util.Map.of(
            "viralSuppression", isSuppressed,
            "onAntiretroviralTherapy", hasARVTherapy,
            "adequateMonitoring", vlTestCount >= 2
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be age 13+ (HIV measure applies to adolescents and adults)
        Integer age = getPatientAge(patient);
        if (age == null || age < 13) {
            return false;
        }

        // Must have HIV diagnosis
        JsonNode hivDiagnoses = getConditions(tenantId, patientId,
            String.join(",", HIV_DIAGNOSIS_CODES));

        return !getEntries(hivDiagnoses).isEmpty();
    }
}
