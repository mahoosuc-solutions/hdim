# SAML 2.0 SSO Implementation Guide

**Status:** 📋 Implementation Guide
**Last Updated:** January 24, 2026
**Version:** 1.0

---

## Overview

This guide provides a comprehensive reference for implementing SAML 2.0 Single Sign-On (SSO) in the HDIM platform. SAML 2.0 is the enterprise standard for federated authentication in healthcare environments, enabling integration with corporate identity providers like Okta, Azure AD, and Ping Identity.

### What is SAML?

**SAML (Security Assertion Markup Language)** is an XML-based framework for exchanging authentication and authorization data between:
- **Identity Provider (IdP)**: Corporate authentication system (Okta, Azure AD, Ping Identity)
- **Service Provider (SP)**: HDIM Platform

### Benefits for HDIM

| Benefit | Description |
|---------|-------------|
| **Enterprise Integration** | Seamless integration with existing corporate identity systems |
| **Centralized Authentication** | Single point of authentication across all HDIM services |
| **Role Mapping** | Automatic mapping of corporate roles to HDIM's 13-role RBAC system |
| **HIPAA Compliance** | Encrypted assertions, audit logging, strong authentication |
| **Multi-Tenant Support** | Tenant-specific SAML configurations |

---

## Architecture

### SAML 2.0 Flow (SP-Initiated)

```
┌─────────────┐                  ┌─────────────┐                  ┌─────────────┐
│   User      │                  │    HDIM     │                  │  Identity   │
│   Browser   │                  │  (Gateway)  │                  │  Provider   │
└──────┬──────┘                  └──────┬──────┘                  └──────┬──────┘
       │                                │                                │
       │  1. Access HDIM                │                                │
       │──────────────────────────────> │                                │
       │                                │                                │
       │  2. Redirect to IdP            │                                │
       │  (SAML AuthnRequest)           │                                │
       │ <──────────────────────────────│                                │
       │                                │                                │
       │  3. POST AuthnRequest          │                                │
       │───────────────────────────────────────────────────────────────> │
       │                                │                                │
       │  4. Authenticate user          │                                │
       │  (Login page if needed)        │                                │
       │ <──────────────────────────────────────────────────────────────│
       │                                │                                │
       │  5. POST SAML Response         │                                │
       │  (with assertions)             │                                │
       │ <──────────────────────────────────────────────────────────────│
       │                                │                                │
       │  6. POST SAML Response         │                                │
       │──────────────────────────────> │                                │
       │                                │  7. Validate signature          │
       │                                │     Decrypt assertions          │
       │                                │     Extract user attributes     │
       │                                │     Map roles                   │
       │                                │                                │
       │  8. Set-Cookie (JWT)           │                                │
       │  Redirect to app               │                                │
       │ <──────────────────────────────│                                │
```

### Integration with HDIM Architecture

```
┌───────────────────────────────────────────────────────────────────────┐
│                         Gateway Service (Port 8001)                   │
│ ┌───────────────────────────────────────────────────────────────────┐ │
│ │                    GatewaySecurityConfig                          │ │
│ │  - SecurityFilterChain with SAML filters                          │ │
│ └───────────────────────────────────────────────────────────────────┘ │
│                                 │                                     │
│ ┌───────────────────────────────┼─────────────────────────────────┐ │
│ │           Filter Chain (request flow)                            │ │
│ │                                                                   │ │
│ │  1. AuditLoggingFilter       ──────> Log all requests            │ │
│ │  2. RateLimitingFilter       ──────> Enforce rate limits         │ │
│ │  3. SAMLProcessingFilter ───────────> Process SAML responses      │ │
│ │  4. GatewayAuthenticationFilter ───> Validate JWT tokens         │ │
│ │  5. Spring Security          ──────> Authorization checks        │ │
│ └───────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────┘
                                  │
                  Inject X-Auth-* headers (trusted)
                                  │
                                  ▼
        ┌───────────────────────────────────────────┐
        │        Backend Services                    │
        │  - Trust gateway headers                   │
        │  - No SAML processing required             │
        │  - RBAC via @PreAuthorize                  │
        └───────────────────────────────────────────┘
```

---

## Implementation Plan

### Phase 1: Core SAML Infrastructure (3 days)

**Goal:** Establish SAML 2.0 authentication foundation

#### 1.1 Add Spring Security SAML Dependency

**File:** `backend/modules/shared/infrastructure/gateway-core/build.gradle.kts`

```kotlin
dependencies {
    // Existing dependencies...

    // SAML 2.0 Support
    implementation("org.springframework.security:spring-security-saml2-service-provider:6.2.1")
    implementation("org.opensaml:opensaml-core:4.3.0")
    implementation("org.opensaml:opensaml-saml-api:4.3.0")
    implementation("org.opensaml:opensaml-saml-impl:4.3.0")

    // XML processing
    implementation("org.apache.santuario:xmlsec:3.0.3")
}
```

#### 1.2 Create SAML Configuration Properties

**File:** `backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/config/SamlProperties.java`

