package com.healthdata.authentication.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OAuth2Config.
 *
 * Tests OAuth2 configuration including:
 * - Provider configuration retrieval
 * - Default provider handling
 * - Provider existence checks
 * - Configuration validation
 *
 * Uses TDD approach with JUnit 5 and AssertJ.
 */
@DisplayName("OAuth2Config Unit Tests")
class OAuth2ConfigTest {

    private OAuth2Config oauth2Config;
    private OAuth2Config.ProviderConfig oktaProviderConfig;
    private OAuth2Config.ProviderConfig azureProviderConfig;
    private OAuth2Config.ProviderConfig auth0ProviderConfig;

    private static final String OKTA_PROVIDER = "okta";
    private static final String AZURE_PROVIDER = "azure";
    private static final String AUTH0_PROVIDER = "auth0";
    private static final String UNKNOWN_PROVIDER = "unknown";

    @BeforeEach
    void setUp() {
        oauth2Config = new OAuth2Config();

        // Setup Okta provider config
        oktaProviderConfig = new OAuth2Config.ProviderConfig();
        oktaProviderConfig.setClientId("okta-client-id");
        oktaProviderConfig.setClientSecret("okta-client-secret");
        oktaProviderConfig.setIssuerUri("https://test.okta.com");
        oktaProviderConfig.setAuthorizationUri("https://test.okta.com/oauth2/v1/authorize");
        oktaProviderConfig.setTokenUri("https://test.okta.com/oauth2/v1/token");
        oktaProviderConfig.setUserInfoUri("https://test.okta.com/oauth2/v1/userinfo");
        oktaProviderConfig.setJwkSetUri("https://test.okta.com/oauth2/v1/keys");
        oktaProviderConfig.setScopes("openid,profile,email");
        oktaProviderConfig.setUsernameClaim("email");
        oktaProviderConfig.setUserIdClaim("sub");
        oktaProviderConfig.setAutoCreateUser(true);
        oktaProviderConfig.setDefaultTenantId("okta-tenant");
        oktaProviderConfig.setDefaultRoles("USER");

        // Setup Azure provider config
        azureProviderConfig = new OAuth2Config.ProviderConfig();
        azureProviderConfig.setClientId("azure-client-id");
        azureProviderConfig.setClientSecret("azure-client-secret");
        azureProviderConfig.setIssuerUri("https://login.microsoftonline.com/tenant-id/v2.0");
        azureProviderConfig.setScopes("openid,profile,email");
        azureProviderConfig.setUsernameClaim("preferred_username");
        azureProviderConfig.setUserIdClaim("oid");
        azureProviderConfig.setAutoCreateUser(true);
        azureProviderConfig.setDefaultTenantId("azure-tenant");
        azureProviderConfig.setDefaultRoles("EVALUATOR");

        // Setup Auth0 provider config
        auth0ProviderConfig = new OAuth2Config.ProviderConfig();
        auth0ProviderConfig.setClientId("auth0-client-id");
        auth0ProviderConfig.setClientSecret("auth0-client-secret");
        auth0ProviderConfig.setIssuerUri("https://test.auth0.com");
        auth0ProviderConfig.setScopes("openid,profile,email");
        auth0ProviderConfig.setUsernameClaim("email");
        auth0ProviderConfig.setUserIdClaim("sub");
        auth0ProviderConfig.setAutoCreateUser(false);

        // Setup providers map
        Map<String, OAuth2Config.ProviderConfig> providers = new HashMap<>();
        providers.put(OKTA_PROVIDER, oktaProviderConfig);
        providers.put(AZURE_PROVIDER, azureProviderConfig);
        providers.put(AUTH0_PROVIDER, auth0ProviderConfig);
        oauth2Config.setProviders(providers);
    }

    // ==================== GET PROVIDER TESTS ====================

