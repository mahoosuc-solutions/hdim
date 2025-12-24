package com.healthdata.authentication.service;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.dto.MfaSetupResponse;
import com.healthdata.authentication.dto.MfaVerifyRequest;
import com.healthdata.authentication.repository.UserRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Service for Multi-Factor Authentication (MFA) using TOTP.
 *
 * Provides:
 * - MFA setup with QR code generation
 * - TOTP code verification
 * - Recovery code generation and validation
 * - MFA enable/disable operations
 *
 * Security Features:
 * - HMAC-SHA1 algorithm (compatible with Google Authenticator, Authy, etc.)
 * - 6-digit codes with 30-second validity
 * - 8 recovery codes for account recovery
 * - Secure secret generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private static final int SECRET_LENGTH = 32;
    private static final int RECOVERY_CODE_COUNT = 8;
    private static final int RECOVERY_CODE_LENGTH = 8;
    private static final String ISSUER = "HDIM";

    private final UserRepository userRepository;

    @Value("${mfa.issuer:HDIM}")
    private String mfaIssuer;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator(SECRET_LENGTH);
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Initialize MFA setup for a user.
     * Generates a new TOTP secret and returns QR code for authenticator app.
     *
     * @param userId User ID
     * @return MFA setup response with QR code and secret
     */
    @Transactional
    public MfaSetupResponse initializeMfaSetup(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Generate new secret
        String secret = secretGenerator.generate();

        // Store secret temporarily (not enabled yet)
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        userRepository.save(user);

        log.info("MFA setup initialized for user: {}", user.getUsername());

        // Generate QR code
        String qrCodeDataUri = generateQrCode(user.getUsername(), secret);

        return MfaSetupResponse.builder()
            .secret(secret)
            .qrCodeDataUri(qrCodeDataUri)
            .issuer(mfaIssuer)
            .username(user.getUsername())
            .build();
    }

    /**
     * Complete MFA setup by verifying TOTP code.
     * User must provide a valid code from their authenticator app to enable MFA.
     *
     * @param userId User ID
     * @param code TOTP code from authenticator
     * @return Recovery codes for account recovery
     */
    @Transactional
    public List<String> completeMfaSetup(UUID userId, String code) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getMfaSecret() == null) {
            throw new IllegalStateException("MFA setup not initialized. Call initializeMfaSetup first.");
        }

        // Verify the code
        if (!verifyCode(user.getMfaSecret(), code)) {
            log.warn("Invalid MFA code during setup for user: {}", user.getUsername());
            throw new IllegalArgumentException("Invalid verification code");
        }

        // Generate recovery codes
        List<String> recoveryCodes = generateRecoveryCodes();
        user.setMfaRecoveryCodes(String.join(",", recoveryCodes));
        user.setMfaEnabled(true);
        user.setMfaEnabledAt(Instant.now());
        userRepository.save(user);

        log.info("MFA enabled successfully for user: {}", user.getUsername());

        return recoveryCodes;
    }

    /**
     * Verify TOTP code for authentication.
     *
     * @param user User to verify
     * @param code TOTP code from authenticator
     * @return true if code is valid
     */
    public boolean verifyMfaCode(User user, String code) {
        if (!user.isMfaConfigured()) {
            log.warn("MFA not configured for user: {}", user.getUsername());
            return false;
        }

        boolean valid = verifyCode(user.getMfaSecret(), code);

        if (valid) {
            log.debug("MFA verification successful for user: {}", user.getUsername());
        } else {
            log.warn("MFA verification failed for user: {}", user.getUsername());
        }

        return valid;
    }

    /**
     * Verify recovery code for authentication.
     * Recovery codes are single-use and invalidated after use.
     *
     * @param userId User ID
     * @param recoveryCode Recovery code
     * @return true if recovery code is valid
     */
    @Transactional
    public boolean verifyRecoveryCode(UUID userId, String recoveryCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getMfaRecoveryCodes() == null || user.getMfaRecoveryCodes().isBlank()) {
            return false;
        }

        List<String> codes = new ArrayList<>(List.of(user.getMfaRecoveryCodes().split(",")));
        String normalizedCode = recoveryCode.toUpperCase().replace("-", "");

        if (codes.contains(normalizedCode)) {
            // Remove used recovery code
            codes.remove(normalizedCode);
            user.setMfaRecoveryCodes(String.join(",", codes));
            userRepository.save(user);

            log.info("Recovery code used for user: {}. {} codes remaining.",
                user.getUsername(), codes.size());

            return true;
        }

        log.warn("Invalid recovery code attempt for user: {}", user.getUsername());
        return false;
    }

    /**
     * Disable MFA for a user.
     * Requires valid TOTP code for security.
     *
     * @param userId User ID
     * @param code TOTP code to verify ownership
     */
    @Transactional
    public void disableMfa(UUID userId, String code) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!user.isMfaConfigured()) {
            throw new IllegalStateException("MFA is not enabled for this user");
        }

        // Verify code before disabling
        if (!verifyCode(user.getMfaSecret(), code)) {
            log.warn("Invalid MFA code during disable for user: {}", user.getUsername());
            throw new IllegalArgumentException("Invalid verification code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaRecoveryCodes(null);
        user.setMfaEnabledAt(null);
        userRepository.save(user);

        log.info("MFA disabled for user: {}", user.getUsername());
    }

    /**
     * Regenerate recovery codes for a user.
     * Invalidates all previous recovery codes.
     *
     * @param userId User ID
     * @param code TOTP code to verify ownership
     * @return New recovery codes
     */
    @Transactional
    public List<String> regenerateRecoveryCodes(UUID userId, String code) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!user.isMfaConfigured()) {
            throw new IllegalStateException("MFA is not enabled for this user");
        }

        // Verify code before regenerating
        if (!verifyCode(user.getMfaSecret(), code)) {
            log.warn("Invalid MFA code during recovery code regeneration for user: {}",
                user.getUsername());
            throw new IllegalArgumentException("Invalid verification code");
        }

        List<String> newCodes = generateRecoveryCodes();
        user.setMfaRecoveryCodes(String.join(",", newCodes));
        userRepository.save(user);

        log.info("Recovery codes regenerated for user: {}", user.getUsername());

        return newCodes;
    }

    /**
     * Check if user has MFA enabled.
     *
     * @param userId User ID
     * @return true if MFA is enabled
     */
    public boolean isMfaEnabled(UUID userId) {
        return userRepository.findById(userId)
            .map(User::isMfaConfigured)
            .orElse(false);
    }

    /**
     * Get remaining recovery code count.
     *
     * @param userId User ID
     * @return Number of remaining recovery codes
     */
    public int getRemainingRecoveryCodeCount(UUID userId) {
        return userRepository.findById(userId)
            .map(user -> {
                if (user.getMfaRecoveryCodes() == null || user.getMfaRecoveryCodes().isBlank()) {
                    return 0;
                }
                return user.getMfaRecoveryCodes().split(",").length;
            })
            .orElse(0);
    }

    // --- Private Helper Methods ---

    private boolean verifyCode(String secret, String code) {
        try {
            return codeVerifier.isValidCode(secret, code);
        } catch (Exception e) {
            log.error("Error verifying TOTP code", e);
            return false;
        }
    }

    private String generateQrCode(String username, String secret) {
        QrData qrData = new QrData.Builder()
            .label(username)
            .secret(secret)
            .issuer(mfaIssuer)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();

        try {
            byte[] imageData = qrGenerator.generate(qrData);
            return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            log.error("Error generating QR code", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    private List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            codes.add(generateRecoveryCode());
        }
        return codes;
    }

    private String generateRecoveryCode() {
        byte[] bytes = new byte[RECOVERY_CODE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            .substring(0, RECOVERY_CODE_LENGTH)
            .toUpperCase();
    }
}
