package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * URI - Appropriate Treatment for Upper Respiratory Infection (HEDIS)
 *
 * Evaluates whether patients diagnosed with URI did NOT receive inappropriate antibiotic treatment.
 * This is an "inverse" measure - higher scores indicate FEWER inappropriate antibiotic prescriptions.
 *
 * Antibiotics are generally NOT indicated for viral URIs.
 */
@Component
public class URIMeasure extends AbstractHedisMeasure {

    private static final List<String> URI_CODES = Arrays.asList(
        "54150009",  // Upper respiratory infection (SNOMED)
        "82272006",  // Common cold (SNOMED)
        "195662009", // Acute pharyngitis (SNOMED)
        "195651005", // Acute laryngitis (SNOMED)
        "15805002",  // Acute tracheitis (SNOMED)
        "70036007",  // Acute rhinitis (SNOMED)
        "54398005"   // Acute sinusitis (SNOMED) - Note: May sometimes require antibiotics
    );

    private static final List<String> INAPPROPRIATE_ANTIBIOTIC_CODES = Arrays.asList(
        "723",       // RxNorm - Amoxicillin
        "1596450",   // RxNorm - Amoxicillin/Clavulanate
        "203563",    // RxNorm - Azithromycin
        "2193",      // RxNorm - Cefdinir
        "1668240",   // RxNorm - Cephalexin
        "21212",     // RxNorm - Ciprofloxacin
        "3640",      // RxNorm - Clarithromycin
        "4419",      // RxNorm - Doxycycline
        "27437",     // RxNorm - Levofloxacin
        "7213"       // RxNorm - Trimethoprim/Sulfamethoxazole
    );

    // Complicating conditions that MIGHT justify antibiotics
    private static final List<String> COMPLICATING_CONDITION_CODES = Arrays.asList(
        "233604007", // Pneumonia (SNOMED)
        "36971009",  // Sinusitis with complications (SNOMED)
        "43878008",  // Streptococcal pharyngitis (SNOMED)
        "10351000119108" // Acute bacterial sinusitis (SNOMED)
    );

    @Override
    public String getMeasureId() {
        return "URI";
    }

    @Override
    public String getMeasureName() {
        return "Appropriate Treatment for Upper Respiratory Infection";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'URI-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating URI measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 3+ with URI diagnosis in last 3 months)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find most recent URI episode
        String uriDateFilter = "ge" + LocalDate.now().minusMonths(3).toString();
        JsonNode uriConditions = getConditions(tenantId, patientId,
            String.join(",", URI_CODES));
        List<JsonNode> uriEntries = getEntries(uriConditions);

        if (uriEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No URI diagnosis found in last 3 months")
                .build();
        }

        // Get URI diagnosis date
        JsonNode mostRecentURI = uriEntries.get(0);
        String uriDateStr = getEffectiveDate(mostRecentURI);
        LocalDate uriDate = LocalDate.parse(uriDateStr);

        // Check for complicating conditions that might justify antibiotics
        JsonNode complicatingConditions = getConditions(tenantId, patientId,
            String.join(",", COMPLICATING_CONDITION_CODES));
        boolean hasComplicatingCondition = !getEntries(complicatingConditions).isEmpty();

        // Check for antibiotic prescriptions within 3 days of URI diagnosis
        LocalDate antibioticWindowEnd = uriDate.plusDays(3);
        String antibioticDateFilter = "ge" + uriDate.toString() + "&date=le" + antibioticWindowEnd.toString();
        JsonNode antibiotics = getMedicationRequests(tenantId, patientId,
            String.join(",", INAPPROPRIATE_ANTIBIOTIC_CODES), antibioticDateFilter);
        boolean receivedAntibiotics = !getEntries(antibiotics).isEmpty();

        // Inverse measure: Patient is in numerator if they did NOT receive inappropriate antibiotics
        // If they have complicating condition, antibiotics might be appropriate (excluded from numerator calc)
        boolean appropriateTreatment = !receivedAntibiotics || hasComplicatingCondition;

        resultBuilder.inNumerator(appropriateTreatment);
        resultBuilder.complianceRate(appropriateTreatment ? 1.0 : 0.0);
        resultBuilder.score(appropriateTreatment ? 100.0 : 0.0);

        if (receivedAntibiotics && !hasComplicatingCondition) {
            // Inappropriate antibiotic prescription - care gap
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("INAPPROPRIATE_ANTIBIOTIC_USE")
                    .description(String.format("Antibiotic prescribed for URI without documented bacterial complication (%s)", uriDateStr))
                    .recommendedAction("Review antibiotic stewardship guidelines - consider symptomatic treatment for viral URIs")
                    .priority("medium")
                    .dueDate(LocalDate.now().plusWeeks(1))
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "uriDate", uriDateStr,
                "receivedAntibiotics", true,
                "hasComplicatingCondition", false,
                "appropriateTreatment", false,
                "treatmentType", "Antibiotic (inappropriate)"
            ));
        } else {
            String treatmentType = !receivedAntibiotics ? "Symptomatic (appropriate)" :
                                  "Antibiotic (justified by complication)";

            resultBuilder.details(java.util.Map.of(
                "uriDate", uriDateStr,
                "receivedAntibiotics", receivedAntibiotics,
                "hasComplicatingCondition", hasComplicatingCondition,
                "appropriateTreatment", true,
                "treatmentType", treatmentType
            ));
        }

        resultBuilder.evidence(java.util.Map.of(
            "uriDiagnosisDate", uriDateStr,
            "antibioticPrescribed", receivedAntibiotics,
            "complicatingCondition", hasComplicatingCondition
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 3) {
            return false;
        }

        // Must have URI diagnosis in last 3 months
        String uriDateFilter = "ge" + LocalDate.now().minusMonths(3).toString();
        JsonNode uriConditions = getConditions(tenantId, patientId,
            String.join(",", URI_CODES));

        return !getEntries(uriConditions).isEmpty();
    }
}
