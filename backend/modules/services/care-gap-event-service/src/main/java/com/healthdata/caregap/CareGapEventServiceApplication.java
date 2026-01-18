package com.healthdata.caregap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Care Gap Event Service Application
 *
 * Spring Boot microservice for processing care gap detection and closure events.
 * Wraps Phase 4 CareGapEventHandler library with REST API, persistence, and Kafka integration.
 *
 * Port: 8092 (configurable via application.yml)
 */
@SpringBootApplication
@EnableKafka
@EnableFeignClients
@EnableJpaRepositories(basePackages = "com.healthdata.caregap.persistence")
@ComponentScan(basePackages = {
    "com.healthdata.caregap",
    "com.healthdata.caregap.api",
    "com.healthdata.caregap.service",
    "com.healthdata.caregap.persistence",
    "com.healthdata.caregap.config"
})
public class CareGapEventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareGapEventServiceApplication.class, args);
    }
}
