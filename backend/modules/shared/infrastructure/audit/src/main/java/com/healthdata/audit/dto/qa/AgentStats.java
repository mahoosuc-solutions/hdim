package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent Stats
 * 
 * Statistics for a specific agent type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStats {
    private long totalDecisions;
    private long approved;
    private long rejected;
    private double approvalRate;
    private double averageConfidence;
    private double accuracy;
}
