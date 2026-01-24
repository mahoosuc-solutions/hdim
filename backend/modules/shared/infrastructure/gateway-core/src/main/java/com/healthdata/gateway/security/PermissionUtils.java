package com.healthdata.gateway.security;

import com.healthdata.authentication.domain.Permission;
import com.healthdata.authentication.domain.RolePermissions;
import com.healthdata.authentication.domain.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for programmatic permission checking.
 *
 * Provides convenient methods to check permissions from anywhere in the application
 * without requiring @PreAuthorize annotations.
 *
 * Usage Examples:
 * <pre>
 * // Service method
 * public void processPatient(String patientId) {
 *     if (!PermissionUtils.hasPermission(Permission.PATIENT_READ)) {
 *         throw new AccessDeniedException("Cannot read patient data");
 *     }
 *     // Process patient...
 * }
 *
 * // Controller method
 * public ResponseEntity<?> getPatient(String patientId) {
 *     if (!PermissionUtils.hasAnyPermission(Permission.PATIENT_READ, Permission.PATIENT_WRITE)) {
 *         return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
 *     }
 *     // Return patient...
 * }
 *
 * // Check current user's roles
 * Set<UserRole> roles = PermissionUtils.getCurrentUserRoles();
 * if (roles.contains(UserRole.SUPER_ADMIN)) {
 *     // Admin-only logic...
 * }
 * </pre>
 *
 * Best Practices:
 * - Prefer @PreAuthorize annotations for controller methods
 * - Use PermissionUtils for conditional logic in service layer
 * - Always check permissions before PHI access
 * - Log permission denials for audit trail
 */
@Component
@Slf4j
public class PermissionUtils {

    /**
     * Check if current user has a specific permission.
     *
     * @param permission Permission to check
     * @return true if user has the permission
     */
    public static boolean hasPermission(Permission permission) {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Set<UserRole> userRoles = extractUserRoles(authentication);
        return RolePermissions.hasPermission(userRoles, permission);
    }

    /**
     * Check if current user has any of the specified permissions.
     *
     * @param permissions Permissions to check (any match returns true)
     * @return true if user has at least one permission
     */
    public static boolean hasAnyPermission(Permission... permissions) {
        for (Permission permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if current user has all of the specified permissions.
     *
     * @param permissions Permissions to check (all must match)
     * @return true if user has all permissions
     */
    public static boolean hasAllPermissions(Permission... permissions) {
        for (Permission permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all permissions granted to the current user.
     *
     * @return Set of permissions (empty if not authenticated)
     */
    public static Set<Permission> getCurrentUserPermissions() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }

        Set<UserRole> userRoles = extractUserRoles(authentication);
        return RolePermissions.getPermissions(userRoles);
    }

    /**
     * Get current user's roles.
     *
     * @return Set of roles (empty if not authenticated)
     */
    public static Set<UserRole> getCurrentUserRoles() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }

        return extractUserRoles(authentication);
    }

    /**
     * Get current user's username.
     *
     * @return Username or null if not authenticated
     */
    public static String getCurrentUsername() {
        Authentication authentication = getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    /**
     * Check if current user is authenticated.
     *
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Check if current user has a specific role.
     *
     * @param role Role to check
     * @return true if user has the role
     */
    public static boolean hasRole(UserRole role) {
        return getCurrentUserRoles().contains(role);
    }

    /**
     * Check if current user has any of the specified roles.
     *
     * @param roles Roles to check (any match returns true)
     * @return true if user has at least one role
     */
    public static boolean hasAnyRole(UserRole... roles) {
        Set<UserRole> userRoles = getCurrentUserRoles();
        for (UserRole role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if current user can access PHI.
     * Checks PATIENT_READ permission.
     *
     * @return true if user can access PHI
     */
    public static boolean canAccessPHI() {
        return hasAnyPermission(
            Permission.PATIENT_READ,
            Permission.PATIENT_WRITE,
            Permission.PATIENT_EXPORT,
            Permission.CARE_GAP_READ,
            Permission.CARE_GAP_WRITE
        );
    }

    /**
     * Check if current user is an administrator.
     * Checks for SUPER_ADMIN or ADMIN roles.
     *
     * @return true if user is an administrator
     */
    public static boolean isAdministrator() {
        return hasAnyRole(UserRole.SUPER_ADMIN, UserRole.ADMIN);
    }

    /**
     * Check if current user is a super administrator.
     *
     * @return true if user is a super admin
     */
    public static boolean isSuperAdmin() {
        return hasRole(UserRole.SUPER_ADMIN);
    }

    // --- Private Helper Methods ---

    /**
     * Get current authentication from SecurityContext.
     */
    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Extract UserRole enums from Spring Security authorities.
     *
     * Spring Security stores roles as GrantedAuthority with "ROLE_" prefix.
     * This method strips the prefix and converts to UserRole enum.
     *
     * @param authentication Current authentication
     * @return Set of UserRole enums
     */
    private static Set<UserRole> extractUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
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
    }
}
