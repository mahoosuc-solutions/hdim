package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Related Decision
 * 
 * Information about a related decision in a decision chain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatedDecision {
    private String decisionId;
    private String agentType;
    private String decisionType;
    private String timestamp;
    private String reviewStatus;
}
