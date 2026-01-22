package com.healthdata.patient.client;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FHIR Service Client Configuration Tests")
class FhirServiceClientConfigurationTest {

    private final FhirServiceClientConfiguration configuration = new FhirServiceClientConfiguration();

    @Test
    @DisplayName("Should provide FULL logging level")
    void shouldProvideFullLogging() {
        assertThat(configuration.fhirServiceLoggerLevel()).isEqualTo(Logger.Level.FULL);
    }

    @Test
    @DisplayName("Should apply FHIR JSON headers")
    void shouldApplyFhirHeaders() {
        RequestInterceptor interceptor = configuration.fhirContentTypeInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("Accept")).contains("application/fhir+json");
        assertThat(template.headers().get("Content-Type")).contains("application/fhir+json");
    }
}
