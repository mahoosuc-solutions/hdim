package com.healthdata.predictive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Issue #19: Population Health Insights Engine
 *
 * Suggested action for addressing an insight.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedAction {

    /**
     * Type of action recommended
     */
    private ActionType type;

    /**
     * Human-readable description of the action
     */
    private String description;

    /**
     * Estimated effort required (in minutes)
     */
    private Integer estimatedEffortMinutes;

    /**
     * Priority score for this action (0-100)
     */
    private Integer priority;

    public enum ActionType {
        BATCH_OUTREACH,      // Contact multiple patients at once
        INDIVIDUAL_FOLLOWUP, // One-on-one patient contact
        SCHEDULE_VISITS,     // Book appointments
        ORDER_LABS,          // Order diagnostic tests
        MEDICATION_REVIEW,   // Review/adjust medications
        CARE_COORDINATION,   // Coordinate with specialists
        PATIENT_EDUCATION,   // Educational outreach
        RISK_ASSESSMENT      // Perform risk evaluations
    }
}
