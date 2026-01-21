package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response containing check-in details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Patient check-in response")
public class CheckInResponse {

    @Schema(description = "Check-in record ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Patient FHIR ID", example = "PATIENT001")
    private String patientId;

    @Schema(description = "Patient full name", example = "John Doe")
    private String patientName;

    @Schema(description = "Appointment FHIR ID", example = "APPT001")
    private String appointmentId;

    @Schema(description = "Check-in timestamp", example = "2026-01-17T09:30:00")
    private LocalDateTime checkInTime;

    @Schema(description = "Insurance verification status", example = "true")
    private Boolean insuranceVerified;

    @Schema(description = "Consent signed status", example = "true")
    private Boolean consentSigned;

    @Schema(description = "Demographics confirmed status", example = "true")
    private Boolean demographicsConfirmed;

    @Schema(description = "Check-in completion status", example = "COMPLETE", allowableValues = {"PENDING", "INCOMPLETE", "COMPLETE"})
    private String status;

    @Schema(description = "Additional notes", example = "Patient arrived early")
    private String notes;

    @Schema(description = "Check-in method used", example = "FRONT_DESK")
    private String checkInMethod;

    @Schema(description = "Tenant identifier", example = "TENANT001")
    private String tenantId;

    @Schema(description = "Record creation timestamp", example = "2026-01-17T09:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Record last update timestamp", example = "2026-01-17T09:35:00")
    private LocalDateTime updatedAt;
}
