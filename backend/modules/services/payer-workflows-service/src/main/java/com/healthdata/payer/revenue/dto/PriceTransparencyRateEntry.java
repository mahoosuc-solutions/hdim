package com.healthdata.payer.revenue.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class PriceTransparencyRateEntry {
    @NotBlank
    private String serviceCode;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    private BigDecimal negotiatedRate;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    private BigDecimal cashPrice;
}
