package com.healthdata.payer.revenue.dto;

import com.healthdata.payer.revenue.RevenueClaimState;
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
public class ReconciliationPreviewResponse {
    private String tenantId;
    private String claimId;
    private String remittanceId;
    private String correlationId;
    private RevenueClaimState priorStatus;
    private RevenueClaimState newStatus;
    private BigDecimal paidAmount;
    private BigDecimal adjustmentAmount;
    private BigDecimal remainingBalance;
    private RevenueErrorCode errorCode;
    private RevenueAuditEnvelope auditEnvelope;
}
