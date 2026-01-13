package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * QA Flagged Decision
 * 
 * Represents a decision that has been flagged for additional review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAFlaggedDecision {
    private String decisionId;
    private String agentType;
    private String decisionType;
    private Instant timestamp;
    private Double confidenceScore;
    private String flagType;
    private String flagReason;
    private String flaggedBy;
    private Instant flaggedAt;
    private String priority;
    private String resolutionStatus;
}
