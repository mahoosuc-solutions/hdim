package com.healthdata.gateway.service;

import com.healthdata.gateway.domain.RefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Token Revocation Service (Phase 2.0 Team 3.2 - Stub)
 *
 * Handles token revocation and blacklisting
 * This is a stub that will be implemented in Team 3.2
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TokenRevocationService {

    /**
     * Revoke a refresh token
     *
     * @param token RefreshToken to revoke
     * @param reason Revocation reason (TOKEN_REFRESH, LOGOUT, etc.)
     */
    public void revokeRefreshToken(RefreshToken token, String reason) {
        // Stub implementation - will be completed in Team 3.2
        token.setRevokedAt(java.time.Instant.now());
        token.setRevocationReason(reason);
        log.info("Revoking refresh token: {} (reason: {})", token.getTokenJti(), reason);
    }
}
