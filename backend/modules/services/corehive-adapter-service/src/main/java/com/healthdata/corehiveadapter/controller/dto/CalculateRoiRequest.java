package com.healthdata.corehiveadapter.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
public class CalculateRoiRequest {

    @NotBlank(message = "contractId is required")
    private String contractId;

    private String contractType;

    @Positive(message = "totalLives must be positive")
    private int totalLives;

    private BigDecimal pmpm;

    @NotNull(message = "totalContractValue is required")
    private BigDecimal totalContractValue;

    private double currentQualityScore;
    private double targetQualityScore;
    private int openCareGapCount;
}
