package com.healthdata.sales.service;

import com.healthdata.sales.config.LinkedInConfig;
import com.healthdata.sales.entity.LinkedInToken;
import com.healthdata.sales.entity.LinkedInToken.TokenStatus;
import com.healthdata.sales.repository.LinkedInTokenRepository;
import com.healthdata.sales.service.LinkedInOAuthService.AuthorizationUrlResponse;
import com.healthdata.sales.service.LinkedInOAuthService.ConnectionStatus;
import com.healthdata.sales.service.LinkedInOAuthService.LinkedInTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LinkedInOAuthService.
 *
 * Tests cover:
 * - Authorization URL generation
 * - Code exchange for tokens
 * - Token refresh
 * - Disconnection (token revocation)
 * - Connection status checks
 * - Valid access token retrieval
 * - Scheduled token refresh
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LinkedInOAuthService Unit Tests")
class LinkedInOAuthServiceTest {

    @Mock
    private LinkedInConfig linkedInConfig;

    @Mock
    private LinkedInTokenRepository tokenRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LinkedInOAuthService oauthService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String REDIRECT_URI = "https://app.hdim.health/oauth/linkedin/callback";
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";

    private LinkedInConfig.OAuth oauthConfig;
    private LinkedInConfig.Api apiConfig;
    private LinkedInToken testToken;

    @BeforeEach
    void setUp() {
        oauthConfig = new LinkedInConfig.OAuth();
        oauthConfig.setClientId(CLIENT_ID);
        oauthConfig.setClientSecret(CLIENT_SECRET);
        oauthConfig.setRedirectUri(REDIRECT_URI);
        oauthConfig.setScope("r_liteprofile r_emailaddress");

        apiConfig = new LinkedInConfig.Api();
        apiConfig.setEnabled(true);
        apiConfig.setBaseUrl("https://api.linkedin.com/v2");
        apiConfig.setAuthUrl("https://www.linkedin.com/oauth/v2");

        testToken = LinkedInToken.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .accessToken("test-access-token")
            .refreshToken("test-refresh-token")
            .expiresAt(Instant.now().plusSeconds(3600))
            .status(TokenStatus.ACTIVE)
            .displayName("John Doe")
            .email("john@example.com")
            .linkedInMemberId("member123")
            .build();
    }

    // ==========================================
    // getAuthorizationUrl Tests
    // ==========================================

    @Nested
    @DisplayName("getAuthorizationUrl Tests")
    class GetAuthorizationUrlTests {

        @Test
        @DisplayName("should generate authorization URL with state")
        void shouldGenerateAuthorizationUrlWithState() {
            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);

            AuthorizationUrlResponse response = oauthService.getAuthorizationUrl(TENANT_ID, USER_ID, null);

            assertThat(response.authorizationUrl()).contains("https://www.linkedin.com/oauth/v2/authorization");
            assertThat(response.authorizationUrl()).contains("client_id=" + CLIENT_ID);
            assertThat(response.authorizationUrl()).contains("response_type=code");
            assertThat(response.authorizationUrl()).contains("redirect_uri=");
            assertThat(response.authorizationUrl()).contains("scope=");
            assertThat(response.state()).isNotEmpty();
        }

        @Test
        @DisplayName("should use custom redirect URI when provided")
        void shouldUseCustomRedirectUriWhenProvided() {
            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);
            String customRedirect = "https://custom.app/callback";

            AuthorizationUrlResponse response = oauthService.getAuthorizationUrl(TENANT_ID, USER_ID, customRedirect);

