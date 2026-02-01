package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request to record patient vital signs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vital signs recording request")
public class VitalSignsRequest {

    @NotBlank(message = "Patient ID is required")
    @Schema(description = "Patient FHIR ID", example = "PATIENT001", required = true)
    private String patientId;

    @NotBlank(message = "Encounter ID is required")
    @Schema(description = "Encounter FHIR ID", example = "ENC001", required = true)
    private String encounterId;

    @NotNull(message = "Measurement time is required")
    @Schema(description = "When vitals were measured", example = "2026-01-17T09:35:00", required = true)
    private LocalDateTime measuredAt;

    @Positive(message = "Systolic blood pressure must be positive")
    @Schema(description = "Systolic blood pressure (mmHg)", example = "120", minimum = "0", maximum = "300")
    private Integer systolicBP;

    @Positive(message = "Diastolic blood pressure must be positive")
    @Schema(description = "Diastolic blood pressure (mmHg)", example = "80", minimum = "0", maximum = "200")
    private Integer diastolicBP;

    @Positive(message = "Heart rate must be positive")
    @Schema(description = "Heart rate (bpm)", example = "72", minimum = "0", maximum = "300")
    private Integer heartRate;

    @Positive(message = "Respiratory rate must be positive")
    @Schema(description = "Respiratory rate (breaths/min)", example = "16", minimum = "0", maximum = "60")
    private Integer respiratoryRate;

    @Positive(message = "Temperature must be positive")
    @Schema(description = "Body temperature (Fahrenheit)", example = "98.6", minimum = "80", maximum = "115")
    private BigDecimal temperature;

    @Schema(description = "Oxygen saturation (%)", example = "98", minimum = "0", maximum = "100")
    private Integer oxygenSaturation;

    @Schema(description = "Weight (pounds)", example = "175.5", minimum = "0")
    private BigDecimal weight;

    @Schema(description = "Height (inches)", example = "68.0", minimum = "0")
    private BigDecimal height;

    @Schema(description = "Pain level (0-10 scale)", example = "3", minimum = "0", maximum = "10")
    private Integer painLevel;

    @Schema(description = "Additional notes", example = "Patient reports feeling well")
    private String notes;
}
