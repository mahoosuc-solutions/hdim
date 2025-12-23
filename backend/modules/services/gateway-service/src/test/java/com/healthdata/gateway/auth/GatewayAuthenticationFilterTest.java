package com.healthdata.gateway.auth;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.gateway.config.GatewayAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GatewayAuthenticationFilter.
 *
 * Tests cover:
 * - JWT validation at gateway
 * - Header stripping (security)
 * - Header injection for downstream services
 * - Public path handling
 * - Error handling
 * - Signature generation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayAuthenticationFilter")
class GatewayAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private GatewayAuthProperties authProperties;

    @Mock
    private PublicPathRegistry publicPathRegistry;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private GatewayAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new GatewayAuthenticationFilter(jwtTokenService, authProperties, publicPathRegistry);
    }

    @Nested
    @DisplayName("Authentication Disabled")
    class AuthenticationDisabled {

        @Test
        @DisplayName("should pass through when authentication is disabled")
        void shouldPassThroughWhenDisabled() throws Exception {
            // Given
            when(authProperties.getEnabled()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenService);
        }
    }

    @Nested
    @DisplayName("Public Paths")
    class PublicPaths {

        @Test
        @DisplayName("should skip authentication for public paths")
        void shouldSkipAuthForPublicPaths() throws Exception {
            // Given
            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/actuator/health");
            when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
            when(publicPathRegistry.isPublicPath("/actuator/health")).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), eq(response));
            verifyNoInteractions(jwtTokenService);
        }

        @Test
        @DisplayName("should skip authentication for swagger paths")
        void shouldSkipAuthForSwaggerPaths() throws Exception {
            // Given
            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
            when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
            when(publicPathRegistry.isPublicPath("/swagger-ui/index.html")).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), eq(response));
        }
    }

    @Nested
    @DisplayName("Header Stripping")
    class HeaderStripping {

        @Test
        @DisplayName("should strip external X-Auth headers")
        void shouldStripExternalAuthHeaders() throws Exception {
            // Given
            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(authProperties.getEnforced()).thenReturn(false);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");
            when(request.getMethod()).thenReturn("GET");
            when(publicPathRegistry.isPublicPath("/api/v1/patients")).thenReturn(false);

            // Mock headers including malicious X-Auth headers
            Vector<String> headerNames = new Vector<>();
            headerNames.add("Authorization");
            headerNames.add("X-Auth-User-Id");  // Should be stripped
            headerNames.add("X-Auth-Roles");    // Should be stripped
            headerNames.add("Content-Type");
            when(request.getHeaderNames()).thenReturn(headerNames.elements());
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(requestCaptor.capture(), eq(response));

            HttpServletRequest wrappedRequest = requestCaptor.getValue();
            // The wrapped request should have X-Auth headers stripped
            assertThat(wrappedRequest.getHeader("X-Auth-User-Id")).isNull();
            assertThat(wrappedRequest.getHeader("X-Auth-Roles")).isNull();
        }
    }

    @Nested
    @DisplayName("JWT Validation")
    class JwtValidation {

        @Test
        @DisplayName("should authenticate with valid JWT")
        void shouldAuthenticateWithValidJwt() throws Exception {
            // Given
            String jwt = "valid.jwt.token";
            UUID userId = UUID.randomUUID();
            String username = "testuser";
            Set<String> tenantIds = Set.of("tenant1", "tenant2");
            Set<String> roles = Set.of("ADMIN", "PROVIDER");
            String tokenId = "token-123";
            Date expiration = new Date(System.currentTimeMillis() + 3600000);

            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(authProperties.getHeaderSigningSecret()).thenReturn("test-secret-32-chars-minimum-here");
            when(authProperties.getAuditLogging()).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Authorization")));
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(publicPathRegistry.isPublicPath("/api/v1/patients")).thenReturn(false);

            when(jwtTokenService.validateToken(jwt)).thenReturn(true);
            when(jwtTokenService.extractUsername(jwt)).thenReturn(username);
            when(jwtTokenService.extractUserId(jwt)).thenReturn(userId);
            when(jwtTokenService.extractTenantIds(jwt)).thenReturn(tenantIds);
            when(jwtTokenService.extractRoles(jwt)).thenReturn(roles);
            when(jwtTokenService.extractJwtId(jwt)).thenReturn(tokenId);
            when(jwtTokenService.getExpirationDate(jwt)).thenReturn(expiration);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtTokenService).validateToken(jwt);
            verify(filterChain).doFilter(any(HttpServletRequest.class), eq(response));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo(username);
            assertThat(auth.getAuthorities()).hasSize(2);
        }

        @Test
        @DisplayName("should reject invalid JWT")
        void shouldRejectInvalidJwt() throws Exception {
            // Given
            String jwt = "invalid.jwt.token";
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Authorization")));
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(publicPathRegistry.isPublicPath("/api/v1/patients")).thenReturn(false);
            when(response.getWriter()).thenReturn(printWriter);

            when(jwtTokenService.validateToken(jwt)).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should reject expired JWT")
        void shouldRejectExpiredJwt() throws Exception {
            // Given
            String jwt = "expired.jwt.token";
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Authorization")));
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(publicPathRegistry.isPublicPath("/api/v1/patients")).thenReturn(false);
            when(response.getWriter()).thenReturn(printWriter);

            when(jwtTokenService.validateToken(jwt)).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            assertThat(stringWriter.toString()).contains("Invalid or expired token");
        }
    }

    @Nested
    @DisplayName("Missing Token Handling")
    class MissingTokenHandling {

        @Test
        @DisplayName("should allow request without token when not enforced")
        void shouldAllowWithoutTokenWhenNotEnforced() throws Exception {
            // Given
            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(authProperties.getEnforced()).thenReturn(false);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
            when(request.getHeader("Authorization")).thenReturn(null);
            when(publicPathRegistry.isPublicPath("/api/v1/patients")).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), eq(response));
            verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("should reject request without token when enforced")
        void shouldRejectWithoutTokenWhenEnforced() throws Exception {
            // Given
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(authProperties.getEnforced()).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
            when(request.getHeader("Authorization")).thenReturn(null);
            when(publicPathRegistry.isPublicPath("/api/v1/patients")).thenReturn(false);
            when(response.getWriter()).thenReturn(printWriter);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            assertThat(stringWriter.toString()).contains("Authentication required");
        }
    }

    @Nested
    @DisplayName("Header Injection")
    class HeaderInjection {

        @Test
        @DisplayName("should inject all required auth headers")
        void shouldInjectAllRequiredHeaders() throws Exception {
            // Given
            String jwt = "valid.jwt.token";
            UUID userId = UUID.randomUUID();
            String username = "testuser";
            Set<String> tenantIds = Set.of("tenant1", "tenant2");
            Set<String> roles = Set.of("ADMIN");
            String tokenId = "token-123";
            Date expiration = new Date(System.currentTimeMillis() + 3600000);

            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(authProperties.getHeaderSigningSecret()).thenReturn("test-secret-32-chars-minimum-here");
            when(authProperties.getAuditLogging()).thenReturn(false);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Authorization")));
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(publicPathRegistry.isPublicPath("/api/v1/patients")).thenReturn(false);

            when(jwtTokenService.validateToken(jwt)).thenReturn(true);
            when(jwtTokenService.extractUsername(jwt)).thenReturn(username);
            when(jwtTokenService.extractUserId(jwt)).thenReturn(userId);
            when(jwtTokenService.extractTenantIds(jwt)).thenReturn(tenantIds);
            when(jwtTokenService.extractRoles(jwt)).thenReturn(roles);
            when(jwtTokenService.extractJwtId(jwt)).thenReturn(tokenId);
            when(jwtTokenService.getExpirationDate(jwt)).thenReturn(expiration);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(requestCaptor.capture(), eq(response));

            HttpServletRequest wrappedRequest = requestCaptor.getValue();

            // Verify injected headers
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_USER_ID))
                .isEqualTo(userId.toString());
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_USERNAME))
                .isEqualTo(username);
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_TENANT_IDS))
                .contains("tenant1", "tenant2");
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_ROLES))
                .isEqualTo("ADMIN");
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_VALIDATED))
                .startsWith("gateway-");
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_TOKEN_ID))
                .isEqualTo(tokenId);
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_TOKEN_EXPIRES))
                .isNotNull();
        }
    }

    @Nested
    @DisplayName("Security Context")
    class SecurityContext {

        @Test
        @DisplayName("should set security context with correct authorities")
        void shouldSetSecurityContextWithAuthorities() throws Exception {
            // Given
            String jwt = "valid.jwt.token";
            UUID userId = UUID.randomUUID();
            String username = "testuser";
            Set<String> roles = Set.of("ADMIN", "PROVIDER", "VIEWER");

            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(authProperties.getHeaderSigningSecret()).thenReturn("test-secret-32-chars-minimum-here");
            when(authProperties.getAuditLogging()).thenReturn(false);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Authorization")));
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(publicPathRegistry.isPublicPath("/api/v1/patients")).thenReturn(false);

            when(jwtTokenService.validateToken(jwt)).thenReturn(true);
            when(jwtTokenService.extractUsername(jwt)).thenReturn(username);
            when(jwtTokenService.extractUserId(jwt)).thenReturn(userId);
            when(jwtTokenService.extractTenantIds(jwt)).thenReturn(Set.of("tenant1"));
            when(jwtTokenService.extractRoles(jwt)).thenReturn(roles);
            when(jwtTokenService.extractJwtId(jwt)).thenReturn("token-id");
            when(jwtTokenService.getExpirationDate(jwt)).thenReturn(new Date());

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo(username);
            assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_PROVIDER", "ROLE_VIEWER");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should handle JWT service exceptions gracefully")
        void shouldHandleJwtServiceExceptions() throws Exception {
            // Given
            String jwt = "valid.jwt.token";
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");
            when(request.getMethod()).thenReturn("GET");
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Authorization")));
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(publicPathRegistry.isPublicPath("/api/v1/patients")).thenReturn(false);
            when(response.getWriter()).thenReturn(printWriter);

            when(jwtTokenService.validateToken(jwt)).thenThrow(new RuntimeException("JWT service error"));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            assertThat(stringWriter.toString()).contains("authentication_error");
        }
    }
}
