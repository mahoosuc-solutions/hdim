package com.healthdata.fhir.security.smart;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SMART Configuration Controller Tests")
class SmartConfigurationControllerTest {

    @Test
    @DisplayName("Should return SMART configuration")
    void shouldReturnSmartConfiguration() {
        SmartConfigurationController controller = new SmartConfigurationController();
        ReflectionTestUtils.setField(controller, "authorizationEndpoint", "http://example.com/oauth/authorize");
        ReflectionTestUtils.setField(controller, "tokenEndpoint", "http://example.com/oauth/token");
        ReflectionTestUtils.setField(controller, "introspectionEndpoint", "http://example.com/oauth/introspect");
        ReflectionTestUtils.setField(controller, "revocationEndpoint", "http://example.com/oauth/revoke");
        ReflectionTestUtils.setField(controller, "userinfoEndpoint", "http://example.com/oauth/userinfo");
        ReflectionTestUtils.setField(controller, "jwksUri", "http://example.com/.well-known/jwks.json");
        ReflectionTestUtils.setField(controller, "registrationEndpoint", "http://example.com/oauth/register");

        var response = controller.getSmartConfiguration();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAuthorizationEndpoint()).isEqualTo("http://example.com/oauth/authorize");
        assertThat(response.getBody().getScopesSupported()).contains("launch");
    }
}
