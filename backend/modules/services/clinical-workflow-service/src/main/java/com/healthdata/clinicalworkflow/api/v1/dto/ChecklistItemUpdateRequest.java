package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to update checklist item status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Checklist item update request")
public class ChecklistItemUpdateRequest {

    @NotBlank(message = "Item code is required")
    @Schema(description = "Item code to update", example = "CHECK_IN", required = true)
    private String itemCode;

    @NotNull(message = "Completed status is required")
    @Schema(description = "Whether item is completed", example = "true", required = true)
    private Boolean completed;

    @Schema(description = "Completion notes", example = "All information verified")
    private String completionNotes;
}
