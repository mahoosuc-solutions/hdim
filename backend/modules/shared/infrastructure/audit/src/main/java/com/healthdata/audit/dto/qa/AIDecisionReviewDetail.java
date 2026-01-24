package com.healthdata.audit.dto.qa;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Detailed AI decision review information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIDecisionReviewDetail {
    private String eventId;
    private Instant timestamp;
    private String agentType;
    private String decisionType;
    private String patientId;
    private String patientName;
    private Double confidenceScore;
    private String recommendedAction;
    private String reasoning;
    private List<String> evidenceSources;
    private Map<String, Object> decisionContext;
    private String reviewPriority;
    private String qaReviewStatus;
    private String clinicalImpact;
    private Boolean requiresPhysicianReview;
    private Instant reviewedAt;
    private String reviewedBy;
    private String reviewNotes;
    private String rejectionReason;
    private List<RelatedDecision> relatedDecisions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedDecision {
        private String eventId;
        private Instant timestamp;
        private String decisionType;
        private String status;
    }
}
