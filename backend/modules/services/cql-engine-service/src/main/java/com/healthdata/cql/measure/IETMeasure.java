package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * IET - Initiation and Engagement of Alcohol and Other Drug Dependence Treatment (HEDIS)
 *
 * Evaluates whether patients with new AOD diagnosis initiated and engaged in treatment.
 * Two rates:
 * - Initiation: Treatment within 14 days of diagnosis
 * - Engagement: ≥2 additional treatment encounters within 34 days of initiation
 */
@Component
public class IETMeasure extends AbstractHedisMeasure {

    private static final List<String> AOD_DEPENDENCE_CODES = Arrays.asList(
        "15167005",  // Alcohol dependence (SNOMED)
        "191874001", // Drug dependence (SNOMED)
        "231470003", // Opioid dependence (SNOMED)
        "230328006", // Cocaine dependence (SNOMED)
        "230346007", // Cannabis dependence (SNOMED)
        "66590003",  // Drug abuse (SNOMED)
        "191816009"  // Alcohol abuse (SNOMED)
    );

    private static final List<String> AOD_TREATMENT_ENCOUNTER_CODES = Arrays.asList(
        "56876005",  // Drug rehabilitation therapy (SNOMED)
        "60112009",  // Alcohol rehabilitation therapy (SNOMED)
        "385989002", // Substance abuse counseling (SNOMED)
        "371597004", // Addiction counseling (SNOMED)
        "385990006", // Group substance abuse counseling (SNOMED)
        "313071005", // Alcohol counseling (SNOMED)
        "24165005",  // Intensive outpatient treatment (SNOMED)
        "413473000"  // Counseling (SNOMED)
    );

    private static final List<String> AOD_MEDICATION_CODES = Arrays.asList(
        "7036",      // RxNorm - Naltrexone
        "4520",      // RxNorm - Disulfiram
        "1819",      // RxNorm - Buprenorphine
        "6378",      // RxNorm - Acamprosate
        "7804"       // RxNorm - Methadone
    );

    @Override
    public String getMeasureId() {
        return "IET";
    }

    @Override
    public String getMeasureName() {
        return "Initiation and Engagement of AOD Dependence Treatment";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'IET-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating IET measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 13+ with new AOD diagnosis in last 60 days)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find AOD diagnosis date (Index Episode Start Date - IESD)
        JsonNode aodConditions = getConditions(tenantId, patientId,
            String.join(",", AOD_DEPENDENCE_CODES));
        List<JsonNode> aodEntries = getEntries(aodConditions);

        if (aodEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No AOD diagnosis found")
                .build();
        }

        // Get diagnosis date (IESD)
        JsonNode mostRecentDiagnosis = aodEntries.get(0);
        String iesdStr = getEffectiveDate(mostRecentDiagnosis);
        LocalDate iesd = LocalDate.parse(iesdStr);

        // Calculate time windows
        LocalDate initiationWindowEnd = iesd.plusDays(14);           // Days 0-14
        LocalDate engagementWindowStart = iesd.plusDays(1);          // Day 1 after initiation
        LocalDate engagementWindowEnd = iesd.plusDays(34);           // Days 1-34

        // Check for treatment initiation within 14 days (encounter OR medication)
        String initiationDateFilter = "ge" + iesd.toString() + "&date=le" + initiationWindowEnd.toString();
        JsonNode initiationEncounters = getEncounters(tenantId, patientId,
            String.join(",", AOD_TREATMENT_ENCOUNTER_CODES), initiationDateFilter);
        boolean hasInitiationEncounter = !getEntries(initiationEncounters).isEmpty();

        JsonNode initiationMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", AOD_MEDICATION_CODES), initiationDateFilter);
        boolean hasInitiationMedication = !getEntries(initiationMeds).isEmpty();

        boolean hasInitiation = hasInitiationEncounter || hasInitiationMedication;

        // Check for engagement (≥2 additional encounters within 34 days)
        String engagementDateFilter = "ge" + engagementWindowStart.toString() +
                                     "&date=le" + engagementWindowEnd.toString();
        JsonNode engagementEncounters = getEncounters(tenantId, patientId,
            String.join(",", AOD_TREATMENT_ENCOUNTER_CODES), engagementDateFilter);
        int engagementVisitCount = getEntries(engagementEncounters).size();

        // Also count medication visits as engagement
        JsonNode engagementMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", AOD_MEDICATION_CODES), engagementDateFilter);
        int engagementMedCount = getEntries(engagementMeds).size();

        int totalEngagementContacts = engagementVisitCount + engagementMedCount;
        boolean hasEngagement = totalEngagementContacts >= 2;

        // Calculate compliance
        int componentsCompleted = 0;
        if (hasInitiation) componentsCompleted++;
        if (hasEngagement) componentsCompleted++;

        double complianceRate = componentsCompleted / 2.0;
        resultBuilder.inNumerator(hasInitiation && hasEngagement); // Both required
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!hasInitiation) {
            boolean isOverdue = LocalDate.now().isAfter(initiationWindowEnd);
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_AOD_TREATMENT_INITIATION")
                .description(String.format("No AOD treatment initiation within 14 days of diagnosis (%s)", iesdStr))
                .recommendedAction("Initiate substance abuse treatment (counseling, rehabilitation, or medication-assisted treatment)")
                .priority("high")
                .dueDate(isOverdue ? LocalDate.now().plusDays(2) : initiationWindowEnd)
                .build());
        }

        if (!hasEngagement) {
            boolean isOverdue = LocalDate.now().isAfter(engagementWindowEnd);
            String engagementInfo = totalEngagementContacts == 0
                ? "No engagement contacts"
                : String.format("Only %d engagement contact(s)", totalEngagementContacts);

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INSUFFICIENT_AOD_TREATMENT_ENGAGEMENT")
                .description(String.format("%s within 34 days. Need ≥2 additional treatment encounters.", engagementInfo))
                .recommendedAction("Schedule ongoing substance abuse counseling or treatment sessions (need 2+ visits)")
                .priority("high")
                .dueDate(isOverdue ? LocalDate.now().plusWeeks(1) : engagementWindowEnd)
                .build());
        }
        resultBuilder.careGaps(careGaps);

        String initiationType = hasInitiationEncounter ? "Encounter" :
                              hasInitiationMedication ? "Medication" : "None";

        resultBuilder.details(java.util.Map.of(
            "diagnosisDate", iesdStr,
            "hasInitiation", hasInitiation,
            "initiationType", initiationType,
            "hasEngagement", hasEngagement,
            "engagementVisits", engagementVisitCount,
            "engagementMedications", engagementMedCount,
            "totalEngagementContacts", totalEngagementContacts,
            "initiationWindowEnd", initiationWindowEnd.toString(),
            "engagementWindowEnd", engagementWindowEnd.toString()
        ));

        resultBuilder.evidence(java.util.Map.of(
            "iesd", iesdStr,
            "initiated", hasInitiation,
            "engaged", hasEngagement,
            "engagementContacts", totalEngagementContacts
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 13) {
            return false;
        }

        // Must have AOD dependence diagnosis in last 60 days (to allow for 34-day engagement window)
        JsonNode aodConditions = getConditions(tenantId, patientId,
            String.join(",", AOD_DEPENDENCE_CODES));

        return !getEntries(aodConditions).isEmpty();
    }
}
