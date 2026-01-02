package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * AMM - Antidepressant Medication Management (HEDIS)
 *
 * Evaluates whether patients diagnosed with depression and started on antidepressant therapy
 * remained on the medication treatment.
 *
 * Two rates:
 * - Effective Acute Phase Treatment: Remained on medication for at least 84 days (12 weeks)
 * - Effective Continuation Phase Treatment: Remained on medication for at least 180 days (6 months)
 */
@Component
public class AMMMeasure extends AbstractHedisMeasure {

    private static final List<String> DEPRESSION_CODES = Arrays.asList(
        "35489007",  // Major depression (SNOMED)
        "36923009",  // Major depressive disorder (SNOMED)
        "48589004",  // Depression disorder (SNOMED)
        "87512008",  // Dysthymia (SNOMED)
        "310497006", // Severe depressive episode (SNOMED)
        "319768000", // Recurrent major depressive disorder (SNOMED)
        "370143000"  // Major depressive disorder single episode (SNOMED)
    );

    private static final List<String> ANTIDEPRESSANT_MEDICATION_CODES = Arrays.asList(
        "36437",     // RxNorm - Sertraline
        "32937",     // RxNorm - Fluoxetine
        "3638",      // RxNorm - Citalopram
        "6646",      // RxNorm - Escitalopram
        "32968",     // RxNorm - Paroxetine
        "704",       // RxNorm - Bupropion
        "39786",     // RxNorm - Duloxetine
        "321988",    // RxNorm - Venlafaxine
        "8123",      // RxNorm - Mirtazapine
        "31565",     // RxNorm - Trazodone
        "7531",      // RxNorm - Amitriptyline
        "5691"       // RxNorm - Desipramine
    );

    @Override
    public String getMeasureId() {
        return "AMM";
    }

    @Override
    public String getMeasureName() {
        return "Antidepressant Medication Management";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'AMM-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating AMM measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18+ with depression and new antidepressant)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Get initial antidepressant prescription (Index Prescription Start Date - IPSD)
        JsonNode antidepressantMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIDEPRESSANT_MEDICATION_CODES), null);
        List<JsonNode> medicationEntries = getEntries(antidepressantMeds);

        if (medicationEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No antidepressant medication found")
                .build();
        }

        // Get the most recent prescription (IPSD)
        JsonNode initialPrescription = medicationEntries.get(0);
        String ipsdStr = getEffectiveDate(initialPrescription);
        LocalDate ipsd = LocalDate.parse(ipsdStr);

        // Calculate treatment windows
        LocalDate acutePhaseEnd = ipsd.plusDays(84);     // 12 weeks
        LocalDate continuationPhaseEnd = ipsd.plusDays(180); // 6 months

        // Count medication refills/prescriptions during treatment periods
        String acuteDateFilter = "ge" + ipsd.toString() + "&date=le" + acutePhaseEnd.toString();
        JsonNode acutePhaseMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIDEPRESSANT_MEDICATION_CODES), acuteDateFilter);
        int acutePhasePrescriptions = getEntries(acutePhaseMeds).size();

        String continuationDateFilter = "ge" + ipsd.toString() + "&date=le" + continuationPhaseEnd.toString();
        JsonNode continuationPhaseMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIDEPRESSANT_MEDICATION_CODES), continuationDateFilter);
        int continuationPhasePrescriptions = getEntries(continuationPhaseMeds).size();

        // Effective treatment requires at least 2 prescriptions during the period
        // (initial prescription + at least one refill)
        boolean effectiveAcutePhase = acutePhasePrescriptions >= 2;
        boolean effectiveContinuationPhase = continuationPhasePrescriptions >= 3; // More refills for 6 months

        // Count components completed
        int componentsCompleted = 0;
        if (effectiveAcutePhase) componentsCompleted++;
        if (effectiveContinuationPhase) componentsCompleted++;

        // Patient is in numerator if both phases are complete
        boolean inNumerator = effectiveAcutePhase && effectiveContinuationPhase;
        resultBuilder.inNumerator(inNumerator);

        double complianceRate = componentsCompleted / 2.0;
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Identify care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!effectiveAcutePhase) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INCOMPLETE_ACUTE_PHASE_TREATMENT")
                .description("Antidepressant medication not continued for acute phase (84 days)")
                .recommendedAction("Continue antidepressant medication for at least 12 weeks")
                .priority("high")
                .dueDate(acutePhaseEnd.isAfter(LocalDate.now()) ? acutePhaseEnd : LocalDate.now().plusWeeks(1))
                .build());
        }
        if (!effectiveContinuationPhase) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INCOMPLETE_CONTINUATION_PHASE_TREATMENT")
                .description("Antidepressant medication not continued for continuation phase (180 days)")
                .recommendedAction("Continue antidepressant medication for at least 6 months")
                .priority("high")
                .dueDate(continuationPhaseEnd.isAfter(LocalDate.now()) ? continuationPhaseEnd : LocalDate.now().plusWeeks(2))
                .build());
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "indexPrescriptionDate", ipsdStr,
            "effectiveAcutePhase", effectiveAcutePhase,
            "effectiveContinuationPhase", effectiveContinuationPhase,
            "acutePhasePrescriptions", acutePhasePrescriptions,
            "continuationPhasePrescriptions", continuationPhasePrescriptions,
            "acutePhaseEndDate", acutePhaseEnd.toString(),
            "continuationPhaseEndDate", continuationPhaseEnd.toString()
        ));

        resultBuilder.evidence(java.util.Map.of(
            "ipsd", ipsdStr,
            "acutePhaseDays", 84,
            "continuationPhaseDays", 180,
            "acutePhasePrescriptions", acutePhasePrescriptions,
            "continuationPhasePrescriptions", continuationPhasePrescriptions
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

        // Must have depression diagnosis
        JsonNode depressionConditions = getConditions(tenantId, patientId,
            String.join(",", DEPRESSION_CODES));
        if (getEntries(depressionConditions).isEmpty()) {
            return false;
        }

        // Must have antidepressant medication
        JsonNode antidepressantMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIDEPRESSANT_MEDICATION_CODES), null);
        return !getEntries(antidepressantMeds).isEmpty();
    }
}
