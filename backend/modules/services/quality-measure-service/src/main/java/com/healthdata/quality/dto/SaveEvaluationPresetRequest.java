package com.healthdata.quality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class SaveEvaluationPresetRequest {
    @NotBlank(message = "Measure ID is required")
    private String measureId;

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    private Boolean useCqlEngine;
}
