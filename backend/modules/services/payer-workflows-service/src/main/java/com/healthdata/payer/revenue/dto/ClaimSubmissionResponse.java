package com.healthdata.payer.revenue.dto;

import com.healthdata.payer.revenue.RevenueClaimState;
import com.healthdata.payer.revenue.RevenueErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSubmissionResponse {
    private String tenantId;
    private String claimId;
    private String correlationId;
    private RevenueClaimState status;
    private boolean duplicate;
    private RevenueErrorCode errorCode;
    private RevenueAuditEnvelope auditEnvelope;
}
