package com.healthdata.authentication.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SmartOnFhirConfig.
 *
 * Tests the SMART on FHIR configuration class that manages OAuth2 settings,
 * scope validation, and endpoint configuration for SMART on FHIR authorization.
 *
 * Coverage:
 * - Configuration initialization with default endpoints
 * - SMART configuration JSON generation
 * - Scope validation (exact match and wildcard patterns)
 * - Request scope filtering
 */
@DisplayName("SmartOnFhirConfig Unit Tests")
class SmartOnFhirConfigTest {

    private SmartOnFhirConfig config;

    @BeforeEach
    void setUp() {
        config = new SmartOnFhirConfig();
        config.setEnabled(true);
        config.setFhirServerUrl("http://localhost:8081/fhir");

        // Set up default scopes
        config.setScopes(new HashSet<>(Arrays.asList(
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
        )));

        // Set up default response types
        config.setResponseTypes(new HashSet<>(Arrays.asList("code", "token")));

        // Set up default grant types
        config.setGrantTypes(new HashSet<>(Arrays.asList(
            "authorization_code",
            "refresh_token",
            "client_credentials"
        )));

        // Set up code challenge methods
        config.setCodeChallengeMethodsSupported(new HashSet<>(Arrays.asList("S256")));

        // Set up capabilities
        config.setCapabilities(new HashSet<>(Arrays.asList(
            "launch-ehr",
            "launch-standalone",
            "client-public",
            "client-confidential-symmetric",
            "permission-patient",
            "permission-user"
        )));

        config.setAccessTokenLifetime(3600);
        config.setRefreshTokenLifetime(604800);
        config.setRequirePkce(true);
    }

    // ==================== SMART CONFIGURATION TESTS ====================

    @Test
    @DisplayName("Should return all required fields in SMART configuration")
    void shouldReturnAllRequiredFieldsInSmartConfiguration() {
        // Set explicit endpoints
        config.setAuthorizationEndpoint("http://localhost:8081/fhir/oauth2/authorize");
        config.setTokenEndpoint("http://localhost:8081/fhir/oauth2/token");
        config.setUserInfoEndpoint("http://localhost:8081/fhir/oauth2/userinfo");
        config.setJwksEndpoint("http://localhost:8081/fhir/.well-known/jwks.json");

        Map<String, Object> smartConfig = config.getSmartConfiguration();

        // Verify all required fields are present
        assertThat(smartConfig).containsKeys(
            "authorization_endpoint",
            "token_endpoint",
            "userinfo_endpoint",
            "jwks_uri",
            "scopes_supported",
            "response_types_supported",
            "grant_types_supported",
            "code_challenge_methods_supported",
            "capabilities"
        );

        // Verify endpoint values
        assertThat(smartConfig.get("authorization_endpoint"))
            .isEqualTo("http://localhost:8081/fhir/oauth2/authorize");
        assertThat(smartConfig.get("token_endpoint"))
            .isEqualTo("http://localhost:8081/fhir/oauth2/token");
        assertThat(smartConfig.get("userinfo_endpoint"))
            .isEqualTo("http://localhost:8081/fhir/oauth2/userinfo");
        assertThat(smartConfig.get("jwks_uri"))
            .isEqualTo("http://localhost:8081/fhir/.well-known/jwks.json");

        // Verify collections
        assertThat(smartConfig.get("scopes_supported")).isInstanceOf(Set.class);
        assertThat(smartConfig.get("response_types_supported")).isInstanceOf(Set.class);
        assertThat(smartConfig.get("grant_types_supported")).isInstanceOf(Set.class);
        assertThat(smartConfig.get("capabilities")).isInstanceOf(Set.class);
    }

    @Test
    @DisplayName("Should generate default endpoints when not configured")
    void shouldGenerateDefaultEndpointsWhenNotConfigured() {
        // Do not set endpoints explicitly
        config.setAuthorizationEndpoint(null);
        config.setTokenEndpoint(null);
        config.setUserInfoEndpoint(null);
        config.setJwksEndpoint(null);

        // Trigger validation to generate defaults
        config.validateConfiguration();

        // Verify defaults are generated based on fhirServerUrl
        assertThat(config.getAuthorizationEndpoint())
            .isEqualTo("http://localhost:8081/fhir/oauth2/authorize");
        assertThat(config.getTokenEndpoint())
            .isEqualTo("http://localhost:8081/fhir/oauth2/token");
        assertThat(config.getUserInfoEndpoint())
            .isEqualTo("http://localhost:8081/fhir/oauth2/userinfo");
        assertThat(config.getJwksEndpoint())
            .isEqualTo("http://localhost:8081/fhir/.well-known/jwks.json");
    }