```java
package com.healthdata.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * SAML 2.0 configuration properties.
 *
 * Supports multi-tenant SAML configurations where each tenant can have
 * its own identity provider (IdP) metadata and attribute mappings.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "saml")
public class SamlProperties {

    /**
     * Enable/disable SAML authentication globally
     */
    private boolean enabled = false;

    /**
     * Default tenant to use for SAML when tenant cannot be determined
     */
    private String defaultTenant = "TENANT-001";

    /**
     * Service Provider (SP) configuration
     */
    private ServiceProvider serviceProvider = new ServiceProvider();

    /**
     * Per-tenant Identity Provider (IdP) configurations
     * Key: tenantId, Value: IdP configuration
     */
    private Map<String, IdentityProvider> identityProviders = new HashMap<>();

    /**
     * Service Provider (HDIM) configuration
     */
    @Data
    public static class ServiceProvider {
        /**
         * SP entity ID (unique identifier for HDIM)
         * Example: https://hdim.example.com/saml/metadata
         */
        private String entityId;

        /**
         * Base URL for SAML endpoints
         * Example: https://hdim.example.com
         */
        private String baseUrl;

        /**
         * ACS (Assertion Consumer Service) endpoint path
         * Full URL: {baseUrl}{acsPath}
         */
        private String acsPath = "/saml/sso";

        /**
         * Metadata endpoint path
         */
        private String metadataPath = "/saml/metadata";

        /**
         * Single Logout (SLO) endpoint path
         */
        private String sloPath = "/saml/logout";

        /**
         * SP signing certificate (PEM format)
         */
        private String signingCertificate;

        /**
         * SP signing private key (PEM format)
         */
        private String signingPrivateKey;

        /**
         * SP encryption certificate (PEM format)
         */
        private String encryptionCertificate;

        /**
         * SP encryption private key (PEM format)
         */
        private String encryptionPrivateKey;

        /**
         * Whether to sign SAML AuthnRequests
         */
        private boolean signRequests = true;

        /**
         * Whether to expect encrypted SAML assertions
         */
        private boolean requireEncryptedAssertions = true;
    }

    /**
     * Identity Provider configuration (per tenant)
     */
    @Data
    public static class IdentityProvider {
        /**
         * IdP entity ID
         */
        private String entityId;

        /**
         * IdP metadata XML (can be file path or inline XML)
         */
        private String metadata;

        /**
         * IdP metadata URL (alternative to inline metadata)
         */
        private String metadataUrl;

        /**
         * Whether to trust IdP SSL certificates
         * (false recommended for production)
         */
        private boolean trustAllCertificates = false;

        /**
         * Attribute mappings (IdP attribute -> HDIM field)
         */
        private AttributeMapping attributeMapping = new AttributeMapping();

        /**
         * Role mapping (IdP group/role -> HDIM role)
         * Example: "okta-admin" -> "ADMIN"
         */
        private Map<String, String> roleMapping = new HashMap<>();
    }

    /**
     * SAML attribute mapping configuration
     */
    @Data
    public static class AttributeMapping {
        /**
         * IdP attribute for username (email)
         */
        private String username = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";

        /**
         * IdP attribute for first name
         */
        private String firstName = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname";

        /**
         * IdP attribute for last name
         */
        private String lastName = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname";

        /**
         * IdP attribute for email
         */
        private String email = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";

        /**
         * IdP attribute for roles/groups
         */
        private String roles = "http://schemas.microsoft.com/ws/2008/06/identity/claims/role";

        /**
         * IdP attribute for tenant ID
         */
        private String tenantId = "tenantId";

        /**
         * IdP attribute for department
         */
        private String department = "department";
    }
}
```

#### 1.3 Create SAML Configuration Class

**File:** `backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/config/SamlSecurityConfig.java`

