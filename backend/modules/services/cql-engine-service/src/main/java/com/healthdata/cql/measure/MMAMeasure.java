package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * MMA - Medication Management for People with Asthma (HEDIS)
 *
 * Evaluates whether patients with persistent asthma received appropriate
 * controller medication and maintained adequate medication adherence.
 *
 * Two age-based components:
 * - Ages 5-11: 50% adherence threshold (PDC ≥50%)
 * - Ages 12-64: 50% adherence threshold (PDC ≥50%)
 *
 * Controller medications reduce inflammation and prevent exacerbations.
 */
@Component
public class MMAMeasure extends AbstractHedisMeasure {

    private static final List<String> PERSISTENT_ASTHMA_CODES = Arrays.asList(
        "195967001", // SNOMED - Asthma (disorder)
        "233678006", // SNOMED - Childhood asthma
        "370218001", // SNOMED - Mild persistent asthma
        "370219009", // SNOMED - Moderate persistent asthma
        "370220003", // SNOMED - Severe persistent asthma
        "370221004"  // SNOMED - Exercise-induced asthma
    );

    private static final List<String> ASTHMA_CONTROLLER_MEDICATION_CODES = Arrays.asList(
        // Inhaled corticosteroids (ICS)
        "6879",      // RxNorm - Fluticasone
        "31301",     // RxNorm - Budesonide
        "1656",      // RxNorm - Beclomethasone
        "42316",     // RxNorm - Mometasone
        // Long-acting beta agonists (LABA)
        "36117",     // RxNorm - Salmeterol
        "4614",      // RxNorm - Formoterol
        // ICS/LABA combinations
        "261101",    // RxNorm - Advair (fluticasone/salmeterol)
        "616843",    // RxNorm - Symbicort (budesonide/formoterol)
        "1547999",   // RxNorm - Breo (fluticasone/vilanterol)
        "1658175",   // RxNorm - Dulera (mometasone/formoterol)
        // Leukotriene modifiers
        "35827",     // RxNorm - Montelukast (Singulair)
        "25025",     // RxNorm - Zafirlukast
        // Long-acting muscarinic antagonists (LAMA)
        "2041737"    // RxNorm - Tiotropium (asthma indication)
    );

    private static final List<String> RESCUE_MEDICATION_CODES = Arrays.asList(
        "745679",    // RxNorm - Albuterol inhaler
        "351137",    // RxNorm - Levalbuterol
        "2123"       // RxNorm - Albuterol nebulizer solution
    );

    private static final List<String> ASTHMA_EXACERBATION_CODES = Arrays.asList(
        "195959008", // SNOMED - Acute exacerbation of asthma
        "370218001", // SNOMED - Severe asthma attack
        "781999",    // SNOMED - Status asthmaticus
        "225597003"  // SNOMED - Emergency room visit for asthma
    );

    @Override
    public String getMeasureId() {
        return "MMA";
    }

    @Override
    public String getMeasureName() {
        return "Medication Management for People with Asthma";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'MMA-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating MMA measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 5-64 with persistent asthma)")
                .build();
        }

