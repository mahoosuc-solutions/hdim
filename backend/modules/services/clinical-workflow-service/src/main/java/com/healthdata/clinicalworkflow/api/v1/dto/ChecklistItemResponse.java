package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response containing individual checklist item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Individual checklist item")
public class ChecklistItemResponse {

    @Schema(description = "Item ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID id;

    @Schema(description = "Item code/identifier", example = "CHECK_IN")
    private String itemCode;

    @Schema(description = "Item display name", example = "Patient Check-In")
    private String displayName;

    @Schema(description = "Item description", example = "Verify patient demographics and insurance")
    private String description;

    @Schema(description = "Item category", example = "ADMINISTRATIVE")
    private String category;

    @Schema(description = "Whether item is required", example = "true")
    private Boolean required;

    @Schema(description = "Whether item is completed", example = "true")
    private Boolean completed;

    @Schema(description = "Sequence/display order", example = "1")
    private Integer sequenceNumber;

    @Schema(description = "When item was completed", example = "2026-01-17T09:30:00")
    private LocalDateTime completedAt;

    @Schema(description = "User who completed item", example = "NURSE001")
    private String completedBy;

    @Schema(description = "Completion notes", example = "All information verified")
    private String completionNotes;
}
