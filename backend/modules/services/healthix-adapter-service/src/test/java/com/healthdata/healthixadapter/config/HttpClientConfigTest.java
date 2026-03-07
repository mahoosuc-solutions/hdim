package com.healthdata.healthixadapter.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class HttpClientConfigTest {

    @Test
    void shouldCreatePooledRestTemplate() {
        HttpClientConfig config = new HttpClientConfig();
        RestTemplate restTemplate = config.pooledRestTemplate(200, 50, 3000, 10000);
        assertThat(restTemplate).isNotNull();
    }

    @Test
    void shouldCreateRestTemplateWithCustomPoolSize() {
        HttpClientConfig config = new HttpClientConfig();
        RestTemplate restTemplate = config.pooledRestTemplate(100, 25, 5000, 15000);
        assertThat(restTemplate).isNotNull();
    }
}
