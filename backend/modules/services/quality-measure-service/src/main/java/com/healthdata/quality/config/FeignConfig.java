package com.healthdata.quality.config;

import feign.RequestInterceptor;
import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign Client Configuration
 *
 * Configures authentication and other Feign client behaviors for
 * inter-service communication.
 */
@Configuration
public class FeignConfig {

    @Value("${cql.engine.auth.username:user}")
    private String cqlEngineUsername;

    @Value("${cql.engine.auth.password:password}")
    private String cqlEnginePassword;

    /**
     * Request interceptor for CQL Engine service authentication
     *
     * Adds HTTP Basic Authentication headers to all Feign requests
     * targeting the CQL Engine service.
     *
     * @return BasicAuthRequestInterceptor with configured credentials
     */
    @Bean
    public RequestInterceptor basicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor(cqlEngineUsername, cqlEnginePassword);
    }
}