            // URL may be encoded or not depending on UriComponentsBuilder encoding
            assertThat(response.authorizationUrl()).containsAnyOf(
                customRedirect,
                customRedirect.replace("://", "%3A%2F%2F").replace("/", "%2F")
            );
        }

        @Test
        @DisplayName("should throw exception when API is disabled")
        void shouldThrowExceptionWhenApiDisabled() {
            apiConfig.setEnabled(false);
            when(linkedInConfig.getApi()).thenReturn(apiConfig);

            assertThatThrownBy(() -> oauthService.getAuthorizationUrl(TENANT_ID, USER_ID, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not enabled");
        }
    }

    // ==========================================
    // exchangeCodeForTokens Tests
    // ==========================================

    @Nested
    @DisplayName("exchangeCodeForTokens Tests")
    class ExchangeCodeForTokensTests {

        @Test
        @DisplayName("should throw exception for invalid state")
        void shouldThrowExceptionForInvalidState() {
            assertThatThrownBy(() -> oauthService.exchangeCodeForTokens("code", "invalid-state", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired state");
        }

        @Test
        @DisplayName("should exchange code and save token")
        void shouldExchangeCodeAndSaveToken() {
            // First generate a valid state
            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);

            AuthorizationUrlResponse authResponse = oauthService.getAuthorizationUrl(TENANT_ID, USER_ID, null);
            String validState = authResponse.state();

            // Mock token exchange response
            Map<String, Object> tokenResponse = Map.of(
                "access_token", "new-access-token",
                "refresh_token", "new-refresh-token",
                "expires_in", 5184000,
                "refresh_token_expires_in", 31536000,
                "scope", "r_liteprofile r_emailaddress"
            );
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

            // Mock profile fetch
            Map<String, Object> profileResponse = Map.of(
                "sub", "member123",
                "name", "John Doe",
                "email", "john@example.com"
            );
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(profileResponse, HttpStatus.OK));

            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.empty());
            when(tokenRepository.save(any(LinkedInToken.class))).thenAnswer(i -> i.getArgument(0));

            LinkedInTokenResponse result = oauthService.exchangeCodeForTokens("auth-code", validState, null);

            assertThat(result.success()).isTrue();
            assertThat(result.displayName()).isEqualTo("John Doe");
            assertThat(result.email()).isEqualTo("john@example.com");

            ArgumentCaptor<LinkedInToken> captor = ArgumentCaptor.forClass(LinkedInToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getAccessToken()).isEqualTo("new-access-token");
            assertThat(captor.getValue().getRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(captor.getValue().getStatus()).isEqualTo(TokenStatus.ACTIVE);
        }

        @Test
        @DisplayName("should update existing token")
        void shouldUpdateExistingToken() {
            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);

            AuthorizationUrlResponse authResponse = oauthService.getAuthorizationUrl(TENANT_ID, USER_ID, null);
            String validState = authResponse.state();

            Map<String, Object> tokenResponse = Map.of(
                "access_token", "updated-access-token",
                "expires_in", 3600
            );
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

            Map<String, Object> profileResponse = Map.of("sub", "member123", "name", "John");
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(profileResponse, HttpStatus.OK));

            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(testToken));
            when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            oauthService.exchangeCodeForTokens("code", validState, null);

            verify(tokenRepository).save(any(LinkedInToken.class));
        }

        @Test
        @DisplayName("should throw exception on API error")
        void shouldThrowExceptionOnApiError() {
            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);

            AuthorizationUrlResponse authResponse = oauthService.getAuthorizationUrl(TENANT_ID, USER_ID, null);
            String validState = authResponse.state();

            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RestClientException("Connection failed"));

            assertThatThrownBy(() -> oauthService.exchangeCodeForTokens("code", validState, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to exchange");
        }
    }

    // ==========================================
    // refreshToken Tests
    // ==========================================

    @Nested
    @DisplayName("refreshToken Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(testToken));

            Map<String, Object> refreshResponse = Map.of(
                "access_token", "refreshed-access-token",
                "refresh_token", "new-refresh-token",
                "expires_in", 5184000
            );
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(refreshResponse, HttpStatus.OK));
            when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            boolean result = oauthService.refreshToken(TENANT_ID, USER_ID);

            assertThat(result).isTrue();
            verify(tokenRepository).save(any(LinkedInToken.class));
        }

        @Test
        @DisplayName("should throw exception when token not found")
        void shouldThrowExceptionWhenTokenNotFound() {
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oauthService.refreshToken(TENANT_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No LinkedIn token found");
        }

        @Test
        @DisplayName("should return false when no refresh token available")
        void shouldReturnFalseWhenNoRefreshToken() {
            testToken.setRefreshToken(null);
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(testToken));

            boolean result = oauthService.refreshToken(TENANT_ID, USER_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when refresh token expired")
        void shouldReturnFalseWhenRefreshTokenExpired() {
            testToken.setRefreshExpiresAt(Instant.now().minusSeconds(3600));
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(testToken));
            when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            boolean result = oauthService.refreshToken(TENANT_ID, USER_ID);

            assertThat(result).isFalse();
            verify(tokenRepository).save(any(LinkedInToken.class));
        }

        @Test
        @DisplayName("should handle API error during refresh")
        void shouldHandleApiErrorDuringRefresh() {
            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(testToken));
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RestClientException("Network error"));
            when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            boolean result = oauthService.refreshToken(TENANT_ID, USER_ID);

            assertThat(result).isFalse();
            ArgumentCaptor<LinkedInToken> captor = ArgumentCaptor.forClass(LinkedInToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getErrorMessage()).contains("Token refresh failed");
        }
    }

    // ==========================================
    // disconnect Tests
    // ==========================================

    @Nested
    @DisplayName("disconnect Tests")
    class DisconnectTests {

        @Test
        @DisplayName("should disconnect and mark token as revoked")
        void shouldDisconnectAndMarkTokenAsRevoked() {
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(testToken));
            when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            oauthService.disconnect(TENANT_ID, USER_ID);

            ArgumentCaptor<LinkedInToken> captor = ArgumentCaptor.forClass(LinkedInToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(TokenStatus.REVOKED);
        }

        @Test
        @DisplayName("should handle no token found gracefully")
        void shouldHandleNoTokenFoundGracefully() {
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

            oauthService.disconnect(TENANT_ID, USER_ID);

            verify(tokenRepository, never()).save(any());
        }
    }

    // ==========================================
    // getConnectionStatus Tests
    // ==========================================

    @Nested
    @DisplayName("getConnectionStatus Tests")
    class GetConnectionStatusTests {

        @Test
        @DisplayName("should return connected status for active token")
        void shouldReturnConnectedStatusForActiveToken() {
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(testToken));

            ConnectionStatus result = oauthService.getConnectionStatus(TENANT_ID, USER_ID);

            assertThat(result.connected()).isTrue();
            assertThat(result.displayName()).isEqualTo("John Doe");
            assertThat(result.email()).isEqualTo("john@example.com");
            assertThat(result.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should return disconnected status when no token")
        void shouldReturnDisconnectedStatusWhenNoToken() {
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

            ConnectionStatus result = oauthService.getConnectionStatus(TENANT_ID, USER_ID);

            assertThat(result.connected()).isFalse();
            assertThat(result.displayName()).isNull();
        }

        @Test
        @DisplayName("should return error status for errored token")
        void shouldReturnErrorStatusForErroredToken() {
            testToken.setStatus(TokenStatus.ERROR);
            testToken.setErrorMessage("Token expired");
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(testToken));

            ConnectionStatus result = oauthService.getConnectionStatus(TENANT_ID, USER_ID);

            assertThat(result.connected()).isFalse();
            assertThat(result.status()).isEqualTo("ERROR");
            assertThat(result.errorMessage()).isEqualTo("Token expired");
        }
    }

    // ==========================================
    // getValidAccessToken Tests
    // ==========================================

    @Nested
    @DisplayName("getValidAccessToken Tests")
    class GetValidAccessTokenTests {

        @Test
        @DisplayName("should return access token when valid")
        void shouldReturnAccessTokenWhenValid() {
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(testToken));
            when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Optional<String> result = oauthService.getValidAccessToken(TENANT_ID, USER_ID);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("test-access-token");
        }

        @Test
        @DisplayName("should return empty when no token")
        void shouldReturnEmptyWhenNoToken() {
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

            Optional<String> result = oauthService.getValidAccessToken(TENANT_ID, USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when token is inactive")
        void shouldReturnEmptyWhenTokenInactive() {
            testToken.setStatus(TokenStatus.REVOKED);
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(testToken));

            Optional<String> result = oauthService.getValidAccessToken(TENANT_ID, USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should refresh expired token automatically")
        void shouldRefreshExpiredTokenAutomatically() {
            testToken.setExpiresAt(Instant.now().minusSeconds(3600));
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(testToken));

            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);

            Map<String, Object> refreshResponse = Map.of(
                "access_token", "refreshed-token",
                "expires_in", 3600
            );
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(refreshResponse, HttpStatus.OK));

            LinkedInToken refreshedToken = LinkedInToken.builder()
                .id(testToken.getId())
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .accessToken("refreshed-token")
                .refreshToken("test-refresh-token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .status(TokenStatus.ACTIVE)
                .build();

            when(tokenRepository.save(any())).thenReturn(testToken);
            when(tokenRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(testToken))
                .thenReturn(Optional.of(refreshedToken));

            Optional<String> result = oauthService.getValidAccessToken(TENANT_ID, USER_ID);

            assertThat(result).isPresent();
        }
    }

    // ==========================================
    // refreshExpiringSoonTokens Tests
    // ==========================================

    @Nested
    @DisplayName("refreshExpiringSoonTokens Tests")
    class RefreshExpiringSoonTokensTests {

        @Test
        @DisplayName("should refresh tokens expiring soon")
        void shouldRefreshTokensExpiringSoon() {
            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);
            when(tokenRepository.findTokensExpiringSoon(any())).thenReturn(List.of(testToken));
            when(tokenRepository.findByTenantIdAndUserId(any(), any())).thenReturn(Optional.of(testToken));

            Map<String, Object> refreshResponse = Map.of("access_token", "refreshed", "expires_in", 3600);
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(refreshResponse, HttpStatus.OK));
            when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            oauthService.refreshExpiringSoonTokens();

            verify(tokenRepository).findTokensExpiringSoon(any());
            verify(tokenRepository, atLeastOnce()).save(any());
        }

        @Test
        @DisplayName("should skip refresh when API disabled")
        void shouldSkipRefreshWhenApiDisabled() {
            apiConfig.setEnabled(false);
            when(linkedInConfig.getApi()).thenReturn(apiConfig);

            oauthService.refreshExpiringSoonTokens();

            verify(tokenRepository, never()).findTokensExpiringSoon(any());
        }

        @Test
        @DisplayName("should handle individual token refresh errors")
        void shouldHandleIndividualTokenRefreshErrors() {
            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);
            when(tokenRepository.findTokensExpiringSoon(any())).thenReturn(List.of(testToken));
            when(tokenRepository.findByTenantIdAndUserId(any(), any())).thenReturn(Optional.of(testToken));
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RestClientException("Network error"));
            when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Should not throw exception
            oauthService.refreshExpiringSoonTokens();

            verify(tokenRepository).findTokensExpiringSoon(any());
        }
    }

    // ==========================================
    // Multi-Tenant Isolation Tests
    // ==========================================

    @Nested
    @DisplayName("Multi-Tenant Isolation Tests")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("should isolate tokens by tenant")
        void shouldIsolateTokensByTenant() {
            UUID otherTenantId = UUID.randomUUID();
            when(tokenRepository.findByTenantIdAndUserId(otherTenantId, USER_ID)).thenReturn(Optional.empty());

            ConnectionStatus result = oauthService.getConnectionStatus(otherTenantId, USER_ID);

            assertThat(result.connected()).isFalse();
            verify(tokenRepository).findByTenantIdAndUserId(otherTenantId, USER_ID);
            verify(tokenRepository, never()).findByTenantIdAndUserId(TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should generate unique state per authorization request")
        void shouldGenerateUniqueStatePerAuthorizationRequest() {
            when(linkedInConfig.getApi()).thenReturn(apiConfig);
            when(linkedInConfig.getOauth()).thenReturn(oauthConfig);

            AuthorizationUrlResponse response1 = oauthService.getAuthorizationUrl(TENANT_ID, USER_ID, null);
            AuthorizationUrlResponse response2 = oauthService.getAuthorizationUrl(TENANT_ID, USER_ID, null);

            assertThat(response1.state()).isNotEqualTo(response2.state());
        }
    }
}
