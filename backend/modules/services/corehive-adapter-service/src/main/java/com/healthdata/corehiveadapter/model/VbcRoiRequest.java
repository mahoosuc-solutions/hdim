package com.healthdata.corehiveadapter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Value-based care ROI calculation request for CoreHive.
 * Contains only contract parameters — no PHI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VbcRoiRequest {
    private String contractId;
    private String tenantId;
    private String contractType;
    private int totalLives;
    private BigDecimal pmpm;
    private BigDecimal totalContractValue;
    private double currentQualityScore;
    private double targetQualityScore;
    private int openCareGapCount;
}
