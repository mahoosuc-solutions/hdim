package com.healthdata.sales.service;

import com.healthdata.sales.dto.*;
import com.healthdata.sales.entity.*;
import com.healthdata.sales.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for pipeline management, Kanban views, and forecasting
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineService {

    private final OpportunityRepository opportunityRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final ActivityRepository activityRepository;
    private final LeadRepository leadRepository;

    private static final int STAGNANT_DAYS_THRESHOLD = 14;
    private static final int AT_RISK_DAYS_NO_ACTIVITY = 7;

    /**
     * Get pipeline Kanban view with opportunities organized by stage
     */
    @Transactional(readOnly = true)
    public PipelineKanbanDTO getKanbanView(UUID tenantId) {
        List<PipelineKanbanDTO.KanbanColumn> columns = new ArrayList<>();
        Map<String, BigDecimal> valueByStage = new HashMap<>();
        Map<String, Long> countByStage = new HashMap<>();

        BigDecimal totalPipelineValue = BigDecimal.ZERO;
        BigDecimal totalWeightedValue = BigDecimal.ZERO;
        long totalOpenOpportunities = 0;
        long totalDaysOpen = 0;
        int opportunityCount = 0;

        // Build columns for each stage (excluding closed stages for Kanban)
        for (OpportunityStage stage : OpportunityStage.values()) {
            List<Opportunity> opportunities = opportunityRepository
                .findByTenantIdAndStage(tenantId, stage, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "amount")))
                .getContent();

            List<PipelineKanbanDTO.KanbanCard> cards = new ArrayList<>();
            BigDecimal stageValue = BigDecimal.ZERO;
            BigDecimal stageWeightedValue = BigDecimal.ZERO;

            for (Opportunity opp : opportunities) {
                PipelineKanbanDTO.KanbanCard card = buildKanbanCard(opp, tenantId);
                cards.add(card);

                if (opp.getAmount() != null) {
                    stageValue = stageValue.add(opp.getAmount());
                    stageWeightedValue = stageWeightedValue.add(opp.getWeightedAmount());
                }

                if (stage.isOpen()) {
                    totalOpenOpportunities++;
                    if (opp.getCreatedAt() != null) {
                        totalDaysOpen += ChronoUnit.DAYS.between(opp.getCreatedAt().toLocalDate(), LocalDate.now());
                        opportunityCount++;
                    }
                }
            }

            valueByStage.put(stage.name(), stageValue);
            countByStage.put(stage.name(), (long) opportunities.size());

            if (stage.isOpen()) {
                totalPipelineValue = totalPipelineValue.add(stageValue);
                totalWeightedValue = totalWeightedValue.add(stageWeightedValue);
            }

            columns.add(PipelineKanbanDTO.KanbanColumn.builder()
                .stage(stage)
                .stageName(formatStageName(stage))
                .stageOrder(stage.getOrder())
                .defaultProbability(stage.getDefaultProbability())
                .opportunityCount((long) opportunities.size())
                .totalValue(stageValue)
                .weightedValue(stageWeightedValue)
                .opportunities(cards)
                .build());
        }

        // Calculate summary statistics
        BigDecimal avgDealSize = totalOpenOpportunities > 0
            ? totalPipelineValue.divide(BigDecimal.valueOf(totalOpenOpportunities), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        int avgDaysOpen = opportunityCount > 0 ? (int) (totalDaysOpen / opportunityCount) : 0;

        double avgProbability = columns.stream()
            .filter(c -> c.getStage().isOpen())
            .flatMap(c -> c.getOpportunities().stream())
            .mapToInt(card -> card.getProbability() != null ? card.getProbability() : 0)
            .average()
            .orElse(0.0);

        PipelineKanbanDTO.PipelineSummary summary = PipelineKanbanDTO.PipelineSummary.builder()
            .totalOpportunities(countByStage.values().stream().mapToLong(Long::longValue).sum())
            .openOpportunities(totalOpenOpportunities)
            .totalPipelineValue(totalPipelineValue)
            .weightedPipelineValue(totalWeightedValue)
            .averageProbability(Math.round(avgProbability * 100.0) / 100.0)
            .averageDealSize(avgDealSize)
            .averageDaysOpen(avgDaysOpen)
            .valueByStage(valueByStage)
            .countByStage(countByStage)
            .build();

        return PipelineKanbanDTO.builder()
            .columns(columns)
            .summary(summary)
            .build();
    }

    /**
     * Get sales forecast with monthly/quarterly projections
     */
    @Transactional(readOnly = true)
    public PipelineForecastDTO getForecast(UUID tenantId) {
        LocalDate today = LocalDate.now();

        // Calculate current periods
        PipelineForecastDTO.ForecastPeriod currentMonth = calculatePeriodForecast(
            tenantId,
            today.with(TemporalAdjusters.firstDayOfMonth()),
            today.with(TemporalAdjusters.lastDayOfMonth()),
            "Current Month"
        );

        PipelineForecastDTO.ForecastPeriod currentQuarter = calculatePeriodForecast(
            tenantId,
            today.with(today.getMonth().firstMonthOfQuarter()).with(TemporalAdjusters.firstDayOfMonth()),
            today.with(today.getMonth().firstMonthOfQuarter()).plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()),
            "Current Quarter"
        );

        PipelineForecastDTO.ForecastPeriod currentYear = calculatePeriodForecast(
            tenantId,
            today.with(TemporalAdjusters.firstDayOfYear()),
            today.with(TemporalAdjusters.lastDayOfYear()),
            "Current Year"
        );

        // Monthly forecasts for next 6 months
        List<PipelineForecastDTO.MonthlyForecast> monthlyForecasts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            YearMonth month = YearMonth.now().plusMonths(i);
            monthlyForecasts.add(calculateMonthlyForecast(tenantId, month));
        }

        // Conversion funnel
        PipelineForecastDTO.ConversionFunnel funnel = calculateConversionFunnel(tenantId);

        // Historical comparison
        PipelineForecastDTO.HistoricalComparison historical = calculateHistoricalComparison(tenantId);

        // At-risk deals
        List<PipelineForecastDTO.AtRiskDeal> atRiskDeals = identifyAtRiskDeals(tenantId);

        return PipelineForecastDTO.builder()
            .currentMonth(currentMonth)
            .currentQuarter(currentQuarter)
            .currentYear(currentYear)
            .monthlyForecasts(monthlyForecasts)
            .funnel(funnel)
            .historical(historical)
            .atRiskDeals(atRiskDeals)
            .build();
    }

    /**
     * Move opportunity to a new stage with workflow automation
     */
    @Transactional
    public OpportunityDTO moveToStage(UUID tenantId, UUID opportunityId, StageTransitionRequest request) {
        Opportunity opportunity = opportunityRepository.findByIdAndTenantId(opportunityId, tenantId)
            .orElseThrow(() -> new RuntimeException("Opportunity not found: " + opportunityId));

        OpportunityStage previousStage = opportunity.getStage();
        OpportunityStage newStage = request.getTargetStage();

        // Validate transition
        validateStageTransition(previousStage, newStage);

        // Update opportunity
        opportunity.setStage(newStage);
        opportunity.updateProbabilityFromStage();

        if (request.getNextStep() != null) {
            opportunity.setNextStep(request.getNextStep());
        }

        // Handle closed stages
        if (newStage == OpportunityStage.CLOSED_WON) {
            opportunity.setActualCloseDate(request.getCloseDate() != null ? request.getCloseDate() : LocalDate.now());
            if (request.getFinalAmount() != null) {
                opportunity.setAmount(request.getFinalAmount());
            }
            if (request.getContractLengthMonths() != null) {
                opportunity.setContractLengthMonths(request.getContractLengthMonths());
            }
        } else if (newStage == OpportunityStage.CLOSED_LOST) {
            opportunity.setActualCloseDate(LocalDate.now());
            opportunity.setLostReason(request.getLostReason());
            opportunity.setLostReasonDetail(request.getLostReasonDetail());
            opportunity.setCompetitor(request.getCompetitor());
        }

        opportunity = opportunityRepository.save(opportunity);
        log.info("Moved opportunity {} from {} to {}", opportunityId, previousStage, newStage);

        // Create follow-up task if requested
        if (Boolean.TRUE.equals(request.getCreateFollowUpTask()) && newStage.isOpen()) {
            createFollowUpActivity(tenantId, opportunity, request);
        }

        return toDTO(opportunity);
    }

    /**
     * Get opportunities closing soon (within specified days)
     */
    @Transactional(readOnly = true)
    public List<OpportunityDTO> getClosingSoon(UUID tenantId, int withinDays) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(withinDays);

        return opportunityRepository.findByExpectedCloseDateRange(tenantId, startDate, endDate, PageRequest.of(0, 50))
            .getContent()
            .stream()
            .filter(o -> o.getStage().isOpen())
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get stagnant opportunities (no activity in X days)
     */
    @Transactional(readOnly = true)
    public List<OpportunityDTO> getStagnantOpportunities(UUID tenantId, int stagnantDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(stagnantDays);

        return opportunityRepository.findOpenOpportunities(tenantId, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .filter(opp -> {
                List<Activity> activities = activityRepository.findByOpportunityId(opp.getId());
                if (activities.isEmpty()) {
                    return true;
                }
                LocalDateTime lastActivity = activities.stream()
                    .map(Activity::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(opp.getCreatedAt());
                return lastActivity.isBefore(cutoffDate);
            })
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    private PipelineKanbanDTO.KanbanCard buildKanbanCard(Opportunity opp, UUID tenantId) {
        // Get account name
        String accountName = null;
        if (opp.getAccountId() != null) {
            accountName = accountRepository.findByIdAndTenantId(opp.getAccountId(), tenantId)
                .map(Account::getName)
                .orElse(null);
        }

        // Get contact name
        String contactName = null;
        if (opp.getPrimaryContactId() != null) {
            contactName = contactRepository.findByIdAndTenantId(opp.getPrimaryContactId(), tenantId)
                .map(c -> c.getFirstName() + " " + c.getLastName())
                .orElse(null);
        }

        // Calculate days in stage and total days open
        int daysInStage = 0;
        int totalDaysOpen = 0;
        if (opp.getCreatedAt() != null) {
            totalDaysOpen = (int) ChronoUnit.DAYS.between(opp.getCreatedAt().toLocalDate(), LocalDate.now());
            daysInStage = (int) ChronoUnit.DAYS.between(opp.getUpdatedAt().toLocalDate(), LocalDate.now());
        }

        // Check if overdue
        boolean isOverdue = opp.getExpectedCloseDate() != null &&
                           opp.getExpectedCloseDate().isBefore(LocalDate.now()) &&
                           opp.getStage().isOpen();

        // Get activity info
        List<Activity> activities = activityRepository.findByOpportunityId(opp.getId());
        int activityCount = activities.size();
        LocalDate lastActivityDate = activities.stream()
            .map(a -> a.getCreatedAt().toLocalDate())
            .max(LocalDate::compareTo)
            .orElse(null);

        // Check if at risk
        boolean isAtRisk = isOverdue || daysInStage > STAGNANT_DAYS_THRESHOLD ||
            (lastActivityDate != null && ChronoUnit.DAYS.between(lastActivityDate, LocalDate.now()) > AT_RISK_DAYS_NO_ACTIVITY);

        return PipelineKanbanDTO.KanbanCard.builder()
            .id(opp.getId())
            .name(opp.getName())
            .accountName(accountName)
            .accountId(opp.getAccountId())
            .contactName(contactName)
            .primaryContactId(opp.getPrimaryContactId())
            .amount(opp.getAmount())
            .probability(opp.getProbability())
            .weightedAmount(opp.getWeightedAmount())
            .expectedCloseDate(opp.getExpectedCloseDate())
            .nextStep(opp.getNextStep())
            .productTier(opp.getProductTier())
            .ownerUserId(opp.getOwnerUserId())
            .daysInStage(daysInStage)
            .totalDaysOpen(totalDaysOpen)
            .isOverdue(isOverdue)
            .isAtRisk(isAtRisk)
            .activityCount(activityCount)
            .lastActivityDate(lastActivityDate)
            .build();
    }

    private PipelineForecastDTO.ForecastPeriod calculatePeriodForecast(
            UUID tenantId, LocalDate startDate, LocalDate endDate, String periodName) {

        List<Opportunity> periodOpportunities = opportunityRepository
            .findByExpectedCloseDateRange(tenantId, startDate, endDate, PageRequest.of(0, 1000))
            .getContent()
            .stream()
            .filter(o -> o.getStage().isOpen())
            .collect(Collectors.toList());

        BigDecimal committedValue = BigDecimal.ZERO;
        BigDecimal bestCaseValue = BigDecimal.ZERO;
        BigDecimal pipelineValue = BigDecimal.ZERO;
        BigDecimal weightedForecast = BigDecimal.ZERO;

        for (Opportunity opp : periodOpportunities) {
            BigDecimal amount = opp.getAmount() != null ? opp.getAmount() : BigDecimal.ZERO;
            int probability = opp.getProbability() != null ? opp.getProbability() : 0;

            if (probability >= 70) {
                committedValue = committedValue.add(amount);
            } else if (probability >= 30) {
                bestCaseValue = bestCaseValue.add(amount);
            } else {
                pipelineValue = pipelineValue.add(amount);
            }

            weightedForecast = weightedForecast.add(opp.getWeightedAmount());
        }

        // Get closed won for period
        BigDecimal closedWonValue = BigDecimal.ZERO;
        long closedWonCount = 0;
        List<Opportunity> wonOpportunities = opportunityRepository
            .findByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_WON, PageRequest.of(0, 1000))
            .getContent()
            .stream()
            .filter(o -> o.getActualCloseDate() != null &&
                        !o.getActualCloseDate().isBefore(startDate) &&
                        !o.getActualCloseDate().isAfter(endDate))
            .collect(Collectors.toList());

        for (Opportunity won : wonOpportunities) {
            closedWonValue = closedWonValue.add(won.getAmount() != null ? won.getAmount() : BigDecimal.ZERO);
            closedWonCount++;
        }

        return PipelineForecastDTO.ForecastPeriod.builder()
            .periodName(periodName)
            .startDate(startDate)
            .endDate(endDate)
            .committedValue(committedValue)
            .bestCaseValue(bestCaseValue)
            .pipelineValue(pipelineValue)
            .weightedForecast(weightedForecast)
            .dealCount((long) periodOpportunities.size())
            .closedWonValue(closedWonValue)
            .closedWonCount(closedWonCount)
            .build();
    }

    private PipelineForecastDTO.MonthlyForecast calculateMonthlyForecast(UUID tenantId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<Opportunity> opportunities = opportunityRepository
            .findByExpectedCloseDateRange(tenantId, startDate, endDate, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .filter(o -> o.getStage().isOpen())
            .collect(Collectors.toList());

        BigDecimal expectedValue = opportunities.stream()
            .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weightedValue = opportunities.stream()
            .map(Opportunity::getWeightedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OpportunityDTO> topOpportunities = opportunities.stream()
            .sorted((a, b) -> {
                BigDecimal amountA = a.getAmount() != null ? a.getAmount() : BigDecimal.ZERO;
                BigDecimal amountB = b.getAmount() != null ? b.getAmount() : BigDecimal.ZERO;
                return amountB.compareTo(amountA);
            })
            .limit(5)
            .map(this::toDTO)
            .collect(Collectors.toList());

        return PipelineForecastDTO.MonthlyForecast.builder()
            .month(month)
            .monthName(month.getMonth().name() + " " + month.getYear())
            .expectedValue(expectedValue)
            .weightedValue(weightedValue)
            .dealCount((long) opportunities.size())
            .topOpportunities(topOpportunities)
            .build();
    }

    private PipelineForecastDTO.ConversionFunnel calculateConversionFunnel(UUID tenantId) {
        // Count leads by status
        long totalLeads = 0;
        long qualifiedLeads = 0;
        for (LeadStatus status : LeadStatus.values()) {
            long count = leadRepository.countByTenantIdAndStatus(tenantId, status);
            totalLeads += count;
            if (status == LeadStatus.QUALIFIED || status == LeadStatus.CONVERTED) {
                qualifiedLeads += count;
            }
        }

        // Count opportunities by stage
        long discoveryCount = opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.DISCOVERY);
        long demoCount = opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.DEMO);
        long proposalCount = opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.PROPOSAL);
        long negotiationCount = opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.NEGOTIATION);
        long closedWonCount = opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_WON);
        long closedLostCount = opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_LOST);

        long totalOpportunities = discoveryCount + demoCount + proposalCount + negotiationCount + closedWonCount + closedLostCount;

        // Calculate conversion rates
        double leadToQualified = totalLeads > 0 ? (qualifiedLeads * 100.0) / totalLeads : 0.0;
        double overallWinRate = (closedWonCount + closedLostCount) > 0
            ? (closedWonCount * 100.0) / (closedWonCount + closedLostCount) : 0.0;

        return PipelineForecastDTO.ConversionFunnel.builder()
            .totalLeads(totalLeads)
            .qualifiedLeads(qualifiedLeads)
            .opportunities(totalOpportunities)
            .demos(demoCount)
            .proposals(proposalCount)
            .negotiations(negotiationCount)
            .closedWon(closedWonCount)
            .leadToQualifiedRate(Math.round(leadToQualified * 100.0) / 100.0)
            .overallWinRate(Math.round(overallWinRate * 100.0) / 100.0)
            .build();
    }

    private PipelineForecastDTO.HistoricalComparison calculateHistoricalComparison(UUID tenantId) {
        LocalDate today = LocalDate.now();

        // Last month
        LocalDate lastMonthStart = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastMonthEnd = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        BigDecimal lastMonthClosed = calculateClosedWonValue(tenantId, lastMonthStart, lastMonthEnd);

        // Last quarter
        LocalDate lastQuarterStart = today.minusMonths(3).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastQuarterEnd = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        BigDecimal lastQuarterClosed = calculateClosedWonValue(tenantId, lastQuarterStart, lastQuarterEnd);

        // Last year
        LocalDate lastYearStart = today.minusYears(1).with(TemporalAdjusters.firstDayOfYear());
        LocalDate lastYearEnd = today.minusYears(1).with(TemporalAdjusters.lastDayOfYear());
        BigDecimal lastYearClosed = calculateClosedWonValue(tenantId, lastYearStart, lastYearEnd);

        return PipelineForecastDTO.HistoricalComparison.builder()
            .lastMonthClosed(lastMonthClosed)
            .lastQuarterClosed(lastQuarterClosed)
            .lastYearClosed(lastYearClosed)
            .averageSalesCycleDays(45) // Would need historical tracking
            .build();
    }

    private BigDecimal calculateClosedWonValue(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        return opportunityRepository.findByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_WON, PageRequest.of(0, 1000))
            .getContent()
            .stream()
            .filter(o -> o.getActualCloseDate() != null &&
                        !o.getActualCloseDate().isBefore(startDate) &&
                        !o.getActualCloseDate().isAfter(endDate))
            .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<PipelineForecastDTO.AtRiskDeal> identifyAtRiskDeals(UUID tenantId) {
        List<PipelineForecastDTO.AtRiskDeal> atRiskDeals = new ArrayList<>();
        LocalDateTime activityCutoff = LocalDateTime.now().minusDays(AT_RISK_DAYS_NO_ACTIVITY);

        List<Opportunity> openOpportunities = opportunityRepository
            .findOpenOpportunities(tenantId, PageRequest.of(0, 100))
            .getContent();

        for (Opportunity opp : openOpportunities) {
            List<String> riskReasons = new ArrayList<>();
            int riskScore = 0;

            // Check if overdue
            boolean isOverdue = opp.getExpectedCloseDate() != null &&
                               opp.getExpectedCloseDate().isBefore(LocalDate.now());
            if (isOverdue) {
                int daysOverdue = (int) ChronoUnit.DAYS.between(opp.getExpectedCloseDate(), LocalDate.now());
                riskReasons.add("Overdue by " + daysOverdue + " days");
                riskScore += Math.min(daysOverdue * 2, 40);
            }

            // Check for stagnant (no recent activity)
            List<Activity> activities = activityRepository.findByOpportunityId(opp.getId());
            LocalDateTime lastActivity = activities.stream()
                .map(Activity::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(opp.getCreatedAt());

            int daysSinceActivity = (int) ChronoUnit.DAYS.between(lastActivity.toLocalDate(), LocalDate.now());
            if (daysSinceActivity > AT_RISK_DAYS_NO_ACTIVITY) {
                riskReasons.add("No activity for " + daysSinceActivity + " days");
                riskScore += Math.min(daysSinceActivity * 3, 30);
            }

            // Check days in stage
            int daysInStage = (int) ChronoUnit.DAYS.between(opp.getUpdatedAt().toLocalDate(), LocalDate.now());
            if (daysInStage > STAGNANT_DAYS_THRESHOLD) {
                riskReasons.add("Stagnant in " + opp.getStage().name() + " for " + daysInStage + " days");
                riskScore += Math.min(daysInStage, 30);
            }

            if (!riskReasons.isEmpty()) {
                String recommendedAction = determineRecommendedAction(opp, riskReasons);

                atRiskDeals.add(PipelineForecastDTO.AtRiskDeal.builder()
                    .opportunity(toDTO(opp))
                    .riskReason(String.join("; ", riskReasons))
                    .riskScore(Math.min(riskScore, 100))
                    .daysSinceActivity(daysSinceActivity)
                    .daysOverdue(isOverdue ? (int) ChronoUnit.DAYS.between(opp.getExpectedCloseDate(), LocalDate.now()) : 0)
                    .stagnant(daysInStage > STAGNANT_DAYS_THRESHOLD)
                    .recommendedAction(recommendedAction)
                    .build());
            }
        }

        // Sort by risk score descending
        atRiskDeals.sort((a, b) -> Integer.compare(b.getRiskScore(), a.getRiskScore()));
        return atRiskDeals.stream().limit(10).collect(Collectors.toList());
    }

    private String determineRecommendedAction(Opportunity opp, List<String> riskReasons) {
        if (riskReasons.stream().anyMatch(r -> r.contains("No activity"))) {
            return "Schedule follow-up call or email";
        }
        if (riskReasons.stream().anyMatch(r -> r.contains("Overdue"))) {
            return "Update expected close date or move to closed-lost";
        }
        if (riskReasons.stream().anyMatch(r -> r.contains("Stagnant"))) {
            return "Review deal strategy and next steps";
        }
        return "Review and update opportunity status";
    }

    private void validateStageTransition(OpportunityStage from, OpportunityStage to) {
        // Cannot reopen closed deals
        if (from.isClosed() && to.isOpen()) {
            throw new IllegalStateException("Cannot reopen a closed opportunity");
        }
        // Cannot skip from discovery to closed
        if (from == OpportunityStage.DISCOVERY && to.isClosed()) {
            log.warn("Opportunity moving directly from DISCOVERY to {}", to);
        }
    }

    private void createFollowUpActivity(UUID tenantId, Opportunity opportunity, StageTransitionRequest request) {
        Activity activity = new Activity();
        activity.setId(UUID.randomUUID());
        activity.setTenantId(tenantId);
        activity.setOpportunityId(opportunity.getId());
        activity.setAccountId(opportunity.getAccountId());
        activity.setActivityType(ActivityType.TASK);
        activity.setSubject("Follow-up: " + opportunity.getName());
        activity.setDescription(request.getFollowUpTaskDescription() != null
            ? request.getFollowUpTaskDescription()
            : "Follow up on stage transition to " + opportunity.getStage().name());
        activity.setScheduledAt(LocalDateTime.now().plusDays(
            request.getFollowUpDays() != null ? request.getFollowUpDays() : 3));
        activity.setCompleted(false);
        activity.setAssignedToUserId(opportunity.getOwnerUserId());

        activityRepository.save(activity);
        log.info("Created follow-up task for opportunity {}", opportunity.getId());
    }

    private String formatStageName(OpportunityStage stage) {
        String name = stage.name().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    private OpportunityDTO toDTO(Opportunity entity) {
        return OpportunityDTO.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .accountId(entity.getAccountId())
            .primaryContactId(entity.getPrimaryContactId())
            .name(entity.getName())
            .description(entity.getDescription())
            .amount(entity.getAmount())
            .stage(entity.getStage())
            .probability(entity.getProbability())
            .expectedCloseDate(entity.getExpectedCloseDate())
            .actualCloseDate(entity.getActualCloseDate())
            .lostReason(entity.getLostReason())
            .lostReasonDetail(entity.getLostReasonDetail())
            .competitor(entity.getCompetitor())
            .nextStep(entity.getNextStep())
            .productTier(entity.getProductTier())
            .contractLengthMonths(entity.getContractLengthMonths())
            .zohoOpportunityId(entity.getZohoOpportunityId())
            .ownerUserId(entity.getOwnerUserId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .weightedAmount(entity.getWeightedAmount())
            .build();
    }
}
