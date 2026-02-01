package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA review request body
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAReviewRequest {
    private String reviewedBy;
    private String notes;
    private String rejectionReason;
    private String reviewNotes;
    private Boolean isFalsePositive;
    private Boolean isFalseNegative;
    private String correctDecisionType;
}
