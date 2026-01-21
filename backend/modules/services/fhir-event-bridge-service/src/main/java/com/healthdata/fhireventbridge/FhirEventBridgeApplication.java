package com.healthdata.fhireventbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * FHIR Event Bridge Service
 *
 * Consumes FHIR Patient events from FHIR service and converts them to domain events
 * for downstream event-driven processing.
 *
 * ★ Insight ─────────────────────────────────────
 * - Bridge pattern: FHIR events → Domain events
 * - Enables FHIR service isolation from event models
 * - Supports patient merge chains via Patient.link processing
 * - Cascades identifier changes and FHIR sync events
 * - Maintains HIPAA compliance with PHI sensitivity tracking
 * ─────────────────────────────────────────────────
 */
@SpringBootApplication
@EnableKafka
@EnableFeignClients
public class FhirEventBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FhirEventBridgeApplication.class, args);
    }
}
