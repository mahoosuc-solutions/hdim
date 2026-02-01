package com.healthdata.patientevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Patient Event Service
 *
 * CQRS Read Model Microservice
 * Consumes domain events from Kafka and maintains denormalized patient projections
 * for fast query performance.
 *
 * Port: 8110
 * Database: patient_event_db
 * Kafka Group: patient-event-service
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.healthdata.patientevent",
        "com.healthdata.shared"
    }
)
@EntityScan(basePackages = {
    "com.healthdata.patientevent.projection",
    "com.healthdata.authentication.domain"
})
@EnableJpaRepositories(
    basePackages = "com.healthdata.patientevent.repository"
)
@EnableTransactionManagement
@EnableKafka
public class PatientEventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientEventServiceApplication.class, args);
    }
}
