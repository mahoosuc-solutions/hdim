package com.healthdata.cql.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JwtTokenService.
 *
 * Tests JWT token validation, claims extraction, and security scenarios:
 * - Valid token processing
 * - Expired token rejection
 * - Malformed token handling
 * - Invalid signature detection
 * - Claims extraction (username, roles, tenants)
 *
 * HIPAA Security Requirement: Proper JWT validation prevents unauthorized access
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("JWT Token Service Security Tests")
class JwtTokenServiceTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtTokenService jwtTokenService;

    // Test secret key (256+ bits for HS512)
    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hs512-algorithm";
    private static final String TEST_ISSUER = "healthdata-in-motion";
    private static final String TEST_AUDIENCE = "healthdata-api";
    private static final String TEST_USERNAME = "testuser@example.com";
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String TEST_TENANT = "tenant-123";
    private static final String TEST_ROLES = "ADMIN,USER";

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn(TEST_SECRET);
        when(jwtConfig.getIssuer()).thenReturn(TEST_ISSUER);
        when(jwtConfig.getAudience()).thenReturn(TEST_AUDIENCE);
    }

    // ==================== Token Generation Helpers ====================

    private String createValidToken() {
        return createToken(
            TEST_USERNAME,
            TEST_USER_ID,
            TEST_TENANT,
            TEST_ROLES,
            Duration.ofHours(1)
        );
    }

    private String createExpiredToken() {
        return createToken(
            TEST_USERNAME,
            TEST_USER_ID,
            TEST_TENANT,
            TEST_ROLES,
            Duration.ofHours(-1) // Already expired
        );
    }

    private String createToken(String username, UUID userId, String tenantIds,
                               String roles, Duration expiration) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(username)
            .claim("userId", userId.toString())
            .claim("tenantIds", tenantIds)
            .claim("roles", roles)
            .issuer(TEST_ISSUER)
            .audience().add(TEST_AUDIENCE).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiration)))
            .id(UUID.randomUUID().toString())
            .signWith(key)
            .compact();
    }

    private String createTokenWithDifferentSecret(String wrongSecret) {
        SecretKey key = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(TEST_USERNAME)
            .issuer(TEST_ISSUER)
            .audience().add(TEST_AUDIENCE).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofHours(1))))
            .signWith(key)
            .compact();
    }

    // ==================== Token Validation Tests ====================

    @Test
    @Order(1)
    @DisplayName("Should validate a properly formed JWT token")
    void testValidateToken_ValidToken_ReturnsTrue() {
        String token = createValidToken();

        boolean isValid = jwtTokenService.validateToken(token);

        assertTrue(isValid, "Valid token should pass validation");
    }

    @Test
    @Order(2)
    @DisplayName("Should reject expired JWT token")
    void testValidateToken_ExpiredToken_ReturnsFalse() {
        String token = createExpiredToken();

        boolean isValid = jwtTokenService.validateToken(token);

        assertFalse(isValid, "Expired token should be rejected");
    }

    @Test
    @Order(3)
    @DisplayName("Should reject malformed JWT token")
    void testValidateToken_MalformedToken_ReturnsFalse() {
        String malformedToken = "not.a.valid.jwt.token";

        boolean isValid = jwtTokenService.validateToken(malformedToken);

        assertFalse(isValid, "Malformed token should be rejected");
    }

    @Test
    @Order(4)
    @DisplayName("Should reject JWT token with invalid signature")
    void testValidateToken_InvalidSignature_ReturnsFalse() {
        // Create token with different secret
        String wrongSecret = "different-secret-key-must-also-be-256-bits-for-hs512-algorithm";
        String tokenWithWrongSignature = createTokenWithDifferentSecret(wrongSecret);

        boolean isValid = jwtTokenService.validateToken(tokenWithWrongSignature);

        assertFalse(isValid, "Token with invalid signature should be rejected");
    }

    @Test
    @Order(5)
    @DisplayName("Should reject null token")
    void testValidateToken_NullToken_ReturnsFalse() {
        boolean isValid = jwtTokenService.validateToken(null);

        assertFalse(isValid, "Null token should be rejected");
    }

    @Test
    @Order(6)
    @DisplayName("Should reject empty token")
    void testValidateToken_EmptyToken_ReturnsFalse() {
        boolean isValid = jwtTokenService.validateToken("");

        assertFalse(isValid, "Empty token should be rejected");
    }

    @Test
    @Order(7)
    @DisplayName("Should reject token with wrong issuer")
    void testValidateToken_WrongIssuer_ReturnsFalse() {
        // Create token with wrong issuer
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String tokenWithWrongIssuer = Jwts.builder()
            .subject(TEST_USERNAME)
            .issuer("wrong-issuer")
            .audience().add(TEST_AUDIENCE).and()
            .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
            .signWith(key)
            .compact();

        boolean isValid = jwtTokenService.validateToken(tokenWithWrongIssuer);

        assertFalse(isValid, "Token with wrong issuer should be rejected");
    }

    @Test
    @Order(8)
    @DisplayName("Should reject token with wrong audience")
    void testValidateToken_WrongAudience_ReturnsFalse() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String tokenWithWrongAudience = Jwts.builder()
            .subject(TEST_USERNAME)
            .issuer(TEST_ISSUER)
            .audience().add("wrong-audience").and()
            .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
            .signWith(key)
            .compact();

        boolean isValid = jwtTokenService.validateToken(tokenWithWrongAudience);

        assertFalse(isValid, "Token with wrong audience should be rejected");
    }

    // ==================== Security Edge Cases ====================

    @Test
    @Order(30)
    @DisplayName("Should reject token that expires exactly now")
    void testValidateToken_TokenExpiresNow_ReturnsFalse() {
        // Create token that expires immediately
        String token = createToken(TEST_USERNAME, TEST_USER_ID, TEST_TENANT, TEST_ROLES, Duration.ZERO);

        // Small delay to ensure token is expired
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        boolean isValid = jwtTokenService.validateToken(token);

        assertFalse(isValid, "Token that expires immediately should be rejected");
    }

    @Test
    @Order(31)
    @DisplayName("Should reject token signed with Base64 encoded different key")
    void testValidateToken_Base64EncodedWrongKey_ReturnsFalse() {
        String differentKey = "YW5vdGhlci1zZWNyZXQta2V5LXRoYXQtaXMtZGlmZmVyZW50LWFuZC1sb25n"; // Base64 encoded
        String tokenWithWrongKey = createTokenWithDifferentSecret(
            new String(java.util.Base64.getDecoder().decode(differentKey)));

        boolean isValid = jwtTokenService.validateToken(tokenWithWrongKey);

        assertFalse(isValid, "Token with different Base64 key should be rejected");
    }

    @Test
    @Order(32)
    @DisplayName("Should reject truncated token")
    void testValidateToken_TruncatedToken_ReturnsFalse() {
        String validToken = createValidToken();
        // Truncate the signature
        String truncatedToken = validToken.substring(0, validToken.lastIndexOf('.') + 5);

        boolean isValid = jwtTokenService.validateToken(truncatedToken);

        assertFalse(isValid, "Truncated token should be rejected");
    }

    // ==================== Claims Extraction Tests ====================

    @Test
    @Order(40)
    @DisplayName("Should extract username from valid token")
    void testExtractUsername_ValidToken_ReturnsUsername() {
        String token = createValidToken();

        String username = jwtTokenService.extractUsername(token);

        assertEquals(TEST_USERNAME, username, "Username should be extracted correctly");
    }

    @Test
    @Order(41)
    @DisplayName("Should extract user ID from valid token")
    void testExtractUserId_ValidToken_ReturnsUserId() {
        String token = createValidToken();

        UUID userId = jwtTokenService.extractUserId(token);

        assertEquals(TEST_USER_ID, userId, "User ID should be extracted correctly");
    }

    @Test
    @Order(42)
    @DisplayName("Should extract tenant IDs from valid token")
    void testExtractTenantIds_ValidToken_ReturnsTenantIds() {
        String token = createValidToken();

        Set<String> tenantIds = jwtTokenService.extractTenantIds(token);

        assertNotNull(tenantIds, "Tenant IDs should not be null");
        assertTrue(tenantIds.contains(TEST_TENANT), "Tenant IDs should contain expected tenant");
    }

    @Test
    @Order(43)
    @DisplayName("Should extract roles from valid token")
    void testExtractRoles_ValidToken_ReturnsRoles() {
        String token = createValidToken();

        Set<String> roles = jwtTokenService.extractRoles(token);

        assertNotNull(roles, "Roles should not be null");
        assertTrue(roles.contains("ADMIN"), "Roles should contain ADMIN");
        assertTrue(roles.contains("USER"), "Roles should contain USER");
    }

    @Test
    @Order(44)
    @DisplayName("Should return false for non-expired token")
    void testIsTokenExpired_ValidToken_ReturnsFalse() {
        String token = createValidToken();

        boolean isExpired = jwtTokenService.isTokenExpired(token);

        assertFalse(isExpired, "Valid token should not be expired");
    }

    @Test
    @Order(45)
    @DisplayName("Should return true for expired token")
    void testIsTokenExpired_ExpiredToken_ReturnsTrue() {
        String token = createExpiredToken();

        boolean isExpired = jwtTokenService.isTokenExpired(token);

        assertTrue(isExpired, "Expired token should be detected");
    }

    @Test
    @Order(46)
    @DisplayName("Should get expiration date from valid token")
    void testGetExpirationDate_ValidToken_ReturnsDate() {
        String token = createValidToken();

        Date expiration = jwtTokenService.getExpirationDate(token);

        assertNotNull(expiration, "Expiration date should not be null");
        assertTrue(expiration.after(new Date()), "Expiration should be in the future");
    }

    @Test
    @Order(47)
    @DisplayName("Should extract JWT ID from valid token")
    void testExtractJwtId_ValidToken_ReturnsJwtId() {
        String token = createValidToken();

        String jwtId = jwtTokenService.extractJwtId(token);

        assertNotNull(jwtId, "JWT ID should not be null");
        assertFalse(jwtId.isBlank(), "JWT ID should not be blank");
    }

    @Test
    @Order(48)
    @DisplayName("Should get issued at date from valid token")
    void testGetIssuedAt_ValidToken_ReturnsDate() {
        String token = createValidToken();

        Date issuedAt = jwtTokenService.getIssuedAt(token);

        assertNotNull(issuedAt, "Issued at date should not be null");
        assertTrue(issuedAt.before(new Date()) || issuedAt.equals(new Date()),
            "Issued at should be now or in the past");
    }

    // ==================== Multiple Tenants/Roles Tests ====================

    @Test
    @Order(50)
    @DisplayName("Should extract multiple tenant IDs from token")
    void testExtractTenantIds_MultipleTenants_ReturnsAll() {
        String token = createToken(
            TEST_USERNAME,
            TEST_USER_ID,
            "tenant-1,tenant-2,tenant-3",
            TEST_ROLES,
            Duration.ofHours(1)
        );

        Set<String> tenantIds = jwtTokenService.extractTenantIds(token);

        assertEquals(3, tenantIds.size(), "Should extract all tenant IDs");
        assertTrue(tenantIds.contains("tenant-1"), "Should contain tenant-1");
        assertTrue(tenantIds.contains("tenant-2"), "Should contain tenant-2");
        assertTrue(tenantIds.contains("tenant-3"), "Should contain tenant-3");
    }

    @Test
    @Order(51)
    @DisplayName("Should extract multiple roles from token")
    void testExtractRoles_MultipleRoles_ReturnsAll() {
        String token = createToken(
            TEST_USERNAME,
            TEST_USER_ID,
            TEST_TENANT,
            "ADMIN,USER,EVALUATOR,SUPER_ADMIN",
            Duration.ofHours(1)
        );

        Set<String> roles = jwtTokenService.extractRoles(token);

        assertEquals(4, roles.size(), "Should extract all roles");
        assertTrue(roles.contains("ADMIN"), "Should contain ADMIN");
        assertTrue(roles.contains("USER"), "Should contain USER");
        assertTrue(roles.contains("EVALUATOR"), "Should contain EVALUATOR");
        assertTrue(roles.contains("SUPER_ADMIN"), "Should contain SUPER_ADMIN");
    }

    @Test
    @Order(52)
    @DisplayName("Should handle empty tenant IDs gracefully")
    void testExtractTenantIds_EmptyTenants_ReturnsEmptySet() {
        String token = createToken(
            TEST_USERNAME,
            TEST_USER_ID,
            "",  // Empty tenant IDs
            TEST_ROLES,
            Duration.ofHours(1)
        );

        Set<String> tenantIds = jwtTokenService.extractTenantIds(token);

        assertNotNull(tenantIds, "Tenant IDs should not be null");
        assertTrue(tenantIds.isEmpty(), "Tenant IDs should be empty");
    }

    @Test
    @Order(53)
    @DisplayName("Should handle empty roles gracefully")
    void testExtractRoles_EmptyRoles_ReturnsEmptySet() {
        String token = createToken(
            TEST_USERNAME,
            TEST_USER_ID,
            TEST_TENANT,
            "",  // Empty roles
            Duration.ofHours(1)
        );

        Set<String> roles = jwtTokenService.extractRoles(token);

        assertNotNull(roles, "Roles should not be null");
        assertTrue(roles.isEmpty(), "Roles should be empty");
    }

    // ==================== Token Tampering Detection ====================

    @Test
    @Order(60)
    @DisplayName("Should throw exception when extracting claims from tampered token")
    void testExtractClaims_TamperedToken_ThrowsException() {
        String validToken = createValidToken();
        // Tamper with the payload by modifying a character
        String[] parts = validToken.split("\\.");
        StringBuilder tamperedPayload = new StringBuilder(parts[1]);
        tamperedPayload.setCharAt(5, tamperedPayload.charAt(5) == 'a' ? 'b' : 'a');
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThrows(Exception.class, () -> {
            jwtTokenService.extractUsername(tamperedToken);
        }, "Extracting claims from tampered token should throw exception");
    }

    @Test
    @Order(61)
    @DisplayName("Should detect token with no signature part")
    void testValidateToken_NoSignature_ReturnsFalse() {
        String validToken = createValidToken();
        String[] parts = validToken.split("\\.");
        String tokenWithoutSignature = parts[0] + "." + parts[1];

        boolean isValid = jwtTokenService.validateToken(tokenWithoutSignature);

        assertFalse(isValid, "Token without signature should be rejected");
    }

    @Test
    @Order(62)
    @DisplayName("Should reject token with 'none' algorithm attack")
    void testValidateToken_NoneAlgorithmAttack_ReturnsFalse() {
        // Simulate 'none' algorithm attack
        String noneAlgToken = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJ0ZXN0dXNlckBleGFtcGxlLmNvbSIsImV4cCI6OTk5OTk5OTk5OX0.";

        boolean isValid = jwtTokenService.validateToken(noneAlgToken);

        assertFalse(isValid, "Token with 'none' algorithm should be rejected");
    }

    @Test
    @Order(63)
    @DisplayName("Should handle whitespace-only token")
    void testValidateToken_WhitespaceToken_ReturnsFalse() {
        boolean isValid = jwtTokenService.validateToken("   ");

        assertFalse(isValid, "Whitespace-only token should be rejected");
    }
}
