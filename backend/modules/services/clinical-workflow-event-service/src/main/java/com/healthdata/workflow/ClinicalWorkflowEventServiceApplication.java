package com.healthdata.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Clinical Workflow Event Service Application
 *
 * Spring Boot microservice for processing clinical workflow events.
 * Wraps Phase 4 ClinicalWorkflowEventHandler library with REST API, persistence, and Kafka integration.
 *
 * Port: 8093 (configurable via application.yml)
 */
@SpringBootApplication
@EnableKafka
@EnableFeignClients
@EnableJpaRepositories(basePackages = {
    "com.healthdata.workflow.persistence",
    "com.healthdata.workflow.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.workflow",
    "com.healthdata.workflow.projection"
})
@ComponentScan(basePackages = {
    "com.healthdata.workflow",
    "com.healthdata.workflow.api",
    "com.healthdata.workflow.service",
    "com.healthdata.workflow.persistence",
    "com.healthdata.workflow.config"
})
public class ClinicalWorkflowEventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicalWorkflowEventServiceApplication.class, args);
    }
}
