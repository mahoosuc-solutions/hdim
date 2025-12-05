package com.healthdata.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot Application for testing HIPAA compliance features.
 * This is only used in tests to provide a Spring Boot context.
 */
@SpringBootApplication(scanBasePackages = "com.healthdata.security")
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
