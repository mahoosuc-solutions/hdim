package com.healthdata.payer.revenue.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCheckRequest {
    @NotBlank
    private String tenantId;

    @NotBlank
    private String payerId;

    @NotBlank
    private String patientId;

    @NotBlank
    private String correlationId;

    @NotBlank
    private String actor;
}
