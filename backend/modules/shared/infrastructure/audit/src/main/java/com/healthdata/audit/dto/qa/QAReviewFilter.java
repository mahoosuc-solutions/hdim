package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * QA Review Filter
 * 
 * Filter criteria for querying the QA review queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAReviewFilter {
    private String tenantId;
    private String agentType;
    private String priority;
    private Double minConfidence;
    private Double maxConfidence;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean includeReviewed;
}
