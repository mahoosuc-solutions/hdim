package com.healthdata.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive sales dashboard metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesDashboardDTO {

    // Lead Metrics
    private LeadMetrics leads;

    // Pipeline Metrics
    private PipelineMetrics pipeline;

    // Activity Metrics
    private ActivityMetrics activities;

    // Account Metrics
    private AccountMetrics accounts;

    // Recent Items
    private RecentItems recent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeadMetrics {
        private Long totalLeads;
        private Long newLeadsThisMonth;
        private Long newLeadsThisWeek;
        private Long qualifiedLeads;
        private Long convertedLeadsThisMonth;
        private Double conversionRate;
        private Map<String, Long> leadsBySource;
        private Map<String, Long> leadsByStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineMetrics {
        private BigDecimal totalPipelineValue;
        private BigDecimal weightedPipelineValue;
        private Long totalOpenOpportunities;
        private Long wonThisMonth;
        private Long lostThisMonth;
        private BigDecimal wonValueThisMonth;
        private Double winRate;
        private BigDecimal averageDealSize;
        private Integer averageSalesCycleDays;
        private Map<String, Long> opportunitiesByStage;
        private Map<String, BigDecimal> valueByStage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityMetrics {
        private Long pendingActivities;
        private Long overdueActivities;
        private Long completedThisWeek;
        private Long completedThisMonth;
        private Long callsThisWeek;
        private Long emailsThisWeek;
        private Long meetingsThisWeek;
        private Long demosThisWeek;
        private Map<String, Long> activitiesByType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountMetrics {
        private Long totalAccounts;
        private Long activeAccounts;
        private Long newAccountsThisMonth;
        private Map<String, Long> accountsByStage;
        private Map<String, Long> accountsByType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentItems {
        private List<LeadDTO> recentLeads;
        private List<OpportunityDTO> recentOpportunities;
        private List<ActivityDTO> upcomingActivities;
        private List<ActivityDTO> overdueActivities;
    }
}
