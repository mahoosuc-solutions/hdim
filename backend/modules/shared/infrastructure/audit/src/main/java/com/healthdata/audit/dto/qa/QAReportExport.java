package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * QA Report Export DTO
 *
 * Comprehensive QA review report for exporting metrics, trends, and decision history.
 * Supports multiple export formats (JSON, CSV, PDF).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAReportExport {

    /**
     * Report metadata
     */
    private Instant generatedAt;
    private String tenantId;
    private String agentType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String format;  // JSON, CSV, PDF

    /**
     * Aggregated metrics
     */
    private QAMetrics metrics;
    private QATrendData trends;

    /**
     * Summary statistics
     */
    private long totalReviews;
    private long approvedCount;
    private long rejectedCount;
    private long flaggedCount;
    private long falsePositiveCount;
    private long falseNegativeCount;
}
