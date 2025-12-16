package com.healthdata.authentication.controller;

import com.healthdata.authentication.config.SmartOnFhirConfig;
import com.healthdata.authentication.service.SmartAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SmartOnFhirController.
 *
 * Tests the SMART on FHIR authorization controller endpoints including
 * configuration discovery, authorization, and token exchange.
 *
 * Uses standalone MockMvc setup without Spring Boot context to avoid
 * security auto-configuration issues.
 *
 * Coverage:
 * - GET /.well-known/smart-configuration endpoint
 * - GET /oauth2/authorize endpoint (various scenarios)
 * - POST /oauth2/token endpoint (different grant types)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SmartOnFhirController Unit Tests")
class SmartOnFhirControllerTest {

    @Mock
    private SmartOnFhirConfig smartConfig;

    @Mock
    private SmartAuthService smartAuthService;

    @InjectMocks
    private SmartOnFhirController controller;

    private MockMvc mockMvc;

    private Map<String, Object> mockSmartConfiguration;
    private Set<String> mockScopes;
    private Set<String> mockGrantTypes;

    @BeforeEach
    void setUp() {
        // Set up standalone MockMvc
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Set up mock scopes
        mockScopes = new HashSet<>(Arrays.asList(
            "launch",
            "launch/patient",
            "openid",
            "profile",
            "patient/*.read",
            "patient/*.write"
        ));

        // Set up mock grant types
        mockGrantTypes = new HashSet<>(Arrays.asList(
            "authorization_code",
            "refresh_token",
            "client_credentials"
        ));

        // Set up mock SMART configuration
        mockSmartConfiguration = new LinkedHashMap<>();
        mockSmartConfiguration.put("authorization_endpoint", "http://localhost:8081/fhir/oauth2/authorize");
        mockSmartConfiguration.put("token_endpoint", "http://localhost:8081/fhir/oauth2/token");
        mockSmartConfiguration.put("userinfo_endpoint", "http://localhost:8081/fhir/oauth2/userinfo");
        mockSmartConfiguration.put("jwks_uri", "http://localhost:8081/fhir/.well-known/jwks.json");
        mockSmartConfiguration.put("scopes_supported", mockScopes);
        mockSmartConfiguration.put("response_types_supported", new HashSet<>(Arrays.asList("code", "token")));
        mockSmartConfiguration.put("grant_types_supported", mockGrantTypes);
        mockSmartConfiguration.put("code_challenge_methods_supported", new HashSet<>(Arrays.asList("S256")));
        mockSmartConfiguration.put("capabilities", new HashSet<>(Arrays.asList(
            "launch-ehr",
            "launch-standalone",
            "client-public"
        )));

        // Configure default mock behavior (lenient for methods not used in all tests)
        org.mockito.Mockito.lenient().when(smartConfig.getSmartConfiguration()).thenReturn(mockSmartConfiguration);
        org.mockito.Mockito.lenient().when(smartConfig.getGrantTypes()).thenReturn(mockGrantTypes);
        org.mockito.Mockito.lenient().when(smartConfig.getAccessTokenLifetime()).thenReturn(3600);

        // Configure SmartAuthService mock responses
        SmartAuthService.TokenResponse defaultTokenResponse = SmartAuthService.TokenResponse.builder()
            .accessToken("mock-access-token-" + UUID.randomUUID())
            .tokenType("Bearer")
            .expiresIn(3600)
            .scope("launch openid")
            .refreshToken("mock-refresh-token")
            .patientId("patient-123")
            .build();

        org.mockito.Mockito.lenient().when(smartAuthService.exchangeAuthorizationCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(defaultTokenResponse);

        org.mockito.Mockito.lenient().when(smartAuthService.refreshAccessToken(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(defaultTokenResponse);

        org.mockito.Mockito.lenient().when(smartAuthService.issueClientCredentialsToken(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(defaultTokenResponse);

        org.mockito.Mockito.lenient().when(smartAuthService.validateTokenAndGetUserInfo(
            org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(Map.of(
            "sub", "user-123",
            "name", "Test User",
            "email", "test@example.com",
            "fhirUser", "Practitioner/123"
        ));

        org.mockito.Mockito.lenient().when(smartAuthService.getJwks())
            .thenReturn(Map.of("keys", List.of(Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", "key-1",
                "n", "mock-modulus",
                "e", "AQAB"
            ))));
    }

    // ==================== SMART CONFIGURATION ENDPOINT TESTS ====================

    @Test
    @DisplayName("Should return SMART configuration JSON at /.well-known/smart-configuration")
    void shouldReturnSmartConfigurationJson() throws Exception {
        mockMvc.perform(get("/.well-known/smart-configuration"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost:8081/fhir/oauth2/authorize"))
            .andExpect(jsonPath("$.token_endpoint").value("http://localhost:8081/fhir/oauth2/token"))
            .andExpect(jsonPath("$.userinfo_endpoint").value("http://localhost:8081/fhir/oauth2/userinfo"))
            .andExpect(jsonPath("$.jwks_uri").value("http://localhost:8081/fhir/.well-known/jwks.json"))
            .andExpect(jsonPath("$.scopes_supported").isArray())
            .andExpect(jsonPath("$.scopes_supported", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.response_types_supported").isArray())
            .andExpect(jsonPath("$.grant_types_supported").isArray())
            .andExpect(jsonPath("$.code_challenge_methods_supported").isArray())
            .andExpect(jsonPath("$.capabilities").isArray());
    }

    @Test
    @DisplayName("Should return SMART configuration JSON at /fhir/.well-known/smart-configuration")
    void shouldReturnSmartConfigurationJsonAtFhirPath() throws Exception {
        mockMvc.perform(get("/fhir/.well-known/smart-configuration"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost:8081/fhir/oauth2/authorize"))
            .andExpect(jsonPath("$.token_endpoint").value("http://localhost:8081/fhir/oauth2/token"));
    }

    @Test
    @DisplayName("Should return all required SMART configuration fields")
    void shouldReturnAllRequiredSmartConfigurationFields() throws Exception {
        mockMvc.perform(get("/.well-known/smart-configuration"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authorization_endpoint").exists())
            .andExpect(jsonPath("$.token_endpoint").exists())
            .andExpect(jsonPath("$.userinfo_endpoint").exists())
            .andExpect(jsonPath("$.jwks_uri").exists())
            .andExpect(jsonPath("$.scopes_supported").exists())
            .andExpect(jsonPath("$.response_types_supported").exists())
            .andExpect(jsonPath("$.grant_types_supported").exists())
            .andExpect(jsonPath("$.capabilities").exists());
    }

    // ==================== AUTHORIZE ENDPOINT TESTS ====================

    @Test
    @DisplayName("Should accept valid authorization request with required parameters")
    void shouldAcceptValidAuthorizationRequest() throws Exception {
        // Mock scope validation
        Set<String> validScopes = new HashSet<>(Arrays.asList("launch", "openid", "patient/Observation.read"));
        when(smartConfig.validateScopes("launch openid patient/Observation.read")).thenReturn(validScopes);

        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client-123")
                .param("redirect_uri", "https://app.example.com/callback")
                .param("scope", "launch openid patient/Observation.read")
                .param("state", "abc123")
                .param("aud", "http://localhost:8081/fhir"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.client_id").value("test-client-123"))
            .andExpect(jsonPath("$.redirect_uri").value("https://app.example.com/callback"))
            .andExpect(jsonPath("$.scope").value("launch openid patient/Observation.read"))
            .andExpect(jsonPath("$.state").value("abc123"));
    }

    @Test
    @DisplayName("Should reject authorization request with invalid response_type")
    void shouldRejectAuthorizationRequestWithInvalidResponseType() throws Exception {
        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "invalid")
                .param("client_id", "test-client-123")
                .param("redirect_uri", "https://app.example.com/callback"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.error").value("unsupported_response_type"))
            .andExpect(jsonPath("$.error_description").value("Only 'code' response type is supported"));
    }

    @Test
    @DisplayName("Should reject authorization request with token response_type")
    void shouldRejectAuthorizationRequestWithTokenResponseType() throws Exception {
        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "token")
                .param("client_id", "test-client-123")
                .param("redirect_uri", "https://app.example.com/callback"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("unsupported_response_type"));
    }

    @Test
    @DisplayName("Should accept authorization request without optional parameters")
    void shouldAcceptAuthorizationRequestWithoutOptionalParameters() throws Exception {
        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client-123")
                .param("redirect_uri", "https://app.example.com/callback"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.client_id").value("test-client-123"))
            .andExpect(jsonPath("$.scope").value(""))
            .andExpect(jsonPath("$.state").value(""))
            .andExpect(jsonPath("$.launch").value(""));
    }

    @Test
    @DisplayName("Should validate scopes in authorization request")
    void shouldValidateScopesInAuthorizationRequest() throws Exception {
        Set<String> validScopes = new HashSet<>(Arrays.asList("launch", "openid"));
        when(smartConfig.validateScopes("launch openid invalid-scope")).thenReturn(validScopes);

        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client-123")
                .param("redirect_uri", "https://app.example.com/callback")
                .param("scope", "launch openid invalid-scope"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle authorization request with EHR launch context")
    void shouldHandleAuthorizationRequestWithEhrLaunchContext() throws Exception {
        Set<String> validScopes = new HashSet<>(Arrays.asList("launch", "launch/patient", "openid"));
        when(smartConfig.validateScopes("launch launch/patient openid")).thenReturn(validScopes);

        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "ehr-client")
                .param("redirect_uri", "https://ehr-app.example.com/callback")
                .param("scope", "launch launch/patient openid")
                .param("state", "xyz789")
                .param("aud", "http://localhost:8081/fhir")
                .param("launch", "launch-token-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.client_id").value("ehr-client"))
            .andExpect(jsonPath("$.launch").value("launch-token-123"));
    }

    @Test
    @DisplayName("Should handle authorization request with PKCE challenge")
    void shouldHandleAuthorizationRequestWithPkceChallenge() throws Exception {
        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "public-client")
                .param("redirect_uri", "https://app.example.com/callback")
                .param("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                .param("code_challenge_method", "S256"))
            .andExpect(status().isOk());
    }

    // ==================== TOKEN ENDPOINT TESTS ====================

    @Test
    @DisplayName("Should exchange authorization code for access token")
    void shouldExchangeAuthorizationCodeForAccessToken() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "authorization_code")
                .param("code", "auth-code-123")
                .param("redirect_uri", "https://app.example.com/callback")
                .param("client_id", "test-client"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.expires_in").value(3600))
            .andExpect(jsonPath("$.scope").exists());
    }

    @Test
    @DisplayName("Should include patient context in token response")
    void shouldIncludePatientContextInTokenResponse() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "authorization_code")
                .param("code", "auth-code-with-patient")
                .param("redirect_uri", "https://app.example.com/callback")
                .param("client_id", "test-client"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patient").exists())
            .andExpect(jsonPath("$.patient").isString());
    }

    @Test
    @DisplayName("Should reject token request with missing authorization code")
    void shouldRejectTokenRequestWithMissingAuthorizationCode() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "authorization_code")
                .param("redirect_uri", "https://app.example.com/callback")
                .param("client_id", "test-client"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"))
            .andExpect(jsonPath("$.error_description").value("Authorization code is required"));
    }

    @Test
    @DisplayName("Should reject token request with blank authorization code")
    void shouldRejectTokenRequestWithBlankAuthorizationCode() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "authorization_code")
                .param("code", "")
                .param("redirect_uri", "https://app.example.com/callback")
                .param("client_id", "test-client"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"))
            .andExpect(jsonPath("$.error_description").value("Authorization code is required"));
    }

    @Test
    @DisplayName("Should reject unsupported grant type")
    void shouldRejectUnsupportedGrantType() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "password")
                .param("username", "user")
                .param("password", "pass"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.error").value("unsupported_grant_type"))
            .andExpect(jsonPath("$.error_description").value("Grant type 'password' is not supported"));
    }

    @Test
    @DisplayName("Should reject implicit grant type")
    void shouldRejectImplicitGrantType() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "implicit"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("unsupported_grant_type"))
            .andExpect(jsonPath("$.error_description").value("Grant type 'implicit' is not supported"));
    }

    @Test
    @DisplayName("Should handle refresh token grant")
    void shouldHandleRefreshTokenGrant() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "refresh_token")
                .param("refresh_token", "refresh-token-123")
                .param("client_id", "test-client"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.expires_in").value(3600));
    }

    @Test
    @DisplayName("Should reject refresh token grant with missing refresh token")
    void shouldRejectRefreshTokenGrantWithMissingRefreshToken() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "refresh_token")
                .param("client_id", "test-client"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"))
            .andExpect(jsonPath("$.error_description").value("Refresh token is required"));
    }

    @Test
    @DisplayName("Should handle client credentials grant")
    void shouldHandleClientCredentialsGrant() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "client_credentials")
                .param("client_id", "backend-service")
                .param("client_secret", "secret-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.expires_in").value(3600))
            .andExpect(jsonPath("$.scope").exists());
    }

    @Test
    @DisplayName("Should reject client credentials grant without client ID")
    void shouldRejectClientCredentialsGrantWithoutClientId() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "client_credentials")
                .param("client_secret", "secret-123"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_client"))
            .andExpect(jsonPath("$.error_description").value("Client credentials are required"));
    }

    @Test
    @DisplayName("Should reject client credentials grant without client secret")
    void shouldRejectClientCredentialsGrantWithoutClientSecret() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "client_credentials")
                .param("client_id", "backend-service"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_client"))
            .andExpect(jsonPath("$.error_description").value("Client credentials are required"));
    }

    @Test
    @DisplayName("Should handle token request with PKCE code verifier")
    void shouldHandleTokenRequestWithPkceCodeVerifier() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "authorization_code")
                .param("code", "auth-code-with-pkce")
                .param("redirect_uri", "https://app.example.com/callback")
                .param("client_id", "public-client")
                .param("code_verifier", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    @DisplayName("Should handle token request with scope parameter")
    void shouldHandleTokenRequestWithScopeParameter() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "refresh_token")
                .param("refresh_token", "refresh-token-123")
                .param("client_id", "test-client")
                .param("scope", "launch openid"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return correct token lifetime")
    void shouldReturnCorrectTokenLifetime() throws Exception {
        // Setup specific mock with 7200 lifetime
        SmartAuthService.TokenResponse customTokenResponse = SmartAuthService.TokenResponse.builder()
            .accessToken("mock-access-token-" + UUID.randomUUID())
            .tokenType("Bearer")
            .expiresIn(7200)
            .scope("launch openid")
            .refreshToken("mock-refresh-token")
            .patientId("patient-123")
            .build();

        when(smartAuthService.exchangeAuthorizationCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(customTokenResponse);

        when(smartConfig.getAccessTokenLifetime()).thenReturn(7200);

        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "authorization_code")
                .param("code", "auth-code")
                .param("client_id", "test-client"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expires_in").value(7200));
    }

    // ==================== JWKS ENDPOINT TESTS ====================

    @Test
    @DisplayName("Should return JWKS JSON at /.well-known/jwks.json")
    void shouldReturnJwksJson() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.keys").isArray())
            .andExpect(jsonPath("$.keys[0].kty").exists())
            .andExpect(jsonPath("$.keys[0].use").exists())
            .andExpect(jsonPath("$.keys[0].alg").exists());
    }

    @Test
    @DisplayName("Should return JWKS with RSA key information")
    void shouldReturnJwksWithRsaKeyInformation() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
            .andExpect(jsonPath("$.keys[0].use").value("sig"))
            .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
            .andExpect(jsonPath("$.keys[0].kid").exists())
            .andExpect(jsonPath("$.keys[0].n").exists())
            .andExpect(jsonPath("$.keys[0].e").value("AQAB"));
    }

