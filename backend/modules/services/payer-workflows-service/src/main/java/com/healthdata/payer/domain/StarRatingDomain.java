package com.healthdata.payer.domain;

import lombok.Getter;

/**
 * Star Rating domains group related measures together.
 * Each domain contributes to the overall Star Rating calculation.
 *
 * Based on CMS Medicare Advantage Star Rating methodology.
 */
@Getter
public enum StarRatingDomain {

    STAYING_HEALTHY(
        "Staying Healthy: Screenings, Tests and Vaccines",
        "Preventive care and health maintenance measures",
        1.0
    ),

    MANAGING_CHRONIC_CONDITIONS(
        "Managing Chronic (Long-Term) Conditions",
        "Disease management and control of chronic conditions",
        1.5  // Higher weight for chronic disease management
    ),

    MEMBER_EXPERIENCE(
        "Member Experience with Health Plan",
        "Patient satisfaction and access to care",
        1.0
    ),

    COMPLAINTS_AND_PERFORMANCE(
        "Member Complaints and Changes in the Health Plan's Performance",
        "Plan responsiveness and quality improvement",
        1.0
    ),

    DRUG_PLAN(
        "Drug Plan Customer Service",
        "Prescription drug plan satisfaction",
        1.0
    ),

    DRUG_SAFETY(
        "Patient Safety and Accuracy of Drug Pricing",
        "Medication safety and appropriate prescribing",
        1.5  // Higher weight for safety
    );

    private final String displayName;
    private final String description;
    private final double domainWeight;

    StarRatingDomain(String displayName, String description, double domainWeight) {
        this.displayName = displayName;
        this.description = description;
        this.domainWeight = domainWeight;
    }
}
