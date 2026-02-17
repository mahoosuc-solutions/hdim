package com.healthdata.fhir.security.smart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.view.RedirectView;

@ExtendWith(MockitoExtension.class)
@DisplayName("SMART Conformance Lane")
class SmartConformanceLaneTest {

    @Mock
    private SmartAuthorizationService authorizationService;

    @Mock
    private SmartClientRepository clientRepository;

    @Mock
    private SmartLaunchContextStore launchContextStore;

    @Test
    @DisplayName("Standalone launch should preserve requested scope context")
    void shouldConformStandaloneLaunchFlow() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        SmartClient client = SmartClient.builder()
                .clientId("smart-standalone")
                .clientName("SMART Standalone App")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("patient/*.read", "fhirUser"))
                .build();

        when(clientRepository.findByClientIdAndActiveTrue("smart-standalone")).thenReturn(Optional.of(client));
        String codeChallenge = "standalone-s256-challenge";
        when(authorizationService.generateAuthorizationCode(
                eq("smart-standalone"), eq("http://app/callback"), any(), eq("state-1"),
                eq(codeChallenge), eq("S256"), any()))
            .thenReturn("auth-code");

        RedirectView view = controller.authorize(
                "code",
                "smart-standalone",
                "http://app/callback",
                "patient/*.read fhirUser",
                "state-1",
                "http://localhost/fhir",
                null,
                codeChallenge,
                "S256");

        assertThat(view.getUrl()).contains("code=auth-code");
        assertThat(view.getUrl()).contains("state=state-1");

        ArgumentCaptor<SmartLaunchContext> captor = ArgumentCaptor.forClass(SmartLaunchContext.class);
        verify(authorizationService).generateAuthorizationCode(
                eq("smart-standalone"), eq("http://app/callback"), any(), eq("state-1"),
                eq(codeChallenge), eq("S256"), captor.capture());
        SmartLaunchContext context = captor.getValue();
        assertThat(Boolean.TRUE.equals(context.getStandalone())).isTrue();
        assertThat(context.getPatient()).isEqualTo("demo-patient-001");
        assertThat(context.getFhirUser()).isEqualTo("Practitioner/demo-practitioner-001");
        assertThat(context.getAudience()).isEqualTo("http://localhost/fhir");
    }

    @Test
    @DisplayName("EHR launch should resolve launch payload context and retain launch continuity")
    void shouldConformEhrLaunchFlow() {
        SmartAuthorizationController controller = new SmartAuthorizationController(authorizationService, clientRepository, launchContextStore);
        SmartClient client = SmartClient.builder()
                .clientId("smart-ehr")
                .clientName("SMART EHR App")
                .clientType(SmartClient.ClientType.CONFIDENTIAL)
                .redirectUris(Set.of("http://app/callback"))
                .allowedScopes(Set.of("launch", "launch/patient", "launch/encounter", "fhirUser"))
                .build();

        when(clientRepository.findByClientIdAndActiveTrue("smart-ehr")).thenReturn(Optional.of(client));
        String codeChallenge = "ehr-s256-challenge";
        when(authorizationService.generateAuthorizationCode(
                eq("smart-ehr"), eq("http://app/callback"), any(), eq("state-2"),
                eq(codeChallenge), eq("S256"), any()))
            .thenReturn("ehr-auth-code");
        when(launchContextStore.storeLaunchContext(any())).thenReturn("lc-ehr-1");

        String payload = "{\"patient\":\"ehr-patient-123\",\"encounter\":\"ehr-enc-9\",\"fhirUser\":\"Practitioner/42\"}";
        String launchPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        RedirectView view = controller.authorize(
                "code",
                "smart-ehr",
                "http://app/callback",
                "launch launch/patient launch/encounter fhirUser",
                "state-2",
                "http://localhost/fhir",
                launchPayload,
                codeChallenge,
                "S256");

        assertThat(view.getUrl()).contains("code=ehr-auth-code");
        assertThat(view.getUrl()).contains("state=state-2");

        ArgumentCaptor<SmartLaunchContext> captor = ArgumentCaptor.forClass(SmartLaunchContext.class);
        verify(authorizationService).generateAuthorizationCode(
                eq("smart-ehr"), eq("http://app/callback"), any(), eq("state-2"),
                eq(codeChallenge), eq("S256"), captor.capture());
        SmartLaunchContext context = captor.getValue();
        assertThat(Boolean.FALSE.equals(context.getStandalone())).isTrue();
        assertThat(context.getLaunch()).isEqualTo("lc-ehr-1");
        assertThat(context.getPatient()).isEqualTo("ehr-patient-123");
        assertThat(context.getEncounter()).isEqualTo("ehr-enc-9");
        assertThat(context.getFhirUser()).isEqualTo("Practitioner/42");
        assertThat(context.getAudience()).isEqualTo("http://localhost/fhir");
    }
}
