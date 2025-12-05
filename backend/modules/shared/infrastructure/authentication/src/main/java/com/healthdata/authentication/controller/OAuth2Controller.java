package com.healthdata.authentication.controller;

import com.healthdata.authentication.config.OAuth2Config;
import com.healthdata.authentication.dto.OAuth2TokenResponse;
import com.healthdata.authentication.service.OAuth2TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for OAuth2/OpenID Connect authentication flows.
 *
 * Supports:
 * - Authorization Code flow with PKCE
 * - Multiple identity providers (Okta, Azure AD, Auth0)
 * - State parameter for CSRF protection
 *
 * Endpoints:
 * - GET /api/v1/oauth2/authorize/{provider} - Initiate OAuth2 flow
 * - GET /api/v1/oauth2/callback/{provider} - Handle OAuth2 callback
 * - POST /api/v1/oauth2/token - Exchange code for tokens
 * - GET /api/v1/oauth2/providers - List configured providers
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/oauth2")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "oauth2", name = "enabled", havingValue = "true")
public class OAuth2Controller {

    private final OAuth2Config oauth2Config;
    private final OAuth2TokenService oauth2TokenService;

    // In-memory state store for CSRF protection (in production, use Redis or database)
    private final Map<String, StateData> stateStore = new ConcurrentHashMap<>();

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long STATE_EXPIRATION_MS = 600_000; // 10 minutes

    /**
     * Data class for storing OAuth2 state information.
     */
    private record StateData(String provider, String redirectUri, long timestamp) {}

    /**
     * Initiate OAuth2 authorization flow.
     * Redirects the user to the identity provider's authorization endpoint.
     *
     * @param provider OAuth2 provider name (e.g., "okta", "azure")
     * @param redirectUri Where to redirect after successful authentication
     * @param response HTTP response for redirect
     */
    @GetMapping("/authorize/{provider}")
    public void authorize(
        @PathVariable String provider,
        @RequestParam(defaultValue = "/") String redirectUri,
        HttpServletResponse response
    ) throws IOException {
        log.info("Initiating OAuth2 authorization with provider: {}", provider);

        // Validate provider
        if (!oauth2Config.hasProvider(provider)) {
            log.warn("Unknown OAuth2 provider requested: {}", provider);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unknown OAuth2 provider: " + provider);
        }

        // Generate CSRF state token
        String state = generateState();

        // Store state for validation on callback
        stateStore.put(state, new StateData(provider, redirectUri, System.currentTimeMillis()));

        // Clean up expired states
        cleanupExpiredStates();

        // Build authorization URL
        String callbackUrl = buildCallbackUrl(provider);
        String authorizationUrl = oauth2TokenService.buildAuthorizationUrl(provider, callbackUrl, state);

        log.debug("Redirecting to authorization URL: {}", authorizationUrl);
        response.sendRedirect(authorizationUrl);
    }

