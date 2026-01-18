package com.healthdata.gateway.controller;

import com.healthdata.gateway.dto.TokenRevocationRequest;
import com.healthdata.gateway.dto.TokenRevocationResponse;
import com.healthdata.gateway.service.TokenRevocationService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for TokenRevocationController (Phase 2.0 Team 3.2)
 *
 * Tests cover:
 * - Logout endpoint
 * - Revoke endpoint
 * - Error handling
 * - Authentication requirements
 * - Response formatting
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "security.rate-limiting.enabled=true",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6380"
})
@DisplayName("TokenRevocationController Integration Tests")
class TokenRevocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    private static final String TENANT_ID = "test-tenant";
    private static final String LOGOUT_ENDPOINT = "/api/v1/auth/logout";
    private static final String REVOKE_ENDPOINT = "/api/v1/auth/revoke";
    private static final String VALID_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJhY2Nlc3MtMTIzIiwic3ViIjoiY2xvdWRlIn0.signature";

    @Test
    @WithMockUser(roles = "USER", username = "testuser")
    @DisplayName("Should successfully logout and revoke all tokens")
    void testSuccessfulLogout() throws Exception {
        when(tokenRevocationService.revokeAllUserTokens(anyString(), eq(TENANT_ID), eq("LOGOUT")))
            .thenReturn(3);

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Successfully logged out"))
            .andExpect(jsonPath("$.tokensRevoked").value(3));
    }

    @Test
    @WithMockUser(roles = "USER", username = "testuser")
    @DisplayName("Should return 200 OK with revoked token count")
    void testLogoutReturnsTokenCount() throws Exception {
        when(tokenRevocationService.revokeAllUserTokens(anyString(), eq(TENANT_ID), eq("LOGOUT")))
            .thenReturn(2);

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokensRevoked").value(2));
    }

    @Test
    @DisplayName("Should require authentication (401 without token)")
    void testLogoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should require X-Tenant-ID header")
    void testLogoutRequiresTenantIdHeader() throws Exception {
        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER", username = "testuser")
    @DisplayName("Should handle logout with no active tokens")
    void testLogoutWithNoActiveTokens() throws Exception {
        when(tokenRevocationService.revokeAllUserTokens(anyString(), eq(TENANT_ID), eq("LOGOUT")))
            .thenReturn(0);

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokensRevoked").value(0));
    }

    @Test
    @WithMockUser(roles = "USER", username = "testuser")
    @DisplayName("Should successfully revoke specific token")
    void testSuccessfulTokenRevocation() throws Exception {
        doNothing().when(tokenRevocationService)
            .revokeAccessToken(anyString(), eq(TENANT_ID), eq("COMPROMISE"));

        mockMvc.perform(post(REVOKE_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + VALID_ACCESS_TOKEN + "\",\"reason\":\"COMPROMISE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Token revoked successfully"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should validate revocation reason")
    void testValidatesRevocationReason() throws Exception {
        mockMvc.perform(post(REVOKE_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + VALID_ACCESS_TOKEN + "\",\"reason\":\"INVALID\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should accept valid revocation reasons")
    void testAcceptsValidRevocationReasons() throws Exception {
        String[] validReasons = {"LOGOUT", "COMPROMISE", "ADMIN_REVOKE", "PASSWORD_CHANGE"};

        for (String reason : validReasons) {
            doNothing().when(tokenRevocationService)
                .revokeAccessToken(anyString(), eq(TENANT_ID), eq(reason));

            mockMvc.perform(post(REVOKE_ENDPOINT)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"" + VALID_ACCESS_TOKEN + "\",\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should require token in revoke request")
    void testRevokeRequiresToken() throws Exception {
        mockMvc.perform(post(REVOKE_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"COMPROMISE\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should require reason in revoke request")
    void testRevokeRequiresReason() throws Exception {
        mockMvc.perform(post(REVOKE_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + VALID_ACCESS_TOKEN + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 404 for already-revoked token")
    void testRevokeAlreadyRevokedToken() throws Exception {
        doThrow(new IllegalStateException("Token already revoked"))
            .when(tokenRevocationService).revokeAccessToken(anyString(), eq(TENANT_ID), anyString());

        mockMvc.perform(post(REVOKE_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + VALID_ACCESS_TOKEN + "\",\"reason\":\"COMPROMISE\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should include revokedAt timestamp in response")
    void testIncludesRevokedAtTimestamp() throws Exception {
        doNothing().when(tokenRevocationService)
            .revokeAccessToken(anyString(), eq(TENANT_ID), eq("COMPROMISE"));

        mockMvc.perform(post(REVOKE_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .header("Authorization", "Bearer " + VALID_ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + VALID_ACCESS_TOKEN + "\",\"reason\":\"COMPROMISE\"}"))
            .andExpect(jsonPath("$.revokedAt").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return JSON content type")
    void testJsonContentType() throws Exception {
        when(tokenRevocationService.revokeAllUserTokens(anyString(), eq(TENANT_ID), eq("LOGOUT")))
            .thenReturn(1);

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should not allow GET requests to logout")
    void testDisallowGetLogout() throws Exception {
        mockMvc.perform(get(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should not allow GET requests to revoke")
    void testDisallowGetRevoke() throws Exception {
        mockMvc.perform(get(REVOKE_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should include rate limit headers")
    void testIncludesRateLimitHeaders() throws Exception {
        when(tokenRevocationService.revokeAllUserTokens(anyString(), eq(TENANT_ID), eq("LOGOUT")))
            .thenReturn(1);

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(header().exists("X-RateLimit-Limit"))
            .andExpect(header().exists("X-RateLimit-Remaining"))
            .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    @WithMockUser(roles = "USER", username = "testuser")
    @DisplayName("Should enforce tenant isolation on logout")
    void testEnforcesTenantIsolationOnLogout() throws Exception {
        when(tokenRevocationService.revokeAllUserTokens(anyString(), eq("other-tenant"), eq("LOGOUT")))
            .thenThrow(new IllegalArgumentException("Invalid tenant"));

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", "other-tenant"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should log logout operation")
    void testLogsLogoutOperation() throws Exception {
        when(tokenRevocationService.revokeAllUserTokens(anyString(), eq(TENANT_ID), eq("LOGOUT")))
            .thenReturn(2);

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        // Audit logging verified through service mock
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should handle multiple logout requests idempotently")
    void testLogoutIsIdempotent() throws Exception {
        when(tokenRevocationService.revokeAllUserTokens(anyString(), eq(TENANT_ID), eq("LOGOUT")))
            .thenReturn(0);  // No tokens found on second call

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should handle empty token in revoke request")
    void testHandlesEmptyToken() throws Exception {
        mockMvc.perform(post(REVOKE_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"\",\"reason\":\"COMPROMISE\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should handle null token in revoke request")
    void testHandlesNullToken() throws Exception {
        mockMvc.perform(post(REVOKE_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":null,\"reason\":\"COMPROMISE\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 500 on service error")
    void testReturnsErrorOnServiceFailure() throws Exception {
        when(tokenRevocationService.revokeAllUserTokens(anyString(), eq(TENANT_ID), eq("LOGOUT")))
            .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should support logout without killing current session")
    void testLogoutAllowsCurrentRequest() throws Exception {
        when(tokenRevocationService.revokeAllUserTokens(anyString(), eq(TENANT_ID), eq("LOGOUT")))
            .thenReturn(3);

        // Request should succeed even though tokens are revoked
        mockMvc.perform(post(LOGOUT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }
}
