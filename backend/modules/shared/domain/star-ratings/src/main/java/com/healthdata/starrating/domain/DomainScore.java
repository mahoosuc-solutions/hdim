package com.healthdata.starrating.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainScore {

    private StarRatingDomain domain;
    private List<MeasureScore> measureScores;
    private double domainStars;
    private double domainWeight;
    private int measureCount;
    private double averagePerformanceRate;
    private Double domainImprovement;
}
