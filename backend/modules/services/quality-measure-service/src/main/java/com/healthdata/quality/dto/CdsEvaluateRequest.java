package com.healthdata.quality.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for evaluating CDS rules for a patient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdsEvaluateRequest {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    /**
     * Optional: Specific rule IDs to evaluate.
     * If empty, all active rules will be evaluated.
     */
    private List<UUID> ruleIds;

    /**
     * Optional: Filter by categories.
     * If empty, all categories will be evaluated.
     */
    private List<String> categories;

    /**
     * Whether to force re-evaluation even if active recommendations exist.
     * Default is false.
     */
    @Builder.Default
    private Boolean forceReEvaluation = false;

    /**
     * Whether to include rule details in the response.
     */
    @Builder.Default
    private Boolean includeRuleDetails = false;
}
