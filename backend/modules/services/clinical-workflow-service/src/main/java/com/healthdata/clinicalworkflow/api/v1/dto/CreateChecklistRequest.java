package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create new pre-visit checklist
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create pre-visit checklist request")
public class CreateChecklistRequest {

    @NotBlank(message = "Patient ID is required")
    @Schema(description = "Patient FHIR ID", example = "PATIENT001", required = true)
    private String patientId;

    @NotBlank(message = "Encounter ID is required")
    @Schema(description = "Encounter FHIR ID", example = "ENC001", required = true)
    private String encounterId;

    @NotBlank(message = "Appointment type is required")
    @Schema(description = "Appointment type for template selection", example = "ANNUAL_PHYSICAL", required = true,
            allowableValues = {"ANNUAL_PHYSICAL", "SICK_VISIT", "FOLLOW_UP", "PROCEDURE", "CONSULTATION"})
    private String appointmentType;

    @Schema(description = "Use custom template if true", example = "false")
    private Boolean useCustomTemplate;

    @Schema(description = "Custom template ID if using custom template")
    private String customTemplateId;
}
