package com.healthdata.patient.client;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Configuration for HCC Service Feign Client
 *
 * Configures logging and headers for communication with the HCC Service.
 *
 * IMPORTANT: This class is NOT annotated with @Configuration because it's
 * used via @FeignClient(configuration = ...). The beans defined here are
 * created specifically for the FeignClient that references this class.
 *
 * Auth header forwarding is handled by AuthHeaderForwardingInterceptor from the
 * authentication module, which uses InheritableThreadLocal to propagate headers
 * across threads.
 *
 * @see com.healthdata.authentication.feign.AuthHeaderForwardingInterceptor
 */
public class HccServiceClientConfiguration {

    /**
     * Configure Feign logging level to FULL for debugging
     */
    @Bean
    Logger.Level hccServiceLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Add Accept and Content-Type headers for JSON content type.
     *
     * Note: Auth headers (X-Auth-*, X-Tenant-ID) are forwarded by the global
     * AuthHeaderForwardingInterceptor from the authentication module.
     */
    @Bean
    public RequestInterceptor hccContentTypeInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("Content-Type", "application/json");
        };
    }
}
