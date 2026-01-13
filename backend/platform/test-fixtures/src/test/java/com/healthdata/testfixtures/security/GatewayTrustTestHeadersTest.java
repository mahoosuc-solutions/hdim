package com.healthdata.testfixtures.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to verify GatewayTrustTestHeaders generates the required X-Auth-Validated header.
 */
class GatewayTrustTestHeadersTest {

    @Test
    void shouldIncludeValidatedHeaderInAdminHeaders() {
        // When
        HttpHeaders headers = GatewayTrustTestHeaders.adminHeaders("tenant-001");

        // Then
        assertThat(headers.containsKey("X-Auth-Validated")).isTrue();
        String validatedHeader = headers.getFirst("X-Auth-Validated");
        assertThat(validatedHeader).isNotNull();
        assertThat(validatedHeader).startsWith("gateway-");
        assertThat(validatedHeader).contains("-dev-signature");
    }

    @Test
    void shouldIncludeValidatedHeaderInEvaluatorHeaders() {
        // When
        HttpHeaders headers = GatewayTrustTestHeaders.evaluatorHeaders("tenant-001");

        // Then
        assertThat(headers.containsKey("X-Auth-Validated")).isTrue();
        String validatedHeader = headers.getFirst("X-Auth-Validated");
        assertThat(validatedHeader).isNotNull();
        assertThat(validatedHeader).startsWith("gateway-");
    }

    @Test
    void shouldIncludeValidatedHeaderInViewerHeaders() {
        // When
        HttpHeaders headers = GatewayTrustTestHeaders.viewerHeaders("tenant-001");

        // Then
        assertThat(headers.containsKey("X-Auth-Validated")).isTrue();
        String validatedHeader = headers.getFirst("X-Auth-Validated");
        assertThat(validatedHeader).isNotNull();
        assertThat(validatedHeader).startsWith("gateway-");
    }

    @Test
    void shouldIncludeValidatedHeaderInAnalystHeaders() {
        // When
        HttpHeaders headers = GatewayTrustTestHeaders.analystHeaders("tenant-001");

        // Then
        assertThat(headers.containsKey("X-Auth-Validated")).isTrue();
        String validatedHeader = headers.getFirst("X-Auth-Validated");
        assertThat(validatedHeader).isNotNull();
        assertThat(validatedHeader).startsWith("gateway-");
    }

    @Test
    void shouldIncludeValidatedHeaderWhenBuildingCustomHeaders() {
        // When
        HttpHeaders headers = GatewayTrustTestHeaders.builder()
            .tenantId("tenant-001")
            .roles("CUSTOM_ROLE")
            .build();

        // Then
        assertThat(headers.containsKey("X-Auth-Validated")).isTrue();
        String validatedHeader = headers.getFirst("X-Auth-Validated");
        assertThat(validatedHeader).isNotNull();
        assertThat(validatedHeader).startsWith("gateway-");
        assertThat(validatedHeader).contains("-dev-signature");
    }

    @Test
    void shouldIncludeRolesHeaderInAdminHeaders() {
        // When
        HttpHeaders headers = GatewayTrustTestHeaders.adminHeaders("tenant-001");

        // Then
        assertThat(headers.containsKey("X-Auth-Roles")).isTrue();
        String rolesHeader = headers.getFirst("X-Auth-Roles");
        assertThat(rolesHeader).contains("ADMIN");
        assertThat(rolesHeader).contains("EVALUATOR");
        assertThat(rolesHeader).contains("VIEWER");
    }

    @Test
    void shouldIncludeRolesHeaderInEvaluatorHeaders() {
        // When
        HttpHeaders headers = GatewayTrustTestHeaders.evaluatorHeaders("tenant-001");

        // Then
        assertThat(headers.containsKey("X-Auth-Roles")).isTrue();
        String rolesHeader = headers.getFirst("X-Auth-Roles");
        assertThat(rolesHeader).contains("EVALUATOR");
        assertThat(rolesHeader).contains("VIEWER");
        assertThat(rolesHeader).doesNotContain("ADMIN");
    }

    @Test
    void shouldIncludeAllRequiredHeaders() {
        // When
        HttpHeaders headers = GatewayTrustTestHeaders.evaluatorHeaders("tenant-001");

        // Then
        assertThat(headers.containsKey("X-Tenant-ID")).isTrue();
        assertThat(headers.containsKey("X-Auth-User-Id")).isTrue();
        assertThat(headers.containsKey("X-Auth-Username")).isTrue();
        assertThat(headers.containsKey("X-Auth-Tenant-Ids")).isTrue();
        assertThat(headers.containsKey("X-Auth-Roles")).isTrue();
        assertThat(headers.containsKey("X-Auth-Validated")).isTrue();
    }
}
