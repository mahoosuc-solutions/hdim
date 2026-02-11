package com.healthdata.authentication.domain;

/**
 * System permissions for fine-grained access control.
 *
 * HDIM implements a permission-based authorization model where:
 * - Roles are assigned to users
 * - Permissions are assigned to roles
 * - @PreAuthorize checks permissions, not roles
 *
 * Permission Categories:
 * 1. User Management (4 permissions)
 * 2. Quality Measures (5 permissions)
 * 3. Patient Data (5 permissions)
 * 4. Care Gaps (4 permissions)
 * 5. Audit & Compliance (3 permissions)
 * 6. Configuration (4 permissions)
 * 7. API & Integration (3 permissions)
 * 8. Reporting (3 permissions)
 *
 * Total: 31 permissions
 *
 * Usage:
 * <pre>
 * @PreAuthorize("hasPermission('PATIENT_READ')")
 * public PatientResponse getPatient(String patientId) { ... }
 * </pre>
 *
 * Permission Naming Convention:
 * - {RESOURCE}_{ACTION}
 * - Actions: READ, WRITE, EXECUTE, DELETE, MANAGE
 * - Resources: PATIENT, MEASURE, USER, AUDIT, etc.
 *
 * NOTE: This class is in authentication-core module (NO Spring dependencies).
 * It can be safely used by any service without triggering entity scanning.
 */
public enum Permission {

    // ========================================
    // User Management Permissions (4)
    // ========================================

    /**
     * View user accounts and profiles.
     * Granted to: SUPER_ADMIN, ADMIN
     */
    USER_READ("View users", "View user accounts and profiles"),

    /**
     * Create and update user accounts.
     * Granted to: SUPER_ADMIN, ADMIN
     */
    USER_WRITE("Manage users", "Create and update user accounts"),

    /**
     * Delete user accounts.
     * Granted to: SUPER_ADMIN only
     */
    USER_DELETE("Delete users", "Delete user accounts"),

    /**
     * Assign roles and permissions to users.
     * Granted to: SUPER_ADMIN, ADMIN
     */
    USER_MANAGE_ROLES("Manage user roles", "Assign roles and permissions"),

    // ========================================
    // Quality Measures Permissions (5)
    // ========================================

    /**
     * View quality measure definitions and metadata.
     * Granted to: All roles except RESTRICTED
     */
    MEASURE_READ("View measures", "View quality measure definitions"),

    /**
     * Create and update quality measure definitions.
     * Granted to: SUPER_ADMIN, ADMIN, MEASURE_DEVELOPER
     */
    MEASURE_WRITE("Manage measures", "Create and update measure definitions"),

    /**
     * Delete quality measure definitions.
     * Granted to: SUPER_ADMIN, ADMIN
     */
    MEASURE_DELETE("Delete measures", "Delete quality measure definitions"),

    /**
     * Execute quality measure evaluations.
     * Granted to: SUPER_ADMIN, ADMIN, QUALITY_OFFICER, MEASURE_DEVELOPER, EVALUATOR
     */
    MEASURE_EXECUTE("Execute evaluations", "Run quality measure evaluations"),

    /**
     * Publish quality measures to production.
     * Granted to: SUPER_ADMIN, ADMIN, QUALITY_OFFICER
     */
    MEASURE_PUBLISH("Publish measures", "Publish measures to production"),

    // ========================================
    // Patient Data Permissions (5)
    // ========================================

    /**
     * View patient demographics and clinical data (PHI).
     * HIPAA: Requires audit logging.
     * Granted to: SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, CLINICIAN, QUALITY_OFFICER, CARE_COORDINATOR, AUDITOR
     */
    PATIENT_READ("View patients", "View patient demographics and clinical data"),

    /**
     * Create and update patient records.
     * Granted to: SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, CLINICIAN
     */
    PATIENT_WRITE("Manage patients", "Create and update patient records"),

    /**
     * Delete patient records.
     * Granted to: SUPER_ADMIN only
     */
    PATIENT_DELETE("Delete patients", "Delete patient records"),

    /**
     * Search and query patient data across tenants.
     * Granted to: SUPER_ADMIN, ADMIN
     */
    PATIENT_SEARCH("Search patients", "Search patient data across system"),

    /**
     * Export patient data for reporting.
     * HIPAA: Requires de-identification or BAA.
     * Granted to: SUPER_ADMIN, ADMIN, QUALITY_OFFICER, ANALYST
     */
    PATIENT_EXPORT("Export patient data", "Export patient data for reporting"),

    // ========================================
    // Care Gaps Permissions (4)
    // ========================================

    /**
     * View care gaps and quality opportunities.
     * Granted to: SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, CLINICIAN, QUALITY_OFFICER, CARE_COORDINATOR, ANALYST
     */
    CARE_GAP_READ("View care gaps", "View care gaps and quality opportunities"),

    /**
     * Create and update care gap records.
     * Granted to: SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, CLINICIAN, CARE_COORDINATOR
     */
    CARE_GAP_WRITE("Manage care gaps", "Create and update care gap records"),

    /**
     * Close and resolve care gaps.
     * Granted to: SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, CLINICIAN, CARE_COORDINATOR
     */
    CARE_GAP_CLOSE("Close care gaps", "Close and resolve care gaps"),

