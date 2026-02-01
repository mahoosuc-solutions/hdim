package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * QA Decision Detail
 * 
 * Detailed information about an AI decision for QA review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QADecisionDetail {
    private String decisionId;
    private String agentType;
    private String agentVersion;
    private String decisionType;
    private Instant timestamp;
    private String priority;
    private Double confidenceScore;
    
    // Customer context
    private Map<String, Object> customerProfile;
    private String patientId;
    private String customerId;
    
    // Decision details
    private Map<String, Object> inputParameters;
    private Map<String, Object> recommendation;
    private List<String> reasoning;
    private Map<String, Object> performanceMetrics;
    
    // Review status
    private String reviewStatus;
    private String reviewedBy;
    private Instant reviewedAt;
    private String reviewNotes;
    private String reviewOutcome;
    
    // Related events
    private String correlationId;
    private List<RelatedDecision> relatedDecisions;
}