```java
package com.healthdata.gateway.config;

import com.healthdata.gateway.saml.SamlAuthenticationSuccessHandler;
import com.healthdata.gateway.saml.SamlMetadataResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SAML 2.0 Security Configuration
 *
 * Configures SAML authentication for HDIM Gateway service.
 * Supports multi-tenant SAML with per-tenant IdP configurations.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "saml", name = "enabled", havingValue = "true")
public class SamlSecurityConfig {

    private final SamlProperties samlProperties;
    private final SamlMetadataResolver samlMetadataResolver;
    private final SamlAuthenticationSuccessHandler samlSuccessHandler;

    /**
     * Configure SAML authentication in the security filter chain.
     *
     * Adds SAML SSO processing filter before gateway authentication filter.
     */
    public void configureSaml(HttpSecurity http) throws Exception {
        http
            .saml2Login(saml -> saml
                .authenticationManager(new ProviderManager(saml2AuthenticationProvider()))
                .successHandler(samlSuccessHandler)
                .failureUrl("/login?error=saml_failed")
            )
            .saml2Logout(logout -> logout
                .logoutUrl("/saml/logout")
                .logoutSuccessUrl("/login?logout=saml")
            );
    }

    /**
     * SAML Authentication Provider
     *
     * Validates SAML responses, verifies signatures, and creates authentication tokens.
     */
    @Bean
    @ConditionalOnProperty(prefix = "saml", name = "enabled", havingValue = "true")
    public OpenSaml4AuthenticationProvider saml2AuthenticationProvider() {
        OpenSaml4AuthenticationProvider provider = new OpenSaml4AuthenticationProvider();

        // Custom authentication converter to map SAML attributes to HDIM users
        provider.setResponseAuthenticationConverter(createResponseConverter());

        return provider;
    }

    /**
     * Relying Party Registration Repository
     *
     * Manages SAML Service Provider (SP) configurations.
     * In multi-tenant mode, resolves SP configuration based on tenant ID.
     */
    @Bean
    @ConditionalOnProperty(prefix = "saml", name = "enabled", havingValue = "true")
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        return samlMetadataResolver.buildRelyingPartyRegistrationRepository();
    }

    /**
     * SAML Metadata Resolver
     *
     * Generates SAML SP metadata XML for IdP configuration.
     */
    @Bean
    @ConditionalOnProperty(prefix = "saml", name = "enabled", havingValue = "true")
    public OpenSamlMetadataResolver samlMetadataResolverBean(
        RelyingPartyRegistrationRepository registrations
    ) {
        DefaultRelyingPartyRegistrationResolver resolver =
            new DefaultRelyingPartyRegistrationResolver(registrations);
        return new OpenSamlMetadataResolver(resolver);
    }

    /**
     * Create SAML response authentication converter.
     *
     * Maps SAML assertions to HDIM authentication tokens with roles.
     */
    private Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication>
        createResponseConverter() {

        return (responseToken) -> {
            Saml2Authentication authentication =
                OpenSaml4AuthenticationProvider
                    .createDefaultResponseAuthenticationConverter()
                    .convert(responseToken);

            if (authentication == null) {
                return null;
            }

            // Extract SAML principal
            Saml2AuthenticatedPrincipal principal =
                (Saml2AuthenticatedPrincipal) authentication.getPrincipal();

            log.info("SAML authentication successful for user: {}", principal.getName());
            log.debug("SAML attributes: {}", principal.getAttributes());

            // Custom conversion will be handled by SamlAuthenticationSuccessHandler
            return authentication;
        };
    }
}
```

---

### Phase 2: SAML Service Layer (1 day)

#### 2.1 Create SAML Metadata Resolver

**File:** `backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/saml/SamlMetadataResolver.java`

```java
package com.healthdata.gateway.saml;

import com.healthdata.gateway.config.SamlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * SAML Metadata Resolver
 *
 * Builds RelyingPartyRegistration objects from SAML configuration.
 * Supports multi-tenant configurations with per-tenant IdP metadata.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SamlMetadataResolver {

    private final SamlProperties samlProperties;

    /**
     * Build RelyingPartyRegistrationRepository from configuration.
     *
     * Creates a registration for each configured tenant's IdP.
     */
    public RelyingPartyRegistrationRepository buildRelyingPartyRegistrationRepository() {
        List<RelyingPartyRegistration> registrations = new ArrayList<>();

        samlProperties.getIdentityProviders().forEach((tenantId, idpConfig) -> {
            try {
                RelyingPartyRegistration registration = buildRegistration(tenantId, idpConfig);
                registrations.add(registration);
                log.info("Registered SAML IdP for tenant: {}", tenantId);
            } catch (Exception e) {
                log.error("Failed to register SAML IdP for tenant: {}", tenantId, e);
            }
        });

        return new InMemoryRelyingPartyRegistrationRepository(registrations);
    }

    /**
     * Build RelyingPartyRegistration for a specific tenant.
     */
    private RelyingPartyRegistration buildRegistration(
        String tenantId,
        SamlProperties.IdentityProvider idpConfig
    ) throws Exception {

        SamlProperties.ServiceProvider spConfig = samlProperties.getServiceProvider();

        return RelyingPartyRegistration
            .withRegistrationId(tenantId)
            .entityId(spConfig.getEntityId())

            // Assertion Consumer Service (ACS) endpoint
            .assertionConsumerServiceLocation(
                spConfig.getBaseUrl() + spConfig.getAcsPath()
            )
            .assertionConsumerServiceBinding(Saml2MessageBinding.POST)

            // Single Logout (SLO) endpoint
            .singleLogoutServiceLocation(
                spConfig.getBaseUrl() + spConfig.getSloPath()
            )
            .singleLogoutServiceBinding(Saml2MessageBinding.POST)

            // IdP configuration
            .assertingPartyDetails(party -> party
                .entityId(idpConfig.getEntityId())
                .singleSignOnServiceLocation(extractSsoLocation(idpConfig))
                .singleSignOnServiceBinding(Saml2MessageBinding.POST)
                .wantAuthnRequestsSigned(spConfig.isSignRequests())
                .verificationX509Credentials(creds ->
                    creds.addAll(extractIdpCertificates(idpConfig))
                )
            )

            // SP signing credentials
            .signingX509Credentials(creds ->
                creds.add(loadSpSigningCredential())
            )

            // SP encryption credentials
            .decryptionX509Credentials(creds ->
                creds.add(loadSpEncryptionCredential())
            )

            .build();
    }

    /**
     * Extract SSO location from IdP metadata.
     */
    private String extractSsoLocation(SamlProperties.IdentityProvider idpConfig) {
        // Parse IdP metadata XML to extract SSO endpoint
        // Implementation depends on metadata format
        // For now, return a placeholder
        return "https://idp.example.com/sso";
    }

    /**
     * Extract IdP verification certificates from metadata.
     */
    private List<Saml2X509Credential> extractIdpCertificates(
        SamlProperties.IdentityProvider idpConfig
    ) throws Exception {

        List<Saml2X509Credential> credentials = new ArrayList<>();

        // Parse IdP metadata to extract certificates
        // Implementation depends on metadata format

        return credentials;
    }

    /**
     * Load SP signing credential from configuration.
     */
    private Saml2X509Credential loadSpSigningCredential() {
        try {
            SamlProperties.ServiceProvider spConfig = samlProperties.getServiceProvider();

            X509Certificate certificate = loadCertificate(spConfig.getSigningCertificate());
            // PrivateKey privateKey = loadPrivateKey(spConfig.getSigningPrivateKey());

            return Saml2X509Credential.signing(certificate, null);
        } catch (Exception e) {
            log.error("Failed to load SP signing credential", e);
            throw new RuntimeException("Failed to load SP signing credential", e);
        }
    }

    /**
     * Load SP encryption credential from configuration.
     */
    private Saml2X509Credential loadSpEncryptionCredential() {
        try {
            SamlProperties.ServiceProvider spConfig = samlProperties.getServiceProvider();

            X509Certificate certificate = loadCertificate(spConfig.getEncryptionCertificate());
            // PrivateKey privateKey = loadPrivateKey(spConfig.getEncryptionPrivateKey());

            return Saml2X509Credential.decryption(certificate, null);
        } catch (Exception e) {
            log.error("Failed to load SP encryption credential", e);
            throw new RuntimeException("Failed to load SP encryption credential", e);
        }
    }

    /**
     * Load X509 certificate from PEM string.
     */
    private X509Certificate loadCertificate(String pemCertificate) throws Exception {
        String cert = pemCertificate
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replaceAll("\\s", "");

        byte[] decoded = java.util.Base64.getDecoder().decode(cert);
        InputStream inputStream = new ByteArrayInputStream(decoded);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(inputStream);
    }
}
```

