package com.healthdata.ingestion.interoperability.dto;

import com.healthdata.ingestion.interoperability.AdtEventState;
import com.healthdata.ingestion.interoperability.InteroperabilityErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdtAcknowledgementResponse {
    private String eventId;
    private String tenantId;
    private String correlationId;
    private AdtEventState state;
    private InteroperabilityErrorCode errorCode;
    private InteroperabilityAuditEnvelope auditEnvelope;
}
