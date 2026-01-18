package com.healthdata.patientevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Patient Event Service Application
 *
 * Spring Boot microservice for processing patient lifecycle events.
 * Wraps Phase 4 PatientEventHandler library with REST API, persistence, and Kafka integration.
 *
 * Port: 8090 (configurable via application.yml)
 */
@SpringBootApplication
@EnableKafka
@EnableFeignClients
@EnableJpaRepositories(basePackages = "com.healthdata.patientevent.persistence")
@ComponentScan(basePackages = {
    "com.healthdata.patientevent",
    "com.healthdata.patientevent.api",
    "com.healthdata.patientevent.service",
    "com.healthdata.patientevent.persistence",
    "com.healthdata.patientevent.config"
})
public class PatientEventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientEventServiceApplication.class, args);
    }
}
