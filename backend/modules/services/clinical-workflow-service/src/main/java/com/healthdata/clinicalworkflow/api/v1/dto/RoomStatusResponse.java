package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response containing room status details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Room status details")
public class RoomStatusResponse {

    @Schema(description = "Room assignment ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Room number", example = "Room 3")
    private String roomNumber;

    @Schema(description = "Room status", example = "OCCUPIED",
            allowableValues = {"AVAILABLE", "OCCUPIED", "CLEANING", "OUT_OF_SERVICE"})
    private String status;

    @Schema(description = "Patient FHIR ID if occupied", example = "PATIENT001")
    private String patientId;

    @Schema(description = "Patient full name if occupied", example = "John Doe")
    private String patientName;

    @Schema(description = "Encounter FHIR ID if occupied", example = "ENC001")
    private String encounterId;

    @Schema(description = "Assigned provider ID", example = "PROV001")
    private String providerId;

    @Schema(description = "Assigned provider name", example = "Dr. Jane Smith")
    private String providerName;

    @Schema(description = "Priority level", example = "ROUTINE")
    private String priority;

    @Schema(description = "When patient was assigned to room", example = "2026-01-17T09:40:00")
    private LocalDateTime assignedAt;

    @Schema(description = "Wait time in minutes since assignment", example = "15")
    private Integer waitTimeMinutes;

    @Schema(description = "Visit type", example = "Annual Physical")
    private String visitType;

    @Schema(description = "Room assignment notes", example = "Patient requires wheelchair accessible room")
    private String notes;

    @Schema(description = "Tenant identifier", example = "TENANT001")
    private String tenantId;

    @Schema(description = "Last status update timestamp", example = "2026-01-17T09:45:00")
    private LocalDateTime updatedAt;
}
