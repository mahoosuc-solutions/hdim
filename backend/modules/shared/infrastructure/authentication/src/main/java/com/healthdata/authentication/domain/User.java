package com.healthdata.authentication.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User entity representing authenticated users in the system.
 * Supports multi-tenancy and role-based access control (RBAC).
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username", unique = true),
    @Index(name = "idx_users_email", columnList = "email", unique = true),
    @Index(name = "idx_users_active", columnList = "active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    /**
     * Tenant ID for multi-tenant isolation.
     * Users can belong to one or more tenants.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_tenants",
        joinColumns = @JoinColumn(name = "user_id"),
        indexes = @Index(name = "idx_user_tenants_tenant_id", columnList = "tenant_id")
    )
    @Column(name = "tenant_id", nullable = false)
    @Builder.Default
    private Set<String> tenantIds = new HashSet<>();

    /**
     * User roles for RBAC.
     * Multiple roles can be assigned to a user.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column
    private Instant lastLoginAt;

    @Column
    private Integer failedLoginAttempts;

    @Column
    private Instant accountLockedUntil;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant deletedAt;

    /**
     * Check if user is locked due to failed login attempts.
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null &&
               accountLockedUntil.isAfter(Instant.now());
    }

    /**
     * Check if user is active and not deleted.
     */
    public boolean isAccountActive() {
        return active && deletedAt == null && !isAccountLocked();
    }

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user has any of the specified roles.
     */
    public boolean hasAnyRole(UserRole... rolesToCheck) {
        if (roles == null || rolesToCheck == null) {
            return false;
        }
        for (UserRole role : rolesToCheck) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has access to a specific tenant.
     */
    public boolean hasAccessToTenant(String tenantId) {
        return tenantIds != null && tenantIds.contains(tenantId);
    }

    /**
     * Get user's full name.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Increment failed login attempts.
     */
    public void incrementFailedLoginAttempts() {
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 1;
        } else {
            failedLoginAttempts++;
        }

        // Lock account after 5 failed attempts for 15 minutes
        if (failedLoginAttempts >= 5) {
            accountLockedUntil = Instant.now().plusSeconds(900); // 15 minutes
        }
    }

    /**
     * Reset failed login attempts on successful login.
     */
    public void resetFailedLoginAttempts() {
        failedLoginAttempts = 0;
        accountLockedUntil = null;
        lastLoginAt = Instant.now();
    }
}
