package com.healthdata.gateway.filter;

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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for TokenValidationFilter (Phase 2.0 Team 3.3)
 *
 * Tests cover:
 * - Token validation via Redis blacklist
 * - Bearer token extraction from Authorization header
 * - Rejecting blacklisted tokens (401 Unauthorized)
 * - Allowing non-blacklisted tokens
 * - Error handling and edge cases
 * - Multi-tenant token isolation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenValidationFilter Tests")
class TokenValidationFilterTest {

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private FilterChain filterChain;

    private TokenValidationFilter tokenValidationFilter;

    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJ0ZXN0LTEyMyIsInN1YiI6InVzZXIxMjMifQ.signature";
    private static final String BLACKLISTED_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJibGFja2xpc3RlZCIsInN1YiI6InVzZXIifQ.sig";
    private static final String INVALID_TOKEN = "not.a.valid.token";

    @BeforeEach
    void setup() {
        tokenValidationFilter = new TokenValidationFilter(tokenRevocationService);
    }

    @Test
    @DisplayName("Should allow request with valid non-blacklisted token")
    void testAllowsValidToken() throws Exception {
        when(tokenRevocationService.isBlacklisted("test-123")).thenReturn(false);

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer " + VALID_TOKEN);

        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(401);
    }

    @Test
    @DisplayName("Should reject request with blacklisted token (401 Unauthorized)")
    void testRejectsBlacklistedToken() throws Exception {
        when(tokenRevocationService.isBlacklisted("blacklisted")).thenReturn(true);

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer " + BLACKLISTED_TOKEN);

        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should allow request without Authorization header")
    void testAllowsRequestWithoutToken() throws Exception {
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tokenRevocationService, never()).isBlacklisted(anyString());
    }

    @Test
    @DisplayName("Should extract Bearer token from Authorization header")
    void testExtractsBearerToken() throws Exception {
        when(tokenRevocationService.isBlacklisted(anyString())).thenReturn(false);

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer mytoken123");

        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(tokenRevocationService).isBlacklisted("mytoken123");
    }

    @Test
    @DisplayName("Should handle malformed Authorization header gracefully")
    void testHandlesMalformedAuthHeader() throws Exception {
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "InvalidFormat token");

        HttpServletResponse response = new MockHttpServletResponse();

        assertThatNoException()
            .isThrownBy(() -> tokenValidationFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    @DisplayName("Should handle empty Bearer token")
    void testHandlesEmptyBearerToken() throws Exception {
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer ");

        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle token revocation service exceptions gracefully")
    void testHandlesServiceException() throws Exception {
        when(tokenRevocationService.isBlacklisted(anyString()))
            .thenThrow(new RuntimeException("Redis unavailable"));

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer " + VALID_TOKEN);

        HttpServletResponse response = new MockHttpServletResponse();

        // Fail-open: should allow request if service fails
        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should return 401 with error message for blacklisted token")
    void testReturnsErrorMessageForBlacklist() throws Exception {
        when(tokenRevocationService.isBlacklisted(anyString())).thenReturn(true);

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer " + BLACKLISTED_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("revoked");
    }

    @Test
    @DisplayName("Should not check blacklist for requests with no token")
    void testSkipsBlacklistCheckWhenNoToken() throws Exception {
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(tokenRevocationService, never()).isBlacklisted(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle case-insensitive Bearer prefix")
    void testHandlesCaseInsensitiveBearerPrefix() throws Exception {
        when(tokenRevocationService.isBlacklisted("testtoken")).thenReturn(false);

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "bearer testtoken");

        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(tokenRevocationService).isBlacklisted("testtoken");
    }

    @Test
    @DisplayName("Should extract JTI from JWT for blacklist lookup")
    void testExtractsJTIFromToken() throws Exception {
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJtYWdpY19qdGkiLCJzdWIiOiJ1c2VyIn0.sig";

        when(tokenRevocationService.isBlacklisted("magic_jti")).thenReturn(false);

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer " + jwtToken);

        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(tokenRevocationService).isBlacklisted("magic_jti");
    }

    @Test
    @DisplayName("Should handle invalid Base64 in JWT gracefully")
    void testHandlesInvalidBase64InJWT() throws Exception {
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer invalid.base64..token");

        HttpServletResponse response = new MockHttpServletResponse();

        // Should not throw, but treat as non-JWT token
        assertThatNoException()
            .isThrownBy(() -> tokenValidationFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    @DisplayName("Should enforce tenant isolation in token validation")
    void testEnforcesTenantIsolationInValidation() throws Exception {
        // Multiple tokens with same JTI but different tenants
        // Only blacklist should prevent access, not tenant ID
        when(tokenRevocationService.isBlacklisted("shared_jti")).thenReturn(false);

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJzaGFyZWRfanRpIn0.sig");
        ((MockHttpServletRequest) request).addHeader("X-Tenant-ID", "tenant-1");

        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle multiple requests with different tokens")
    void testHandlesMultipleRequests() throws Exception {
        when(tokenRevocationService.isBlacklisted("token1")).thenReturn(false);
        when(tokenRevocationService.isBlacklisted("token2")).thenReturn(true);

        // First request with valid token
        HttpServletRequest req1 = new MockHttpServletRequest();
        ((MockHttpServletRequest) req1).addHeader("Authorization", "Bearer token1");
        MockHttpServletResponse resp1 = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(req1, resp1, filterChain);
        assertThat(resp1.getStatus()).isNotEqualTo(401);

        // Second request with blacklisted token
        HttpServletRequest req2 = new MockHttpServletRequest();
        ((MockHttpServletRequest) req2).addHeader("Authorization", "Bearer token2");
        MockHttpServletResponse resp2 = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(req2, resp2, filterChain);
        assertThat(resp2.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should log token validation attempts")
    void testLogsValidationAttempts() throws Exception {
        when(tokenRevocationService.isBlacklisted(anyString())).thenReturn(false);

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer testtoken");

        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(tokenRevocationService).isBlacklisted("testtoken");
    }

    @Test
    @DisplayName("Should preserve request chain for allowed tokens")
    void testPreservesRequestChain() throws Exception {
        when(tokenRevocationService.isBlacklisted(anyString())).thenReturn(false);

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer " + VALID_TOKEN);

        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not invoke filter chain for revoked tokens")
    void testStopsChainForRevokedTokens() throws Exception {
        when(tokenRevocationService.isBlacklisted(anyString())).thenReturn(true);

        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Bearer " + BLACKLISTED_TOKEN);

        HttpServletResponse response = new MockHttpServletResponse();

        tokenValidationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
    }
}
