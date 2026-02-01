package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * QA Review Result
 * 
 * Result of a QA review action (approve, reject, or flag).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAReviewResult {
    private String decisionId;
    private String reviewStatus;
    private String reviewedBy;
    private Instant reviewedAt;
    private String outcome;
    private String message;
    private boolean success;
}
