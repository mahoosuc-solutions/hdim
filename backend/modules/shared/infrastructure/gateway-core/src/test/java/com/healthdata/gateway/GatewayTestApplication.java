package com.healthdata.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Test application for Gateway Core integration tests.
 *
 * Provides minimal Spring Boot context for testing gateway components
 * including rate limiting, filters, and authentication.
 */
@SpringBootApplication
@TestConfiguration
public class GatewayTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayTestApplication.class, args);
    }
}
