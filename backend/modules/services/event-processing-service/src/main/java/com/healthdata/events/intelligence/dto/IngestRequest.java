package com.healthdata.events.intelligence.dto;

import com.healthdata.eventsourcing.intelligence.CanonicalEventEnvelope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request model for intelligence ingestion endpoint.
 */
public record IngestRequest(
        @NotBlank String eventId,
        @NotBlank String patientRef,
        @NotBlank String sourceType,
        @NotBlank String resourceType,
        @NotBlank String schemaVersion,
        String traceId,
        @NotNull Map<String, Object> payload,
        Map<String, Object> provenance,
        Double confidence,
        String riskTier
) {
    public CanonicalEventEnvelope toEnvelope(String tenantId) {
        return CanonicalEventEnvelope.builder()
                .eventId(eventId)
                .tenantId(tenantId)
                .patientRef(patientRef)
                .sourceType(sourceType)
                .resourceType(resourceType)
                .schemaVersion(schemaVersion)
                .traceId(traceId)
                .payload(payload)
                .provenance(provenance)
                .confidence(confidence != null ? confidence : 0.7)
                .riskTier(riskTier != null ? riskTier : "MEDIUM")
                .build();
    }
}
