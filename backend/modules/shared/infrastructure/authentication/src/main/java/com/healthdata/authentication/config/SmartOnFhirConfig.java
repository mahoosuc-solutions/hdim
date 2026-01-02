package com.healthdata.authentication.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for SMART on FHIR authorization.
 *
 * SMART on FHIR (Substitutable Medical Applications and Reusable Technologies)
 * is a set of open standards that enable third-party applications to securely
 * access EHR data.
 *
 * Supports:
 * - Standalone launch (app launches from outside EHR)
 * - EHR launch (app launches from within EHR)
 * - Patient context (patient-facing apps)
 * - Practitioner context (provider-facing apps)
 * - System-level access (backend services)
 *
 * Example configuration:
 * <pre>
 * smart:
 *   enabled: true
 *   fhir-server-url: http://localhost:8081/fhir
 *   scopes:
 *     - launch
 *     - launch/patient
 *     - launch/encounter
 *     - patient/*.read
 *     - patient/*.write
 *     - user/*.read
 *     - openid
 *     - profile
 *     - fhirUser
 * </pre>
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "smart")
public class SmartOnFhirConfig {

    /**
     * Enable/disable SMART on FHIR authorization.
     */
    private boolean enabled = false;

    /**
     * FHIR server base URL.
     */
    private String fhirServerUrl = "http://localhost:8081/fhir";

    /**
     * Authorization endpoint URL.
     */
    private String authorizationEndpoint;

    /**
     * Token endpoint URL.
     */
    private String tokenEndpoint;

    /**
     * User info endpoint URL.
     */
    private String userInfoEndpoint;

    /**
     * JWKS endpoint URL for token validation.
     */
    private String jwksEndpoint;

    /**
     * Supported SMART scopes.
     */
    private Set<String> scopes = new HashSet<>(Arrays.asList(
        "launch",
        "launch/patient",
        "launch/encounter",
        "patient/*.read",
        "patient/*.write",
        "user/*.read",
        "user/*.write",
        "openid",
        "profile",
        "fhirUser"
    ));

    /**
     * Supported response types.
     */
    private Set<String> responseTypes = new HashSet<>(Arrays.asList(
        "code",
        "token"
    ));

    /**
     * Supported grant types.
     */
    private Set<String> grantTypes = new HashSet<>(Arrays.asList(
        "authorization_code",
        "refresh_token",
        "client_credentials"
    ));

    /**
     * Supported code challenge methods (PKCE).
     */
    private Set<String> codeChallengeMethodsSupported = new HashSet<>(Arrays.asList(
        "S256"
    ));

    /**
     * SMART capabilities.
     */
    private Set<String> capabilities = new HashSet<>(Arrays.asList(
        "launch-ehr",
        "launch-standalone",
        "client-public",
        "client-confidential-symmetric",
        "context-passthrough-banner",
        "context-passthrough-style",
        "context-ehr-patient",
        "context-ehr-encounter",
        "context-standalone-patient",
        "context-standalone-encounter",
        "permission-offline",
        "permission-patient",
        "permission-user",
        "sso-openid-connect"
    ));

    /**
     * Access token lifetime in seconds.
     */
    private int accessTokenLifetime = 3600;

    /**
     * Refresh token lifetime in seconds.
     */
    private int refreshTokenLifetime = 604800; // 7 days

    /**
     * Whether to require PKCE for public clients.
     */
    private boolean requirePkce = true;

    /**
     * Validate configuration on startup.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (!enabled) {
            log.info("SMART on FHIR authorization is disabled");
            return;
        }

        log.info("Initializing SMART on FHIR configuration...");

        // Set default endpoints if not configured
        if (authorizationEndpoint == null || authorizationEndpoint.isBlank()) {
            authorizationEndpoint = fhirServerUrl + "/oauth2/authorize";
        }

        if (tokenEndpoint == null || tokenEndpoint.isBlank()) {
            tokenEndpoint = fhirServerUrl + "/oauth2/token";
        }

        if (userInfoEndpoint == null || userInfoEndpoint.isBlank()) {
            userInfoEndpoint = fhirServerUrl + "/oauth2/userinfo";
        }

        if (jwksEndpoint == null || jwksEndpoint.isBlank()) {
            jwksEndpoint = fhirServerUrl + "/.well-known/jwks.json";
        }

        log.info("SMART on FHIR configuration:");
        log.info("  FHIR Server URL: {}", fhirServerUrl);
        log.info("  Authorization Endpoint: {}", authorizationEndpoint);
        log.info("  Token Endpoint: {}", tokenEndpoint);
        log.info("  Supported Scopes: {}", scopes.size());
        log.info("  Require PKCE: {}", requirePkce);

        log.info("SMART on FHIR configuration validated successfully");
    }

    /**
     * Generate the SMART configuration JSON for .well-known/smart-configuration.
     *
     * @return SMART configuration as a Map
     */
    public java.util.Map<String, Object> getSmartConfiguration() {
        java.util.Map<String, Object> config = new java.util.LinkedHashMap<>();

        config.put("authorization_endpoint", authorizationEndpoint);
        config.put("token_endpoint", tokenEndpoint);
        config.put("userinfo_endpoint", userInfoEndpoint);
        config.put("jwks_uri", jwksEndpoint);
        config.put("scopes_supported", scopes);
        config.put("response_types_supported", responseTypes);
        config.put("grant_types_supported", grantTypes);
        config.put("code_challenge_methods_supported", codeChallengeMethodsSupported);
        config.put("capabilities", capabilities);

        return config;
    }

    /**
     * Check if a scope is supported.
     *
     * @param scope scope to check
     * @return true if scope is supported
     */
    public boolean isScopeSupported(String scope) {
        // Check exact match
        if (scopes.contains(scope)) {
            return true;
        }

        // Check wildcard matches (e.g., patient/*.read matches patient/Observation.read)
        for (String supportedScope : scopes) {
            if (supportedScope.contains("*")) {
                String pattern = supportedScope
                    .replace(".", "\\.")
                    .replace("*", ".*");
                if (scope.matches(pattern)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Validate requested scopes against supported scopes.
     *
     * @param requestedScopes space-separated scopes
     * @return set of valid scopes
     */
    public Set<String> validateScopes(String requestedScopes) {
        Set<String> validScopes = new HashSet<>();

        if (requestedScopes == null || requestedScopes.isBlank()) {
            return validScopes;
        }

        for (String scope : requestedScopes.split("\\s+")) {
            if (isScopeSupported(scope)) {
                validScopes.add(scope);
            } else {
                log.warn("Unsupported SMART scope requested: {}", scope);
            }
        }

        return validScopes;
    }
}
