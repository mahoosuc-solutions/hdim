package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * SDOH-2 - Social Determinants of Health Screen Positive Rate
 *
 * CMS Measure ID: CMS1239v1 (2024 APP Plus)
 * APP Plus 2028: Required measure for ACO performance
 *
 * Measure Description:
 * Percentage of patients 18 years of age and older who screened positive
 * for at least one health-related social need (HRSN) during SDOH screening.
 *
 * Eligible Population:
 * - Ages 18 years and older
 * - Completed SDOH screening (all 5 domains) during measurement period
 *
 * Numerator:
 * - Patients with a positive screen result in at least one HRSN domain
 *
 * Five HRSN Domains:
 * 1. Food Insecurity
 * 2. Housing Instability
 * 3. Transportation Needs
 * 4. Utility Difficulties
 * 5. Interpersonal Safety
 *
 * Note: This measure tracks prevalence of social needs among screened patients
 * to help organizations understand community needs and resource allocation.
 */
@Component
public class SDOH2Measure extends AbstractHedisMeasure {

    // LOINC codes for Food Insecurity positive results
    private static final List<String> FOOD_INSECURITY_POSITIVE_CODES = Arrays.asList(
        "LA28397-0",  // At risk for food insecurity
        "LA19952-3",  // Often true (food insecurity)
        "LA28398-8"   // Food insecurity
    );

    // LOINC codes for Housing Instability positive results
    private static final List<String> HOUSING_POSITIVE_CODES = Arrays.asList(
        "LA31993-1",  // I have a steady place to live, but I am worried about losing it
        "LA31994-9",  // I do not have a steady place to live
        "LA28580-1"   // At risk for housing instability
    );

    // LOINC codes for Transportation positive results
    private static final List<String> TRANSPORTATION_POSITIVE_CODES = Arrays.asList(
        "LA33-6",     // Yes (transportation barrier)
        "LA31980-8"   // Yes, it has kept me from medical appointments
    );

    // LOINC codes for Utility Difficulties positive results
    private static final List<String> UTILITY_POSITIVE_CODES = Arrays.asList(
        "LA33-6",     // Yes (utility difficulty)
        "LA31983-2"   // Already shut off
    );

    // LOINC codes for Interpersonal Safety positive results
    private static final List<String> SAFETY_POSITIVE_CODES = Arrays.asList(
        "LA33-6",     // Yes (safety concern)
        "LA15173-0",  // Fairly often
        "LA10137-0"   // Very often
    );

    // LOINC codes for SDOH screening (to check if screening was completed)
    private static final List<String> SDOH_SCREENING_CODES = Arrays.asList(
        "88122-7",  // Food insecurity
        "71802-3",  // Housing status
        "93030-5",  // Transportation needs
        "93031-3",  // Utility needs
        "93038-8"   // Interpersonal safety
    );

    // All positive indicator codes combined
    private static final List<String> ALL_POSITIVE_CODES;
    static {
        ALL_POSITIVE_CODES = new ArrayList<>();
        ALL_POSITIVE_CODES.addAll(FOOD_INSECURITY_POSITIVE_CODES);
        ALL_POSITIVE_CODES.addAll(HOUSING_POSITIVE_CODES);
        ALL_POSITIVE_CODES.addAll(TRANSPORTATION_POSITIVE_CODES);
        ALL_POSITIVE_CODES.addAll(UTILITY_POSITIVE_CODES);
        ALL_POSITIVE_CODES.addAll(SAFETY_POSITIVE_CODES);
    }

    @Override
    public String getMeasureId() {
        return "SDOH-2";
    }

