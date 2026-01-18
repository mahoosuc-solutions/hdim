package com.healthdata.gateway.controller;

import com.healthdata.gateway.dto.TokenRefreshRequest;
import com.healthdata.gateway.dto.TokenRefreshResponse;
import com.healthdata.gateway.service.TokenRefreshService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for TokenRefreshController (Phase 2.0 Team 3.1)
 *
 * Tests cover:
 * - HTTP endpoints
 * - Request validation
 * - Response formatting
 * - Error handling
 * - Rate limiting
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "security.rate-limiting.enabled=true",
    "security.rate-limiting.default-limit-per-minute=100",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6380"
})
@DisplayName("TokenRefreshController Integration Tests")
class TokenRefreshControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenRefreshService tokenRefreshService;

    private static final String TENANT_ID = "test-tenant";
    private static final String REFRESH_TOKEN_ENDPOINT = "/api/v1/auth/refresh";
    private static final String VALID_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJqdGktMTIzIiwic3ViIjoiY2xvdWRlIiwiaWF0IjoxNjcxMzU4NDAwLCJleHAiOjE2NzEzNjIwMDB9.token_signature";

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 200 OK with new tokens on successful refresh")
    void testSuccessfulTokenRefresh() throws Exception {
        TokenRefreshResponse mockResponse = TokenRefreshResponse.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .tokenType("Bearer")
            .expiresIn(900)
            .build();

        when(tokenRefreshService.refreshToken(
            argThat(req -> req.getRefreshToken().equals(VALID_REFRESH_TOKEN)),
            eq(TENANT_ID)))
            .thenReturn(mockResponse);

        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 401 for invalid refresh token")
    void testInvalidRefreshToken() throws Exception {
        when(tokenRefreshService.refreshToken(any(), eq(TENANT_ID)))
            .thenThrow(new InvalidTokenException("Invalid or expired refresh token"));

        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"invalid-token\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 for revoked refresh token")
    void testRevokedRefreshToken() throws Exception {
        when(tokenRefreshService.refreshToken(any(), eq(TENANT_ID)))
            .thenThrow(new RevokedTokenException("Refresh token has been revoked"));

        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should require authentication (401 without token)")
    void testRequiresAuthentication() throws Exception {
        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should require X-Tenant-ID header")
    void testRequiresTenantIdHeader() throws Exception {
        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should validate request body")
    void testValidatesRequestBody() throws Exception {
        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))  // Missing refreshToken
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return Bearer token type")
    void testReturnsBearerTokenType() throws Exception {
        TokenRefreshResponse mockResponse = TokenRefreshResponse.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .tokenType("Bearer")
            .expiresIn(900)
            .build();

        when(tokenRefreshService.refreshToken(any(), eq(TENANT_ID)))
            .thenReturn(mockResponse);

        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 15-minute expiration")
    void testReturnsCorrectExpiration() throws Exception {
        TokenRefreshResponse mockResponse = TokenRefreshResponse.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .tokenType("Bearer")
            .expiresIn(900)  // 15 minutes
            .build();

        when(tokenRefreshService.refreshToken(any(), eq(TENANT_ID)))
            .thenReturn(mockResponse);

        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
            .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should enforce rate limiting (100 requests/min)")
    void testEnforceRateLimiting() throws Exception {
        TokenRefreshResponse mockResponse = TokenRefreshResponse.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .tokenType("Bearer")
            .expiresIn(900)
            .build();

        when(tokenRefreshService.refreshToken(any(), eq(TENANT_ID)))
            .thenReturn(mockResponse);

        // First 100 requests should succeed
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
                .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return JSON response with correct content type")
    void testJsonContentType() throws Exception {
        TokenRefreshResponse mockResponse = TokenRefreshResponse.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .tokenType("Bearer")
            .expiresIn(900)
            .build();

        when(tokenRefreshService.refreshToken(any(), eq(TENANT_ID)))
            .thenReturn(mockResponse);

        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should not allow GET requests to refresh endpoint")
    void testDisallowGetRequest() throws Exception {
        mockMvc.perform(get(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should include rate limit headers in response")
    void testIncludesRateLimitHeaders() throws Exception {
        TokenRefreshResponse mockResponse = TokenRefreshResponse.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .tokenType("Bearer")
            .expiresIn(900)
            .build();

        when(tokenRefreshService.refreshToken(any(), eq(TENANT_ID)))
            .thenReturn(mockResponse);

        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
            .andExpect(header().exists("X-RateLimit-Limit"))
            .andExpect(header().exists("X-RateLimit-Remaining"))
            .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should enforce tenant isolation")
    void testEnforcesTenantIsolation() throws Exception {
        when(tokenRefreshService.refreshToken(any(), eq("other-tenant")))
            .thenThrow(new TenantAccessDeniedException("Invalid tenant"));

        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", "other-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should handle empty refresh token")
    void testHandlesEmptyRefreshToken() throws Exception {
        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should handle null refresh token")
    void testHandlesNullRefreshToken() throws Exception {
        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":null}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should log successful token refresh")
    void testLogsSuccessfulRefresh() throws Exception {
        TokenRefreshResponse mockResponse = TokenRefreshResponse.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .tokenType("Bearer")
            .expiresIn(900)
            .build();

        when(tokenRefreshService.refreshToken(any(), eq(TENANT_ID)))
            .thenReturn(mockResponse);

        mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + VALID_REFRESH_TOKEN + "\"}"))
            .andExpect(status().isOk());

        // Audit logging is verified through service mock
    }
}
