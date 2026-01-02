package com.healthdata.consent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Consent Service - HIPAA-compliant consent management
 *
 * Manages patient consent records, consent policies, and consent history
 * in compliance with HIPAA 42 CFR Part 2 and GDPR requirements.
 */
@SpringBootApplication
public class ConsentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsentServiceApplication.class, args);
    }
}
