package com.healthdata.audit.dto.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA Review Request
 * 
 * Request body for approving or rejecting an AI decision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAReviewRequest {
    
    @NotBlank(message = "Reviewer identifier is required")
    private String reviewedBy;
    
    @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
    private String reviewNotes;
    
    private Boolean isFalsePositive;
    private Boolean isFalseNegative;
    private String correctDecisionType;
}
