package com.healthdata.authentication.service;

import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.entity.RefreshToken;
import com.healthdata.authentication.repository.RefreshTokenRepository;
import com.healthdata.authentication.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing refresh tokens.
 *
 * Handles:
 * - Creating refresh tokens
 * - Validating refresh tokens
 * - Revoking refresh tokens
 * - Cleanup of expired tokens
 *
 * Security Features:
 * - Tokens stored in database for revocation support
 * - IP address and user agent tracking for audit
 * - Automatic expiration
 * - Transactional operations for consistency
 */
// NOTE: No @Service annotation - this bean must be explicitly configured in Gateway service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtConfig jwtConfig;

    /**
     * Create and store a refresh token for a user.
     *
     * @param user user for whom to create the token
     * @param jwtToken JWT refresh token string
     * @param request HTTP request for IP and user agent (can be null for tests)
     * @return created RefreshToken entity
     */
    @Transactional
    public RefreshToken createRefreshToken(User user, String jwtToken, HttpServletRequest request) {
        log.debug("Creating refresh token for user: {}", user.getUsername());

        Instant expiresAt = Instant.now().plusMillis(jwtConfig.getRefreshTokenExpirationMillis());

        RefreshToken refreshToken = RefreshToken.builder()
            .token(jwtToken)
            .user(user)
            .expiresAt(expiresAt)
            .ipAddress(request != null ? getClientIpAddress(request) : null)
            .userAgent(request != null ? request.getHeader("User-Agent") : null)
            .build();

        refreshToken = refreshTokenRepository.save(refreshToken);

        log.info("Refresh token created for user: {}, expires at: {}", user.getUsername(), expiresAt);
        return refreshToken;
    }

    /**
     * Validate a refresh token.
     * Checks JWT validity and database record.
     *
     * @param tokenValue refresh token value
     * @return Optional containing the refresh token if valid
     */
    @Transactional(readOnly = true)
    public Optional<RefreshToken> validateRefreshToken(String tokenValue) {
        try {
            // First validate the JWT itself
            if (!jwtTokenService.validateToken(tokenValue)) {
                log.debug("Refresh token JWT validation failed");
                return Optional.empty();
            }

            // Then check database record
            Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenAndRevokedAtIsNull(tokenValue);

            if (tokenOpt.isEmpty()) {
                log.debug("Refresh token not found in database or was revoked");
                return Optional.empty();
            }

            RefreshToken token = tokenOpt.get();

            // Check if expired
            if (token.isExpired()) {
                log.debug("Refresh token is expired");
                return Optional.empty();
            }

            log.debug("Refresh token validated successfully");
            return Optional.of(token);

        } catch (JwtException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get user from refresh token.
     *
     * @param tokenValue refresh token value
     * @return Optional containing the user if token is valid
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserFromRefreshToken(String tokenValue) {
        return validateRefreshToken(tokenValue)
            .map(RefreshToken::getUser);
    }

    /**
     * Revoke a refresh token.
     *
     * @param tokenValue token to revoke
     * @return true if token was revoked, false if not found
     */
    @Transactional
    public boolean revokeRefreshToken(String tokenValue) {
        log.debug("Revoking refresh token");

        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            log.debug("Refresh token not found");
            return false;
        }

        RefreshToken token = tokenOpt.get();
        token.revoke();
        refreshTokenRepository.save(token);

        log.info("Refresh token revoked for user: {}", token.getUser().getUsername());
        return true;
    }

    /**
     * Revoke all refresh tokens for a user.
     * Used for logout from all devices.
     *
     * @param userId user ID
     * @return number of tokens revoked
     */
    @Transactional
    public int revokeAllUserTokens(UUID userId) {
        log.info("Revoking all refresh tokens for user: {}", userId);

        int count = refreshTokenRepository.revokeAllUserTokens(userId, Instant.now());

        log.info("Revoked {} refresh tokens for user: {}", count, userId);
        return count;
    }

    /**
     * Get all active refresh tokens for a user.
     *
     * @param userId user ID
     * @return list of active tokens
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> getActiveTokensForUser(UUID userId) {
        return refreshTokenRepository.findActiveTokensByUserId(userId, Instant.now());
    }

    /**
     * Delete expired refresh tokens.
     * Should be called by scheduled cleanup task.
     *
     * @return number of tokens deleted
     */
    @Transactional
    public int deleteExpiredTokens() {
        log.debug("Deleting expired refresh tokens");

        int count = refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());

        if (count > 0) {
            log.info("Deleted {} expired refresh tokens", count);
        }

        return count;
    }

    /**
     * Delete revoked tokens older than specified days.
     *
     * @param days number of days
     * @return number of tokens deleted
     */
    @Transactional
    public int deleteOldRevokedTokens(int days) {
        log.debug("Deleting revoked tokens older than {} days", days);

        Instant cutoffDate = Instant.now().minusSeconds(days * 86400L);
        int count = refreshTokenRepository.deleteRevokedTokensBefore(cutoffDate);

        if (count > 0) {
            log.info("Deleted {} old revoked tokens", count);
        }

        return count;
    }

    /**
     * Count active tokens for a user.
     *
     * @param userId user ID
     * @return number of active tokens
     */
    @Transactional(readOnly = true)
    public long countActiveTokens(UUID userId) {
        return refreshTokenRepository.countActiveTokensByUserId(userId, Instant.now());
    }

    /**
     * Extract client IP address from request.
     * Handles proxy headers (X-Forwarded-For, X-Real-IP).
     *
     * @param request HTTP request
     * @return client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For can contain multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
