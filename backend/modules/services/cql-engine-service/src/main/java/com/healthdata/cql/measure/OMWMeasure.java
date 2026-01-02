package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * OMW - Osteoporosis Management in Women Who Had a Fracture (HEDIS)
 *
 * Evaluates whether women aged 67-85 who suffered a fracture received:
 * - Bone mineral density (BMD) test, or
 * - Osteoporosis medication (bisphosphonates or other)
 * within 6 months of fracture
 */
@Component
public class OMWMeasure extends AbstractHedisMeasure {

    private static final List<String> FRACTURE_CODES = Arrays.asList(
        "263102004", // Fracture (SNOMED)
        "125605004", // Fracture of bone (SNOMED)
        "71642004",  // Hip fracture (SNOMED)
        "46866001",  // Fracture of vertebral column (SNOMED)
        "52329006",  // Fracture of wrist (SNOMED)
        "263204007", // Fracture of forearm (SNOMED)
        "239108004"  // Fracture of humerus (SNOMED)
    );

    private static final List<String> BMD_TEST_CODES = Arrays.asList(
        "24701-5",   // Femur DXA BMD (LOINC)
        "80948-3",   // DXA Femur and Hip BMD (LOINC)
        "38265-5",   // DXA Spine BMD (LOINC)
        "24966-4",   // Lumbar spine DXA BMD (LOINC)
        "46278-8",   // BMD area density (LOINC)
        "80947-5"    // DXA Forearm BMD (LOINC)
    );

    private static final List<String> OSTEOPOROSIS_MEDICATION_CODES = Arrays.asList(
        "10179",     // RxNorm - Alendronate
        "215573",    // RxNorm - Risedronate
        "836092",    // RxNorm - Ibandronate
        "282673",    // RxNorm - Zoledronic acid
        "203131",    // RxNorm - Raloxifene
        "25076",     // RxNorm - Calcitonin
        "1043562",   // RxNorm - Denosumab
        "996583"     // RxNorm - Teriparatide
    );

    @Override
    public String getMeasureId() {
        return "OMW";
    }

    @Override
    public String getMeasureName() {
        return "Osteoporosis Management in Women Who Had a Fracture";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'OMW-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating OMW measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be female, age 67-85 with fracture in last 6 months)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find most recent fracture in last 6 months
        String fractureDateFilter = "ge" + LocalDate.now().minusMonths(6).toString();
        JsonNode fractureConditions = getConditions(tenantId, patientId,
            String.join(",", FRACTURE_CODES));
        List<JsonNode> fractures = getEntries(fractureConditions);

        if (fractures.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No fracture found in last 6 months")
                .build();
        }

        // Get fracture date
        JsonNode mostRecentFracture = fractures.get(0);
        String fractureDateStr = getEffectiveDate(mostRecentFracture);
        LocalDate fractureDate = LocalDate.parse(fractureDateStr);

        // Check for BMD test after fracture (within 6 months)
        String bmdDateFilter = "ge" + fractureDate.toString();
        JsonNode bmdTests = getObservations(tenantId, patientId,
            String.join(",", BMD_TEST_CODES), bmdDateFilter);
        boolean hasBMDTest = !getEntries(bmdTests).isEmpty();

        // Check for osteoporosis medication after fracture (within 6 months)
        String medDateFilter = "ge" + fractureDate.toString();
        JsonNode osteoporosisMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", OSTEOPOROSIS_MEDICATION_CODES), medDateFilter);
        boolean hasOsteoporosisMedication = !getEntries(osteoporosisMeds).isEmpty();

        // Patient is in numerator if they had EITHER BMD test OR medication
        boolean inNumerator = hasBMDTest || hasOsteoporosisMedication;
        resultBuilder.inNumerator(inNumerator);
        resultBuilder.complianceRate(inNumerator ? 1.0 : 0.0);
        resultBuilder.score(inNumerator ? 100.0 : 0.0);

        if (inNumerator) {
            String managementType = hasBMDTest ? "BMD Test" : "Osteoporosis Medication";
            if (hasBMDTest && hasOsteoporosisMedication) {
                managementType = "Both BMD Test and Medication";
            }

            resultBuilder.evidence(java.util.Map.of(
                "fractureDate", fractureDateStr,
                "hasBMDTest", hasBMDTest,
                "hasOsteoporosisMedication", hasOsteoporosisMedication,
                "managementType", managementType
            ));

            resultBuilder.details(java.util.Map.of(
                "osteoporosisManagement", true,
                "fractureDate", fractureDateStr,
                "managementType", managementType
            ));
        } else {
            // Care gap - no management after fracture
            LocalDate managementDueDate = fractureDate.plusMonths(6);
            boolean isOverdue = LocalDate.now().isAfter(managementDueDate);

            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_OSTEOPOROSIS_MANAGEMENT")
                    .description(String.format("No osteoporosis management after fracture on %s", fractureDateStr))
                    .recommendedAction("Order BMD test or prescribe osteoporosis medication (bisphosphonate, denosumab, or teriparatide)")
                    .priority("high")
                    .dueDate(isOverdue ? LocalDate.now().plusWeeks(1) : managementDueDate)
                    .build()
            ));

            resultBuilder.details(java.util.Map.of(
                "osteoporosisManagement", false,
                "fractureDate", fractureDateStr,
                "managementDueDate", managementDueDate.toString(),
                "isOverdue", isOverdue
            ));
        }

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be female
        boolean isFemale = false;
        if (patient.has("gender")) {
            String gender = patient.get("gender").asText();
            isFemale = gender.equalsIgnoreCase("female");
        }

        if (!isFemale) return false;

        // Age 67-85
        Integer age = getPatientAge(patient);
        if (age == null || age < 67 || age > 85) {
            return false;
        }

        // Must have fracture in last 6 months
        String fractureDateFilter = "ge" + LocalDate.now().minusMonths(6).toString();
        JsonNode fractureConditions = getConditions(tenantId, patientId,
            String.join(",", FRACTURE_CODES));

        return !getEntries(fractureConditions).isEmpty();
    }
}
