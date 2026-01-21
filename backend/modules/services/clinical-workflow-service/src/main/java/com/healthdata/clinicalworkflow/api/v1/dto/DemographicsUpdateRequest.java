package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to update patient demographics confirmation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Demographics confirmation update request")
public class DemographicsUpdateRequest {

    @NotNull(message = "Confirmation status is required")
    @Schema(description = "Whether demographics were confirmed", example = "true", required = true)
    private Boolean confirmed;

    @Schema(description = "Address changed", example = "false")
    private Boolean addressChanged;

    @Schema(description = "Phone number changed", example = "true")
    private Boolean phoneChanged;

    @Schema(description = "Emergency contact changed", example = "false")
    private Boolean emergencyContactChanged;

    @Schema(description = "Update notes", example = "Updated cell phone number")
    private String updateNotes;
}
