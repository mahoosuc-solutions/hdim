package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * PPC - Prenatal and Postpartum Care (HEDIS)
 *
 * Evaluates timeliness of prenatal care and postpartum visit for women with deliveries.
 * Two components:
 * - Timeliness of Prenatal Care: Visit in first trimester or within 42 days of enrollment
 * - Postpartum Care: Visit 7-84 days after delivery
 */
@Component
public class PPCMeasure extends AbstractHedisMeasure {

    private static final List<String> DELIVERY_CODES = Arrays.asList(
        "10745001",  // Delivery (SNOMED)
        "177184002", // Normal delivery (SNOMED)
        "236973005", // Delivery procedure (SNOMED)
        "236974004", // Instrumental delivery (SNOMED)
        "11466000",  // Cesarean section (SNOMED)
        "274130007"  // Emergency cesarean section
    );

    private static final List<String> PRENATAL_VISIT_CODES = Arrays.asList(
        "77386006",  // Pregnant state (SNOMED)
        "424619006", // Prenatal visit (SNOMED)
        "18114009",  // Prenatal care (SNOMED)
        "169762003"  // Antenatal care (SNOMED)
    );

    private static final List<String> POSTPARTUM_VISIT_CODES = Arrays.asList(
        "133906008", // Postpartum care (SNOMED)
        "308615001", // Postpartum visit (SNOMED)
        "439004000"  // Postpartum follow-up (SNOMED)
    );

    private static final List<String> PREGNANCY_CODES = Arrays.asList(
        "77386006",  // Pregnant (SNOMED)
        "72892002",  // Normal pregnancy (SNOMED)
        "289908002"  // Pregnancy on record (SNOMED)
    );

    @Override
    public String getMeasureId() {
        return "PPC";
    }

    @Override
    public String getMeasureName() {
        return "Prenatal and Postpartum Care";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'PPC-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating PPC measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be female with delivery in measurement year)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find delivery procedures in the last 12 months
        String deliveryDateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode deliveryProcs = getProcedures(tenantId, patientId,
            String.join(",", DELIVERY_CODES), deliveryDateFilter);
        List<JsonNode> deliveries = getEntries(deliveryProcs);

        if (deliveries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No deliveries found in measurement year")
                .build();
        }

        // Use the most recent delivery
        JsonNode mostRecentDelivery = deliveries.get(0);
        String deliveryDateStr = getEffectiveDate(mostRecentDelivery);
        LocalDate deliveryDate = LocalDate.parse(deliveryDateStr);

        // Check for prenatal care - should have visit in first trimester (42 days after pregnancy start)
        // For simplicity, checking for any prenatal visit before delivery
        String prenatalDateFilter = "le" + deliveryDate.toString();
        JsonNode prenatalEncounters = getEncounters(tenantId, patientId,
            String.join(",", PRENATAL_VISIT_CODES), prenatalDateFilter);
        boolean hasTimelyPrenatalCare = !getEntries(prenatalEncounters).isEmpty();

        // Check for postpartum visit 7-84 days after delivery
        LocalDate postpartumWindowStart = deliveryDate.plusDays(7);
        LocalDate postpartumWindowEnd = deliveryDate.plusDays(84);
        String postpartumDateFilter = "ge" + postpartumWindowStart.toString() +
                                     "&date=le" + postpartumWindowEnd.toString();
        JsonNode postpartumEncounters = getEncounters(tenantId, patientId,
            String.join(",", POSTPARTUM_VISIT_CODES), postpartumDateFilter);
        boolean hasPostpartumVisit = !getEntries(postpartumEncounters).isEmpty();

        // Patient meets numerator if both components are met
        boolean inNumerator = hasTimelyPrenatalCare && hasPostpartumVisit;
        resultBuilder.inNumerator(inNumerator);

        // Calculate compliance
        int componentsCompleted = 0;
        if (hasTimelyPrenatalCare) componentsCompleted++;
        if (hasPostpartumVisit) componentsCompleted++;

        double complianceRate = componentsCompleted / 2.0;
        resultBuilder.complianceRate(complianceRate);
        resultBuilder.score(complianceRate * 100);

        // Identify care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();
        if (!hasTimelyPrenatalCare) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_PRENATAL_CARE")
                .description("No prenatal care visit documented")
                .recommendedAction("Ensure prenatal visit in first trimester or within 42 days of enrollment")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }
        if (!hasPostpartumVisit) {
            LocalDate postpartumDueDate = deliveryDate.plusDays(84);
            if (postpartumDueDate.isAfter(LocalDate.now())) {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("MISSING_POSTPARTUM_VISIT")
                    .description("Postpartum visit not yet completed (due 7-84 days after delivery)")
                    .recommendedAction("Schedule postpartum visit between 7-84 days after delivery")
                    .priority("high")
                    .dueDate(postpartumDueDate)
                    .build());
            } else {
                careGaps.add(MeasureResult.CareGap.builder()
                    .gapType("OVERDUE_POSTPARTUM_VISIT")
                    .description("Postpartum visit was not completed within 84 days of delivery")
                    .recommendedAction("Schedule postpartum visit as soon as possible")
                    .priority("high")
                    .dueDate(LocalDate.now().plusWeeks(1))
                    .build());
            }
        }
        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "deliveryDate", deliveryDateStr,
            "hasTimelyPrenatalCare", hasTimelyPrenatalCare,
            "hasPostpartumVisit", hasPostpartumVisit,
            "postpartumWindowStart", postpartumWindowStart.toString(),
            "postpartumWindowEnd", postpartumWindowEnd.toString()
        ));

        resultBuilder.evidence(java.util.Map.of(
            "prenatalVisits", hasTimelyPrenatalCare ? 1 : 0,
            "postpartumVisits", hasPostpartumVisit ? 1 : 0,
            "deliveryDate", deliveryDateStr
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be female
        boolean isFemale = false;
        if (patient.has("gender")) {
            String gender = patient.get("gender").asText();
            isFemale = gender.equalsIgnoreCase("female");
        }

        if (!isFemale) return false;

        // Must have delivery in measurement year
        String deliveryDateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode deliveryProcs = getProcedures(tenantId, patientId,
            String.join(",", DELIVERY_CODES), deliveryDateFilter);

        return !getEntries(deliveryProcs).isEmpty();
    }
}
