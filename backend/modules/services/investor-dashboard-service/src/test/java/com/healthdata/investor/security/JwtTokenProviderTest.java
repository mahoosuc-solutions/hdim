package com.healthdata.investor.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtTokenProvider.
 * Tests JWT token generation, validation, and parsing.
 */
@DisplayName("JWT Token Provider Tests")
@Tag("unit")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // Base64 encoded secret key (at least 512 bits for HS512 = 64 bytes)
    // Exactly 64 ASCII characters = 64 bytes = 512 bits
    private static final String TEST_SECRET_RAW = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+/"; // 64 chars
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(TEST_SECRET_RAW.getBytes());
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 86400000L; // 24 hours

    private static final UUID TEST_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String TEST_EMAIL = "investor@test.com";
    private static final String TEST_ROLE = "ADMIN";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid access token with correct claims")
        void shouldGenerateAccessTokenWithCorrectClaims() {
            // When
            String token = jwtTokenProvider.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // Then
            assertThat(token).isNotNull().isNotBlank();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
            assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(TEST_USER_ID);
            assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(TEST_EMAIL);
            assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(TEST_ROLE);
        }

        @Test
        @DisplayName("Should generate valid refresh token with correct claims")
        void shouldGenerateRefreshTokenWithCorrectClaims() {
            // When
            String token = jwtTokenProvider.generateRefreshToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // Then
            assertThat(token).isNotNull().isNotBlank();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
            assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(TEST_USER_ID);
            assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(TEST_EMAIL);
            assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(TEST_ROLE);
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            // Given
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            // When
            String token1 = jwtTokenProvider.generateToken(userId1, "user1@test.com", "USER");
            String token2 = jwtTokenProvider.generateToken(userId2, "user2@test.com", "ADMIN");

            // Then
            assertThat(token1).isNotEqualTo(token2);
            assertThat(jwtTokenProvider.getUserIdFromToken(token1)).isEqualTo(userId1);
            assertThat(jwtTokenProvider.getUserIdFromToken(token2)).isEqualTo(userId2);
        }

        @Test
        @DisplayName("Should generate different access and refresh tokens")
        void shouldGenerateDifferentAccessAndRefreshTokens() {
            // When
            String accessToken = jwtTokenProvider.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);
            String refreshToken = jwtTokenProvider.generateRefreshToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // Then
            assertThat(accessToken).isNotEqualTo(refreshToken);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate valid token")
        void shouldValidateValidToken() {
            // Given
            String token = jwtTokenProvider.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // When/Then
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            // Given
            String malformedToken = "not.a.valid.jwt.token";

            // When/Then
            assertThat(jwtTokenProvider.validateToken(malformedToken)).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            // When/Then
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("Should return false for token signed with different secret")
        void shouldReturnFalseForTokenWithWrongSignature() {
            // Given - create a provider with a DIFFERENT secret key
            String differentSecretRaw = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"; // Different 64-char key
            String differentSecret = Base64.getEncoder().encodeToString(differentSecretRaw.getBytes());
            JwtTokenProvider differentProvider = new JwtTokenProvider(differentSecret, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);

            // Generate token with the different provider
            String tokenFromDifferentProvider = differentProvider.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // When/Then - token signed with different secret should fail validation on original provider
            assertThat(jwtTokenProvider.validateToken(tokenFromDifferentProvider)).isFalse();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            // Given - create provider with very short expiration
            JwtTokenProvider shortLivedProvider = new JwtTokenProvider(TEST_SECRET, 1L, 1L); // 1ms expiration
            String token = shortLivedProvider.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When/Then
            assertThat(shortLivedProvider.validateToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should detect non-expired token")
        void shouldDetectNonExpiredToken() {
            // Given
            String token = jwtTokenProvider.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // When/Then
            assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
        }

        @Test
        @DisplayName("Should detect expired token")
        void shouldDetectExpiredToken() {
            // Given - create provider with very short expiration
            JwtTokenProvider shortLivedProvider = new JwtTokenProvider(TEST_SECRET, 1L, 1L);
            String token = shortLivedProvider.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When/Then
            assertThat(shortLivedProvider.isTokenExpired(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("Token Parsing Tests")
    class TokenParsingTests {

        @Test
        @DisplayName("Should extract user ID from token")
        void shouldExtractUserId() {
            // Given
            String token = jwtTokenProvider.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // When
            UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            // Then
            assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should extract email from token")
        void shouldExtractEmail() {
            // Given
            String token = jwtTokenProvider.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // When
            String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

            // Then
            assertThat(extractedEmail).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should extract role from token")
        void shouldExtractRole() {
            // Given
            String token = jwtTokenProvider.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

            // When
            String extractedRole = jwtTokenProvider.getRoleFromToken(token);

            // Then
            assertThat(extractedRole).isEqualTo(TEST_ROLE);
        }

        @Test
        @DisplayName("Should throw exception when parsing malformed token")
        void shouldThrowWhenParsingMalformedToken() {
            // Given
            String malformedToken = "invalid.token";

            // When/Then
            assertThatThrownBy(() -> jwtTokenProvider.getUserIdFromToken(malformedToken))
                    .isInstanceOf(MalformedJwtException.class);
        }
    }
}
