package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SDOH-1 - Social Determinants of Health Screening Rate
 *
 * CMS Measure ID: CMS1238v1 (2024 APP Plus)
 * APP Plus 2028: Required measure for ACO performance
 *
 * Measure Description:
 * Percentage of patients 18 years of age and older screened for
 * social determinants of health using a standardized screening tool
 * for all 5 health-related social needs (HRSN) domains:
 *
 * Five HRSN Domains (Required):
 * 1. Food Insecurity - LOINC 88122-7
 * 2. Housing Instability - LOINC 71802-3
 * 3. Transportation Needs - LOINC 93030-5
 * 4. Utility Difficulties - LOINC 93031-3
 * 5. Interpersonal Safety - LOINC 93038-8
 *
 * Standardized Screening Tools:
 * - AHC-HRSN (Accountable Health Communities HRSN Screening Tool)
 * - PRAPARE (Protocol for Responding to and Assessing Patient Assets, Risks, and Experiences)
 *
 * Numerator:
 * - Patients with documented screening for ALL 5 HRSN domains
 *
 * Denominator Exclusions:
 * - Patient refusal documented
 * - Hospice care
 */
@Component
public class SDOH1Measure extends AbstractHedisMeasure {

    // LOINC codes for Food Insecurity screening
    private static final List<String> FOOD_INSECURITY_CODES = Arrays.asList(
        "88122-7",  // Within the past 12 months, worried food would run out [HVS]
        "88123-5",  // Within the past 12 months, food didn't last [HVS]
        "88124-3",  // Food insecurity risk [Calculated]
        "76513-1",  // Food insecurity screening [AHC-HRSN]
        "96777-8"   // Food insecurity status [PRAPARE]
    );

    // LOINC codes for Housing Instability screening
    private static final List<String> HOUSING_INSTABILITY_CODES = Arrays.asList(
        "71802-3",  // Housing status [PRAPARE]
        "93033-9",  // Housing status [AHC-HRSN]
        "93034-7",  // Housing instability [AHC-HRSN]
        "96778-6",  // Are you worried about losing your housing?
        "96779-4"   // Have you moved 2+ times in last 12 months?
    );

    // LOINC codes for Transportation screening
    private static final List<String> TRANSPORTATION_CODES = Arrays.asList(
        "93030-5",  // Transportation needs [AHC-HRSN]
        "96780-2",  // Has lack of transportation prevented you from getting healthcare?
        "93028-9"   // Transportation problems [PRAPARE]
    );

    // LOINC codes for Utility Difficulties screening
    private static final List<String> UTILITY_CODES = Arrays.asList(
        "93031-3",  // Utility needs [AHC-HRSN]
        "96781-0",  // Unable to get utilities (electric, gas, oil, water)
        "93029-7"   // Utilities - trouble paying bills [PRAPARE]
    );

    // LOINC codes for Interpersonal Safety screening
    private static final List<String> INTERPERSONAL_SAFETY_CODES = Arrays.asList(
        "93038-8",  // Stress from partner [AHC-HRSN]
        "76501-6",  // Physical hurt by someone [HITS]
        "95618-5",  // Feel safe at home
        "93039-6"   // Interpersonal safety [AHC-HRSN]
    );

    // All SDOH screening codes
    private static final List<String> ALL_SDOH_SCREENING_CODES;
    static {
        ALL_SDOH_SCREENING_CODES = new ArrayList<>();
        ALL_SDOH_SCREENING_CODES.addAll(FOOD_INSECURITY_CODES);
        ALL_SDOH_SCREENING_CODES.addAll(HOUSING_INSTABILITY_CODES);
        ALL_SDOH_SCREENING_CODES.addAll(TRANSPORTATION_CODES);
        ALL_SDOH_SCREENING_CODES.addAll(UTILITY_CODES);
        ALL_SDOH_SCREENING_CODES.addAll(INTERPERSONAL_SAFETY_CODES);
    }

