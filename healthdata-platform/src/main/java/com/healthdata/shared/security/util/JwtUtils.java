package com.healthdata.shared.security.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JWT Utilities - Helper methods for JWT and authentication operations
 *
 * Provides convenient methods for accessing current authentication context
 * and extracting user information from SecurityContext.
 */
@Slf4j
public class JwtUtils {

    private JwtUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Get current authentication from SecurityContext
     *
     * @return Current Authentication or null if not authenticated
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Get current authenticated username
     *
     * @return Username if authenticated, null otherwise
     */
    public static String getCurrentUsername() {
        Authentication auth = getCurrentAuthentication();
        return auth != null ? auth.getName() : null;
    }

    /**
     * Check if current user is authenticated
     *
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication auth = getCurrentAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    /**
     * Check if current user has a specific role
     *
     * @param role Role to check
     * @return true if user has the role
     */
    public static boolean hasRole(String role) {
        Authentication auth = getCurrentAuthentication();
        if (auth == null) {
            return false;
        }

        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleWithPrefix));
    }

    /**
     * Check if current user has any of the specified roles
     *
     * @param roles Roles to check
     * @return true if user has any role
     */
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if current user has all specified roles
     *
     * @param roles Roles to check
     * @return true if user has all roles
     */
    public static boolean hasAllRoles(String... roles) {
        for (String role : roles) {
            if (!hasRole(role)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clear the current security context
     * Useful for logout operations
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        log.debug("Security context cleared");
    }
}
