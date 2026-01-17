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
 * Request to check in a patient for an appointment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Patient check-in request")
public class CheckInRequest {

    @NotBlank(message = "Patient ID is required")
    @Schema(description = "Patient FHIR ID", example = "PATIENT001", required = true)
    private String patientId;

    @NotBlank(message = "Appointment ID is required")
    @Schema(description = "Appointment FHIR ID", example = "APPT001", required = true)
    private String appointmentId;

    @NotNull(message = "Check-in time is required")
    @Schema(description = "Check-in timestamp", example = "2026-01-17T09:30:00", required = true)
    private LocalDateTime checkInTime;

    @Schema(description = "Insurance verified at check-in", example = "true")
    private Boolean insuranceVerified;

    @Schema(description = "Consent forms signed", example = "true")
    private Boolean consentSigned;

    @Schema(description = "Demographics reviewed and confirmed", example = "true")
    private Boolean demographicsConfirmed;

    @Schema(description = "Additional notes from front desk", example = "Patient arrived early")
    private String notes;

    @Schema(description = "Check-in method", example = "FRONT_DESK", allowableValues = {"FRONT_DESK", "KIOSK", "MOBILE_APP"})
    private String checkInMethod;
}
