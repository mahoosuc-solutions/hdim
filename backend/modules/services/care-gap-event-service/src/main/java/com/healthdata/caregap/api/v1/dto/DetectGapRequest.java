package com.healthdata.caregap.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectGapRequest {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Gap code is required")
    private String gapCode;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Severity is required")
    @Pattern(regexp = "CRITICAL|HIGH|MEDIUM|LOW", message = "Severity must be CRITICAL, HIGH, MEDIUM, or LOW")
    private String severity;  // CRITICAL, HIGH, MEDIUM, LOW
}
