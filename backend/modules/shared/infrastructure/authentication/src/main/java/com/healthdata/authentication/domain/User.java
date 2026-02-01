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
    private UUID id;  // ID provided by authentication service/gateway, not auto-generated

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

    /**
     * OAuth2 provider name (e.g., "okta", "azure", "auth0").
     * Null for users using local authentication.
     */
    @Column(length = 50)
    private String oauthProvider;

    /**
     * OAuth2 provider's unique user ID.
     * Used to link local user to external identity.
     */
    @Column(length = 255)
    private String oauthProviderId;

    /**
     * MFA enabled flag.
     * When true, users must provide TOTP code after password authentication.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean mfaEnabled = false;

    /**
     * Encrypted TOTP secret key for MFA.
     * Generated when MFA is first enabled.
     */
    @JsonIgnore
    @Column(length = 255)
    private String mfaSecret;

    /**
     * MFA recovery codes (comma-separated, encrypted).
     * Used when user loses access to authenticator app.
     */
    @JsonIgnore
    @Column(length = 1000)
    private String mfaRecoveryCodes;

    /**
     * Timestamp when MFA was enabled.
     */
    @Column
    private Instant mfaEnabledAt;

    /**
     * MFA method type (TOTP, SMS, or BOTH).
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private MfaMethod mfaMethod = MfaMethod.TOTP;

    /**
     * Phone number for SMS MFA (E.164 format: +15555551234).
     */
    @Column(length = 20)
    private String mfaPhoneNumber;

    /**
     * Hashed SMS verification code (6-digit).
     */
    @JsonIgnore
    @Column(length = 255)
    private String smsCode;

    /**
     * SMS code expiration timestamp (5-minute TTL).
     */
    @Column
    private Instant smsCodeExpiry;

    /**
     * SMS code send count (for rate limiting).
     */
    @Column
    @Builder.Default
    private Integer smsCodeSentCount = 0;

    /**
     * Last reset timestamp for hourly rate limit counter.
     */
    @Column
    private Instant smsCodeLastReset;

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
     * Check if MFA is enabled and configured for this user.
     */
    public boolean isMfaConfigured() {
        return Boolean.TRUE.equals(mfaEnabled) && mfaSecret != null && !mfaSecret.isBlank();
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

    /**
     * Check if SMS code rate limit has been exceeded.
     */
    public boolean isSmsRateLimitExceeded() {
        if (smsCodeLastReset == null) {
            return false;
        }

        // Reset counter if more than 1 hour has passed
        if (smsCodeLastReset.isBefore(Instant.now().minusSeconds(3600))) {
            return false;
        }

        // Max 5 SMS codes per hour
        return smsCodeSentCount != null && smsCodeSentCount >= 5;
    }

    /**
     * Increment SMS code send count for rate limiting.
     */
    public void incrementSmsCodeSentCount() {
        Instant now = Instant.now();

        // Reset counter if more than 1 hour has passed
        if (smsCodeLastReset == null || smsCodeLastReset.isBefore(now.minusSeconds(3600))) {
            smsCodeSentCount = 1;
            smsCodeLastReset = now;
        } else {
            smsCodeSentCount = (smsCodeSentCount == null ? 0 : smsCodeSentCount) + 1;
        }
    }

    /**
     * MFA method enum.
     */
    public enum MfaMethod {
        /**
         * TOTP-based MFA (Google Authenticator, Authy, etc.)
         */
        TOTP,

        /**
         * SMS-based MFA (text message verification code)
         */
        SMS,

        /**
         * Dual MFA (both TOTP and SMS required)
         */
        BOTH
    }
}
