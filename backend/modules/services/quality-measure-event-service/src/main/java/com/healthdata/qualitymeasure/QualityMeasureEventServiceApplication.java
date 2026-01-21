package com.healthdata.qualitymeasure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Quality Measure Event Service Application
 *
 * Spring Boot microservice for processing quality measure evaluation events.
 * Wraps Phase 4 QualityMeasureEventHandler library with REST API, persistence, and Kafka integration.
 *
 * Port: 8091 (configurable via application.yml)
 */
@SpringBootApplication
@EnableKafka
@EnableFeignClients
@EnableJpaRepositories(basePackages = {
    "com.healthdata.qualitymeasure.persistence",
    "com.healthdata.qualitymeasure.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.qualitymeasure",
    "com.healthdata.qualitymeasure.projection"
})
@ComponentScan(basePackages = {
    "com.healthdata.qualitymeasure",
    "com.healthdata.qualitymeasure.api",
    "com.healthdata.qualitymeasure.service",
    "com.healthdata.qualitymeasure.persistence",
    "com.healthdata.qualitymeasure.config"
})
public class QualityMeasureEventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QualityMeasureEventServiceApplication.class, args);
    }
}
