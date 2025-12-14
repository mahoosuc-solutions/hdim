package com.healthdata.fhir.security.smart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * SMART on FHIR Configuration response.
 *
 * Implements the SMART App Launch Framework specification:
 * http://hl7.org/fhir/smart-app-launch/
 *
 * This configuration is served at /.well-known/smart-configuration
 */
@Data
@Builder
public class SmartConfiguration {

    /**
     * URL to the OAuth2 authorization endpoint.
     */
    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    /**
     * URL to the OAuth2 token endpoint.
     */
    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    /**
     * URL to the OAuth2 token introspection endpoint.
     */
    @JsonProperty("introspection_endpoint")
    private String introspectionEndpoint;

    /**
     * URL to the OAuth2 token revocation endpoint.
     */
    @JsonProperty("revocation_endpoint")
    private String revocationEndpoint;

    /**
     * URL to the OAuth2 user info endpoint.
     */
    @JsonProperty("userinfo_endpoint")
    private String userinfoEndpoint;

    /**
     * URL to the JWKS endpoint for token verification.
     */
    @JsonProperty("jwks_uri")
    private String jwksUri;

    /**
     * URL where the server's capability statement may be found.
     */
    @JsonProperty("capabilities")
    private List<String> capabilities;

    /**
     * OAuth 2.0 scopes supported by the server.
     */
    @JsonProperty("scopes_supported")
    private List<String> scopesSupported;

    /**
     * OAuth 2.0 response_type values supported.
     */
    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported;

    /**
     * Client authentication methods supported at the token endpoint.
     */
    @JsonProperty("token_endpoint_auth_methods_supported")
    private List<String> tokenEndpointAuthMethodsSupported;

    /**
     * Grant types supported by the server.
     */
    @JsonProperty("grant_types_supported")
    private List<String> grantTypesSupported;

    /**
     * Code challenge methods supported for PKCE.
     */
    @JsonProperty("code_challenge_methods_supported")
    private List<String> codeChallengeMethodsSupported;

    /**
     * URL to the registration endpoint for dynamic client registration.
     */
    @JsonProperty("registration_endpoint")
    private String registrationEndpoint;

    /**
     * URL to the management endpoint for managing client registrations.
     */
    @JsonProperty("management_endpoint")
    private String managementEndpoint;
}
