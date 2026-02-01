package com.healthdata.eventstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Event Store Service - Immutable event log for event sourcing
 *
 * Purpose: Central event persistence service for HDIM event-driven architecture
 *
 * Responsibilities:
 * - Append-only event log (NEVER update/delete events)
 * - Event retrieval for aggregate reconstruction
 * - Snapshot management for performance optimization
 * - Consumer processing status tracking
 *
 * Port: 8090 (configured in application.yml)
 * Database: event_store_db (PostgreSQL)
 *
 * Key Tables:
 * - event_store: Primary event log (immutable)
 * - event_snapshots: Performance optimization
 * - event_processing_status: Consumer tracking
 */
@SpringBootApplication(scanBasePackages = {
        "com.healthdata.eventstore",
        "com.healthdata.common",
        "com.healthdata.persistence"
        // Exclude security/authentication - event-store-service uses gateway trust authentication only
})
@EntityScan(basePackages = {
        "com.healthdata.eventstore.domain"
        // Exclude security/authentication entities - event-store-service uses gateway trust authentication
})
@EnableJpaRepositories(basePackages = {
        "com.healthdata.eventstore.repository"
})
public class EventStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventStoreApplication.class, args);
    }
}
