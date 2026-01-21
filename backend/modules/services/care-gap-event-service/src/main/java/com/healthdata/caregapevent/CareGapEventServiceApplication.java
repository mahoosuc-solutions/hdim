package com.healthdata.caregapevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Care Gap Event Service
 *
 * CQRS Read Model Microservice
 * Consumes domain events from Kafka and maintains denormalized care gap projections
 * for fast query performance.
 *
 * Port: 8111
 * Database: care_gap_event_db
 * Kafka Group: care-gap-event-service
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.healthdata.caregapevent",
        "com.healthdata.shared"
    }
)
@EntityScan(basePackages = {
    "com.healthdata.caregapevent.projection",
    "com.healthdata.authentication.domain"
})
@EnableJpaRepositories(
    basePackages = "com.healthdata.caregapevent.repository"
)
@EnableTransactionManagement
@EnableKafka
public class CareGapEventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareGapEventServiceApplication.class, args);
    }
}
