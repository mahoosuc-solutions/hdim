package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Daily Trend Point
 * 
 * Single data point in a trend analysis representing one day.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrendPoint {
    private LocalDate date;
    private long totalDecisions;
    private long approved;
    private long rejected;
    private double accuracy;
    private double averageConfidence;
}
