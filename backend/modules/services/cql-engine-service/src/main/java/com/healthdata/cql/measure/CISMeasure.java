package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * CIS - Childhood Immunization Status (HEDIS)
 *
 * Evaluates whether children turning 2 years old received all required immunizations by their second birthday.
 * Required vaccines:
 * - DTaP/DT: 4 doses
 * - IPV (Polio): 3 doses
 * - MMR: 1 dose
 * - HiB: 3 doses
 * - Hepatitis B: 3 doses
 * - VZV (Varicella): 1 dose
 * - Pneumococcal conjugate: 4 doses
 * - Hepatitis A: 1 dose
 * - Rotavirus: 2-3 doses
 * - Influenza: 2 doses
 */
@Component
public class CISMeasure extends AbstractHedisMeasure {

    // CVX codes for required vaccines
    private static final List<String> DTAP_CVX_CODES = Arrays.asList(
        "20",  // DTaP
        "106", // DTaP, 5 pertussis antigens
        "107", // DTaP, unspecified formulation
        "110", // DTaP-Hep B-IPV
        "50",  // DTaP-Hib
        "120", // DTaP-Hep B-IPV-Hib
        "130"  // DTaP-IPV
    );

    private static final List<String> IPV_CVX_CODES = Arrays.asList(
        "10",  // IPV
        "120", // DTaP-Hep B-IPV-Hib
        "110", // DTaP-Hep B-IPV
        "130"  // DTaP-IPV
    );

    private static final List<String> MMR_CVX_CODES = Arrays.asList(
        "03",  // MMR
        "94"   // MMRV
    );

    private static final List<String> HIB_CVX_CODES = Arrays.asList(
        "46",  // Hib (PRP-D)
        "47",  // Hib (HbOC)
        "48",  // Hib (PRP-T)
        "49",  // Hib (PRP-OMP)
        "50",  // DTaP-Hib
        "120"  // DTaP-Hep B-IPV-Hib
    );

    private static final List<String> HEP_B_CVX_CODES = Arrays.asList(
        "08",  // Hep B, adolescent or pediatric
        "110", // DTaP-Hep B-IPV
        "120"  // DTaP-Hep B-IPV-Hib
    );

    private static final List<String> VZV_CVX_CODES = Arrays.asList(
        "21",  // Varicella
        "94"   // MMRV
    );

    private static final List<String> PNEUMO_CVX_CODES = Arrays.asList(
        "133", // PCV13
        "152"  // PCV15
    );

    private static final List<String> HEP_A_CVX_CODES = Arrays.asList(
        "83",  // Hep A, pediatric
        "85"   // Hep A, unspecified formulation
    );

    private static final List<String> ROTAVIRUS_CVX_CODES = Arrays.asList(
        "116", // Rotavirus, pentavalent
        "119", // Rotavirus, monovalent
        "122"  // Rotavirus, unspecified formulation
    );

    private static final List<String> INFLUENZA_CVX_CODES = Arrays.asList(
        "135", // Influenza, high dose seasonal
        "140", // Influenza, seasonal, injectable, preservative free
        "141", // Influenza, seasonal, injectable
        "150", // Influenza, injectable, quadrivalent, preservative free
        "153", // Influenza, injectable, MDCK, preservative free
        "158", // Influenza, injectable, quadrivalent
        "161"  // Influenza, injectable, quadrivalent, preservative free, pediatric
    );

    @Override
    public String getMeasureId() {
        return "CIS";
    }

