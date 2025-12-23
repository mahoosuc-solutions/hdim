package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * EED - Eye Exam for Patients with Diabetes (HEDIS)
 *
 * Evaluates whether diabetic patients received annual retinal eye exam.
 * Diabetic retinopathy is a leading cause of blindness - early detection is critical.
 *
 * Two methods accepted:
 * - Retinal eye exam by eye care professional
 * - Retinal imaging (fundus photography) with interpretation
 */
@Component
public class EEDMeasure extends AbstractHedisMeasure {

    private static final List<String> DIABETES_CODES = Arrays.asList(
        "44054006",  // Type 2 diabetes mellitus (SNOMED)
        "46635009",  // Type 1 diabetes mellitus (SNOMED)
        "73211009",  // Diabetes mellitus (SNOMED)
        "199223000", // Pre-existing diabetes mellitus (SNOMED)
        "237599002", // Insulin-dependent diabetes mellitus (SNOMED)
        "237618001"  // Non-insulin-dependent diabetes mellitus (SNOMED)
    );

    private static final List<String> RETINAL_EYE_EXAM_CODES = Arrays.asList(
        "252779009", // SNOMED - Ophthalmoscopy
        "410451008", // SNOMED - Fundoscopy
        "4688000",   // SNOMED - Ophthalmologic examination and evaluation
        "36228007",  // SNOMED - Dilated eye examination
        "274795007", // SNOMED - Diabetic retinopathy screening
        "420213007"  // SNOMED - Fundus examination (procedure)
    );

    private static final List<String> RETINAL_IMAGING_CODES = Arrays.asList(
        "252776001", // SNOMED - Fundus photography
        "392012000", // SNOMED - Retinal photography
        "241611005", // SNOMED - Fluorescein angiography
        "392014004", // SNOMED - Fundus fluorescein angiography
        "56844000"   // SNOMED - Imaging of retina
    );

    private static final List<String> DIABETIC_RETINOPATHY_CODES = Arrays.asList(
        "4855003",   // SNOMED - Diabetic retinopathy
        "312912001", // SNOMED - Proliferative diabetic retinopathy
        "312903003", // SNOMED - Mild nonproliferative diabetic retinopathy
        "193349004", // SNOMED - Preproliferative diabetic retinopathy
        "314010006", // SNOMED - Macular edema due to diabetes mellitus
        "59276001"   // SNOMED - Proliferative retinopathy
    );

    private static final List<String> OPHTHALMOLOGY_VISIT_CODES = Arrays.asList(
        "185349003", // SNOMED - Outpatient encounter
        "308335008", // SNOMED - Patient encounter procedure
        "390906007", // SNOMED - Follow-up encounter
        "185463005"  // SNOMED - Office visit
    );

    @Override
    public String getMeasureId() {
        return "EED";
    }

    @Override
    public String getMeasureName() {
        return "Eye Exam for Patients with Diabetes";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'EED-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating EED measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18-75 with diabetes)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Check for retinal eye exam in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusMonths(12).toString();

        // Check for direct eye exam
        JsonNode eyeExams = getEncounters(tenantId, patientId,
            String.join(",", RETINAL_EYE_EXAM_CODES), dateFilter);
        boolean hasEyeExam = !getEntries(eyeExams).isEmpty();
        String eyeExamDate = hasEyeExam ? getEffectiveDate(getEntries(eyeExams).get(0)) : null;

        // Check for retinal imaging
        JsonNode retinalImaging = getObservations(tenantId, patientId,
            String.join(",", RETINAL_IMAGING_CODES), dateFilter);
        boolean hasRetinalImaging = !getEntries(retinalImaging).isEmpty();
        String imagingDate = hasRetinalImaging ? getEffectiveDate(getEntries(retinalImaging).get(0)) : null;

        // Either exam or imaging satisfies the measure
        boolean hasRetinalScreening = hasEyeExam || hasRetinalImaging;
        String screeningDate = hasEyeExam ? eyeExamDate : imagingDate;
        String screeningMethod = hasEyeExam ? "Eye exam" : hasRetinalImaging ? "Retinal imaging" : "None";

        // Check for diabetic retinopathy diagnosis (indicates screening occurred and found condition)
        JsonNode retinopathyConditions = getConditions(tenantId, patientId,
            String.join(",", DIABETIC_RETINOPATHY_CODES));
        boolean hasRetinopathy = !getEntries(retinopathyConditions).isEmpty();
        String retinopathyStatus = hasRetinopathy ? "Diabetic retinopathy detected" : "No retinopathy detected";

        resultBuilder.inNumerator(hasRetinalScreening);
        resultBuilder.complianceRate(hasRetinalScreening ? 1.0 : 0.0);
        resultBuilder.score(hasRetinalScreening ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasRetinalScreening) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_DIABETIC_EYE_EXAM")
                .description("No retinal eye exam in last 12 months for diabetic patient")
                .recommendedAction("Refer to ophthalmologist or optometrist for dilated retinal exam - annual screening required")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("DIABETIC_RETINOPATHY_SCREENING")
                .description("Diabetic retinopathy is preventable cause of blindness - early detection critical")
                .recommendedAction("Schedule comprehensive dilated eye exam or retinal photography within 30 days")
                .priority("high")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        } else if (hasRetinopathy) {
            // Has retinopathy - needs more frequent monitoring
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("DIABETIC_RETINOPATHY_MONITORING")
                .description("Diabetic retinopathy detected - requires more frequent monitoring than annual screening")
                .recommendedAction("Ensure ophthalmology follow-up per retinopathy severity (may need 3-6 month intervals)")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "hasEyeExam", hasEyeExam,
            "eyeExamDate", eyeExamDate != null ? eyeExamDate : "None",
            "hasRetinalImaging", hasRetinalImaging,
            "imagingDate", imagingDate != null ? imagingDate : "None",
            "hasRetinalScreening", hasRetinalScreening,
            "screeningMethod", screeningMethod,
            "screeningDate", screeningDate != null ? screeningDate : "None",
            "hasRetinopathy", hasRetinopathy,
            "retinopathyStatus", retinopathyStatus
        ));

        resultBuilder.evidence(java.util.Map.of(
            "retinalScreeningComplete", hasRetinalScreening,
            "screeningMethod", screeningMethod,
            "diabeticRetinopathyPresent", hasRetinopathy
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        Integer age = getPatientAge(patient);
        if (age == null || age < 18 || age > 75) {
            return false;
        }

        // Must have diabetes diagnosis
        JsonNode diabetesConditions = getConditions(tenantId, patientId,
            String.join(",", DIABETES_CODES));

        return !getEntries(diabetesConditions).isEmpty();
    }
}