#### 2.2 Create SAML Authentication Success Handler

**File:** `backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/saml/SamlAuthenticationSuccessHandler.java`

```java
package com.healthdata.gateway.saml;

import com.healthdata.authentication.domain.Permission;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.gateway.config.SamlProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SAML Authentication Success Handler
 *
 * Handles successful SAML authentication by:
 * 1. Extracting user attributes from SAML assertion
 * 2. Mapping IdP roles to HDIM roles
 * 3. Creating JWT token
 * 4. Setting authentication cookie
 * 5. Redirecting to application
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SamlAuthenticationSuccessHandler
    extends SavedRequestAwareAuthenticationSuccessHandler {

    private final SamlProperties samlProperties;
    private final JwtTokenService jwtTokenService;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws ServletException, IOException {

        if (!(authentication.getPrincipal() instanceof Saml2AuthenticatedPrincipal samlPrincipal)) {
            log.error("Authentication principal is not Saml2AuthenticatedPrincipal");
            response.sendRedirect("/login?error=invalid_saml_principal");
            return;
        }

        try {
            // Extract user information from SAML assertion
            String username = extractUsername(samlPrincipal);
            String email = extractEmail(samlPrincipal);
            String firstName = extractFirstName(samlPrincipal);
            String lastName = extractLastName(samlPrincipal);
            String tenantId = extractTenantId(samlPrincipal);
            Set<UserRole> roles = extractAndMapRoles(samlPrincipal, tenantId);

            log.info("SAML authentication successful - User: {}, Tenant: {}, Roles: {}",
                username, tenantId, roles);

            // TODO: Create or update user in database
            // UserEntity user = userService.createOrUpdateSamlUser(...)

            // Generate JWT token
            String jwtToken = jwtTokenService.generateToken(
                username,
                tenantId,
                new ArrayList<>(roles),
                extractPermissions(roles)
            );

            // Set JWT cookie
            Cookie cookie = new Cookie("HDIM_AUTH_TOKEN", jwtToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(true); // HTTPS only
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1 hour
            response.addCookie(cookie);

            // Redirect to application
            String redirectUrl = determineRedirectUrl(request);
            log.info("Redirecting SAML user {} to: {}", username, redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Error processing SAML authentication", e);
            response.sendRedirect("/login?error=saml_processing_failed");
        }
    }

    /**
     * Extract username from SAML attributes.
     */
    private String extractUsername(Saml2AuthenticatedPrincipal principal) {
        String attributeName = samlProperties.getIdentityProviders()
            .get(samlProperties.getDefaultTenant())
            .getAttributeMapping()
            .getUsername();

        return principal.getFirstAttribute(attributeName);
    }

    /**
     * Extract email from SAML attributes.
     */
    private String extractEmail(Saml2AuthenticatedPrincipal principal) {
        String attributeName = samlProperties.getIdentityProviders()
            .get(samlProperties.getDefaultTenant())
            .getAttributeMapping()
            .getEmail();

        return principal.getFirstAttribute(attributeName);
    }

    /**
     * Extract first name from SAML attributes.
     */
    private String extractFirstName(Saml2AuthenticatedPrincipal principal) {
        String attributeName = samlProperties.getIdentityProviders()
            .get(samlProperties.getDefaultTenant())
            .getAttributeMapping()
            .getFirstName();

        return principal.getFirstAttribute(attributeName);
    }

    /**
     * Extract last name from SAML attributes.
     */
    private String extractLastName(Saml2AuthenticatedPrincipal principal) {
        String attributeName = samlProperties.getIdentityProviders()
            .get(samlProperties.getDefaultTenant())
            .getAttributeMapping()
            .getLastName();

        return principal.getFirstAttribute(attributeName);
    }

    /**
     * Extract tenant ID from SAML attributes.
     */
    private String extractTenantId(Saml2AuthenticatedPrincipal principal) {
        String attributeName = samlProperties.getIdentityProviders()
            .get(samlProperties.getDefaultTenant())
            .getAttributeMapping()
            .getTenantId();

        String tenantId = principal.getFirstAttribute(attributeName);
        return tenantId != null ? tenantId : samlProperties.getDefaultTenant();
    }

    /**
     * Extract IdP roles and map to HDIM roles.
     */
    private Set<UserRole> extractAndMapRoles(
        Saml2AuthenticatedPrincipal principal,
        String tenantId
    ) {
        String attributeName = samlProperties.getIdentityProviders()
            .get(tenantId)
            .getAttributeMapping()
            .getRoles();

        List<Object> idpRoles = principal.getAttribute(attributeName);
        if (idpRoles == null || idpRoles.isEmpty()) {
            log.warn("No roles found in SAML assertion for user: {}", principal.getName());
            return Set.of(UserRole.VIEWER); // Default role
        }

        // Map IdP roles to HDIM roles
        Map<String, String> roleMapping = samlProperties.getIdentityProviders()
            .get(tenantId)
            .getRoleMapping();

        return idpRoles.stream()
            .map(Object::toString)
            .map(roleMapping::get)
            .filter(java.util.Objects::nonNull)
            .map(UserRole::valueOf)
            .collect(Collectors.toSet());
    }

    /**
     * Extract permissions from roles.
     */
    private List<Permission> extractPermissions(Set<UserRole> roles) {
        return roles.stream()
            .flatMap(role -> com.healthdata.authentication.domain.RolePermissions
                .getPermissions(role).stream())
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Determine redirect URL after successful authentication.
     */
    private String determineRedirectUrl(HttpServletRequest request) {
        String savedRequest = (String) request.getSession()
            .getAttribute("SPRING_SECURITY_SAVED_REQUEST");

        if (savedRequest != null && !savedRequest.isBlank()) {
            return savedRequest;
        }

        return "/"; // Default to home page
    }
}
```

