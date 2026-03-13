package com.healthdata.caregap.api.v1.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SimulatedGapClosureRequest {

    @NotBlank
    private String gapCode;

    @Min(1)
    private int closures;
}
