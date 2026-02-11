package com.healthdata.payer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Return on Investment (ROI) analysis by HEDIS measure.
 *
 * Aggregates financial and clinical outcomes across all tasks for a specific measure,
 * enabling payers to understand which quality improvement interventions generate the highest ROI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureROIResponse {

    /**
     * HEDIS measure code (e.g., "BCS", "CDC", "COL", "CWP", "DM")
     */
    @JsonProperty("measure")
    private String measure;

    /**
     * Total quality bonus revenue captured across all interventions for this measure.
     * Sum of qualityBonusCaptured from all Phase2ExecutionTask records for this measure.
     */
    @JsonProperty("totalCaptured")
    private BigDecimal totalCaptured;

    /**
     * Total number of care gaps closed across all interventions for this measure.
     * Sum of gapsClosed from all Phase2ExecutionTask records for this measure.
     */
    @JsonProperty("totalGapsClosed")
    private Integer totalGapsClosed;

    /**
     * Number of completed intervention tasks for this measure.
     * Count of Phase2ExecutionTask records where status = COMPLETED and hediseMeasure = measure.
     */
    @JsonProperty("taskCount")
    private Integer taskCount;
}
