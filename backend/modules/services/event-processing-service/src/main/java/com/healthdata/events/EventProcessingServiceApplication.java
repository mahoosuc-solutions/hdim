package com.healthdata.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class EventProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventProcessingServiceApplication.class, args);
    }
}