    @Override
    public String getMeasureName() {
        return "Social Determinants of Health Screen Positive Rate";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'SDOH2-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating SDOH-2 measure for patient: {}", patientId);

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

        // Check for exclusions
        if (isInHospice(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient is in hospice care")
                .build();
        }

        // Check if patient completed SDOH screening (denominator)
        if (!hasCompletedSdohScreening(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient has not completed SDOH screening for all 5 domains")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Evaluate each HRSN domain for positive results
        List<MeasureResult.CareGap> careGaps = new ArrayList<>();
        int domainsPositive = 0;
        List<String> positivedomains = new ArrayList<>();

        boolean foodPositive = evaluateDomainPositive(tenantId, patientId,
            "88122-7", FOOD_INSECURITY_POSITIVE_CODES, "Food Insecurity");
        if (foodPositive) {
            domainsPositive++;
            positivedomains.add("Food Insecurity");
        }

        boolean housingPositive = evaluateDomainPositive(tenantId, patientId,
            "71802-3", HOUSING_POSITIVE_CODES, "Housing Instability");
        if (housingPositive) {
            domainsPositive++;
            positivedomains.add("Housing Instability");
        }

        boolean transportPositive = evaluateDomainPositive(tenantId, patientId,
            "93030-5", TRANSPORTATION_POSITIVE_CODES, "Transportation");
        if (transportPositive) {
            domainsPositive++;
            positivedomains.add("Transportation");
        }

        boolean utilityPositive = evaluateDomainPositive(tenantId, patientId,
            "93031-3", UTILITY_POSITIVE_CODES, "Utility Difficulties");
        if (utilityPositive) {
            domainsPositive++;
            positivedomains.add("Utility Difficulties");
        }

        boolean safetyPositive = evaluateDomainPositive(tenantId, patientId,
            "93038-8", SAFETY_POSITIVE_CODES, "Interpersonal Safety");
        if (safetyPositive) {
            domainsPositive++;
            positivedomains.add("Interpersonal Safety");
        }

        // Create care gaps for positive screens without interventions
        if (domainsPositive > 0) {
            createInterventionCareGaps(tenantId, patientId, positivedomains, careGaps);
        }

        if (!careGaps.isEmpty()) {
            resultBuilder.careGaps(careGaps);
        }

        // Patient is in numerator if ANY domain screened positive
        boolean inNumerator = domainsPositive > 0;
        double positiveRate = domainsPositive / 5.0;

        resultBuilder
            .inNumerator(inNumerator)
            .complianceRate(positiveRate)
            .score(positiveRate * 100)
            .evidence(java.util.Map.of(
                "domainsPositive", domainsPositive,
                "totalDomains", 5,
                "positiveDomains", positivedomains
            ))
            .details(java.util.Map.of(
                "foodInsecurityPositive", foodPositive,
                "housingInstabilityPositive", housingPositive,
                "transportationPositive", transportPositive,
                "utilityDifficultiesPositive", utilityPositive,
                "interpersonalSafetyPositive", safetyPositive,
                "anyDomainPositive", inNumerator,
                "totalPositiveDomains", domainsPositive
            ));

        MeasureResult result = resultBuilder.build();
        logger.info("SDOH-2 evaluation complete for patient {}: positive={}/5, inNumerator={}",
            patientId, domainsPositive, result.isInNumerator());

        return result;
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);
        return age != null && age >= 18;
    }

    /**
     * Check if patient has completed SDOH screening for all 5 domains.
     */
    private boolean hasCompletedSdohScreening(String tenantId, UUID patientId) {
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        int domainsCompleted = 0;

        // Check each domain
        for (String domainCode : SDOH_SCREENING_CODES) {
            JsonNode observations = getObservations(tenantId, patientId, domainCode, dateFilter);
            if (!getEntries(observations).isEmpty()) {
                domainsCompleted++;
            }
        }

        return domainsCompleted >= 5;
    }

    /**
     * Evaluate if a specific HRSN domain has a positive screen result.
     */
    private boolean evaluateDomainPositive(String tenantId, UUID patientId,
                                            String screeningCode, List<String> positiveCodes,
                                            String domainName) {
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        JsonNode observations = getObservations(tenantId, patientId, screeningCode, dateFilter);
        List<JsonNode> screeningObs = getEntries(observations);

        for (JsonNode obs : screeningObs) {
            // Check if the value indicates a positive result
            if (obs.has("valueCodeableConcept")) {
                JsonNode valueCoding = obs.get("valueCodeableConcept");
                if (valueCoding.has("coding")) {
                    for (JsonNode coding : valueCoding.get("coding")) {
                        String code = coding.has("code") ? coding.get("code").asText() : "";
                        if (positiveCodes.contains(code)) {
                            logger.debug("Positive {} screen found for patient {}", domainName, patientId);
                            return true;
                        }
                    }
                }
            }

            // Also check for numeric scores indicating positive
            if (obs.has("valueQuantity")) {
                // For certain screening tools, a score above threshold indicates positive
                double score = obs.get("valueQuantity").has("value")
                    ? obs.get("valueQuantity").get("value").asDouble() : 0;

                // AHC-HRSN typically uses Yes/No, but some tools use scores
                if (score >= 1) {
                    logger.debug("Positive {} screen (score) found for patient {}", domainName, patientId);
                    return true;
                }
            }

            // Check for boolean true responses
            if (obs.has("valueBoolean") && obs.get("valueBoolean").asBoolean()) {
                logger.debug("Positive {} screen (boolean) found for patient {}", domainName, patientId);
                return true;
            }
        }

        return false;
    }

    /**
     * Create care gaps for positive screens that lack documented interventions.
     */
    private void createInterventionCareGaps(String tenantId, UUID patientId,
                                             List<String> positiveDomains,
                                             List<MeasureResult.CareGap> careGaps) {
        for (String domain : positiveDomains) {
            // Check if intervention is documented for this domain
            if (!hasDocumentedIntervention(tenantId, patientId, domain)) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("MISSING_SDOH_INTERVENTION_" + domain.toUpperCase().replace(" ", "_"))
                    .description("Positive " + domain + " screen without documented intervention")
                    .recommendedAction("Document intervention or referral for " + domain + " need")
                    .priority("high")
                    .dueDate(LocalDate.now().plusDays(14))
                    .build());
            }
        }
    }

