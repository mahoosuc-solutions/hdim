package com.healthdata.authentication.controller;

import com.healthdata.authentication.config.SmartOnFhirConfig;
import com.healthdata.authentication.service.SmartAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for SMART on FHIR authorization endpoints.
 *
 * SMART on FHIR (Substitutable Medical Applications and Reusable Technologies)
 * enables third-party applications to securely access EHR data.
 *
 * Endpoints:
 * - GET /.well-known/smart-configuration - SMART configuration discovery
 * - GET /fhir/.well-known/smart-configuration - Alternative SMART discovery path
 * - POST /oauth2/authorize - Authorization endpoint
 * - POST /oauth2/token - Token endpoint
 *
 * @see <a href="https://hl7.org/fhir/smart-app-launch/">SMART App Launch</a>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smart", name = "enabled", havingValue = "true")
public class SmartOnFhirController {

    private final SmartOnFhirConfig smartConfig;
    private final SmartAuthService smartAuthService;

    /**
     * SMART configuration discovery endpoint.
     * Returns SMART on FHIR capabilities and endpoints.
     *
     * @return SMART configuration JSON
     * @see <a href="https://hl7.org/fhir/smart-app-launch/conformance.html">SMART Conformance</a>
     */
    @GetMapping(
        value = "/.well-known/smart-configuration",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> getSmartConfiguration() {
        log.debug("SMART configuration requested");
        return ResponseEntity.ok(smartConfig.getSmartConfiguration());
    }

    /**
     * Alternative SMART configuration discovery endpoint under /fhir.
     * Some SMART clients look for configuration at the FHIR server's base URL.
     *
     * @return SMART configuration JSON
     */
    @GetMapping(
        value = "/fhir/.well-known/smart-configuration",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> getSmartConfigurationFhirPath() {
        log.debug("SMART configuration requested (FHIR path)");
        return ResponseEntity.ok(smartConfig.getSmartConfiguration());
    }

    /**
     * SMART authorization endpoint.
     * Initiates the authorization flow for SMART apps.
     *
     * @param responseType Expected to be "code"
     * @param clientId Client application ID
     * @param redirectUri Where to redirect after authorization
     * @param scope Space-separated scopes requested
     * @param state CSRF protection state
     * @param aud FHIR server URL (required for EHR launch)
     * @param launch Launch context token (for EHR launch)
     * @return Redirect to authorization page or error
     */
    @GetMapping(value = "/oauth2/authorize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> authorize(
        @RequestParam("response_type") String responseType,
        @RequestParam("client_id") String clientId,
        @RequestParam("redirect_uri") String redirectUri,
        @RequestParam(required = false) String scope,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String aud,
        @RequestParam(required = false) String launch
    ) {
        log.info("SMART authorization request: client_id={}, scope={}", clientId, scope);

        // Validate response type
        if (!"code".equals(responseType)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "unsupported_response_type",
                "error_description", "Only 'code' response type is supported"
            ));
        }

        // Validate scopes
        if (scope != null) {
            var validScopes = smartConfig.validateScopes(scope);
            log.debug("Valid scopes: {}", validScopes);
        }

        // For now, return a placeholder response
        // In production, this would redirect to a login/consent page
        return ResponseEntity.ok(Map.of(
            "message", "Authorization endpoint - redirect to login page",
            "client_id", clientId,
            "redirect_uri", redirectUri,
            "scope", scope != null ? scope : "",
            "state", state != null ? state : "",
            "launch", launch != null ? launch : ""
        ));
    }

