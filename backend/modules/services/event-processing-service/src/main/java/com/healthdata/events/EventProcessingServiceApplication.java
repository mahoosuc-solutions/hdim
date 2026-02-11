package com.healthdata.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Event Processing Service - Event sourcing and subscription management
 *
 * Handles event sourcing, event subscriptions, and dead letter queue
 * for failed event processing.
 *
 * Features:
 * - Dead Letter Queue with automatic retry (exponential backoff)
 * - Event monitoring and metrics
 * - Event routing and subscription management
 *
 * ARCHITECTURE:
 * This service uses the authentication-headers module which provides:
 * - TrustedHeaderAuthFilter: Extracts user context from gateway-injected headers
 * - TrustedTenantAccessFilter: Validates tenant access without database lookup
 * - UserContextHolder: Thread-local storage for audit context
 *
 * The authentication-headers module has NO @EntityScan, so it doesn't try to
 * validate the User entity against this service's database. This allows
 * event-processing-service to use its own database schema (healthdata_events)
 * without requiring a users table.
 *
 * Security Flow:
 * 1. Gateway validates JWT and injects X-Auth-* headers
 * 2. TrustedHeaderAuthFilter extracts user context and sets SecurityContext
 * 3. TrustedTenantAccessFilter validates X-Tenant-ID header
 * 4. Controller receives authenticated request
 */
@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = "com.healthdata.events.entity")
@EnableJpaRepositories(basePackages = "com.healthdata.events.repository")
public class EventProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventProcessingServiceApplication.class, args);
    }
}