    @Override
    public String getMeasureName() {
        return "Childhood Immunization Status";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'CIS-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating CIS measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 2)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Get patient birthdate to check vaccines received by 2nd birthday
        JsonNode patient = getPatientData(tenantId, patientId);
        LocalDate birthDate = getBirthDate(patient);
        LocalDate secondBirthday = birthDate.plusYears(2);

        // Check each vaccine series
        int dtapDoses = countImmunizations(tenantId, patientId, DTAP_CVX_CODES, secondBirthday);
        int ipvDoses = countImmunizations(tenantId, patientId, IPV_CVX_CODES, secondBirthday);
        int mmrDoses = countImmunizations(tenantId, patientId, MMR_CVX_CODES, secondBirthday);
        int hibDoses = countImmunizations(tenantId, patientId, HIB_CVX_CODES, secondBirthday);
        int hepBDoses = countImmunizations(tenantId, patientId, HEP_B_CVX_CODES, secondBirthday);
        int vzvDoses = countImmunizations(tenantId, patientId, VZV_CVX_CODES, secondBirthday);
        int pneumoDoses = countImmunizations(tenantId, patientId, PNEUMO_CVX_CODES, secondBirthday);
        int hepADoses = countImmunizations(tenantId, patientId, HEP_A_CVX_CODES, secondBirthday);
        int rotavirusDoses = countImmunizations(tenantId, patientId, ROTAVIRUS_CVX_CODES, secondBirthday);
        int fluDoses = countImmunizations(tenantId, patientId, INFLUENZA_CVX_CODES, secondBirthday);

        // Check if series are complete
        boolean dtapComplete = dtapDoses >= 4;
        boolean ipvComplete = ipvDoses >= 3;
        boolean mmrComplete = mmrDoses >= 1;
        boolean hibComplete = hibDoses >= 3;
        boolean hepBComplete = hepBDoses >= 3;
        boolean vzvComplete = vzvDoses >= 1;
        boolean pneumoComplete = pneumoDoses >= 4;
        boolean hepAComplete = hepADoses >= 1;
        boolean rotavirusComplete = rotavirusDoses >= 2;
        boolean fluComplete = fluDoses >= 2;

        // Count completed series
        int seriesCompleted = 0;
        if (dtapComplete) seriesCompleted++;
        if (ipvComplete) seriesCompleted++;
        if (mmrComplete) seriesCompleted++;
        if (hibComplete) seriesCompleted++;
        if (hepBComplete) seriesCompleted++;
        if (vzvComplete) seriesCompleted++;
        if (pneumoComplete) seriesCompleted++;
        if (hepAComplete) seriesCompleted++;
        if (rotavirusComplete) seriesCompleted++;
        if (fluComplete) seriesCompleted++;

        // Patient is in numerator if ALL required series are complete
        boolean inNumerator = seriesCompleted == 10;
        resultBuilder.inNumerator(inNumerator);

        double complianceRate = seriesCompleted / 10.0;
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Identify care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        addVaccineCareGap(careGaps, "DTaP", dtapDoses, 4, dtapComplete);
        addVaccineCareGap(careGaps, "IPV (Polio)", ipvDoses, 3, ipvComplete);
        addVaccineCareGap(careGaps, "MMR", mmrDoses, 1, mmrComplete);
        addVaccineCareGap(careGaps, "HiB", hibDoses, 3, hibComplete);
        addVaccineCareGap(careGaps, "Hepatitis B", hepBDoses, 3, hepBComplete);
        addVaccineCareGap(careGaps, "Varicella", vzvDoses, 1, vzvComplete);
        addVaccineCareGap(careGaps, "Pneumococcal", pneumoDoses, 4, pneumoComplete);
        addVaccineCareGap(careGaps, "Hepatitis A", hepADoses, 1, hepAComplete);
        addVaccineCareGap(careGaps, "Rotavirus", rotavirusDoses, 2, rotavirusComplete);
        addVaccineCareGap(careGaps, "Influenza", fluDoses, 2, fluComplete);

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "seriesCompleted", seriesCompleted,
            "totalSeriesRequired", 10,
            "secondBirthday", secondBirthday.toString()
        ));

        resultBuilder.evidence(java.util.Map.of(
            "dtapDoses", dtapDoses,
            "ipvDoses", ipvDoses,
            "mmrDoses", mmrDoses,
            "hibDoses", hibDoses,
            "hepBDoses", hepBDoses,
            "vzvDoses", vzvDoses,
            "pneumoDoses", pneumoDoses,
            "hepADoses", hepADoses,
            "rotavirusDoses", rotavirusDoses,
            "fluDoses", fluDoses
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        // Eligible if patient is 2 years old
        return age != null && age >= 2 && age <= 2;
    }

    /**
     * Count immunizations received before the specified date
     */
    private int countImmunizations(String tenantId, String patientId, List<String> cvxCodes, LocalDate beforeDate) {
        try {
            String vaccineCodeParam = cvxCodes.stream()
                .map(code -> "http://hl7.org/fhir/sid/cvx|" + code)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

            String dateFilter = "le" + beforeDate.toString();
            String immunizationsJson = fhirClient.searchImmunizations(tenantId, patientId, vaccineCodeParam, dateFilter);
            JsonNode immunizations = objectMapper.readTree(immunizationsJson);
            return getEntries(immunizations).size();
        } catch (Exception e) {
            logger.error("Error counting immunizations for patient {}: {}", patientId, e.getMessage());
            return 0;
        }
    }

    /**
     * Add care gap for incomplete vaccine series
     */
    private void addVaccineCareGap(List<MeasureResult.CareGap> careGaps, String vaccineName,
                                   int currentDoses, int requiredDoses, boolean isComplete) {
        if (!isComplete) {
            String description = currentDoses == 0
                ? String.format("%s vaccine series not started", vaccineName)
                : String.format("%s vaccine series incomplete (%d of %d doses)", vaccineName, currentDoses, requiredDoses);

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INCOMPLETE_" + vaccineName.toUpperCase().replace(" ", "_") + "_SERIES")
                .description(description)
                .recommendedAction(String.format("Complete %s vaccine series (%d more dose(s) needed)",
                    vaccineName, requiredDoses - currentDoses))
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }
    }

    /**
     * Extract birth date from patient resource
     */
    private LocalDate getBirthDate(JsonNode patient) {
        if (patient != null && patient.has("birthDate")) {
            return LocalDate.parse(patient.get("birthDate").asText());
        }
        return null;
    }
}
