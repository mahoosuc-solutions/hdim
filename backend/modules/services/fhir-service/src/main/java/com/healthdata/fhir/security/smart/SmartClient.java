package com.healthdata.fhir.security.smart;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * SMART on FHIR Client Registration.
 *
 * Represents a registered SMART/OAuth2 client application.
 * Supports both confidential (server-side) and public (browser/mobile) clients.
 */
@Entity
@Table(name = "smart_clients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartClient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Client ID used in OAuth flows.
     */
    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;

    /**
     * Client secret for confidential clients.
     * Null for public clients.
     */
    @Column(name = "client_secret")
    private String clientSecret;

    /**
     * Human-readable client name.
     */
    @Column(name = "client_name", nullable = false)
    private String clientName;

    /**
     * Client type: CONFIDENTIAL or PUBLIC.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false)
    private ClientType clientType;

    /**
     * Allowed redirect URIs for authorization flow.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "smart_client_redirect_uris", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "redirect_uri")
    private Set<String> redirectUris;

    /**
     * Scopes this client is allowed to request.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "smart_client_scopes", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "scope")
    private Set<String> allowedScopes;

    /**
     * Grant types this client can use.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "smart_client_grant_types", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "grant_type")
    private Set<String> grantTypes;

    /**
     * Token endpoint authentication method.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "token_endpoint_auth_method")
    private TokenEndpointAuthMethod tokenEndpointAuthMethod;

    /**
     * JWKS URI for asymmetric client authentication.
     */
    @Column(name = "jwks_uri")
    private String jwksUri;

    /**
     * Client description.
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Logo URI for consent screens.
     */
    @Column(name = "logo_uri")
    private String logoUri;

    /**
     * Client URI (home page).
     */
    @Column(name = "client_uri")
    private String clientUri;

    /**
     * Policy URI for privacy policy.
     */
    @Column(name = "policy_uri")
    private String policyUri;

    /**
     * Terms of service URI.
     */
    @Column(name = "tos_uri")
    private String tosUri;

    /**
     * Tenant ID for multi-tenant support.
     */
    @Column(name = "tenant_id")
    private String tenantId;

    /**
     * Whether the client is active.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Whether this client requires PKCE.
     */
    @Column(name = "require_pkce")
    @Builder.Default
    private boolean requirePkce = false;

    /**
     * Access token lifetime in seconds.
     */
    @Column(name = "access_token_lifetime")
    @Builder.Default
    private int accessTokenLifetime = 3600; // 1 hour

    /**
     * Refresh token lifetime in seconds.
     */
    @Column(name = "refresh_token_lifetime")
    @Builder.Default
    private int refreshTokenLifetime = 86400; // 24 hours

    /**
     * Creation timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Client types as per OAuth 2.0 spec.
     */
    public enum ClientType {
        /**
         * Server-side applications that can securely store secrets.
         */
        CONFIDENTIAL,

        /**
         * Browser-based or mobile apps that cannot securely store secrets.
         */
        PUBLIC
    }

    /**
     * Token endpoint authentication methods.
     */
    public enum TokenEndpointAuthMethod {
        /**
         * HTTP Basic authentication with client_id:client_secret.
         */
        CLIENT_SECRET_BASIC,

        /**
         * Client credentials in request body.
         */
        CLIENT_SECRET_POST,

        /**
         * JWT signed with client secret (HMAC).
         */
        CLIENT_SECRET_JWT,

        /**
         * JWT signed with private key.
         */
        PRIVATE_KEY_JWT,

        /**
         * No authentication (public clients).
         */
        NONE
    }

    /**
     * Check if this client supports a specific grant type.
     */
    public boolean supportsGrantType(String grantType) {
        return grantTypes != null && grantTypes.contains(grantType);
    }

    /**
     * Check if this client is allowed to request a specific scope.
     */
    public boolean isAllowedScope(String scope) {
        return allowedScopes != null && allowedScopes.contains(scope);
    }

    /**
     * Check if the redirect URI is valid for this client.
     */
    public boolean isValidRedirectUri(String redirectUri) {
        return redirectUris != null && redirectUris.contains(redirectUri);
    }

    /**
     * Check if this is a public client.
     */
    public boolean isPublicClient() {
        return clientType == ClientType.PUBLIC;
    }

    /**
     * Check if this is a confidential client.
     */
    public boolean isConfidentialClient() {
        return clientType == ClientType.CONFIDENTIAL;
    }
}
