package com.healthdata.quality.service;

import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.persistence.CareGapRepository;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Care Gap Detection Service
 * Analyzes quality measure results to automatically detect and create care gaps
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareGapDetectionService {

    private final CareGapRepository careGapRepository;
    private final CareGapPrioritizationService prioritizationService;

    // Measure metadata for mapping measures to gap categories and recommendations
    private static final Map<String, MeasureMetadata> MEASURE_METADATA = new HashMap<>();

    static {
        // Preventive Care / Screening Measures
        MEASURE_METADATA.put("CMS125", new MeasureMetadata(
            "Breast Cancer Screening",
            CareGapEntity.GapCategory.SCREENING,
            "Schedule mammography screening:\n" +
            "1. Order bilateral mammogram\n" +
            "2. Provide patient education on importance of screening\n" +
            "3. Address any barriers to screening\n" +
            "4. Schedule appointment within 30 days"
        ));

        MEASURE_METADATA.put("CMS130", new MeasureMetadata(
            "Colorectal Cancer Screening",
            CareGapEntity.GapCategory.SCREENING,
            "Order colorectal cancer screening:\n" +
            "1. Discuss screening options (colonoscopy, FIT, Cologuard)\n" +
            "2. Order appropriate screening test\n" +
            "3. Provide prep instructions if colonoscopy\n" +
            "4. Schedule within 60 days"
        ));

        // Chronic Disease Measures
        MEASURE_METADATA.put("CMS134", new MeasureMetadata(
            "Diabetes: HbA1c Control",
            CareGapEntity.GapCategory.CHRONIC_DISEASE,
            "Diabetes HbA1c management:\n" +
            "1. Order HbA1c test immediately\n" +
            "2. Review current diabetes medications\n" +
            "3. Assess adherence and barriers\n" +
            "4. Consider medication adjustment if HbA1c >9%\n" +
            "5. Schedule diabetes education if needed\n" +
            "6. Follow up in 2 weeks to review results"
        ));

        MEASURE_METADATA.put("CMS122", new MeasureMetadata(
            "Diabetes: HbA1c Testing",
            CareGapEntity.GapCategory.CHRONIC_DISEASE,
            "Order HbA1c testing:\n" +
            "1. Order HbA1c lab test\n" +
            "2. Review most recent HbA1c value\n" +
            "3. Schedule follow-up to review results\n" +
            "4. Document in diabetes flow sheet"
        ));

        MEASURE_METADATA.put("CMS135", new MeasureMetadata(
            "Heart Failure: ACE/ARB Therapy",
            CareGapEntity.GapCategory.MEDICATION,
            "ACE/ARB therapy for heart failure:\n" +
            "1. Review contraindications\n" +
            "2. Prescribe ACE inhibitor or ARB if no contraindications\n" +
            "3. Educate patient on medication importance\n" +
            "4. Monitor potassium and creatinine in 1-2 weeks\n" +
            "5. Titrate to target dose"
        ));

        // Mental Health Measures
        MEASURE_METADATA.put("CMS2", new MeasureMetadata(
            "Depression Screening and Follow-up",
            CareGapEntity.GapCategory.MENTAL_HEALTH,
            "Depression screening follow-up:\n" +
            "1. Administer PHQ-9 or PHQ-2 screening\n" +
            "2. If positive (PHQ-9 ≥10), conduct clinical assessment\n" +
            "3. Discuss treatment options (therapy, medication)\n" +
            "4. Provide mental health resources\n" +
            "5. Consider behavioral health referral\n" +
            "6. Schedule follow-up in 2 weeks"
        ));

        // Preventive Care
        MEASURE_METADATA.put("CMS147", new MeasureMetadata(
            "Influenza Immunization",
            CareGapEntity.GapCategory.PREVENTIVE_CARE,
            "Administer flu vaccine:\n" +
            "1. Verify no contraindications\n" +
            "2. Administer influenza vaccine\n" +
            "3. Document lot number and site\n" +
            "4. Provide VIS (Vaccine Information Statement)\n" +
            "5. Schedule patient for next year"
        ));

        MEASURE_METADATA.put("CMS165", new MeasureMetadata(
            "Controlling High Blood Pressure",
            CareGapEntity.GapCategory.CHRONIC_DISEASE,
            "Blood pressure control:\n" +
            "1. Measure blood pressure at visit\n" +
            "2. If BP >140/90, review current medications\n" +
            "3. Assess medication adherence\n" +
            "4. Consider adding or increasing antihypertensive\n" +
            "5. Lifestyle counseling (diet, exercise, sodium)\n" +
            "6. Follow up in 2-4 weeks"
        ));
    }

    /**
     * Analyze a measure result and create care gap if needed
     * Called when a measure calculation completes
     */
    @Transactional
    public void analyzeAndCreateCareGaps(QualityMeasureResultEntity measureResult) {
        log.debug("Analyzing measure result {} for patient {} (tenant: {})",
            measureResult.getMeasureId(),
            measureResult.getPatientId(),
            measureResult.getTenantId()
        );

        // Only create gap if patient is eligible (in denominator) but not compliant (not in numerator)
        if (!shouldCreateGap(measureResult)) {
            log.debug("No gap needed for measure {} - patient is compliant or not eligible",
                measureResult.getMeasureId());
            return;
        }

        // Check for duplicate gap
        String gapType = buildGapType(measureResult);
        if (careGapRepository.existsOpenCareGap(
            measureResult.getTenantId(),
            measureResult.getPatientId(),
            gapType
        )) {
            log.info("Care gap already exists for patient {} measure {} - skipping duplicate",
                measureResult.getPatientId(), measureResult.getMeasureId());
            return;
        }

        // Create the care gap
        CareGapEntity gap = createCareGapFromMeasure(measureResult);
        careGapRepository.save(gap);

        log.info("Created care gap {} for patient {} from measure {} (Priority: {})",
            gap.getId(),
            gap.getPatientId(),
            measureResult.getMeasureId(),
            gap.getPriority()
        );
    }

    /**
     * Determine if a care gap should be created
     * Gap exists when: in denominator BUT NOT in numerator
     */
    private boolean shouldCreateGap(QualityMeasureResultEntity measureResult) {
        // Must be eligible (in denominator)
        if (!measureResult.getDenominatorElligible()) {
            return false;
        }

        // Must NOT be compliant (not in numerator)
        return !measureResult.getNumeratorCompliant();
    }

    /**
     * Create care gap entity from measure result
     */
    private CareGapEntity createCareGapFromMeasure(QualityMeasureResultEntity measureResult) {
        MeasureMetadata metadata = getMeasureMetadata(measureResult.getMeasureId());
        CareGapEntity.GapCategory category = metadata.category;

        // Determine priority based on patient risk and gap category
        CareGapEntity.Priority priority = prioritizationService.determinePriority(
            measureResult.getTenantId(),
            measureResult.getPatientId(),
            measureResult.getMeasureId(),
            category
        );

        // Calculate due date
        Instant dueDate = prioritizationService.calculateDueDate(priority);

        return CareGapEntity.builder()
            .tenantId(measureResult.getTenantId())
            .patientId(measureResult.getPatientId())
            .category(category)
            .gapType(buildGapType(measureResult))
            .title(buildTitle(measureResult, metadata))
            .description(buildDescription(measureResult, metadata))
            .priority(priority)
            .status(CareGapEntity.Status.OPEN)
            .qualityMeasure(measureResult.getMeasureId())
            .measureResultId(measureResult.getId())
            .createdFromMeasure(true)
            .recommendation(metadata.recommendation)
            .evidence(buildEvidence(measureResult))
            .dueDate(dueDate)
            .identifiedDate(measureResult.getCalculationDate().atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault()).toInstant())
            .build();
    }

    /**
     * Build gap type for deduplication
     */
    private String buildGapType(QualityMeasureResultEntity measureResult) {
        return "measure-gap-" + measureResult.getMeasureId().toLowerCase();
    }

    /**
     * Build gap title
     */
    private String buildTitle(QualityMeasureResultEntity measureResult, MeasureMetadata metadata) {
        return String.format("%s - Care Gap Identified", metadata.name);
    }

    /**
     * Build gap description
     */
    private String buildDescription(QualityMeasureResultEntity measureResult, MeasureMetadata metadata) {
        return String.format(
            "Patient is eligible for %s (%s) but does not meet compliance criteria. " +
            "This quality measure was calculated on %s and identified a care gap requiring clinical action.",
            metadata.name,
            measureResult.getMeasureId(),
            measureResult.getCalculationDate()
        );
    }

    /**
     * Build evidence string
     */
    private String buildEvidence(QualityMeasureResultEntity measureResult) {
        return String.format(
            "Quality Measure: %s (%s)\n" +
            "Measure Year: %d\n" +
            "Calculation Date: %s\n" +
            "Denominator Eligible: %s\n" +
            "Numerator Compliant: %s\n" +
            "CQL Library: %s",
            measureResult.getMeasureName(),
            measureResult.getMeasureId(),
            measureResult.getMeasureYear() != null ? measureResult.getMeasureYear() : LocalDate.now().getYear(),
            measureResult.getCalculationDate(),
            measureResult.getDenominatorElligible(),
            measureResult.getNumeratorCompliant(),
            measureResult.getCqlLibrary() != null ? measureResult.getCqlLibrary() : "N/A"
        );
    }

    /**
     * Get measure metadata
     */
    private MeasureMetadata getMeasureMetadata(String measureId) {
        return MEASURE_METADATA.getOrDefault(measureId, new MeasureMetadata(
            measureId,
            CareGapEntity.GapCategory.PREVENTIVE_CARE,
            "Review patient record and address identified care gap according to clinical guidelines."
        ));
    }

    /**
     * Measure metadata holder
     */
    private record MeasureMetadata(
        String name,
        CareGapEntity.GapCategory category,
        String recommendation
    ) {}
}
