package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to update room status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Room status update request")
public class RoomStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Schema(description = "New room status", example = "CLEANING", required = true,
            allowableValues = {"AVAILABLE", "OCCUPIED", "CLEANING", "OUT_OF_SERVICE"})
    private String status;

    @Schema(description = "Reason for status change", example = "Room requires deep cleaning")
    private String reason;
}
