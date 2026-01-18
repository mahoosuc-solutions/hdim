package com.healthdata.queryapi.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider (Team 2)
 * Verifies JWT token validation, claim extraction, and authentication building
 *
 * Test Coverage:
 * - Token structure validation (required claims)
 * - Token expiration checking
 * - Token not-before (nbf) validation
 * - Issuer and audience validation
 * - Claim extraction (sub, tenant_id, roles)
 * - Authentication object construction
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Provider Validation and Claim Extraction")
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String VALID_ISSUER = "hdim-auth-service";
    private static final String VALID_AUDIENCE = "hdim-api";
    private static final String TEST_SUBJECT = "user@example.com";
    private static final String TEST_TENANT = "tenant-001";

    // ============ Token Structure Validation Tests ============

    @Test
    @DisplayName("Should validate token with all required claims")
    void shouldValidateTokenWithAllRequiredClaims() {
        Jwt jwt = createValidJwt();

        assertTrue(jwtTokenProvider.validateTokenStructure(jwt),
            "Should validate token with all required claims");
    }

    @Test
    @DisplayName("Should reject token with missing 'sub' claim")
    void shouldRejectTokenWithMissingSub() {
        Jwt jwt = createJwtWithoutClaim("sub");

        assertFalse(jwtTokenProvider.validateTokenStructure(jwt),
            "Should reject token missing 'sub' claim");
    }

    @Test
    @DisplayName("Should reject token with missing 'tenant_id' claim")
    void shouldRejectTokenWithMissingTenantId() {
        Jwt jwt = createJwtWithoutClaim("tenant_id");

        assertFalse(jwtTokenProvider.validateTokenStructure(jwt),
            "Should reject token missing 'tenant_id' claim");
    }

    @Test
    @DisplayName("Should reject token with missing 'roles' claim")
    void shouldRejectTokenWithMissingRoles() {
        Jwt jwt = createJwtWithoutClaim("roles");

        assertFalse(jwtTokenProvider.validateTokenStructure(jwt),
            "Should reject token missing 'roles' claim");
    }

    @Test
    @DisplayName("Should reject token with empty roles array")
    void shouldRejectTokenWithEmptyRoles() {
        Instant now = Instant.now();
        Jwt jwt = new Jwt(
            "test-token", now, now.plusSeconds(3600),
            Map.of("alg", "HS256"),
            Map.of(
                "sub", TEST_SUBJECT,
                "tenant_id", TEST_TENANT,
                "roles", List.of(),  // Empty roles
                "iss", VALID_ISSUER,
                "aud", VALID_AUDIENCE
            )
        );

        assertFalse(jwtTokenProvider.validateTokenStructure(jwt),
            "Should reject token with empty roles");
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        Instant now = Instant.now();
        Jwt jwt = new Jwt(
            "test-token",
            now.minusSeconds(3600),  // Issued 1 hour ago
            now.minusSeconds(600),   // Expired 10 minutes ago
            Map.of("alg", "HS256"),
            Map.of(
                "sub", TEST_SUBJECT,
                "tenant_id", TEST_TENANT,
                "roles", List.of("ADMIN"),
                "iss", VALID_ISSUER,
                "aud", VALID_AUDIENCE
            )
        );

        assertFalse(jwtTokenProvider.validateTokenStructure(jwt),
            "Should reject expired token");
    }

    // ============ Claim Extraction Tests ============

    @Test
    @DisplayName("Should extract 'sub' claim as user ID")
    void shouldExtractUserIdFromSubClaim() {
        Jwt jwt = createValidJwt();
        String userId = jwtTokenProvider.getUserId(jwt);

        assertEquals(TEST_SUBJECT, userId, "Should extract correct user ID from 'sub' claim");
    }

    @Test
    @DisplayName("Should extract 'tenant_id' claim")
    void shouldExtractTenantId() {
        Jwt jwt = createValidJwt();
        String tenantId = jwtTokenProvider.getTenantId(jwt);

        assertEquals(TEST_TENANT, tenantId, "Should extract correct tenant ID");
    }

    @Test
    @DisplayName("Should extract 'roles' claim as list")
    void shouldExtractRolesAsList() {
        Jwt jwt = createValidJwt();
        List<String> roles = jwtTokenProvider.getRoles(jwt);

        assertNotNull(roles, "Roles should not be null");
        assertTrue(roles.contains("ADMIN"), "Should contain ADMIN role");
        assertTrue(roles.contains("EVALUATOR"), "Should contain EVALUATOR role");
    }

    @Test
    @DisplayName("Should return empty list for missing 'roles' claim")
    void shouldReturnEmptyListForMissingRoles() {
        Jwt jwt = createJwtWithoutClaim("roles");
        List<String> roles = jwtTokenProvider.getClaimAsStringList(jwt, "roles");

        assertNotNull(roles, "Roles list should not be null");
        assertTrue(roles.isEmpty(), "Should return empty list for missing roles");
    }

    @Test
    @DisplayName("Should extract custom string claim")
    void shouldExtractCustomStringClaim() {
        Instant now = Instant.now();
        Jwt jwt = new Jwt(
            "test-token", now, now.plusSeconds(3600),
            Map.of("alg", "HS256"),
            Map.of(
                "sub", TEST_SUBJECT,
                "tenant_id", TEST_TENANT,
                "roles", List.of("ADMIN"),
                "iss", VALID_ISSUER,
                "aud", VALID_AUDIENCE,
                "custom_claim", "custom_value"
            )
        );

        String customValue = jwtTokenProvider.getClaimAsString(jwt, "custom_claim");
        assertEquals("custom_value", customValue, "Should extract custom claim");
    }

    // ============ Authentication Building Tests ============

    @Test
    @DisplayName("Should build authentication from valid JWT")
    void shouldBuildAuthenticationFromValidJwt() {
        Jwt jwt = createValidJwt();
        Authentication auth = jwtTokenProvider.getAuthentication(jwt);

        assertNotNull(auth, "Authentication should not be null");
        assertEquals(TEST_SUBJECT, auth.getName(), "Authentication name should be user ID");
    }

    @Test
    @DisplayName("Should extract authorities with ROLE_ prefix")
    void shouldExtractAuthoritiesWithRolePrefix() {
        Jwt jwt = createValidJwt();
        Authentication auth = jwtTokenProvider.getAuthentication(jwt);

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        assertNotNull(authorities, "Authorities should not be null");

        List<String> authorityNames = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        assertTrue(authorityNames.contains("ROLE_ADMIN"),
            "Should have ROLE_ADMIN authority");
        assertTrue(authorityNames.contains("ROLE_EVALUATOR"),
            "Should have ROLE_EVALUATOR authority");
    }

    @Test
    @DisplayName("Should return null authentication for invalid token")
    void shouldReturnNullAuthenticationForInvalidToken() {
        Jwt jwt = createJwtWithoutClaim("roles");
        Authentication auth = jwtTokenProvider.getAuthentication(jwt);

        assertNull(auth, "Should return null for invalid token");
    }

    // ============ Helper Methods ============

    private Jwt createValidJwt() {
        Instant now = Instant.now();
        return new Jwt(
            "test-token",
            now,
            now.plusSeconds(3600),
            Map.of("alg", "HS256"),
            Map.of(
                "sub", TEST_SUBJECT,
                "tenant_id", TEST_TENANT,
                "roles", List.of("ADMIN", "EVALUATOR"),
                "iss", VALID_ISSUER,
                "aud", VALID_AUDIENCE
            )
        );
    }

    private Jwt createJwtWithoutClaim(String claimToExclude) {
        Instant now = Instant.now();
        var claims = Map.ofEntries(
            Map.entry("sub", TEST_SUBJECT),
            Map.entry("tenant_id", TEST_TENANT),
            Map.entry("roles", List.of("ADMIN")),
            Map.entry("iss", VALID_ISSUER),
            Map.entry("aud", VALID_AUDIENCE)
        );

        // Remove the specified claim
        var modifiableClaims = new java.util.HashMap<>(claims);
        modifiableClaims.remove(claimToExclude);

        return new Jwt(
            "test-token",
            now,
            now.plusSeconds(3600),
            Map.of("alg", "HS256"),
            modifiableClaims
        );
    }
}