    @Test
    @DisplayName("Should generate default endpoints when configured with blank strings")
    void shouldGenerateDefaultEndpointsWhenConfiguredWithBlankStrings() {
        // Set endpoints to blank strings
        config.setAuthorizationEndpoint("");
        config.setTokenEndpoint("  ");
        config.setUserInfoEndpoint("\t");
        config.setJwksEndpoint("\n");

        // Trigger validation to generate defaults
        config.validateConfiguration();

        // Verify defaults are generated
        assertThat(config.getAuthorizationEndpoint())
            .isEqualTo("http://localhost:8081/fhir/oauth2/authorize");
        assertThat(config.getTokenEndpoint())
            .isEqualTo("http://localhost:8081/fhir/oauth2/token");
        assertThat(config.getUserInfoEndpoint())
            .isEqualTo("http://localhost:8081/fhir/oauth2/userinfo");
        assertThat(config.getJwksEndpoint())
            .isEqualTo("http://localhost:8081/fhir/.well-known/jwks.json");
    }

    @Test
    @DisplayName("Should preserve explicitly configured endpoints")
    void shouldPreserveExplicitlyConfiguredEndpoints() {
        // Set custom endpoints
        config.setAuthorizationEndpoint("https://custom.example.com/authorize");
        config.setTokenEndpoint("https://custom.example.com/token");
        config.setUserInfoEndpoint("https://custom.example.com/userinfo");
        config.setJwksEndpoint("https://custom.example.com/jwks");

        // Trigger validation
        config.validateConfiguration();

        // Verify custom endpoints are preserved
        assertThat(config.getAuthorizationEndpoint())
            .isEqualTo("https://custom.example.com/authorize");
        assertThat(config.getTokenEndpoint())
            .isEqualTo("https://custom.example.com/token");
        assertThat(config.getUserInfoEndpoint())
            .isEqualTo("https://custom.example.com/userinfo");
        assertThat(config.getJwksEndpoint())
            .isEqualTo("https://custom.example.com/jwks");
    }

    @Test
    @DisplayName("Should not validate configuration when disabled")
    void shouldNotValidateConfigurationWhenDisabled() {
        config.setEnabled(false);
        config.setAuthorizationEndpoint(null);

        // Should not throw exception or modify state
        config.validateConfiguration();

        // Endpoints should remain null
        assertThat(config.getAuthorizationEndpoint()).isNull();
    }

    // ==================== SCOPE VALIDATION TESTS ====================

    @Test
    @DisplayName("Should support exact scope match")
    void shouldSupportExactScopeMatch() {
        // Test exact matches
        assertThat(config.isScopeSupported("launch")).isTrue();
        assertThat(config.isScopeSupported("launch/patient")).isTrue();
        assertThat(config.isScopeSupported("openid")).isTrue();
        assertThat(config.isScopeSupported("profile")).isTrue();
        assertThat(config.isScopeSupported("fhirUser")).isTrue();
    }

    @Test
    @DisplayName("Should reject unsupported exact scope")
    void shouldRejectUnsupportedExactScope() {
        // Test scopes that don't exist
        assertThat(config.isScopeSupported("invalid")).isFalse();
        assertThat(config.isScopeSupported("admin")).isFalse();
        assertThat(config.isScopeSupported("launch/invalid")).isFalse();
    }

    @Test
    @DisplayName("Should support wildcard pattern matching for patient resources")
    void shouldSupportWildcardPatternMatchingForPatientResources() {
        // patient/*.read should match specific resource reads
        assertThat(config.isScopeSupported("patient/Observation.read")).isTrue();
        assertThat(config.isScopeSupported("patient/Patient.read")).isTrue();
        assertThat(config.isScopeSupported("patient/Condition.read")).isTrue();
        assertThat(config.isScopeSupported("patient/MedicationRequest.read")).isTrue();
        assertThat(config.isScopeSupported("patient/DiagnosticReport.read")).isTrue();
    }

    @Test
    @DisplayName("Should support wildcard pattern matching for patient writes")
    void shouldSupportWildcardPatternMatchingForPatientWrites() {
        // patient/*.write should match specific resource writes
        assertThat(config.isScopeSupported("patient/Observation.write")).isTrue();
        assertThat(config.isScopeSupported("patient/Patient.write")).isTrue();
        assertThat(config.isScopeSupported("patient/Condition.write")).isTrue();
    }

