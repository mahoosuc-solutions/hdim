package com.healthdata.enrichment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Data quality metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataQualityScore {
    private double completenessScore;
    private double accuracyScore;
    private double consistencyScore;
    private double timelinessScore;
    private double overallScore;

    @Builder.Default
    private Map<String, Double> dimensionScores = new HashMap<>();
}
