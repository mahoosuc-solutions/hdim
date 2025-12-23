package com.healthdata.authentication.filter;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import com.healthdata.authentication.filter.TrustedHeaderAuthFilter.TrustedHeaderAuthConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for TrustedHeaderAuthFilter.
 *
 * Tests cover:
 * - Valid header extraction and authentication
 * - Missing/invalid header handling
 * - Signature validation
 * - Role and tenant parsing
 * - Security context population
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrustedHeaderAuthFilter")
class TrustedHeaderAuthFilterTest {

    @Mock(lenient = true)
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private TrustedHeaderAuthFilter filter;
    private TrustedHeaderAuthConfig config;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        config = TrustedHeaderAuthConfig.development();
        filter = new TrustedHeaderAuthFilter(config);
        // Default stubs for request
        lenient().when(request.getRequestURI()).thenReturn("/api/v1/test");
        // Default all headers to null (to be overridden in specific tests)
        lenient().when(request.getHeader(anyString())).thenReturn(null);
    }

    @Nested
    @DisplayName("Valid Authentication")
    class ValidAuthentication {

        @Test
        @DisplayName("should authenticate user with valid headers")
        void shouldAuthenticateWithValidHeaders() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            String username = "testuser";
            String tenantIds = "tenant1,tenant2";
            String roles = "ADMIN,PROVIDER";
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";

            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USER_ID)).thenReturn(userId);
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn(username);
            when(request.getHeader(AuthHeaderConstants.HEADER_TENANT_IDS)).thenReturn(tenantIds);
            when(request.getHeader(AuthHeaderConstants.HEADER_ROLES)).thenReturn(roles);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo(username);
            assertThat(auth.getAuthorities()).hasSize(2);
        }

        @Test
        @DisplayName("should set tenant IDs in request attribute")
        void shouldSetTenantIdsInRequestAttribute() throws Exception {
            // Given
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn("testuser");
            when(request.getHeader(AuthHeaderConstants.HEADER_TENANT_IDS)).thenReturn("tenant1,tenant2");
            when(request.getHeader(AuthHeaderConstants.HEADER_ROLES)).thenReturn("ADMIN");
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(request).setAttribute(eq(AuthHeaderConstants.ATTR_TENANT_IDS), any(Set.class));
        }

        @Test
        @DisplayName("should set user ID in request attribute when valid UUID")
        void shouldSetUserIdInRequestAttribute() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";

            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USER_ID)).thenReturn(userId.toString());
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn("testuser");
            when(request.getHeader(AuthHeaderConstants.HEADER_TENANT_IDS)).thenReturn("tenant1");
            when(request.getHeader(AuthHeaderConstants.HEADER_ROLES)).thenReturn("ADMIN");
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(request).setAttribute(eq(AuthHeaderConstants.ATTR_USER_ID), eq(userId));
        }
    }

    @Nested
    @DisplayName("Missing Headers")
    class MissingHeaders {

        @Test
        @DisplayName("should skip authentication when validated header is missing")
        void shouldSkipWhenValidatedHeaderMissing() throws Exception {
            // Given
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should skip authentication when username header is missing")
        void shouldSkipWhenUsernameHeaderMissing() throws Exception {
            // Given
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should skip authentication when username header is blank")
        void shouldSkipWhenUsernameHeaderBlank() throws Exception {
            // Given
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn("   ");
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Signature Validation")
    class SignatureValidation {

        @Test
        @DisplayName("should reject invalid signature prefix")
        void shouldRejectInvalidSignaturePrefix() throws Exception {
            // Given
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn("invalid-signature");
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should accept valid signature in development mode")
        void shouldAcceptValidSignatureInDevMode() throws Exception {
            // Given
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn("testuser");
            when(request.getHeader(AuthHeaderConstants.HEADER_ROLES)).thenReturn("ADMIN");
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Role Parsing")
    class RoleParsing {

        @Test
        @DisplayName("should parse multiple roles correctly")
        void shouldParseMultipleRoles() throws Exception {
            // Given
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn("testuser");
            when(request.getHeader(AuthHeaderConstants.HEADER_ROLES)).thenReturn("ADMIN,PROVIDER,VIEWER");
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities()).hasSize(3);
            assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_PROVIDER", "ROLE_VIEWER");
        }

        @Test
        @DisplayName("should handle empty roles")
        void shouldHandleEmptyRoles() throws Exception {
            // Given
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn("testuser");
            when(request.getHeader(AuthHeaderConstants.HEADER_ROLES)).thenReturn("");
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("should handle null roles")
        void shouldHandleNullRoles() throws Exception {
            // Given
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn("testuser");
            when(request.getHeader(AuthHeaderConstants.HEADER_ROLES)).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tenant Parsing")
    class TenantParsing {

        @Test
        @DisplayName("should parse multiple tenant IDs correctly")
        void shouldParseMultipleTenantIds() throws Exception {
            // Given
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn("testuser");
            when(request.getHeader(AuthHeaderConstants.HEADER_TENANT_IDS)).thenReturn("tenant1,tenant2,tenant3");
            when(request.getHeader(AuthHeaderConstants.HEADER_ROLES)).thenReturn("ADMIN");
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(request).setAttribute(eq(AuthHeaderConstants.ATTR_TENANT_IDS), argThat(arg -> {
                Set<String> tenants = (Set<String>) arg;
                return tenants.size() == 3 &&
                    tenants.contains("tenant1") &&
                    tenants.contains("tenant2") &&
                    tenants.contains("tenant3");
            }));
        }
    }

    @Nested
    @DisplayName("Path Filtering")
    class PathFiltering {

        @Test
        @DisplayName("should skip filter for actuator endpoints")
        void shouldSkipActuatorEndpoints() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/actuator/health");

            // When
            boolean shouldNotFilter = filter.shouldNotFilter(request);

            // Then
            assertThat(shouldNotFilter).isTrue();
        }

        @Test
        @DisplayName("should skip filter for swagger endpoints")
        void shouldSkipSwaggerEndpoints() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

            // When
            boolean shouldNotFilter = filter.shouldNotFilter(request);

            // Then
            assertThat(shouldNotFilter).isTrue();
        }

        @Test
        @DisplayName("should not skip filter for API endpoints")
        void shouldNotSkipApiEndpoints() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            boolean shouldNotFilter = filter.shouldNotFilter(request);

            // Then
            assertThat(shouldNotFilter).isFalse();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should handle invalid user ID format gracefully")
        void shouldHandleInvalidUserIdFormat() throws Exception {
            // Given
            String validSignature = "gateway-" + (System.currentTimeMillis() / 1000) + "-dev";
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenReturn(validSignature);
            when(request.getHeader(AuthHeaderConstants.HEADER_USER_ID)).thenReturn("not-a-uuid");
            when(request.getHeader(AuthHeaderConstants.HEADER_USERNAME)).thenReturn("testuser");
            when(request.getHeader(AuthHeaderConstants.HEADER_ROLES)).thenReturn("ADMIN");
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            // Should still authenticate, just not set user ID attribute
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }

        @Test
        @DisplayName("should clear security context on exception")
        void shouldClearSecurityContextOnException() throws Exception {
            // Given
            when(request.getHeader(AuthHeaderConstants.HEADER_VALIDATED)).thenThrow(new RuntimeException("Test error"));
            when(request.getRequestURI()).thenReturn("/api/v1/patients");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("should create development config correctly")
        void shouldCreateDevelopmentConfig() {
            TrustedHeaderAuthConfig devConfig = TrustedHeaderAuthConfig.development();

            assertThat(devConfig.isDevelopmentMode()).isTrue();
            assertThat(devConfig.getSharedSecret()).isNull();
        }

        @Test
        @DisplayName("should create production config correctly")
        void shouldCreateProductionConfig() {
            String secret = "my-production-secret";
            TrustedHeaderAuthConfig prodConfig = TrustedHeaderAuthConfig.production(secret);

            assertThat(prodConfig.isDevelopmentMode()).isFalse();
            assertThat(prodConfig.getSharedSecret()).isEqualTo(secret);
        }
    }
}
