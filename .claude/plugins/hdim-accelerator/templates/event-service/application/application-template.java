package com.healthdata.{{DOMAIN}}event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * {{DOMAIN_PASCAL}}EventService - CQRS Read Model Service
 *
 * Purpose: Consumes domain events from Kafka and builds optimized read model projections
 * Architecture: Event-driven CQRS pattern with eventual consistency
 *
 * Port: {{PORT}}
 * Context Path: /{{DOMAIN}}-event
 * Database: {{DATABASE}}
 * Kafka Topics: {{KAFKA_TOPICS}}
 *
 * Event Sources:
 * - {{DOMAIN}}-service (command-side)
 *
 * Read Model:
 * - {{DOMAIN_PASCAL}}Projection (denormalized for fast queries)
 *
 * SLA:
 * - Eventual Consistency: < 500ms from event publication to query visibility
 * - Query Response Time: < 100ms (99th percentile)
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.{{DOMAIN}}event",
    "com.healthdata.shared"  // Scan shared modules
})
@EntityScan(basePackages = {
    "com.healthdata.{{DOMAIN}}event.projection",
    "com.healthdata.authentication.domain"  // Shared authentication tables
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.{{DOMAIN}}event.repository"
})
@EnableTransactionManagement
@EnableKafka  // Enable Kafka listener support
@EnableCaching
public class {{DOMAIN_PASCAL}}EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run({{DOMAIN_PASCAL}}EventServiceApplication.class, args);
    }
}