    /**
     * Assign care gaps to care coordinators.
     * Granted to: SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, QUALITY_OFFICER
     */
    CARE_GAP_ASSIGN("Assign care gaps", "Assign care gaps to coordinators"),

    // ========================================
    // Audit & Compliance Permissions (3)
    // ========================================

    /**
     * View audit logs and access history.
     * HIPAA §164.312(b): Required for PHI access auditing.
     * Granted to: SUPER_ADMIN, ADMIN, AUDITOR, QUALITY_OFFICER
     */
    AUDIT_READ("View audit logs", "View audit logs and access history"),

    /**
     * Export audit logs for compliance reporting.
     * Granted to: SUPER_ADMIN, AUDITOR
     */
    AUDIT_EXPORT("Export audit logs", "Export audit logs for compliance"),

    /**
     * Review and approve compliance reports.
     * Granted to: SUPER_ADMIN, QUALITY_OFFICER, AUDITOR
     */
    AUDIT_REVIEW("Review compliance", "Review and approve compliance reports"),

    // ========================================
    // Configuration Permissions (4)
    // ========================================

    /**
     * View system configuration and settings.
     * Granted to: SUPER_ADMIN, ADMIN, CLINICAL_ADMIN, DEVELOPER
     */
    CONFIG_READ("View configuration", "View system settings"),

    /**
     * Update system configuration and settings.
     * Granted to: SUPER_ADMIN, ADMIN, CLINICAL_ADMIN
     */
    CONFIG_WRITE("Manage configuration", "Update system settings"),

    /**
     * Manage tenant settings and multi-tenant configuration.
     * Granted to: SUPER_ADMIN only
     */
    TENANT_MANAGE("Manage tenants", "Manage tenant configuration"),

    /**
     * Configure integrations and external connections.
     * Granted to: SUPER_ADMIN, ADMIN, DEVELOPER
     */
    INTEGRATION_MANAGE("Manage integrations", "Configure integrations and APIs"),

    // ========================================
    // API & Integration Permissions (3)
    // ========================================

    /**
     * Access FHIR APIs for data retrieval.
     * Granted to: SUPER_ADMIN, ADMIN, DEVELOPER, EVALUATOR, CLINICIAN
     */
    API_READ("API read access", "Access APIs for data retrieval"),

    /**
     * Use FHIR APIs for data modification.
     * Granted to: SUPER_ADMIN, ADMIN, DEVELOPER
     */
    API_WRITE("API write access", "Use APIs for data modification"),

    /**
     * Generate and manage API keys.
     * Granted to: SUPER_ADMIN, ADMIN, DEVELOPER
     */
    API_MANAGE_KEYS("Manage API keys", "Generate and manage API keys"),

    // ========================================
    // Reporting Permissions (3)
    // ========================================

    /**
     * View quality reports and dashboards.
     * Granted to: All roles except RESTRICTED
     */
    REPORT_READ("View reports", "View quality reports and dashboards"),

    /**
     * Create and schedule custom reports.
     * Granted to: SUPER_ADMIN, ADMIN, QUALITY_OFFICER, ANALYST
     */
    REPORT_CREATE("Create reports", "Create and schedule custom reports"),

    /**
     * Export reports for external distribution.
     * Granted to: SUPER_ADMIN, ADMIN, QUALITY_OFFICER, ANALYST
     */
    REPORT_EXPORT("Export reports", "Export reports for distribution");

    private final String displayName;
    private final String description;

    Permission(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this is a PHI-related permission.
     * PHI permissions require additional audit logging per HIPAA §164.312(b).
     */
    public boolean isPHIPermission() {
        return this == PATIENT_READ || this == PATIENT_WRITE || this == PATIENT_DELETE ||
               this == PATIENT_EXPORT || this == CARE_GAP_READ || this == CARE_GAP_WRITE;
    }

    /**
     * Check if this is an administrative permission.
     */
    public boolean isAdministrative() {
        return this == USER_WRITE || this == USER_DELETE || this == USER_MANAGE_ROLES ||
               this == CONFIG_WRITE || this == TENANT_MANAGE;
    }

    /**
     * Get permission category.
     */
    public PermissionCategory getCategory() {
        if (name().startsWith("USER_")) return PermissionCategory.USER_MANAGEMENT;
        if (name().startsWith("MEASURE_")) return PermissionCategory.QUALITY_MEASURES;
        if (name().startsWith("PATIENT_")) return PermissionCategory.PATIENT_DATA;
        if (name().startsWith("CARE_GAP_")) return PermissionCategory.CARE_GAPS;
        if (name().startsWith("AUDIT_")) return PermissionCategory.AUDIT_COMPLIANCE;
        if (name().startsWith("CONFIG_") || name().startsWith("TENANT_") || name().startsWith("INTEGRATION_"))
            return PermissionCategory.CONFIGURATION;
        if (name().startsWith("API_")) return PermissionCategory.API_INTEGRATION;
        if (name().startsWith("REPORT_")) return PermissionCategory.REPORTING;
        return PermissionCategory.OTHER;
    }

    /**
     * Permission categories for organization.
     */
    public enum PermissionCategory {
        USER_MANAGEMENT,
        QUALITY_MEASURES,
        PATIENT_DATA,
        CARE_GAPS,
        AUDIT_COMPLIANCE,
        CONFIGURATION,
        API_INTEGRATION,
        REPORTING,
        OTHER
    }
}
