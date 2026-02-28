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
public class PriceTransparencyRatePublishResponse {
    private String tenantId;
    private String versionId;
    private String sourceReference;
    private String checksum;
    private int lineItemCount;
    private Instant publishedAt;
    private String publishedBy;
    private RevenueAuditEnvelope auditEnvelope;
}
