package com.healthdata.payer.revenue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAuditEnvelope {
    private String tenantId;
    private String correlationId;
    private String actor;
    private Instant timestamp;
    private String action;
    private String outcome;
}
