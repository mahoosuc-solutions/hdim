package com.healthdata.queryapi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for JwtAuthenticationConverter (Phase 1.9 Team 1).
 *
 * Tests verify:
 * 1. JWT token is converted to JwtAuthenticationToken
 * 2. Username extracted from 'sub' claim
 * 3. Tenant ID extracted from 'tenant_id' claim
 * 4. Roles extracted from 'roles' claim array
 * 5. Roles converted to Spring authorities with ROLE_ prefix
 * 6. Empty roles handled gracefully
 * 7. Missing claims handled with defaults
 * 8. Authority names are uppercase
 *
 * @author HDIM Security Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationConverterTest {

    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JwtAuthenticationConverter();
    }

    /**
     * Test 1: Convert JWT with all claims successfully
     * Verifies complete JWT with sub, tenant_id, and roles is converted
     */
    @Test
    void shouldConvertJwtWithAllClaims() {
        // Create JWT with all expected claims
        Jwt jwt = createJwt(
            "user@example.com",
            "tenant-001",
            Arrays.asList("ADMIN", "EVALUATOR")
        );

        // Convert to authentication token
        var auth = converter.convert(jwt);

        // Verify result
        assertNotNull(auth);
        assertInstanceOf(JwtAuthenticationToken.class, auth);
        assertEquals("user@example.com", auth.getName());
        assertFalse(auth.getAuthorities().isEmpty());
    }

    /**
     * Test 2: Extract username from 'sub' claim
     * Verifies 'sub' claim is used as principal name
     */
    @Test
    void shouldExtractUsernameFromSubClaim() {
        Jwt jwt = createJwt("john.doe@example.com", "tenant-001", Arrays.asList("EVALUATOR"));
        var auth = converter.convert(jwt);

        assertEquals("john.doe@example.com", auth.getName());
    }

    /**
     * Test 3: Handle missing 'sub' claim
     * Verifies fallback when 'sub' claim is missing
     */
    @Test
    void shouldHandleMissingSubClaim() {
        Jwt jwt = createJwtWithoutSub("tenant-001", Arrays.asList("VIEWER"));
        var auth = converter.convert(jwt);

        // Should fall back to "unknown"
        assertEquals("unknown", auth.getName());
    }

    /**
     * Test 4: Extract all roles from 'roles' array
     * Verifies all roles in array are extracted
     */
    @Test
    void shouldExtractAllRolesFromArray() {
        List<String> roles = Arrays.asList("ADMIN", "EVALUATOR", "ANALYST");
        Jwt jwt = createJwt("user@example.com", "tenant-001", roles);

        var auth = converter.convert(jwt);
        Collection<GrantedAuthority> authorities = auth.getAuthorities();

        assertEquals(3, authorities.size());
    }

    /**
     * Test 5: Convert role names to ROLE_ prefixed authorities
     * Verifies Spring Security naming convention is applied
     */
    @Test
    void shouldPrefixRolesWithRolePrefix() {
        Jwt jwt = createJwt("user@example.com", "tenant-001", Arrays.asList("ADMIN"));
        var auth = converter.convert(jwt);

        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    /**
     * Test 6: Convert role names to uppercase
     * Verifies role names are normalized to uppercase
     */
    @Test
    void shouldConvertRolesToUppercase() {
        Jwt jwt = createJwt("user@example.com", "tenant-001", Arrays.asList("admin", "evaluator"));
        var auth = converter.convert(jwt);

        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_EVALUATOR")));
    }

    /**
     * Test 7: Handle empty roles array
     * Verifies empty authority collection when roles array is empty
     */
    @Test
    void shouldHandleEmptyRolesArray() {
        Jwt jwt = createJwt("user@example.com", "tenant-001", Arrays.asList());
        var auth = converter.convert(jwt);

        assertTrue(auth.getAuthorities().isEmpty());
    }

    /**
     * Test 8: Handle missing 'roles' claim
     * Verifies empty authority collection when 'roles' claim is missing
     */
    @Test
    void shouldHandleMissingRolesClaim() {
        Jwt jwt = createJwtWithoutRoles("user@example.com", "tenant-001");
        var auth = converter.convert(jwt);

        assertTrue(auth.getAuthorities().isEmpty());
    }

    /**
     * Test 9: Handle null values in roles array
     * Verifies null role values are filtered out
     */
    @Test
    void shouldFilterNullValuesFromRolesArray() {
        List<String> rolesWithNull = Arrays.asList("ADMIN", null, "EVALUATOR", null);
        Jwt jwt = createJwtWithCustomRoles("user@example.com", "tenant-001", rolesWithNull);
        var auth = converter.convert(jwt);

        // Should have 2 authorities (null values filtered)
        assertEquals(2, auth.getAuthorities().size());
    }

    /**
     * Test 10: Handle blank role strings
     * Verifies blank role strings are filtered out
     */
    @Test
    void shouldFilterBlankRoleStrings() {
        List<String> rolesWithBlanks = Arrays.asList("ADMIN", "", " ", "EVALUATOR");
        Jwt jwt = createJwtWithCustomRoles("user@example.com", "tenant-001", rolesWithBlanks);
        var auth = converter.convert(jwt);

        // Should have 2 authorities (blank strings filtered)
        assertEquals(2, auth.getAuthorities().size());
    }

    /**
     * Test 11: Multiple roles result in multiple authorities
     * Verifies hasAnyRole() will work correctly
     */
    @Test
    void shouldCreateMultipleAuthoritiesForMultipleRoles() {
        List<String> roles = Arrays.asList("SUPER_ADMIN", "ADMIN", "EVALUATOR");
        Jwt jwt = createJwt("user@example.com", "tenant-001", roles);

        var auth = converter.convert(jwt);

        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")));
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_EVALUATOR")));
    }

    /**
     * Test 12: Role hierarchy roles are properly named
     * Verifies all hierarchy roles (SUPER_ADMIN, ADMIN, EVALUATOR, ANALYST, VIEWER)
     * are properly converted
     */
    @Test
    void shouldHandleAllRoleHierarchyRoles() {
        List<String> roles = Arrays.asList("SUPER_ADMIN", "ADMIN", "EVALUATOR", "ANALYST", "VIEWER");
        Jwt jwt = createJwt("user@example.com", "tenant-001", roles);

        var auth = converter.convert(jwt);

        assertEquals(5, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream()
            .allMatch(a -> a.getAuthority().startsWith("ROLE_")));
    }

    // ============ Helper Methods ============

    /**
     * Create a mock JWT token with all claims
     */
    private Jwt createJwt(String sub, String tenantId, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("tenant_id", tenantId);
        claims.put("roles", roles);
        claims.put("exp", Instant.now().plusSeconds(3600));
        claims.put("iat", Instant.now());
        claims.put("iss", "hdim-auth-service");
        claims.put("aud", "hdim-api");

        return new Jwt(
            "token-string",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "HS256"),
            claims
        );
    }

    /**
     * Create a mock JWT token without 'sub' claim
     */
    private Jwt createJwtWithoutSub(String tenantId, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant_id", tenantId);
        claims.put("roles", roles);
        claims.put("exp", Instant.now().plusSeconds(3600));
        claims.put("iat", Instant.now());

        return new Jwt(
            "token-string",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "HS256"),
            claims
        );
    }

    /**
     * Create a mock JWT token without 'roles' claim
     */
    private Jwt createJwtWithoutRoles(String sub, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("tenant_id", tenantId);
        claims.put("exp", Instant.now().plusSeconds(3600));
        claims.put("iat", Instant.now());

        return new Jwt(
            "token-string",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "HS256"),
            claims
        );
    }

    /**
     * Create a mock JWT token with custom roles list
     * Allows testing null and blank values
     */
    private Jwt createJwtWithCustomRoles(String sub, String tenantId, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("tenant_id", tenantId);
        claims.put("roles", roles);
        claims.put("exp", Instant.now().plusSeconds(3600));
        claims.put("iat", Instant.now());

        return new Jwt(
            "token-string",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "HS256"),
            claims
        );
    }
}
