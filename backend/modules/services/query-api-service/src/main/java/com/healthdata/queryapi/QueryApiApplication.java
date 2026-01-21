package com.healthdata.queryapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Query API Service - REST endpoints for CQRS Read Model
 *
 * Exposes Phase 1.7 Query Services via REST API:
 * - PatientQueryService → PatientController
 * - ObservationQueryService → ObservationController
 * - ConditionQueryService → ConditionController
 * - CarePlanQueryService → CarePlanController
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.healthdata.queryapi",
    "com.healthdata.eventsourcing"  // Include shared event-sourcing components
})
public class QueryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryApiApplication.class, args);
    }
}
