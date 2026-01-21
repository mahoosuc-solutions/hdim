package com.healthdata.audit.dto.clinical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request to review/approve a clinical decision
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalReviewRequest {
    
    @NotBlank(message = "Review outcome is required")
    private String reviewOutcome; // APPROVED, REJECTED, NEEDS_REVISION
    
    @Size(max = 2000, message = "Review notes cannot exceed 2000 characters")
    private String reviewNotes;
    
    private Boolean applyOverride;
    private String overrideReason;
    
    @Size(max = 1000, message = "Alternative recommendation cannot exceed 1000 characters")
    private String alternativeRecommendation;
    
    private Map<String, Object> reviewerAssessment;
}
