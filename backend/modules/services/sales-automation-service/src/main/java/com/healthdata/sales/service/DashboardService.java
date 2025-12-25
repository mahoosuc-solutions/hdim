package com.healthdata.sales.service;

import com.healthdata.sales.dto.*;
import com.healthdata.sales.entity.*;
import com.healthdata.sales.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final LeadRepository leadRepository;
    private final AccountRepository accountRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final LeadService leadService;
    private final OpportunityService opportunityService;
    private final ActivityService activityService;

    @Transactional(readOnly = true)
    public SalesDashboardDTO getDashboard(UUID tenantId) {
        return SalesDashboardDTO.builder()
            .leads(getLeadMetrics(tenantId))
            .pipeline(getPipelineMetrics(tenantId))
            .activities(getActivityMetrics(tenantId))
            .accounts(getAccountMetrics(tenantId))
            .recent(getRecentItems(tenantId))
            .build();
    }

    private SalesDashboardDTO.LeadMetrics getLeadMetrics(UUID tenantId) {
        LocalDateTime startOfMonth = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0);
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);

        // Count leads by status
        Map<String, Long> leadsByStatus = new HashMap<>();
        for (LeadStatus status : LeadStatus.values()) {
            Long count = leadRepository.countByTenantIdAndStatus(tenantId, status);
            leadsByStatus.put(status.name(), count);
        }

        // Calculate totals
        Long totalLeads = leadsByStatus.values().stream().mapToLong(Long::longValue).sum();
        Long convertedCount = leadsByStatus.getOrDefault(LeadStatus.CONVERTED.name(), 0L);
        Long qualifiedCount = leadsByStatus.getOrDefault(LeadStatus.QUALIFIED.name(), 0L);

        // Get new leads this month
        List<Lead> newLeadsThisMonth = leadRepository.findRecentLeadsByStatus(
            tenantId, LeadStatus.NEW, startOfMonth);

        // Calculate conversion rate
        Long closedLeads = convertedCount + leadsByStatus.getOrDefault(LeadStatus.UNQUALIFIED.name(), 0L);
        Double conversionRate = closedLeads > 0 ? (convertedCount * 100.0) / closedLeads : 0.0;

        // Count leads by source (simplified - would need additional repository methods for accuracy)
        Map<String, Long> leadsBySource = new HashMap<>();
        for (LeadSource source : LeadSource.values()) {
            leadsBySource.put(source.name(), 0L); // Placeholder - would need query
        }

        return SalesDashboardDTO.LeadMetrics.builder()
            .totalLeads(totalLeads)
            .newLeadsThisMonth((long) newLeadsThisMonth.size())
            .newLeadsThisWeek(0L) // Would need query
            .qualifiedLeads(qualifiedCount)
            .convertedLeadsThisMonth(0L) // Would need query
            .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
            .leadsBySource(leadsBySource)
            .leadsByStatus(leadsByStatus)
            .build();
    }

    private SalesDashboardDTO.PipelineMetrics getPipelineMetrics(UUID tenantId) {
        BigDecimal totalPipeline = opportunityRepository.sumOpenPipelineValue(tenantId);
        BigDecimal weightedPipeline = opportunityRepository.sumWeightedPipelineValue(tenantId);

        Map<String, Long> opportunitiesByStage = new HashMap<>();
        Map<String, BigDecimal> valueByStage = new HashMap<>();

        for (OpportunityStage stage : OpportunityStage.values()) {
            Long count = opportunityRepository.countByTenantIdAndStage(tenantId, stage);
            opportunitiesByStage.put(stage.name(), count);
            valueByStage.put(stage.name(), BigDecimal.ZERO); // Simplified - would need query
        }

        Long wonCount = opportunitiesByStage.getOrDefault(OpportunityStage.CLOSED_WON.name(), 0L);
        Long lostCount = opportunitiesByStage.getOrDefault(OpportunityStage.CLOSED_LOST.name(), 0L);
        Long totalClosed = wonCount + lostCount;
        Double winRate = totalClosed > 0 ? (wonCount * 100.0) / totalClosed : 0.0;

        Long openOpportunities = opportunitiesByStage.values().stream().mapToLong(Long::longValue).sum()
            - wonCount - lostCount;

        BigDecimal avgDealSize = BigDecimal.ZERO;
        if (wonCount > 0 && totalPipeline != null) {
            avgDealSize = totalPipeline.divide(BigDecimal.valueOf(wonCount), 2, RoundingMode.HALF_UP);
        }

        return SalesDashboardDTO.PipelineMetrics.builder()
            .totalPipelineValue(totalPipeline != null ? totalPipeline : BigDecimal.ZERO)
            .weightedPipelineValue(weightedPipeline != null ? weightedPipeline : BigDecimal.ZERO)
            .totalOpenOpportunities(openOpportunities)
            .wonThisMonth(0L) // Would need date-filtered query
            .lostThisMonth(0L) // Would need date-filtered query
            .wonValueThisMonth(BigDecimal.ZERO) // Would need date-filtered query
            .winRate(Math.round(winRate * 100.0) / 100.0)
            .averageDealSize(avgDealSize)
            .averageSalesCycleDays(45) // Placeholder - would need calculation
            .opportunitiesByStage(opportunitiesByStage)
            .valueByStage(valueByStage)
            .build();
    }

    private SalesDashboardDTO.ActivityMetrics getActivityMetrics(UUID tenantId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0);

        Long pendingActivities = activityRepository.countPendingActivities(tenantId);

        // Count completed activities this week/month
        List<Activity> completedThisWeek = activityRepository.findCompletedActivitiesInRange(
            tenantId, startOfWeek, now);
        List<Activity> completedThisMonth = activityRepository.findCompletedActivitiesInRange(
            tenantId, startOfMonth, now);

        // Count by type for this week
        Map<ActivityType, Long> activitiesByTypeThisWeek = completedThisWeek.stream()
            .collect(Collectors.groupingBy(Activity::getActivityType, Collectors.counting()));

        // Build activities by type map
        Map<String, Long> activitiesByType = new HashMap<>();
        for (ActivityType type : ActivityType.values()) {
            activitiesByType.put(type.name(), activitiesByTypeThisWeek.getOrDefault(type, 0L));
        }

        return SalesDashboardDTO.ActivityMetrics.builder()
            .pendingActivities(pendingActivities)
            .overdueActivities(0L) // Would need query
            .completedThisWeek((long) completedThisWeek.size())
            .completedThisMonth((long) completedThisMonth.size())
            .callsThisWeek(activitiesByTypeThisWeek.getOrDefault(ActivityType.CALL, 0L))
            .emailsThisWeek(activitiesByTypeThisWeek.getOrDefault(ActivityType.EMAIL, 0L))
            .meetingsThisWeek(activitiesByTypeThisWeek.getOrDefault(ActivityType.MEETING, 0L))
            .demosThisWeek(activitiesByTypeThisWeek.getOrDefault(ActivityType.DEMO, 0L))
            .activitiesByType(activitiesByType)
            .build();
    }

    private SalesDashboardDTO.AccountMetrics getAccountMetrics(UUID tenantId) {
        Map<String, Long> accountsByStage = new HashMap<>();
        for (AccountStage stage : AccountStage.values()) {
            Long count = accountRepository.countByTenantIdAndStage(tenantId, stage);
            accountsByStage.put(stage.name(), count);
        }

        Long totalAccounts = accountsByStage.values().stream().mapToLong(Long::longValue).sum();

        // Active accounts = not CHURNED
        Long activeAccounts = totalAccounts - accountsByStage.getOrDefault(AccountStage.CHURNED.name(), 0L);

        // Simplified - would need proper type counting
        Map<String, Long> accountsByType = new HashMap<>();
        for (OrganizationType type : OrganizationType.values()) {
            accountsByType.put(type.name(), 0L); // Placeholder
        }

        return SalesDashboardDTO.AccountMetrics.builder()
            .totalAccounts(totalAccounts)
            .activeAccounts(activeAccounts)
            .newAccountsThisMonth(0L) // Would need date-filtered query
            .accountsByStage(accountsByStage)
            .accountsByType(accountsByType)
            .build();
    }

    private SalesDashboardDTO.RecentItems getRecentItems(UUID tenantId) {
        PageRequest top5 = PageRequest.of(0, 5);

        // Get recent leads
        List<LeadDTO> recentLeads = leadService.findAll(tenantId, top5).getContent();

        // Get recent opportunities
        List<OpportunityDTO> recentOpportunities = opportunityService.findOpenOpportunities(tenantId, top5).getContent();

        // Get upcoming activities
        List<ActivityDTO> upcomingActivities = activityService.findUpcomingActivities(tenantId, 7, top5).getContent();

        // Get overdue activities
        List<ActivityDTO> overdueActivities = activityService.findOverdueActivities(tenantId, top5).getContent();

        return SalesDashboardDTO.RecentItems.builder()
            .recentLeads(recentLeads)
            .recentOpportunities(recentOpportunities)
            .upcomingActivities(upcomingActivities)
            .overdueActivities(overdueActivities)
            .build();
    }
}
