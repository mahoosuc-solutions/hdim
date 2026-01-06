package com.healthdata.notification.domain.model;

/**
 * Types of notifications that providers can configure preferences for.
 * Issue #148: Smart Notification Preferences
 */
public enum NotificationType {
    /**
     * Critical results requiring immediate attention.
     * Always enabled - cannot be disabled.
     */
    CRITICAL_RESULT("Critical Results", true, "Abnormal lab values, critical findings"),

    /**
     * Care gap overdue notifications.
     * Configurable by provider.
     */
    CARE_GAP_OVERDUE("Care Gap Overdue", false, "Overdue preventive care and quality measures"),

    /**
     * Quality measure updates.
     * Configurable by provider.
     */
    QUALITY_MEASURE_UPDATE("Quality Measure Updates", false, "Changes to quality measure performance"),

    /**
     * Patient messages and communications.
     * Configurable by provider.
     */
    PATIENT_MESSAGE("Patient Messages", false, "Messages from patients"),

    /**
     * Risk score changes.
     * Configurable by provider.
     */
    RISK_SCORE_CHANGE("Risk Score Changes", false, "Significant changes in patient risk scores"),

    /**
     * Prior authorization updates.
     * Configurable by provider.
     */
    PRIOR_AUTH_UPDATE("Prior Auth Updates", false, "Prior authorization status changes"),

    /**
     * Care team notifications.
     * Configurable by provider.
     */
    CARE_TEAM_UPDATE("Care Team Updates", false, "Updates from care team members");

    private final String displayName;
    private final boolean alwaysEnabled;
    private final String description;

    NotificationType(String displayName, boolean alwaysEnabled, String description) {
        this.displayName = displayName;
        this.alwaysEnabled = alwaysEnabled;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAlwaysEnabled() {
        return alwaysEnabled;
    }

    public String getDescription() {
        return description;
    }
}
