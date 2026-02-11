package com.healthdata.agentvalidation.domain.enums;

/**
 * Types of user stories for agent validation testing.
 * Each type maps to a specific role and set of expected agent behaviors.
 */
public enum UserStoryType {

    // Clinician stories (CLINICIAN role)
    PATIENT_SUMMARY_REVIEW("Patient summary review", "CLINICIAN"),
    LAB_INTERPRETATION("Lab result interpretation", "CLINICIAN"),
    MEDICATION_RECONCILIATION("Medication reconciliation", "CLINICIAN"),
    CARE_GAP_EVALUATION("Care gap evaluation", "CLINICIAN"),

    // Care Coordinator stories (CARE_COORDINATOR role)
    CARE_GAP_PRIORITIZATION("Panel prioritization", "CARE_COORDINATOR"),
    OUTREACH_OPTIMIZATION("Outreach planning", "CARE_COORDINATOR"),
    BARRIER_ANALYSIS("Barrier analysis", "CARE_COORDINATOR"),
    BUNDLING_OPPORTUNITIES("Care gap bundling", "CARE_COORDINATOR"),

    // Quality Officer stories (QUALITY_OFFICER role)
    HEDIS_MEASURE_EVALUATION("HEDIS performance analysis", "QUALITY_OFFICER"),
    STAR_RATING_ANALYSIS("Star Rating analysis", "QUALITY_OFFICER"),
    COMPLIANCE_AUDIT("Compliance audit", "QUALITY_OFFICER"),

    // Evaluator stories (EVALUATOR role)
    CQL_MEASURE_EXECUTION("CQL measure execution", "EVALUATOR"),
    BULK_EVALUATION("Bulk population evaluation", "EVALUATOR"),

    // Analyst stories (ANALYST role)
    REPORT_GENERATION("Report generation", "ANALYST"),
    TREND_ANALYSIS("Trend analysis", "ANALYST"),

    // Admin stories (ADMIN role)
    SYSTEM_CONFIGURATION("System configuration review", "ADMIN"),
    USER_MANAGEMENT("User management queries", "ADMIN");

    private final String description;
    private final String targetRole;

    UserStoryType(String description, String targetRole) {
        this.description = description;
        this.targetRole = targetRole;
    }

    public String getDescription() {
        return description;
    }

    public String getTargetRole() {
        return targetRole;
    }
}
