package com.healthdata.authentication.controller;

import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.dto.*;
import com.healthdata.authentication.repository.UserRepository;
import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.authentication.service.MfaService;
import com.healthdata.authentication.service.MfaTokenService;
import com.healthdata.authentication.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Multi-Factor Authentication (MFA) operations.
 *
 * Endpoints:
 * - POST /api/v1/auth/mfa/verify - Complete login with MFA code
 * - POST /api/v1/auth/mfa/setup - Initialize MFA setup (returns QR code)
 * - POST /api/v1/auth/mfa/confirm - Confirm MFA setup with TOTP code
 * - POST /api/v1/auth/mfa/disable - Disable MFA
 * - GET /api/v1/auth/mfa/status - Check MFA status
 * - POST /api/v1/auth/mfa/recovery-codes - Regenerate recovery codes
 *
 * MFA Flow:
 * 1. User logs in with username/password
 * 2. If MFA enabled, /login returns mfaToken instead of JWT
 * 3. User calls /mfa/verify with mfaToken + TOTP code
 * 4. If valid, returns JWT tokens
 *
 * Setup Flow:
 * 1. Authenticated user calls /mfa/setup
 * 2. Returns QR code and secret
 * 3. User scans QR with authenticator app
 * 4. User calls /mfa/confirm with TOTP code
 * 5. Returns recovery codes, MFA is now enabled
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/mfa")
@Validated
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "authentication.controller",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class MfaController {

    private final UserRepository userRepository;
    private final MfaService mfaService;
    private final MfaTokenService mfaTokenService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtConfig jwtConfig;

    /**
     * Complete login by verifying MFA code.
     * Called after /login returns mfaRequired=true.
     *
     * @param request MFA verification request with mfaToken and code
     * @param httpRequest HTTP request for IP tracking
     * @return JWT tokens if verification successful
     */
    @PostMapping("/verify")
    public ResponseEntity<JwtAuthenticationResponse> verifyMfa(
        @Valid @RequestBody MfaLoginRequest request,
        HttpServletRequest httpRequest
    ) {
        log.info("MFA verification attempt");

        // Validate MFA token
        UUID userId = mfaTokenService.validateMfaToken(request.getMfaToken());
        if (userId == null) {
            log.warn("Invalid or expired MFA token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired MFA token");
        }

        // Load user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Verify code
        boolean valid;
        if (request.isUseRecoveryCode()) {
            valid = mfaService.verifyRecoveryCode(userId, request.getCode());
        } else {
            valid = mfaService.verifyMfaCode(user, request.getCode());
        }

        if (!valid) {
            log.warn("MFA verification failed for user: {}", user.getUsername());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        log.info("MFA verification successful for user: {}", user.getUsername());

        // Generate JWT tokens
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        // Store refresh token
        refreshTokenService.createRefreshToken(user, refreshToken, httpRequest);

        // Build response
        JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtConfig.getAccessTokenExpirationSeconds())
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(user.getRoles())
            .tenantIds(user.getTenantIds())
            .mfaEnabled(true)
            .message("MFA verification successful")
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Initialize MFA setup for the current user.
     * Returns QR code for authenticator app.
     *
     * @param authentication Current authentication
     * @return MFA setup response with QR code
     */
    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> initializeMfaSetup(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        if (user.isMfaConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "MFA is already enabled. Disable it first to reconfigure.");
        }

        log.info("MFA setup initiated for user: {}", user.getUsername());
        MfaSetupResponse response = mfaService.initializeMfaSetup(user.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Confirm MFA setup by verifying TOTP code.
     * Enables MFA and returns recovery codes.
     *
     * @param request MFA verification request
     * @param authentication Current authentication
     * @return Recovery codes
     */
    @PostMapping("/confirm")
    public ResponseEntity<MfaRecoveryCodesResponse> confirmMfaSetup(
        @Valid @RequestBody MfaVerifyRequest request,
        Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        try {
            List<String> recoveryCodes = mfaService.completeMfaSetup(user.getId(), request.getCode());

            log.info("MFA enabled for user: {}", user.getUsername());

            MfaRecoveryCodesResponse response = MfaRecoveryCodesResponse.builder()
                .recoveryCodes(recoveryCodes)
                .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Disable MFA for the current user.
     * Requires valid TOTP code for security.
     *
     * @param request MFA verification request
     * @param authentication Current authentication
     * @return Empty response with 200 OK
     */
    @PostMapping("/disable")
    public ResponseEntity<Void> disableMfa(
        @Valid @RequestBody MfaVerifyRequest request,
        Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        try {
            mfaService.disableMfa(user.getId(), request.getCode());
            log.info("MFA disabled for user: {}", user.getUsername());
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Get MFA status for the current user.
     *
     * @param authentication Current authentication
     * @return MFA status
     */
    @GetMapping("/status")
    public ResponseEntity<MfaStatusResponse> getMfaStatus(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        MfaStatusResponse response = MfaStatusResponse.builder()
            .mfaEnabled(user.isMfaConfigured())
            .enabledAt(user.getMfaEnabledAt())
            .remainingRecoveryCodes(mfaService.getRemainingRecoveryCodeCount(user.getId()))
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Regenerate recovery codes.
     * Invalidates all previous recovery codes.
     *
     * @param request MFA verification request
     * @param authentication Current authentication
     * @return New recovery codes
     */
    @PostMapping("/recovery-codes")
    public ResponseEntity<MfaRecoveryCodesResponse> regenerateRecoveryCodes(
        @Valid @RequestBody MfaVerifyRequest request,
        Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        try {
            List<String> newCodes = mfaService.regenerateRecoveryCodes(user.getId(), request.getCode());

            log.info("Recovery codes regenerated for user: {}", user.getUsername());

            MfaRecoveryCodesResponse response = MfaRecoveryCodesResponse.builder()
                .recoveryCodes(newCodes)
                .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // --- Helper Methods ---

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
