package com.healthdata.migration.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@DisplayName("CdrProcessorClientConfig")
class CdrProcessorClientConfigTest {

    @Test
    @DisplayName("Should build WebClient with base URL")
    void shouldBuildWebClient() {
        CdrProcessorClientConfig config = new CdrProcessorClientConfig();
        ReflectionTestUtils.setField(config, "cdrProcessorUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(config, "connectTimeout", Duration.ofSeconds(1));
        ReflectionTestUtils.setField(config, "readTimeout", Duration.ofSeconds(2));

        WebClient client = config.cdrProcessorWebClient(WebClient.builder());

        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("Should execute request/response filters")
    void shouldExecuteFilters() {
        CdrProcessorClientConfig config = new CdrProcessorClientConfig();
        ExchangeFilterFunction requestFilter = ReflectionTestUtils.invokeMethod(config, "logRequest");
        ExchangeFilterFunction responseFilter = ReflectionTestUtils.invokeMethod(config, "logResponse");

        ClientRequest request = ClientRequest.create(HttpMethod.GET, java.net.URI.create("http://localhost"))
                .build();
        ExchangeFunction exchange = req -> Mono.just(ClientResponse.create(HttpStatus.OK).build());

        requestFilter.filter(request, exchange).block();
        responseFilter.filter(request, exchange).block();
    }
}
