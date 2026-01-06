package com.healthdata.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for bulk signing quality measure results.
 * Allows providers to sign multiple normal results at once.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to bulk sign quality measure results")
public class BulkSignRequest {

    @NotEmpty(message = "At least one result ID is required")
    @Schema(description = "List of result IDs to sign", required = true)
    private List<UUID> resultIds;

    @NotNull(message = "Signature type is required")
    @Schema(description = "Type of electronic signature", example = "ELECTRONIC")
    private SignatureType signatureType;

    @Valid
    @Schema(description = "Acknowledgments for abnormal results requiring individual review")
    private List<AbnormalAcknowledgment> acknowledgments;

    @Schema(description = "Optional notes for the signing action")
    private String notes;

    /**
     * Type of electronic signature
     */
    public enum SignatureType {
        ELECTRONIC,
        DIGITAL_CERTIFICATE,
        BIOMETRIC
    }

    /**
     * Acknowledgment for abnormal results
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbnormalAcknowledgment {
        @NotNull(message = "Result ID is required for acknowledgment")
        private UUID resultId;

        @NotNull(message = "Acknowledgment status is required")
        private boolean acknowledged;

        @Schema(description = "Notes explaining the acknowledgment")
        private String notes;
    }
}
