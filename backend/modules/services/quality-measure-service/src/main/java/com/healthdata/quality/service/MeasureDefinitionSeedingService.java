package com.healthdata.quality.service;

import com.healthdata.quality.persistence.QualityMeasureEntity;
import com.healthdata.quality.persistence.QualityMeasureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for seeding HEDIS quality measure definitions into the database.
 * Used by demo-seeding-service to ensure measures are available for evaluation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureDefinitionSeedingService {

    private final QualityMeasureRepository measureRepository;

    /**
     * Seed all standard HEDIS measure definitions for a tenant.
     *
     * @param tenantId The tenant to seed measures for
     * @return Number of measures seeded (or already existed)
     */
    @Transactional
    public int seedHedisMeasures(String tenantId) {
        log.info("Seeding HEDIS measure definitions for tenant: {}", tenantId);

        List<QualityMeasureEntity> measures = createHedisMeasureDefinitions(tenantId);
        int seededCount = 0;

        for (QualityMeasureEntity measure : measures) {
            if (!measureRepository.existsByMeasureIdAndTenantId(measure.getMeasureId(), tenantId)) {
                measureRepository.save(measure);
                log.debug("Seeded measure: {} - {}", measure.getMeasureId(), measure.getMeasureName());
                seededCount++;
            } else {
                log.debug("Measure already exists: {}", measure.getMeasureId());
            }
        }

        log.info("Seeded {} new HEDIS measures for tenant: {}", seededCount, tenantId);
        return seededCount;
    }

    /**
     * Get count of measures for a tenant.
     */
    public long getMeasureCount(String tenantId) {
        return measureRepository.countByTenantIdAndMeasureSet(tenantId, "HEDIS");
    }

    /**
     * Create HEDIS measure definitions.
     */
    private List<QualityMeasureEntity> createHedisMeasureDefinitions(String tenantId) {
        List<QualityMeasureEntity> measures = new ArrayList<>();
        LocalDate effectiveStart = LocalDate.of(2024, 1, 1);

        // BCS - Breast Cancer Screening
        measures.add(QualityMeasureEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .measureId("BCS")
            .measureName("Breast Cancer Screening")
            .measureSet("HEDIS")
            .version("2024")
            .domain("Prevention & Health Promotion")
            .category("Oncology")
            .measureType("proportion")
            .description("Assesses the percentage of women 50-74 years of age who had a mammogram " +
                "to screen for breast cancer within the past 27 months.")
            .rationale("Breast cancer is the second most common cancer among women. " +
                "Regular screening mammography can detect breast cancer early when treatment is most effective.")
            .guidance("Patient is compliant if they have had a screening or diagnostic mammogram " +
                "within the measurement period (27 months).")
            .scoringMethod("proportion")
            .improvementNotation("higher-is-better")
            .active(true)
            .effectivePeriodStart(effectiveStart)
            .build());

        // COL - Colorectal Cancer Screening
        measures.add(QualityMeasureEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .measureId("COL")
            .measureName("Colorectal Cancer Screening")
            .measureSet("HEDIS")
            .version("2024")
            .domain("Prevention & Health Promotion")
            .category("Oncology")
            .measureType("proportion")
            .description("Assesses the percentage of adults 45-75 years of age who had appropriate " +
                "screening for colorectal cancer.")
            .rationale("Colorectal cancer is the third most common cancer. " +
                "Screening can find precancerous polyps for removal before they become cancer.")
            .guidance("Patient is compliant if they have had: colonoscopy within 10 years, " +
                "sigmoidoscopy within 5 years, FIT-DNA within 3 years, or FOBT/FIT within 1 year.")
            .scoringMethod("proportion")
            .improvementNotation("higher-is-better")
            .active(true)
            .effectivePeriodStart(effectiveStart)
            .build());

        // CBP - Controlling High Blood Pressure
        measures.add(QualityMeasureEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .measureId("CBP")
            .measureName("Controlling High Blood Pressure")
            .measureSet("HEDIS")
            .version("2024")
            .domain("Chronic Disease Management")
            .category("Cardiology")
            .measureType("proportion")
            .description("Assesses the percentage of patients 18-85 years of age who had a diagnosis " +
                "of hypertension and whose blood pressure was adequately controlled (<140/90 mm Hg).")
            .rationale("Uncontrolled hypertension is a major risk factor for heart disease, stroke, " +
                "and kidney disease. Proper blood pressure control significantly reduces these risks.")
            .guidance("Patient is compliant if their most recent blood pressure reading shows " +
                "systolic < 140 mm Hg AND diastolic < 90 mm Hg.")
            .scoringMethod("proportion")
            .improvementNotation("higher-is-better")
            .active(true)
            .effectivePeriodStart(effectiveStart)
            .build());

        // CDC - Comprehensive Diabetes Care
        measures.add(QualityMeasureEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .measureId("CDC")
            .measureName("Comprehensive Diabetes Care")
            .measureSet("HEDIS")
            .version("2024")
            .domain("Chronic Disease Management")
            .category("Endocrinology")
            .measureType("proportion")
            .description("Assesses the percentage of patients 18-75 years of age with diabetes who " +
                "received appropriate diabetes care including HbA1c testing, eye exam, " +
                "nephropathy screening, and blood pressure control.")
            .rationale("Diabetes requires comprehensive management to prevent complications. " +
                "Regular monitoring and preventive care can significantly reduce morbidity and mortality.")
            .guidance("Patient is evaluated on multiple sub-measures: HbA1c testing and control, " +
                "diabetic eye exam, nephropathy screening, and blood pressure control.")
            .scoringMethod("proportion")
            .improvementNotation("higher-is-better")
            .active(true)
            .effectivePeriodStart(effectiveStart)
            .build());

        // CCS - Cervical Cancer Screening
        measures.add(QualityMeasureEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .measureId("CCS")
            .measureName("Cervical Cancer Screening")
            .measureSet("HEDIS")
            .version("2024")
            .domain("Prevention & Health Promotion")
            .category("Oncology")
            .measureType("proportion")
            .description("Assesses the percentage of women 21-64 years of age who were screened " +
                "for cervical cancer using either cervical cytology (Pap test) or HPV testing.")
            .rationale("Cervical cancer screening can detect precancerous changes that can be " +
                "treated before cancer develops. Nearly all cervical cancers can be prevented " +
                "through screening and HPV vaccination.")
            .guidance("Patient is compliant if they have had: Pap test within 3 years, or " +
                "HPV test within 5 years (ages 30-64).")
            .scoringMethod("proportion")
            .improvementNotation("higher-is-better")
            .active(true)
            .effectivePeriodStart(effectiveStart)
            .build());

        // EED - Eye Exam for Patients with Diabetes
        measures.add(QualityMeasureEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .measureId("EED")
            .measureName("Eye Exam for Patients With Diabetes")
            .measureSet("HEDIS")
            .version("2024")
            .domain("Chronic Disease Management")
            .category("Ophthalmology")
            .measureType("proportion")
            .description("Assesses the percentage of patients 18-75 years of age with diabetes " +
                "who had a retinal or dilated eye exam by an eye care professional.")
            .rationale("Diabetic retinopathy is the leading cause of blindness in working-age adults. " +
                "Early detection through annual eye exams can prevent vision loss.")
            .guidance("Patient is compliant if they have had a diabetic retinal exam or " +
                "dilated eye exam within the measurement period.")
            .scoringMethod("proportion")
            .improvementNotation("higher-is-better")
            .active(true)
            .effectivePeriodStart(effectiveStart)
            .build());

        // SPC - Statin Therapy for Patients With Cardiovascular Disease
        measures.add(QualityMeasureEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .measureId("SPC")
            .measureName("Statin Therapy for Patients With Cardiovascular Disease")
            .measureSet("HEDIS")
            .version("2024")
            .domain("Chronic Disease Management")
            .category("Cardiology")
            .measureType("proportion")
            .description("Assesses the percentage of males 21-75 years of age and females 40-75 years " +
                "of age during the measurement year who were identified as having clinical ASCVD " +
                "and who were dispensed at least one high-intensity or moderate-intensity statin medication.")
            .rationale("Statin therapy reduces cardiovascular events in patients with established ASCVD. " +
                "Secondary prevention with statins is recommended by major cardiovascular guidelines.")
            .guidance("Patient is compliant if they have an active prescription for a statin medication.")
            .scoringMethod("proportion")
            .improvementNotation("higher-is-better")
            .active(true)
            .effectivePeriodStart(effectiveStart)
            .build());

        // AAB - Avoidance of Antibiotic Treatment for Acute Bronchitis/Bronchiolitis
        measures.add(QualityMeasureEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .measureId("AAB")
            .measureName("Avoidance of Antibiotic Treatment for Acute Bronchitis/Bronchiolitis")
            .measureSet("HEDIS")
            .version("2024")
            .domain("Appropriate Use")
            .category("Antimicrobial Stewardship")
            .measureType("proportion")
            .description("Assesses avoidance of inappropriate antibiotic prescribing for acute bronchitis/bronchiolitis.")
            .rationale("Reducing unnecessary antibiotic use improves care quality and helps prevent antimicrobial resistance.")
            .guidance("Patient is compliant when no inappropriate antibiotic was dispensed for an acute bronchitis/bronchiolitis episode.")
            .scoringMethod("proportion")
            .improvementNotation("higher-is-better")
            .active(true)
            .effectivePeriodStart(effectiveStart)
            .build());

        // COU - Risk of Continued Opioid Use
        measures.add(QualityMeasureEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .measureId("COU")
            .measureName("Risk of Continued Opioid Use")
            .measureSet("HEDIS")
            .version("2024")
            .domain("Behavioral Health")
            .category("Medication Safety")
            .measureType("proportion")
            .description("Assesses risk of prolonged opioid use following a new opioid prescribing event.")
            .rationale("Early detection of continued opioid exposure supports safer prescribing and follow-up care.")
            .guidance("Patient is compliant when follow-up and medication use patterns remain within safe thresholds.")
            .scoringMethod("proportion")
            .improvementNotation("higher-is-better")
            .active(true)
            .effectivePeriodStart(effectiveStart)
            .build());

        // FMC - Follow-Up after Mental Health Encounter
        measures.add(QualityMeasureEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .measureId("FMC")
            .measureName("Follow-Up after Mental Health Encounter")
            .measureSet("HEDIS")
            .version("2024")
            .domain("Behavioral Health")
            .category("Care Continuity")
            .measureType("proportion")
            .description("Assesses timely post-encounter follow-up for members with qualifying mental health visits.")
            .rationale("Timely follow-up improves continuity, engagement, and outcomes in behavioral health care.")
            .guidance("Patient is compliant when qualifying follow-up is completed within the measurement window.")
            .scoringMethod("proportion")
            .improvementNotation("higher-is-better")
            .active(true)
            .effectivePeriodStart(effectiveStart)
            .build());

        return measures;
    }
}
