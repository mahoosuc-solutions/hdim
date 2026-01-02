package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * SFM - Safe Use of Opioids - Concurrent Prescribing (HEDIS)
 *
 * Identifies patients receiving CONCURRENT prescriptions for:
 * - Opioids AND benzodiazepines (dangerous combination - respiratory depression)
 *
 * This is an INVERSE measure - LOWER rates are better.
 * Being in the numerator indicates UNSAFE PRACTICE (concurrent prescribing).
 */
@Component
public class SFMMeasure extends AbstractHedisMeasure {

    private static final List<String> OPIOID_MEDICATION_CODES = Arrays.asList(
        "7052",      // RxNorm - Hydrocodone
        "5489",      // RxNorm - Oxycodone
        "3423",      // RxNorm - Codeine
        "7804",      // RxNorm - Methadone
        "6813",      // RxNorm - Morphine
        "5640",      // RxNorm - Hydromorphone
        "4337",      // RxNorm - Fentanyl
        "8001",      // RxNorm - Oxymorphone
        "237005",    // RxNorm - Tapentadol
        "787390"     // RxNorm - Tramadol
    );

    private static final List<String> BENZODIAZEPINE_CODES = Arrays.asList(
        "2356",      // RxNorm - Alprazolam (Xanax)
        "2597",      // RxNorm - Clonazepam (Klonopin)
        "3016",      // RxNorm - Diazepam (Valium)
        "6470",      // RxNorm - Lorazepam (Ativan)
        "9490",      // RxNorm - Temazepam (Restoril)
        "35296",     // RxNorm - Triazolam (Halcion)
        "3318",      // RxNorm - Chlordiazepoxide (Librium)
        "8120",      // RxNorm - Oxazepam (Serax)
        "2048",      // RxNorm - Clorazepate (Tranxene)
        "4078"       // RxNorm - Flurazepam (Dalmane)
    );

    @Override
    public String getMeasureId() {
        return "SFM";
    }

    @Override
    public String getMeasureName() {
        return "Safe Use of Opioids - Concurrent Prescribing";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'SFM-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating SFM measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18+ with opioid prescription)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for opioid prescriptions in last 60 days
        String dateFilter = "ge" + LocalDate.now().minusDays(60).toString();
        JsonNode opioidMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", OPIOID_MEDICATION_CODES), dateFilter);
        List<JsonNode> opioidEntries = getEntries(opioidMeds);

        // Check for benzodiazepine prescriptions in same period
        JsonNode benzoMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", BENZODIAZEPINE_CODES), dateFilter);
        List<JsonNode> benzoEntries = getEntries(benzoMeds);

        // Check for concurrent prescribing (both opioid AND benzodiazepine)
        boolean hasConcurrentPrescribing = !opioidEntries.isEmpty() && !benzoEntries.isEmpty();

        // Get most recent prescription dates
        String mostRecentOpioidDate = !opioidEntries.isEmpty() ?
            getEffectiveDate(opioidEntries.get(0)) : null;
        String mostRecentBenzoDate = !benzoEntries.isEmpty() ?
            getEffectiveDate(benzoEntries.get(0)) : null;

        // For INVERSE measure: being in numerator = unsafe practice (bad outcome)
        resultBuilder.inNumerator(hasConcurrentPrescribing);
        // Compliance = NOT having concurrent prescribing
        resultBuilder.complianceRate(hasConcurrentPrescribing ? 0.0 : 1.0);
        resultBuilder.score(hasConcurrentPrescribing ? 0.0 : 100.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (hasConcurrentPrescribing) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("CONCURRENT_OPIOID_BENZODIAZEPINE")
                .description("UNSAFE: Concurrent opioid and benzodiazepine prescribing - 10x increased overdose death risk")
                .recommendedAction("URGENT: Review medications; taper one drug if possible; if both essential, use lowest effective doses and close monitoring")
                .priority("high")
                .dueDate(LocalDate.now().plusDays(7))
                .build());

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("OPIOID_BENZODIAZEPINE_SAFETY")
                .description("FDA black box warning: Combined use increases risk of respiratory depression, coma, and death")
                .recommendedAction("Prescribe naloxone; educate patient on overdose signs; consider alternatives to benzodiazepines (buspirone, SSRIs)")
                .priority("high")
                .dueDate(LocalDate.now().plusDays(7))
                .build());

            // Check if multiple prescribers involved
            if (opioidEntries.size() > 1 || benzoEntries.size() > 1) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("MULTIPLE_CONCURRENT_PRESCRIPTIONS")
                    .description("Multiple concurrent prescriptions suggest lack of coordination between providers")
                    .recommendedAction("Coordinate care between prescribers; check PDMP; designate single prescriber for controlled substances")
                    .priority("high")
                    .dueDate(LocalDate.now().plusWeeks(1))
                    .build());
            }
        }

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "hasOpioidPrescription", !opioidEntries.isEmpty(),
            "opioidCount", opioidEntries.size(),
            "mostRecentOpioidDate", mostRecentOpioidDate != null ? mostRecentOpioidDate : "None",
            "hasBenzodiazepinePrescription", !benzoEntries.isEmpty(),
            "benzodiazepineCount", benzoEntries.size(),
            "mostRecentBenzoDate", mostRecentBenzoDate != null ? mostRecentBenzoDate : "None",
            "hasConcurrentPrescribing", hasConcurrentPrescribing
        ));

        resultBuilder.evidence(java.util.Map.of(
            "concurrentOpioidBenzo", hasConcurrentPrescribing,
            "safePrescribing", !hasConcurrentPrescribing
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

        // Must have opioid prescription in last 60 days
        String dateFilter = "ge" + LocalDate.now().minusDays(60).toString();
        JsonNode opioidMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", OPIOID_MEDICATION_CODES), dateFilter);

        return !getEntries(opioidMeds).isEmpty();
    }
}
