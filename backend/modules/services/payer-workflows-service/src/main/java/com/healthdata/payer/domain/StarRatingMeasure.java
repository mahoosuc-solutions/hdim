package com.healthdata.payer.domain;

import lombok.Getter;

/**
 * HEDIS measures that affect Medicare Advantage Star Ratings.
 * Maps to CMS Star Rating methodology and includes measure weights.
 *
 * Based on CMS 2024 Star Ratings Technical Notes.
 */
@Getter
public enum StarRatingMeasure {

    // Staying Healthy: Screenings, Tests and Vaccines
    BREAST_CANCER_SCREENING("BCS", "Breast Cancer Screening", StarRatingDomain.STAYING_HEALTHY, 1.0),
    COLORECTAL_CANCER_SCREENING("COL", "Colorectal Cancer Screening", StarRatingDomain.STAYING_HEALTHY, 3.0),
    ADULT_BMI_ASSESSMENT("ABA", "Adult BMI Assessment", StarRatingDomain.STAYING_HEALTHY, 1.0),
    CARE_FOR_OLDER_ADULTS_MEDICATION_REVIEW("COA-MR", "Care for Older Adults - Medication Review", StarRatingDomain.STAYING_HEALTHY, 1.0),
    CARE_FOR_OLDER_ADULTS_FUNCTIONAL_STATUS("COA-FS", "Care for Older Adults - Functional Status Assessment", StarRatingDomain.STAYING_HEALTHY, 1.0),
    CARE_FOR_OLDER_ADULTS_PAIN_ASSESSMENT("COA-PA", "Care for Older Adults - Pain Assessment", StarRatingDomain.STAYING_HEALTHY, 1.0),
    OSTEOPOROSIS_MANAGEMENT_IN_WOMEN("OMW", "Osteoporosis Management in Women Who Had a Fracture", StarRatingDomain.STAYING_HEALTHY, 3.0),
    DIABETES_CARE_EYE_EXAM("CDC-E", "Comprehensive Diabetes Care: Eye Exam", StarRatingDomain.STAYING_HEALTHY, 1.0),
    GLAUCOMA_SCREENING("GSO", "Glaucoma Screening in Older Adults", StarRatingDomain.STAYING_HEALTHY, 1.0),
    ANNUAL_FLU_VACCINE("FLU", "Annual Flu Vaccine", StarRatingDomain.STAYING_HEALTHY, 1.0),
    IMPROVING_BLADDER_CONTROL("IBC", "Improving or Maintaining Physical Health", StarRatingDomain.STAYING_HEALTHY, 1.0),
    PHYSICAL_ACTIVITY("PAC", "Physical Activity in Older Adults", StarRatingDomain.STAYING_HEALTHY, 1.0),

    // Managing Chronic (Long-Term) Conditions
    CONTROLLING_BLOOD_PRESSURE("CBP", "Controlling High Blood Pressure", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 3.0),
    DIABETES_CARE_HBA1C_POOR_CONTROL("CDC-H9", "Comprehensive Diabetes Care: HbA1c Poor Control (>9.0%)", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 3.0),
    DIABETES_CARE_HBA1C_CONTROL("CDC-H8", "Comprehensive Diabetes Care: HbA1c Control (<8.0%)", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 1.0),
    DIABETES_CARE_BP_CONTROL("CDC-BP", "Comprehensive Diabetes Care: Blood Pressure Control (<140/90)", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 1.0),
    STATIN_THERAPY_FOR_CVD("SPC", "Statin Therapy for Patients with Cardiovascular Disease", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 3.0),
    MEDICATION_ADHERENCE_DIABETES("MED-D", "Medication Adherence for Diabetes Medications", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 1.0),
    MEDICATION_ADHERENCE_HYPERTENSION("MED-H", "Medication Adherence for Hypertension (RAS antagonists)", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 1.0),
    MEDICATION_ADHERENCE_CHOLESTEROL("MED-C", "Medication Adherence for Cholesterol (Statins)", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 1.0),
    RHEUMATOID_ARTHRITIS_MANAGEMENT("ART", "Disease Modifying Anti-Rheumatic Drug Therapy for Rheumatoid Arthritis", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 1.0),
    OSTEOPOROSIS_TESTING("OPT", "Osteoporosis Testing in Older Women", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 1.0),
    KIDNEY_HEALTH_FOR_DIABETES("KED", "Kidney Health Evaluation for Patients with Diabetes", StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, 3.0),