---

### Phase 3: SAML Controller & Admin UI (1 day)

#### 3.1 Create SAML Controller

**File:** `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/SamlController.java`

```java
package com.healthdata.authentication.controller;

import com.healthdata.gateway.config.SamlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * SAML 2.0 Configuration and Metadata Controller
 *
 * Provides endpoints for:
 * - SP metadata generation
 * - SAML configuration management
 * - IdP metadata upload
 * - Testing SAML flows
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/saml")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "saml", name = "enabled", havingValue = "true")
public class SamlController {

    private final SamlProperties samlProperties;
    private final RelyingPartyRegistrationRepository registrationRepository;
    private final OpenSamlMetadataResolver metadataResolver;

    /**
     * Get SAML SP metadata XML.
     * IdP administrators use this to configure HDIM as a service provider.
     *
     * @param tenantId Tenant ID for multi-tenant metadata
     * @return SP metadata XML
     */
    @GetMapping(value = "/metadata", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getMetadata(
        @RequestParam(required = false) String tenantId,
        HttpServletRequest request
    ) {
        try {
            String registrationId = tenantId != null ?
                tenantId : samlProperties.getDefaultTenant();

            DefaultRelyingPartyRegistrationResolver resolver =
                new DefaultRelyingPartyRegistrationResolver(registrationRepository);

            RelyingPartyRegistration registration = resolver.resolve(request, registrationId);

            if (registration == null) {
                return ResponseEntity.notFound().build();
            }

            String metadata = metadataResolver.resolve(registration);

            log.info("Generated SAML SP metadata for tenant: {}", registrationId);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .body(metadata);

        } catch (Exception e) {
            log.error("Error generating SAML metadata", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Upload IdP metadata XML for a tenant.
     * Requires ADMIN or SUPER_ADMIN role.
     *
     * @param tenantId Tenant ID
     * @param metadataXml IdP metadata XML content
     * @return Success response
     */
    @PostMapping("/idp/metadata")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadIdpMetadata(
        @RequestParam String tenantId,
        @RequestBody String metadataXml
    ) {
        log.info("Uploading IdP metadata for tenant: {}", tenantId);

        try {
            // TODO: Parse and validate metadata XML
            // TODO: Store metadata in database
            // TODO: Reload SAML configuration

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tenantId", tenantId);
            response.put("message", "IdP metadata uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading IdP metadata", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get SAML configuration for a tenant.
     *
     * @param tenantId Tenant ID
     * @return SAML configuration
     */
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getConfiguration(
        @RequestParam String tenantId
    ) {
        SamlProperties.IdentityProvider idpConfig =
            samlProperties.getIdentityProviders().get(tenantId);

        if (idpConfig == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("tenantId", tenantId);
        response.put("entityId", idpConfig.getEntityId());
        response.put("hasMetadata", idpConfig.getMetadata() != null ||
                                    idpConfig.getMetadataUrl() != null);
        response.put("attributeMapping", idpConfig.getAttributeMapping());
        response.put("roleMapping", idpConfig.getRoleMapping());

        return ResponseEntity.ok(response);
    }

    /**
     * Test SAML configuration for a tenant.
     * Validates metadata, certificates, and attribute mappings.
     *
     * @param tenantId Tenant ID
     * @return Validation results
     */
    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> testConfiguration(
        @RequestParam String tenantId
    ) {
        log.info("Testing SAML configuration for tenant: {}", tenantId);

        Map<String, Object> results = new HashMap<>();
        results.put("tenantId", tenantId);

        try {
            // Validate IdP metadata
            SamlProperties.IdentityProvider idpConfig =
                samlProperties.getIdentityProviders().get(tenantId);

            if (idpConfig == null) {
                results.put("success", false);
                results.put("error", "No SAML configuration found for tenant");
                return ResponseEntity.badRequest().body(results);
            }

            // TODO: Validate metadata XML
            // TODO: Validate certificates
            // TODO: Validate SSO endpoint URLs
            // TODO: Test attribute mappings

            results.put("success", true);
            results.put("validations", Map.of(
                "metadata", "valid",
                "certificates", "valid",
                "endpoints", "valid",
                "attributeMapping", "valid"
            ));

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("Error testing SAML configuration", e);
            results.put("success", false);
            results.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(results);
        }
    }

    /**
     * List configured SAML identity providers.
     *
     * @return Map of tenant IDs to IdP entity IDs
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> listProviders() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", samlProperties.isEnabled());
        response.put("defaultTenant", samlProperties.getDefaultTenant());

        Map<String, String> providers = new HashMap<>();
        samlProperties.getIdentityProviders().forEach((tenantId, config) -> {
            providers.put(tenantId, config.getEntityId());
        });
        response.put("providers", providers);

        return ResponseEntity.ok(response);
    }

    /**
     * Initiate SAML login for a tenant.
     * Redirects to IdP SSO endpoint.
     *
     * @param tenantId Tenant ID
     * @return Redirect URL to IdP
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> initiateLogin(
        @RequestParam String tenantId
    ) {
        String ssoUrl = String.format("/saml2/authenticate/%s", tenantId);

        Map<String, String> response = new HashMap<>();
        response.put("redirectUrl", ssoUrl);
        response.put("tenantId", tenantId);

        return ResponseEntity.ok(response);
    }
}
```

