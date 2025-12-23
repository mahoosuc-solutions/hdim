package com.healthdata.authentication.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuthHeaderConstants.
 */
@DisplayName("AuthHeaderConstants")
class AuthHeaderConstantsTest {

    @Nested
    @DisplayName("Header Names")
    class HeaderNames {

        @Test
        @DisplayName("should have correct prefix for auth headers")
        void shouldHaveCorrectPrefix() {
            assertThat(AuthHeaderConstants.AUTH_HEADER_PREFIX).isEqualTo("X-Auth-");
        }

        @Test
        @DisplayName("should have all required auth headers defined")
        void shouldHaveAllRequiredHeaders() {
            assertThat(AuthHeaderConstants.HEADER_USER_ID).isEqualTo("X-Auth-User-Id");
            assertThat(AuthHeaderConstants.HEADER_USERNAME).isEqualTo("X-Auth-Username");
            assertThat(AuthHeaderConstants.HEADER_TENANT_IDS).isEqualTo("X-Auth-Tenant-Ids");
            assertThat(AuthHeaderConstants.HEADER_ROLES).isEqualTo("X-Auth-Roles");
            assertThat(AuthHeaderConstants.HEADER_VALIDATED).isEqualTo("X-Auth-Validated");
            assertThat(AuthHeaderConstants.HEADER_TOKEN_ID).isEqualTo("X-Auth-Token-Id");
            assertThat(AuthHeaderConstants.HEADER_TOKEN_EXPIRES).isEqualTo("X-Auth-Token-Expires");
        }

        @Test
        @DisplayName("all auth headers should start with the prefix")
        void allHeadersShouldStartWithPrefix() {
            String[] headers = AuthHeaderConstants.getAllAuthHeaders();

            for (String header : headers) {
                assertThat(header).startsWith(AuthHeaderConstants.AUTH_HEADER_PREFIX);
            }
        }

        @Test
        @DisplayName("getAllAuthHeaders should return all auth headers")
        void getAllAuthHeadersShouldReturnAllHeaders() {
            String[] headers = AuthHeaderConstants.getAllAuthHeaders();

            assertThat(headers).containsExactlyInAnyOrder(
                AuthHeaderConstants.HEADER_USER_ID,
                AuthHeaderConstants.HEADER_USERNAME,
                AuthHeaderConstants.HEADER_TENANT_IDS,
                AuthHeaderConstants.HEADER_ROLES,
                AuthHeaderConstants.HEADER_VALIDATED,
                AuthHeaderConstants.HEADER_TOKEN_ID,
                AuthHeaderConstants.HEADER_TOKEN_EXPIRES
            );
        }
    }

    @Nested
    @DisplayName("Attribute Keys")
    class AttributeKeys {

        @Test
        @DisplayName("should have all required attribute keys defined")
        void shouldHaveAllRequiredAttributes() {
            assertThat(AuthHeaderConstants.ATTR_TENANT_IDS).isEqualTo("userTenantIds");
            assertThat(AuthHeaderConstants.ATTR_USER_ID).isEqualTo("userId");
            assertThat(AuthHeaderConstants.ATTR_USERNAME).isEqualTo("username");
            assertThat(AuthHeaderConstants.ATTR_ROLES).isEqualTo("userRoles");
        }
    }

    @Nested
    @DisplayName("isAuthHeader")
    class IsAuthHeader {

        @ParameterizedTest
        @ValueSource(strings = {
            "X-Auth-User-Id",
            "X-Auth-Username",
            "X-Auth-Tenant-Ids",
            "X-Auth-Roles",
            "X-Auth-Validated",
            "X-Auth-Custom"
        })
        @DisplayName("should return true for headers starting with X-Auth-")
        void shouldReturnTrueForAuthHeaders(String headerName) {
            assertThat(AuthHeaderConstants.isAuthHeader(headerName)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "Authorization",
            "Content-Type",
            "X-Request-Id",
            "X-Correlation-Id",
            "X-Forwarded-For"
        })
        @DisplayName("should return false for non-auth headers")
        void shouldReturnFalseForNonAuthHeaders(String headerName) {
            assertThat(AuthHeaderConstants.isAuthHeader(headerName)).isFalse();
        }

        @Test
        @DisplayName("should return false for null header")
        void shouldReturnFalseForNull() {
            assertThat(AuthHeaderConstants.isAuthHeader(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for empty header")
        void shouldReturnFalseForEmpty() {
            assertThat(AuthHeaderConstants.isAuthHeader("")).isFalse();
        }

        @Test
        @DisplayName("should be case-sensitive")
        void shouldBeCaseSensitive() {
            assertThat(AuthHeaderConstants.isAuthHeader("x-auth-user-id")).isFalse();
            assertThat(AuthHeaderConstants.isAuthHeader("X-AUTH-USER-ID")).isFalse();
        }
    }

    @Nested
    @DisplayName("Value Delimiter")
    class ValueDelimiter {

        @Test
        @DisplayName("should use comma as delimiter")
        void shouldUseCommaAsDelimiter() {
            assertThat(AuthHeaderConstants.VALUE_DELIMITER).isEqualTo(",");
        }
    }

    @Nested
    @DisplayName("Validation Signature")
    class ValidationSignature {

        @Test
        @DisplayName("should have correct signature prefix")
        void shouldHaveCorrectSignaturePrefix() {
            assertThat(AuthHeaderConstants.VALIDATED_SIGNATURE_PREFIX).isEqualTo("gateway-");
        }
    }
}
