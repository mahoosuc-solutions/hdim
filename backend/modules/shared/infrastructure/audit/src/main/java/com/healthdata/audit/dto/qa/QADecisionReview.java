package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * QA Decision Review
 * 
 * Represents an AI decision pending QA review in the review queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QADecisionReview {
    private String decisionId;
    private String agentType;
    private String decisionType;
    private Instant timestamp;
    private String priority;
    private Double confidenceScore;
    private String patientId;
    private String customerId;
    private Map<String, Object> recommendation;
    private String reviewStatus;
    private Integer timeToReviewMinutes;
}
