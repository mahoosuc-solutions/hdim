package com.healthdata.clinicalworkflowevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Clinical Workflow Event Service
 *
 * CQRS Read Model Microservice
 * Consumes domain events from Kafka and maintains denormalized workflow projections
 * for fast query performance.
 *
 * Port: 8113
 * Database: clinical_workflow_event_db
 * Kafka Group: clinical-workflow-event-service
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.healthdata.clinicalworkflowevent",
        "com.healthdata.workflow",
        "com.healthdata.shared",
        "com.healthdata.eventstore.client"
    }
)
@EntityScan(basePackages = {
    "com.healthdata.clinicalworkflowevent.projection",
    "com.healthdata.authentication.domain"
})
@EnableJpaRepositories(
    basePackages = "com.healthdata.clinicalworkflowevent.repository"
)
@EnableFeignClients(basePackages = {"com.healthdata.clinicalworkflowevent", "com.healthdata.eventstore.client"})
@EnableTransactionManagement
@EnableKafka
public class ClinicalWorkflowEventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicalWorkflowEventServiceApplication.class, args);
    }
}
