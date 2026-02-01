package com.healthdata.audit.dto.clinical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of clinical decision review
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalReviewResult {
    
    private String decisionId;
    private String reviewOutcome;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private Boolean success;
    private String message;
    private Boolean overrideApplied;
}
