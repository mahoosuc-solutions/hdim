package com.healthdata.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedInCampaignDTO {

    private String campaignName;
    private int totalLeads;
    private int scheduled;
    private List<String> errors;
    private LocalDateTime startDate;
    private LocalDateTime estimatedEndDate;
}
