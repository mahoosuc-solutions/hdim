package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * QA Trend Data
 * 
 * Historical trend data for accuracy and confidence over time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QATrendData {
    private List<DailyTrendPoint> dailyTrends;
    private Map<String, List<DailyTrendPoint>> byAgentType;
}
