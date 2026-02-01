package com.healthdata.gateway.security;

import com.healthdata.authentication.domain.Permission;
import com.healthdata.authentication.domain.RolePermissions;
import com.healthdata.authentication.domain.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom PermissionEvaluator for HDIM's permission-based authorization.
 *
 * Integrates with Spring Security's @PreAuthorize to enable:
 * <pre>
 * @PreAuthorize("hasPermission('PATIENT_READ')")
 * public PatientResponse getPatient(String patientId) { ... }
 * </pre>
 *
 * Permission Evaluation Logic:
 * 1. Extract user roles from Spring Security Authentication
 * 2. Query RolePermissions utility for permission grants
 * 3. Return true if ANY role grants the requested permission
 *
 * Multi-Tenant Support:
 * - Permissions are checked per user regardless of tenant
 * - Tenant-level access control handled by TrustedTenantAccessFilter
 * - This evaluator only validates permission grants
 *
 * HIPAA Compliance:
 * - All permission checks are audit logged
 * - PHI permissions (PATIENT_READ, etc.) trigger enhanced logging
 * - Denials are logged for security incident investigation
 *
 * Usage Examples:
 * <pre>
 * // Simple permission check
 * @PreAuthorize("hasPermission('PATIENT_READ')")
 * public PatientResponse getPatient(String patientId) { ... }
 *
 * // Multiple permissions (any)
 * @PreAuthorize("hasPermission('PATIENT_READ') or hasPermission('PATIENT_WRITE')")
 * public PatientResponse updatePatient(String patientId, UpdateRequest request) { ... }
 *
 * // Combined with role check (not recommended)
 * @PreAuthorize("hasRole('ADMIN') and hasPermission('MEASURE_PUBLISH')")
 * public void publishMeasure(String measureId) { ... }
 * </pre>
 *
 * Performance:
 * - Static role-permission map (no database queries)
 * - O(1) permission lookup per role
 * - Minimal overhead on request processing
 */
@Component
@Slf4j
public class HdimPermissionEvaluator implements PermissionEvaluator {

    /**
     * Evaluate permission for a target domain object.
     *
     * This method is called by Spring Security when evaluating expressions like:
     * @PreAuthorize("hasPermission(#patient, 'PATIENT_READ')")
     *
     * @param authentication Current user authentication
     * @param targetDomainObject Domain object being accessed (e.g., Patient entity)
     * @param permission Permission name (e.g., "PATIENT_READ")
     * @return true if user has the permission
     */
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Permission check failed: User not authenticated");
            return false;
        }

        if (permission == null) {
            log.warn("Permission check failed: Permission is null");
            return false;
        }

        String permissionName = permission.toString();
        boolean hasPermission = evaluatePermission(authentication, permissionName);

        if (!hasPermission) {
            log.warn("Permission denied: user={}, permission={}, target={}",
                authentication.getName(), permissionName,
                targetDomainObject != null ? targetDomainObject.getClass().getSimpleName() : "null");
        }

        return hasPermission;
    }

    /**
     * Evaluate permission for a target by identifier.
     *
     * This method is called by Spring Security when evaluating expressions like:
     * @PreAuthorize("hasPermission(#patientId, 'Patient', 'PATIENT_READ')")
     *
     * @param authentication Current user authentication
     * @param targetId Target resource ID
     * @param targetType Target resource type (e.g., "Patient")
     * @param permission Permission name (e.g., "PATIENT_READ")
     * @return true if user has the permission
     */
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Permission check failed: User not authenticated");
            return false;
        }

        if (permission == null) {
            log.warn("Permission check failed: Permission is null");
            return false;
        }

        String permissionName = permission.toString();
        boolean hasPermission = evaluatePermission(authentication, permissionName);

        if (!hasPermission) {
            log.warn("Permission denied: user={}, permission={}, targetType={}, targetId={}",
                authentication.getName(), permissionName, targetType, targetId);
        }

        return hasPermission;
    }

    /**
     * Core permission evaluation logic.
     *
     * Extracts user roles from Authentication and checks if any role grants the permission.
     *
     * @param authentication Current user authentication
     * @param permissionName Permission name (e.g., "PATIENT_READ")
     * @return true if user has the permission
     */
    private boolean evaluatePermission(Authentication authentication, String permissionName) {
        // Parse permission enum
        Permission permission;
        try {
            permission = Permission.valueOf(permissionName);
        } catch (IllegalArgumentException e) {
            log.error("Invalid permission name: {}", permissionName);
            return false;
        }

        // Extract user roles from Spring Security authorities
        Set<UserRole> userRoles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring(5)) // Remove "ROLE_" prefix
            .map(roleName -> {
                try {
                    return UserRole.valueOf(roleName);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown role in authorities: {}", roleName);
                    return null;
                }
            })
            .filter(role -> role != null)
            .collect(Collectors.toSet());

        if (userRoles.isEmpty()) {
            log.warn("User has no valid roles: user={}", authentication.getName());
            return false;
        }

        // Check if any role grants the permission
        boolean hasPermission = RolePermissions.hasPermission(userRoles, permission);

        if (hasPermission) {
            // Log PHI access (HIPAA §164.312(b) requirement)
            if (permission.isPHIPermission()) {
                log.info("PHI permission granted: user={}, permission={}, roles={}",
                    authentication.getName(), permissionName, userRoles);
            } else {
                log.debug("Permission granted: user={}, permission={}, roles={}",
                    authentication.getName(), permissionName, userRoles);
            }
        }

        return hasPermission;
    }

    /**
     * Check if user has a specific permission (utility method for programmatic checks).
     *
     * @param authentication Current user authentication
     * @param permission Permission to check
     * @return true if user has the permission
     */
    public boolean hasPermission(Authentication authentication, Permission permission) {
        return evaluatePermission(authentication, permission.name());
    }

    /**
     * Check if user has any of the specified permissions (utility method).
     *
     * @param authentication Current user authentication
     * @param permissions Permissions to check (any match returns true)
     * @return true if user has at least one permission
     */
    public boolean hasAnyPermission(Authentication authentication, Permission... permissions) {
        for (Permission permission : permissions) {
            if (evaluatePermission(authentication, permission.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has all of the specified permissions (utility method).
     *
     * @param authentication Current user authentication
     * @param permissions Permissions to check (all must match)
     * @return true if user has all permissions
     */
    public boolean hasAllPermissions(Authentication authentication, Permission... permissions) {
        for (Permission permission : permissions) {
            if (!evaluatePermission(authentication, permission.name())) {
                return false;
            }
        }
        return true;
    }
}