    @Test
    @DisplayName("Should get provider config by name")
    void shouldGetProviderConfigByName() {
        // When
        OAuth2Config.ProviderConfig result = oauth2Config.getProvider(OKTA_PROVIDER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(oktaProviderConfig);
        assertThat(result.getClientId()).isEqualTo("okta-client-id");
        assertThat(result.getIssuerUri()).isEqualTo("https://test.okta.com");
    }

    @Test
    @DisplayName("Should return null for unknown provider")
    void shouldReturnNullForUnknownProvider() {
        // When
        OAuth2Config.ProviderConfig result = oauth2Config.getProvider(UNKNOWN_PROVIDER);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should get Azure provider config")
    void shouldGetAzureProviderConfig() {
        // When
        OAuth2Config.ProviderConfig result = oauth2Config.getProvider(AZURE_PROVIDER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(azureProviderConfig);
        assertThat(result.getClientId()).isEqualTo("azure-client-id");
        assertThat(result.getUsernameClaim()).isEqualTo("preferred_username");
        assertThat(result.getUserIdClaim()).isEqualTo("oid");
    }

    @Test
    @DisplayName("Should get Auth0 provider config")
    void shouldGetAuth0ProviderConfig() {
        // When
        OAuth2Config.ProviderConfig result = oauth2Config.getProvider(AUTH0_PROVIDER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(auth0ProviderConfig);
        assertThat(result.isAutoCreateUser()).isFalse();
    }

    // ==================== GET DEFAULT PROVIDER TESTS ====================

    @Test
    @DisplayName("Should get default provider config")
    void shouldGetDefaultProviderConfig() {
        // Given
        oauth2Config.setDefaultProvider(OKTA_PROVIDER);

        // When
        OAuth2Config.ProviderConfig result = oauth2Config.getDefaultProviderConfig();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(oktaProviderConfig);
    }

    @Test
    @DisplayName("Should return null when default provider is not set")
    void shouldReturnNullWhenDefaultProviderIsNotSet() {
        // Given
        oauth2Config.setDefaultProvider(null);

        // When
        OAuth2Config.ProviderConfig result = oauth2Config.getDefaultProviderConfig();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when default provider does not exist")
    void shouldReturnNullWhenDefaultProviderDoesNotExist() {
        // Given
        oauth2Config.setDefaultProvider(UNKNOWN_PROVIDER);

        // When
        OAuth2Config.ProviderConfig result = oauth2Config.getDefaultProviderConfig();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should get Azure as default provider")
    void shouldGetAzureAsDefaultProvider() {
        // Given
        oauth2Config.setDefaultProvider(AZURE_PROVIDER);

        // When
        OAuth2Config.ProviderConfig result = oauth2Config.getDefaultProviderConfig();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(azureProviderConfig);
    }

    // ==================== HAS PROVIDER TESTS ====================

    @Test
    @DisplayName("Should return true when provider exists")
    void shouldReturnTrueWhenProviderExists() {
        // When
        boolean hasOkta = oauth2Config.hasProvider(OKTA_PROVIDER);
        boolean hasAzure = oauth2Config.hasProvider(AZURE_PROVIDER);
        boolean hasAuth0 = oauth2Config.hasProvider(AUTH0_PROVIDER);

        // Then
        assertThat(hasOkta).isTrue();
        assertThat(hasAzure).isTrue();
        assertThat(hasAuth0).isTrue();
    }

    @Test
    @DisplayName("Should return false when provider does not exist")
    void shouldReturnFalseWhenProviderDoesNotExist() {
        // When
        boolean hasUnknown = oauth2Config.hasProvider(UNKNOWN_PROVIDER);

        // Then
        assertThat(hasUnknown).isFalse();
    }

    @Test
    @DisplayName("Should handle null provider name in hasProvider")
    void shouldHandleNullProviderNameInHasProvider() {
        // When
        boolean hasNull = oauth2Config.hasProvider(null);

        // Then
        assertThat(hasNull).isFalse();
    }

    @Test
    @DisplayName("Should return false for empty provider name")
    void shouldReturnFalseForEmptyProviderName() {
        // When
        boolean hasEmpty = oauth2Config.hasProvider("");

        // Then
        assertThat(hasEmpty).isFalse();
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("Should handle validation with missing providers")
    void shouldHandleValidationWithMissingProviders() {
        // Given
        OAuth2Config configWithNoProviders = new OAuth2Config();
        configWithNoProviders.setEnabled(true);
        configWithNoProviders.setProviders(new HashMap<>());

        // When & Then - Should not throw exception
        assertThatCode(() -> configWithNoProviders.validateConfiguration())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should validate when OAuth2 is disabled")
    void shouldValidateWhenOAuth2IsDisabled() {
        // Given
        oauth2Config.setEnabled(false);

        // When & Then - Should not throw exception
        assertThatCode(() -> oauth2Config.validateConfiguration())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should validate with configured providers")
    void shouldValidateWithConfiguredProviders() {
        // Given
        oauth2Config.setEnabled(true);
        oauth2Config.setDefaultProvider(OKTA_PROVIDER);

        // When & Then - Should not throw exception
        assertThatCode(() -> oauth2Config.validateConfiguration())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should validate when default provider does not exist")
    void shouldValidateWhenDefaultProviderDoesNotExist() {
        // Given
        oauth2Config.setEnabled(true);
        oauth2Config.setDefaultProvider(UNKNOWN_PROVIDER);

        // When & Then - Should log warning but not throw exception
        assertThatCode(() -> oauth2Config.validateConfiguration())
            .doesNotThrowAnyException();
    }

    // ==================== PROVIDER CONFIG TESTS ====================

    @Test
    @DisplayName("Should have correct provider configuration properties")
    void shouldHaveCorrectProviderConfigurationProperties() {
        // When
        OAuth2Config.ProviderConfig config = oauth2Config.getProvider(OKTA_PROVIDER);

        // Then
        assertThat(config.getClientId()).isEqualTo("okta-client-id");
        assertThat(config.getClientSecret()).isEqualTo("okta-client-secret");
        assertThat(config.getIssuerUri()).isEqualTo("https://test.okta.com");
        assertThat(config.getAuthorizationUri()).isEqualTo("https://test.okta.com/oauth2/v1/authorize");
        assertThat(config.getTokenUri()).isEqualTo("https://test.okta.com/oauth2/v1/token");
        assertThat(config.getUserInfoUri()).isEqualTo("https://test.okta.com/oauth2/v1/userinfo");
        assertThat(config.getJwkSetUri()).isEqualTo("https://test.okta.com/oauth2/v1/keys");
        assertThat(config.getScopes()).isEqualTo("openid,profile,email");
        assertThat(config.getUsernameClaim()).isEqualTo("email");
        assertThat(config.getUserIdClaim()).isEqualTo("sub");
        assertThat(config.isAutoCreateUser()).isTrue();
        assertThat(config.getDefaultTenantId()).isEqualTo("okta-tenant");
        assertThat(config.getDefaultRoles()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should have correct Azure-specific configuration")
    void shouldHaveCorrectAzureSpecificConfiguration() {
        // When
        OAuth2Config.ProviderConfig config = oauth2Config.getProvider(AZURE_PROVIDER);

        // Then
        assertThat(config.getUsernameClaim()).isEqualTo("preferred_username");
        assertThat(config.getUserIdClaim()).isEqualTo("oid");
        assertThat(config.getDefaultTenantId()).isEqualTo("azure-tenant");
        assertThat(config.getDefaultRoles()).isEqualTo("EVALUATOR");
    }

    @Test
    @DisplayName("Should support custom tenant and role claims")
    void shouldSupportCustomTenantAndRoleClaims() {
        // Given
        OAuth2Config.ProviderConfig config = new OAuth2Config.ProviderConfig();
        config.setClientId("test-client");
        config.setClientSecret("test-secret");
        config.setIssuerUri("https://test.com");
        config.setTenantClaim("custom_tenants");
        config.setRolesClaim("custom_roles");

        // When
        String tenantClaim = config.getTenantClaim();
        String rolesClaim = config.getRolesClaim();

        // Then
        assertThat(tenantClaim).isEqualTo("custom_tenants");
        assertThat(rolesClaim).isEqualTo("custom_roles");
    }

    @Test
    @DisplayName("Should have default scopes value")
    void shouldHaveDefaultScopesValue() {
        // Given
        OAuth2Config.ProviderConfig config = new OAuth2Config.ProviderConfig();

        // When
        String scopes = config.getScopes();

        // Then
        assertThat(scopes).isEqualTo("openid,profile,email");
    }

    @Test
    @DisplayName("Should have default username claim")
    void shouldHaveDefaultUsernameClaim() {
        // Given
        OAuth2Config.ProviderConfig config = new OAuth2Config.ProviderConfig();

        // When
        String usernameClaim = config.getUsernameClaim();

        // Then
        assertThat(usernameClaim).isEqualTo("email");
    }

    @Test
    @DisplayName("Should have default user ID claim")
    void shouldHaveDefaultUserIdClaim() {
        // Given
        OAuth2Config.ProviderConfig config = new OAuth2Config.ProviderConfig();

        // When
        String userIdClaim = config.getUserIdClaim();

        // Then
        assertThat(userIdClaim).isEqualTo("sub");
    }

    @Test
    @DisplayName("Should have auto-create user enabled by default")
    void shouldHaveAutoCreateUserEnabledByDefault() {
        // Given
        OAuth2Config.ProviderConfig config = new OAuth2Config.ProviderConfig();

        // When
        boolean autoCreateUser = config.isAutoCreateUser();

        // Then
        assertThat(autoCreateUser).isTrue();
    }

    @Test
    @DisplayName("Should have default roles as USER")
    void shouldHaveDefaultRolesAsUser() {
        // Given
        OAuth2Config.ProviderConfig config = new OAuth2Config.ProviderConfig();

        // When
        String defaultRoles = config.getDefaultRoles();

        // Then
        assertThat(defaultRoles).isEqualTo("USER");
    }

    // ==================== OAUTH2 CONFIG MAIN TESTS ====================

    @Test
    @DisplayName("Should have OAuth2 disabled by default")
    void shouldHaveOAuth2DisabledByDefault() {
        // Given
        OAuth2Config config = new OAuth2Config();

        // When
        boolean enabled = config.isEnabled();

        // Then
        assertThat(enabled).isFalse();
    }

    @Test
    @DisplayName("Should have default provider set to okta")
    void shouldHaveDefaultProviderSetToOkta() {
        // Given
        OAuth2Config config = new OAuth2Config();

        // When
        String defaultProvider = config.getDefaultProvider();

        // Then
        assertThat(defaultProvider).isEqualTo("okta");
    }

    @Test
    @DisplayName("Should initialize with empty providers map")
    void shouldInitializeWithEmptyProvidersMap() {
        // Given
        OAuth2Config config = new OAuth2Config();

        // When
        Map<String, OAuth2Config.ProviderConfig> providers = config.getProviders();

        // Then
        assertThat(providers).isNotNull();
        assertThat(providers).isEmpty();
    }

    @Test
    @DisplayName("Should allow setting and getting enabled state")
    void shouldAllowSettingAndGettingEnabledState() {
        // Given
        OAuth2Config config = new OAuth2Config();

        // When
        config.setEnabled(true);

        // Then
        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should allow changing default provider")
    void shouldAllowChangingDefaultProvider() {
        // Given
        oauth2Config.setDefaultProvider(AZURE_PROVIDER);

        // When
        String defaultProvider = oauth2Config.getDefaultProvider();
        OAuth2Config.ProviderConfig defaultConfig = oauth2Config.getDefaultProviderConfig();

        // Then
        assertThat(defaultProvider).isEqualTo(AZURE_PROVIDER);
        assertThat(defaultConfig).isEqualTo(azureProviderConfig);
    }

    @Test
    @DisplayName("Should support multiple providers simultaneously")
    void shouldSupportMultipleProvidersSimultaneously() {
        // When
        boolean hasOkta = oauth2Config.hasProvider(OKTA_PROVIDER);
        boolean hasAzure = oauth2Config.hasProvider(AZURE_PROVIDER);
        boolean hasAuth0 = oauth2Config.hasProvider(AUTH0_PROVIDER);

        OAuth2Config.ProviderConfig okta = oauth2Config.getProvider(OKTA_PROVIDER);
        OAuth2Config.ProviderConfig azure = oauth2Config.getProvider(AZURE_PROVIDER);
        OAuth2Config.ProviderConfig auth0 = oauth2Config.getProvider(AUTH0_PROVIDER);

        // Then
        assertThat(hasOkta).isTrue();
        assertThat(hasAzure).isTrue();
        assertThat(hasAuth0).isTrue();

        assertThat(okta).isNotNull();
        assertThat(azure).isNotNull();
        assertThat(auth0).isNotNull();

        assertThat(okta.getClientId()).isNotEqualTo(azure.getClientId());
        assertThat(azure.getClientId()).isNotEqualTo(auth0.getClientId());
    }

    @Test
    @DisplayName("Should handle provider config with minimal settings")
    void shouldHandleProviderConfigWithMinimalSettings() {
        // Given
        OAuth2Config.ProviderConfig minimalConfig = new OAuth2Config.ProviderConfig();
        minimalConfig.setClientId("minimal-client");
        minimalConfig.setClientSecret("minimal-secret");
        minimalConfig.setIssuerUri("https://minimal.com");

        Map<String, OAuth2Config.ProviderConfig> providers = new HashMap<>();
        providers.put("minimal", minimalConfig);
        oauth2Config.setProviders(providers);

        // When
        OAuth2Config.ProviderConfig retrieved = oauth2Config.getProvider("minimal");

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getClientId()).isEqualTo("minimal-client");
        assertThat(retrieved.getScopes()).isEqualTo("openid,profile,email"); // Default
        assertThat(retrieved.getUsernameClaim()).isEqualTo("email"); // Default
        assertThat(retrieved.isAutoCreateUser()).isTrue(); // Default
    }
}
