package com.healthdata.shared.security.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User Entity - Represents application users with role-based access control
 *
 * Key features:
 * - Multi-tenant support with tenant isolation
 * - Password hashing with BCrypt (handled by service layer)
 * - Role management with many-to-many relationship
 * - Account status tracking (enabled, locked, expired)
 * - Audit fields (created_at, updated_at, last_login)
 * - Soft delete support (deleted_at field)
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_is_active", columnList = "is_active"),
    @Index(name = "idx_is_locked", columnList = "is_locked"),
    @Index(name = "idx_deleted_at", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password", "roles"})
@EqualsAndHashCode(exclude = {"password", "roles"})
public class User implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique user identifier (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private String id;

    /**
     * Username - must be unique across the system
     * Used for login authentication
     */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /**
     * Email address - must be unique across the system
     * Used for notifications and password recovery
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * BCrypt hashed password
     * Never returned in API responses
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * First name of the user
     */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /**
     * Last name of the user
     */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /**
     * Phone number (optional)
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Tenant ID for multi-tenant isolation
     * All user operations must be filtered by this tenant
     */
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    /**
     * Whether the user account is enabled/active
     * Disabled users cannot authenticate
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Whether the user account is locked
     * Locked users cannot authenticate (but account can be unlocked)
     * Typically used after too many failed login attempts
     */
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean locked = false;

    /**
     * Timestamp of the last failed login attempt
     * Used for tracking suspicious activity
     */
    @Column(name = "last_failed_login")
    private LocalDateTime lastFailedLogin;

    /**
     * Number of failed login attempts since last successful login
     * Used to implement account locking
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    /**
     * Timestamp of the last successful login
     * Used for account activity tracking
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Timestamp when password was last changed
     */
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    /**
     * Password expiration in days (for password rotation policy)
     * 0 or null means password never expires
     */
    @Column(name = "password_expiry_days")
    private Integer passwordExpiryDays;

    /**
     * Whether multi-factor authentication (MFA) is enabled
     */
    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    /**
     * Whether email has been verified
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Timestamp when email was verified
     */
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    /**
     * User roles - many-to-many relationship
     * User can have multiple roles (e.g., both PROVIDER and CARE_MANAGER)
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
        indexes = {
            @Index(name = "idx_user_id", columnList = "user_id"),
            @Index(name = "idx_role_id", columnList = "role_id")
        }
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Soft delete timestamp
     * If not null, user is considered deleted (but data is not removed)
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * User who created this user record
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Timestamp when user was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * User who last updated this user record
     */
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Timestamp when user was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Additional user metadata (JSON field)
     * Can store custom attributes like department, role specifics, etc.
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    // ============ Spring Security UserDetails Implementation ============

    /**
     * Get user authorities based on assigned roles
     *
     * @return Collection of GrantedAuthority objects
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add role-based authorities
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

            // Add permission-based authorities
            Set<String> rolePermissions = role.getPermissionsAsSet();
            for (String permission : rolePermissions) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }

        return authorities;
    }

    /**
     * Get user's password
     * Required by UserDetails interface
     *
     * @return Hashed password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Get user's username
     * Required by UserDetails interface
     *
     * @return Username
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Check if account is not expired
     * Required by UserDetails interface
     *
     * @return true if account is not expired
     */
    @Override
    public boolean isAccountNonExpired() {
        return !isAccountExpired();
    }

    /**
     * Check if account is not locked
     * Required by UserDetails interface
     *
     * @return true if account is not locked
     */
    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    /**
     * Check if credentials are not expired
     * Required by UserDetails interface
     *
     * @return true if credentials are not expired
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return !isPasswordExpired();
    }

    /**
     * Check if account is enabled
     * Required by UserDetails interface
     *
     * @return true if account is enabled and not deleted
     */
    @Override
    public boolean isEnabled() {
        return active && deletedAt == null;
    }

    // ============ Custom Methods ============

    /**
     * Check if user's password has expired based on password expiry policy
     *
     * @return true if password has expired
     */
    public boolean isPasswordExpired() {
        if (passwordExpiryDays == null || passwordExpiryDays <= 0 || passwordChangedAt == null) {
            return false;
        }

        LocalDateTime expiryDate = passwordChangedAt.plusDays(passwordExpiryDays);
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Check if account is expired
     * Currently not implemented, but can be extended to support account expiration
     *
     * @return true if account has expired
     */
    public boolean isAccountExpired() {
        return false; // Can be extended to support account expiration dates
    }

    /**
     * Check if user has a specific role
     *
     * @param roleName Role name to check (without "ROLE_" prefix)
     * @return true if user has the role
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Check if user has any of the specified roles
     *
     * @param roleNames Role names to check
     * @return true if user has any of the roles
     */
    public boolean hasAnyRole(String... roleNames) {
        Set<String> userRoleNames = roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        for (String roleName : roleNames) {
            if (userRoleNames.contains(roleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has all specified roles
     *
     * @param roleNames Role names to check
     * @return true if user has all specified roles
     */
    public boolean hasAllRoles(String... roleNames) {
        Set<String> userRoleNames = roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        for (String roleName : roleNames) {
            if (!userRoleNames.contains(roleName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if user has a specific permission
     *
     * @param permission Permission to check
     * @return true if user has the permission through any of their roles
     */
    public boolean hasPermission(String permission) {
        return roles.stream()
                .anyMatch(role -> role.hasPermission(permission));
    }

    /**
     * Add role to user
     *
     * @param role Role to add
     */
    public void addRole(Role role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }

    /**
     * Remove role from user
     *
     * @param role Role to remove
     */
    public void removeRole(Role role) {
        if (roles != null) {
            roles.remove(role);
        }
    }

    /**
     * Get user's full name
     *
     * @return Full name or username if first/last names not set
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return username;
        }
    }

    /**
     * Record failed login attempt
     * Used to implement account locking after too many failures
     */
    public void recordFailedLogin() {
        failedLoginAttempts++;
        lastFailedLogin = LocalDateTime.now();

        // Auto-lock after 5 failed attempts (can be configured)
        if (failedLoginAttempts >= 5) {
            locked = true;
        }
    }

    /**
     * Record successful login
     * Resets failed attempt counter and updates last login timestamp
     */
    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        lastFailedLogin = null;
        lastLogin = LocalDateTime.now();
        locked = false;
    }

    /**
     * Unlock the user account
     * Used by administrators to re-enable locked accounts
     */
    public void unlock() {
        locked = false;
        failedLoginAttempts = 0;
        lastFailedLogin = null;
    }

    /**
     * Check if user is soft-deleted
     *
     * @return true if user is deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft delete the user
     * User record remains in database but is marked as deleted
     */
    public void softDelete() {
        deletedAt = LocalDateTime.now();
        active = false;
    }

    /**
     * Restore a soft-deleted user
     */
    public void restore() {
        deletedAt = null;
        active = true;
    }
}
