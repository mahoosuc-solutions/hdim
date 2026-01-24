package com.healthdata.authentication.domain;

/**
 * User roles for Role-Based Access Control (RBAC).
 *
 * HDIM implements a 13-role RBAC system designed for healthcare quality measurement
 * and clinical decision support. Roles are organized by privilege level and functional area.
 *
 * Role Hierarchy (from most to least privileged):
 * 1. SUPER_ADMIN - Full system access across all tenants
 * 2. ADMIN - Tenant-level administration
 * 3. CLINICAL_ADMIN - Clinical operations administration
 * 4. QUALITY_OFFICER - Quality measurement oversight
 * 5. CLINICIAN - Clinical staff with patient care access
 * 6. MEASURE_DEVELOPER - Quality measure development
 * 7. EVALUATOR - Execute quality measure evaluations
 * 8. CARE_COORDINATOR - Care gap management and coordination
 * 9. AUDITOR - Compliance and audit access
 * 10. ANALYST - Analytics and reporting (read-only)
 * 11. DEVELOPER - API and integration access
 * 12. VIEWER - Basic read-only access
 * 13. RESTRICTED - Minimal access for specific workflows
 *
 * Permission Model:
 * - Each role has associated permissions (see Permission enum)
 * - Permissions are checked via @PreAuthorize("hasPermission(...)")
 * - Role hierarchy provides implicit permission inheritance
 */
public enum UserRole {
    /**
     * Super Administrator role.
     * - Full system access across all tenants
     * - User and tenant management
     * - System configuration
     * - All permissions granted
     *
     * Use Cases: Platform operators, SRE teams
     */
    SUPER_ADMIN("Super Administrator", "Full system access with all permissions"),

    /**
     * Administrator role.
     * - Tenant-level administration
     * - User management within tenant
     * - Configuration and settings
     * - All tenant-level permissions
     *
     * Use Cases: Health plan IT administrators, tenant admins
     */
    ADMIN("Administrator", "Tenant administration and configuration"),

    /**
     * Clinical Administrator role.
     * - Clinical operations management
     * - Clinical workflow configuration
     * - Care gap management oversight
     * - Clinical staff management
     *
     * Use Cases: Chief clinical officers, nursing directors
     */
    CLINICAL_ADMIN("Clinical Administrator", "Clinical operations and workflow management"),

    /**
     * Quality Officer role.
     * - Quality measurement oversight
     * - HEDIS/CMS measure management
     * - Quality reporting and compliance
     * - Audit review and approval
     *
     * Use Cases: Quality directors, HEDIS coordinators
     */
    QUALITY_OFFICER("Quality Officer", "Quality measurement and compliance oversight"),

    /**
     * Clinician role.
     * - Patient care access (demographics, clinical data)
     * - Care gap review and closure
     * - Clinical decision support access
     * - Quality measure review
     *
     * Use Cases: Physicians, nurse practitioners, PAs
     */
    CLINICIAN("Clinician", "Clinical staff with patient care access"),

    /**
     * Measure Developer role.
     * - Create and manage measure definitions
     * - CQL authoring and testing
     * - Value set management
     * - Measure version control
     *
     * Use Cases: Clinical informaticists, quality measure authors
     */
    MEASURE_DEVELOPER("Measure Developer", "Create and manage quality measures"),

    /**
     * Evaluator role.
     * - Execute quality measure evaluations
     * - Run CQL evaluations
     * - View evaluation results
     * - Generate quality reports
     *
     * Use Cases: Quality analysts, evaluation specialists
     */
    EVALUATOR("Evaluator", "Execute quality measure evaluations"),

    /**
     * Care Coordinator role.
     * - Care gap identification and tracking
     * - Patient outreach management
     * - Care plan coordination
     * - Gap closure documentation
     *
     * Use Cases: Care managers, patient navigators
     */
    CARE_COORDINATOR("Care Coordinator", "Care gap management and patient coordination"),

    /**
     * Auditor role.
     * - Access audit logs and compliance reports
     * - PHI access auditing (HIPAA §164.312(b))
     * - Security incident investigation
     * - Read-only access to clinical data
     *
     * Use Cases: Compliance officers, security auditors
     */
    AUDITOR("Auditor", "Compliance monitoring and audit access"),

    /**
     * Analyst role.
     * - View evaluation results and analytics
     * - Generate reports and dashboards
     * - Export data for analysis
     * - Read-only access (no execution)
     *
     * Use Cases: Data analysts, business intelligence teams
     */
    ANALYST("Analyst", "Analytics and reporting (read-only)"),

    /**
     * Developer role.
     * - API access for integrations
     * - Test environment access
     * - Webhook configuration
     * - No PHI access (synthetic data only)
     *
     * Use Cases: Integration developers, API consumers
     */
    DEVELOPER("Developer", "API and integration development access"),

    /**
     * Viewer role.
     * - Basic read-only access
     * - View measure definitions
     * - Limited evaluation results
     * - No PHI access
     *
     * Use Cases: External stakeholders, read-only users
     */
    VIEWER("Viewer", "Basic read-only access"),

    /**
     * Restricted role.
     * - Minimal access for specific workflows
     * - No general system access
     * - Explicitly granted permissions only
     *
     * Use Cases: Limited-scope service accounts, temporary access
     */
    RESTRICTED("Restricted User", "Minimal access for specific workflows");

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
     * Check if role is administrative (SUPER_ADMIN, ADMIN, or CLINICAL_ADMIN).
     */
    public boolean isAdministrative() {
        return this == SUPER_ADMIN || this == ADMIN || this == CLINICAL_ADMIN;
    }

    /**
     * Check if role can execute evaluations.
     */
    public boolean canExecuteEvaluations() {
        return this == SUPER_ADMIN || this == ADMIN || this == MEASURE_DEVELOPER ||
               this == EVALUATOR || this == QUALITY_OFFICER;
    }

    /**
     * Check if role has clinical access (patient data, care gaps).
     */
    public boolean hasClinicalAccess() {
        return this == SUPER_ADMIN || this == ADMIN || this == CLINICAL_ADMIN ||
               this == CLINICIAN || this == QUALITY_OFFICER || this == CARE_COORDINATOR;
    }

    /**
     * Check if role can access PHI (Protected Health Information).
     * HIPAA compliance: Only clinical roles and auditors can access PHI.
     */
    public boolean canAccessPHI() {
        return this == SUPER_ADMIN || this == ADMIN || this == CLINICAL_ADMIN ||
               this == CLINICIAN || this == QUALITY_OFFICER || this == CARE_COORDINATOR ||
               this == AUDITOR;
    }

    /**
     * Check if role is read-only (no write/execute permissions).
     */
    public boolean isReadOnly() {
        return this == ANALYST || this == VIEWER || this == AUDITOR || this == RESTRICTED;
    }

    /**
     * Check if role can manage users.
     */
    public boolean canManageUsers() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    /**
     * Check if role can configure system settings.
     */
    public boolean canConfigureSystem() {
        return this == SUPER_ADMIN || this == ADMIN || this == CLINICAL_ADMIN;
    }
}
