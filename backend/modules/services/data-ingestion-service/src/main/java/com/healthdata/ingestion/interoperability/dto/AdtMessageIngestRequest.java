package com.healthdata.ingestion.interoperability.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdtMessageIngestRequest {
    @NotBlank
    private String tenantId;

    @NotBlank
    private String sourceSystem;

    @NotBlank
    private String sourceMessageId;

    @NotBlank
    private String eventType;

    @NotBlank
    private String patientExternalId;

    @NotBlank
    private String encounterExternalId;

    @NotBlank
    private String payloadHash;

    @NotBlank
    private String correlationId;
}
