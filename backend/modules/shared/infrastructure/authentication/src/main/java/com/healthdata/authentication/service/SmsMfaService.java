package com.healthdata.authentication.service;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for SMS-based Multi-Factor Authentication (MFA).
 *
 * Provides:
 * - SMS MFA setup with phone number verification
 * - SMS code generation and sending via Twilio
 * - SMS code verification
 * - Rate limiting (max 5 codes per hour)
 * - Code expiration (5-minute TTL)
 *
 * Security Features:
 * - 6-digit codes with 5-minute expiration
 * - BCrypt hashing for stored codes
 * - Rate limiting to prevent SMS abuse
 * - Twilio API integration for SMS delivery
 */
@Service
@ConditionalOnProperty(
    prefix = "authentication.controller",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Slf4j
@RequiredArgsConstructor
public class SmsMfaService {

    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_SECONDS = 300; // 5 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TwilioSmsClient twilioSmsClient;

    @Value("${mfa.sms.enabled:true}")
    private boolean smsEnabled;

    /**
     * Enable SMS MFA for a user with phone number verification.
     *
     * @param user User to enable SMS MFA for
     * @param phoneNumber Phone number in E.164 format (+15555551234)
     * @return Verification code sent to user's phone
     * @throws IllegalStateException if rate limit exceeded or SMS disabled
     */
    @Transactional
    public String enableSmsMfa(User user, String phoneNumber) {
        if (!smsEnabled) {
            throw new IllegalStateException("SMS MFA is disabled");
        }

        if (user.isSmsRateLimitExceeded()) {
            throw new IllegalStateException("SMS rate limit exceeded. Try again in 1 hour.");
        }

        // Validate phone number format (E.164)
        if (!phoneNumber.matches("^\\+[1-9]\\d{1,14}$")) {
            throw new IllegalArgumentException("Invalid phone number format. Use E.164 format (+15555551234)");
        }

        // Generate and send verification code
        String code = generateVerificationCode();
        String hashedCode = passwordEncoder.encode(code);

        user.setMfaPhoneNumber(phoneNumber);
        user.setSmsCode(hashedCode);
        user.setSmsCodeExpiry(Instant.now().plusSeconds(CODE_EXPIRY_SECONDS));
        user.incrementSmsCodeSentCount();

        userRepository.save(user);

        // Send SMS
        twilioSmsClient.sendSms(phoneNumber, formatSmsMessage(code));

        log.info("SMS MFA setup initiated for user: {}, phone: {}", user.getUsername(), maskPhoneNumber(phoneNumber));
        return code; // Return code for testing/logging (remove in production)
    }

    /**
     * Send SMS verification code during login.
     *
     * @param user User requesting SMS code
     * @return Verification code sent to user's phone
     * @throws IllegalStateException if rate limit exceeded, SMS not enabled, or phone not configured
     */
    @Transactional
    public String sendVerificationCode(User user) {
        if (!smsEnabled) {
            throw new IllegalStateException("SMS MFA is disabled");
        }

        if (user.getMfaPhoneNumber() == null || user.getMfaPhoneNumber().isBlank()) {
            throw new IllegalStateException("SMS MFA not configured for this user");
        }

        if (user.isSmsRateLimitExceeded()) {
            throw new IllegalStateException("SMS rate limit exceeded. Try again in 1 hour.");
        }

        // Generate and send verification code
        String code = generateVerificationCode();
        String hashedCode = passwordEncoder.encode(code);

        user.setSmsCode(hashedCode);
        user.setSmsCodeExpiry(Instant.now().plusSeconds(CODE_EXPIRY_SECONDS));
        user.incrementSmsCodeSentCount();

        userRepository.save(user);

        // Send SMS
        twilioSmsClient.sendSms(user.getMfaPhoneNumber(), formatSmsMessage(code));

        log.info("SMS verification code sent to user: {}", user.getUsername());
        return code; // Return code for testing/logging (remove in production)
    }

    /**
     * Verify SMS code for authentication.
     *
     * @param user User to verify code for
     * @param code Code entered by user
     * @return true if code is valid and not expired, false otherwise
     */
    @Transactional
    public boolean verifySmsCode(User user, String code) {
        if (user.getSmsCode() == null || user.getSmsCodeExpiry() == null) {
            log.warn("SMS code verification failed: no code set for user {}", user.getUsername());
            return false;
        }

        // Check expiration
        if (user.getSmsCodeExpiry().isBefore(Instant.now())) {
            log.warn("SMS code verification failed: code expired for user {}", user.getUsername());
            return false;
        }

        // Verify code
        boolean isValid = passwordEncoder.matches(code, user.getSmsCode());

        if (isValid) {
            // Clear code after successful verification
            user.setSmsCode(null);
            user.setSmsCodeExpiry(null);
            userRepository.save(user);

            log.info("SMS code verified successfully for user: {}", user.getUsername());
        } else {
            log.warn("SMS code verification failed: invalid code for user {}", user.getUsername());
        }

        return isValid;
    }

    /**
     * Disable SMS MFA for a user.
     *
     * @param user User to disable SMS MFA for
     */
    @Transactional
    public void disableSmsMfa(User user) {
        user.setMfaPhoneNumber(null);
        user.setSmsCode(null);
        user.setSmsCodeExpiry(null);
        user.setSmsCodeSentCount(0);
        user.setSmsCodeLastReset(null);

        // If user has only SMS MFA, disable MFA entirely
        if (user.getMfaMethod() == User.MfaMethod.SMS) {
            user.setMfaEnabled(false);
            user.setMfaMethod(null);
        } else if (user.getMfaMethod() == User.MfaMethod.BOTH) {
            // Switch to TOTP only
            user.setMfaMethod(User.MfaMethod.TOTP);
        }

        userRepository.save(user);

        log.info("SMS MFA disabled for user: {}", user.getUsername());
    }

    /**
     * Generate a 6-digit verification code.
     */
    private String generateVerificationCode() {
        int code = RANDOM.nextInt(900000) + 100000; // 100000 to 999999
        return String.valueOf(code);
    }

    /**
     * Format SMS message with verification code.
     */
    private String formatSmsMessage(String code) {
        return String.format("Your HDIM verification code is: %s. Valid for 5 minutes. Do not share this code.", code);
    }

    /**
     * Mask phone number for logging (show last 4 digits only).
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    /**
     * Check if user has SMS MFA enabled.
     */
    public boolean isSmsMfaEnabled(User user) {
        return user.getMfaPhoneNumber() != null &&
               !user.getMfaPhoneNumber().isBlank() &&
               (user.getMfaMethod() == User.MfaMethod.SMS || user.getMfaMethod() == User.MfaMethod.BOTH);
    }
}
