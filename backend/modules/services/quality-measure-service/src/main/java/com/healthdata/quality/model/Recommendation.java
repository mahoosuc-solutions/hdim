package com.healthdata.quality.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Clinical recommendation generated from measure calculation results.
 * Provides actionable guidance for improving patient outcomes.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Recommendation {

    /**
     * Priority level: "high", "medium", "low"
     */
    private String priority;

    /**
     * Recommended action
     * Example: "Consider intensive medication management"
     */
    private String action;

    /**
     * Clinical rationale for the recommendation
     * Example: "HbA1c > 9% indicates poor glycemic control"
     */
    private String rationale;

    /**
     * Category of recommendation
     * Values: "medication", "referral", "visit", "lifestyle", "screening"
     */
    private String category;
}