    @Test
    @DisplayName("Should support wildcard pattern matching for user resources")
    void shouldSupportWildcardPatternMatchingForUserResources() {
        // user/*.read should match specific resource reads
        assertThat(config.isScopeSupported("user/Observation.read")).isTrue();
        assertThat(config.isScopeSupported("user/Patient.read")).isTrue();
        assertThat(config.isScopeSupported("user/Practitioner.read")).isTrue();

        // user/*.write should match specific resource writes
        assertThat(config.isScopeSupported("user/Observation.write")).isTrue();
        assertThat(config.isScopeSupported("user/Patient.write")).isTrue();
    }

    @Test
    @DisplayName("Should reject wildcard pattern that doesn't match")
    void shouldRejectWildcardPatternThatDoesntMatch() {
        // These don't match any wildcard patterns
        assertThat(config.isScopeSupported("system/Patient.read")).isFalse();
        assertThat(config.isScopeSupported("patient/")).isFalse();
        assertThat(config.isScopeSupported("patient.Observation.read")).isFalse();
    }

    @Test
    @DisplayName("Should handle scope validation with special characters")
    void shouldHandleScopeValidationWithSpecialCharacters() {
        // Add a scope with special characters
        config.getScopes().add("special-scope");
        config.getScopes().add("scope:with:colons");

        assertThat(config.isScopeSupported("special-scope")).isTrue();
        assertThat(config.isScopeSupported("scope:with:colons")).isTrue();
    }

    @Test
    @DisplayName("Should be case-sensitive for scope matching")
    void shouldBeCaseSensitiveForScopeMatching() {
        // Scopes are case-sensitive
        assertThat(config.isScopeSupported("launch")).isTrue();
        assertThat(config.isScopeSupported("Launch")).isFalse();
        assertThat(config.isScopeSupported("LAUNCH")).isFalse();
        assertThat(config.isScopeSupported("openid")).isTrue();
        assertThat(config.isScopeSupported("OpenID")).isFalse();
    }

    // ==================== VALIDATE SCOPES TESTS ====================

    @Test
    @DisplayName("Should validate and filter requested scopes")
    void shouldValidateAndFilterRequestedScopes() {
        String requestedScopes = "launch openid patient/Observation.read invalid/scope";

        Set<String> validScopes = config.validateScopes(requestedScopes);

        // Should contain valid scopes only
        assertThat(validScopes)
            .hasSize(3)
            .contains("launch", "openid", "patient/Observation.read")
            .doesNotContain("invalid/scope");
    }

    @Test
    @DisplayName("Should filter all invalid scopes")
    void shouldFilterAllInvalidScopes() {
        String requestedScopes = "invalid1 invalid2 invalid3";

        Set<String> validScopes = config.validateScopes(requestedScopes);

        // Should return empty set
        assertThat(validScopes).isEmpty();
    }