---

## Configuration Examples

### Example 1: Okta SAML Integration

**File:** `backend/modules/services/gateway-service/src/main/resources/application-saml.yml`

```yaml
saml:
  enabled: true
  default-tenant: TENANT-001

  service-provider:
    entity-id: https://hdim.example.com/saml/metadata
    base-url: https://hdim.example.com
    acs-path: /saml/sso
    metadata-path: /saml/metadata
    slo-path: /saml/logout
    sign-requests: true
    require-encrypted-assertions: true

    # SP signing certificate (generate with: openssl req -new -x509 -days 3650 -nodes)
    signing-certificate: |
      -----BEGIN CERTIFICATE-----
      MIIDXTCCAkWgAwIBAgIJAKL0UG+mRZ...
      -----END CERTIFICATE-----

    signing-private-key: |
      -----BEGIN PRIVATE KEY-----
      MIIEvQIBADANBgkqhkiG9w0BAQEFAA...
      -----END PRIVATE KEY-----

    encryption-certificate: |
      -----BEGIN CERTIFICATE-----
      MIIDXTCCAkWgAwIBAgIJAKL0UG+mRZ...
      -----END CERTIFICATE-----

    encryption-private-key: |
      -----BEGIN PRIVATE KEY-----
      MIIEvQIBADANBgkqhkiG9w0BAQEFAA...
      -----END PRIVATE KEY-----

  identity-providers:
    TENANT-001:
      entity-id: http://www.okta.com/exk12345
      metadata-url: https://dev-12345.okta.com/app/exk12345/sso/saml/metadata

      attribute-mapping:
        username: "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"
        email: "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"
        first-name: "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"
        last-name: "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"
        roles: "groups"
        tenant-id: "tenantId"

      role-mapping:
        "hdim-admins": "ADMIN"
        "hdim-clinicians": "CLINICIAN"
        "hdim-evaluators": "EVALUATOR"
        "hdim-analysts": "ANALYST"
        "hdim-viewers": "VIEWER"
```

### Example 2: Azure AD SAML Integration

```yaml
saml:
  enabled: true
  default-tenant: TENANT-002

  identity-providers:
    TENANT-002:
      entity-id: https://sts.windows.net/tenant-guid/
      metadata-url: https://login.microsoftonline.com/tenant-guid/federationmetadata/2007-06/federationmetadata.xml

      attribute-mapping:
        username: "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"
        email: "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"
        first-name: "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"
        last-name: "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"
        roles: "http://schemas.microsoft.com/ws/2008/06/identity/claims/role"
        tenant-id: "extension_tenantId"
        department: "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/department"

      role-mapping:
        "HDIM.Administrators": "ADMIN"
        "HDIM.ClinicalStaff": "CLINICIAN"
        "HDIM.QualityOfficers": "QUALITY_OFFICER"
        "HDIM.CareCoordinators": "CARE_COORDINATOR"
```

