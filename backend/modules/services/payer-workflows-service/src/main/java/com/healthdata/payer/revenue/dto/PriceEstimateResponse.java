package com.healthdata.payer.revenue.dto;

import com.healthdata.payer.revenue.RevenueErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceEstimateResponse {
    private String tenantId;
    private String versionId;
    private String serviceCode;
    private int units;
    private BigDecimal unitRate;
    private BigDecimal estimatedAllowedAmount;
    private BigDecimal estimatedPatientResponsibility;
    private String correlationId;
    private RevenueErrorCode errorCode;
    private RevenueAuditEnvelope auditEnvelope;
}
