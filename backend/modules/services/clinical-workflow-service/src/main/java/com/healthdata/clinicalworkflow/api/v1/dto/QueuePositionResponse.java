package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response containing patient's queue position
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Patient queue position details")
public class QueuePositionResponse {

    @Schema(description = "Queue entry ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Patient FHIR ID", example = "PATIENT001")
    private String patientId;

    @Schema(description = "Patient full name", example = "John Doe")
    private String patientName;

    @Schema(description = "Encounter FHIR ID", example = "ENC001")
    private String encounterId;

    @Schema(description = "Queue type", example = "PROVIDER")
    private String queueType;

    @Schema(description = "Current status", example = "WAITING",
            allowableValues = {"WAITING", "CALLED", "IN_PROGRESS", "COMPLETED"})
    private String status;

    @Schema(description = "Priority level", example = "ROUTINE")
    private String priority;

    @Schema(description = "Position in queue (1-based)", example = "3")
    private Integer position;

    @Schema(description = "Total patients ahead in queue", example = "2")
    private Integer patientsAhead;

    @Schema(description = "Estimated wait time in minutes", example = "15")
    private Integer estimatedWaitMinutes;

    @Schema(description = "Actual wait time in minutes", example = "8")
    private Integer actualWaitMinutes;

    @Schema(description = "When patient entered queue", example = "2026-01-17T09:30:00")
    private LocalDateTime enteredQueueAt;

    @Schema(description = "When patient was called", example = "2026-01-17T09:38:00")
    private LocalDateTime calledAt;

    @Schema(description = "Visit type", example = "Annual Physical")
    private String visitType;

    @Schema(description = "Assigned provider ID", example = "PROV001")
    private String providerId;

    @Schema(description = "Assigned provider name", example = "Dr. Jane Smith")
    private String providerName;

    @Schema(description = "Queue entry notes", example = "Patient is wheelchair bound")
    private String notes;

    @Schema(description = "Tenant identifier", example = "TENANT001")
    private String tenantId;
}
