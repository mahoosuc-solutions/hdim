package com.healthdata.eventstore.client.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the EventStoreClient Feign client.
 * Note: @EnableFeignClients is declared in the application class that uses this client.
 */
@Configuration
public class EventStoreClientConfig {

    /**
     * Feign logger level for debugging.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Request interceptor for adding common headers.
     * Tenant ID is passed explicitly in method calls, so no interceptor needed.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // Add any common headers if needed
            template.header("Content-Type", "application/json");
        };
    }

    /**
     * Custom error decoder for handling event store errors.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new EventStoreErrorDecoder();
    }
}
