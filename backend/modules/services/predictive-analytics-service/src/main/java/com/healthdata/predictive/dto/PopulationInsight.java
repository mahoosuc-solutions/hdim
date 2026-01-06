package com.healthdata.predictive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Issue #19: Population Health Insights Engine
 *
 * A single insight about a provider's patient panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopulationInsight {

    /**
     * Unique identifier for this insight
     */
    private UUID id;

    /**
     * Type of insight (CARE_GAP_CLUSTER, PERFORMANCE_TREND, etc.)
     */
    private InsightType type;

    /**
     * Short title summarizing the insight
     */
    private String title;

    /**
     * Detailed natural language description
     */
    private String description;

    /**
     * Impact level (HIGH, MEDIUM, LOW)
     */
    private InsightImpact impact;

    /**
     * Number of patients affected by this insight
     */
    private Integer affectedPatients;

    /**
     * List of affected patient IDs (for drilldown)
     */
    private List<String> affectedPatientIds;

    /**
     * Suggested action to address this insight
     */
    private SuggestedAction suggestedAction;

    /**
     * Quantitative metrics associated with this insight
     */
    private Map<String, Object> metrics;

    /**
     * Source data used to generate this insight
     */
    private InsightSource source;

    /**
     * When this insight was generated
     */
    private Instant generatedAt;

    /**
     * Whether this insight has been dismissed
     */
    private Boolean dismissed;

    /**
     * Reason for dismissal (if dismissed)
     */
    private String dismissalReason;

    /**
     * When this insight was dismissed (if applicable)
     */
    private Instant dismissedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightSource {
        private String dataType;      // e.g., "care_gaps", "risk_scores", "metrics"
        private String measureId;     // e.g., "COL" for colorectal screening
        private String measureName;   // Human-readable measure name
        private Instant dataAsOf;     // When the source data was current
    }
}
