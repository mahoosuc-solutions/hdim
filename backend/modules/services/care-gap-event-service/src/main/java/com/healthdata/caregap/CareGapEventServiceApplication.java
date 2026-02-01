package com.healthdata.caregap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Care Gap Event Service Application
 *
 * Spring Boot microservice for processing care gap detection and closure events.
 * Wraps Phase 4 CareGapEventHandler library with REST API, persistence, and Kafka integration.
 *
 * Port: 8111 (configured in application.yml)
 *
 * Note: JPA entity scanning is configured in JpaConfig to prevent conflicts between
 * handler library and event service projection entities.
 */
@SpringBootApplication
@EnableKafka
@EnableFeignClients(basePackages = {"com.healthdata.caregap", "com.healthdata.eventstore.client"})
@Import(com.healthdata.caregap.config.CacheConfig.class)
@ComponentScan(basePackages = {
    "com.healthdata.caregap",            // Event service API + application logic
    "com.healthdata.caregap.config",     // JPA configuration
    "com.healthdata.eventstore.client",
    "com.healthdata.shared"
})
public class CareGapEventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareGapEventServiceApplication.class, args);
    }
}
