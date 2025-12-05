package com.healthdata.authentication.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for OAuth2/OpenID Connect authentication.
 *
 * Supports multiple identity providers:
 * - Okta
 * - Azure AD
 * - Auth0
 * - Google
 * - Custom OIDC providers
 *
 * Example configuration in application.yml:
 * <pre>
 * oauth2:
 *   enabled: true
 *   default-provider: okta
 *   providers:
 *     okta:
 *       client-id: ${OKTA_CLIENT_ID}
 *       client-secret: ${OKTA_CLIENT_SECRET}
 *       issuer-uri: https://your-domain.okta.com
 *       scopes: openid,profile,email
 *     azure:
 *       client-id: ${AZURE_CLIENT_ID}
 *       client-secret: ${AZURE_CLIENT_SECRET}
 *       issuer-uri: https://login.microsoftonline.com/{tenant}/v2.0
 *       scopes: openid,profile,email
 * </pre>
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "oauth2")
@Validated
public class OAuth2Config {

    /**
     * Enable/disable OAuth2 authentication.
     * Default: false (uses JWT-only authentication)
     */
    private boolean enabled = false;

    /**
     * Default OAuth2 provider to use when not specified.
     */
    private String defaultProvider = "okta";

    /**
     * Map of OAuth2 provider configurations.
     * Key: provider name (e.g., "okta", "azure", "auth0")
     * Value: provider configuration
     */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    /**
     * OAuth2 Provider Configuration.
     */
    @Data
    public static class ProviderConfig {

        /**
         * OAuth2 Client ID.
         */
        @NotBlank(message = "OAuth2 client ID cannot be blank")
        private String clientId;

        /**
         * OAuth2 Client Secret.
         */
        @NotBlank(message = "OAuth2 client secret cannot be blank")
        private String clientSecret;

        /**
         * OIDC Issuer URI (for auto-discovery).
         * Example: https://your-domain.okta.com
         */
        @NotBlank(message = "OAuth2 issuer URI cannot be blank")
        private String issuerUri;

        /**
         * Authorization endpoint (optional - discovered from issuer).
         */
        private String authorizationUri;

        /**
         * Token endpoint (optional - discovered from issuer).
         */
        private String tokenUri;

        /**
         * User info endpoint (optional - discovered from issuer).
         */
        private String userInfoUri;

        /**
         * JWK Set URI for token validation (optional - discovered from issuer).
         */
        private String jwkSetUri;

        /**
         * OAuth2 scopes to request.
         * Default: openid,profile,email
         */
        private String scopes = "openid,profile,email";

        /**
         * Claim to use for username.
         * Default: email
         */
        private String usernameClaim = "email";

        /**
         * Claim to use for user ID.
         * Default: sub
         */
        private String userIdClaim = "sub";

        /**
         * Claim to use for tenant IDs (optional).
         * Can be a custom claim from the IdP.
         */
        private String tenantClaim;

        /**
         * Claim to use for roles (optional).
         * Can be a custom claim from the IdP.
         */
        private String rolesClaim;

        /**
         * Whether to create local user on first login.
         * Default: true
         */
        private boolean autoCreateUser = true;

        /**
         * Default tenant ID for new users (if no tenant claim).
         */
        private String defaultTenantId;

        /**
         * Default roles for new users (if no roles claim).
         */
        private String defaultRoles = "USER";
    }

    /**
     * Validate OAuth2 configuration after properties are loaded.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (!enabled) {
            log.info("OAuth2 authentication is disabled");
            return;
        }

        log.info("Initializing OAuth2 configuration...");

        if (providers.isEmpty()) {
            log.warn("OAuth2 is enabled but no providers are configured");
            return;
        }

        // Validate default provider exists
        if (defaultProvider != null && !providers.containsKey(defaultProvider)) {
            log.warn("Default OAuth2 provider '{}' is not configured", defaultProvider);
        }

        // Log configured providers
        providers.forEach((name, config) -> {
            log.info("OAuth2 provider '{}' configured:", name);
            log.info("  Issuer URI: {}", config.getIssuerUri());
            log.info("  Scopes: {}", config.getScopes());
            log.info("  Auto-create user: {}", config.isAutoCreateUser());
        });

        log.info("OAuth2 configuration validated successfully");
    }

    /**
     * Get provider configuration by name.
     *
     * @param providerName provider name
     * @return provider configuration or null if not found
     */
    public ProviderConfig getProvider(String providerName) {
        return providers.get(providerName);
    }

    /**
     * Get default provider configuration.
     *
     * @return default provider configuration or null if not configured
     */
    public ProviderConfig getDefaultProviderConfig() {
        if (defaultProvider == null) {
            return null;
        }
        return providers.get(defaultProvider);
    }

    /**
     * Check if a specific provider is configured.
     *
     * @param providerName provider name
     * @return true if provider is configured
     */
    public boolean hasProvider(String providerName) {
        return providers.containsKey(providerName);
    }
}
