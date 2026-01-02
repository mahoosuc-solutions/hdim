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
