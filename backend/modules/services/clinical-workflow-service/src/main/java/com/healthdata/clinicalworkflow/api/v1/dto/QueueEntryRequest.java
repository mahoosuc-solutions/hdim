package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request to add patient to waiting queue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add patient to queue request")
public class QueueEntryRequest {

    @NotBlank(message = "Patient ID is required")
    @Schema(description = "Patient FHIR ID", example = "PATIENT001", required = true)
    private String patientId;

    @NotBlank(message = "Encounter ID is required")
    @Schema(description = "Encounter FHIR ID", example = "ENC001", required = true)
    private String encounterId;

    @NotBlank(message = "Queue type is required")
    @Schema(description = "Type of queue", example = "CHECK_IN", required = true,
            allowableValues = {"CHECK_IN", "VITALS", "PROVIDER", "CHECKOUT"})
    private String queueType;

    @NotNull(message = "Entry time is required")
    @Schema(description = "When patient entered queue", example = "2026-01-17T09:30:00", required = true)
    private LocalDateTime enteredQueueAt;

    @NotBlank(message = "Priority is required")
    @Schema(description = "Priority level", example = "ROUTINE", required = true,
            allowableValues = {"STAT", "URGENT", "ROUTINE"})
    private String priority;

    @Schema(description = "Visit type", example = "Annual Physical")
    private String visitType;

    @Schema(description = "Assigned provider ID", example = "PROV001")
    private String providerId;

    @Schema(description = "Queue entry notes", example = "Patient is wheelchair bound")
    private String notes;
}