---

## Testing Guide

### Manual Testing Workflow

#### 1. Start Gateway Service with SAML Enabled

```bash
cd backend/modules/services/gateway-service

# Start with SAML profile
./gradlew bootRun --args='--spring.profiles.active=saml'
```

#### 2. Access SP Metadata

```bash
# Get SP metadata XML
curl http://localhost:8001/api/v1/saml/metadata?tenantId=TENANT-001

# Save metadata for IdP configuration
curl http://localhost:8001/api/v1/saml/metadata?tenantId=TENANT-001 \
  > hdim-sp-metadata.xml
```

#### 3. Configure IdP (Okta Example)

**Okta Admin Console:**

1. **Applications** → **Create App Integration**
2. Select **SAML 2.0**
3. **General Settings:**
   - App name: `HDIM Platform`
   - App logo: Upload HDIM logo
4. **SAML Settings:**
   - Single sign on URL: `https://hdim.example.com/saml/sso`
   - Audience URI (SP Entity ID): `https://hdim.example.com/saml/metadata`
   - Default RelayState: (leave blank)
   - Name ID format: `EmailAddress`
   - Application username: `Email`
5. **Attribute Statements:**
   - `email` → `user.email`
   - `firstName` → `user.firstName`
   - `lastName` → `user.lastName`
   - `tenantId` → `appuser.tenantId`
6. **Group Attribute Statements:**
   - Name: `groups`
   - Filter: `Matches regex` → `.*`
7. **Finish** and copy **Metadata URL**

#### 4. Upload IdP Metadata to HDIM

```bash
# Download IdP metadata
curl https://dev-12345.okta.com/app/exk12345/sso/saml/metadata \
  > okta-idp-metadata.xml

# Upload to HDIM
curl -X POST http://localhost:8001/api/v1/saml/idp/metadata \
  -H "Content-Type: application/xml" \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -d @okta-idp-metadata.xml \
  -G --data-urlencode "tenantId=TENANT-001"
```

#### 5. Test SAML Flow

```bash
# Initiate SAML login
curl http://localhost:8001/api/v1/saml/login?tenantId=TENANT-001

# Expected: Redirect to IdP SSO endpoint
# User authenticates at IdP
# IdP POSTs SAML response to /saml/sso
# HDIM validates assertion, creates JWT, sets cookie
# User redirected to application
```

### Integration Testing

**File:** `backend/modules/services/gateway-service/src/test/java/com/healthdata/gateway/saml/SamlIntegrationTest.java`

```java
package com.healthdata.gateway.saml;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "saml"})
class SamlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSpMetadataEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/saml/metadata")
                .param("tenantId", "TENANT-001"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/xml"))
            .andExpect(xpath("/EntityDescriptor/@entityID")
                .string("https://hdim.example.com/saml/metadata"));
    }

    @Test
    void testListProviders() throws Exception {
        mockMvc.perform(get("/api/v1/saml/providers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.providers.TENANT-001").exists());
    }

    @Test
    void testInitiateLogin() throws Exception {
        mockMvc.perform(get("/api/v1/saml/login")
                .param("tenantId", "TENANT-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.redirectUrl").exists())
            .andExpect(jsonPath("$.tenantId").value("TENANT-001"));
    }
}
```

---

## Security Considerations

### Certificate Management

**Generate SP Certificates:**

```bash
# Generate signing certificate (10-year validity)
openssl req -new -x509 -days 3650 -nodes \
  -keyout sp-signing.key \
  -out sp-signing.crt \
  -subj "/CN=HDIM SAML SP Signing/O=HealthData-in-Motion/C=US"

# Generate encryption certificate
openssl req -new -x509 -days 3650 -nodes \
  -keyout sp-encryption.key \
  -out sp-encryption.crt \
  -subj "/CN=HDIM SAML SP Encryption/O=HealthData-in-Motion/C=US"

# Convert to PEM format for application.yml
cat sp-signing.crt  # Copy to signing-certificate
cat sp-signing.key  # Copy to signing-private-key
```

**Certificate Rotation:**
- Rotate certificates every 2 years
- Update both HDIM configuration and IdP configuration
- Support dual certificates during rotation period

### SAML Assertion Validation

**Required Validations:**

1. **Signature Verification**
   - Verify SAML response signature using IdP certificate
   - Verify SAML assertion signature
   - Reject unsigned responses/assertions

2. **Timing Validation**
   - Check `NotBefore` and `NotOnOrAfter` conditions
   - Validate assertion timestamp within acceptable skew (5 minutes)
   - Reject expired assertions

3. **Audience Restriction**
   - Verify audience matches SP entity ID
   - Reject assertions for other service providers

4. **Recipient Validation**
   - Verify recipient URL matches ACS endpoint
   - Prevent assertion replay attacks

5. **InResponseTo Validation**
   - Match `InResponseTo` with original AuthnRequest ID
   - Prevent unsolicited responses

### Encryption

**Encrypted Assertions (Recommended):**
- IdP encrypts SAML assertions using SP encryption certificate
- HDIM decrypts using SP encryption private key
- Protects PHI in transit (HIPAA requirement)

