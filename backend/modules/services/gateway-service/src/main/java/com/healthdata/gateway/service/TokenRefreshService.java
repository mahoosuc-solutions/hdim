package com.healthdata.gateway.service;

import com.healthdata.authentication.entity.RefreshToken;
import com.healthdata.authentication.repository.RefreshTokenRepository;
import com.healthdata.gateway.dto.TokenRefreshRequest;
import com.healthdata.gateway.dto.TokenRefreshResponse;
import com.healthdata.gateway.exception.*;
import com.healthdata.authentication.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Token Refresh Service (Phase 2.0 Team 3.1)
 *
 * Handles refresh token validation and issuance of new access/refresh tokens
 * Implements sliding window session extension for better UX
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TokenRefreshService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenRevocationService tokenRevocationService;
    private final AuditLogService auditLogService;

    /**
     * Refresh access token using a valid refresh token
     *
     * Process:
     * 1. Validate refresh token signature
     * 2. Extract JTI, user ID, tenant ID from token
     * 3. Check token exists in database
     * 4. Check token is not expired
     * 5. Check token is not revoked
     * 6. Verify tenant isolation
     * 7. Revoke old token
     * 8. Generate new access and refresh tokens
     * 9. Update last_used_at timestamp
     * 10. Audit log the refresh operation
     *
     * @param request TokenRefreshRequest containing refresh token
     * @param tenantId Tenant ID from X-Tenant-ID header
     * @return TokenRefreshResponse with new tokens
     * @throws InvalidTokenException if token signature invalid
     * @throws ExpiredTokenException if token expired
     * @throws RevokedTokenException if token revoked
     * @throws TenantAccessDeniedException if tenant mismatch
     */
    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request, String tenantId) {
        String refreshToken = request.getRefreshToken();

        // Step 1: Validate token signature
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Invalid refresh token signature");
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        // Step 2: Extract claims from token
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String tokenTenantId = jwtTokenProvider.getTenantIdFromToken(refreshToken);

        // Step 3: Lookup token in database by token value
        Optional<RefreshToken> storedToken = refreshTokenRepository.findByToken(refreshToken);
        if (storedToken.isEmpty()) {
            log.warn("Refresh token not found in database");
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        RefreshToken token = storedToken.get();

        // Step 4: Check token not expired
        if (token.isExpired()) {
            log.warn("Refresh token expired for user: {}", userId);
            throw new ExpiredTokenException("Refresh token has expired");
        }

        // Step 5: Check token not revoked
        if (token.isRevoked()) {
            log.warn("Refresh token revoked for user: {}", userId);
            throw new RevokedTokenException("Refresh token has been revoked");
        }

        // Step 6: Verify tenant isolation (check tenant from JWT matches request)
        if (!tenantId.equals(tokenTenantId)) {
            log.warn("Tenant mismatch for refresh token: expected {}, got {}", tenantId, tokenTenantId);
            throw new TenantAccessDeniedException("Token belongs to different tenant");
        }

        try {
            // Step 7: Revoke old token to prevent reuse
            tokenRevocationService.revokeRefreshToken(token, "TOKEN_REFRESH");

            // Step 8: Generate new tokens
            String newAccessToken = jwtTokenProvider.generateAccessToken(userId, tenantId);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, tenantId);

            // Step 9: Audit log the refresh
            auditLogService.logTokenRefresh(userId, tenantId, "SUCCESS");

            log.info("Token refreshed successfully for user: {} tenant: {}", userId, tenantId);

            return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900)  // 15 minutes
                .build();

        } catch (Exception e) {
            auditLogService.logTokenRefresh(userId, tenantId, "FAILURE");
            log.error("Token refresh failed: {}", e.getMessage());
            throw e;
        }
    }
}
