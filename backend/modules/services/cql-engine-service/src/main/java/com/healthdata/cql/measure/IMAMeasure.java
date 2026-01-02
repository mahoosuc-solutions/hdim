package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * IMA - Immunization for Adolescents (HEDIS)
 *
 * Evaluates adolescent immunizations by 13th birthday:
 * - Meningococcal conjugate (MenACWY) - 1 dose
 * - Tdap (Tetanus, diphtheria, pertussis) - 1 dose
 * - HPV (Human papillomavirus) - 3 doses
 */
@Component
public class IMAMeasure extends AbstractHedisMeasure {

    // CVX codes for vaccines
    private static final List<String> MENINGOCOCCAL_CVX_CODES = Arrays.asList(
        "114", // MenACWY-CRM
        "136", // MenACWY-D
        "147", // MenACWY-TT
        "103"  // Meningococcal polysaccharide
    );

    private static final List<String> TDAP_CVX_CODES = Arrays.asList(
        "115", // Tdap
        "113"  // Td (adult)
    );

    private static final List<String> HPV_CVX_CODES = Arrays.asList(
        "62",  // HPV quadrivalent
        "137", // HPV 9-valent
        "165"  // HPV 9-valent, 2 dose
    );

    @Override
    public String getMeasureId() {
        return "IMA";
    }

    @Override
    public String getMeasureName() {
        return "Immunization for Adolescents";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'IMA-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating IMA measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 13)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for Meningococcal vaccine (1 dose by 13th birthday)
        JsonNode menImmunizations = searchImmunizationsByCVX(tenantId, patientId, MENINGOCOCCAL_CVX_CODES);
        boolean hasMeningococcal = !getEntries(menImmunizations).isEmpty();

        // Check for Tdap vaccine (1 dose by 13th birthday)
        JsonNode tdapImmunizations = searchImmunizationsByCVX(tenantId, patientId, TDAP_CVX_CODES);
        boolean hasTdap = !getEntries(tdapImmunizations).isEmpty();

        // Check for HPV vaccine (need to evaluate series completion)
        JsonNode hpvImmunizations = searchImmunizationsByCVX(tenantId, patientId, HPV_CVX_CODES);
        List<JsonNode> hpvDoses = getEntries(hpvImmunizations);
        boolean hasCompleteHPV = hpvDoses.size() >= 2; // 2-3 doses depending on age at first dose

        // Patient is in numerator if all three vaccines are complete
        boolean inNumerator = hasMeningococcal && hasTdap && hasCompleteHPV;
        resultBuilder.inNumerator(inNumerator);

        // Calculate compliance
        int vaccinesCompleted = 0;
        if (hasMeningococcal) vaccinesCompleted++;
        if (hasTdap) vaccinesCompleted++;
        if (hasCompleteHPV) vaccinesCompleted++;

        double complianceRate = vaccinesCompleted / 3.0;
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Identify care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!hasMeningococcal) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_MENINGOCOCCAL")
                .description("Meningococcal conjugate vaccine not administered")
                .recommendedAction("Administer MenACWY vaccine")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }
        if (!hasTdap) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_TDAP")
                .description("Tdap vaccine not administered")
                .recommendedAction("Administer Tdap vaccine")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }
        if (!hasCompleteHPV) {
            String hpvGapDescription = hpvDoses.isEmpty()
                ? "HPV vaccine series not started"
                : String.format("HPV vaccine series incomplete (%d of 2-3 doses)", hpvDoses.size());
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INCOMPLETE_HPV")
                .description(hpvGapDescription)
                .recommendedAction("Continue/complete HPV vaccine series")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(2))
                .build());
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "hasMeningococcal", hasMeningococcal,
            "hasTdap", hasTdap,
            "hpvDosesReceived", hpvDoses.size(),
            "hasCompleteHPV", hasCompleteHPV
        ));

        resultBuilder.evidence(java.util.Map.of(
            "menDoses", hasMeningococcal ? 1 : 0,
            "tdapDoses", hasTdap ? 1 : 0,
            "hpvDoses", hpvDoses.size()
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        // Eligible if patient is 13 years old (turning 13 is the measurement point)
        return age != null && age >= 13 && age <= 13;
    }

    /**
     * Search immunizations by CVX codes
     */
    private JsonNode searchImmunizationsByCVX(String tenantId, UUID patientId, List<String> cvxCodes) {
        try {
            // Build vaccine-code parameter with CVX codes
            String vaccineCodeParam = cvxCodes.stream()
                .map(code -> "http://hl7.org/fhir/sid/cvx|" + code)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

            String immunizationsJson = fhirClient.searchImmunizations(tenantId, patientId, vaccineCodeParam, null);
            return objectMapper.readTree(immunizationsJson);
        } catch (Exception e) {
            logger.error("Error fetching immunizations for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }
}
