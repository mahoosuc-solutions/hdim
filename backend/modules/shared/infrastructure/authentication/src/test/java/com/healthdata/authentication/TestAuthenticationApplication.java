package com.healthdata.authentication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application for authentication integration tests.
 * This is a minimal Spring Boot application used only for testing.
 */
@SpringBootApplication
@EntityScan(basePackages = {"com.healthdata.authentication.entity", "com.healthdata.authentication.domain"})
@EnableJpaRepositories(basePackages = "com.healthdata.authentication.repository")
public class TestAuthenticationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestAuthenticationApplication.class, args);
    }
}
