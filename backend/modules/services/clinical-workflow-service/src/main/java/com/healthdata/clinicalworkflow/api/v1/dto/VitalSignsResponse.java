package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response containing vital signs record
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vital signs record response")
public class VitalSignsResponse {

    @Schema(description = "Vital signs record ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Patient FHIR ID", example = "PATIENT001")
    private String patientId;

    @Schema(description = "Patient full name", example = "John Doe")
    private String patientName;

    @Schema(description = "Encounter FHIR ID", example = "ENC001")
    private String encounterId;

    @Schema(description = "Measurement timestamp", example = "2026-01-17T09:35:00")
    private LocalDateTime measuredAt;

    @Schema(description = "Systolic blood pressure (mmHg)", example = "120")
    private Integer systolicBP;

    @Schema(description = "Diastolic blood pressure (mmHg)", example = "80")
    private Integer diastolicBP;

    @Schema(description = "Heart rate (bpm)", example = "72")
    private Integer heartRate;

    @Schema(description = "Respiratory rate (breaths/min)", example = "16")
    private Integer respiratoryRate;

    @Schema(description = "Body temperature (Fahrenheit)", example = "98.6")
    private BigDecimal temperature;

    @Schema(description = "Oxygen saturation (%)", example = "98")
    private Integer oxygenSaturation;

    @Schema(description = "Weight (pounds)", example = "175.5")
    private BigDecimal weight;

    @Schema(description = "Height (inches)", example = "68.0")
    private BigDecimal height;

    @Schema(description = "Body Mass Index", example = "26.6")
    private BigDecimal bmi;

    @Schema(description = "Pain level (0-10)", example = "3")
    private Integer painLevel;

    @Schema(description = "Alert flags for abnormal values")
    private List<String> alerts;

    @Schema(description = "Whether any vitals are critical", example = "false")
    private Boolean hasCriticalAlerts;

    @Schema(description = "Additional notes", example = "Patient reports feeling well")
    private String notes;

    @Schema(description = "Tenant identifier", example = "TENANT001")
    private String tenantId;

    @Schema(description = "Record creation timestamp", example = "2026-01-17T09:35:00")
    private LocalDateTime createdAt;

    @Schema(description = "Recorded by user ID", example = "NURSE001")
    private String recordedBy;
}
