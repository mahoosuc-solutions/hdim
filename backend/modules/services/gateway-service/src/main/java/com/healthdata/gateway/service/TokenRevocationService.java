package com.healthdata.gateway.service;

import com.healthdata.authentication.entity.RefreshToken;

/**
 * Token Revocation Service Interface (Phase 2.0 Team 3.2)
 *
 * Defines contract for token revocation and blacklist management.
 * Implementation integrates with Redis for fast blacklist lookups.
 */
public interface TokenRevocationService {

    /**
     * Revoke a single refresh token
     *
     * @param token the refresh token to revoke
     * @param reason reason for revocation (LOGOUT, COMPROMISE, etc.)
     */
    void revokeRefreshToken(RefreshToken token, String reason);

    /**
     * Revoke all tokens for a user in a tenant
     *
     * @param userId user identifier
     * @param tenantId tenant identifier
     * @param reason reason for revocation
     * @return count of revoked tokens
     */
    int revokeAllUserTokens(String userId, String tenantId, String reason);

    /**
     * Revoke an access token
     *
     * @param accessToken the access token to revoke
     * @param tenantId tenant identifier
     * @param reason reason for revocation
     */
    void revokeAccessToken(String accessToken, String tenantId, String reason);

    /**
     * Check if a token is blacklisted
     *
     * @param jti JWT ID (or full token identifier)
     * @return true if token is blacklisted, false otherwise
     */
    boolean isBlacklisted(String jti);
}
