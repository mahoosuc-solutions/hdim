package com.healthdata.gateway.service;

import com.healthdata.authentication.entity.RefreshToken;
import com.healthdata.authentication.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token Revocation Service (Phase 2.0 Team 3.2)
 *
 * Handles token revocation and Redis blacklist management
 * Supports both individual token revocation and bulk logout
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TokenRevocationServiceImpl implements TokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditLogService auditLogService;

    private static final String BLACKLIST_KEY_PREFIX = "token_blacklist:";
    private static final String REVOCATION_REASON_PREFIX = "token_revocation:";

    /**
     * Revocation reasons supported
     */
    private static final java.util.Set<String> VALID_REASONS = java.util.Set.of(
        "LOGOUT", "TOKEN_REFRESH", "COMPROMISE", "ADMIN_REVOKE", "INACTIVITY", "PASSWORD_CHANGE"
    );

    /**
     * Revoke a single refresh token
     *
     * @param token RefreshToken to revoke
     * @param reason Revocation reason
     * @throws IllegalArgumentException if reason is invalid
     */
    @Override
    public void revokeRefreshToken(RefreshToken token, String reason) {
        validateRevocationReason(reason);

        try {
            if (token == null) {
                throw new IllegalArgumentException("RefreshToken cannot be null");
            }

            // Revoke the token
            token.revoke();
            refreshTokenRepository.save(token);

            // Add to Redis blacklist
            String jti = token.getToken() != null ? token.getToken().substring(0, Math.min(100, token.getToken().length())) : "unknown";
            String userId = token.getUser() != null && token.getUser().getId() != null ? token.getUser().getId().toString() : "system";

            addToBlacklist(jti, token.getExpiresAt(), userId, "default");

            // Audit log
            auditLogService.logTokenRevocation(userId, "default", "TOKEN:" + jti, reason);

            log.info("Token revoked: {} (reason: {})", jti, reason);

        } catch (Exception e) {
            log.error("Failed to revoke token", e);
            throw e;
        }
    }

    /**
     * Revoke all active tokens for a user (logout)
     *
     * @param userId User ID (as string, will be converted to UUID)
     * @param tenantId Tenant ID
     * @param reason Revocation reason
     * @return Number of tokens revoked
     */
    @Override
    public int revokeAllUserTokens(String userId, String tenantId, String reason) {
        validateRevocationReason(reason);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("User ID is required");
            }

            // Convert user ID string to UUID for repository query
            UUID userUUID;
            try {
                userUUID = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid user ID format: {}", userId);
                return 0;  // No tokens revoked if invalid UUID
            }

            // Revoke all active tokens for this user
            // The repository method handles finding and revoking in one operation
            int revokedCount = refreshTokenRepository.revokeAllUserTokens(userUUID, Instant.now());

            // Audit log
            auditLogService.logTokenRevocation(userId, tenantId, "USER:" + userId, reason);

            log.info("Revoked {} tokens for user {} (tenant: {})", revokedCount, userId, tenantId);

            return revokedCount;

        } catch (Exception e) {
            log.error("Failed to revoke all tokens for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * Revoke a specific access token
     *
     * @param accessToken JWT access token
     * @param tenantId Tenant ID
     * @param reason Revocation reason
     */
    @Override
    public void revokeAccessToken(String accessToken, String tenantId, String reason) {
        validateRevocationReason(reason);

        try {
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new IllegalArgumentException("Access token is required");
            }

            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new IllegalArgumentException("Tenant ID is required");
            }

            // For MVP: treat accessToken as JTI identifier
            // Production: extend to parse JWT tokens and extract JTI
            String jti = accessToken.substring(0, Math.min(100, accessToken.length()));

            // Calculate default TTL (1 hour)
            Instant expiresAt = Instant.now().plusSeconds(3600);

            // Add to blacklist
            addToBlacklist(jti, expiresAt, "system", tenantId);

            // Audit log
            auditLogService.logTokenRevocation("system", tenantId, "JTI:" + jti, reason);

            log.info("Access token revoked: {} (reason: {})", jti, reason);

        } catch (Exception e) {
            log.error("Failed to revoke access token", e);
            throw e;
        }
    }

    /**
     * Add token to Redis blacklist
     *
     * @param jti JWT ID (unique identifier)
     * @param expiresAt Token expiration time
     * @param userId User ID
     * @param tenantId Tenant ID
     */
    private void addToBlacklist(String jti, Instant expiresAt, String userId, String tenantId) {
        try {
            String blacklistKey = BLACKLIST_KEY_PREFIX + jti;

            // Create blacklist entry
            String blacklistValue = String.format(
                "{\"userId\":\"%s\",\"tenantId\":\"%s\",\"revokedAt\":\"%s\"}",
                userId, tenantId, Instant.now()
            );

            // Calculate TTL (time remaining until token expires)
            Duration ttl = Duration.between(Instant.now(), expiresAt);
            long ttlSeconds = ttl.getSeconds();

            // Store in Redis with TTL
            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set(
                    blacklistKey,
                    blacklistValue,
                    ttlSeconds,
                    TimeUnit.SECONDS
                );
                log.debug("Added to blacklist: {} (TTL: {} seconds)", jti, ttlSeconds);
            } else {
                // Token already expired, don't add to blacklist
                log.debug("Token already expired, skipping blacklist: {}", jti);
            }

        } catch (Exception e) {
            log.error("Failed to add token to blacklist: {}", jti, e);
            // Don't rethrow - blacklist is for performance optimization
            // Missing from blacklist is not a critical failure
        }
    }

    /**
     * Check if token is in blacklist
     *
     * @param jti JWT ID
     * @return true if token is blacklisted
     */
    @Override
    public boolean isBlacklisted(String jti) {
        try {
            String blacklistKey = BLACKLIST_KEY_PREFIX + jti;
            Object blacklistEntry = redisTemplate.opsForValue().get(blacklistKey);
            return blacklistEntry != null;
        } catch (Exception e) {
            log.error("Error checking blacklist: {}", jti, e);
            return false;  // Fail-open: if Redis error, allow request
        }
    }

    /**
     * Validate revocation reason
     *
     * @param reason Revocation reason
     * @throws IllegalArgumentException if reason is invalid
     */
    private void validateRevocationReason(String reason) {
        if (!VALID_REASONS.contains(reason)) {
            throw new IllegalArgumentException("Invalid revocation reason: " + reason);
        }
    }
}
