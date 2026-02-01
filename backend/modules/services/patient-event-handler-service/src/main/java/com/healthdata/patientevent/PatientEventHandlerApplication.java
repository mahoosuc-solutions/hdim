package com.healthdata.patientevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Patient Event Handler Service Application
 *
 * Processes domain events related to patient lifecycle management:
 * - PatientCreatedEvent: Initial patient record creation
 * - PatientMergedEvent: Patient consolidation and duplicate handling
 * - PatientIdentifierChangedEvent: Changes to patient identifiers (MRN, SSN, etc.)
 * - PatientLinkedEvent: FHIR-based patient linkage and relationships
 *
 * Maintains denormalized projections for fast queries and cascades
 * updates to dependent services (care gaps, quality measures, workflows).
 *
 * Multi-tenant architecture: Ensures complete tenant isolation across all operations.
 */
@SpringBootApplication
@EnableKafka
@EnableScheduling
public class PatientEventHandlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientEventHandlerApplication.class, args);
    }
}
