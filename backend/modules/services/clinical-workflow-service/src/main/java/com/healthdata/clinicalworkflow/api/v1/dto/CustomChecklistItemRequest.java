package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to add custom checklist item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add custom checklist item request")
public class CustomChecklistItemRequest {

    @NotBlank(message = "Display name is required")
    @Schema(description = "Item display name", example = "Wheelchair Assistance", required = true)
    private String displayName;

    @Schema(description = "Item description", example = "Patient requires wheelchair for mobility")
    private String description;

    @Schema(description = "Item category", example = "CLINICAL")
    private String category;

    @NotNull(message = "Required flag is required")
    @Schema(description = "Whether item is required", example = "false", required = true)
    private Boolean required;

    @Schema(description = "Display order/sequence", example = "99")
    private Integer sequenceNumber;
}