    // ==================== USERINFO ENDPOINT TESTS ====================

    @Test
    @DisplayName("Should return user info with valid Bearer token")
    void shouldReturnUserInfoWithValidBearerToken() throws Exception {
        mockMvc.perform(get("/oauth2/userinfo")
                .header("Authorization", "Bearer valid-access-token"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.sub").exists())
            .andExpect(jsonPath("$.name").exists())
            .andExpect(jsonPath("$.email").exists())
            .andExpect(jsonPath("$.fhirUser").exists());
    }

    @Test
    @DisplayName("Should reject userinfo request without Authorization header")
    void shouldRejectUserinfoRequestWithoutAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/oauth2/userinfo"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_token"))
            .andExpect(jsonPath("$.error_description").value("Bearer token required"));
    }

    @Test
    @DisplayName("Should reject userinfo request with invalid Authorization header")
    void shouldRejectUserinfoRequestWithInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/oauth2/userinfo")
                .header("Authorization", "Basic invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_token"))
            .andExpect(jsonPath("$.error_description").value("Bearer token required"));
    }

    @Test
    @DisplayName("Should reject userinfo request with missing Bearer prefix")
    void shouldRejectUserinfoRequestWithMissingBearerPrefix() throws Exception {
        mockMvc.perform(get("/oauth2/userinfo")
                .header("Authorization", "invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_token"));
    }

    @Test
    @DisplayName("Should return user info with fhirUser claim")
    void shouldReturnUserInfoWithFhirUserClaim() throws Exception {
        mockMvc.perform(get("/oauth2/userinfo")
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fhirUser").value(startsWith("Practitioner/")));
    }

    // ==================== EDGE CASE AND ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("Should handle authorization request with very long state parameter")
    void shouldHandleAuthorizationRequestWithVeryLongStateParameter() throws Exception {
        String longState = "a".repeat(500);

        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "https://app.example.com/callback")
                .param("state", longState))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value(longState));
    }

    @Test
    @DisplayName("Should handle authorization request with special characters in parameters")
    void shouldHandleAuthorizationRequestWithSpecialCharactersInParameters() throws Exception {
        String specialCharsState = "state-with-special!@#$%^&*()_+-={}[]|:;<>?,./";

        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "https://app.example.com/callback")
                .param("state", specialCharsState))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle token request with empty string parameters")
    void shouldHandleTokenRequestWithEmptyStringParameters() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "authorization_code")
                .param("code", "")
                .param("client_id", "test-client"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    @DisplayName("Should handle multiple simultaneous token requests")
    void shouldHandleMultipleSimultaneousTokenRequests() throws Exception {
        // Test that the endpoint can handle multiple requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/oauth2/token")
                    .contentType("application/x-www-form-urlencoded")
                    .param("grant_type", "authorization_code")
                    .param("code", "auth-code-" + i)
                    .param("client_id", "test-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
        }
    }

    @Test
    @DisplayName("Should return JSON response for all token endpoint errors")
    void shouldReturnJsonResponseForAllTokenEndpointErrors() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "invalid_grant"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.error_description").exists());
    }

    @Test
    @DisplayName("Should handle case-sensitive grant type validation")
    void shouldHandleCaseSensitiveGrantTypeValidation() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "AUTHORIZATION_CODE")
                .param("code", "auth-code-123")
                .param("client_id", "test-client"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
    }

    @Test
    @DisplayName("Should handle whitespace in grant type")
    void shouldHandleWhitespaceInGrantType() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", " authorization_code ")
                .param("code", "auth-code-123")
                .param("client_id", "test-client"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
    }

    // ==================== DIRECT CONTROLLER METHOD TESTS ====================

    @Test
    @DisplayName("Should return correct configuration directly from method")
    void shouldReturnCorrectConfigurationDirectlyFromMethod() {
        ResponseEntity<Map<String, Object>> response = controller.getSmartConfiguration();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKeys(
            "authorization_endpoint",
            "token_endpoint",
            "userinfo_endpoint",
            "jwks_uri"
        );
    }
}
