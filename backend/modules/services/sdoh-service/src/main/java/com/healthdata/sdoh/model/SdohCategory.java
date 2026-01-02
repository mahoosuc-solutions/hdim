package com.healthdata.sdoh.model;

/**
 * SDOH Category Enumeration
 *
 * Based on Gravity Project SDOH domains and categories
 */
public enum SdohCategory {
    FOOD_INSECURITY("Food Insecurity", "88122-7"),
    HOUSING_INSTABILITY("Housing Instability", "71802-3"),
    TRANSPORTATION("Transportation Insecurity", "93030-5"),
    FINANCIAL_STRAIN("Financial Strain", "96777-8"),
    EDUCATION("Educational Attainment", "82589-3"),
    EMPLOYMENT("Employment Status", "67875-5"),
    UTILITIES("Utilities Insecurity", "93031-3"),
    SOCIAL_ISOLATION("Social Isolation", "93029-7"),
    INTERPERSONAL_VIOLENCE("Interpersonal Violence", "93038-8"),
    HEALTH_LITERACY("Health Literacy", "71969-0"),
    SUBSTANCE_USE("Substance Use", "96842-0"),
    MENTAL_HEALTH("Mental Health", "93026-3"),
    DISABILITY("Disability Status", "69858-9"),
    IMMIGRATION("Immigration Status", "93032-1");

    private final String displayName;
    private final String loincCode;

    SdohCategory(String displayName, String loincCode) {
        this.displayName = displayName;
        this.loincCode = loincCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLoincCode() {
        return loincCode;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
