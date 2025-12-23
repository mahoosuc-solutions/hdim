package com.healthdata.gateway.auth;

import com.healthdata.gateway.config.GatewayAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PublicPathRegistry.
 *
 * Tests cover:
 * - Default public paths
 * - Configured public paths
 * - Runtime-registered paths
 * - Ant-style pattern matching
 * - Service-specific paths
 * - Path normalization
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicPathRegistry")
class PublicPathRegistryTest {

    @Mock
    private GatewayAuthProperties authProperties;

    private PublicPathRegistry registry;

    @BeforeEach
    void setUp() {
        when(authProperties.getGlobalPublicPaths()).thenReturn(List.of());
        when(authProperties.getServicePublicPaths("test-service")).thenReturn(List.of());
        registry = new PublicPathRegistry(authProperties);
    }

    @Nested
    @DisplayName("Default Public Paths")
    class DefaultPublicPaths {

        @ParameterizedTest
        @ValueSource(strings = {
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness",
            "/actuator/info"
        })
        @DisplayName("should recognize actuator health endpoints as public")
        void shouldRecognizeActuatorHealthAsPublic(String path) {
            assertThat(registry.isPublicPath(path)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
        })
        @DisplayName("should recognize auth endpoints as public")
        void shouldRecognizeAuthEndpointsAsPublic(String path) {
            assertThat(registry.isPublicPath(path)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/swagger-ui/swagger-ui.css",
            "/v3/api-docs",
            "/v3/api-docs/swagger-config"
        })
        @DisplayName("should recognize swagger/OpenAPI endpoints as public")
        void shouldRecognizeSwaggerAsPublic(String path) {
            assertThat(registry.isPublicPath(path)).isTrue();
        }

        @Test
        @DisplayName("should recognize favicon as public")
        void shouldRecognizeFaviconAsPublic() {
            assertThat(registry.isPublicPath("/favicon.ico")).isTrue();
        }
    }

    @Nested
    @DisplayName("Protected Paths")
    class ProtectedPaths {

        @ParameterizedTest
        @ValueSource(strings = {
            "/api/v1/patients",
            "/api/v1/patients/123",
            "/fhir/Patient",
            "/cql-engine/evaluate",
            "/quality-measure/calculate"
        })
        @DisplayName("should recognize API endpoints as protected")
        void shouldRecognizeApiEndpointsAsProtected(String path) {
            assertThat(registry.isPublicPath(path)).isFalse();
        }

        @Test
        @DisplayName("should recognize actuator metrics as protected")
        void shouldRecognizeActuatorMetricsAsProtected() {
            assertThat(registry.isPublicPath("/actuator/metrics")).isFalse();
            assertThat(registry.isPublicPath("/actuator/prometheus")).isFalse();
        }
    }

    @Nested
    @DisplayName("Configured Public Paths")
    class ConfiguredPublicPaths {

        @Test
        @DisplayName("should recognize configured global public paths")
        void shouldRecognizeConfiguredGlobalPaths() {
            // Given
            when(authProperties.getGlobalPublicPaths()).thenReturn(List.of(
                "/custom/public/**",
                "/webhook/github"
            ));
            registry = new PublicPathRegistry(authProperties);

            // Then
            assertThat(registry.isPublicPath("/custom/public/callback")).isTrue();
            assertThat(registry.isPublicPath("/custom/public/nested/path")).isTrue();
            assertThat(registry.isPublicPath("/webhook/github")).isTrue();
            assertThat(registry.isPublicPath("/webhook/gitlab")).isFalse();
        }

        @Test
        @DisplayName("should recognize service-specific public paths")
        void shouldRecognizeServiceSpecificPaths() {
            // Given
            when(authProperties.getServicePublicPaths("fhir-service")).thenReturn(List.of(
                "/fhir/metadata",
                "/fhir/.well-known/**"
            ));
            registry = new PublicPathRegistry(authProperties);

            // Then
            assertThat(registry.isPublicPathForService("/fhir/metadata", "fhir-service")).isTrue();
            assertThat(registry.isPublicPathForService("/fhir/.well-known/smart-configuration", "fhir-service")).isTrue();
            assertThat(registry.isPublicPathForService("/fhir/Patient", "fhir-service")).isFalse();
        }
    }

    @Nested
    @DisplayName("Runtime Registration")
    class RuntimeRegistration {

        @Test
        @DisplayName("should register runtime public path")
        void shouldRegisterRuntimePath() {
            // Given
            registry.registerPublicPath("/dynamic/public/**");

            // Then
            assertThat(registry.isPublicPath("/dynamic/public/endpoint")).isTrue();
        }

        @Test
        @DisplayName("should unregister runtime public path")
        void shouldUnregisterRuntimePath() {
            // Given
            registry.registerPublicPath("/temporary/public");
            assertThat(registry.isPublicPath("/temporary/public")).isTrue();

            // When
            registry.unregisterPublicPath("/temporary/public");

            // Then
            assertThat(registry.isPublicPath("/temporary/public")).isFalse();
        }

