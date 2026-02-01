package com.healthdata.authentication.domain;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Role-Permission mapping for HDIM RBAC system.
 *
 * This class defines which permissions each role has access to.
 * Permissions are cumulative based on role privilege level.
 *
 * Permission Inheritance:
 * - Higher privilege roles inherit permissions from lower privilege roles
 * - SUPER_ADMIN has all permissions
 * - Roles can have overlapping permissions for shared functionality
 *
 * HIPAA Compliance:
 * - PHI access permissions are restricted to clinical roles
 * - Audit permissions are restricted to compliance roles
 * - All permission checks are audit logged
 */
public class RolePermissions {

    private static final Map<UserRole, Set<Permission>> ROLE_PERMISSION_MAP = new HashMap<>();

    static {
        // SUPER_ADMIN: All permissions
        ROLE_PERMISSION_MAP.put(UserRole.SUPER_ADMIN, EnumSet.allOf(Permission.class));

        // ADMIN: Tenant-level administration (all except cross-tenant operations)
        ROLE_PERMISSION_MAP.put(UserRole.ADMIN, EnumSet.of(
            // User Management
            Permission.USER_READ,
            Permission.USER_WRITE,
            Permission.USER_MANAGE_ROLES,
            // Quality Measures
            Permission.MEASURE_READ,
            Permission.MEASURE_WRITE,
            Permission.MEASURE_DELETE,
            Permission.MEASURE_EXECUTE,
            Permission.MEASURE_PUBLISH,
            // Patient Data
            Permission.PATIENT_READ,
            Permission.PATIENT_WRITE,
            Permission.PATIENT_SEARCH,
            Permission.PATIENT_EXPORT,
            // Care Gaps
            Permission.CARE_GAP_READ,
            Permission.CARE_GAP_WRITE,
            Permission.CARE_GAP_CLOSE,
            Permission.CARE_GAP_ASSIGN,
            // Audit
            Permission.AUDIT_READ,
            Permission.AUDIT_EXPORT,
            Permission.AUDIT_REVIEW,
            // Configuration
            Permission.CONFIG_READ,
            Permission.CONFIG_WRITE,
            Permission.INTEGRATION_MANAGE,
            // API
            Permission.API_READ,
            Permission.API_WRITE,
            Permission.API_MANAGE_KEYS,
            // Reporting
            Permission.REPORT_READ,
            Permission.REPORT_CREATE,
            Permission.REPORT_EXPORT
        ));

        // CLINICAL_ADMIN: Clinical operations management
        ROLE_PERMISSION_MAP.put(UserRole.CLINICAL_ADMIN, EnumSet.of(
            // User Management (view only)
            Permission.USER_READ,
            // Quality Measures
            Permission.MEASURE_READ,
            Permission.MEASURE_EXECUTE,
            // Patient Data
            Permission.PATIENT_READ,
            Permission.PATIENT_WRITE,
            Permission.PATIENT_SEARCH,
            Permission.PATIENT_EXPORT,
            // Care Gaps
            Permission.CARE_GAP_READ,
            Permission.CARE_GAP_WRITE,
            Permission.CARE_GAP_CLOSE,
            Permission.CARE_GAP_ASSIGN,
            // Configuration
            Permission.CONFIG_READ,
            Permission.CONFIG_WRITE,
            // Reporting
            Permission.REPORT_READ,
            Permission.REPORT_CREATE,
            Permission.REPORT_EXPORT
        ));

        // QUALITY_OFFICER: Quality measurement oversight
        ROLE_PERMISSION_MAP.put(UserRole.QUALITY_OFFICER, EnumSet.of(
            // Quality Measures
            Permission.MEASURE_READ,
            Permission.MEASURE_EXECUTE,
            Permission.MEASURE_PUBLISH,
            // Patient Data (limited for quality review)
            Permission.PATIENT_READ,
            Permission.PATIENT_EXPORT,
            // Care Gaps
            Permission.CARE_GAP_READ,
            Permission.CARE_GAP_ASSIGN,
            // Audit
            Permission.AUDIT_READ,
            Permission.AUDIT_REVIEW,
            // Reporting
            Permission.REPORT_READ,
            Permission.REPORT_CREATE,
            Permission.REPORT_EXPORT
        ));

        // CLINICIAN: Clinical staff with patient care access
        ROLE_PERMISSION_MAP.put(UserRole.CLINICIAN, EnumSet.of(
            // Patient Data
            Permission.PATIENT_READ,
            Permission.PATIENT_WRITE,
            // Care Gaps
            Permission.CARE_GAP_READ,
            Permission.CARE_GAP_WRITE,
            Permission.CARE_GAP_CLOSE,
            // Quality Measures (view only)
            Permission.MEASURE_READ,
            // API (read for FHIR access)
            Permission.API_READ,
            // Reporting (view only)
            Permission.REPORT_READ
        ));

        // MEASURE_DEVELOPER: Quality measure development
        ROLE_PERMISSION_MAP.put(UserRole.MEASURE_DEVELOPER, EnumSet.of(
            // Quality Measures
            Permission.MEASURE_READ,
            Permission.MEASURE_WRITE,
            Permission.MEASURE_EXECUTE,
            // Patient Data (for testing)
            Permission.PATIENT_READ,
            // API (for FHIR access)
            Permission.API_READ,
            // Reporting
            Permission.REPORT_READ,
            Permission.REPORT_CREATE
        ));

        // EVALUATOR: Execute quality measure evaluations
        ROLE_PERMISSION_MAP.put(UserRole.EVALUATOR, EnumSet.of(
            // Quality Measures
            Permission.MEASURE_READ,
            Permission.MEASURE_EXECUTE,
            // Patient Data (read for evaluations)
            Permission.PATIENT_READ,
            // Care Gaps (read for evaluations)
            Permission.CARE_GAP_READ,
            // API (for FHIR access)
            Permission.API_READ,
            // Reporting
            Permission.REPORT_READ,
            Permission.REPORT_CREATE
        ));

        // CARE_COORDINATOR: Care gap management
        ROLE_PERMISSION_MAP.put(UserRole.CARE_COORDINATOR, EnumSet.of(
            // Patient Data
            Permission.PATIENT_READ,
            Permission.PATIENT_WRITE,
            // Care Gaps
            Permission.CARE_GAP_READ,
            Permission.CARE_GAP_WRITE,
            Permission.CARE_GAP_CLOSE,
            // Reporting (view only)
            Permission.REPORT_READ
        ));

        // AUDITOR: Compliance and audit access
        ROLE_PERMISSION_MAP.put(UserRole.AUDITOR, EnumSet.of(
            // Patient Data (read-only for auditing)
            Permission.PATIENT_READ,
            // Audit
            Permission.AUDIT_READ,
            Permission.AUDIT_EXPORT,
            Permission.AUDIT_REVIEW,
            // Reporting
            Permission.REPORT_READ,
            Permission.REPORT_EXPORT
        ));

        // ANALYST: Analytics and reporting
        ROLE_PERMISSION_MAP.put(UserRole.ANALYST, EnumSet.of(
            // Patient Data (de-identified export only)
            Permission.PATIENT_EXPORT,
            // Care Gaps (read for reporting)
            Permission.CARE_GAP_READ,
            // Quality Measures (read for reporting)
            Permission.MEASURE_READ,
            // Reporting
            Permission.REPORT_READ,
            Permission.REPORT_CREATE,
            Permission.REPORT_EXPORT
        ));

        // DEVELOPER: API and integration access
        ROLE_PERMISSION_MAP.put(UserRole.DEVELOPER, EnumSet.of(
            // Configuration (read for development)
            Permission.CONFIG_READ,
            Permission.INTEGRATION_MANAGE,
            // API
            Permission.API_READ,
            Permission.API_WRITE,
            Permission.API_MANAGE_KEYS,
            // Quality Measures (read for testing)
            Permission.MEASURE_READ
        ));

        // VIEWER: Basic read-only access
        ROLE_PERMISSION_MAP.put(UserRole.VIEWER, EnumSet.of(
            // Quality Measures (read only)
            Permission.MEASURE_READ,
            // Reporting (view only)
            Permission.REPORT_READ
        ));

        // RESTRICTED: Minimal permissions
        ROLE_PERMISSION_MAP.put(UserRole.RESTRICTED, EnumSet.noneOf(Permission.class));
    }

