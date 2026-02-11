package com.healthdata.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for bulk campaign creation with leads.
 * Returns information about the scheduled outreach activities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedInBulkCampaignResponse {

    private UUID campaignId;
    private String campaignName;
    private int totalLeads;
    private int scheduled;
    private List<String> errors;
    private LocalDateTime startDate;
    private LocalDateTime estimatedEndDate;
}
