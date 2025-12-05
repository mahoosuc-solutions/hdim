package com.healthdata.quality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for calculating quality measures
 * Includes comprehensive validation constraints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureCalculationRequest {

    @NotBlank(message = "Measure ID is required")
    private String measureId;

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotNull(message = "Period start date is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end date is required")
    private LocalDate periodEnd;

    // Optional parameters for measure calculation
    private Map<String, Object> parameters;

    private String createdBy;
}