        @Test
        @DisplayName("should clear all runtime paths")
        void shouldClearAllRuntimePaths() {
            // Given
            registry.registerPublicPath("/runtime/path1");
            registry.registerPublicPath("/runtime/path2");
            assertThat(registry.isPublicPath("/runtime/path1")).isTrue();
            assertThat(registry.isPublicPath("/runtime/path2")).isTrue();

            // When
            registry.clearRuntimePaths();

            // Then
            assertThat(registry.isPublicPath("/runtime/path1")).isFalse();
            assertThat(registry.isPublicPath("/runtime/path2")).isFalse();
        }

        @Test
        @DisplayName("should ignore null pattern registration")
        void shouldIgnoreNullPattern() {
            // When
            registry.registerPublicPath(null);

            // Then - no exception
            assertThat(registry.getAllPublicPaths()).doesNotContain((String) null);
        }

        @Test
        @DisplayName("should ignore blank pattern registration")
        void shouldIgnoreBlankPattern() {
            // When
            registry.registerPublicPath("   ");

            // Then
            assertThat(registry.getAllPublicPaths()).doesNotContain("   ");
        }
    }

    @Nested
    @DisplayName("Pattern Matching")
    class PatternMatching {

        @Test
        @DisplayName("should match single wildcard patterns")
        void shouldMatchSingleWildcard() {
            // Given
            registry.registerPublicPath("/api/*/public");

            // Then
            assertThat(registry.isPublicPath("/api/v1/public")).isTrue();
            assertThat(registry.isPublicPath("/api/v2/public")).isTrue();
            assertThat(registry.isPublicPath("/api/v1/v2/public")).isFalse(); // Single * doesn't match /
        }

        @Test
        @DisplayName("should match double wildcard patterns")
        void shouldMatchDoubleWildcard() {
            // Given
            registry.registerPublicPath("/api/**/public");

            // Then
            assertThat(registry.isPublicPath("/api/v1/public")).isTrue();
            assertThat(registry.isPublicPath("/api/v1/v2/public")).isTrue();
            assertThat(registry.isPublicPath("/api/v1/v2/v3/public")).isTrue();
        }

        @Test
        @DisplayName("should match trailing double wildcard")
        void shouldMatchTrailingDoubleWildcard() {
            // Given
            registry.registerPublicPath("/static/**");

            // Then
            assertThat(registry.isPublicPath("/static/js/app.js")).isTrue();
            assertThat(registry.isPublicPath("/static/css/style.css")).isTrue();
            assertThat(registry.isPublicPath("/static")).isTrue();
        }
    }

    @Nested
    @DisplayName("Path Normalization")
    class PathNormalization {

        @Test
        @DisplayName("should handle paths with query strings")
        void shouldHandleQueryStrings() {
            assertThat(registry.isPublicPath("/actuator/health?details=true")).isTrue();
            assertThat(registry.isPublicPath("/api/v1/auth/login?redirect=/dashboard")).isTrue();
        }

        @Test
        @DisplayName("should handle trailing slashes")
        void shouldHandleTrailingSlashes() {
            assertThat(registry.isPublicPath("/actuator/health/")).isTrue();
            assertThat(registry.isPublicPath("/swagger-ui/")).isTrue();
        }

        @Test
        @DisplayName("should handle null path")
        void shouldHandleNullPath() {
            assertThat(registry.isPublicPath(null)).isFalse();
        }

        @Test
        @DisplayName("should handle empty path")
        void shouldHandleEmptyPath() {
            assertThat(registry.isPublicPath("")).isFalse();
        }

        @Test
        @DisplayName("should handle blank path")
        void shouldHandleBlankPath() {
            assertThat(registry.isPublicPath("   ")).isFalse();
        }
    }

    @Nested
    @DisplayName("getAllPublicPaths")
    class GetAllPublicPaths {

        @Test
        @DisplayName("should include default paths")
        void shouldIncludeDefaultPaths() {
            List<String> paths = registry.getAllPublicPaths();

            assertThat(paths).contains(
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/info",
                "/api/v1/auth/login",
                "/swagger-ui/**"
            );
        }

        @Test
        @DisplayName("should include configured paths")
        void shouldIncludeConfiguredPaths() {
            // Given
            when(authProperties.getGlobalPublicPaths()).thenReturn(List.of("/custom/path"));
            registry = new PublicPathRegistry(authProperties);

            // Then
            assertThat(registry.getAllPublicPaths()).contains("/custom/path");
        }

        @Test
        @DisplayName("should include runtime-registered paths")
        void shouldIncludeRuntimePaths() {
            // Given
            registry.registerPublicPath("/runtime/path");

            // Then
            assertThat(registry.getAllPublicPaths()).contains("/runtime/path");
        }
    }

    @Nested
    @DisplayName("getPublicPathsForService")
    class GetPublicPathsForService {

        @Test
        @DisplayName("should include global and service-specific paths")
        void shouldIncludeGlobalAndServicePaths() {
            // Given
            when(authProperties.getGlobalPublicPaths()).thenReturn(List.of("/global/path"));
            when(authProperties.getServicePublicPaths("my-service")).thenReturn(List.of("/my-service/public"));
            registry = new PublicPathRegistry(authProperties);

            // Then
            List<String> paths = registry.getPublicPathsForService("my-service");
            assertThat(paths).contains("/global/path", "/my-service/public");
        }
    }
}
