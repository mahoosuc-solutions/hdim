package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response for API errors
 * HIPAA-compliant: Does not expose PHI in error messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API error response")
public class ErrorResponse {

    @Schema(description = "Error timestamp", example = "2026-01-17T09:45:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private Integer status;

    @Schema(description = "Error type", example = "VALIDATION_ERROR",
            allowableValues = {"VALIDATION_ERROR", "RESOURCE_NOT_FOUND", "UNAUTHORIZED", "FORBIDDEN",
                    "CONFLICT", "INTERNAL_ERROR"})
    private String error;

    @Schema(description = "Error message (HIPAA-safe, no PHI)", example = "Invalid request parameters")
    private String message;

    @Schema(description = "Request path", example = "/api/v1/check-in")
    private String path;

    @Schema(description = "Validation errors if applicable")
    private List<FieldError> fieldErrors;

    /**
     * Field-level validation error
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Field validation error")
    public static class FieldError {

        @Schema(description = "Field name", example = "patientId")
        private String field;

        @Schema(description = "Rejected value (sanitized)", example = "null")
        private String rejectedValue;

        @Schema(description = "Error message", example = "Patient ID is required")
        private String message;
    }
}
