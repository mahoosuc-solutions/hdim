package com.healthdata.payer.revenue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemittanceAdviceEvent {
    @NotBlank
    private String tenantId;

    @NotBlank
    private String claimId;

    @NotBlank
    private String remittanceId;

    @NotNull
    private BigDecimal paymentAmount;

    @NotNull
    private BigDecimal adjustmentAmount;

    @NotBlank
    private String correlationId;

    @NotBlank
    private String actor;
}
