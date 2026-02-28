package com.healthdata.payer.revenue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceEstimateRequest {
    @NotBlank
    private String tenantId;

    @NotBlank
    private String serviceCode;

    @Min(1)
    private int units;

    private String versionId;

    @NotBlank
    private String correlationId;

    @NotBlank
    private String actor;
}
