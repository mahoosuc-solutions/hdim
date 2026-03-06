package com.healthdata.eventsourcing.intelligence;

import com.healthdata.eventsourcing.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical event envelope shared across ingestion, validation, and intelligence paths.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalEventEnvelope {

    private String eventId;
    private String tenantId;
    private String patientRef;
    private String sourceType;
    private String resourceType;
    private String schemaVersion;
    private String traceId;
    private Instant occurredAt;
    private Map<String, Object> payload;
    private Map<String, Object> provenance;
    private Double confidence;
    private String riskTier;

    /**
     * Build a canonical envelope from an existing domain event.
     */
    public static CanonicalEventEnvelope fromDomainEvent(DomainEvent event, String sourceType) {
        return CanonicalEventEnvelope.builder()
                .eventId(event.getEventId() != null ? event.getEventId() : UUID.randomUUID().toString())
                .tenantId(event.getTenantId())
                .patientRef(event.getAggregateId())
                .sourceType(sourceType)
                .resourceType(event.getResourceType())
                .schemaVersion("1.0")
                .traceId(event.getEventId())
                .occurredAt(event.getTimestamp() != null ? event.getTimestamp() : Instant.now())
                .payload(Collections.emptyMap())
                .provenance(Collections.emptyMap())
                .confidence(1.0)
                .riskTier("MEDIUM")
                .build();
    }
}
