package com.healthdata.hedisadapter.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "external.hedis.enabled", havingValue = "true")
@RequiredArgsConstructor
public class HedisClientConfig {

    private final HedisProperties properties;

    @Bean("hedisRestTemplate")
    public RestTemplate hedisRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(properties.getBaseUrl())
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .additionalInterceptors(apiKeyInterceptor())
                .build();
    }

    @Bean("hedisCqlRestTemplate")
    public RestTemplate hedisCqlRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(properties.getCqlUrl())
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .additionalInterceptors(apiKeyInterceptor())
                .build();
    }

    private ClientHttpRequestInterceptor apiKeyInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().set("X-API-Key", properties.getApiKey());
            request.getHeaders().set("X-Source-System", "hdim");
            return execution.execute(request, body);
        };
    }
}
