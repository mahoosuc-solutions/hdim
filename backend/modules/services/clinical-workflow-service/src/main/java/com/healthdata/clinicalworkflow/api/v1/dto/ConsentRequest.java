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
 * Request to record patient consent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Patient consent recording request")
public class ConsentRequest {

    @NotNull(message = "Consent status is required")
    @Schema(description = "Whether consent was obtained", example = "true", required = true)
    private Boolean consentObtained;

    @NotBlank(message = "Consent type is required")
    @Schema(description = "Type of consent obtained", example = "TREATMENT", required = true,
            allowableValues = {"TREATMENT", "HIPAA", "RESEARCH", "BILLING"})
    private String consentType;

    @Schema(description = "Consent signature captured", example = "true")
    private Boolean signatureCaptured;

    @Schema(description = "Timestamp when consent was signed", example = "2026-01-17T09:32:00")
    private LocalDateTime consentSignedAt;

    @Schema(description = "Additional consent notes", example = "Patient signed electronically on tablet")
    private String notes;
}
