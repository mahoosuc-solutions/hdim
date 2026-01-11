package com.healthdata.authentication.service;

import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.entity.RefreshToken;
import com.healthdata.authentication.repository.RefreshTokenRepository;
import com.healthdata.authentication.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefreshTokenService.
 *
 * Tests cover:
 * - Token creation with and without HTTP context
 * - Token validation (valid, expired, revoked)
 * - Token revocation (single and batch)
 * - Cleanup operations
 * - User retrieval from tokens
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private String testJwtToken;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .build();

        testJwtToken = "test.jwt.token";

        // Default config mock (lenient as not all tests use this)
        lenient().when(jwtConfig.getRefreshTokenExpirationMillis()).thenReturn(604800000L); // 7 days
    }

    @Nested
    @DisplayName("Token Creation")
    class TokenCreation {

        @Test
        @DisplayName("should create refresh token with HTTP request context")
        void shouldCreateTokenWithHttpContext() {
            // Given
            String testIp = "192.168.1.100";
            String testUserAgent = "Mozilla/5.0";

            when(request.getHeader("X-Forwarded-For")).thenReturn(testIp);
            when(request.getHeader("User-Agent")).thenReturn(testUserAgent);

            RefreshToken savedToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token(testJwtToken)
                    .user(testUser)
                    .ipAddress(testIp)
                    .userAgent(testUserAgent)
                    .expiresAt(Instant.now().plusMillis(604800000L))
                    .build();

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            // When
            RefreshToken result = refreshTokenService.createRefreshToken(testUser, testJwtToken, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getToken()).isEqualTo(testJwtToken);
            assertThat(result.getIpAddress()).isEqualTo(testIp);
            assertThat(result.getUserAgent()).isEqualTo(testUserAgent);

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());

            RefreshToken capturedToken = tokenCaptor.getValue();
            assertThat(capturedToken.getTokenHash()).isNotNull(); // Verify token was hashed
        }

        @Test
        @DisplayName("should create refresh token without HTTP request context")
        void shouldCreateTokenWithoutHttpContext() {
            // Given
            RefreshToken savedToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token(testJwtToken)
                    .user(testUser)
                    .ipAddress(null)
                    .userAgent(null)
                    .expiresAt(Instant.now().plusMillis(604800000L))
                    .build();

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            // When
            RefreshToken result = refreshTokenService.createRefreshToken(testUser, testJwtToken, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getIpAddress()).isNull();
            assertThat(result.getUserAgent()).isNull();

            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("should handle X-Real-IP header as fallback")
        void shouldHandleXRealIpFallback() {
            // Given
            String testIp = "10.0.0.50";
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(testIp);
            when(request.getHeader("User-Agent")).thenReturn("Test");

            RefreshToken savedToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token(testJwtToken)
                    .user(testUser)
                    .ipAddress(testIp)
                    .userAgent("Test")
                    .expiresAt(Instant.now().plusMillis(604800000L))
                    .build();

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            // When
            RefreshToken result = refreshTokenService.createRefreshToken(testUser, testJwtToken, request);

            // Then
            assertThat(result.getIpAddress()).isEqualTo(testIp);
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("should validate active non-expired token")
        void shouldValidateActiveToken() {
            // Given
            RefreshToken activeToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token(testJwtToken)
                    .user(testUser)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            when(jwtTokenService.validateToken(anyString())).thenReturn(true);
            when(refreshTokenRepository.findByTokenAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(activeToken));

            // When
            Optional<RefreshToken> result = refreshTokenService.validateRefreshToken(testJwtToken);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(activeToken);
        }

        @Test
        @DisplayName("should reject expired token")
        void shouldRejectExpiredToken() {
            // Given
            RefreshToken expiredToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token(testJwtToken)
                    .user(testUser)
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .revoked(false)
                    .build();

            when(jwtTokenService.validateToken(anyString())).thenReturn(true);
            when(refreshTokenRepository.findByTokenAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(expiredToken));

            // When
            Optional<RefreshToken> result = refreshTokenService.validateRefreshToken(testJwtToken);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should reject revoked token")
        void shouldRejectRevokedToken() {
            // Given
            // Revoked tokens won't be returned by findByTokenAndRevokedAtIsNull
            when(jwtTokenService.validateToken(anyString())).thenReturn(true);
            when(refreshTokenRepository.findByTokenAndRevokedAtIsNull(anyString())).thenReturn(Optional.empty());

            // When
            Optional<RefreshToken> result = refreshTokenService.validateRefreshToken(testJwtToken);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for non-existent token")
        void shouldReturnEmptyForNonExistentToken() {
            // Given
            when(jwtTokenService.validateToken(anyString())).thenReturn(true);
            when(refreshTokenRepository.findByTokenAndRevokedAtIsNull(anyString())).thenReturn(Optional.empty());

            // When
            Optional<RefreshToken> result = refreshTokenService.validateRefreshToken(testJwtToken);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Token Revocation")
    class TokenRevocation {

        @Test
        @DisplayName("should revoke existing token")
        void shouldRevokeExistingToken() {
            // Given
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token(testJwtToken)
                    .user(testUser)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(token));

            // When
            boolean result = refreshTokenService.revokeRefreshToken(testJwtToken);

            // Then
            assertThat(result).isTrue();
            assertThat(token.isRevoked()).isTrue();
            assertThat(token.getRevokedAt()).isNotNull();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("should return false for non-existent token")
        void shouldReturnFalseForNonExistentToken() {
            // Given
            when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

            // When
            boolean result = refreshTokenService.revokeRefreshToken(testJwtToken);

            // Then
            assertThat(result).isFalse();
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("should revoke all tokens for user")
        void shouldRevokeAllUserTokens() {
            // Given
            when(refreshTokenRepository.revokeAllUserTokens(any(UUID.class), any(Instant.class))).thenReturn(3);

            // When
            int result = refreshTokenService.revokeAllUserTokens(testUserId);

            // Then
            assertThat(result).isEqualTo(3);
            verify(refreshTokenRepository).revokeAllUserTokens(any(UUID.class), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("User Retrieval")
    class UserRetrieval {

        @Test
        @DisplayName("should get user from valid token")
        void shouldGetUserFromValidToken() {
            // Given
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token(testJwtToken)
                    .user(testUser)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            when(jwtTokenService.validateToken(anyString())).thenReturn(true);
            when(refreshTokenRepository.findByTokenAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(token));

            // When
            Optional<User> result = refreshTokenService.getUserFromRefreshToken(testJwtToken);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("should return empty for invalid token")
        void shouldReturnEmptyForInvalidToken() {
            // Given
            when(jwtTokenService.validateToken(anyString())).thenReturn(false);

            // When
            Optional<User> result = refreshTokenService.getUserFromRefreshToken(testJwtToken);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Token Queries")
    class TokenQueries {

        @Test
        @DisplayName("should get active tokens for user")
        void shouldGetActiveTokensForUser() {
            // Given
            List<RefreshToken> activeTokens = List.of(
                    RefreshToken.builder().id(UUID.randomUUID()).user(testUser).revoked(false).build(),
                    RefreshToken.builder().id(UUID.randomUUID()).user(testUser).revoked(false).build()
            );

            when(refreshTokenRepository.findActiveTokensByUserId(any(UUID.class), any(Instant.class))).thenReturn(activeTokens);

            // When
            List<RefreshToken> result = refreshTokenService.getActiveTokensForUser(testUserId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(activeTokens);
        }

        @Test
        @DisplayName("should count active tokens for user")
        void shouldCountActiveTokens() {
            // Given
            when(refreshTokenRepository.countActiveTokensByUserId(any(UUID.class), any(Instant.class))).thenReturn(5L);

            // When
            long result = refreshTokenService.countActiveTokens(testUserId);

            // Then
            assertThat(result).isEqualTo(5L);
            verify(refreshTokenRepository).countActiveTokensByUserId(any(UUID.class), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("Cleanup Operations")
    class CleanupOperations {

        @Test
        @DisplayName("should delete expired tokens")
        void shouldDeleteExpiredTokens() {
            // Given
            when(refreshTokenRepository.deleteByExpiresAtBefore(any(Instant.class))).thenReturn(10);

            // When
            int result = refreshTokenService.deleteExpiredTokens();

            // Then
            assertThat(result).isEqualTo(10);
            verify(refreshTokenRepository).deleteByExpiresAtBefore(any(Instant.class));
        }

        @Test
        @DisplayName("should delete old revoked tokens")
        void shouldDeleteOldRevokedTokens() {
            // Given
            int daysOld = 30;
            when(refreshTokenRepository.deleteRevokedTokensBefore(any(Instant.class))).thenReturn(15);

            // When
            int result = refreshTokenService.deleteOldRevokedTokens(daysOld);

            // Then
            assertThat(result).isEqualTo(15);
            verify(refreshTokenRepository).deleteRevokedTokensBefore(any(Instant.class));
        }
    }
}
