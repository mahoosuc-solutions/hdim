package com.healthdata.authentication.service;

import com.healthdata.authentication.config.OAuth2Config;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.OAuth2TokenResponse;
import com.healthdata.authentication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service for handling OAuth2/OIDC authentication flows.
 *
 * Supports:
 * - Authorization Code flow
 * - Token exchange
 * - Token validation
 * - User provisioning from OAuth2 claims
 *
 * Compatible with:
 * - Okta
 * - Azure AD
 * - Auth0
 * - Any OIDC-compliant provider
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "oauth2", name = "enabled", havingValue = "true")
public class OAuth2TokenService {

    private final OAuth2Config oauth2Config;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Exchange authorization code for tokens.
     *
     * @param provider OAuth2 provider name
     * @param code authorization code
     * @param redirectUri redirect URI used in authorization request
     * @return OAuth2 token response with access and refresh tokens
     */
    public OAuth2TokenResponse exchangeCodeForTokens(String provider, String code, String redirectUri) {
        log.debug("Exchanging authorization code for tokens with provider: {}", provider);

        OAuth2Config.ProviderConfig providerConfig = oauth2Config.getProvider(provider);
        if (providerConfig == null) {
            throw new IllegalArgumentException("Unknown OAuth2 provider: " + provider);
        }

        String tokenUri = providerConfig.getTokenUri();
        if (tokenUri == null || tokenUri.isBlank()) {
            tokenUri = providerConfig.getIssuerUri() + "/oauth2/v1/token";
        }

        // Build token request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(providerConfig.getClientId(), providerConfig.getClientSecret());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();

                String accessToken = (String) tokenResponse.get("access_token");
                String refreshToken = (String) tokenResponse.get("refresh_token");
                String idToken = (String) tokenResponse.get("id_token");
                Integer expiresIn = (Integer) tokenResponse.get("expires_in");

                // Extract user info and provision local user
                User user = provisionUserFromToken(provider, idToken != null ? idToken : accessToken);

                // Generate local JWT tokens
                String localAccessToken = jwtTokenService.generateAccessToken(user);
                String localRefreshToken = jwtTokenService.generateRefreshToken(user);

                log.info("Successfully authenticated user via OAuth2: {}", user.getUsername());

                return OAuth2TokenResponse.builder()
                    .accessToken(localAccessToken)
                    .refreshToken(localRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn != null ? expiresIn : 3600)
                    .idToken(idToken)
                    .provider(provider)
                    .userId(user.getId().toString())
                    .username(user.getUsername())
                    .build();
            } else {
                throw new RuntimeException("Failed to exchange authorization code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error exchanging authorization code for tokens", e);
            throw new RuntimeException("OAuth2 token exchange failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate an external OAuth2/OIDC token.
     *
     * @param provider OAuth2 provider name
     * @param token access token or ID token
     * @return true if token is valid
     */
    public boolean validateExternalToken(String provider, String token) {
        log.debug("Validating external OAuth2 token from provider: {}", provider);

        OAuth2Config.ProviderConfig providerConfig = oauth2Config.getProvider(provider);
        if (providerConfig == null) {
            log.warn("Unknown OAuth2 provider: {}", provider);
            return false;
        }

        // For OIDC, validate the token using the introspection endpoint or JWKS
        String introspectionUri = providerConfig.getIssuerUri() + "/oauth2/v1/introspect";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(providerConfig.getClientId(), providerConfig.getClientSecret());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", token);
        body.add("token_type_hint", "access_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(introspectionUri, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean active = (Boolean) response.getBody().get("active");
                return Boolean.TRUE.equals(active);
            }
        } catch (Exception e) {
            log.warn("Error validating external OAuth2 token: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Provision or update local user from OAuth2 token claims.
     *
     * @param provider OAuth2 provider name
     * @param token ID token or access token
     * @return provisioned or updated user
     */
    public User provisionUserFromToken(String provider, String token) {
        OAuth2Config.ProviderConfig providerConfig = oauth2Config.getProvider(provider);
        if (providerConfig == null) {
            throw new IllegalArgumentException("Unknown OAuth2 provider: " + provider);
        }

        // Decode JWT token to extract claims (simplified - in production use proper JWT validation)
        Map<String, Object> claims = decodeJwtClaims(token);

        String username = extractClaim(claims, providerConfig.getUsernameClaim(), "email");
        String userId = extractClaim(claims, providerConfig.getUserIdClaim(), "sub");
        String email = extractClaim(claims, "email", null);
        String name = extractClaim(claims, "name", username);

        // Check if user exists
        Optional<User> existingUser = userRepository.findByUsername(username);

        if (existingUser.isPresent()) {
            log.debug("Updating existing user from OAuth2: {}", username);
            User user = existingUser.get();
            user.setEmail(email);
            user.setFirstName(extractFirstName(name));
            user.setLastName(extractLastName(name));
            return userRepository.save(user);
        }

        if (!providerConfig.isAutoCreateUser()) {
            throw new RuntimeException("User not found and auto-creation is disabled: " + username);
        }

        // Create new user
        log.info("Creating new user from OAuth2: {}", username);

        Set<String> tenantIds = extractTenantIds(claims, providerConfig);
        Set<UserRole> roles = extractRoles(claims, providerConfig);

        User newUser = User.builder()
            .username(username)
            .email(email)
            .firstName(extractFirstName(name) != null ? extractFirstName(name) : username)
            .lastName(extractLastName(name) != null ? extractLastName(name) : "")
            .passwordHash(UUID.randomUUID().toString()) // Random password hash since OAuth2 handles auth
            .tenantIds(tenantIds)
            .roles(roles)
            .active(true)
            .emailVerified(true) // Assume OAuth2 provider verified email
            .oauthProvider(provider)
            .oauthProviderId(userId)
            .build();

        return userRepository.save(newUser);
    }

    /**
     * Build authorization URL for OAuth2 flow.
     *
     * @param provider OAuth2 provider name
     * @param redirectUri redirect URI
     * @param state CSRF state parameter
     * @return authorization URL
     */
    public String buildAuthorizationUrl(String provider, String redirectUri, String state) {
        OAuth2Config.ProviderConfig providerConfig = oauth2Config.getProvider(provider);
        if (providerConfig == null) {
            throw new IllegalArgumentException("Unknown OAuth2 provider: " + provider);
        }

        String authorizationUri = providerConfig.getAuthorizationUri();
        if (authorizationUri == null || authorizationUri.isBlank()) {
            authorizationUri = providerConfig.getIssuerUri() + "/oauth2/v1/authorize";
        }

        StringBuilder url = new StringBuilder(authorizationUri);
        url.append("?response_type=code");
        url.append("&client_id=").append(providerConfig.getClientId());
        url.append("&redirect_uri=").append(redirectUri);
        url.append("&scope=").append(providerConfig.getScopes().replace(",", "%20"));
        url.append("&state=").append(state);

        return url.toString();
    }

    /**
     * Decode JWT claims (simplified - in production use proper JWT library with JWKS validation).
     */
    private Map<String, Object> decodeJwtClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Collections.emptyMap();
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // Simple JSON parsing - in production use Jackson
            Map<String, Object> claims = new HashMap<>();
            payload = payload.replaceAll("[{}]", "");
            for (String pair : payload.split(",")) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim().replaceAll("\"", "");
                    claims.put(key, value);
                }
            }
            return claims;
        } catch (Exception e) {
            log.warn("Error decoding JWT claims: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String extractClaim(Map<String, Object> claims, String claimName, String defaultValue) {
        if (claimName == null) {
            return defaultValue;
        }
        Object value = claims.get(claimName);
        return value != null ? value.toString() : defaultValue;
    }

    private Set<String> extractTenantIds(Map<String, Object> claims, OAuth2Config.ProviderConfig config) {
        Set<String> tenantIds = new HashSet<>();

        if (config.getTenantClaim() != null) {
            String tenantValue = extractClaim(claims, config.getTenantClaim(), null);
            if (tenantValue != null) {
                tenantIds.addAll(Arrays.asList(tenantValue.split(",")));
            }
        }

        if (tenantIds.isEmpty() && config.getDefaultTenantId() != null) {
            tenantIds.add(config.getDefaultTenantId());
        }

        return tenantIds;
    }

    private Set<UserRole> extractRoles(Map<String, Object> claims, OAuth2Config.ProviderConfig config) {
        Set<UserRole> roles = new HashSet<>();

        if (config.getRolesClaim() != null) {
            String rolesValue = extractClaim(claims, config.getRolesClaim(), null);
            if (rolesValue != null) {
                for (String role : rolesValue.split(",")) {
                    try {
                        roles.add(UserRole.valueOf(role.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown role from OAuth2 claim: {}", role);
                    }
                }
            }
        }

        if (roles.isEmpty() && config.getDefaultRoles() != null) {
            for (String role : config.getDefaultRoles().split(",")) {
                try {
                    roles.add(UserRole.valueOf(role.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown default role: {}", role);
                }
            }
        }

        return roles;
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 1 ? parts[parts.length - 1] : null;
    }
}
