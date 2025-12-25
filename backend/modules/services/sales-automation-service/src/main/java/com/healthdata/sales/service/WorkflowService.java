package com.healthdata.sales.service;

import com.healthdata.sales.entity.*;
import com.healthdata.sales.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Workflow automation service for sales processes
 * Handles automated tasks, reminders, and stage-based actions
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowService {

    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;

    private static final int LEAD_FOLLOW_UP_DAYS = 2;
    private static final int OPPORTUNITY_STAGNANT_DAYS = 14;
    private static final int DEMO_FOLLOW_UP_DAYS = 1;

    // ==================== Automated Lead Workflows ====================

    /**
     * Auto-create follow-up task for new leads
     */
    @Transactional
    public void createLeadFollowUpTask(Lead lead) {
        Activity task = new Activity();
        task.setId(UUID.randomUUID());
        task.setTenantId(lead.getTenantId());
        task.setLeadId(lead.getId());
        task.setActivityType(ActivityType.TASK);
        task.setSubject("Follow up with new lead: " + lead.getFirstName() + " " + lead.getLastName());
        task.setDescription("Initial follow-up call/email for new lead from " + lead.getSource().name());
        task.setScheduledAt(LocalDateTime.now().plusDays(LEAD_FOLLOW_UP_DAYS));
        task.setCompleted(false);
        task.setAssignedToUserId(lead.getAssignedToUserId());

        activityRepository.save(task);
        log.info("Created auto follow-up task for lead {}", lead.getId());
    }

    /**
     * Auto-assign score to new lead based on criteria
     */
    @Transactional
    public int calculateLeadScore(Lead lead) {
        int score = 0;

        // Source scoring
        if (lead.getSource() != null) {
            switch (lead.getSource()) {
                case ROI_CALCULATOR -> score += 30;
                case DEMO_REQUEST -> score += 25;
                case REFERRAL -> score += 20;
                case CONFERENCE -> score += 15;
                case WEBSITE -> score += 10;
                default -> score += 5;
            }
        }

        // Company info scoring
        if (lead.getCompany() != null && !lead.getCompany().isEmpty()) {
            score += 10;
        }

        // Title scoring (decision makers)
        if (lead.getTitle() != null) {
            String title = lead.getTitle().toLowerCase();
            if (title.contains("ceo") || title.contains("cto") || title.contains("cio") ||
                title.contains("chief") || title.contains("president") || title.contains("vp") ||
                title.contains("director")) {
                score += 20;
            } else if (title.contains("manager")) {
                score += 10;
            }
        }

        // Organization type scoring (if converted to account)
        if (lead.getOrganizationType() != null) {
            switch (lead.getOrganizationType()) {
                case ACO -> score += 25;
                case HEALTH_SYSTEM -> score += 20;
                case PAYER -> score += 15;
                case HIE -> score += 15;
                case FQHC -> score += 10;
                default -> score += 5;
            }
        }

        // Cap at 100
        return Math.min(score, 100);
    }

    // ==================== Automated Opportunity Workflows ====================

    /**
     * Create follow-up task after demo
     */
    @Transactional
    public void createDemoFollowUp(Opportunity opportunity) {
        Activity task = new Activity();
        task.setId(UUID.randomUUID());
        task.setTenantId(opportunity.getTenantId());
        task.setOpportunityId(opportunity.getId());
        task.setAccountId(opportunity.getAccountId());
        task.setActivityType(ActivityType.TASK);
        task.setSubject("Demo follow-up: " + opportunity.getName());
        task.setDescription("Follow up after demo to gather feedback and discuss next steps");
        task.setScheduledAt(LocalDateTime.now().plusDays(DEMO_FOLLOW_UP_DAYS));
        task.setCompleted(false);
        task.setAssignedToUserId(opportunity.getOwnerUserId());

        activityRepository.save(task);
        log.info("Created demo follow-up task for opportunity {}", opportunity.getId());
    }

    /**
     * Create proposal review reminder
     */
    @Transactional
    public void createProposalReminder(Opportunity opportunity) {
        Activity task = new Activity();
        task.setId(UUID.randomUUID());
        task.setTenantId(opportunity.getTenantId());
        task.setOpportunityId(opportunity.getId());
        task.setAccountId(opportunity.getAccountId());
        task.setActivityType(ActivityType.TASK);
        task.setSubject("Proposal review: " + opportunity.getName());
        task.setDescription("Check in on proposal status and address any questions");
        task.setScheduledAt(LocalDateTime.now().plusDays(3));
        task.setCompleted(false);
        task.setAssignedToUserId(opportunity.getOwnerUserId());

        activityRepository.save(task);
        log.info("Created proposal reminder for opportunity {}", opportunity.getId());
    }

    /**
     * Handle stage transition workflow
     */
    @Transactional
    public void onStageTransition(Opportunity opportunity, OpportunityStage fromStage, OpportunityStage toStage) {
        log.info("Opportunity {} transitioned from {} to {}", opportunity.getId(), fromStage, toStage);

        // Auto-create tasks based on new stage
        switch (toStage) {
            case DEMO -> createDemoFollowUp(opportunity);
            case PROPOSAL -> createProposalReminder(opportunity);
            case NEGOTIATION -> createNegotiationTask(opportunity);
            case CONTRACT -> createContractTask(opportunity);
            case CLOSED_WON -> handleClosedWon(opportunity);
            case CLOSED_LOST -> handleClosedLost(opportunity);
            default -> {}
        }
    }

    private void createNegotiationTask(Opportunity opportunity) {
        Activity task = new Activity();
        task.setId(UUID.randomUUID());
        task.setTenantId(opportunity.getTenantId());
        task.setOpportunityId(opportunity.getId());
        task.setAccountId(opportunity.getAccountId());
        task.setActivityType(ActivityType.TASK);
        task.setSubject("Negotiation: " + opportunity.getName());
        task.setDescription("Review pricing and terms, prepare for contract discussions");
        task.setScheduledAt(LocalDateTime.now().plusDays(1));
        task.setCompleted(false);
        task.setAssignedToUserId(opportunity.getOwnerUserId());

        activityRepository.save(task);
    }

    private void createContractTask(Opportunity opportunity) {
        Activity task = new Activity();
        task.setId(UUID.randomUUID());
        task.setTenantId(opportunity.getTenantId());
        task.setOpportunityId(opportunity.getId());
        task.setAccountId(opportunity.getAccountId());
        task.setActivityType(ActivityType.TASK);
        task.setSubject("Contract review: " + opportunity.getName());
        task.setDescription("Finalize contract terms and send for signature");
        task.setScheduledAt(LocalDateTime.now().plusDays(1));
        task.setCompleted(false);
        task.setAssignedToUserId(opportunity.getOwnerUserId());

        activityRepository.save(task);
    }

    private void handleClosedWon(Opportunity opportunity) {
        // Update account stage to customer
        if (opportunity.getAccountId() != null) {
            accountRepository.findById(opportunity.getAccountId()).ifPresent(account -> {
                account.setStage(AccountStage.CUSTOMER);
                accountRepository.save(account);
                log.info("Updated account {} to CUSTOMER stage", account.getId());
            });
        }

        // Create onboarding task
        Activity task = new Activity();
        task.setId(UUID.randomUUID());
        task.setTenantId(opportunity.getTenantId());
        task.setOpportunityId(opportunity.getId());
        task.setAccountId(opportunity.getAccountId());
        task.setActivityType(ActivityType.TASK);
        task.setSubject("Onboarding kickoff: " + opportunity.getName());
        task.setDescription("Schedule onboarding kickoff meeting with new customer");
        task.setScheduledAt(LocalDateTime.now().plusDays(1));
        task.setCompleted(false);
        task.setAssignedToUserId(opportunity.getOwnerUserId());

        activityRepository.save(task);
        log.info("Created onboarding task for closed won opportunity {}", opportunity.getId());
    }

    private void handleClosedLost(Opportunity opportunity) {
        // Create post-mortem task
        Activity task = new Activity();
        task.setId(UUID.randomUUID());
        task.setTenantId(opportunity.getTenantId());
        task.setOpportunityId(opportunity.getId());
        task.setAccountId(opportunity.getAccountId());
        task.setActivityType(ActivityType.TASK);
        task.setSubject("Loss analysis: " + opportunity.getName());
        task.setDescription("Document reasons for loss: " +
            (opportunity.getLostReason() != null ? opportunity.getLostReason().name() : "Not specified"));
        task.setScheduledAt(LocalDateTime.now().plusDays(7));
        task.setCompleted(false);
        task.setAssignedToUserId(opportunity.getOwnerUserId());

        activityRepository.save(task);
        log.info("Created loss analysis task for closed lost opportunity {}", opportunity.getId());
    }

    // ==================== Scheduled Workflows ====================

    /**
     * Daily check for stagnant opportunities
     * Creates reminder tasks for opportunities with no activity
     */
    @Scheduled(cron = "0 0 8 * * MON-FRI")  // 8 AM weekdays
    @Transactional
    public void checkStagnantOpportunities() {
        log.info("Running stagnant opportunity check");
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(OPPORTUNITY_STAGNANT_DAYS);

        // Get all open opportunities
        List<Opportunity> openOpportunities = opportunityRepository
            .findOpenOpportunities(null, org.springframework.data.domain.Pageable.unpaged())
            .getContent();

        for (Opportunity opp : openOpportunities) {
            List<Activity> activities = activityRepository.findByOpportunityId(opp.getId());
            LocalDateTime lastActivity = activities.stream()
                .map(Activity::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(opp.getCreatedAt());

            if (lastActivity.isBefore(cutoffDate)) {
                // Check if reminder already exists
                boolean hasRecentReminder = activities.stream()
                    .filter(a -> a.getSubject() != null && a.getSubject().contains("Stagnant"))
                    .anyMatch(a -> a.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)));

                if (!hasRecentReminder) {
                    createStagnantReminder(opp, (int) ChronoUnit.DAYS.between(lastActivity.toLocalDate(),
                        LocalDateTime.now().toLocalDate()));
                }
            }
        }
    }

    private void createStagnantReminder(Opportunity opportunity, int daysSinceActivity) {
        Activity task = new Activity();
        task.setId(UUID.randomUUID());
        task.setTenantId(opportunity.getTenantId());
        task.setOpportunityId(opportunity.getId());
        task.setAccountId(opportunity.getAccountId());
        task.setActivityType(ActivityType.TASK);
        task.setSubject("Stagnant deal alert: " + opportunity.getName());
        task.setDescription("No activity for " + daysSinceActivity + " days. Review and take action.");
        task.setScheduledAt(LocalDateTime.now());
        task.setCompleted(false);
        task.setAssignedToUserId(opportunity.getOwnerUserId());

        activityRepository.save(task);
        log.info("Created stagnant reminder for opportunity {} ({} days inactive)",
            opportunity.getId(), daysSinceActivity);
    }

    /**
     * Daily check for overdue close dates
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI")  // 9 AM weekdays
    @Transactional
    public void checkOverdueCloseDates() {
        log.info("Running overdue close date check");

        List<Opportunity> openOpportunities = opportunityRepository
            .findOpenOpportunities(null, org.springframework.data.domain.Pageable.unpaged())
            .getContent();

        for (Opportunity opp : openOpportunities) {
            if (opp.getExpectedCloseDate() != null &&
                opp.getExpectedCloseDate().isBefore(LocalDateTime.now().toLocalDate())) {

                // Check if reminder already exists
                List<Activity> activities = activityRepository.findByOpportunityId(opp.getId());
                boolean hasRecentReminder = activities.stream()
                    .filter(a -> a.getSubject() != null && a.getSubject().contains("Overdue"))
                    .anyMatch(a -> a.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)));

                if (!hasRecentReminder) {
                    createOverdueReminder(opp);
                }
            }
        }
    }

    private void createOverdueReminder(Opportunity opportunity) {
        int daysOverdue = (int) ChronoUnit.DAYS.between(
            opportunity.getExpectedCloseDate(),
            LocalDateTime.now().toLocalDate());

        Activity task = new Activity();
        task.setId(UUID.randomUUID());
        task.setTenantId(opportunity.getTenantId());
        task.setOpportunityId(opportunity.getId());
        task.setAccountId(opportunity.getAccountId());
        task.setActivityType(ActivityType.TASK);
        task.setSubject("Overdue close date: " + opportunity.getName());
        task.setDescription("Expected close date was " + daysOverdue + " days ago. Update close date or close deal.");
        task.setScheduledAt(LocalDateTime.now());
        task.setCompleted(false);
        task.setAssignedToUserId(opportunity.getOwnerUserId());

        activityRepository.save(task);
        log.info("Created overdue reminder for opportunity {} ({} days overdue)",
            opportunity.getId(), daysOverdue);
    }

    // ==================== Lead Assignment ====================

    /**
     * Round-robin lead assignment (placeholder for more sophisticated assignment)
     */
    @Transactional
    public UUID assignLead(UUID tenantId, Lead lead) {
        // For now, return null to indicate manual assignment needed
        // In production, this would implement round-robin or territory-based assignment
        log.info("Lead {} requires manual assignment", lead.getId());
        return null;
    }
}
