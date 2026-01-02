package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * AMR - Asthma Medication Ratio (HEDIS)
 *
 * Evaluates the ratio of controller medications to total asthma medications.
 * Target: AMR >= 0.50 (at least 50% controller medications)
 *
 * Controller medications: Inhaled corticosteroids, leukotriene modifiers, long-acting beta agonists (in combination)
 * Reliever medications: Short-acting beta agonists (SABAs)
 */
@Component
public class AMRMeasure extends AbstractHedisMeasure {

    private static final List<String> ASTHMA_CODES = Arrays.asList(
        "195967001", // Asthma (SNOMED)
        "233678006", // Childhood asthma (SNOMED)
        "233679003", // Exercise-induced asthma (SNOMED)
        "707445000", // Exacerbation of asthma (SNOMED)
        "426979002", // Mild persistent asthma (SNOMED)
        "427603009", // Moderate persistent asthma (SNOMED)
        "426656000"  // Severe persistent asthma (SNOMED)
    );

    private static final List<String> CONTROLLER_MEDICATION_CODES = Arrays.asList(
        // Inhaled Corticosteroids
        "51940",     // RxNorm - Fluticasone
        "6851",      // RxNorm - Budesonide
        "1001",      // RxNorm - Beclomethasone
        "7716",      // RxNorm - Mometasone
        "3827",      // RxNorm - Ciclesonide
        // Leukotriene Modifiers
        "42331",     // RxNorm - Montelukast
        "8040",      // RxNorm - Zafirlukast
        "16681",     // RxNorm - Zileuton
        // Combination Products (ICS + LABA)
        "249803",    // RxNorm - Fluticasone/Salmeterol
        "797034",    // RxNorm - Budesonide/Formoterol
        "746815"     // RxNorm - Mometasone/Formoterol
    );

    private static final List<String> RELIEVER_MEDICATION_CODES = Arrays.asList(
        // Short-Acting Beta Agonists
        "435",       // RxNorm - Albuterol
        "8123",      // RxNorm - Levalbuterol
        "8498",      // RxNorm - Metaproterenol
        "36117"      // RxNorm - Pirbuterol
    );

    @Override
    public String getMeasureId() {
        return "AMR";
    }

    @Override
    public String getMeasureName() {
        return "Asthma Medication Ratio";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'AMR-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating AMR measure for patient: {}", patientId);

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

        // Count controller medication prescriptions in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode controllerMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", CONTROLLER_MEDICATION_CODES), dateFilter);
        int controllerCount = getEntries(controllerMeds).size();

        // Count reliever medication prescriptions in last 12 months
        JsonNode relieverMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", RELIEVER_MEDICATION_CODES), dateFilter);
        int relieverCount = getEntries(relieverMeds).size();

        int totalMedications = controllerCount + relieverCount;

        if (totalMedications == 0) {
            // No asthma medications - care gap
            resultBuilder.inNumerator(false);
            resultBuilder.complianceRate(0.0);
            resultBuilder.score(0.0);

            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("NO_ASTHMA_MEDICATIONS")
                    .description("Patient with persistent asthma has no asthma medication prescriptions in last 12 months")
                    .recommendedAction("Initiate asthma controller therapy (inhaled corticosteroid or leukotriene modifier)")
                    .priority("high")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "controllerCount", 0,
                "relieverCount", 0,
                "totalMedications", 0,
                "amr", 0.0
            ));

            return resultBuilder.build();
        }

        // Calculate AMR (Asthma Medication Ratio)
        double amr = (double) controllerCount / totalMedications;

        // Target: AMR >= 0.50
        boolean meetsTarget = amr >= 0.50;

        resultBuilder.inNumerator(meetsTarget);
        resultBuilder.complianceRate(amr);
        resultBuilder.score(amr * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!meetsTarget) {
            String priority;
            String description;

            if (amr < 0.30) {
                priority = "high";
                description = String.format("Very low controller medication ratio (%.1f%%). Over-reliance on rescue inhalers.", amr * 100);
            } else {
                priority = "medium";
                description = String.format("Suboptimal controller medication ratio (%.1f%%). Target is ≥50%%.", amr * 100);
            }

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("LOW_ASTHMA_MEDICATION_RATIO")
                .description(description)
                .recommendedAction("Increase controller medication use (inhaled corticosteroids, leukotriene modifiers) relative to rescue inhalers")
                .priority(priority)
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        // Additional care gap if no controller meds at all
        if (controllerCount == 0) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("NO_CONTROLLER_MEDICATIONS")
                .description("Patient using only rescue inhalers without controller therapy")
                .recommendedAction("Initiate controller medication (inhaled corticosteroid or leukotriene modifier) for persistent asthma")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "controllerCount", controllerCount,
            "relieverCount", relieverCount,
            "totalMedications", totalMedications,
            "amr", amr,
            "meetsTarget", meetsTarget,
            "targetAMR", 0.50
        ));

        resultBuilder.evidence(java.util.Map.of(
            "controllerMedications", controllerCount,
            "relieverMedications", relieverCount,
            "asthmaControlRatio", amr
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 5 || age > 64) {
            return false;
        }

        // Must have persistent asthma diagnosis
        JsonNode asthmaConditions = getConditions(tenantId, patientId,
            String.join(",", ASTHMA_CODES));
        if (getEntries(asthmaConditions).isEmpty()) {
            return false;
        }

        // Must have had at least 4 asthma medication prescriptions in the year
        // (indicating persistent asthma, not just occasional symptoms)
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode controllerMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", CONTROLLER_MEDICATION_CODES), dateFilter);
        JsonNode relieverMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", RELIEVER_MEDICATION_CODES), dateFilter);

        int totalPrescriptions = getEntries(controllerMeds).size() + getEntries(relieverMeds).size();

        return totalPrescriptions >= 4;
    }
}
