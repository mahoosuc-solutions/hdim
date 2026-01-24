package com.healthdata.audit.dto.qa;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * AI decision pending QA review
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIDecisionForReview {

    private String eventId;
    private Instant timestamp;
    private String agentType;
    private String decisionType;
    private String patientId;
    private Double confidenceScore;
    private String recommendedAction;
    private String reasoning;
    private String reviewPriority; // critical, high, medium, low
    private String qaReviewStatus; // pending, approved, rejected, flagged, false-positive, false-negative
    private String clinicalImpact; // high, medium, low
    private Boolean requiresPhysicianReview;
    private String tenantId;
    private Instant reviewedAt;
    private String reviewedBy;
    private String reviewNotes;
}
