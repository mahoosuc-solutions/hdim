package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * SPD - Statin Therapy for Patients with Cardiovascular Disease (HEDIS)
 *
 * Evaluates whether patients with cardiovascular disease are receiving statin therapy.
 * Eligible: Adults aged 21-75 with clinical atherosclerotic cardiovascular disease (ASCVD)
 * Target: Received statin therapy during measurement year
 */
@Component
public class SPDMeasure extends AbstractHedisMeasure {

    private static final List<String> ASCVD_CODES = Arrays.asList(
        "53741008",  // Coronary arteriosclerosis (SNOMED)
        "414545008", // Ischemic heart disease (SNOMED)
        "22298006",  // Myocardial infarction (SNOMED)
        "230690007", // Cerebrovascular accident (SNOMED)
        "413838009", // Chronic ischemic heart disease (SNOMED)
        "429559004", // Typical angina (SNOMED)
        "300995000", // Exercise-induced angina (SNOMED)
        "194828000", // Angina pectoris (SNOMED)
        "371039008", // Transient ischemic attack (SNOMED)
        "399211009", // History of myocardial infarction (SNOMED)
        "440140008"  // Acute coronary syndrome (SNOMED)
    );

    private static final List<String> REVASCULARIZATION_CODES = Arrays.asList(
        "81266008",  // Coronary artery bypass graft (SNOMED)
        "232717009", // Coronary angioplasty (SNOMED)
        "11101003",  // Percutaneous transluminal coronary angioplasty (SNOMED)
        "36969009",  // Placement of stent in coronary artery (SNOMED)
        "415070008"  // Percutaneous coronary intervention (SNOMED)
    );

    private static final List<String> PERIPHERAL_VASCULAR_CODES = Arrays.asList(
        "399957001", // Peripheral vascular disease (SNOMED)
        "440141007", // Atherosclerosis of peripheral artery (SNOMED)
        "413838009"  // Chronic limb ischemia (SNOMED)
    );

    private static final List<String> CAROTID_DISEASE_CODES = Arrays.asList(
        "64586002",  // Carotid artery stenosis (SNOMED)
        "300920004"  // Occlusion of carotid artery (SNOMED)
    );

    private static final List<String> STATIN_MEDICATION_CODES = Arrays.asList(
        "83367",     // RxNorm - Atorvastatin
        "36567",     // RxNorm - Simvastatin
        "41127",     // RxNorm - Pravastatin
        "42463",     // RxNorm - Rosuvastatin
        "6472",      // RxNorm - Lovastatin
        "40254",     // RxNorm - Fluvastatin
        "301542"     // RxNorm - Pitavastatin
    );

    @Override
    public String getMeasureId() {
        return "SPD";
    }

    @Override
    public String getMeasureName() {
        return "Statin Therapy for Patients with Cardiovascular Disease";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'SPD-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating SPD measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 21-75 with ASCVD)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for statin therapy in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();
        JsonNode statinMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", STATIN_MEDICATION_CODES), dateFilter);
        boolean hasStatinTherapy = !getEntries(statinMeds).isEmpty();

        boolean inNumerator = hasStatinTherapy;
        resultBuilder.inNumerator(inNumerator);
        resultBuilder.complianceRate(inNumerator ? 1.0 : 0.0);
        resultBuilder.score(inNumerator ? 100.0 : 0.0);

        if (hasStatinTherapy) {
            List<JsonNode> statinEntries = getEntries(statinMeds);
            String lastStatinDate = getEffectiveDate(statinEntries.get(0));
            resultBuilder.evidence(java.util.Map.of(
                "statinTherapy", true,
                "lastStatinPrescription", lastStatinDate,
                "statinMedicationCount", statinEntries.size()
            ));
            resultBuilder.details(java.util.Map.of(
                "onStatinTherapy", true,
                "lastPrescriptionDate", lastStatinDate
            ));
        } else {
            // Identify care gap
            resultBuilder.careGaps(List.of(
                MeasureResult.CareGap.builder()
                    .gapType("MISSING_STATIN_THERAPY")
                    .description("Patient with ASCVD not on statin therapy")
                    .recommendedAction("Initiate statin therapy (e.g., atorvastatin, simvastatin, or rosuvastatin)")
                    .priority("high")
                    .dueDate(LocalDate.now().plusWeeks(2))
                    .build()
            ));
            resultBuilder.details(java.util.Map.of(
                "onStatinTherapy", false,
                "hasASCVD", true
            ));
        }

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 21 || age > 75) {
            return false;
        }

        // Check for ASCVD diagnosis
        JsonNode ascvdConditions = getConditions(tenantId, patientId, String.join(",", ASCVD_CODES));
        if (!getEntries(ascvdConditions).isEmpty()) {
            return true;
        }

        // Check for revascularization procedures
        JsonNode revascProcs = getProcedures(tenantId, patientId, String.join(",", REVASCULARIZATION_CODES), null);
        if (!getEntries(revascProcs).isEmpty()) {
            return true;
        }

        // Check for peripheral vascular disease
        JsonNode pvdConditions = getConditions(tenantId, patientId, String.join(",", PERIPHERAL_VASCULAR_CODES));
        if (!getEntries(pvdConditions).isEmpty()) {
            return true;
        }

        // Check for carotid disease
        JsonNode carotidConditions = getConditions(tenantId, patientId, String.join(",", CAROTID_DISEASE_CODES));
        if (!getEntries(carotidConditions).isEmpty()) {
            return true;
        }

        return false;
    }
}