    /**
     * SMART token endpoint.
     * Exchanges authorization code for access token.
     *
     * @param grantType Grant type (authorization_code, refresh_token, client_credentials)
     * @param code Authorization code (for authorization_code grant)
     * @param redirectUri Redirect URI used in authorization request
     * @param clientId Client application ID
     * @param clientSecret Client secret (for confidential clients)
     * @param refreshToken Refresh token (for refresh_token grant)
     * @param scope Scopes requested
     * @param codeVerifier PKCE code verifier
     * @return Token response
     */
    @PostMapping(
        value = "/oauth2/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> token(
        @RequestParam("grant_type") String grantType,
        @RequestParam(required = false) String code,
        @RequestParam(name = "redirect_uri", required = false) String redirectUri,
        @RequestParam(name = "client_id", required = false) String clientId,
        @RequestParam(name = "client_secret", required = false) String clientSecret,
        @RequestParam(name = "refresh_token", required = false) String refreshToken,
        @RequestParam(required = false) String scope,
        @RequestParam(name = "code_verifier", required = false) String codeVerifier
    ) {
        log.info("SMART token request: grant_type={}, client_id={}", grantType, clientId);

        // Validate grant type
        if (!smartConfig.getGrantTypes().contains(grantType)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "unsupported_grant_type",
                "error_description", "Grant type '" + grantType + "' is not supported"
            ));
        }

        // Handle different grant types
        return switch (grantType) {
            case "authorization_code" -> handleAuthorizationCodeGrant(
                code, redirectUri, clientId, clientSecret, codeVerifier
            );
            case "refresh_token" -> handleRefreshTokenGrant(
                refreshToken, clientId, clientSecret, scope
            );
            case "client_credentials" -> handleClientCredentialsGrant(
                clientId, clientSecret, scope
            );
            default -> ResponseEntity.badRequest().body(Map.of(
                "error", "unsupported_grant_type",
                "error_description", "Grant type '" + grantType + "' is not supported"
            ));
        };
    }

    /**
     * Handle authorization code grant.
     * Exchanges an authorization code for access and refresh tokens.
     */
    private ResponseEntity<Map<String, Object>> handleAuthorizationCodeGrant(
        String code,
        String redirectUri,
        String clientId,
        String clientSecret,
        String codeVerifier
    ) {
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_request",
                "error_description", "Authorization code is required"
            ));
        }

        // Exchange authorization code for tokens
        SmartAuthService.TokenResponse tokenResponse = smartAuthService.exchangeAuthorizationCode(
                code, clientId, clientSecret, redirectUri, codeVerifier);

        if (tokenResponse == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_grant",
                "error_description", "Authorization code is invalid, expired, or has already been used"
            ));
        }

        // Build response per SMART on FHIR spec
        var response = new java.util.LinkedHashMap<String, Object>();
        response.put("access_token", tokenResponse.getAccessToken());
        response.put("token_type", tokenResponse.getTokenType());
        response.put("expires_in", tokenResponse.getExpiresIn());
        response.put("scope", tokenResponse.getScope());

        if (tokenResponse.getRefreshToken() != null) {
            response.put("refresh_token", tokenResponse.getRefreshToken());
        }

        // Include patient context if present (for launch/patient scope)
        if (tokenResponse.getPatientId() != null) {
            response.put("patient", tokenResponse.getPatientId());
        }

        log.info("Authorization code exchanged successfully for client: {}", clientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle refresh token grant.
     * Issues new access and refresh tokens using a valid refresh token.
     */
    private ResponseEntity<Map<String, Object>> handleRefreshTokenGrant(
        String refreshToken,
        String clientId,
        String clientSecret,
        String scope
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_request",
                "error_description", "Refresh token is required"
            ));
        }

        // Refresh access token using refresh token
        SmartAuthService.TokenResponse tokenResponse = smartAuthService.refreshAccessToken(
                refreshToken, clientId, clientSecret, scope);

        if (tokenResponse == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_grant",
                "error_description", "Refresh token is invalid or expired"
            ));
        }

        // Build response
        var response = new java.util.LinkedHashMap<String, Object>();
        response.put("access_token", tokenResponse.getAccessToken());
        response.put("token_type", tokenResponse.getTokenType());
        response.put("expires_in", tokenResponse.getExpiresIn());
        response.put("scope", tokenResponse.getScope());

        if (tokenResponse.getRefreshToken() != null) {
            response.put("refresh_token", tokenResponse.getRefreshToken());
        }

        log.info("Token refreshed successfully for client: {}", clientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle client credentials grant (backend services).
     * Issues tokens for backend service-to-service authentication without user context.
     */
    private ResponseEntity<Map<String, Object>> handleClientCredentialsGrant(
        String clientId,
        String clientSecret,
        String scope
    ) {
        if (clientId == null || clientSecret == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_client",
                "error_description", "Client credentials are required"
            ));
        }

        // Issue client credentials token
        SmartAuthService.TokenResponse tokenResponse = smartAuthService.issueClientCredentialsToken(
                clientId, clientSecret, scope);

        if (tokenResponse == null) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "invalid_client",
                "error_description", "Client authentication failed"
            ));
        }

        // Build response (no refresh token for client credentials)
        var response = new java.util.LinkedHashMap<String, Object>();
        response.put("access_token", tokenResponse.getAccessToken());
        response.put("token_type", tokenResponse.getTokenType());
        response.put("expires_in", tokenResponse.getExpiresIn());
        response.put("scope", tokenResponse.getScope());

        log.info("Client credentials token issued for client: {}", clientId);
        return ResponseEntity.ok(response);
    }

    /**
     * JWKS endpoint for token validation.
     * Returns public keys for verifying JWT signatures.
     *
     * @return JWKS JSON
     */
    @GetMapping(
        value = "/.well-known/jwks.json",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> getJwks() {
        log.debug("JWKS requested");

        // Get JWKS from authentication service
        Map<String, Object> jwks = smartAuthService.getJwks();
        return ResponseEntity.ok(jwks);
    }

    /**
     * OpenID Connect userinfo endpoint.
     * Returns claims about the authenticated user.
     *
     * @return User info claims
     */
    @GetMapping(
        value = "/oauth2/userinfo",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> getUserInfo(
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        log.debug("UserInfo requested");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "invalid_token",
                "error_description", "Bearer token required"
            ));
        }

        // Extract token from Authorization header
        String token = authorization.substring(7); // Remove "Bearer " prefix

        // Validate token and get user info
        Map<String, Object> userInfo = smartAuthService.validateTokenAndGetUserInfo(token);

        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "invalid_token",
                "error_description", "Token is invalid or expired"
            ));
        }

        log.debug("UserInfo returned for user: {}", userInfo.get("sub"));
        return ResponseEntity.ok(userInfo);
    }
}