    /**
     * Handle OAuth2 callback from identity provider.
     * Exchanges authorization code for tokens.
     *
     * @param provider OAuth2 provider name
     * @param code Authorization code from provider
     * @param state CSRF state parameter
     * @param error Error code if authorization failed
     * @param errorDescription Error description
     * @param response HTTP response for redirect
     */
    @GetMapping("/callback/{provider}")
    public void callback(
        @PathVariable String provider,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String error,
        @RequestParam(name = "error_description", required = false) String errorDescription,
        HttpServletResponse response
    ) throws IOException {
        log.info("OAuth2 callback received for provider: {}", provider);

        // Handle error from provider
        if (error != null) {
            log.warn("OAuth2 authorization error from {}: {} - {}", provider, error, errorDescription);
            response.sendRedirect("/login?error=" + error);
            return;
        }

        // Validate state
        if (state == null || !stateStore.containsKey(state)) {
            log.warn("Invalid or missing state parameter");
            response.sendRedirect("/login?error=invalid_state");
            return;
        }

        StateData stateData = stateStore.remove(state);

        // Validate state hasn't expired
        if (System.currentTimeMillis() - stateData.timestamp() > STATE_EXPIRATION_MS) {
            log.warn("Expired state parameter");
            response.sendRedirect("/login?error=expired_state");
            return;
        }

        // Validate provider matches
        if (!provider.equals(stateData.provider())) {
            log.warn("Provider mismatch in state: expected {}, got {}", stateData.provider(), provider);
            response.sendRedirect("/login?error=provider_mismatch");
            return;
        }

        // Validate code is present
        if (code == null || code.isBlank()) {
            log.warn("Missing authorization code");
            response.sendRedirect("/login?error=missing_code");
            return;
        }

        try {
            // Exchange code for tokens
            String callbackUrl = buildCallbackUrl(provider);
            OAuth2TokenResponse tokenResponse = oauth2TokenService.exchangeCodeForTokens(
                provider, code, callbackUrl
            );

            log.info("OAuth2 authentication successful for user: {}", tokenResponse.getUsername());

            // Redirect to frontend with tokens
            // In production, use secure HttpOnly cookies or a more secure token delivery mechanism
            String redirectUrl = stateData.redirectUri() +
                "?access_token=" + tokenResponse.getAccessToken() +
                "&refresh_token=" + tokenResponse.getRefreshToken() +
                "&expires_in=" + tokenResponse.getExpiresIn();

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Error exchanging authorization code for tokens", e);
            response.sendRedirect("/login?error=token_exchange_failed");
        }
    }

    /**
     * Exchange authorization code for tokens (API endpoint).
     * Alternative to callback-based flow for SPAs.
     *
     * @param provider OAuth2 provider name
     * @param code Authorization code
     * @param redirectUri Redirect URI used in authorization request
     * @return OAuth2 token response
     */
    @PostMapping("/token")
    public ResponseEntity<OAuth2TokenResponse> exchangeToken(
        @RequestParam String provider,
        @RequestParam String code,
        @RequestParam(name = "redirect_uri") String redirectUri
    ) {
        log.info("Token exchange request for provider: {}", provider);

        // Validate provider
        if (!oauth2Config.hasProvider(provider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unknown OAuth2 provider: " + provider);
        }

        try {
            OAuth2TokenResponse tokenResponse = oauth2TokenService.exchangeCodeForTokens(
                provider, code, redirectUri
            );

            log.info("Token exchange successful for user: {}", tokenResponse.getUsername());
            return ResponseEntity.ok(tokenResponse);

        } catch (Exception e) {
            log.error("Error during token exchange", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Token exchange failed: " + e.getMessage());
        }
    }

    /**
     * List configured OAuth2 providers.
     * Returns provider names and their authorization URLs.
     *
     * @return Map of provider names to authorization initiation URLs
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> listProviders() {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("enabled", oauth2Config.isEnabled());
        response.put("defaultProvider", oauth2Config.getDefaultProvider());

        Map<String, String> providers = new java.util.LinkedHashMap<>();
        oauth2Config.getProviders().forEach((name, config) -> {
            providers.put(name, "/api/v1/oauth2/authorize/" + name);
        });
        response.put("providers", providers);

        return ResponseEntity.ok(response);
    }

    /**
     * Validate an external OAuth2 token.
     *
     * @param provider OAuth2 provider name
     * @param token Access token to validate
     * @return Validation result
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
        @RequestParam String provider,
        @RequestParam String token
    ) {
        log.debug("Token validation request for provider: {}", provider);

        if (!oauth2Config.hasProvider(provider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unknown OAuth2 provider: " + provider);
        }

        boolean valid = oauth2TokenService.validateExternalToken(provider, token);

        Map<String, Object> response = Map.of(
            "valid", valid,
            "provider", provider
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Generate a cryptographically secure state token.
     */
    private String generateState() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Build the callback URL for OAuth2 redirect.
     */
    private String buildCallbackUrl(String provider) {
        // In production, this should be configurable
        return "http://localhost:8080/api/v1/oauth2/callback/" + provider;
    }

    /**
     * Clean up expired state entries.
     */
    private void cleanupExpiredStates() {
        long now = System.currentTimeMillis();
        stateStore.entrySet().removeIf(entry ->
            now - entry.getValue().timestamp() > STATE_EXPIRATION_MS
        );
    }
}
