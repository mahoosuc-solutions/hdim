package com.healthdata.enrichment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Suggested fix for data issues.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationAction {
    private String actionId;
    private String description;
    private String issueId;
    private int priority;
    private String actionType;
    private String expectedOutcome;
}
