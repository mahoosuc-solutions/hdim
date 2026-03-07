package com.healthdata.corehiveadapter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VbcRoiResponse {
    private String contractId;
    private BigDecimal estimatedRoi;
    private BigDecimal projectedSavings;
    private BigDecimal investmentRequired;
    private double projectedQualityScore;
    private int gapsToClose;
    private String modelVersion;
}
