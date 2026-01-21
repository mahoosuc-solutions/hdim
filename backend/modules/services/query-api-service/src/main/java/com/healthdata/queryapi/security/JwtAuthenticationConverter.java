package com.healthdata.queryapi.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security converter that transforms JWT tokens into authenticated principals.
 *
 * This converter is invoked after JWT signature validation to extract claims and
 * build the authentication context used for authorization decisions.
 *
 * Responsibilities:
 * 1. Extract user identity from 'sub' claim (email/ID)
 * 2. Extract roles from 'roles' claim (as JSON array)
 * 3. Convert roles to Spring Security GrantedAuthority objects
 * 4. Build JwtAuthenticationToken for SecurityContext
 *
 * Expected JWT Token Structure:
 * {
 *   "sub": "user@example.com",
 *   "tenant_id": "tenant-001",
 *   "roles": ["ADMIN", "EVALUATOR"],
 *   "exp": 1699564800,
 *   "iat": 1699561200,
 *   "iss": "hdim-auth-service",
 *   "aud": "hdim-api"
 * }
 *
 * @author HDIM Security Team
 * @version 1.0
 */
@Slf4j
@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * Converts JWT token to JwtAuthenticationToken with authorities.
     *
     * Process:
     * 1. Extract username from 'sub' claim
     * 2. Extract and validate roles array
     * 3. Map role strings to GrantedAuthority objects (ROLE_ prefix added by Spring)
     * 4. Build JwtAuthenticationToken with authentication name and authorities
     * 5. Return token ready for SecurityContext
     *
     * Security Notes:
     * - Empty roles result in empty authority collection (no default roles granted)
     * - Missing 'sub' claim falls back to "unknown" (will fail authorization checks)
     * - Missing 'roles' claim results in empty authority collection
     * - Role names are case-sensitive (typically uppercase)
     *
     * @param jwt The validated JWT token containing claims
     * @return JwtAuthenticationToken with authorities ready for SecurityContext
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        log.debug("Converting JWT token to authentication token");

        // Extract username from 'sub' claim (Subject - typically user email or ID)
        String username = jwt.getClaimAsString("sub");
        if (username == null) {
            log.warn("JWT token missing 'sub' claim, falling back to 'unknown'");
            username = "unknown";
        }

        // Extract tenant ID for audit/logging purposes
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId != null) {
            log.debug("JWT token authenticated for tenant: {}", tenantId);
        }

        // Extract roles from 'roles' claim (expected to be a JSON array)
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        log.debug("JWT authentication successful for user: {} with authorities: {}",
                 username, authorities.stream()
                     .map(GrantedAuthority::getAuthority)
                     .collect(Collectors.joining(", ")));

        // Create and return authentication token
        // The name will be used as the principal in SecurityContext
        return new JwtAuthenticationToken(jwt, authorities, username);
    }

    /**
     * Extracts roles from JWT 'roles' claim and converts to Spring GrantedAuthority.
     *
     * Conversion Logic:
     * 1. Get 'roles' claim as a list of strings
     * 2. Filter out null/empty role names
     * 3. Prefix each role with "ROLE_" (Spring Security convention)
     * 4. Create SimpleGrantedAuthority for each role
     *
     * Example:
     * JWT roles: ["ADMIN", "EVALUATOR"]
     * Resulting authorities: [ROLE_ADMIN, ROLE_EVALUATOR]
     *
     * This allows for both:
     * @PreAuthorize("hasRole('ADMIN')")  // Matches ROLE_ADMIN authority
     * @PreAuthorize("hasRole('EVALUATOR')")  // Matches ROLE_EVALUATOR authority
     *
     * Error Handling:
     * - Missing 'roles' claim: Returns empty collection (no authorities)
     * - Null role in array: Filtered out
     * - Empty role string: Filtered out
     *
     * @param jwt JWT token containing 'roles' claim
     * @return Collection of GrantedAuthority objects representing user roles
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Get 'roles' claim as List (will be null if claim doesn't exist)
        List<String> rolesList = jwt.getClaimAsStringList("roles");

        if (rolesList == null || rolesList.isEmpty()) {
            log.debug("JWT token has no 'roles' claim or empty roles array");
            return authorities;
        }

        // Convert each role string to GrantedAuthority with ROLE_ prefix
        authorities = rolesList.stream()
            .filter(role -> role != null && !role.isBlank())
            .map(role -> {
                // Spring Security convention: prefix role names with "ROLE_"
                // This allows both hasRole('ADMIN') and hasAuthority('ROLE_ADMIN') to work
                String authority = "ROLE_" + role.toUpperCase();
                return new SimpleGrantedAuthority(authority);
            })
            .collect(Collectors.toSet());

        log.debug("Extracted {} authorities from JWT token", authorities.size());
        return authorities;
    }
}
