package com.healthdata.qualityevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Quality Measure Event Service
 *
 * CQRS Read Model Microservice
 * Consumes domain events from Kafka and maintains denormalized measure evaluation projections
 * for fast query performance.
 *
 * Port: 8112
 * Database: quality_measure_event_db
 * Kafka Group: quality-measure-event-service
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.healthdata.qualityevent",
        "com.healthdata.shared"
    }
)
@EntityScan(basePackages = {
    "com.healthdata.qualityevent.projection",
    "com.healthdata.authentication.domain"
})
@EnableJpaRepositories(
    basePackages = "com.healthdata.qualityevent.repository"
)
@EnableTransactionManagement
@EnableKafka
public class QualityMeasureEventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QualityMeasureEventServiceApplication.class, args);
    }
}
