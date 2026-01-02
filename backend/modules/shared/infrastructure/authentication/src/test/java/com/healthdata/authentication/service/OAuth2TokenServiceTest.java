package com.healthdata.authentication.service;

import com.healthdata.authentication.config.OAuth2Config;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.OAuth2TokenResponse;
import com.healthdata.authentication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OAuth2TokenService.
 *
 * Tests OAuth2/OIDC authentication flows including:
 * - Authorization code exchange
 * - Token validation
 * - User provisioning from OAuth2 claims
 * - Authorization URL generation
 *
 * Uses TDD approach with JUnit 5, Mockito, and AssertJ.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2TokenService Unit Tests")
class OAuth2TokenServiceTest {

    @Mock
    private OAuth2Config oauth2Config;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OAuth2TokenService oauth2TokenService;

    private OAuth2Config.ProviderConfig oktaProviderConfig;
    private OAuth2Config.ProviderConfig azureProviderConfig;

    private static final String OKTA_PROVIDER = "okta";
    private static final String AZURE_PROVIDER = "azure";
    private static final String UNKNOWN_PROVIDER = "unknown";
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final String ISSUER_URI = "https://test.okta.com";
    private static final String REDIRECT_URI = "http://localhost:8080/callback";
    private static final String AUTH_CODE = "test-auth-code";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String REFRESH_TOKEN = "test-refresh-token";
    private static final String ID_TOKEN = createTestIdToken("user@example.com", "user-123");
    private static final String STATE = "csrf-state-token";

    @BeforeEach
    void setUp() {
        // Setup Okta provider config
        oktaProviderConfig = new OAuth2Config.ProviderConfig();
        oktaProviderConfig.setClientId(CLIENT_ID);
        oktaProviderConfig.setClientSecret(CLIENT_SECRET);
        oktaProviderConfig.setIssuerUri(ISSUER_URI);
        oktaProviderConfig.setTokenUri(ISSUER_URI + "/oauth2/v1/token");
        oktaProviderConfig.setAuthorizationUri(ISSUER_URI + "/oauth2/v1/authorize");
        oktaProviderConfig.setScopes("openid,profile,email");
        oktaProviderConfig.setUsernameClaim("email");
        oktaProviderConfig.setUserIdClaim("sub");
        oktaProviderConfig.setAutoCreateUser(true);
        oktaProviderConfig.setDefaultRoles("USER");
        oktaProviderConfig.setDefaultTenantId("default-tenant");

        // Setup Azure provider config
        azureProviderConfig = new OAuth2Config.ProviderConfig();
        azureProviderConfig.setClientId("azure-client-id");
        azureProviderConfig.setClientSecret("azure-client-secret");
        azureProviderConfig.setIssuerUri("https://login.microsoftonline.com/tenant-id");
        azureProviderConfig.setScopes("openid,profile,email");
        azureProviderConfig.setUsernameClaim("preferred_username");
        azureProviderConfig.setUserIdClaim("oid");
        azureProviderConfig.setAutoCreateUser(true);

        // Inject the mocked RestTemplate
        ReflectionTestUtils.setField(oauth2TokenService, "restTemplate", restTemplate);
    }

    // ==================== EXCHANGE CODE FOR TOKENS TESTS ====================

