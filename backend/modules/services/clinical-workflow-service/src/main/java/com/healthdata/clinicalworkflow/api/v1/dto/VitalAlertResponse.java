package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response for vital sign alerts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vital sign alert details")
public class VitalAlertResponse {

    @Schema(description = "Alert ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID alertId;

    @Schema(description = "Vital signs record ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID vitalSignsRecordId;

    @Schema(description = "Patient FHIR ID", example = "PATIENT001")
    private String patientId;

    @Schema(description = "Patient full name", example = "John Doe")
    private String patientName;

    @Schema(description = "Room number if assigned", example = "Room 3")
    private String roomNumber;

    @Schema(description = "Alert type", example = "HIGH_BLOOD_PRESSURE",
            allowableValues = {"HIGH_BLOOD_PRESSURE", "LOW_BLOOD_PRESSURE", "HIGH_HEART_RATE", "LOW_HEART_RATE",
                    "HIGH_TEMPERATURE", "LOW_TEMPERATURE", "LOW_OXYGEN_SATURATION"})
    private String alertType;

    @Schema(description = "Severity level", example = "CRITICAL", allowableValues = {"NORMAL", "WARNING", "CRITICAL"})
    private String severity;

    @Schema(description = "Alert message", example = "Systolic BP 180 mmHg exceeds critical threshold")
    private String message;

    @Schema(description = "Measured value", example = "180")
    private String measuredValue;

    @Schema(description = "Normal range", example = "90-140 mmHg")
    private String normalRange;

    @Schema(description = "When alert was triggered", example = "2026-01-17T09:35:00")
    private LocalDateTime alertedAt;

    @Schema(description = "Whether alert has been acknowledged", example = "false")
    private Boolean acknowledged;

    @Schema(description = "When alert was acknowledged", example = "2026-01-17T09:40:00")
    private LocalDateTime acknowledgedAt;

    @Schema(description = "User who acknowledged alert", example = "NURSE001")
    private String acknowledgedBy;
}
