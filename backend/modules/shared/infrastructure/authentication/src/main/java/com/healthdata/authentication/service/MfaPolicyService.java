package com.healthdata.authentication.service;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for enforcing MFA policies.
 * HIPAA §164.312(d) - Person or Entity Authentication
 *
 * <p>Enforces mandatory MFA for administrative accounts (ADMIN, SUPER_ADMIN)
 * with a grace period for new accounts to set up MFA.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MfaPolicyService {

    /**
     * Grace period for new admin accounts to set up MFA before login is blocked.
     * HIPAA compliance allows reasonable grace period for implementation.
     */
    private static final int MFA_GRACE_PERIOD_DAYS = 7;

    /**
     * Check if user's role requires MFA (ADMIN or SUPER_ADMIN).
     *
     * <p>HIPAA §164.312(d) requires strong authentication for accounts
     * that access or modify ePHI. Admin accounts have full system access
     * and must use MFA.</p>
     *
     * @param user the user to check
     * @return true if user has ADMIN or SUPER_ADMIN role
     */
    public boolean isMfaRequired(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }

        return user.getRoles().stream()
            .anyMatch(role -> role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN);
    }

    /**
     * Check if login should be blocked due to MFA not being set up.
     *
     * <p>Grace period logic:</p>
     * <ul>
     *   <li>Admin accounts created < 7 days ago: Login allowed with warning</li>
     *   <li>Admin accounts created ≥ 7 days ago without MFA: Login blocked</li>
     *   <li>Admin accounts with MFA configured: Always allowed (MFA challenge required)</li>
     *   <li>Non-admin accounts: Never blocked (MFA optional)</li>
     * </ul>
     *
     * @param user the user attempting to log in
     * @return true if login should be blocked due to missing MFA
     */
    public boolean shouldBlockLogin(User user) {
        if (user == null) {
            return false;
        }

        // Non-admin users are never blocked for MFA
        if (!isMfaRequired(user)) {
            return false;
        }

        // Admin users with MFA configured are never blocked
        if (user.isMfaConfigured()) {
            return false;
        }

        // Check if grace period has expired
        Instant gracePeriodEnd = user.getCreatedAt()
            .plus(Duration.ofDays(MFA_GRACE_PERIOD_DAYS));

        boolean isExpired = Instant.now().isAfter(gracePeriodEnd);

        if (isExpired) {
            log.warn("MFA grace period expired for admin user: {} (created: {}, expired: {})",
                user.getUsername(), user.getCreatedAt(), gracePeriodEnd);
        }

        return isExpired;
    }

    /**
     * Get remaining days in MFA setup grace period.
     *
     * @param user the user to check
     * @return number of days remaining, or 0 if grace period expired
     */
    public long getGracePeriodRemainingDays(User user) {
        if (user == null || user.getCreatedAt() == null) {
            return 0;
        }

        Instant gracePeriodEnd = user.getCreatedAt()
            .plus(Duration.ofDays(MFA_GRACE_PERIOD_DAYS));

        Duration remaining = Duration.between(Instant.now(), gracePeriodEnd);
        long daysRemaining = remaining.toDays();

        return Math.max(0, daysRemaining);
    }

    /**
     * Check if user is currently in MFA setup grace period.
     *
     * @param user the user to check
     * @return true if admin user without MFA is in grace period
     */
    public boolean isInGracePeriod(User user) {
        return isMfaRequired(user)
            && !user.isMfaConfigured()
            && !shouldBlockLogin(user);
    }

    /**
     * Get descriptive message for MFA status.
     *
     * @param user the user to check
     * @return human-readable MFA status message
     */
    public String getMfaStatusMessage(User user) {
        if (!isMfaRequired(user)) {
            return user.isMfaConfigured()
                ? "MFA enabled (optional for your role)"
                : "MFA not required for your role";
        }

        if (user.isMfaConfigured()) {
            return "MFA enabled and required for your role";
        }

        long daysRemaining = getGracePeriodRemainingDays(user);
        if (daysRemaining > 0) {
            return String.format(
                "MFA setup required within %d days. Your admin account must enable MFA for security compliance.",
                daysRemaining
            );
        } else {
            return "MFA setup overdue. Please contact your administrator for account recovery.";
        }
    }
}
