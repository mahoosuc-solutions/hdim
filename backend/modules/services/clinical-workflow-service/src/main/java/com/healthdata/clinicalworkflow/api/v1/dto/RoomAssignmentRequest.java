package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to assign patient to a room
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Room assignment request")
public class RoomAssignmentRequest {

    @NotBlank(message = "Patient ID is required")
    @Schema(description = "Patient FHIR ID", example = "PATIENT001", required = true)
    private String patientId;

    @NotBlank(message = "Encounter ID is required")
    @Schema(description = "Encounter FHIR ID", example = "ENC001", required = true)
    private String encounterId;

    @Schema(description = "Priority level", example = "ROUTINE",
            allowableValues = {"STAT", "URGENT", "ROUTINE"})
    private String priority;

    @Schema(description = "Assigned provider ID", example = "PROV001")
    private String providerId;

    @Schema(description = "Assignment notes", example = "Patient requires wheelchair accessible room")
    private String notes;
}
