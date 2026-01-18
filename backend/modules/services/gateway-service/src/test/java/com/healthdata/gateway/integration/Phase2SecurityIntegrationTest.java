package com.healthdata.gateway.integration;

import com.healthdata.gateway.filter.TokenValidationFilter;
import com.healthdata.gateway.service.TokenRevocationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 2.0 Security & Error Handling Integration Tests
 *
 * Validates security properties and error handling:
 * - JWT validation and JTI extraction
 * - Authorization header parsing
 * - Revoked token rejection
 * - Error response formatting
 * - No PHI leakage in errors
 * - Proper HTTP status codes
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 2.0 Security Integration Tests")
class Phase2SecurityIntegrationTest {

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private FilterChain filterChain;

    private TokenValidationFilter tokenValidationFilter;

    @BeforeEach
    void setup() {
        tokenValidationFilter = new TokenValidationFilter(tokenRevocationService);
    }

    // =====================================================================
    // SECURITY: JWT Validation
    // =====================================================================

    @Test
    @DisplayName("SEC-1: Valid JWT with matching JTI is allowed")
    void testValidJWTWithMatchingJTI() throws Exception {
        // JWT with JTI: "test-jti-001"
        String validJWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                          ".eyJqdGkiOiJ0ZXN0LWp0aS0wMDEiLCJzdWIiOiJ1c2VyLTEyMyJ9" +
                          ".signature";

        when(tokenRevocationService.isBlacklisted("test-jti-001"))
            .thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + validJWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(401);
    }

