package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * CWP - Appropriate Testing for Pharyngitis (Children) (HEDIS)
 *
 * Evaluates whether children with pharyngitis received appropriate group A streptococcus
 * testing before antibiotics were dispensed.
 *
 * Appropriate testing = strep test within 3 days before or after antibiotic prescription
 * This measure promotes antibiotic stewardship and prevents unnecessary antibiotic use.
 */
@Component
public class CWPMeasure extends AbstractHedisMeasure {

    private static final List<String> PHARYNGITIS_CODES = Arrays.asList(
        "405737000", // SNOMED - Acute pharyngitis
        "363746003", // SNOMED - Acute bacterial pharyngitis
        "90979004",  // SNOMED - Streptococcal pharyngitis
        "195655000", // SNOMED - Acute streptococcal pharyngitis
        "43878008",  // SNOMED - Streptococcal sore throat
        "10351005"   // SNOMED - Acute pharyngitis (disorder)
    );

    private static final List<String> STREP_TEST_CODES = Arrays.asList(
        "6557-3",    // LOINC - Streptococcus pyogenes Ag [Presence] in Throat
        "17656-0",   // LOINC - Streptococcus pyogenes [Presence] in Throat by Organism specific culture
        "11268-0",   // LOINC - Streptococcus pyogenes Ag [Presence] in Throat by Rapid immunoassay
        "78012-2",   // LOINC - Streptococcus pyogenes DNA [Presence] in Throat by NAA with probe detection
        "68954-7",   // LOINC - Streptococcus pyogenes rRNA [Presence] in Throat by Probe
        "49610-9"    // LOINC - Streptococcus pyogenes [Identifier] in Throat by Organism specific culture
    );

    private static final List<String> ANTIBIOTIC_CODES = Arrays.asList(
        // Penicillins
        "7980",      // RxNorm - Penicillin V
        "723",       // RxNorm - Amoxicillin
        "19711",     // RxNorm - Amoxicillin / Clavulanate (Augmentin)
        "860092",    // RxNorm - Ampicillin
        // Cephalosporins
        "2180",      // RxNorm - Cefdinir (Omnicef)
        "3007",      // RxNorm - Cephalexin (Keflex)
        "2193",      // RxNorm - Cefuroxime (Ceftin)
        "20481",     // RxNorm - Cefprozil (Cefzil)
        "2193",      // RxNorm - Cefadroxil
        // Macrolides
        "1291",      // RxNorm - Azithromycin (Zithromax)
        "4053",      // RxNorm - Clarithromycin (Biaxin)
        "4337"       // RxNorm - Erythromycin
    );

    private static final List<String> OUTPATIENT_VISIT_CODES = Arrays.asList(
        "185349003", // SNOMED - Outpatient encounter
        "308335008", // SNOMED - Patient encounter procedure
        "390906007", // SNOMED - Follow-up encounter
        "185463005", // SNOMED - Office visit
        "185317003"  // SNOMED - Telephone encounter
    );

    @Override
    public String getMeasureId() {
        return "CWP";
    }

