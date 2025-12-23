package com.healthdata.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@DisplayName("RestTemplateConfig")
class RestTemplateConfigTest {

    @Test
    @DisplayName("Should build RestTemplate with timeouts")
    void shouldBuildRestTemplateWithTimeouts() {
        RestTemplateConfig config = new RestTemplateConfig();

        RestTemplate restTemplate = config.restTemplate();

        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);

        SimpleClientHttpRequestFactory factory =
            (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        Integer connectTimeout = (Integer) ReflectionTestUtils.getField(factory, "connectTimeout");
        Integer readTimeout = (Integer) ReflectionTestUtils.getField(factory, "readTimeout");

        assertThat(connectTimeout).isNotNull();
        assertThat(readTimeout).isNotNull();
        assertThat(connectTimeout).isEqualTo(5000);
        assertThat(readTimeout).isEqualTo(30000);
    }
}
