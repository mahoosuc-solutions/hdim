package com.healthdata.gateway.service;

import com.healthdata.authentication.entity.RefreshToken;

/**
 * Token Revocation Service Interface (Phase 2.0 Team 3.2)
 *
 * Handles token revocation and Redis blacklist management.
 * Provides contract for token revocation operations used by authentication
 * and token validation filters.
 */
public interface TokenRevocationService {

    /**
     * Revoke a single refresh token
     *
     * @param token RefreshToken to revoke
     * @param reason Revocation reason
     */
    void revokeRefreshToken(RefreshToken token, String reason);

    /**
     * Revoke all active tokens for a user (logout)
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param reason Revocation reason
     * @return Number of tokens revoked
     */
    int revokeAllUserTokens(String userId, String tenantId, String reason);

    /**
     * Revoke a specific access token
     *
     * @param accessToken JWT access token
     * @param tenantId Tenant ID
     * @param reason Revocation reason
     */
    void revokeAccessToken(String accessToken, String tenantId, String reason);

    /**
     * Check if token is in blacklist
     *
     * @param jti JWT ID
     * @return true if token is blacklisted
     */
    boolean isBlacklisted(String jti);
}
