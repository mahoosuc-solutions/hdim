package com.healthdata.gateway.service;

import com.healthdata.gateway.domain.RefreshToken;
import com.healthdata.gateway.domain.RefreshTokenRepository;
import com.healthdata.gateway.dto.TokenRefreshRequest;
import com.healthdata.gateway.dto.TokenRefreshResponse;
import com.healthdata.authentication.config.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for TokenRefreshService (Phase 2.0 Team 3.1)
 *
 * Tests cover:
 * - Refresh token validation
 * - New token generation
 * - Old token revocation
 * - Error handling
 * - Multi-tenant isolation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRefreshService Tests")
class TokenRefreshServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TokenRefreshService tokenRefreshService;

    private static final String TENANT_ID = "tenant-001";
    private static final String USER_ID = "user-123";
    private static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    private static final String TOKEN_JTI = "jti-unique-123";
    private static final String TOKEN_HASH = "hash-abc123def456";

    private RefreshToken validRefreshToken;

    @BeforeEach
    void setup() {
        validRefreshToken = RefreshToken.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .tokenJti(TOKEN_JTI)
            .tokenHash(TOKEN_HASH)
            .expiresAt(Instant.now().plusSeconds(3600))
            .revocationReason(null)
            .revokedAt(null)
            .build();
    }

    @Test
    @DisplayName("Should successfully refresh valid token")
    void testSuccessfulTokenRefresh() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);

        // Setup mock behaviors
        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn(TENANT_ID);
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.of(validRefreshToken));
        when(jwtTokenProvider.generateAccessToken(USER_ID, TENANT_ID))
            .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(USER_ID, TENANT_ID))
            .thenReturn("new-refresh-token");

        // Execute
        TokenRefreshResponse response = tokenRefreshService.refreshToken(request, TENANT_ID);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900);
    }

    @Test
    @DisplayName("Should revoke old refresh token after use")
    void testRevokesOldTokenAfterRefresh() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn(TENANT_ID);
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.of(validRefreshToken));
        when(jwtTokenProvider.generateAccessToken(USER_ID, TENANT_ID))
            .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(USER_ID, TENANT_ID))
            .thenReturn("new-refresh-token");

        tokenRefreshService.refreshToken(request, TENANT_ID);

        // Verify old token was revoked
        verify(tokenRevocationService).revokeRefreshToken(
            argThat(token -> token.getTokenJti().equals(TOKEN_JTI)),
            eq("TOKEN_REFRESH")
        );
    }

    @Test
    @DisplayName("Should reject invalid refresh token signature")
    void testRejectsInvalidSignature() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(false);

        assertThatThrownBy(() -> tokenRefreshService.refreshToken(request, TENANT_ID))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Should reject expired refresh token")
    void testRejectsExpiredToken() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);
        RefreshToken expiredToken = validRefreshToken.toBuilder()
            .expiresAt(Instant.now().minusSeconds(100))
            .build();

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn(TENANT_ID);
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> tokenRefreshService.refreshToken(request, TENANT_ID))
            .isInstanceOf(ExpiredTokenException.class);
    }

    @Test
    @DisplayName("Should reject revoked refresh token")
    void testRejectsRevokedToken() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);
        RefreshToken revokedToken = validRefreshToken.toBuilder()
            .revokedAt(Instant.now().minusSeconds(60))
            .revocationReason("LOGOUT")
            .build();

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn(TENANT_ID);
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> tokenRefreshService.refreshToken(request, TENANT_ID))
            .isInstanceOf(RevokedTokenException.class);
    }

    @Test
    @DisplayName("Should reject token not found in database")
    void testRejectsTokenNotInDatabase() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn(TENANT_ID);
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenRefreshService.refreshToken(request, TENANT_ID))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation")
    void testEnforcesTenantIsolation() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn("tenant-other");  // Different tenant
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.of(validRefreshToken));

        assertThatThrownBy(() -> tokenRefreshService.refreshToken(request, "tenant-different"))
            .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    @DisplayName("Should generate new tokens with correct claims")
    void testGeneratesNewTokensWithCorrectClaims() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn(TENANT_ID);
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.of(validRefreshToken));
        when(jwtTokenProvider.generateAccessToken(USER_ID, TENANT_ID))
            .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(USER_ID, TENANT_ID))
            .thenReturn("new-refresh-token");

        tokenRefreshService.refreshToken(request, TENANT_ID);

        // Verify correct parameters were used
        verify(jwtTokenProvider).generateAccessToken(USER_ID, TENANT_ID);
        verify(jwtTokenProvider).generateRefreshToken(USER_ID, TENANT_ID);
    }

    @Test
    @DisplayName("Should update last_used_at timestamp")
    void testUpdatesLastUsedTimestamp() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn(TENANT_ID);
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.of(validRefreshToken));
        when(jwtTokenProvider.generateAccessToken(USER_ID, TENANT_ID))
            .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(USER_ID, TENANT_ID))
            .thenReturn("new-refresh-token");

        tokenRefreshService.refreshToken(request, TENANT_ID);

        // Verify last_used_at was updated
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should audit token refresh operation")
    void testAuditTokenRefresh() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn(TENANT_ID);
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.of(validRefreshToken));
        when(jwtTokenProvider.generateAccessToken(USER_ID, TENANT_ID))
            .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(USER_ID, TENANT_ID))
            .thenReturn("new-refresh-token");

        tokenRefreshService.refreshToken(request, TENANT_ID);

        // Verify audit was logged
        verify(auditLogService).logTokenRefresh(USER_ID, TENANT_ID, "SUCCESS");
    }

    @Test
    @DisplayName("Should handle missing JTI claim in token")
    void testHandlesMissingJtiClaim() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(null);

        assertThatThrownBy(() -> tokenRefreshService.refreshToken(request, TENANT_ID))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Should return correct token response structure")
    void testTokenResponseStructure() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn(TENANT_ID);
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.of(validRefreshToken));
        when(jwtTokenProvider.generateAccessToken(USER_ID, TENANT_ID))
            .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(USER_ID, TENANT_ID))
            .thenReturn("new-refresh-token");

        TokenRefreshResponse response = tokenRefreshService.refreshToken(request, TENANT_ID);

        // Verify response structure
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900);  // 15 minutes
    }

    @Test
    @DisplayName("Should prevent token reuse after revocation")
    void testPreventsTokenReuseAfterRevocation() {
        TokenRefreshRequest request = new TokenRefreshRequest(REFRESH_TOKEN);
        RefreshToken alreadyRevoked = validRefreshToken.toBuilder()
            .revokedAt(Instant.now())
            .revocationReason("ALREADY_USED")
            .build();

        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN))
            .thenReturn(true);
        when(jwtTokenProvider.getJtiFromToken(REFRESH_TOKEN))
            .thenReturn(TOKEN_JTI);
        when(jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN))
            .thenReturn(USER_ID);
        when(jwtTokenProvider.getTenantIdFromToken(REFRESH_TOKEN))
            .thenReturn(TENANT_ID);
        when(refreshTokenRepository.findByTokenJti(TOKEN_JTI))
            .thenReturn(Optional.of(alreadyRevoked));

        assertThatThrownBy(() -> tokenRefreshService.refreshToken(request, TENANT_ID))
            .isInstanceOf(RevokedTokenException.class);
    }
}
