package com.healthdata.fhir.security.smart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Controller for SMART on FHIR Well-Known Configuration.
 *
 * Implements the SMART App Launch Framework discovery endpoint:
 * GET /.well-known/smart-configuration
 *
 * This allows SMART apps to discover authorization endpoints and capabilities.
 */
@Slf4j
@RestController
@Tag(name = "SMART", description = "SMART on FHIR OAuth 2.0 endpoints")
public class SmartConfigurationController {

    @Value("${smart.base-url:http://localhost:8085/fhir}")
    private String baseUrl;

    @Value("${smart.authorization-endpoint:${smart.base-url}/oauth/authorize}")
    private String authorizationEndpoint;

    @Value("${smart.token-endpoint:${smart.base-url}/oauth/token}")
    private String tokenEndpoint;

    @Value("${smart.introspection-endpoint:${smart.base-url}/oauth/introspect}")
    private String introspectionEndpoint;

    @Value("${smart.revocation-endpoint:${smart.base-url}/oauth/revoke}")
    private String revocationEndpoint;

    @Value("${smart.userinfo-endpoint:${smart.base-url}/oauth/userinfo}")
    private String userinfoEndpoint;

    @Value("${smart.jwks-uri:${smart.base-url}/.well-known/jwks.json}")
    private String jwksUri;

    @Value("${smart.registration-endpoint:${smart.base-url}/oauth/register}")
    private String registrationEndpoint;

    @Operation(
        summary = "SMART Configuration Discovery",
        description = "Returns SMART on FHIR configuration including authorization endpoints, supported scopes, and capabilities. This is the discovery endpoint for SMART apps.",
        operationId = "getSmartConfiguration"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "SMART configuration returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SmartConfiguration.class))
        )
    })
    @GetMapping(
        path = "/.well-known/smart-configuration",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SmartConfiguration> getSmartConfiguration() {
        log.debug("Serving SMART configuration");

        SmartConfiguration config = SmartConfiguration.builder()
            .authorizationEndpoint(authorizationEndpoint)
            .tokenEndpoint(tokenEndpoint)
            .introspectionEndpoint(introspectionEndpoint)
            .revocationEndpoint(revocationEndpoint)
            .userinfoEndpoint(userinfoEndpoint)
            .jwksUri(jwksUri)
            .registrationEndpoint(registrationEndpoint)
            .capabilities(getCapabilities())
            .scopesSupported(getSupportedScopes())
            .responseTypesSupported(getResponseTypes())
            .tokenEndpointAuthMethodsSupported(getTokenAuthMethods())
            .grantTypesSupported(getGrantTypes())
            .codeChallengeMethodsSupported(getCodeChallengeMethods())
            .build();

        return ResponseEntity.ok(config);
    }

    /**
     * Get SMART capabilities supported by this server.
     */
    private List<String> getCapabilities() {
        return Arrays.asList(
            // Authorization
            "launch-ehr",              // Support EHR launch
            "launch-standalone",        // Support standalone launch
            "client-public",           // Support public clients
            "client-confidential-symmetric", // Support symmetric key clients
            "client-confidential-asymmetric", // Support asymmetric key clients

            // SSO
            "sso-openid-connect",      // OpenID Connect SSO

            // Context
            "context-passthrough-banner",  // Pass banner info
            "context-passthrough-style",   // Pass style info
            "context-ehr-patient",     // EHR-provided patient context
            "context-ehr-encounter",   // EHR-provided encounter context
            "context-standalone-patient", // Standalone patient selection
            "context-standalone-encounter", // Standalone encounter selection

            // Permissions
            "permission-offline",      // Offline access
            "permission-patient",      // Patient-level scopes
            "permission-user",         // User-level scopes

            // PKCE
            "authorize-pkce"           // PKCE support
        );
    }

    /**
     * Get supported OAuth scopes.
     */
    private List<String> getSupportedScopes() {
        return SmartScope.getAllScopeStrings();
    }

    /**
     * Get supported response types.
     */
    private List<String> getResponseTypes() {
        return Arrays.asList(
            "code",                    // Authorization code flow
            "token",                   // Implicit flow (deprecated but supported)
            "id_token",               // OpenID Connect
            "code id_token"           // Hybrid flow
        );
    }

    /**
     * Get supported token endpoint authentication methods.
     */
    private List<String> getTokenAuthMethods() {
        return Arrays.asList(
            "client_secret_basic",     // HTTP Basic auth
            "client_secret_post",      // Credentials in body
            "private_key_jwt",         // JWT with private key
            "none"                     // Public clients
        );
    }

    /**
     * Get supported grant types.
     */
    private List<String> getGrantTypes() {
        return Arrays.asList(
            "authorization_code",      // Standard authorization code
            "refresh_token",           // Refresh token
            "client_credentials"       // Backend services
        );
    }

    /**
     * Get supported PKCE code challenge methods.
     */
    private List<String> getCodeChallengeMethods() {
        return Arrays.asList(
            "S256",                    // SHA-256 (recommended)
            "plain"                    // Plain (not recommended)
        );
    }
}