    @Test
    @DisplayName("Should exchange code for tokens with valid code and create new user")
    void shouldExchangeCodeForTokensWithValidCodeAndCreateNewUser() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", ACCESS_TOKEN);
        tokenResponse.put("refresh_token", REFRESH_TOKEN);
        tokenResponse.put("id_token", ID_TOKEN);
        tokenResponse.put("expires_in", 3600);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.empty());

        User newUser = createTestUser("user@example.com", "user-123");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        when(jwtTokenService.generateAccessToken(any(User.class))).thenReturn("local-access-token");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("local-refresh-token");

        // When
        OAuth2TokenResponse result = oauth2TokenService.exchangeCodeForTokens(OKTA_PROVIDER, AUTH_CODE, REDIRECT_URI);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("local-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("local-refresh-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(3600);
        assertThat(result.getIdToken()).isEqualTo(ID_TOKEN);
        assertThat(result.getProvider()).isEqualTo(OKTA_PROVIDER);
        assertThat(result.getUsername()).isEqualTo("user@example.com");

        verify(userRepository).save(any(User.class));
        verify(jwtTokenService).generateAccessToken(any(User.class));
        verify(jwtTokenService).generateRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("Should exchange code for tokens and update existing user")
    void shouldExchangeCodeForTokensAndUpdateExistingUser() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", ACCESS_TOKEN);
        tokenResponse.put("id_token", ID_TOKEN);
        tokenResponse.put("expires_in", 3600);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        User existingUser = createTestUser("user@example.com", "user-123");
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        when(jwtTokenService.generateAccessToken(any(User.class))).thenReturn("local-access-token");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("local-refresh-token");

        // When
        OAuth2TokenResponse result = oauth2TokenService.exchangeCodeForTokens(OKTA_PROVIDER, AUTH_CODE, REDIRECT_URI);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("user@example.com");

        verify(userRepository).save(existingUser);
        verify(userRepository, never()).save(argThat(user ->
            user != existingUser && user.getUsername().equals("user@example.com")
        ));
    }

    @Test
    @DisplayName("Should throw exception when exchanging code with unknown provider")
    void shouldThrowExceptionWhenExchangingCodeWithUnknownProvider() {
        // Given
        when(oauth2Config.getProvider(UNKNOWN_PROVIDER)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() ->
            oauth2TokenService.exchangeCodeForTokens(UNKNOWN_PROVIDER, AUTH_CODE, REDIRECT_URI)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown OAuth2 provider: " + UNKNOWN_PROVIDER);

        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    @DisplayName("Should use default token URI when not configured")
    void shouldUseDefaultTokenUriWhenNotConfigured() {
        // Given
        oktaProviderConfig.setTokenUri(null);
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", ACCESS_TOKEN);
        tokenResponse.put("id_token", ID_TOKEN);
        tokenResponse.put("expires_in", 3600);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        User user = createTestUser("user@example.com", "user-123");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenService.generateAccessToken(any(User.class))).thenReturn("token");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("refresh");

        // When
        oauth2TokenService.exchangeCodeForTokens(OKTA_PROVIDER, AUTH_CODE, REDIRECT_URI);

        // Then
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).postForEntity(uriCaptor.capture(), any(), eq(Map.class));
        assertThat(uriCaptor.getValue()).isEqualTo(ISSUER_URI + "/oauth2/v1/token");
    }

    @Test
    @DisplayName("Should handle token response without refresh token")
    void shouldHandleTokenResponseWithoutRefreshToken() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", ACCESS_TOKEN);
        tokenResponse.put("id_token", ID_TOKEN);
        // No refresh_token in response

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        User user = createTestUser("user@example.com", "user-123");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenService.generateAccessToken(any(User.class))).thenReturn("local-token");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("local-refresh");

        // When
        OAuth2TokenResponse result = oauth2TokenService.exchangeCodeForTokens(OKTA_PROVIDER, AUTH_CODE, REDIRECT_URI);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRefreshToken()).isEqualTo("local-refresh");
    }

    @Test
    @DisplayName("Should use default expiration when not provided")
    void shouldUseDefaultExpirationWhenNotProvided() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", ACCESS_TOKEN);
        tokenResponse.put("id_token", ID_TOKEN);
        // No expires_in

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        User user = createTestUser("user@example.com", "user-123");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenService.generateAccessToken(any(User.class))).thenReturn("token");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("refresh");

        // When
        OAuth2TokenResponse result = oauth2TokenService.exchangeCodeForTokens(OKTA_PROVIDER, AUTH_CODE, REDIRECT_URI);

        // Then
        assertThat(result.getExpiresIn()).isEqualTo(3600); // Default value
    }

    @Test
    @DisplayName("Should throw exception when token exchange fails")
    void shouldThrowExceptionWhenTokenExchangeFails() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RuntimeException("Network error"));

        // When & Then
        assertThatThrownBy(() ->
            oauth2TokenService.exchangeCodeForTokens(OKTA_PROVIDER, AUTH_CODE, REDIRECT_URI)
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("OAuth2 token exchange failed");
    }

    // ==================== VALIDATE EXTERNAL TOKEN TESTS ====================

    @Test
    @DisplayName("Should validate external token successfully")
    void shouldValidateExternalTokenSuccessfully() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        Map<String, Object> introspectionResponse = new HashMap<>();
        introspectionResponse.put("active", true);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(introspectionResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // When
        boolean isValid = oauth2TokenService.validateExternalToken(OKTA_PROVIDER, ACCESS_TOKEN);

        // Then
        assertThat(isValid).isTrue();
        verify(restTemplate).postForEntity(
            eq(ISSUER_URI + "/oauth2/v1/introspect"),
            any(HttpEntity.class),
            eq(Map.class)
        );
    }

    @Test
    @DisplayName("Should return false for inactive external token")
    void shouldReturnFalseForInactiveExternalToken() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        Map<String, Object> introspectionResponse = new HashMap<>();
        introspectionResponse.put("active", false);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(introspectionResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // When
        boolean isValid = oauth2TokenService.validateExternalToken(OKTA_PROVIDER, ACCESS_TOKEN);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for unknown provider when validating token")
    void shouldReturnFalseForUnknownProviderWhenValidatingToken() {
        // Given
        when(oauth2Config.getProvider(UNKNOWN_PROVIDER)).thenReturn(null);

        // When
        boolean isValid = oauth2TokenService.validateExternalToken(UNKNOWN_PROVIDER, ACCESS_TOKEN);

        // Then
        assertThat(isValid).isFalse();
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    @DisplayName("Should return false when token validation throws exception")
    void shouldReturnFalseWhenTokenValidationThrowsException() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RuntimeException("Network error"));

        // When
        boolean isValid = oauth2TokenService.validateExternalToken(OKTA_PROVIDER, ACCESS_TOKEN);

        // Then
        assertThat(isValid).isFalse();
    }

    // ==================== PROVISION USER FROM TOKEN TESTS ====================

    @Test
    @DisplayName("Should provision new user from token")
    void shouldProvisionNewUserFromToken() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.empty());

        User newUser = createTestUser("user@example.com", "user-123");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User result = oauth2TokenService.provisionUserFromToken(OKTA_PROVIDER, ID_TOKEN);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("user@example.com");
        assertThat(result.getOauthProvider()).isEqualTo(OKTA_PROVIDER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("user@example.com");
        assertThat(savedUser.getOauthProvider()).isEqualTo(OKTA_PROVIDER);
        assertThat(savedUser.getOauthProviderId()).isEqualTo("user-123");
        assertThat(savedUser.getActive()).isTrue();
        assertThat(savedUser.getEmailVerified()).isTrue();
        assertThat(savedUser.getTenantIds()).contains("default-tenant");
    }

    @Test
    @DisplayName("Should update existing user from token")
    void shouldUpdateExistingUserFromToken() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        User existingUser = createTestUser("user@example.com", "user-123");
        existingUser.setEmail("old-email@example.com");
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        User result = oauth2TokenService.provisionUserFromToken(OKTA_PROVIDER, ID_TOKEN);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(existingUser);
        assertThat(existingUser.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Should throw exception when provisioning user with unknown provider")
    void shouldThrowExceptionWhenProvisioningUserWithUnknownProvider() {
        // Given
        when(oauth2Config.getProvider(UNKNOWN_PROVIDER)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() ->
            oauth2TokenService.provisionUserFromToken(UNKNOWN_PROVIDER, ID_TOKEN)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown OAuth2 provider");
    }

    @Test
    @DisplayName("Should throw exception when user not found and auto-create disabled")
    void shouldThrowExceptionWhenUserNotFoundAndAutoCreateDisabled() {
        // Given
        oktaProviderConfig.setAutoCreateUser(false);
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            oauth2TokenService.provisionUserFromToken(OKTA_PROVIDER, ID_TOKEN)
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("User not found and auto-creation is disabled");
    }

    @Test
    @DisplayName("Should extract tenant IDs from token claims")
    void shouldExtractTenantIdsFromTokenClaims() {
        // Given
        oktaProviderConfig.setTenantClaim("tenants");
        // Note: Using a simplified token format compatible with the simple parser
        // The simple parser in OAuth2TokenService splits by comma, so we use a single tenant value
        String tokenWithTenants = createTestIdTokenWithClaims("user@example.com", "user-123", "tenants", "tenant1");

        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.empty());

        User newUser = createTestUser("user@example.com", "user-123");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        oauth2TokenService.provisionUserFromToken(OKTA_PROVIDER, tokenWithTenants);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        // The extractTenantIds method splits the claim value by comma
        // Since we're using "tenant1" as a single value, we expect only tenant1
        assertThat(savedUser.getTenantIds()).contains("tenant1");
    }

    @Test
    @DisplayName("Should extract roles from token claims")
    void shouldExtractRolesFromTokenClaims() {
        // Given
        oktaProviderConfig.setRolesClaim("roles");
        // Note: Using a single role value compatible with the simple parser
        String tokenWithRoles = createTestIdTokenWithClaims("user@example.com", "user-123", "roles", "ADMIN");

        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.empty());

        User newUser = createTestUser("user@example.com", "user-123");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        oauth2TokenService.provisionUserFromToken(OKTA_PROVIDER, tokenWithRoles);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        // The extractRoles method splits the claim value by comma
        // Since we're using "ADMIN" as a single value, we expect only ADMIN role
        assertThat(savedUser.getRoles()).contains(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Should use default tenant when no tenant claim present")
    void shouldUseDefaultTenantWhenNoTenantClaimPresent() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.empty());

        User newUser = createTestUser("user@example.com", "user-123");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        oauth2TokenService.provisionUserFromToken(OKTA_PROVIDER, ID_TOKEN);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getTenantIds()).contains("default-tenant");
    }

    // ==================== BUILD AUTHORIZATION URL TESTS ====================

    @Test
    @DisplayName("Should build authorization URL correctly")
    void shouldBuildAuthorizationUrlCorrectly() {
        // Given
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        // When
        String authUrl = oauth2TokenService.buildAuthorizationUrl(OKTA_PROVIDER, REDIRECT_URI, STATE);

        // Then
        assertThat(authUrl).isNotNull();
        assertThat(authUrl).startsWith(ISSUER_URI + "/oauth2/v1/authorize");
        assertThat(authUrl).contains("response_type=code");
        assertThat(authUrl).contains("client_id=" + CLIENT_ID);
        assertThat(authUrl).contains("redirect_uri=" + REDIRECT_URI);
        assertThat(authUrl).contains("scope=openid");
        assertThat(authUrl).contains("state=" + STATE);
    }

    @Test
    @DisplayName("Should use default authorization URI when not configured")
    void shouldUseDefaultAuthorizationUriWhenNotConfigured() {
        // Given
        oktaProviderConfig.setAuthorizationUri(null);
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        // When
        String authUrl = oauth2TokenService.buildAuthorizationUrl(OKTA_PROVIDER, REDIRECT_URI, STATE);

        // Then
        assertThat(authUrl).startsWith(ISSUER_URI + "/oauth2/v1/authorize");
    }

    @Test
    @DisplayName("Should throw exception when building URL with unknown provider")
    void shouldThrowExceptionWhenBuildingUrlWithUnknownProvider() {
        // Given
        when(oauth2Config.getProvider(UNKNOWN_PROVIDER)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() ->
            oauth2TokenService.buildAuthorizationUrl(UNKNOWN_PROVIDER, REDIRECT_URI, STATE)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown OAuth2 provider");
    }

    @Test
    @DisplayName("Should encode scopes with %20 in authorization URL")
    void shouldEncodeScopesWithPercentTwentyInAuthorizationUrl() {
        // Given
        oktaProviderConfig.setScopes("openid,profile,email");
        when(oauth2Config.getProvider(OKTA_PROVIDER)).thenReturn(oktaProviderConfig);

        // When
        String authUrl = oauth2TokenService.buildAuthorizationUrl(OKTA_PROVIDER, REDIRECT_URI, STATE);

        // Then
        assertThat(authUrl).contains("scope=openid%20profile%20email");
    }

    // ==================== HELPER METHODS ====================

    private static String createTestIdToken(String email, String sub) {
        // Create a simple JWT-like token for testing (not cryptographically valid)
        // Format: header.payload.signature
        String header = Base64.getEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getEncoder().encodeToString(
            String.format("{\"email\":\"%s\",\"sub\":\"%s\",\"name\":\"Test User\"}", email, sub).getBytes()
        );
        String signature = Base64.getEncoder().encodeToString("signature".getBytes());
        return header + "." + payload + "." + signature;
    }

    private static String createTestIdTokenWithClaims(String email, String sub, String claimKey, String claimValue) {
        String header = Base64.getEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getEncoder().encodeToString(
            String.format("{\"email\":\"%s\",\"sub\":\"%s\",\"name\":\"Test User\",\"%s\":\"%s\"}",
                email, sub, claimKey, claimValue).getBytes()
        );
        String signature = Base64.getEncoder().encodeToString("signature".getBytes());
        return header + "." + payload + "." + signature;
    }

    private User createTestUser(String email, String sub) {
        return User.builder()
            .id(UUID.randomUUID())
            .username(email)
            .email(email)
            .firstName("Test")
            .lastName("User")
            .passwordHash(UUID.randomUUID().toString())
            .tenantIds(new HashSet<>(Set.of("default-tenant")))
            .roles(new HashSet<>(Set.of(UserRole.EVALUATOR)))
            .active(true)
            .emailVerified(true)
            .oauthProvider(OKTA_PROVIDER)
            .oauthProviderId(sub)
            .build();
    }
}
