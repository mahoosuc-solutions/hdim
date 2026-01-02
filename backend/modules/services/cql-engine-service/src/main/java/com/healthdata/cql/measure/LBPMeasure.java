package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * LBP - Use of Imaging Studies for Low Back Pain (HEDIS)
 *
 * Identifies inappropriate use of imaging (X-ray, CT, MRI) for acute low back pain
 * within 6 weeks of diagnosis, without red flag conditions.
 *
 * Clinical guidelines recommend conservative treatment first - imaging doesn't
 * improve outcomes for uncomplicated acute low back pain.
 *
 * This is an INVERSE measure - LOWER rates are better (less overuse).
 * Being in the numerator indicates INAPPROPRIATE IMAGING.
 */
@Component
public class LBPMeasure extends AbstractHedisMeasure {

    private static final List<String> LOW_BACK_PAIN_CODES = Arrays.asList(
        "279039007", // SNOMED - Low back pain
        "161891005", // SNOMED - Backache
        "202794004", // SNOMED - Acute low back pain
        "22253000",  // SNOMED - Pain in lumbar spine
        "300954003", // SNOMED - Pain in lower limb
        "134407002"  // SNOMED - Chronic low back pain
    );

    private static final List<String> LUMBAR_IMAGING_CODES = Arrays.asList(
        // X-ray
        "168731009", // SNOMED - Radiography of lumbar spine
        "241665008", // SNOMED - Plain X-ray of lumbar spine
        // CT
        "241551005", // SNOMED - CT of lumbar spine
        "429858000", // SNOMED - Computed tomography of abdomen and pelvis
        // MRI
        "241557009", // SNOMED - MRI of lumbar spine
        "450451004", // SNOMED - Magnetic resonance imaging of spine
        "698354004"  // SNOMED - MRI of lumbosacral spine
    );

    private static final List<String> RED_FLAG_CONDITIONS = Arrays.asList(
        // Cancer
        "363346000", // SNOMED - Malignant neoplasm (primary)
        "128462008", // SNOMED - Metastatic malignant neoplasm
        "94391008",  // SNOMED - Secondary malignant neoplasm of bone
        // Infection
        "128192003", // SNOMED - Spinal infection
        "240476005", // SNOMED - Spinal osteomyelitis
        "91302008",  // SNOMED - Sepsis
        // Fracture/Trauma
        "263204007", // SNOMED - Fracture of lumbar vertebra
        "399166001", // SNOMED - Traumatic injury
        "125602000", // SNOMED - Fracture of vertebral column
        // Cauda equina syndrome
        "192681008", // SNOMED - Cauda equina syndrome
        // Neurological deficits
        "280816001", // SNOMED - Motor weakness
        "225606002", // SNOMED - Abnormal sensation
        "78314001",  // SNOMED - Paralysis
        // Other serious conditions
        "202606003", // SNOMED - Ankylosing spondylitis
        "35920005"   // SNOMED - Aortic aneurysm
    );

    private static final int INAPPROPRIATE_IMAGING_WINDOW_DAYS = 42; // 6 weeks

    @Override
    public String getMeasureId() {
        return "LBP";
    }

    @Override
    public String getMeasureName() {
        return "Use of Imaging Studies for Low Back Pain";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'LBP-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, UUID patientId) {
        logger.info("Evaluating LBP measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 18-50 with low back pain diagnosis)")
                .build();
        }

        resultBuilder.inDenominator(true);

        // Find low back pain diagnosis date
        String lbpDateFilter = "ge" + LocalDate.now().minusMonths(6).toString();
        JsonNode lbpConditions = getConditions(tenantId, patientId,
            String.join(",", LOW_BACK_PAIN_CODES));
        List<JsonNode> lbpEntries = getEntries(lbpConditions);

