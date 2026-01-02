package com.healthdata.cql.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 *
 * Tests JWT token extraction, validation, and authentication context handling:
 * - Token extraction from Authorization header (Bearer scheme)
 * - Token extraction from HttpOnly cookie
 * - Filter skip logic for public endpoints
 * - Authentication context creation
 * - Error handling scenarios
 *
 * HIPAA Security Requirement: Verify authentication filter properly secures endpoints
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("JWT Authentication Filter Security Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;
    private static final String TEST_USERNAME = "testuser@example.com";
    private static final Set<String> TEST_ROLES = Set.of("ADMIN", "EVALUATOR");
    private static final Set<String> TEST_TENANTS = Set.of("tenant-1");

    @BeforeEach
    void setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Clear security context after each test
        SecurityContextHolder.clearContext();
    }

    // ==================== Token Extraction Tests ====================

    @Test
    @Order(1)
    @DisplayName("Should extract token from Authorization header with Bearer prefix")
    void testDoFilterInternal_BearerToken_ExtractsAndValidates() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(jwtTokenService.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtTokenService.extractRoles(VALID_TOKEN)).thenReturn(TEST_ROLES);
        when(jwtTokenService.extractTenantIds(VALID_TOKEN)).thenReturn(TEST_TENANTS);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtTokenService).validateToken(VALID_TOKEN);
        verify(jwtTokenService).extractUsername(VALID_TOKEN);
        verify(jwtTokenService).extractRoles(VALID_TOKEN);
        verify(filterChain).doFilter(request, response);

        // Verify authentication was set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be set");
        assertEquals(TEST_USERNAME, auth.getPrincipal(), "Principal should be username");
    }

    @Test
    @Order(2)
    @DisplayName("Should extract token from HttpOnly cookie when no Authorization header")
    void testDoFilterInternal_CookieToken_ExtractsAndValidates() throws ServletException, IOException {
        Cookie tokenCookie = new Cookie("hdim_access_token", VALID_TOKEN);

        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(request.getCookies()).thenReturn(new Cookie[]{tokenCookie});
        when(jwtTokenService.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtTokenService.extractRoles(VALID_TOKEN)).thenReturn(TEST_ROLES);
        when(jwtTokenService.extractTenantIds(VALID_TOKEN)).thenReturn(TEST_TENANTS);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtTokenService).validateToken(VALID_TOKEN);
        verify(filterChain).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be set from cookie");
    }

    @Test
    @Order(3)
    @DisplayName("Should prefer Authorization header over cookie")
    void testDoFilterInternal_BothHeaderAndCookie_PrefersHeader() throws ServletException, IOException {
        String headerToken = "header.jwt.token";
        String cookieToken = "cookie.jwt.token";
        Cookie tokenCookie = new Cookie("hdim_access_token", cookieToken);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + headerToken);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(request.getCookies()).thenReturn(new Cookie[]{tokenCookie});
        when(jwtTokenService.validateToken(headerToken)).thenReturn(true);
        when(jwtTokenService.extractUsername(headerToken)).thenReturn(TEST_USERNAME);
        when(jwtTokenService.extractRoles(headerToken)).thenReturn(TEST_ROLES);
        when(jwtTokenService.extractTenantIds(headerToken)).thenReturn(TEST_TENANTS);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Should validate header token, not cookie token
        verify(jwtTokenService).validateToken(headerToken);
        verify(jwtTokenService, never()).validateToken(cookieToken);
    }

    // ==================== No Token Scenarios ====================

    @Test
    @Order(10)
    @DisplayName("Should continue filter chain when no token present")
    void testDoFilterInternal_NoToken_ContinuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(request.getCookies()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtTokenService, never()).validateToken(any());

        // Authentication should not be set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @Order(11)
    @DisplayName("Should continue filter chain when Authorization header is not Bearer")
    void testDoFilterInternal_NonBearerAuth_ContinuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(request.getCookies()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtTokenService, never()).validateToken(any());
    }

    @Test
    @Order(12)
    @DisplayName("Should ignore other cookies and only use access token cookie")
    void testDoFilterInternal_OtherCookies_Ignored() throws ServletException, IOException {
        Cookie sessionCookie = new Cookie("JSESSIONID", "abc123");
        Cookie otherCookie = new Cookie("other_cookie", "value");

        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(request.getCookies()).thenReturn(new Cookie[]{sessionCookie, otherCookie});

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtTokenService, never()).validateToken(any());
    }

    // ==================== Invalid Token Scenarios ====================

    @Test
    @Order(20)
    @DisplayName("Should clear context and continue when token is invalid")
    void testDoFilterInternal_InvalidToken_ClearsContextAndContinues() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(jwtTokenService.validateToken(VALID_TOKEN)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @Order(21)
    @DisplayName("Should handle exception during token processing gracefully")
    void testDoFilterInternal_TokenProcessingException_ContinuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(jwtTokenService.validateToken(VALID_TOKEN)).thenThrow(new RuntimeException("Token service error"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Should still continue filter chain
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ==================== shouldNotFilter Tests ====================

    @Test
    @Order(30)
    @DisplayName("Should skip filter for actuator endpoints")
    void testShouldNotFilter_ActuatorEndpoint_ReturnsTrue() throws ServletException {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        boolean shouldSkip = jwtAuthenticationFilter.shouldNotFilter(request);

        assertTrue(shouldSkip, "Filter should skip actuator endpoints");
    }

    @Test
    @Order(31)
    @DisplayName("Should skip filter for swagger-ui endpoints")
    void testShouldNotFilter_SwaggerUI_ReturnsTrue() throws ServletException {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        boolean shouldSkip = jwtAuthenticationFilter.shouldNotFilter(request);

        assertTrue(shouldSkip, "Filter should skip swagger-ui endpoints");
    }

    @Test
    @Order(32)
    @DisplayName("Should skip filter for OpenAPI docs")
    void testShouldNotFilter_ApiDocs_ReturnsTrue() throws ServletException {
        when(request.getRequestURI()).thenReturn("/v3/api-docs");

        boolean shouldSkip = jwtAuthenticationFilter.shouldNotFilter(request);

        assertTrue(shouldSkip, "Filter should skip OpenAPI docs");
    }

    @Test
    @Order(33)
    @DisplayName("Should skip filter for WebSocket endpoints")
    void testShouldNotFilter_WebSocket_ReturnsTrue() throws ServletException {
        when(request.getRequestURI()).thenReturn("/ws/connect");

        boolean shouldSkip = jwtAuthenticationFilter.shouldNotFilter(request);

        assertTrue(shouldSkip, "Filter should skip WebSocket endpoints");
    }

    @Test
    @Order(34)
    @DisplayName("Should skip filter for CQL WebSocket endpoints")
    void testShouldNotFilter_CqlWebSocket_ReturnsTrue() throws ServletException {
        when(request.getRequestURI()).thenReturn("/cql-engine/ws/evaluate");

        boolean shouldSkip = jwtAuthenticationFilter.shouldNotFilter(request);

        assertTrue(shouldSkip, "Filter should skip CQL WebSocket endpoints");
    }

    @Test
    @Order(35)
    @DisplayName("Should skip filter for favicon.ico")
    void testShouldNotFilter_Favicon_ReturnsTrue() throws ServletException {
        when(request.getRequestURI()).thenReturn("/favicon.ico");

        boolean shouldSkip = jwtAuthenticationFilter.shouldNotFilter(request);

        assertTrue(shouldSkip, "Filter should skip favicon");
    }

    @Test
    @Order(36)
    @DisplayName("Should NOT skip filter for API endpoints")
    void testShouldNotFilter_ApiEndpoint_ReturnsFalse() throws ServletException {
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");

        boolean shouldSkip = jwtAuthenticationFilter.shouldNotFilter(request);

        assertFalse(shouldSkip, "Filter should NOT skip API endpoints");
    }

    @Test
    @Order(37)
    @DisplayName("Should NOT skip filter for evaluate endpoint")
    void testShouldNotFilter_EvaluateEndpoint_ReturnsFalse() throws ServletException {
        when(request.getRequestURI()).thenReturn("/evaluate");

        boolean shouldSkip = jwtAuthenticationFilter.shouldNotFilter(request);

        assertFalse(shouldSkip, "Filter should NOT skip evaluate endpoint");
    }

    // ==================== Authority Mapping Tests ====================

    @Test
    @Order(40)
    @DisplayName("Should map roles to Spring Security authorities with ROLE_ prefix")
    void testDoFilterInternal_RoleMapping_AddsRolePrefix() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(jwtTokenService.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtTokenService.extractRoles(VALID_TOKEN)).thenReturn(Set.of("ADMIN", "EVALUATOR"));
        when(jwtTokenService.extractTenantIds(VALID_TOKEN)).thenReturn(TEST_TENANTS);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
            "Should have ROLE_ADMIN authority");
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_EVALUATOR")),
            "Should have ROLE_EVALUATOR authority");
    }

    @Test
    @Order(41)
    @DisplayName("Should store tenant IDs in request attribute")
    void testDoFilterInternal_TenantIds_StoredInRequest() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(jwtTokenService.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtTokenService.extractRoles(VALID_TOKEN)).thenReturn(TEST_ROLES);
        when(jwtTokenService.extractTenantIds(VALID_TOKEN)).thenReturn(TEST_TENANTS);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute(eq("userTenantIds"), eq(TEST_TENANTS));
    }

    // ==================== Edge Cases ====================

    @Test
    @Order(50)
    @DisplayName("Should handle empty Bearer token gracefully")
    void testDoFilterInternal_EmptyBearerToken_ContinuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(request.getCookies()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Empty token should not cause validation attempt
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @Order(51)
    @DisplayName("Should not set authentication if already authenticated")
    void testDoFilterInternal_AlreadyAuthenticated_SkipsAuthentication() throws ServletException, IOException {
        // Pre-set authentication
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "existinguser", null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(jwtTokenService.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtTokenService.extractRoles(VALID_TOKEN)).thenReturn(TEST_ROLES);
        when(jwtTokenService.extractTenantIds(VALID_TOKEN)).thenReturn(TEST_TENANTS);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Should keep existing authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("existinguser", auth.getPrincipal());
    }

    @Test
    @Order(52)
    @DisplayName("Should handle null cookies array gracefully")
    void testDoFilterInternal_NullCookies_ContinuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(request.getCookies()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @Order(53)
    @DisplayName("Should handle empty cookies array gracefully")
    void testDoFilterInternal_EmptyCookies_ContinuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/cql/libraries");
        when(request.getCookies()).thenReturn(new Cookie[0]);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
