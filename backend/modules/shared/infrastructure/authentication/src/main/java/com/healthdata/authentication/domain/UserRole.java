package com.healthdata.authentication.domain;

/**
 * User roles for Role-Based Access Control (RBAC).
 *
 * Role Hierarchy (from most to least privileged):
 * 1. SUPER_ADMIN - Full system access across all tenants
 * 2. ADMIN - Tenant-level administration
 * 3. MEASURE_DEVELOPER - Can create and manage measure definitions
 * 4. EVALUATOR - Can create and execute evaluations
 * 5. ANALYST - Can view results and analytics (read-only)
 * 6. VIEWER - Can only view basic information (read-only)
 */
public enum UserRole {
    /**
     * Super Administrator role.
     * - Full system access across all tenants
     * - User management
     * - System configuration
     * - Tenant management
     */
    SUPER_ADMIN("Super Administrator", "Full system access with all permissions"),

    /**
     * Administrator role.
     * - Tenant-level administration
     * - User management within tenant
     * - Measure configuration
     * - Value set management
     * - Library management
     */
    ADMIN("Administrator", "Tenant administration and configuration"),

    /**
     * Measure developer role.
     * - Create and manage measure definitions
     * - Maintain measure versions
     * - Execute measure evaluations for validation
     */
    MEASURE_DEVELOPER("Measure Developer", "Create and manage quality measures"),

    /**
     * Evaluator role.
     * - Create and execute CQL evaluations
     * - View evaluation results
     * - Manage own evaluations
     * - Access FHIR resources
     */
    EVALUATOR("Evaluator", "Execute quality measure evaluations"),

    /**
     * Analyst role.
     * - View evaluation results
     * - Generate reports
     * - View analytics and dashboards
     * - Export data (read-only)
     */
    ANALYST("Analyst", "View and analyze evaluation results"),

    /**
     * Viewer role.
     * - Basic read-only access
     * - View measure definitions
     * - View limited evaluation results
     */
    VIEWER("Viewer", "Read-only access to basic information");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
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
     * Check if this role has higher or equal privilege than another role.
     */
    public boolean hasHigherOrEqualPrivilegeThan(UserRole other) {
        return this.ordinal() <= other.ordinal();
    }

    /**
     * Check if role is administrative (SUPER_ADMIN or ADMIN).
     */
    public boolean isAdministrative() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    /**
     * Check if role can execute evaluations.
     */
    public boolean canExecuteEvaluations() {
        return this == SUPER_ADMIN || this == ADMIN || this == MEASURE_DEVELOPER || this == EVALUATOR;
    }

    /**
     * Check if role is read-only.
     */
    public boolean isReadOnly() {
        return this == ANALYST || this == VIEWER;
    }
}
