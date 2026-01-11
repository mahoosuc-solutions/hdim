package com.healthdata.authentication.audit;

/**
 * MFA audit event types for HIPAA compliance logging.
 *
 * <p>HIPAA §164.308(a)(1)(ii)(D) requires audit controls to record and examine
 * information system activity. All MFA operations must be logged for security
 * and compliance purposes.</p>
 */
public enum MfaAuditEvent {

    /**
     * User initiated MFA setup process.
     * Triggered when /api/v1/auth/mfa/setup is called.
     */
    MFA_SETUP_INITIATED("MFA setup initiated"),

    /**
     * User successfully enabled MFA.
     * Triggered when TOTP code is verified and mfaEnabled set to true.
     */
    MFA_ENABLED("MFA enabled successfully"),

    /**
     * MFA verification succeeded during login.
     * Triggered when valid TOTP code is provided during login.
     */
    MFA_VERIFICATION_SUCCESS("MFA verification successful"),

    /**
     * MFA verification failed during login.
     * Triggered when invalid TOTP code is provided.
     * Multiple failures may indicate attack attempt.
     */
    MFA_VERIFICATION_FAILURE("MFA verification failed"),

    /**
     * Recovery code used for login.
     * Triggered when user authenticates with recovery code instead of TOTP.
     * Single-use codes are marked as consumed.
     */
    MFA_RECOVERY_CODE_USED("Recovery code used for login"),

    /**
     * User disabled MFA.
     * Triggered when MFA is turned off (admin users cannot disable).
     */
    MFA_DISABLED("MFA disabled"),

    /**
     * User regenerated recovery codes.
     * Triggered when new recovery codes are generated, invalidating old ones.
     */
    MFA_RECOVERY_CODES_REGENERATED("Recovery codes regenerated"),

    /**
     * Login blocked due to missing MFA after grace period.
     * Triggered when admin user attempts login without MFA after 7-day grace period.
     * HIPAA compliance enforcement event.
     */
    MFA_LOGIN_BLOCKED("Login blocked due to missing MFA");

    private final String description;

    MfaAuditEvent(String description) {
        this.description = description;
    }

    /**
     * Get human-readable description of the audit event.
     *
     * @return event description
     */
    public String getDescription() {
        return description;
    }
}
