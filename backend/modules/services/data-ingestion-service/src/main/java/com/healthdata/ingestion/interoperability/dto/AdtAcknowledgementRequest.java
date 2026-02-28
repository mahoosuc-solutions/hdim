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
public class AdtAcknowledgementRequest {
    @NotBlank
    private String tenantId;

    @NotBlank
    private String eventId;

    @NotBlank
    private String sourceSystem;

    @NotBlank
    private String correlationId;
}