    @Override
    public String getMeasureId() {
        return "SDOH-1";
    }

    @Override
    public String getMeasureName() {
        return "Social Determinants of Health Screening";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'SDOH1-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating SDOH-1 measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        // Check eligibility (must be 18+)
        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient is under 18 years of age")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for exclusions
        if (isInHospice(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(true)
                .inNumerator(false)
                .exclusionReason("Patient is in hospice care")
                .build();
        }

        // Evaluate each HRSN domain
        List<MeasureResult.CareGap> careGaps = new ArrayList<>();
        int domainsScreened = 0;

        boolean foodScreened = evaluateDomainScreening(tenantId, patientId,
            FOOD_INSECURITY_CODES, "Food Insecurity", careGaps);
        if (foodScreened) domainsScreened++;

        boolean housingScreened = evaluateDomainScreening(tenantId, patientId,
            HOUSING_INSTABILITY_CODES, "Housing Instability", careGaps);
        if (housingScreened) domainsScreened++;

        boolean transportScreened = evaluateDomainScreening(tenantId, patientId,
            TRANSPORTATION_CODES, "Transportation", careGaps);
        if (transportScreened) domainsScreened++;

        boolean utilityScreened = evaluateDomainScreening(tenantId, patientId,
            UTILITY_CODES, "Utility Difficulties", careGaps);
        if (utilityScreened) domainsScreened++;

        boolean safetyScreened = evaluateDomainScreening(tenantId, patientId,
            INTERPERSONAL_SAFETY_CODES, "Interpersonal Safety", careGaps);
        if (safetyScreened) domainsScreened++;

        // Set care gaps
        if (!careGaps.isEmpty()) {
            resultBuilder.careGaps(careGaps);
        }

        // Patient is in numerator if ALL 5 domains are screened
        boolean inNumerator = domainsScreened == 5;
        double complianceRate = domainsScreened / 5.0;

        resultBuilder
            .inNumerator(inNumerator)
            .complianceRate(complianceRate)
            .score(complianceRate * 100)
            .evidence(java.util.Map.of(
                "domainsScreened", domainsScreened,
                "totalDomains", 5
            ))
            .details(java.util.Map.of(
                "foodInsecurityScreened", foodScreened,
                "housingInstabilityScreened", housingScreened,
                "transportationScreened", transportScreened,
                "utilityDifficultiesScreened", utilityScreened,
                "interpersonalSafetyScreened", safetyScreened,
                "allDomainsComplete", inNumerator
            ));

        MeasureResult result = resultBuilder.build();
        logger.info("SDOH-1 evaluation complete for patient {}: domains={}/5, inNumerator={}",
            patientId, domainsScreened, result.isInNumerator());

        return result;
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);
        return age != null && age >= 18;
    }

    /**
     * Evaluate if a specific HRSN domain has been screened.
     */
    private boolean evaluateDomainScreening(String tenantId, String patientId,
                                            List<String> domainCodes, String domainName,
                                            List<MeasureResult.CareGap> careGaps) {
        // Look for screening in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        JsonNode observations = getObservations(tenantId, patientId,
            String.join(",", domainCodes), dateFilter);

        List<JsonNode> screeningObs = getEntries(observations);

        if (screeningObs.isEmpty()) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_SDOH_SCREENING_" + domainName.toUpperCase().replace(" ", "_"))
                .description("No " + domainName + " screening documented in the past 12 months")
                .recommendedAction("Complete SDOH screening for " + domainName + " domain using AHC-HRSN or PRAPARE tool")
                .priority("high")
                .dueDate(LocalDate.now().plusDays(30))
                .build());
            return false;
        }

        return true;
    }

    /**
     * Check if patient is in hospice care.
     */
    private boolean isInHospice(String tenantId, String patientId) {
        JsonNode encounters = getEncounters(tenantId, patientId, "183919006", null);
        return !getEntries(encounters).isEmpty();
    }
}