    // Members with Medicare and Medicaid
    PLAN_ALL_CAUSE_READMISSIONS("PCR", "Plan All-Cause Readmissions", StarRatingDomain.MEMBER_EXPERIENCE, 3.0),

    // Member Experience with Health Plan
    GETTING_NEEDED_CARE("GNC", "Getting Needed Care", StarRatingDomain.MEMBER_EXPERIENCE, 1.5),
    GETTING_APPOINTMENTS_AND_CARE_QUICKLY("GAC", "Getting Appointments and Care Quickly", StarRatingDomain.MEMBER_EXPERIENCE, 1.5),
    CUSTOMER_SERVICE("CS", "Customer Service", StarRatingDomain.MEMBER_EXPERIENCE, 1.5),
    RATING_OF_HEALTH_CARE_QUALITY("RHQ", "Rating of Health Care Quality", StarRatingDomain.MEMBER_EXPERIENCE, 1.5),
    RATING_OF_HEALTH_PLAN("RHP", "Rating of Health Plan", StarRatingDomain.MEMBER_EXPERIENCE, 1.5),
    CARE_COORDINATION("CC", "Care Coordination", StarRatingDomain.MEMBER_EXPERIENCE, 1.5),

    // Member Complaints and Changes in the Health Plan's Performance
    COMPLAINTS_ABOUT_THE_HEALTH_PLAN("CPL", "Complaints about the Health Plan", StarRatingDomain.COMPLAINTS_AND_PERFORMANCE, 1.5),
    MEMBERS_CHOOSING_TO_LEAVE("MCL", "Members Choosing to Leave the Plan", StarRatingDomain.COMPLAINTS_AND_PERFORMANCE, 1.5),
    HEALTH_PLAN_QUALITY_IMPROVEMENT("HPQI", "Health Plan Quality Improvement", StarRatingDomain.COMPLAINTS_AND_PERFORMANCE, 5.0),
    PLAN_MAKES_TIMELY_DECISIONS("PTD", "Plan Makes Timely Decisions about Appeals", StarRatingDomain.COMPLAINTS_AND_PERFORMANCE, 1.5),
    REVIEWING_APPEALS_DECISIONS("RAD", "Reviewing Appeals Decisions", StarRatingDomain.COMPLAINTS_AND_PERFORMANCE, 1.5),
    CALL_CENTER_FOREIGN_LANGUAGE_INTERPRETER("CLI", "Call Center - Foreign Language Interpreter and TTY Availability", StarRatingDomain.COMPLAINTS_AND_PERFORMANCE, 1.0),

    // Drug Plan Customer Service
    RATING_OF_DRUG_PLAN("RDP", "Rating of Drug Plan", StarRatingDomain.DRUG_PLAN, 1.5),
    GETTING_INFORMATION_FROM_DRUG_PLAN("GID", "Getting Information from Drug Plan", StarRatingDomain.DRUG_PLAN, 1.5),

    // Patient Safety and Accuracy of Drug Pricing
    HIGH_RISK_MEDICATION("HRM", "Use of High-Risk Medications in the Elderly", StarRatingDomain.DRUG_SAFETY, 3.0),
    DIABETES_TREATMENT("DTR", "Diabetes Treatment", StarRatingDomain.DRUG_SAFETY, 1.0),
    MEDICATION_RECONCILIATION("MRP", "Medication Reconciliation Post-Discharge", StarRatingDomain.DRUG_SAFETY, 3.0),
    STATIN_USE_IN_DIABETES("SUD", "Statin Use in Persons with Diabetes", StarRatingDomain.DRUG_SAFETY, 3.0);

    private final String code;
    private final String displayName;
    private final StarRatingDomain domain;
    private final double weight;

    StarRatingMeasure(String code, String displayName, StarRatingDomain domain, double weight) {
        this.code = code;
        this.displayName = displayName;
        this.domain = domain;
        this.weight = weight;
    }

    /**
     * Find a measure by its HEDIS code
     */
    public static StarRatingMeasure fromCode(String code) {
        for (StarRatingMeasure measure : values()) {
            if (measure.code.equalsIgnoreCase(code)) {
                return measure;
            }
        }
        throw new IllegalArgumentException("Unknown Star Rating measure code: " + code);
    }
}
