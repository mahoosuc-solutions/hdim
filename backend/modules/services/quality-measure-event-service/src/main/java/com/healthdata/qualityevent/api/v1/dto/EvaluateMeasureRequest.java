package com.healthdata.qualityevent.api.v1.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateMeasureRequest {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Measure code is required")
    private String measureCode;

    @Min(value = 0, message = "Score must be >= 0.0")
    @Max(value = 1, message = "Score must be <= 1.0")
    private float score;
}
