package com.healthdata.audit.dto.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA Flag Request
 * 
 * Request body for flagging an AI decision for additional review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAFlagRequest {
    
    @NotBlank(message = "Reviewer identifier is required")
    private String reviewedBy;
    
    @NotNull(message = "Flag type is required")
    private String flagType; // NEEDS_ESCALATION, DATA_QUALITY_ISSUE, ALGORITHM_ERROR, CLINICAL_REVIEW_NEEDED
    
    @NotBlank(message = "Flag reason is required")
    @Size(max = 2000, message = "Flag reason must not exceed 2000 characters")
    private String flagReason;
    
    private String priority; // HIGH, MEDIUM, LOW
}
