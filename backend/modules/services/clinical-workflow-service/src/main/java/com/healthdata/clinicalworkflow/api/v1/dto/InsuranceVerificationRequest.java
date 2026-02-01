package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to update insurance verification status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Insurance verification update request")
public class InsuranceVerificationRequest {

    @NotNull(message = "Verification status is required")
    @Schema(description = "Insurance verification status", example = "true", required = true)
    private Boolean verified;

    @Schema(description = "Insurance provider name", example = "Blue Cross Blue Shield")
    private String insuranceProvider;

    @Schema(description = "Member ID", example = "MEM123456789")
    private String memberId;

    @Schema(description = "Group number", example = "GRP987654")
    private String groupNumber;

    @Schema(description = "Verification notes", example = "Coverage confirmed for office visit")
    private String verificationNotes;
}
