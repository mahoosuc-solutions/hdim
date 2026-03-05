package com.healthdata.fhireventbridge.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a FHIR Patient event received from fhir-service via Kafka.
 * Contains the FHIR resource ID and metadata needed to convert
 * to domain events for downstream processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FhirPatientEvent {

    private String fhirResourceId;
    private String tenantId;
    private String eventType;
    private String linkedFhirResourceId;
    private String linkType;
    private Instant timestamp;
}
