package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * ADD - Follow-Up Care for Children Prescribed ADHD Medication (HEDIS)
 *
 * Evaluates whether children newly prescribed ADHD medication received appropriate follow-up care.
 * Two rates:
 * - Initiation Phase: At least 1 follow-up visit within 30 days of prescription
 * - Continuation Phase: At least 2 follow-up visits in the 9 months after initiation (days 31-300)
 */
@Component
public class ADDMeasure extends AbstractHedisMeasure {

    private static final List<String> ADHD_CODES = Arrays.asList(
        "406506008", // Attention deficit hyperactivity disorder (SNOMED)
        "192127007", // ADHD, predominantly hyperactive/impulsive type (SNOMED)
        "406505007"  // ADHD, combined type (SNOMED)
    );

    private static final List<String> ADHD_MEDICATION_CODES = Arrays.asList(
        "562008",    // RxNorm - Methylphenidate
        "42347",     // RxNorm - Amphetamine
        "3638",      // RxNorm - Dexmethylphenidate
        "284635",    // RxNorm - Lisdexamfetamine
        "1377",      // RxNorm - Atomoxetine
        "42330",     // RxNorm - Guanfacine
        "1046"       // RxNorm - Clonidine
    );

    private static final List<String> FOLLOW_UP_VISIT_CODES = Arrays.asList(
        "185349003", // Encounter for check up (SNOMED)
        "390906007", // Follow-up encounter (SNOMED)
        "185389009", // Follow-up visit (SNOMED)
        "185345009", // Encounter for symptom (SNOMED)
        "308335008", // Patient encounter procedure (SNOMED)
        "76168009",  // Psychiatric diagnostic evaluation (SNOMED)
        "313234004"  // Mental health counseling (SNOMED)
    );

    @Override
    public String getMeasureId() {
        return "ADD";
    }

    @Override
    public String getMeasureName() {
        return "Follow-Up Care for Children Prescribed ADHD Medication";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'ADD-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating ADD measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 6-12 with new ADHD medication in last 10 months)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find initial ADHD medication prescription (Index Prescription Start Date - IPSD)
        JsonNode adhdMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ADHD_MEDICATION_CODES), null);
        List<JsonNode> medicationEntries = getEntries(adhdMeds);

        if (medicationEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No ADHD medication found")
                .build();
        }

        // Get the most recent prescription (IPSD)
        JsonNode initialPrescription = medicationEntries.get(0);
        String ipsdStr = getEffectiveDate(initialPrescription);
        LocalDate ipsd = LocalDate.parse(ipsdStr);

        // Calculate follow-up windows
        LocalDate initiationPhaseEnd = ipsd.plusDays(30);       // Days 1-30
        LocalDate continuationPhaseStart = ipsd.plusDays(31);   // Day 31
        LocalDate continuationPhaseEnd = ipsd.plusDays(300);    // Day 300

        // Check for initiation phase follow-up (at least 1 visit in first 30 days)
        String initiationDateFilter = "ge" + ipsd.toString() + "&date=le" + initiationPhaseEnd.toString();
        JsonNode initiationVisits = getEncounters(tenantId, patientId,
            String.join(",", FOLLOW_UP_VISIT_CODES), initiationDateFilter);
        int initiationVisitCount = getEntries(initiationVisits).size();
        boolean hasInitiationFollowUp = initiationVisitCount >= 1;

        // Check for continuation phase follow-up (at least 2 visits in days 31-300)
        String continuationDateFilter = "ge" + continuationPhaseStart.toString() + "&date=le" + continuationPhaseEnd.toString();
        JsonNode continuationVisits = getEncounters(tenantId, patientId,
            String.join(",", FOLLOW_UP_VISIT_CODES), continuationDateFilter);
        int continuationVisitCount = getEntries(continuationVisits).size();
        boolean hasContinuationFollowUp = continuationVisitCount >= 2;

        // Count components completed
        int componentsCompleted = 0;
        if (hasInitiationFollowUp) componentsCompleted++;
        if (hasContinuationFollowUp) componentsCompleted++;

        // Patient is in numerator if both phases are complete
        boolean inNumerator = hasInitiationFollowUp && hasContinuationFollowUp;
        resultBuilder.inNumerator(inNumerator);

        double complianceRate = componentsCompleted / 2.0;
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!hasInitiationFollowUp) {
            boolean isOverdue = LocalDate.now().isAfter(initiationPhaseEnd);
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_INITIATION_FOLLOWUP")
                .description(String.format("No follow-up visit within 30 days of ADHD medication initiation (%s)", ipsdStr))
                .recommendedAction("Schedule follow-up visit within 30 days of ADHD medication start")
                .priority("high")
                .dueDate(isOverdue ? LocalDate.now().plusWeeks(1) : initiationPhaseEnd)
                .build());
        }

        if (!hasContinuationFollowUp) {
            boolean isOverdue = LocalDate.now().isAfter(continuationPhaseEnd);
            String visitInfo = continuationVisitCount == 0
                ? "No follow-up visits"
                : String.format("Only %d follow-up visit(s)", continuationVisitCount);

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INCOMPLETE_CONTINUATION_FOLLOWUP")
                .description(String.format("%s in continuation phase (days 31-300). Need at least 2 visits.", visitInfo))
                .recommendedAction("Schedule follow-up visits to monitor ADHD medication effectiveness and side effects (need 2 visits total in days 31-300)")
                .priority("high")
                .dueDate(isOverdue ? LocalDate.now().plusWeeks(2) : continuationPhaseEnd)
                .build());
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "medicationStartDate", ipsdStr,
            "hasInitiationFollowUp", hasInitiationFollowUp,
            "hasContinuationFollowUp", hasContinuationFollowUp,
            "initiationVisitCount", initiationVisitCount,
            "continuationVisitCount", continuationVisitCount,
            "initiationPhaseEnd", initiationPhaseEnd.toString(),
            "continuationPhaseEnd", continuationPhaseEnd.toString()
        ));

        resultBuilder.evidence(java.util.Map.of(
            "ipsd", ipsdStr,
            "initiationVisits", initiationVisitCount,
            "continuationVisits", continuationVisitCount,
            "totalFollowUpVisits", initiationVisitCount + continuationVisitCount
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 6 || age > 12) {
            return false;
        }

        // Must have ADHD diagnosis
        JsonNode adhdConditions = getConditions(tenantId, patientId,
            String.join(",", ADHD_CODES));
        if (getEntries(adhdConditions).isEmpty()) {
            return false;
        }

        // Must have ADHD medication prescribed in last 10 months (to allow for 300-day follow-up window)
        String medDateFilter = "ge" + LocalDate.now().minusMonths(10).toString();
        JsonNode adhdMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", ADHD_MEDICATION_CODES), medDateFilter);

        return !getEntries(adhdMeds).isEmpty();
    }
}
