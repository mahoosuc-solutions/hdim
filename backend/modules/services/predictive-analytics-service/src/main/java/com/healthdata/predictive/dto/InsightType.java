package com.healthdata.predictive.dto;

/**
 * Issue #19: Population Health Insights Engine
 *
 * Types of insights that can be generated for a provider's patient panel.
 */
public enum InsightType {
    /**
     * Care Gap Cluster: >10 patients with the same care gap
     * Example: "23 patients need colorectal screening"
     */
    CARE_GAP_CLUSTER,

    /**
     * Performance Trend: >5% change in quality metrics over 30 days
     * Example: "A1c control improving (+7%)"
     */
    PERFORMANCE_TREND,

    /**
     * At-Risk Population: Patients with increasing risk scores
     * Example: "12 patients moved to high-risk tier"
     */
    AT_RISK_POPULATION,

    /**
     * Intervention Opportunity: Similar gaps across similar patients
     * Example: "Batch outreach for flu vaccines"
     */
    INTERVENTION_OPPORTUNITY
}
