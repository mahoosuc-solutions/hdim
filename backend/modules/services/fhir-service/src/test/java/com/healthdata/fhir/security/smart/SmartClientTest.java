package com.healthdata.fhir.security.smart;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SMART Client Tests")
class SmartClientTest {

    @Test
    @DisplayName("Should validate scopes, grant types, and redirect URIs")
    void shouldValidateScopesAndUris() {
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("launch", "openid"))
                .grantTypes(Set.of("authorization_code", "refresh_token"))
                .build();

        assertThat(client.supportsGrantType("authorization_code")).isTrue();
        assertThat(client.supportsGrantType("client_credentials")).isFalse();
        assertThat(client.isAllowedScope("openid")).isTrue();
        assertThat(client.isAllowedScope("profile")).isFalse();
        assertThat(client.isValidRedirectUri("http://app/callback")).isTrue();
        assertThat(client.isValidRedirectUri("http://bad/callback")).isFalse();
    }

    @Test
    @DisplayName("Should report client type")
    void shouldReportClientType() {
        SmartClient publicClient = SmartClient.builder()
                .clientId("public")
                .clientName("Public")
                .clientType(SmartClient.ClientType.PUBLIC)
                .build();

        SmartClient confidentialClient = SmartClient.builder()
                .clientId("conf")
                .clientName("Confidential")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .build();

        assertThat(publicClient.isPublicClient()).isTrue();
        assertThat(publicClient.isConfidentialClient()).isFalse();
        assertThat(confidentialClient.isConfidentialClient()).isTrue();
        assertThat(confidentialClient.isPublicClient()).isFalse();
    }
}
