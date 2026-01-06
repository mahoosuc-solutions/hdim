package com.healthdata.predictive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Issue #19: Population Health Insights Engine
 *
 * Response containing all insights for a provider's patient panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopulationInsightsResponse {

    /**
     * Provider this response is generated for
     */
    private String providerId;

    /**
     * Tenant context
     */
    private String tenantId;

    /**
     * When this response was generated
     */
    private Instant generatedAt;

    /**
     * Total patients in the provider's panel
     */
    private Integer totalPanelSize;

    /**
     * List of population health insights
     */
    private List<PopulationInsight> insights;

    /**
     * Summary counts by impact level
     */
    private InsightSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightSummary {
        private Integer highImpact;
        private Integer mediumImpact;
        private Integer lowImpact;
        private Integer dismissed;
        private Integer totalInsights;
    }
}
