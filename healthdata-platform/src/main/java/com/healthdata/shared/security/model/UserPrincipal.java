package com.healthdata.shared.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserPrincipal - Represents authenticated user in SecurityContext
 *
 * Implements Spring Security UserDetails interface.
 * Contains user ID, username, password (hashed), and roles/authorities.
 * Used in authentication tokens and SecurityContext.
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique user identifier (UUID or database ID)
     */
    private String userId;

    /**
     * Username for authentication
     */
    private String username;

    /**
     * Email address of the user
     */
    private String email;

    /**
     * Hashed password (not sent in responses)
     */
    private String password;

    /**
     * User's roles/authorities
     * Examples: "ADMIN", "PROVIDER", "PATIENT", "CARE_MANAGER"
     */
    private List<String> roles;

    /**
     * Whether user account is enabled
     */
    private boolean enabled = true;

    /**
     * Whether user account is not locked
     */
    private boolean accountNonLocked = true;

    /**
     * Whether credentials are not expired
     */
    private boolean credentialsNonExpired = true;

    /**
     * Whether account is not expired
     */
    private boolean accountNonExpired = true;

    /**
     * Create UserPrincipal from JWT token data
     *
     * @param username User's username
     * @param roles User's roles
     * @return UserPrincipal instance
     */
    public static UserPrincipal fromJwt(String username, List<String> roles) {
        return UserPrincipal.builder()
                .username(username)
                .roles(roles)
                .enabled(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .build();
    }

    /**
     * Create UserPrincipal from JWT token data with user ID
     *
     * @param userId User's unique identifier
     * @param username User's username
     * @param roles User's roles
     * @return UserPrincipal instance
     */
    public static UserPrincipal fromJwt(String userId, String username, List<String> roles) {
        return UserPrincipal.builder()
                .userId(userId)
                .username(username)
                .roles(roles)
                .enabled(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .build();
    }

    /**
     * Get authorities based on user roles
     *
     * Converts role names to Spring Security GrantedAuthority objects.
     * Adds "ROLE_" prefix if not already present.
     *
     * @return Collection of GrantedAuthority
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        return roles.stream()
                .map(role -> {
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    return new SimpleGrantedAuthority(authority);
                })
                .collect(Collectors.toList());
    }

    /**
     * Check if user has a specific role
     *
     * @param role Role to check (with or without "ROLE_" prefix)
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        if (roles == null) {
            return false;
        }
        String roleName = role.startsWith("ROLE_") ? role.substring(5) : role;
        return roles.contains(roleName);
    }

    /**
     * Check if user has any of the specified roles
     *
     * @param requiredRoles Roles to check
     * @return true if user has any of the roles
     */
    public boolean hasAnyRole(String... requiredRoles) {
        if (roles == null) {
            return false;
        }
        for (String requiredRole : requiredRoles) {
            String roleName = requiredRole.startsWith("ROLE_") ? requiredRole.substring(5) : requiredRole;
            if (roles.contains(roleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has all specified roles
     *
     * @param requiredRoles Roles to check
     * @return true if user has all roles
     */
    public boolean hasAllRoles(String... requiredRoles) {
        if (roles == null) {
            return false;
        }
        for (String requiredRole : requiredRoles) {
            String roleName = requiredRole.startsWith("ROLE_") ? requiredRole.substring(5) : requiredRole;
            if (!roles.contains(roleName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Username is required for authentication
     * Throws UnsupportedOperationException as UserPrincipal requires explicit initialization
     */
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    /**
     * Check if account is locked
     */
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    /**
     * Check if credentials are not expired
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    /**
     * Check if user account is enabled
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Return string representation of user principal
     */
    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                ", enabled=" + enabled +
                ", accountNonLocked=" + accountNonLocked +
                ", credentialsNonExpired=" + credentialsNonExpired +
                ", accountNonExpired=" + accountNonExpired +
                '}';
    }
}
