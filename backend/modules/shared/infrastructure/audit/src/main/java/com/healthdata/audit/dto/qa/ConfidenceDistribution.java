package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Confidence Distribution
 * 
 * Distribution of confidence scores across decision categories.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfidenceDistribution {
    private long highConfidence;    // >= 0.9
    private long mediumConfidence;  // 0.7 - 0.89
    private long lowConfidence;     // < 0.7
}