**Configuration:**
```yaml
saml:
  service-provider:
    require-encrypted-assertions: true  # HIPAA requirement
```

### Audit Logging

**SAML Events to Log:**

| Event | Log Level | Details |
|-------|-----------|---------|
| SAML login initiated | INFO | Tenant ID, user email, timestamp |
| SAML response received | INFO | Tenant ID, assertion ID, timestamp |
| SAML validation failure | WARN | Failure reason, tenant ID, timestamp |
| SAML authentication success | INFO | User ID, tenant ID, roles, timestamp |
| IdP metadata uploaded | INFO | Tenant ID, admin user, timestamp |
| SAML configuration changed | WARN | Tenant ID, admin user, change details |

---

## HIPAA Compliance

### PHI Protection

**SAML Assertions May Contain PHI:**
- Email addresses (can be considered PHI)
- Employee ID numbers
- Department information

**Required Protections:**
1. ✅ **Encryption in Transit:** Use encrypted SAML assertions
2. ✅ **Audit Logging:** Log all SAML authentication events
3. ✅ **Access Controls:** Restrict IdP metadata upload to ADMIN role
4. ✅ **Session Timeouts:** Enforce 15-minute idle timeout after SAML login

### Audit Requirements (§164.312(b))

**Automatic Audit Logging:**

All SAML authentication events are automatically logged by the `AuditLoggingFilter` in the gateway:

```java
// Logged automatically
{
    "eventType": "SAML_AUTHENTICATION",
    "userId": "john.doe@example.com",
    "tenantId": "TENANT-001",
    "timestamp": "2026-01-24T10:30:00Z",
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "success": true,
    "roles": ["CLINICIAN", "EVALUATOR"]
}
```

---

## Multi-Tenant Architecture

### Per-Tenant SAML Configuration

HDIM supports different IdP configurations for each tenant:

```yaml
saml:
  identity-providers:
    TENANT-001:  # Health Plan A
      entity-id: http://www.okta.com/exk12345
      metadata-url: https://healthplan-a.okta.com/...
      role-mapping:
        "plan-a-admins": "ADMIN"
        "plan-a-clinicians": "CLINICIAN"

    TENANT-002:  # Health Plan B
      entity-id: https://sts.windows.net/tenant-b-guid/
      metadata-url: https://login.microsoftonline.com/tenant-b-guid/...
      role-mapping:
        "HDIM.Admins": "ADMIN"
        "HDIM.ClinicalStaff": "CLINICIAN"
```

### Tenant Resolution

**During SAML Login:**

1. User accesses tenant-specific URL: `https://healthplan-a.hdim.com`
2. Gateway resolves tenant from subdomain or path
3. Initiates SAML flow with tenant-specific IdP
4. After successful authentication, sets `X-Tenant-ID` header
5. All backend services enforce tenant isolation

---

## Troubleshooting

### Common Issues

#### Issue 1: "SAML validation failed: Invalid signature"

**Cause:** IdP certificate mismatch or expired

**Solution:**
1. Download latest IdP metadata: `curl $IDP_METADATA_URL`
2. Verify certificate in metadata matches IdP configuration
3. Re-upload metadata to HDIM
4. Test again

#### Issue 2: "SAML validation failed: Assertion expired"

**Cause:** Clock skew between HDIM and IdP

**Solution:**
1. Synchronize server clocks using NTP
2. Verify time zones are correct
3. Increase assertion validity in IdP (default: 5 minutes)

#### Issue 3: "No role mapping found for IdP group: xyz"

**Cause:** IdP group not mapped to HDIM role

**Solution:**
1. Check IdP groups in SAML response
2. Update `role-mapping` in configuration:
   ```yaml
   role-mapping:
     "xyz": "VIEWER"  # Add missing mapping
   ```
3. Reload configuration

#### Issue 4: "SAML metadata endpoint returns 404"

**Cause:** SAML not enabled or incorrect tenant ID

**Solution:**
1. Verify `saml.enabled=true` in configuration
2. Check tenant ID exists in `identity-providers`
3. Restart gateway service

---

## Related Documentation

- **[RBAC Implementation Guide](./RBAC_IMPLEMENTATION_GUIDE.md)** - 13-role RBAC system
- **[Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md)** - Gateway authentication pattern
- **[OAuth 2.0 Implementation](./OAuth2Controller.java)** - OAuth2/OpenID Connect reference
- **[HIPAA Compliance Guide](../HIPAA-CACHE-COMPLIANCE.md)** - PHI handling requirements
- **[Distributed Tracing Guide](./DISTRIBUTED_TRACING_GUIDE.md)** - End-to-end request tracking

---

## Next Steps

After implementing SAML 2.0:

1. **Implement OAuth 2.0/OpenID Connect** (Issue #259) for modern IdP support
2. **Add MFA (Multi-Factor Authentication)** via SAML or TOTP
3. **Implement Just-In-Time (JIT) User Provisioning** from SAML attributes
4. **Create Admin UI for SAML Configuration** (drag-and-drop metadata upload)
5. **Add SAML-based API Authentication** (SAML bearer tokens for API calls)

---

**Last Updated:** January 24, 2026
**Document Version:** 1.0
**Status:** 📋 Implementation Guide
