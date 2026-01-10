package com.healthdata.cms.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * RestTemplate Configuration for CMS API Clients
 * 
 * Configures HTTP client with:
 * - Connection pooling
 * - Timeouts (1s connection, 5s read)
 * - Logging interceptors
 * - Resilience patterns (circuit breaker, retry)
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    /**
     * Configure RestTemplate for CMS API calls
     * - HTTP connection pooling
     * - Timeouts: 1s connection, 5s read
     * - Logging interceptor for debugging
     */
    @Bean("cmsRestTemplate")
    public RestTemplate cmsRestTemplate(RestTemplateBuilder builder) {
        log.info("Configuring CMS RestTemplate with connection pooling and timeouts");
        
        // Configure HTTP client connection manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50); // Max total connections
        connectionManager.setDefaultMaxPerRoute(10); // Max per route
        
        // Configure request timeout
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(1))
            .setResponseTimeout(Timeout.ofSeconds(5))
            .setConnectionRequestTimeout(Timeout.ofSeconds(1))
            .build();
        
        // Build HttpClient with config
        HttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();
        
        // Create request factory
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(1000); // 1 second
        factory.setReadTimeout(5000); // 5 seconds
        factory.setConnectionRequestTimeout(1000);
        
        return builder
            .requestFactory(() -> new BufferingClientHttpRequestFactory(factory))
            .setConnectTimeout(java.time.Duration.ofSeconds(1))
            .setReadTimeout(java.time.Duration.ofSeconds(5))
            .interceptors((request, body, execution) -> {
                log.debug("CMS API Request: {} {}", request.getMethod(), request.getURI());
                return execution.execute(request, body);
            })
            .build();
    }

    /**
     * Configure RestTemplate for OAuth2 token endpoint
     * - Shorter timeouts (1s total)
     * - Dedicated instance for token requests
     */
    @Bean("oauth2RestTemplate")
    public RestTemplate oauth2RestTemplate(RestTemplateBuilder builder) {
        log.info("Configuring OAuth2 RestTemplate with short timeouts");
        
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(10);
        connectionManager.setDefaultMaxPerRoute(5);
        
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(500))
            .setResponseTimeout(Timeout.ofMilliseconds(1000))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(500))
            .build();
        
        HttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();
        
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(500);
        factory.setReadTimeout(1000);
        
        return builder
            .requestFactory(() -> new BufferingClientHttpRequestFactory(factory))
            .setConnectTimeout(java.time.Duration.ofMillis(500))
            .setReadTimeout(java.time.Duration.ofMillis(1000))
            .build();
    }

    /**
     * Register circuit breaker event logging
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer(CircuitBreakerRegistry registry) {
        registry.getEventPublisher()
            .onEntryAdded(event -> log.info("Circuit breaker registered: {}", event.getAddedEntry().getName()))
            .onEntryRemoved(event -> log.info("Circuit breaker removed: {}", event.getRemovedEntry().getName()));
        
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                CircuitBreaker circuitBreaker = event.getAddedEntry();
                circuitBreaker.getEventPublisher()
                    .onStateTransition(e -> log.warn("CircuitBreaker {} changed state from {} to {}",
                        e.getCircuitBreakerName(), e.getStateTransition().getFromState(),
                        e.getStateTransition().getToState()))
                    .onError(e -> log.error("CircuitBreaker {} recorded error: {}",
                        e.getCircuitBreakerName(), e.getThrowable().getMessage()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {
                // Handle removal if needed
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> event) {
                // Handle replacement if needed
            }
        };
    }

    /**
     * Register retry event logging
     */
    @Bean
    public RegistryEventConsumer<Retry> retryEventConsumer(RetryRegistry registry) {
        registry.getEventPublisher()
            .onEntryAdded(event -> log.info("Retry registered: {}", event.getAddedEntry().getName()))
            .onEntryRemoved(event -> log.info("Retry removed: {}", event.getRemovedEntry().getName()));
        
        return new RegistryEventConsumer<Retry>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> event) {
                Retry retry = event.getAddedEntry();
                retry.getEventPublisher()
                    .onRetry(e -> log.warn("Retry {} - attempt {} failed: {}",
                        e.getName(), e.getNumberOfRetryAttempts(), e.getLastThrowable().getMessage()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> event) {
                // Handle removal if needed
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> event) {
                // Handle replacement if needed
            }
        };
    }
}