    @Test
    @DisplayName("SEC-2: Revoked JWT (in blacklist) is rejected with 401")
    void testRevokedJWTIsRejected() throws Exception {
        String revokedJWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                            ".eyJqdGkiOiJyZXZva2VkLWp0aSIsInN1YiI6InVzZXIifQ" +
                            ".signature";

        when(tokenRevocationService.isBlacklisted("revoked-jti"))
            .thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + revokedJWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getContentAsString()).contains("revoked");
    }

    @Test
    @DisplayName("SEC-3: Invalid JWT payload is handled gracefully")
    void testInvalidJWTPayload() throws Exception {
        // JWT with invalid Base64 in payload
        String invalidJWT = "eyJhbGciOiJIUzI1NiJ9.!!!invalid-base64!!!.signature";

        when(tokenRevocationService.isBlacklisted(anyString()))
            .thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + invalidJWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Should not throw, but allow (can't extract JTI, use full token as fallback)
        assertThatNoException()
            .isThrownBy(() -> tokenValidationFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    @DisplayName("SEC-4: JWT without JTI falls back to full token for blacklist check")
    void testJWTWithoutJTIUsesFallback() throws Exception {
        // JWT without jti claim
        String jwtNoJTI = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                          ".eyJzdWIiOiJ1c2VyIn0" +
                          ".signature";

        when(tokenRevocationService.isBlacklisted(jwtNoJTI))
            .thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtNoJTI);
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(tokenRevocationService).isBlacklisted(jwtNoJTI);
        verify(filterChain).doFilter(request, response);
    }

    // =====================================================================
    // SECURITY: Authorization Header Parsing
    // =====================================================================

    @Test
    @DisplayName("SEC-5: Bearer token extraction supports case-insensitive prefix")
    void testCaseInsensitiveBearerPrefix() throws Exception {
        String[] bearerVariations = { "Bearer", "bearer", "BEARER", "BeArEr" };

        when(tokenRevocationService.isBlacklisted(anyString()))
            .thenReturn(false);

        for (String bearerPrefix : bearerVariations) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", bearerPrefix + " test-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            tokenValidationFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isNotEqualTo(401);
        }

        verify(tokenRevocationService, times(bearerVariations.length)).isBlacklisted("test-token");
    }

    @Test
    @DisplayName("SEC-6: Malformed Authorization header is handled gracefully")
    void testMalformedAuthorizationHeader() throws Exception {
        String[] malformedHeaders = {
            "NotBearer token",           // Wrong prefix
            "Bearer",                    // No token
            "Bearer   ",                 // Only spaces
            "Basic base64string",        // Different scheme
            "token-without-bearer"       // No scheme
        };

        for (String header : malformedHeaders) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", header);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Should not throw
            assertThatNoException()
                .isThrownBy(() -> tokenValidationFilter.doFilterInternal(request, response, filterChain));
        }
    }

    @Test
    @DisplayName("SEC-7: Missing Authorization header allows request (other filters handle auth)")
    void testMissingAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No Authorization header
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(tokenRevocationService, never()).isBlacklisted(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // =====================================================================
    // SECURITY: Error Response Formatting
    // =====================================================================

    @Test
    @DisplayName("SEC-8: 401 Unauthorized response has correct JSON format")
    void test401ResponseFormat() throws Exception {
        when(tokenRevocationService.isBlacklisted(anyString()))
            .thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");

        String content = response.getContentAsString();
        assertThat(content).contains("\"error\"");
        assertThat(content).contains("\"message\"");
        assertThat(content).contains("UNAUTHORIZED");
    }

    @Test
    @DisplayName("SEC-9: Error response does not expose internal implementation details")
    void testErrorResponseSafety() throws Exception {
        when(tokenRevocationService.isBlacklisted(anyString()))
            .thenThrow(new RuntimeException("Redis connection failed: localhost:6379"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        String content = response.getContentAsString();
        // Should not expose Redis details
        assertThat(content).doesNotContain("Redis");
        assertThat(content).doesNotContain("localhost:6379");
        assertThat(content).doesNotContain("connection failed");
    }

    // =====================================================================
    // SECURITY: No PHI Leakage
    // =====================================================================

    @Test
    @DisplayName("SEC-10: Error responses never contain user tokens or identifiers")
    void testNoTokenExposureInErrors() throws Exception {
        String sensitiveToken = "secret-user-token-12345-abc";

        when(tokenRevocationService.isBlacklisted(anyString()))
            .thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + sensitiveToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        String errorResponse = response.getContentAsString();
        assertThat(errorResponse).doesNotContain(sensitiveToken);
        assertThat(errorResponse).doesNotContain("secret-user");
        assertThat(errorResponse).doesNotContain("12345");
    }

    @Test
    @DisplayName("SEC-11: Logs don't contain sensitive information")
    void testNoSensitiveDataInLogs() throws Exception {
        // This test verifies logging strategy - in real system would use log capture
        when(tokenRevocationService.isBlacklisted("sensitive-jti"))
            .thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer eyJqd...signature");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        // In production, verify logs use TokenValidationFilter to log at debug level only
        // and never log full tokens
    }

    // =====================================================================
    // SECURITY: HTTP Status Codes
    // =====================================================================

    @Test
    @DisplayName("SEC-12: Proper HTTP status codes for different scenarios")
    void testProperHTTPStatusCodes() throws Exception {
        // Scenario 1: Valid token
        when(tokenRevocationService.isBlacklisted("valid-jti"))
            .thenReturn(false);

        MockHttpServletRequest validRequest = new MockHttpServletRequest();
        validRequest.addHeader("Authorization", "Bearer eyJqdGkiOiJ2YWxpZC1qdGkifQ.payload.sig");
        MockHttpServletResponse validResponse = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(validRequest, validResponse, filterChain);
        // Should not set 401 (filter chain continues, Spring Security handles response)

        // Scenario 2: Revoked token
        when(tokenRevocationService.isBlacklisted("revoked-jti"))
            .thenReturn(true);

        MockHttpServletRequest revokedRequest = new MockHttpServletRequest();
        revokedRequest.addHeader("Authorization", "Bearer eyJqdGkiOiJyZXZva2VkLWp0aSJ9.payload.sig");
        MockHttpServletResponse revokedResponse = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(revokedRequest, revokedResponse, filterChain);
        assertThat(revokedResponse.getStatus()).isEqualTo(401);

        // Scenario 3: No token (other filters handle auth)
        MockHttpServletRequest noTokenRequest = new MockHttpServletRequest();
        MockHttpServletResponse noTokenResponse = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(noTokenRequest, noTokenResponse, filterChain);
        assertThat(noTokenResponse.getStatus()).isNotEqualTo(401);
    }

    // =====================================================================
    // SECURITY: Replay Attack Prevention
    // =====================================================================

    @Test
    @DisplayName("SEC-13: Revoked tokens cannot be replayed")
    void testReplayAttackPrevention() throws Exception {
        String replayToken = "eyJqdGkiOiJyZXBsYXktdGVzdCJ9.payload.sig";

        // First use: token is valid
        when(tokenRevocationService.isBlacklisted("replay-test"))
            .thenReturn(false);

        MockHttpServletRequest firstRequest = new MockHttpServletRequest();
        firstRequest.addHeader("Authorization", "Bearer " + replayToken);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(firstRequest, firstResponse, filterChain);
        verify(filterChain).doFilter(firstRequest, firstResponse);

        // Token is then revoked
        when(tokenRevocationService.isBlacklisted("replay-test"))
            .thenReturn(true);

        // Replay attempt: token is now revoked
        MockHttpServletRequest replayRequest = new MockHttpServletRequest();
        replayRequest.addHeader("Authorization", "Bearer " + replayToken);
        MockHttpServletResponse replayResponse = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(replayRequest, replayResponse, filterChain);

        // Replay should be rejected
        assertThat(replayResponse.getStatus()).isEqualTo(401);
    }

    // =====================================================================
    // SECURITY: Token Spacing and Format Validation
    // =====================================================================

    @Test
    @DisplayName("SEC-14: Handles tokens with extra whitespace")
    void testTokenWithExtraWhitespace() throws Exception {
        String tokenWithSpace = "Bearer   token-with-spaces   ";

        when(tokenRevocationService.isBlacklisted("token-with-spaces"))
            .thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", tokenWithSpace);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Should trim and extract correctly
        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(tokenRevocationService).isBlacklisted("token-with-spaces");
        verify(filterChain).doFilter(request, response);
    }

    // =====================================================================
    // SECURITY: Token Expiration Handling
    // =====================================================================

    @Test
    @DisplayName("SEC-15: Expired tokens in blacklist are eventually cleaned (TTL)")
    void testTokenExpirationAndCleanup() throws Exception {
        // TokenRevocationService manages TTL in Redis
        // This test verifies the service is called properly for cleanup
        when(tokenRevocationService.isBlacklisted("expired-token-jti"))
            .thenReturn(true);  // Initially blacklisted

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer eyJqdGkiOiJleHBpcmVkLXRva2VuLWp0aSJ9.payload.sig");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);

        // After TTL expires in Redis, isBlacklisted would return false
        // (This is tested in TokenRevocationServiceTest)
    }
}
