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
public class ClaimStatusRequest {
    @NotBlank
    private String tenantId;

    @NotBlank
    private String claimId;

    @NotBlank
    private String correlationId;

    @NotBlank
    private String actor;
}