        if (lbpEntries.isEmpty()) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("No low back pain diagnosis found")
                .build();
        }

        // Get diagnosis date
        JsonNode mostRecentLBP = lbpEntries.get(0);
        String lbpDateStr = getEffectiveDate(mostRecentLBP);
        LocalDate lbpDate = LocalDate.parse(lbpDateStr);

        // Calculate inappropriate imaging window (within 6 weeks of diagnosis)
        LocalDate imagingWindowEnd = lbpDate.plusDays(INAPPROPRIATE_IMAGING_WINDOW_DAYS);

        // Check for red flag conditions (make imaging appropriate)
        JsonNode redFlags = getConditions(tenantId, patientId,
            String.join(",", RED_FLAG_CONDITIONS));
        boolean hasRedFlags = !getEntries(redFlags).isEmpty();

        // Check for lumbar imaging within 6 weeks
        String imagingDateFilter = "ge" + lbpDate.toString() +
                                   "&date=le" + imagingWindowEnd.toString();
        JsonNode lumbarImaging = getObservations(tenantId, patientId,
            String.join(",", LUMBAR_IMAGING_CODES), imagingDateFilter);
        boolean hasEarlyImaging = !getEntries(lumbarImaging).isEmpty();

        String imagingDate = hasEarlyImaging ?
            getEffectiveDate(getEntries(lumbarImaging).get(0)) : null;

        // Inappropriate imaging = early imaging WITHOUT red flags
        boolean hasInappropriateImaging = hasEarlyImaging && !hasRedFlags;

        // For INVERSE measure: being in numerator = inappropriate imaging (bad outcome)
        resultBuilder.inNumerator(hasInappropriateImaging);
        // Compliance = NOT having inappropriate imaging
        resultBuilder.complianceRate(hasInappropriateImaging ? 0.0 : 1.0);
        resultBuilder.score(hasInappropriateImaging ? 0.0 : 100.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (hasInappropriateImaging) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("INAPPROPRIATE_EARLY_IMAGING_LBP")
                .description(String.format("Lumbar imaging performed within 6 weeks of acute low back pain (%s) without red flags",
                    lbpDateStr))
                .recommendedAction("Follow clinical guidelines: Conservative treatment first; imaging only if red flags or no improvement after 6 weeks")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(3))
                .build());

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("IMAGING_OVERUSE_EDUCATION")
                .description("Early imaging for uncomplicated low back pain doesn't improve outcomes and increases costs")
                .recommendedAction("Educate on evidence-based low back pain management; implement clinical decision support for imaging orders")
                .priority("low")
                .dueDate(LocalDate.now().plusMonths(6))
                .build());
        } else if (hasEarlyImaging && hasRedFlags) {
            // Imaging was appropriate due to red flags - no care gap
            // Could add a care gap indicating appropriate use for tracking
        }

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "lbpDiagnosisDate", lbpDateStr,
            "imagingWindowEnd", imagingWindowEnd.toString(),
            "hasEarlyImaging", hasEarlyImaging,
            "imagingDate", imagingDate != null ? imagingDate : "None",
            "hasRedFlags", hasRedFlags,
            "hasInappropriateImaging", hasInappropriateImaging,
            "imagingAppropriate", hasRedFlags ? "Yes (red flags present)" :
                                               !hasEarlyImaging ? "Yes (no early imaging)" :
                                               "No (inappropriate early imaging)"
        ));

        resultBuilder.evidence(java.util.Map.of(
            "inappropriateImaging", hasInappropriateImaging,
            "redFlagsPresent", hasRedFlags,
            "appropriateCare", !hasInappropriateImaging
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, UUID patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be age 18-50 (measure focuses on younger adults with uncomplicated LBP)
        Integer age = getPatientAge(patient);
        if (age == null || age < 18 || age > 50) {
            return false;
        }

        // Must have low back pain diagnosis in last 6 months
        String lbpDateFilter = "ge" + LocalDate.now().minusMonths(6).toString();
        JsonNode lbpConditions = getConditions(tenantId, patientId,
            String.join(",", LOW_BACK_PAIN_CODES));

        return !getEntries(lbpConditions).isEmpty();
    }
}
