package com.healthdata.starrating.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureScore {

    private StarRatingMeasure measure;
    private double performanceRate;
    private int stars;
    private int numerator;
    private int denominator;
    private double weight;
    private double[] cutPoints;
}
