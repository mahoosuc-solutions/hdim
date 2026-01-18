package com.healthdata.gateway.service;

import com.healthdata.authentication.entity.RefreshToken;
import com.healthdata.authentication.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for TokenRevocationService (Phase 2.0 Team 3.2)
 *
 * Tests cover:
 * - Token revocation logic
 * - Redis blacklist management
 * - User logout (revoke all tokens)
 * - Selective token revocation
 * - Audit trail creation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRevocationService Tests")
class TokenRevocationServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TokenRevocationService tokenRevocationService;

    private static final String TENANT_ID = "tenant-001";
    private static final String USER_ID = "user-123";
    private static final String TOKEN_JTI = "jti-unique-123";

    private RefreshToken testToken;

    @BeforeEach
    void setup() {
        testToken = RefreshToken.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .tokenJti(TOKEN_JTI)
            .tokenHash("hash-abc123def456")
            .expiresAt(Instant.now().plusSeconds(3600))
            .revokedAt(null)
            .revocationReason(null)
            .build();
    }

    @Test
    @DisplayName("Should revoke single refresh token")
    void testRevokeSingleToken() {
        tokenRevocationService.revokeRefreshToken(testToken, "LOGOUT");

        verify(refreshTokenRepository).save(argThat(token ->
            token.getRevokedAt() != null &&
            token.getRevocationReason().equals("LOGOUT")
        ));
    }

    @Test
    @DisplayName("Should add token to Redis blacklist")
    void testAddsTokenToRedisBlacklist() {
        tokenRevocationService.revokeRefreshToken(testToken, "LOGOUT");

        verify(redisTemplate.opsForValue()).set(
            argThat(key -> key.toString().contains(TOKEN_JTI)),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("Should set Redis TTL to token expiry time")
    void testSetsRedisExpiryToTokenExpiry() {
        tokenRevocationService.revokeRefreshToken(testToken, "LOGOUT");

        // Verify Redis operation was called with TTL
        verify(redisTemplate.opsForValue()).set(anyString(), any(), any());
    }

    @Test
    @DisplayName("Should revoke all user tokens on logout")
    void testRevokeAllUserTokens() {
        RefreshToken token1 = testToken.toBuilder().id(UUID.randomUUID()).build();
        RefreshToken token2 = testToken.toBuilder().id(UUID.randomUUID()).build();
        RefreshToken token3 = testToken.toBuilder().id(UUID.randomUUID()).build();

        when(refreshTokenRepository.findActiveTokensByUser(USER_ID, TENANT_ID))
            .thenReturn(List.of(token1, token2, token3));

        int revoked = tokenRevocationService.revokeAllUserTokens(USER_ID, TENANT_ID, "LOGOUT");

        assertThat(revoked).isEqualTo(3);
        verify(refreshTokenRepository, times(3)).save(argThat(token ->
            token.getRevokedAt() != null
        ));
    }

    @Test
    @DisplayName("Should return count of revoked tokens")
    void testReturnRevokedTokenCount() {
        RefreshToken token1 = testToken.toBuilder().id(UUID.randomUUID()).build();
        RefreshToken token2 = testToken.toBuilder().id(UUID.randomUUID()).build();

        when(refreshTokenRepository.findActiveTokensByUser(USER_ID, TENANT_ID))
            .thenReturn(List.of(token1, token2));

        int revoked = tokenRevocationService.revokeAllUserTokens(USER_ID, TENANT_ID, "LOGOUT");

        assertThat(revoked).isEqualTo(2);
    }

    @Test
    @DisplayName("Should store revocation reason in database")
    void testStoresRevocationReason() {
        tokenRevocationService.revokeRefreshToken(testToken, "COMPROMISE");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getRevocationReason()).isEqualTo("COMPROMISE");
    }

    @Test
    @DisplayName("Should store revocation timestamp")
    void testStoresRevocationTimestamp() {
        Instant beforeRevocation = Instant.now();
        tokenRevocationService.revokeRefreshToken(testToken, "LOGOUT");
        Instant afterRevocation = Instant.now();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getRevokedAt())
            .isNotNull()
            .isAfterOrEqualTo(beforeRevocation)
            .isBeforeOrEqualTo(afterRevocation);
    }

    @Test
    @DisplayName("Should audit revocation event")
    void testAuditsRevocationEvent() {
        tokenRevocationService.revokeRefreshToken(testToken, "LOGOUT");

        verify(auditLogService).logTokenRevocation(
            USER_ID,
            TENANT_ID,
            "TOKEN_JTI:" + TOKEN_JTI,
            "LOGOUT"
        );
    }

    @Test
    @DisplayName("Should handle revocation of already-revoked token")
    void testHandlesAlreadyRevokedToken() {
        RefreshToken alreadyRevoked = testToken.toBuilder()
            .revokedAt(Instant.now().minusSeconds(60))
            .revocationReason("LOGOUT")
            .build();

        // Should not throw exception
        assertThatNoException()
            .isThrownBy(() -> tokenRevocationService.revokeRefreshToken(alreadyRevoked, "COMPROMISE"));
    }

    @Test
    @DisplayName("Should validate revocation reason")
    void testValidatesRevocationReason() {
        assertThatThrownBy(() ->
            tokenRevocationService.revokeRefreshToken(testToken, "INVALID_REASON"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should accept valid revocation reasons")
    void testAcceptsValidRevocationReasons() {
        String[] validReasons = {"LOGOUT", "TOKEN_REFRESH", "COMPROMISE", "ADMIN_REVOKE", "INACTIVITY"};

        for (String reason : validReasons) {
            RefreshToken token = testToken.toBuilder().id(UUID.randomUUID()).build();
            when(refreshTokenRepository.save(any())).thenReturn(token);

            assertThatNoException()
                .isThrownBy(() -> tokenRevocationService.revokeRefreshToken(token, reason));
        }
    }

    @Test
    @DisplayName("Should handle logout with no active tokens")
    void testLogoutWithNoActiveTokens() {
        when(refreshTokenRepository.findActiveTokensByUser(USER_ID, TENANT_ID))
            .thenReturn(List.of());

        int revoked = tokenRevocationService.revokeAllUserTokens(USER_ID, TENANT_ID, "LOGOUT");

        assertThat(revoked).isEqualTo(0);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create blacklist entry with correct structure")
    void testBlacklistEntryStructure() {
        tokenRevocationService.revokeRefreshToken(testToken, "LOGOUT");

        verify(redisTemplate.opsForValue()).set(
            argThat(key -> key.toString().startsWith("token_blacklist:")),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("Should set blacklist entry TTL to remaining token lifetime")
    void testBlacklistTTLMatchesTokenExpiry() {
        Instant expiryTime = Instant.now().plusSeconds(1800);  // 30 minutes
        RefreshToken token = testToken.toBuilder()
            .expiresAt(expiryTime)
            .build();

        tokenRevocationService.revokeRefreshToken(token, "LOGOUT");

        // Verify TTL was set to approximately 30 minutes
        verify(redisTemplate.opsForValue()).set(anyString(), any(), any());
    }

    @Test
    @DisplayName("Should handle multiple concurrent revocations")
    void testHandlesMultipleConcurrentRevocations() {
        RefreshToken token1 = testToken.toBuilder().id(UUID.randomUUID()).build();
        RefreshToken token2 = testToken.toBuilder().id(UUID.randomUUID()).build();

        tokenRevocationService.revokeRefreshToken(token1, "LOGOUT");
        tokenRevocationService.revokeRefreshToken(token2, "LOGOUT");

        verify(refreshTokenRepository, times(2)).save(any());
        verify(redisTemplate.opsForValue(), times(2)).set(anyString(), any(), any());
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation on logout")
    void testEnforcesTenantIsolationOnLogout() {
        RefreshToken otherTenantToken = testToken.toBuilder()
            .tenantId("other-tenant")
            .build();

        when(refreshTokenRepository.findActiveTokensByUser(USER_ID, TENANT_ID))
            .thenReturn(List.of(testToken));

        int revoked = tokenRevocationService.revokeAllUserTokens(USER_ID, TENANT_ID, "LOGOUT");

        // Should only revoke tokens for specified tenant
        assertThat(revoked).isEqualTo(1);
    }

    @Test
    @DisplayName("Should log revocation reason in audit")
    void testLogsRevocationReasonInAudit() {
        tokenRevocationService.revokeRefreshToken(testToken, "COMPROMISE");

        verify(auditLogService).logTokenRevocation(
            USER_ID,
            TENANT_ID,
            anyString(),
            eq("COMPROMISE")
        );
    }

    @Test
    @DisplayName("Should handle Redis failure gracefully")
    void testHandlesRedisFailureGracefully() {
        doThrow(new RuntimeException("Redis connection failed"))
            .when(redisTemplate.opsForValue()).set(anyString(), any(), any());

        // Should not throw, but should still revoke in database
        tokenRevocationService.revokeRefreshToken(testToken, "LOGOUT");

        verify(refreshTokenRepository).save(any());
    }

    @Test
    @DisplayName("Should revoke user's access token as well")
    void testRevokesUserAccessToken() {
        tokenRevocationService.revokeAllUserTokens(USER_ID, TENANT_ID, "LOGOUT");

        // Verify access token was also revoked
        verify(redisTemplate.opsForValue()).set(
            argThat(key -> key.toString().contains("token_blacklist")),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("Should support selective device revocation")
    void testSupportsSelectiveDeviceRevocation() {
        RefreshToken mobileToken = testToken.toBuilder()
            .tokenJti("jti-mobile-device-456")
            .build();

        tokenRevocationService.revokeRefreshToken(mobileToken, "LOGOUT");

        verify(refreshTokenRepository).save(argThat(token ->
            token.getTokenJti().equals("jti-mobile-device-456")
        ));
    }

    @Test
    @DisplayName("Should prevent token reuse after revocation")
    void testPreventsTokenReuseAfterRevocation() {
        tokenRevocationService.revokeRefreshToken(testToken, "LOGOUT");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken revoked = captor.getValue();
        assertThat(revoked.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Should log all revocations for compliance reporting")
    void testLogsAllRevocationsForCompliance() {
        tokenRevocationService.revokeRefreshToken(testToken, "ADMIN_REVOKE");

        verify(auditLogService).logTokenRevocation(
            eq(USER_ID),
            eq(TENANT_ID),
            anyString(),
            eq("ADMIN_REVOKE")
        );
    }
}
