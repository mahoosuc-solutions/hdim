package com.healthdata.payer.revenue.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceTransparencyRatePublishRequest {
    @NotBlank
    private String tenantId;

    @NotBlank
    private String sourceReference;

    @NotBlank
    private String correlationId;

    @NotBlank
    private String actor;

    @Valid
    @NotEmpty
    private List<PriceTransparencyRateEntry> rates;
}
