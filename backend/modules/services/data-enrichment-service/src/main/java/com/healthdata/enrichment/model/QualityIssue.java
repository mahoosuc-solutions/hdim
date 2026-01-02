package com.healthdata.enrichment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Identified data quality issue.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityIssue {
    private String issueId;
    private QualityDimension dimension;
    private String description;
    private String severity;
    private String affectedElement;
    private String location;
}
