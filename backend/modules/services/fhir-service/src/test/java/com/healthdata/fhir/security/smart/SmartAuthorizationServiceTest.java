package com.healthdata.fhir.security.smart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SMART Authorization Service Tests")
class SmartAuthorizationServiceTest {

    @Mock
    private SmartClientRepository clientRepository;

    @Test
    @DisplayName("Should exchange authorization code with PKCE")
    void shouldExchangeAuthorizationCodeWithPkce() throws Exception {
        SmartAuthorizationService service = buildService();

        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .accessTokenLifetime(3600)
                .refreshTokenLifetime(3600)
                .allowedScopes(Set.of("launch", "offline_access"))
                .redirectUris(Set.of("http://app/callback"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        String codeVerifier = "verifier-123";
        String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII)));

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("launch", "offline_access"),
                "state",
                codeChallenge,
                "S256",
                SmartLaunchContext.builder().patient("patient-1").build());

        TokenResponse response = service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", codeVerifier);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getPatient()).isEqualTo("patient-1");
    }

    @Test
    @DisplayName("Should reject invalid authorization code")
    void shouldRejectInvalidAuthorizationCode() {
        SmartAuthorizationService service = buildService();

        assertThatThrownBy(() -> service.exchangeAuthorizationCode(
                "bad-code", "client-1", "http://app/callback", "verifier"))
            .isInstanceOf(SmartAuthorizationException.class);
    }

    @Test
    @DisplayName("Should reject expired authorization code")
    void shouldRejectExpiredAuthorizationCode() {
        SmartAuthorizationService service = buildService();
        ReflectionTestUtils.setField(service, "authorizationCodeLifetime", -1);

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("launch"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder().build());

        assertThatThrownBy(() -> service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", null))
            .isInstanceOf(SmartAuthorizationException.class)
            .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should reject client ID mismatch")
    void shouldRejectClientIdMismatch() {
        SmartAuthorizationService service = buildService();

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("launch"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder().build());

        assertThatThrownBy(() -> service.exchangeAuthorizationCode(
                code, "client-2", "http://app/callback", null))
            .isInstanceOf(SmartAuthorizationException.class)
            .hasMessageContaining("Client ID mismatch");
    }

    @Test
    @DisplayName("Should reject redirect URI mismatch")
    void shouldRejectRedirectMismatch() {
        SmartAuthorizationService service = buildService();

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("launch"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder().build());

        assertThatThrownBy(() -> service.exchangeAuthorizationCode(
                code, "client-1", "http://app/other", null))
            .isInstanceOf(SmartAuthorizationException.class)
            .hasMessageContaining("Redirect URI mismatch");
    }

    @Test
    @DisplayName("Should reject missing code verifier")
    void shouldRejectMissingCodeVerifier() {
        SmartAuthorizationService service = buildService();

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("launch"),
                null,
                "challenge",
                "plain",
                SmartLaunchContext.builder().build());

        assertThatThrownBy(() -> service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", null))
            .isInstanceOf(SmartAuthorizationException.class)
            .hasMessageContaining("Code verifier required");
    }

    @Test
    @DisplayName("Should reject invalid code verifier")
    void shouldRejectInvalidCodeVerifier() {
        SmartAuthorizationService service = buildService();

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("launch"),
                null,
                "challenge",
                "plain",
                SmartLaunchContext.builder().build());

        assertThatThrownBy(() -> service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", "wrong"))
            .isInstanceOf(SmartAuthorizationException.class)
            .hasMessageContaining("Invalid code verifier");
    }

    @Test
    @DisplayName("Should reject unsupported PKCE method")
    void shouldRejectUnsupportedPkceMethod() {
        SmartAuthorizationService service = buildService();

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("launch"),
                null,
                "challenge",
                "bad",
                SmartLaunchContext.builder().build());

        assertThatThrownBy(() -> service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", "challenge"))
            .isInstanceOf(SmartAuthorizationException.class)
            .hasMessageContaining("Invalid code verifier");
    }

    @Test
    @DisplayName("Should reject client not found on exchange")
    void shouldRejectClientNotFoundOnExchange() {
        SmartAuthorizationService service = buildService();

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("launch"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder().build());

        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", null))
            .isInstanceOf(SmartAuthorizationException.class)
            .hasMessageContaining("Client not found");
    }

    @Test
    @DisplayName("Should refresh access token")
    void shouldRefreshAccessToken() {
        SmartAuthorizationService service = buildService();

        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .accessTokenLifetime(3600)
                .refreshTokenLifetime(3600)
                .allowedScopes(Set.of("offline_access"))
                .redirectUris(Set.of("http://app/callback"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("offline_access"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder().build());

        TokenResponse initial = service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", null);
        TokenResponse refreshed = service.refreshAccessToken(initial.getRefreshToken(), "client-1");

        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getScope()).contains("offline_access");
    }

    @Test
    @DisplayName("Should reject invalid refresh token")
    void shouldRejectInvalidRefreshToken() {
        SmartAuthorizationService service = buildService();

        assertThatThrownBy(() -> service.refreshAccessToken("missing", "client-1"))
            .isInstanceOf(SmartAuthorizationException.class)
            .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("Should reject expired refresh token")
    void shouldRejectExpiredRefreshToken() {
        SmartAuthorizationService service = buildService();

        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .accessTokenLifetime(3600)
                .refreshTokenLifetime(3600)
                .allowedScopes(Set.of("offline_access"))
                .redirectUris(Set.of("http://app/callback"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("offline_access"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder().build());
        TokenResponse initial = service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> refreshTokens = (Map<String, Object>) ReflectionTestUtils.getField(service, "refreshTokens");
        Object tokenData = refreshTokens.get(initial.getRefreshToken());
        ReflectionTestUtils.setField(tokenData, "expiresAt", Instant.now().minusSeconds(5));

        assertThatThrownBy(() -> service.refreshAccessToken(initial.getRefreshToken(), "client-1"))
            .isInstanceOf(SmartAuthorizationException.class)
            .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should reject refresh token client mismatch")
    void shouldRejectRefreshTokenClientMismatch() {
        SmartAuthorizationService service = buildService();

        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .accessTokenLifetime(3600)
                .refreshTokenLifetime(3600)
                .allowedScopes(Set.of("offline_access"))
                .redirectUris(Set.of("http://app/callback"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("offline_access"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder().build());
        TokenResponse initial = service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", null);

        assertThatThrownBy(() -> service.refreshAccessToken(initial.getRefreshToken(), "client-2"))
            .isInstanceOf(SmartAuthorizationException.class)
            .hasMessageContaining("Client ID mismatch");
    }

    @Test
    @DisplayName("Should reject client credentials grant for public client")
    void shouldRejectClientCredentialsForPublicClient() {
        SmartAuthorizationService service = buildService();
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.PUBLIC)
                .clientSecret("secret")
                .allowedScopes(Set.of("launch"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.clientCredentialsGrant("client-1", "secret", Set.of("launch")))
                .isInstanceOf(SmartAuthorizationException.class);
    }

    @Test
    @DisplayName("Should reject invalid client credentials")
    void shouldRejectInvalidClientCredentials() {
        SmartAuthorizationService service = buildService();
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .allowedScopes(Set.of("launch"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.clientCredentialsGrant("client-1", "wrong", Set.of("launch")))
                .isInstanceOf(SmartAuthorizationException.class)
                .hasMessageContaining("Invalid client credentials");
    }

    @Test
    @DisplayName("Should reject invalid client credentials scopes")
    void shouldRejectInvalidClientCredentialsScopes() {
        SmartAuthorizationService service = buildService();
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .allowedScopes(Set.of("launch"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.clientCredentialsGrant("client-1", "secret", Set.of("openid")))
                .isInstanceOf(SmartAuthorizationException.class)
                .hasMessageContaining("Scope not allowed");
    }

    @Test
    @DisplayName("Should issue client credentials token")
    void shouldIssueClientCredentialsToken() {
        SmartAuthorizationService service = buildService();
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .accessTokenLifetime(3600)
                .allowedScopes(Set.of("launch"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        TokenResponse response = service.clientCredentialsGrant("client-1", "secret", Set.of("launch"));

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getScope()).contains("launch");
    }

    @Test
    @DisplayName("Should revoke refresh token")
    void shouldRevokeRefreshToken() {
        SmartAuthorizationService service = buildService();

        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .accessTokenLifetime(3600)
                .refreshTokenLifetime(3600)
                .allowedScopes(Set.of("offline_access"))
                .redirectUris(Set.of("http://app/callback"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("offline_access"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder().build());
        TokenResponse initial = service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> refreshTokens = (Map<String, Object>) ReflectionTestUtils.getField(service, "refreshTokens");
        assertThat(refreshTokens).containsKey(initial.getRefreshToken());

        service.revokeToken(initial.getRefreshToken(), null);

        assertThat(refreshTokens).doesNotContainKey(initial.getRefreshToken());
    }

    @Test
    @DisplayName("Should reject refresh when client not found")
    void shouldRejectRefreshWhenClientNotFound() {
        SmartAuthorizationService service = buildService();

        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .accessTokenLifetime(3600)
                .refreshTokenLifetime(3600)
                .allowedScopes(Set.of("offline_access"))
                .redirectUris(Set.of("http://app/callback"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("offline_access"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder().build());
        TokenResponse initial = service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", null);

        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refreshAccessToken(initial.getRefreshToken(), "client-1"))
                .isInstanceOf(SmartAuthorizationException.class)
                .hasMessageContaining("Client not found");
    }

    @Test
    @DisplayName("Should validate access token claims")
    void shouldValidateAccessTokenClaims() {
        SmartAuthorizationService service = buildService();

        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .accessTokenLifetime(3600)
                .refreshTokenLifetime(3600)
                .allowedScopes(Set.of("launch"))
                .redirectUris(Set.of("http://app/callback"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("launch"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder().build());

        TokenResponse response = service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", null);

        var claims = service.validateAccessToken(response.getAccessToken());
        assertThat(claims.getSubject()).isEqualTo("client-1");
        assertThat(claims.get("scope")).isEqualTo("launch");
    }

    @Test
    @DisplayName("Should include launch context claims in access token")
    void shouldIncludeLaunchContextClaimsInAccessToken() {
        SmartAuthorizationService service = buildService();

        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .clientSecret("secret")
                .accessTokenLifetime(3600)
                .refreshTokenLifetime(3600)
                .allowedScopes(Set.of("launch"))
                .redirectUris(Set.of("http://app/callback"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        String code = service.generateAuthorizationCode(
                "client-1",
                "http://app/callback",
                Set.of("launch"),
                null,
                null,
                "plain",
                SmartLaunchContext.builder()
                        .patient("patient-1")
                        .encounter("enc-1")
                        .fhirUser("Practitioner/1")
                        .tenant("tenant-1")
                        .build());

        TokenResponse response = service.exchangeAuthorizationCode(
                code, "client-1", "http://app/callback", null);

        var claims = service.validateAccessToken(response.getAccessToken());
        assertThat(claims.get("patient")).isEqualTo("patient-1");
        assertThat(claims.get("encounter")).isEqualTo("enc-1");
        assertThat(claims.get("fhirUser")).isEqualTo("Practitioner/1");
        assertThat(claims.get("tenant")).isEqualTo("tenant-1");
    }

    private SmartAuthorizationService buildService() {
        SmartAuthorizationService service = new SmartAuthorizationService(clientRepository);
        ReflectionTestUtils.setField(service, "issuer", "http://example.com/fhir");
        ReflectionTestUtils.setField(service, "jwtSecret", "smart-on-fhir-secret-key-minimum-256-bits-required");
        ReflectionTestUtils.setField(service, "authorizationCodeLifetime", 600);
        return service;
    }
}
