package com.healthdata.fhir.security.smart;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmartSupportTest {

    @Test
    void shouldEvaluateSmartLaunchContext() {
        SmartLaunchContext context = SmartLaunchContext.builder()
                .patient("patient-1")
                .encounter("enc-1")
                .fhirUser("Practitioner/123")
                .audience("https://fhir.example")
                .clientId("client-1")
                .tenant("tenant-1")
                .standalone(true)
                .build();

        assertThat(context.hasPatientContext()).isTrue();
        assertThat(context.hasEncounterContext()).isTrue();
        assertThat(context.hasFhirUserContext()).isTrue();

        SmartLaunchContext emptyContext = new SmartLaunchContext();
        assertThat(emptyContext.hasPatientContext()).isFalse();
        assertThat(emptyContext.hasEncounterContext()).isFalse();
        assertThat(emptyContext.hasFhirUserContext()).isFalse();
    }

    @Test
    void shouldExposeTokenEndpointAuthMethods() {
        assertThat(SmartClient.TokenEndpointAuthMethod.valueOf("CLIENT_SECRET_BASIC"))
                .isEqualTo(SmartClient.TokenEndpointAuthMethod.CLIENT_SECRET_BASIC);
        assertThat(SmartClient.TokenEndpointAuthMethod.values())
                .contains(SmartClient.TokenEndpointAuthMethod.NONE);
    }

    @Test
    void shouldExposeAuthorizationExceptionDetails() {
        SmartAuthorizationException invalid = new SmartAuthorizationException("invalid_request", "bad request");
        assertThat(invalid.getErrorCode()).isEqualTo("invalid_request");
        assertThat(invalid.getErrorDescription()).isEqualTo("bad request");

        SmartAuthorizationException defaultError = new SmartAuthorizationException("missing");
        assertThat(defaultError.getErrorCode()).isEqualTo("invalid_request");
        assertThat(defaultError.getErrorDescription()).isEqualTo("missing");

        SmartAuthorizationException serverError = new SmartAuthorizationException("boom", new RuntimeException("cause"));
        assertThat(serverError.getErrorCode()).isEqualTo("server_error");
        assertThat(serverError.getErrorDescription()).isEqualTo("boom");
        assertThat(serverError.getCause()).isNotNull();
    }
}
