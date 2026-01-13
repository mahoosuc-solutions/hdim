package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent Performance
 * 
 * Performance metrics by agent type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPerformance {
    private Map<String, AgentStats> byAgentType;
}
