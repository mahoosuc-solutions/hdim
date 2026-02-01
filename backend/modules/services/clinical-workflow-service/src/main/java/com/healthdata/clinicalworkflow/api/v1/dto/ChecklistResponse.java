package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response containing pre-visit checklist
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pre-visit checklist with items")
public class ChecklistResponse {

    @Schema(description = "Checklist ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Patient FHIR ID", example = "PATIENT001")
    private String patientId;

    @Schema(description = "Patient full name", example = "John Doe")
    private String patientName;

    @Schema(description = "Encounter FHIR ID", example = "ENC001")
    private String encounterId;

    @Schema(description = "Appointment type", example = "ANNUAL_PHYSICAL")
    private String appointmentType;

    @Schema(description = "Overall completion status", example = "IN_PROGRESS",
            allowableValues = {"NOT_STARTED", "IN_PROGRESS", "COMPLETED"})
    private String status;

    @Schema(description = "List of checklist items")
    private List<ChecklistItemResponse> items;

    @Schema(description = "Total number of items", example = "8")
    private Integer totalItems;

    @Schema(description = "Number of completed items", example = "5")
    private Integer completedItems;

    @Schema(description = "Completion percentage", example = "62.5")
    private Double completionPercentage;

    @Schema(description = "When checklist was created", example = "2026-01-17T08:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "When checklist was last updated", example = "2026-01-17T09:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Tenant identifier", example = "TENANT001")
    private String tenantId;
}
