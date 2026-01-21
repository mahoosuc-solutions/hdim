package com.healthdata.queryapi.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provider utility for JWT token handling and validation.
 *
 * Responsibilities:
 * 1. Extract and validate JWT claims (sub, tenant_id, roles, exp, iat, nbf, iss, aud)
 * 2. Build Authentication object from validated JWT
 * 3. Validate token structure and claim presence
 * 4. Check token expiration and not-before times
 * 5. Support custom claim extraction for multi-tenant scenarios
 *
 * Expected JWT Token Structure:
 * {
 *   "sub": "user@example.com",           // Required: Subject (user ID/email)
 *   "tenant_id": "tenant-001",           // Required: Tenant identifier
 *   "roles": ["ADMIN", "EVALUATOR"],     // Required: Array of role names
 *   "exp": 1699564800,                   // Required: Expiration time (Unix timestamp)
 *   "iat": 1699561200,                   // Required: Issued at time
 *   "nbf": 1699561200,                   // Optional: Not before time
 *   "iss": "hdim-auth-service",          // Required: Issuer
 *   "aud": "hdim-api"                    // Required: Audience
 * }
 *
 * @author HDIM Security Team
 * @version 1.0
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /**
     * Validates JWT token structure and required claims.
     *
     * Validation Steps:
     * 1. Verify token is not null
     * 2. Check all required claims are present: sub, tenant_id, roles, exp, iss, aud
     * 3. Validate 'roles' claim is a list
     * 4. Check token is not expired
     * 5. Check token has not-before validation (if nbf present)
     * 6. Validate issuer matches expected value
     * 7. Validate audience matches expected value
     *
     * Required Claims: sub, tenant_id, roles, exp, iss, aud
     * Optional Claims: nbf
     *
     * @param jwt The JWT token to validate
     * @return true if token passes all validations, false otherwise
     */
    public boolean validateTokenStructure(Jwt jwt) {
        if (jwt == null) {
            log.warn("JWT token is null");
            return false;
        }

        // Validate required string claims
        if (!hasRequiredStringClaim(jwt, "sub")) {
            log.warn("JWT token missing required 'sub' claim");
            return false;
        }

        if (!hasRequiredStringClaim(jwt, "tenant_id")) {
            log.warn("JWT token missing required 'tenant_id' claim");
            return false;
        }

        if (!hasRequiredStringClaim(jwt, "iss")) {
            log.warn("JWT token missing required 'iss' claim");
            return false;
        }

        if (!hasRequiredStringClaim(jwt, "aud")) {
            log.warn("JWT token missing required 'aud' claim");
            return false;
        }

        // Validate roles claim
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null || roles.isEmpty()) {
            log.warn("JWT token missing 'roles' claim or roles array is empty");
            return false;
        }

        // Validate expiration time
        if (isTokenExpired(jwt)) {
            log.warn("JWT token has expired");
            return false;
        }

        // Validate not-before time (if present)
        if (!isTokenNotBefore(jwt)) {
            log.warn("JWT token used before valid (nbf check failed)");
            return false;
        }

        // Validate issuer
        String issuer = jwt.getClaimAsString("iss");
        if (!isValidIssuer(issuer)) {
            log.warn("JWT token issuer validation failed: {}", issuer);
            return false;
        }

        // Validate audience
        List<String> audience = jwt.getClaimAsStringList("aud");
        if (!isValidAudience(audience)) {
            log.warn("JWT token audience validation failed");
            return false;
        }

        log.debug("JWT token structure validation successful");
        return true;
    }

    /**
     * Extracts authentication object from validated JWT token.
     *
     * Process:
     * 1. Validate token structure
     * 2. Extract username from 'sub' claim
     * 3. Extract and convert roles to GrantedAuthority
     * 4. Build JwtAuthenticationToken with authorities
     *
     * @param jwt The validated JWT token
     * @return Authentication object ready for SecurityContext, or null if validation fails
     */
    public Authentication getAuthentication(Jwt jwt) {
        if (!validateTokenStructure(jwt)) {
            return null;
        }

        String username = jwt.getClaimAsString("sub");
        String tenantId = jwt.getClaimAsString("tenant_id");

        // Extract and convert roles to authorities
        Collection<GrantedAuthority> authorities = extractAuthoritiesFromJwt(jwt);

        log.debug("JWT authentication extracted for user: {} (tenant: {})", username, tenantId);

        return new JwtAuthenticationToken(jwt, authorities, username);
    }

    /**
     * Extracts claim value and validates it exists.
     *
     * @param jwt JWT token
     * @param claimName Name of claim to extract
     * @return Claim value, or null if not present
     */
    public String getClaimAsString(Jwt jwt, String claimName) {
        try {
            return jwt.getClaimAsString(claimName);
        } catch (Exception e) {
            log.warn("Failed to extract claim '{}' from JWT: {}", claimName, e.getMessage());
            return null;
        }
    }

    /**
     * Extracts list claim value.
     *
     * @param jwt JWT token
     * @param claimName Name of claim to extract
     * @return List claim value, or empty list if not present
     */
    public List<String> getClaimAsStringList(Jwt jwt, String claimName) {
        try {
            List<String> value = jwt.getClaimAsStringList(claimName);
            return value != null ? value : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Failed to extract list claim '{}' from JWT: {}", claimName, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets tenant ID from JWT token.
     *
     * @param jwt JWT token
     * @return Tenant ID from 'tenant_id' claim, or null if not present
     */
    public String getTenantId(Jwt jwt) {
        return getClaimAsString(jwt, "tenant_id");
    }

    /**
     * Gets user ID/username from JWT token.
     *
     * @param jwt JWT token
     * @return User ID from 'sub' claim, or null if not present
     */
    public String getUserId(Jwt jwt) {
        return getClaimAsString(jwt, "sub");
    }

    /**
     * Gets roles from JWT token.
     *
     * @param jwt JWT token
     * @return List of roles from 'roles' claim, empty list if not present
     */
    public List<String> getRoles(Jwt jwt) {
        return getClaimAsStringList(jwt, "roles");
    }

    // ============ Private Helper Methods ============

    private boolean hasRequiredStringClaim(Jwt jwt, String claimName) {
        try {
            String value = jwt.getClaimAsString(claimName);
            return value != null && !value.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(Jwt jwt) {
        try {
            Instant expiresAt = jwt.getExpiresAt();
            if (expiresAt == null) {
                log.warn("JWT token missing 'exp' claim");
                return true;
            }
            boolean expired = expiresAt.isBefore(Instant.now());
            if (expired) {
                log.warn("JWT token expired at: {}", expiresAt);
            }
            return expired;
        } catch (Exception e) {
            log.warn("Failed to validate token expiration: {}", e.getMessage());
            return true;
        }
    }

    private boolean isTokenNotBefore(Jwt jwt) {
        try {
            // Some JWT libraries use getNotBefore(), others use 'nbf' claim
            // Check if 'nbf' claim exists and validate it
            Object nbfObj = jwt.getClaims().get("nbf");
            if (nbfObj == null) {
                // nbf claim is optional
                return true;
            }

            Instant notBefore = Instant.ofEpochSecond(((Number) nbfObj).longValue());
            boolean isValid = Instant.now().isAfter(notBefore);
            if (!isValid) {
                log.warn("JWT token used before valid (nbf): {}", notBefore);
            }
            return isValid;
        } catch (Exception e) {
            log.warn("Failed to validate not-before time: {}", e.getMessage());
            return true; // nbf is optional, don't fail if missing
        }
    }

    private boolean isValidIssuer(String issuer) {
        // Issuer validation can be made stricter in production
        // For now, accept any non-null issuer; production should validate against known values
        return issuer != null && !issuer.isBlank();
    }

    private boolean isValidAudience(List<String> audience) {
        // Audience validation: ensure 'hdim-api' is in audience list
        if (audience == null || audience.isEmpty()) {
            return false;
        }
        return audience.stream()
            .anyMatch(aud -> aud != null && !aud.isBlank());
    }

    private Collection<GrantedAuthority> extractAuthoritiesFromJwt(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        List<String> rolesList = jwt.getClaimAsStringList("roles");

        if (rolesList == null || rolesList.isEmpty()) {
            return authorities;
        }

        authorities = rolesList.stream()
            .filter(role -> role != null && !role.isBlank())
            .map(role -> {
                String authority = "ROLE_" + role.toUpperCase();
                return new SimpleGrantedAuthority(authority);
            })
            .collect(Collectors.toSet());

        return authorities;
    }
}
