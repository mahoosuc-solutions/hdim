package com.healthdata.quality.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for AI-assisted CQL explanation.
 *
 * Issue #150: Implement AI-Assisted CQL Generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CqlExplainRequest {

    /**
     * The CQL code to explain.
     */
    @NotBlank(message = "CQL code is required")
    private String cqlCode;

    /**
     * Detail level for explanation.
     * SUMMARY - Brief overview
     * DETAILED - Line-by-line explanation
     * EDUCATIONAL - In-depth with learning resources
     */
    @Builder.Default
    private DetailLevel detailLevel = DetailLevel.DETAILED;

    /**
     * Whether to include suggestions for improvement.
     */
    @Builder.Default
    private boolean includeSuggestions = true;

    /**
     * Detail level enum.
     */
    public enum DetailLevel {
        SUMMARY,
        DETAILED,
        EDUCATIONAL
    }
}
