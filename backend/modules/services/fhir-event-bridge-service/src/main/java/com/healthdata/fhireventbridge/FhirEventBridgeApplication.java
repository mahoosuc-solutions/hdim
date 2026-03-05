package com.healthdata.fhireventbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * FHIR Event Bridge Service
 *
 * Consumes FHIR Patient events from FHIR service and converts them to domain events
 * for downstream event-driven processing.
 */
@SpringBootApplication
@EnableKafka
public class FhirEventBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FhirEventBridgeApplication.class, args);
    }
}
