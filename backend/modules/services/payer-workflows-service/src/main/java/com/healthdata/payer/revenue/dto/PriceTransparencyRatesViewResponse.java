package com.healthdata.payer.revenue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceTransparencyRatesViewResponse {
    private String tenantId;
    private String versionId;
    private String sourceReference;
    private String checksum;
    private Instant publishedAt;
    private String publishedBy;
    private List<PriceTransparencyRateEntry> rates;
    private RevenueAuditEnvelope auditEnvelope;
}
