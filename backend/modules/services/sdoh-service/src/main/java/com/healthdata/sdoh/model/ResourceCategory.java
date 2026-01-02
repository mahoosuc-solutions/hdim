package com.healthdata.sdoh.model;

/**
 * Community Resource Category Enumeration
 */
public enum ResourceCategory {
    FOOD("Food Assistance", "Food banks, meal programs, SNAP assistance"),
    HOUSING("Housing Assistance", "Emergency shelter, affordable housing, housing counseling"),
    TRANSPORTATION("Transportation", "Public transit, medical transportation, ride sharing"),
    UTILITIES("Utilities Assistance", "Energy assistance, water/gas bill payment programs"),
    EMPLOYMENT("Employment Services", "Job training, employment placement, career counseling"),
    EDUCATION("Education Services", "Adult education, GED programs, literacy programs"),
    FINANCIAL("Financial Services", "Financial counseling, debt management, tax assistance"),
    HEALTHCARE("Healthcare Access", "Free/low-cost clinics, health insurance enrollment"),
    MENTAL_HEALTH("Mental Health Services", "Counseling, substance abuse treatment, crisis services"),
    LEGAL("Legal Services", "Legal aid, immigration assistance, advocacy"),
    CHILDCARE("Childcare Services", "Daycare, after-school programs, early childhood education"),
    ELDERCARE("Elder Care Services", "Senior centers, home care, adult day programs");

    private final String displayName;
    private final String description;

    ResourceCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
