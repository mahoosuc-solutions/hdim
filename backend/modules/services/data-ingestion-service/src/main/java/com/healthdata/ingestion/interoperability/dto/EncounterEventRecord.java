package com.healthdata.ingestion.interoperability.dto;

import com.healthdata.ingestion.interoperability.AdtEventState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterEventRecord {
    private String eventId;
    private String tenantId;
    private String sourceSystem;
    private String sourceMessageId;
    private String eventType;
    private String patientExternalId;
    private String encounterExternalId;
    private String payloadHash;
    private String correlationId;
    private AdtEventState state;
    private InteroperabilityAuditEnvelope auditEnvelope;
}
