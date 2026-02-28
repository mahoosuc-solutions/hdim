package com.healthdata.ingestion.interoperability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteroperabilityAuditEnvelope {
    private String tenantId;
    private String correlationId;
    private String sourceSystem;
    private String eventType;
    private Instant timestamp;
    private String outcome;
}