    /**
     * Check if patient has documented intervention for a specific SDOH domain.
     */
    private boolean hasDocumentedIntervention(String tenantId, UUID patientId, String domain) {
        // Look for referrals, service requests, or documented interventions
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        // Check for SDOH intervention procedure codes (Z-codes related interventions)
        // These would be ServiceRequest or Procedure resources
        JsonNode procedures = getProcedures(tenantId, patientId, getInterventionCode(domain), dateFilter);

        return !getEntries(procedures).isEmpty();
    }

    /**
     * Get intervention SNOMED code for a given domain.
     */
    private String getInterventionCode(String domain) {
        return switch (domain) {
            case "Food Insecurity" -> "710824005";      // Assessment of food security status
            case "Housing Instability" -> "710971000";  // Referral to housing assistance program
            case "Transportation" -> "410207009";       // Referral to transportation assistance
            case "Utility Difficulties" -> "710972007"; // Referral to utility assistance program
            case "Interpersonal Safety" -> "225337009"; // Referral to social services
            default -> "";
        };
    }

    /**
     * Get procedures for a patient.
     */
    protected JsonNode getProcedures(String tenantId, UUID patientId, String code, String date) {
        try {
            String proceduresJson = fhirClient.searchProcedures(tenantId, patientId, code, date);
            return objectMapper.readTree(proceduresJson);
        } catch (Exception e) {
            logger.error("Error fetching procedures for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    /**
     * Check if patient is in hospice care.
     */
    private boolean isInHospice(String tenantId, UUID patientId) {
        JsonNode encounters = getEncounters(tenantId, patientId, "183919006", null);
        return !getEntries(encounters).isEmpty();
    }
}