    @Override
    public String getMeasureName() {
        return "Appropriate Testing for Pharyngitis (Children)";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'CWP-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating CWP measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 3-17 with pharyngitis diagnosis and antibiotic prescription)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find pharyngitis episodes with antibiotic prescriptions in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        // Get antibiotic prescriptions
        JsonNode antibiotics = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIBIOTIC_CODES), dateFilter);
        List<JsonNode> antibioticEntries = getEntries(antibiotics);

        if (antibioticEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No antibiotic prescriptions found for pharyngitis")
                .build();
        }

        // Get most recent antibiotic prescription
        JsonNode mostRecentAntibiotic = antibioticEntries.get(0);
        String antibioticDate = getEffectiveDate(mostRecentAntibiotic);
        LocalDate rxDate = LocalDate.parse(antibioticDate);

        // Check for strep test within 3 days before or after antibiotic prescription
        LocalDate testWindowStart = rxDate.minusDays(3);
        LocalDate testWindowEnd = rxDate.plusDays(3);

        String testDateFilter = "ge" + testWindowStart.toString() + "&date=le" + testWindowEnd.toString();
        JsonNode strepTests = getObservations(tenantId, patientId,
            String.join(",", STREP_TEST_CODES), testDateFilter);
        List<JsonNode> strepTestEntries = getEntries(strepTests);

        boolean hasAppropriateTest = !strepTestEntries.isEmpty();
        String strepTestDate = hasAppropriateTest ? getEffectiveDate(strepTestEntries.get(0)) : null;

        // Extract test result if available
        String testResult = "Unknown";
        if (hasAppropriateTest) {
            try {
                JsonNode testNode = strepTestEntries.get(0);
                if (testNode.has("valueCodeableConcept")) {
                    JsonNode valueCode = testNode.get("valueCodeableConcept");
                    if (valueCode.has("text")) {
                        testResult = valueCode.get("text").asText();
                    } else if (valueCode.has("coding")) {
                        JsonNode codings = valueCode.get("coding");
                        if (codings.isArray() && codings.size() > 0) {
                            JsonNode coding = codings.get(0);
                            if (coding.has("display")) {
                                testResult = coding.get("display").asText();
                            }
                        }
                    }
                } else if (testNode.has("valueString")) {
                    testResult = testNode.get("valueString").asText();
                }
            } catch (Exception e) {
                logger.debug("Could not extract strep test result: {}", e.getMessage());
            }
        }

        resultBuilder.inNumerator(hasAppropriateTest);
        resultBuilder.complianceRate(hasAppropriateTest ? 1.0 : 0.0);
        resultBuilder.score(hasAppropriateTest ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasAppropriateTest) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_STREP_TEST_BEFORE_ANTIBIOTICS")
                .description(String.format("Antibiotic prescribed for pharyngitis (%s) without documented strep test", antibioticDate))
                .recommendedAction("Always perform rapid strep test or throat culture before prescribing antibiotics for pharyngitis")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("ANTIBIOTIC_STEWARDSHIP")
                .description("Inappropriate antibiotic prescribing increases resistance and side effects")
                .recommendedAction("Implement point-of-care strep testing; educate on viral vs bacterial pharyngitis differentiation")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());
        } else {
            // Test was done - good practice
            String resultLower = testResult.toLowerCase();
            if (resultLower.contains("negative") || resultLower.contains("not detected")) {
                // Negative test but still prescribed antibiotics - questionable
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("ANTIBIOTICS_WITH_NEGATIVE_STREP")
                    .description(String.format("Antibiotics prescribed despite negative strep test (result: %s)", testResult))
                    .recommendedAction("Review antibiotic prescribing practices; antibiotics not indicated for negative strep pharyngitis")
                    .priority("low")
                    .dueDate(LocalDate.now().plusMonths(3))
                    .build());
            }
        }

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "antibioticDate", antibioticDate,
            "hasStrepTest", hasAppropriateTest,
            "strepTestDate", strepTestDate != null ? strepTestDate : "No test",
            "testResult", testResult,
            "testWindowStart", testWindowStart.toString(),
            "testWindowEnd", testWindowEnd.toString(),
            "appropriateTesting", hasAppropriateTest
        ));

        resultBuilder.evidence(java.util.Map.of(
            "strepTestPerformed", hasAppropriateTest,
            "testResult", testResult,
            "appropriateAntibioticUse", hasAppropriateTest
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 3 || age > 17) {
            return false;
        }

        // Must have pharyngitis diagnosis in last 12 months
        JsonNode pharyngitisConditions = getConditions(tenantId, patientId,
            String.join(",", PHARYNGITIS_CODES));
        if (getEntries(pharyngitisConditions).isEmpty()) {
            return false;
        }

        // Must have antibiotic prescription in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode antibiotics = getMedicationRequests(tenantId, patientId,
            String.join(",", ANTIBIOTIC_CODES), dateFilter);

        return !getEntries(antibiotics).isEmpty();
    }
}