    @Test
    @DisplayName("Should handle null scope string")
    void shouldHandleNullScopeString() {
        Set<String> validScopes = config.validateScopes(null);

        assertThat(validScopes).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty scope string")
    void shouldHandleEmptyScopeString() {
        Set<String> validScopes = config.validateScopes("");

        assertThat(validScopes).isEmpty();
    }

    @Test
    @DisplayName("Should handle blank scope string")
    void shouldHandleBlankScopeString() {
        Set<String> validScopes = config.validateScopes("   ");

        assertThat(validScopes).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple spaces between scopes")
    void shouldHandleMultipleSpacesBetweenScopes() {
        String requestedScopes = "launch    openid     patient/Observation.read";

        Set<String> validScopes = config.validateScopes(requestedScopes);

        assertThat(validScopes)
            .hasSize(3)
            .contains("launch", "openid", "patient/Observation.read");
    }

    @Test
    @DisplayName("Should validate scopes with wildcard patterns")
    void shouldValidateScopesWithWildcardPatterns() {
        String requestedScopes = "patient/Observation.read patient/Patient.write user/Practitioner.read";

        Set<String> validScopes = config.validateScopes(requestedScopes);

        // All should match wildcard patterns
        assertThat(validScopes)
            .hasSize(3)
            .contains("patient/Observation.read", "patient/Patient.write", "user/Practitioner.read");
    }

    @Test
    @DisplayName("Should validate mixed exact and wildcard scopes")
    void shouldValidateMixedExactAndWildcardScopes() {
        String requestedScopes = "launch openid patient/Observation.read user/Patient.write invalid";

        Set<String> validScopes = config.validateScopes(requestedScopes);

        assertThat(validScopes)
            .hasSize(4)
            .contains("launch", "openid", "patient/Observation.read", "user/Patient.write")
            .doesNotContain("invalid");
    }

    @Test
    @DisplayName("Should preserve all valid scopes when all are valid")
    void shouldPreserveAllValidScopesWhenAllAreValid() {
        String requestedScopes = "launch launch/patient openid profile";

        Set<String> validScopes = config.validateScopes(requestedScopes);

        assertThat(validScopes)
            .hasSize(4)
            .contains("launch", "launch/patient", "openid", "profile");
    }

    // ==================== CONFIGURATION PROPERTY TESTS ====================

    @Test
    @DisplayName("Should have correct default values")
    void shouldHaveCorrectDefaultValues() {
        SmartOnFhirConfig defaultConfig = new SmartOnFhirConfig();

        assertThat(defaultConfig.isEnabled()).isFalse();
        assertThat(defaultConfig.getFhirServerUrl()).isEqualTo("http://localhost:8081/fhir");
        assertThat(defaultConfig.getAccessTokenLifetime()).isEqualTo(3600);
        assertThat(defaultConfig.getRefreshTokenLifetime()).isEqualTo(604800);
        assertThat(defaultConfig.isRequirePkce()).isTrue();

        // Default scopes should be populated
        assertThat(defaultConfig.getScopes())
            .contains("launch", "openid", "patient/*.read");

        // Default response types
        assertThat(defaultConfig.getResponseTypes())
            .contains("code", "token");

        // Default grant types
        assertThat(defaultConfig.getGrantTypes())
            .contains("authorization_code", "refresh_token", "client_credentials");
    }

    @Test
    @DisplayName("Should allow configuration updates")
    void shouldAllowConfigurationUpdates() {
        config.setFhirServerUrl("https://prod.example.com/fhir");
        config.setAccessTokenLifetime(7200);
        config.setRefreshTokenLifetime(1209600);
        config.setRequirePkce(false);

        assertThat(config.getFhirServerUrl()).isEqualTo("https://prod.example.com/fhir");
        assertThat(config.getAccessTokenLifetime()).isEqualTo(7200);
        assertThat(config.getRefreshTokenLifetime()).isEqualTo(1209600);
        assertThat(config.isRequirePkce()).isFalse();
    }

    @Test
    @DisplayName("Should return scopes as a set")
    void shouldReturnScopesAsASet() {
        Map<String, Object> smartConfig = config.getSmartConfiguration();

        Object scopesSupported = smartConfig.get("scopes_supported");
        assertThat(scopesSupported).isInstanceOf(Set.class);

        @SuppressWarnings("unchecked")
        Set<String> scopes = (Set<String>) scopesSupported;
        assertThat(scopes).contains("launch", "openid", "patient/*.read");
    }

    @Test
    @DisplayName("Should return response types as a set")
    void shouldReturnResponseTypesAsASet() {
        Map<String, Object> smartConfig = config.getSmartConfiguration();

        Object responseTypes = smartConfig.get("response_types_supported");
        assertThat(responseTypes).isInstanceOf(Set.class);

        @SuppressWarnings("unchecked")
        Set<String> types = (Set<String>) responseTypes;
        assertThat(types).contains("code", "token");
    }

    @Test
    @DisplayName("Should return grant types as a set")
    void shouldReturnGrantTypesAsASet() {
        Map<String, Object> smartConfig = config.getSmartConfiguration();

        Object grantTypes = smartConfig.get("grant_types_supported");
        assertThat(grantTypes).isInstanceOf(Set.class);

        @SuppressWarnings("unchecked")
        Set<String> types = (Set<String>) grantTypes;
        assertThat(types).contains("authorization_code", "refresh_token");
    }

    @Test
    @DisplayName("Should include PKCE challenge methods")
    void shouldIncludePkceChallenageMethods() {
        Map<String, Object> smartConfig = config.getSmartConfiguration();

        Object challengeMethods = smartConfig.get("code_challenge_methods_supported");
        assertThat(challengeMethods).isInstanceOf(Set.class);

        @SuppressWarnings("unchecked")
        Set<String> methods = (Set<String>) challengeMethods;
        assertThat(methods).contains("S256");
    }

    @Test
    @DisplayName("Should include capabilities")
    void shouldIncludeCapabilities() {
        Map<String, Object> smartConfig = config.getSmartConfiguration();

        Object capabilities = smartConfig.get("capabilities");
        assertThat(capabilities).isInstanceOf(Set.class);

        @SuppressWarnings("unchecked")
        Set<String> caps = (Set<String>) capabilities;
        assertThat(caps).isNotEmpty();
    }
}