        resultBuilder.inDenominator(true);

        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);

        // Check for controller medications in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusYears(1).toString();
        JsonNode controllerMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ASTHMA_CONTROLLER_MEDICATION_CODES), dateFilter);
        List<JsonNode> controllerEntries = getEntries(controllerMeds);

        boolean hasControllerMedication = !controllerEntries.isEmpty();

        // Calculate PDC (Proportion of Days Covered) if controller medication prescribed
        AdherenceData adherence = null;
        if (hasControllerMedication) {
            adherence = calculatePDC(controllerEntries, 365);
        }

        double pdc = adherence != null ? adherence.pdc : 0.0;
        boolean meetsAdherenceThreshold = pdc >= 0.50; // 50% threshold for asthma

        // Check for rescue medication use (indicator of control)
        JsonNode rescueMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", RESCUE_MEDICATION_CODES), dateFilter);
        int rescueFillCount = getEntries(rescueMeds).size();

        // Check for exacerbations (hospitalizations, ED visits)
        JsonNode exacerbations = getConditions(tenantId, patientId,
            String.join(",", ASTHMA_EXACERBATION_CODES));
        boolean hasExacerbations = !getEntries(exacerbations).isEmpty();

        resultBuilder.inNumerator(hasControllerMedication && meetsAdherenceThreshold);
        resultBuilder.complianceRate(meetsAdherenceThreshold ? 1.0 : 0.0);
        resultBuilder.score(meetsAdherenceThreshold ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasControllerMedication) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_ASTHMA_CONTROLLER")
                .description("Persistent asthma without controller medication")
                .recommendedAction("Prescribe daily controller medication (ICS or ICS/LABA); educate on proper inhaler technique; develop asthma action plan")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        } else if (!meetsAdherenceThreshold) {
            String adherenceLevel = pdc >= 0.30 ? "Low adherence" : "Very low adherence";

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("POOR_ASTHMA_MEDICATION_ADHERENCE")
                .description(String.format("%s to controller medication (%.0f%% PDC - goal ≥50%%)", adherenceLevel, pdc * 100))
                .recommendedAction("Assess adherence barriers (cost, side effects, technique); simplify regimen; provide education; consider once-daily options")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }

        if (rescueFillCount > 3) {
            // Excessive rescue inhaler use (>3 fills/year suggests poor control)
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("EXCESSIVE_RESCUE_INHALER_USE")
                .description(String.format("%d rescue inhaler fills in last year - suggests poor asthma control", rescueFillCount))
                .recommendedAction("Step up controller therapy; reassess inhaler technique; consider specialist referral")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }

        if (hasExacerbations) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("ASTHMA_EXACERBATION_HISTORY")
                .description("Recent asthma exacerbation requiring ED visit or hospitalization")
                .recommendedAction("Review and update asthma action plan; optimize controller therapy; ensure patient has oral corticosteroid rescue pack; consider biologics if severe")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(1))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        String controlStatus = "Unknown";
        if (hasControllerMedication) {
            if (meetsAdherenceThreshold && rescueFillCount <= 2 && !hasExacerbations) {
                controlStatus = "Well-controlled";
            } else if (meetsAdherenceThreshold) {
                controlStatus = "Partially controlled";
            } else {
                controlStatus = "Poorly controlled - low adherence";
            }
        } else {
            controlStatus = "Uncontrolled - no controller medication";
        }

        resultBuilder.details(java.util.Map.of(
            "patientAge", age,
            "hasControllerMedication", hasControllerMedication,
            "medicationPDC", String.format("%.0f%%", pdc * 100),
            "meetsAdherenceThreshold", meetsAdherenceThreshold,
            "rescueFillCount", rescueFillCount,
            "hasExacerbations", hasExacerbations,
            "asthmaControlStatus", controlStatus
        ));

        resultBuilder.evidence(java.util.Map.of(
            "controllerMedicationPrescribed", hasControllerMedication,
            "adequateAdherence", meetsAdherenceThreshold,
            "excessiveRescueUse", rescueFillCount > 3,
            "noExacerbations", !hasExacerbations
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be age 5-64
        Integer age = getPatientAge(patient);
        if (age == null || age < 5 || age > 64) {
            return false;
        }

        // Must have persistent asthma diagnosis
        JsonNode asthmaDiagnoses = getConditions(tenantId, patientId,
            String.join(",", PERSISTENT_ASTHMA_CODES));

        return !getEntries(asthmaDiagnoses).isEmpty();
    }

    /**
     * Calculate PDC (Proportion of Days Covered) for asthma medications
     */
    private AdherenceData calculatePDC(List<JsonNode> medicationEntries, int periodDays) {
        boolean[] coveredDays = new boolean[periodDays];
        int fillCount = 0;

        for (JsonNode medication : medicationEntries) {
            try {
                LocalDate fillDate = LocalDate.parse(getEffectiveDate(medication));
                int daysSupply = getDaysSupply(medication);
                fillCount++;

                // Calculate days from fill date
                LocalDate today = LocalDate.now();
                int startDay = Math.max(0, (int) java.time.temporal.ChronoUnit.DAYS.between(
                    today.minusDays(periodDays), fillDate));

                // Mark covered days (handle overlaps)
                for (int i = 0; i < daysSupply && (startDay + i) < periodDays; i++) {
                    coveredDays[startDay + i] = true;
                }
            } catch (Exception e) {
                logger.debug("Could not process medication fill: {}", e.getMessage());
            }
        }

        // Count covered days
        int daysCovered = 0;
        for (boolean covered : coveredDays) {
            if (covered) daysCovered++;
        }

        double pdc = (double) daysCovered / periodDays;
        return new AdherenceData(pdc, daysCovered, periodDays, fillCount);
    }

    /**
     * Get days supply from medication request
     */
    private int getDaysSupply(JsonNode medication) {
        try {
            if (medication.has("dispenseRequest")) {
                JsonNode dispenseRequest = medication.get("dispenseRequest");
                if (dispenseRequest.has("expectedSupplyDuration")) {
                    return dispenseRequest.get("expectedSupplyDuration").get("value").asInt();
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract days supply: {}", e.getMessage());
        }
        return 30; // Default to 30-day supply
    }

    /**
     * Data class for adherence calculations
     */
    private static class AdherenceData {
        double pdc;
        int daysCovered;
        int totalDays;
        int fillCount;

        AdherenceData(double pdc, int daysCovered, int totalDays, int fillCount) {
            this.pdc = pdc;
            this.daysCovered = daysCovered;
            this.totalDays = totalDays;
            this.fillCount = fillCount;
        }
    }
}
