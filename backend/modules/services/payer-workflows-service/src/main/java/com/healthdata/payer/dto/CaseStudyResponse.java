package com.healthdata.payer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Case study for a completed Phase 2 execution task.
 *
 * Represents a customer success story demonstrating the clinical and financial impact
 * of a quality improvement intervention. Can be published to marketing materials or
 * kept as draft for internal review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseStudyResponse {

    /**
     * Unique identifier for the case study task
     */
    @JsonProperty("id")
    private String id;

    /**
     * Name of the intervention task (e.g., "BCS Care Gap Closure", "Digital Outreach Program")
     */
    @JsonProperty("taskName")
    private String taskName;

    /**
     * HEDIS measure code that the intervention targeted (e.g., "BCS", "CDC", "DM")
     */
    @JsonProperty("hediseMeasure")
    private String hediseMeasure;

    /**
     * Baseline performance percentage before the intervention.
     * Represents the starting point for improvement measurement.
     * Example: 65.00 means 65% of eligible patients met the measure criteria
     */
    @JsonProperty("baselinePerformance")
    private BigDecimal baselinePerformance;

    /**
     * Current performance percentage after the intervention.
     * Shows the improvement achieved through the quality improvement initiative.
     * Example: 82.50 means 82.5% of eligible patients now meet the measure criteria
     */
    @JsonProperty("currentPerformance")
    private BigDecimal currentPerformance;

    /**
     * Total quality bonus revenue captured through this intervention.
     * Calculated based on performance improvement and applicable bonus thresholds.
     * Example: 150000.00 represents $150,000 in captured quality bonuses
     */
    @JsonProperty("bonusCaptured")
    private BigDecimal bonusCaptured;

    /**
     * Number of individual patient care gaps successfully closed.
     * Direct measure of clinical impact and intervention effectiveness.
     * Example: 45 means 45 patients moved from "gap" to "gap closed" status
     */
    @JsonProperty("gapsClosed")
    private Integer gapsClosed;

    /**
     * Customer testimonial or quote about the intervention's impact.
     * Demonstrates customer satisfaction and value realization.
     * Example: "This intervention transformed our care delivery and patient outcomes."
     */
    @JsonProperty("customerQuote")
    private String customerQuote;

    /**
     * Flag indicating whether this case study has been published.
     * - true: Case study is visible in marketing materials, investor decks, and public channels
     * - false: Case study is in draft status, pending review or approval before publication
     */
    @JsonProperty("published")
    private Boolean published;
}
