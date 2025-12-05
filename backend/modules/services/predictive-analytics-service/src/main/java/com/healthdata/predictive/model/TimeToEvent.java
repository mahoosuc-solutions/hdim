package com.healthdata.predictive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Predicted time to key events (disease progression, hospitalization, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeToEvent {

    /**
     * Event type (e.g., "progression", "hospitalization", "complication")
     */
    private String eventType;

    /**
     * Predicted days until event
     */
    private Integer predictedDays;

    /**
     * Predicted months until event
     */
    private Integer predictedMonths;

    /**
     * Predicted event date
     */
    private LocalDate predictedEventDate;

    /**
     * Confidence interval - lower bound (days)
     */
    private Integer confidenceLowerDays;

    /**
     * Confidence interval - upper bound (days)
     */
    private Integer confidenceUpperDays;

    /**
     * Probability that event will occur within predicted timeframe
     */
    private double eventProbability;

    /**
     * Calculate predicted months from days
     */
    public Integer calculatePredictedMonths() {
        return predictedDays != null ? predictedDays / 30 : null;
    }
}
