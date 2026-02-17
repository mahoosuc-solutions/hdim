package com.healthdata.fhir.security.smart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.Claims;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

@ExtendWith(MockitoExtension.class)
@DisplayName("SMART Authorization Controller Tests")
@Tag("integration")
class SmartAuthorizationControllerTest {

    @Mock
    private SmartAuthorizationService authorizationService;

    @Mock
    private SmartClientRepository clientRepository;

    @Mock
    private SmartLaunchContextStore launchContextStore;

    @Test
    @DisplayName("Should authorize and redirect with code")
    void shouldAuthorize() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.PUBLIC)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("launch"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));
        when(authorizationService.generateAuthorizationCode(
                eq("client-1"), eq("http://app/callback"), any(), eq("state"),
                eq(null), eq("S256"), any()))
            .thenReturn("auth-code");

        RedirectView view = controller.authorize(
                "code",
                "client-1",
                "http://app/callback",
                "launch",
                "state",
                null,
                null,
                null,
                "S256");

        assertThat(view.getUrl()).contains("code=auth-code");
        assertThat(view.getUrl()).contains("state=state");
    }

    @Test
    @DisplayName("Should return error redirect for unsupported response type")
    void shouldReturnErrorRedirect() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.PUBLIC)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("launch"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        RedirectView view = controller.authorize(
                "token",
                "client-1",
                "http://app/callback",
                "launch",
                "state",
                null,
                null,
                null,
                "S256");

        assertThat(view.getUrl()).contains("unsupported_response_type");
    }

    @Test
    @DisplayName("Should return error redirect for invalid redirect URI")
    void shouldReturnErrorRedirectForInvalidRedirectUri() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.PUBLIC)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("launch"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        RedirectView view = controller.authorize(
                "code",
                "client-1",
                "http://bad/callback",
                "launch",
                "state",
                null,
                null,
                null,
                "S256");

        assertThat(view.getUrl()).contains("invalid_request");
    }

    @Test
    @DisplayName("Should return error redirect for invalid scope")
    void shouldReturnErrorRedirectForInvalidScope() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.PUBLIC)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("launch"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));

        RedirectView view = controller.authorize(
                "code",
                "client-1",
                "http://app/callback",
                "launch openid",
                "state",
                null,
                null,
                null,
                "S256");

        assertThat(view.getUrl()).contains("invalid_scope");
    }

    @Test
    @DisplayName("Should return error redirect when client missing")
    void shouldReturnErrorRedirectWhenClientMissing() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        when(clientRepository.findByClientIdAndActiveTrue("missing")).thenReturn(Optional.empty());

        RedirectView view = controller.authorize(
                "code",
                "missing",
                "http://app/callback",
                "launch",
                "state",
                null,
                null,
                null,
                "S256");

        assertThat(view.getUrl()).contains("invalid_client");
    }

    @Test
    @DisplayName("Should include launch context from scopes")
    void shouldIncludeLaunchContextFromScopes() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.PUBLIC)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("launch/patient", "launch/encounter", "fhirUser"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));
        when(authorizationService.generateAuthorizationCode(
                eq("client-1"), eq("http://app/callback"), any(), eq("state"),
                eq(null), eq("S256"), any()))
            .thenReturn("auth-code");

        controller.authorize(
                "code",
                "client-1",
                "http://app/callback",
                "launch/patient launch/encounter fhirUser",
                "state",
                "aud",
                "launch-1",
                null,
                "S256");

        ArgumentCaptor<SmartLaunchContext> captor = ArgumentCaptor.forClass(SmartLaunchContext.class);
        verify(authorizationService).generateAuthorizationCode(
                eq("client-1"), eq("http://app/callback"), any(), eq("state"),
                eq(null), eq("S256"), captor.capture());
        SmartLaunchContext context = captor.getValue();
        assertThat(context.getLaunch()).isEqualTo("launch-1");
        assertThat(Boolean.FALSE.equals(context.getStandalone())).isTrue();
        assertThat(context.getPatient()).isEqualTo("demo-patient-001");
        assertThat(context.getEncounter()).isEqualTo("demo-encounter-001");
        assertThat(context.getFhirUser()).isEqualTo("Practitioner/demo-practitioner-001");
        assertThat(context.getAudience()).isEqualTo("aud");
    }

    @Test
    @DisplayName("Should extract EHR launch context from base64url launch payload")
    void shouldExtractLaunchContextFromPayload() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.PUBLIC)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("launch/patient", "launch/encounter", "fhirUser"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));
        when(authorizationService.generateAuthorizationCode(
                eq("client-1"), eq("http://app/callback"), any(), eq("state"),
                eq(null), eq("S256"), any()))
            .thenReturn("auth-code");
        when(launchContextStore.storeLaunchContext(any())).thenReturn("lc-test-123");

        String launchPayloadJson = "{\"patient\":\"ehr-patient-42\",\"encounter\":\"ehr-enc-7\",\"fhirUser\":\"Practitioner/abc\",\"tenant\":\"acme-health\",\"intent\":\"chart-review\",\"need_patient_banner\":true}";
        String launchToken = Base64.getUrlEncoder().withoutPadding().encodeToString(launchPayloadJson.getBytes(StandardCharsets.UTF_8));

        controller.authorize(
                "code",
                "client-1",
                "http://app/callback",
                "launch/patient launch/encounter fhirUser",
                "state",
                "aud",
                launchToken,
                null,
                "S256");

        ArgumentCaptor<SmartLaunchContext> captor = ArgumentCaptor.forClass(SmartLaunchContext.class);
        verify(authorizationService).generateAuthorizationCode(
                eq("client-1"), eq("http://app/callback"), any(), eq("state"),
                eq(null), eq("S256"), captor.capture());
        SmartLaunchContext context = captor.getValue();
        assertThat(context.getLaunch()).isEqualTo("lc-test-123");
        assertThat(context.getPatient()).isEqualTo("ehr-patient-42");
        assertThat(context.getEncounter()).isEqualTo("ehr-enc-7");
        assertThat(context.getFhirUser()).isEqualTo("Practitioner/abc");
        assertThat(context.getTenant()).isEqualTo("acme-health");
        assertThat(context.getIntent()).isEqualTo("chart-review");
        assertThat(context.getNeedPatientBanner()).isTrue();
        assertThat(Boolean.FALSE.equals(context.getStandalone())).isTrue();
    }

    @Test
    @DisplayName("Should resolve opaque launch ID from server-side launch context store")
    void shouldResolveOpaqueLaunchIdFromStore() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.PUBLIC)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("launch/patient", "launch/encounter", "fhirUser"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));
        when(authorizationService.generateAuthorizationCode(
                eq("client-1"), eq("http://app/callback"), any(), eq("state"),
                eq(null), eq("S256"), any()))
            .thenReturn("auth-code");
        when(launchContextStore.resolveLaunchContext("opaque-launch-id")).thenReturn(Optional.of(Map.of(
            "patient", "ehr-patient-777",
            "encounter", "ehr-enc-777",
            "fhirUser", "Practitioner/opaque",
            "tenant", "acme"
        )));

        controller.authorize(
                "code",
                "client-1",
                "http://app/callback",
                "launch/patient launch/encounter fhirUser",
                "state",
                "aud",
                "opaque-launch-id",
                null,
                "S256");

        ArgumentCaptor<SmartLaunchContext> captor = ArgumentCaptor.forClass(SmartLaunchContext.class);
        verify(authorizationService).generateAuthorizationCode(
                eq("client-1"), eq("http://app/callback"), any(), eq("state"),
                eq(null), eq("S256"), captor.capture());
        SmartLaunchContext context = captor.getValue();
        assertThat(context.getLaunch()).isEqualTo("opaque-launch-id");
        assertThat(context.getPatient()).isEqualTo("ehr-patient-777");
        assertThat(context.getEncounter()).isEqualTo("ehr-enc-777");
        assertThat(context.getFhirUser()).isEqualTo("Practitioner/opaque");
        assertThat(context.getTenant()).isEqualTo("acme");
    }

    @Test
    @DisplayName("Should return server error redirect on exception")
    void shouldReturnServerErrorRedirect() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        SmartClient client = SmartClient.builder()
                .clientId("client-1")
                .clientName("Test")
                .clientType(SmartClient.ClientType.PUBLIC)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("launch"))
                .build();
        when(clientRepository.findByClientIdAndActiveTrue("client-1")).thenReturn(Optional.of(client));
        when(authorizationService.generateAuthorizationCode(
                eq("client-1"), eq("http://app/callback"), any(), eq("state"),
                eq(null), eq("S256"), any()))
            .thenThrow(new RuntimeException("boom"));

        RedirectView view = controller.authorize(
                "code",
                "client-1",
                "http://app/callback",
                "launch",
                "state",
                null,
                null,
                null,
                "S256");

        assertThat(view.getUrl()).contains("server_error");
    }

    @Test
    @DisplayName("Should exchange authorization code for token")
    void shouldExchangeToken() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        TokenResponse token = TokenResponse.builder()
                .accessToken("token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();
        when(authorizationService.exchangeAuthorizationCode("code", "client-1", "http://app/callback", "verifier"))
                .thenReturn(token);

        ResponseEntity<?> response = controller.token(
                "authorization_code",
                "code",
                "http://app/callback",
                "client-1",
                null,
                "verifier",
                null,
                null,
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(token);
    }

    @Test
    @DisplayName("Should return error for missing auth code params")
    void shouldReturnErrorForMissingAuthCodeParams() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);

        ResponseEntity<?> response = controller.token(
                "authorization_code",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("invalid_request");
    }

    @Test
    @DisplayName("Should refresh access token")
    void shouldRefreshToken() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        TokenResponse token = TokenResponse.builder()
                .accessToken("token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();
        when(authorizationService.refreshAccessToken("refresh", "client-1")).thenReturn(token);

        ResponseEntity<?> response = controller.token(
                "refresh_token",
                null,
                null,
                "client-1",
                null,
                null,
                "refresh",
                null,
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(token);
    }

    @Test
    @DisplayName("Should return error for missing refresh token")
    void shouldReturnErrorForMissingRefreshToken() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);

        ResponseEntity<?> response = controller.token(
                "refresh_token",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("invalid_request");
    }

    @Test
    @DisplayName("Should return error for missing client credentials")
    void shouldReturnErrorForMissingClientCredentials() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);

        ResponseEntity<?> response = controller.token(
                "client_credentials",
                null,
                null,
                "client-1",
                null,
                null,
                null,
                null,
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("invalid_request");
    }

    @Test
    @DisplayName("Should handle authorization service errors in token endpoint")
    void shouldHandleAuthorizationServiceErrorsInTokenEndpoint() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        doThrow(new SmartAuthorizationException("invalid_grant", "bad grant"))
                .when(authorizationService).exchangeAuthorizationCode("code", "client-1", "http://app/callback", "verifier");

        ResponseEntity<?> response = controller.token(
                "authorization_code",
                "code",
                "http://app/callback",
                "client-1",
                null,
                "verifier",
                null,
                null,
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("invalid_grant");
    }

    @Test
    @DisplayName("Should return server error on token exception")
    void shouldReturnServerErrorOnTokenException() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        doThrow(new RuntimeException("boom"))
                .when(authorizationService).exchangeAuthorizationCode("code", "client-1", "http://app/callback", null);

        ResponseEntity<?> response = controller.token(
                "authorization_code",
                "code",
                "http://app/callback",
                "client-1",
                null,
                null,
                null,
                null,
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("server_error");
    }

    @Test
    @DisplayName("Should handle invalid grant type")
    void shouldHandleInvalidGrantType() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);

        ResponseEntity<?> response = controller.token(
                "unknown",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("unsupported_grant_type");
    }

    @Test
    @DisplayName("Should return userinfo for valid token")
    void shouldReturnUserinfo() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(claims.containsKey("patient")).thenReturn(true);
        when(claims.containsKey("fhirUser")).thenReturn(true);
        when(claims.get("patient")).thenReturn("patient-1");
        when(claims.get("fhirUser")).thenReturn("Practitioner/1");
        when(authorizationService.validateAccessToken("token")).thenReturn(claims);

        ResponseEntity<?> response = controller.userinfo("Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) response.getBody()).get("sub")).isEqualTo("user-1");
        assertThat(((Map<?, ?>) response.getBody()).get("fhirUser")).isEqualTo("Practitioner/1");
    }

    @Test
    @DisplayName("Should return error for missing bearer token")
    void shouldReturnErrorForMissingBearerToken() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);

        ResponseEntity<?> response = controller.userinfo("Token token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("invalid_token");
    }

    @Test
    @DisplayName("Should return error for invalid userinfo token")
    void shouldReturnErrorForInvalidUserinfoToken() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        doThrow(new IllegalArgumentException("bad")).when(authorizationService).validateAccessToken("token");

        ResponseEntity<?> response = controller.userinfo("Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("invalid_token");
    }

    @Test
    @DisplayName("Should introspect token")
    void shouldIntrospectToken() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(claims.get("client_id")).thenReturn("client-1");
        when(claims.get("scope")).thenReturn("launch");
        when(claims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() + 10000));
        when(claims.getIssuedAt()).thenReturn(new java.util.Date(System.currentTimeMillis() - 1000));
        when(claims.getIssuer()).thenReturn("issuer");
        when(claims.getAudience()).thenReturn(java.util.Set.of("aud"));
        when(authorizationService.validateAccessToken("token")).thenReturn(claims);

        ResponseEntity<?> response = controller.introspect("token", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) response.getBody()).get("active")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should return inactive on introspect error")
    void shouldReturnInactiveOnIntrospectError() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        doThrow(new IllegalArgumentException("bad")).when(authorizationService).validateAccessToken("token");

        ResponseEntity<?> response = controller.introspect("token", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) response.getBody()).get("active")).isEqualTo(false);
    }

    @Test
    @DisplayName("Should revoke token")
    void shouldRevokeToken() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);

        ResponseEntity<Void> response = controller.revoke("token", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authorizationService).revokeToken("token", null);
    }

    @Test
    @DisplayName("Should parse basic auth credentials")
    void shouldParseBasicAuthCredentials() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        TokenResponse token = TokenResponse.builder().accessToken("token").tokenType("Bearer").build();
        when(authorizationService.clientCredentialsGrant("client-1", "secret", Set.of()))
                .thenReturn(token);

        String authHeader = "Basic " + Base64.getEncoder().encodeToString("client-1:secret".getBytes());
        ResponseEntity<?> response = controller.token(
                "client_credentials",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                authHeader);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