    /**
     * Get all permissions for a role.
     *
     * @param role User role
     * @return Set of permissions granted to the role
     */
    public static Set<Permission> getPermissions(UserRole role) {
        return ROLE_PERMISSION_MAP.getOrDefault(role, EnumSet.noneOf(Permission.class));
    }

    /**
     * Get all permissions for multiple roles (union).
     *
     * @param roles Set of user roles
     * @return Combined set of permissions from all roles
     */
    public static Set<Permission> getPermissions(Set<UserRole> roles) {
        return roles.stream()
            .flatMap(role -> getPermissions(role).stream())
            .collect(Collectors.toSet());
    }

    /**
     * Check if a role has a specific permission.
     *
     * @param role User role
     * @param permission Permission to check
     * @return true if role has the permission
     */
    public static boolean hasPermission(UserRole role, Permission permission) {
        return getPermissions(role).contains(permission);
    }

    /**
     * Check if any of the roles has a specific permission.
     *
     * @param roles Set of user roles
     * @param permission Permission to check
     * @return true if any role has the permission
     */
    public static boolean hasPermission(Set<UserRole> roles, Permission permission) {
        return roles.stream().anyMatch(role -> hasPermission(role, permission));
    }

    /**
     * Get all roles that have a specific permission.
     *
     * @param permission Permission to check
     * @return Set of roles that have the permission
     */
    public static Set<UserRole> getRolesWithPermission(Permission permission) {
        return ROLE_PERMISSION_MAP.entrySet().stream()
            .filter(entry -> entry.getValue().contains(permission))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * Get permission matrix (role -> permissions) as read-only map.
     *
     * @return Unmodifiable map of role-permission assignments
     */
    public static Map<UserRole, Set<Permission>> getPermissionMatrix() {
        return Collections.unmodifiableMap(ROLE_PERMISSION_MAP);
    }

    /**
     * Get permission count by role.
     *
     * @return Map of role -> permission count
     */
    public static Map<UserRole, Integer> getPermissionCounts() {
        return ROLE_PERMISSION_MAP.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().size()
            ));
    }
}
