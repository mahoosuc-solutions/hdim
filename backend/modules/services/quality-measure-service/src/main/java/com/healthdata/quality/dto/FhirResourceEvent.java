package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * FHIR Resource Event DTO
 * Represents events published when FHIR resources are created/updated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FhirResourceEvent {

    private String eventId;
    private String eventType; // "fhir.procedures.created", "fhir.observations.created"
    private String resourceType; // "Procedure", "Observation"
    private String resourceId;
    private String tenantId;
    private UUID patientId;
    private Instant timestamp;

    // Resource-specific data
    private List<CodeableConcept> codes;
    private CodeableConcept category;
    private String status;
    private Instant performedDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeableConcept {
        private List<Coding> coding;
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coding {
        private String system;
        private String code;
        private String display;
    }
}
