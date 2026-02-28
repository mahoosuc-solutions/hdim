package com.healthdata.payer.revenue.dto;

import com.healthdata.payer.revenue.RevenueErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCheckResponse {
    private String tenantId;
    private String payerId;
    private String patientId;
    private String correlationId;
    private boolean eligible;
    private RevenueErrorCode errorCode;
    private RevenueAuditEnvelope auditEnvelope;
}
