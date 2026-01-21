package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response containing checklist completion progress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Checklist completion progress summary")
public class ChecklistProgressResponse {

    @Schema(description = "Total items in checklist", example = "8")
    private Integer totalItems;

    @Schema(description = "Number of completed items", example = "5")
    private Integer completedItems;

    @Schema(description = "Number of incomplete items", example = "3")
    private Integer incompleteItems;

    @Schema(description = "Number of required items", example = "6")
    private Integer requiredItems;

    @Schema(description = "Number of completed required items", example = "4")
    private Integer completedRequiredItems;

    @Schema(description = "Overall completion percentage", example = "62.5")
    private Double overallCompletionPercentage;

    @Schema(description = "Required items completion percentage", example = "66.7")
    private Double requiredCompletionPercentage;

    @Schema(description = "Whether all required items are complete", example = "false")
    private Boolean allRequiredComplete;

    @Schema(description = "Whether checklist is fully complete", example = "false")
    private Boolean fullyComplete;

    @Schema(description = "Completion counts by category (ADMINISTRATIVE, CLINICAL, etc.)")
    private Map<String, Integer> completionByCategory;

    @Schema(description = "Total counts by category")
    private Map<String, Integer> totalByCategory;
}
