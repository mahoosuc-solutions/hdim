package com.healthdata.approval.routing;

import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import com.healthdata.approval.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service for intelligent routing and auto-assignment of approval requests.
 *
 * Features:
 * - Routes approvals to appropriate reviewers based on request type and risk level
 * - Implements round-robin load balancing among eligible reviewers
 * - Handles auto-escalation when SLA is breached
 * - Supports role hierarchy for escalation paths
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalRoutingService {

    private final ApprovalRequestRepository requestRepository;
    private final ReviewerPoolService reviewerPoolService;

    @Value("${hdim.approval.auto-escalation-hours:4}")
    private int autoEscalationHours;

    @Value("${hdim.approval.routing.enabled:true}")
    private boolean routingEnabled;

    /**
     * Role hierarchy for escalation paths.
     * When a request is escalated, it moves up to the next role in the hierarchy.
     */
    private static final Map<String, String> ESCALATION_PATHS = Map.of(
        "CLINICAL_REVIEWER", "CLINICAL_SUPERVISOR",
        "CLINICAL_SUPERVISOR", "CLINICAL_DIRECTOR",
        "TECHNICAL_REVIEWER", "TECHNICAL_LEAD",
        "TECHNICAL_LEAD", "ADMIN",
        "ADMIN", "SUPER_ADMIN"
    );

    /**
     * Risk level to required role mapping.
     * Higher risk levels require higher-authority reviewers.
     */
    private static final Map<RiskLevel, String> RISK_LEVEL_ROLES = Map.of(
        RiskLevel.LOW, "TECHNICAL_REVIEWER",
        RiskLevel.MEDIUM, "CLINICAL_REVIEWER",
        RiskLevel.HIGH, "CLINICAL_SUPERVISOR",
        RiskLevel.CRITICAL, "CLINICAL_DIRECTOR"
    );

    /**
     * Request type specific role overrides.
     */
    private static final Map<RequestType, String> REQUEST_TYPE_ROLES = Map.of(
        RequestType.AGENT_ACTION, "CLINICAL_REVIEWER",
        RequestType.DATA_MUTATION, "CLINICAL_SUPERVISOR",
        RequestType.EXPORT, "CLINICAL_SUPERVISOR",
        RequestType.WORKFLOW_DEPLOY, "TECHNICAL_LEAD",
        RequestType.DLQ_REPROCESS, "TECHNICAL_REVIEWER",
        RequestType.GUARDRAIL_REVIEW, "CLINICAL_REVIEWER",
        RequestType.CONSENT_CHANGE, "CLINICAL_SUPERVISOR",
        RequestType.EMERGENCY_ACCESS, "CLINICAL_DIRECTOR"
    );

    /**
     * Determine the appropriate role for a request based on type and risk level.
     */
    public String determineRequiredRole(ApprovalRequest request) {
        // If role was explicitly set, use it
        if (request.getAssignedRole() != null && !request.getAssignedRole().isBlank()) {
            return request.getAssignedRole();
        }

        // Get role based on request type
        String typeRole = REQUEST_TYPE_ROLES.getOrDefault(request.getRequestType(), "CLINICAL_REVIEWER");

        // Get role based on risk level
        String riskRole = RISK_LEVEL_ROLES.getOrDefault(request.getRiskLevel(), "CLINICAL_REVIEWER");

        // Use the higher-authority role (whichever requires more clearance)
        return getHigherRole(typeRole, riskRole);
    }

    /**
     * Automatically assign a pending request to an available reviewer.
     * Uses round-robin load balancing among eligible reviewers.
     */
    @Transactional
    public Optional<String> autoAssign(ApprovalRequest request) {
        if (!routingEnabled) {
            log.debug("Auto-routing disabled, skipping auto-assign for request {}", request.getId());
            return Optional.empty();
        }

        String requiredRole = determineRequiredRole(request);
        log.debug("Looking for reviewer with role {} for request {}", requiredRole, request.getId());

        // Get available reviewers for this role and tenant
        List<String> eligibleReviewers = reviewerPoolService.getAvailableReviewers(
            request.getTenantId(), requiredRole);

        if (eligibleReviewers.isEmpty()) {
            log.warn("No available reviewers found with role {} for tenant {}",
                requiredRole, request.getTenantId());
            return Optional.empty();
        }

        // Select reviewer using round-robin
        String selectedReviewer = reviewerPoolService.selectNextReviewer(
            request.getTenantId(), requiredRole, eligibleReviewers);

        log.info("Auto-assigning request {} to reviewer {} (role: {})",
            request.getId(), selectedReviewer, requiredRole);

        // Update the request
        request.assign(selectedReviewer);
        request.setAssignedRole(requiredRole);
        requestRepository.save(request);

        return Optional.of(selectedReviewer);
    }

    /**
     * Get the escalation target role for a given current role.
     */
    public String getEscalationRole(String currentRole) {
        return ESCALATION_PATHS.getOrDefault(currentRole, "ADMIN");
    }

    /**
     * Scheduled job to auto-escalate requests that have been pending too long.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRateString = "${hdim.approval.escalation-check-interval-ms:300000}")
    @Transactional
    public void autoEscalateStaleRequests() {
        if (!routingEnabled) {
            return;
        }

        Instant escalationThreshold = Instant.now().minus(Duration.ofHours(autoEscalationHours));
        List<ApprovalRequest> staleRequests = requestRepository.findStaleAssignedRequests(escalationThreshold);

        for (ApprovalRequest request : staleRequests) {
            try {
                String currentRole = request.getAssignedRole();
                String escalationRole = getEscalationRole(currentRole);

                // Find a reviewer at the escalation level
                List<String> escalationReviewers = reviewerPoolService.getAvailableReviewers(
                    request.getTenantId(), escalationRole);

                if (!escalationReviewers.isEmpty()) {
                    String escalateTo = reviewerPoolService.selectNextReviewer(
                        request.getTenantId(), escalationRole, escalationReviewers);

                    request.escalate(escalateTo,
                        String.format("Auto-escalated after %d hours without response", autoEscalationHours));
                    request.setAssignedRole(escalationRole);
                    requestRepository.save(request);

                    log.info("Auto-escalated request {} from {} to {} (role: {} -> {})",
                        request.getId(), request.getAssignedTo(), escalateTo, currentRole, escalationRole);
                } else {
                    log.warn("No escalation reviewers available for role {} in tenant {}",
                        escalationRole, request.getTenantId());
                }
            } catch (Exception e) {
                log.error("Failed to auto-escalate request {}: {}", request.getId(), e.getMessage(), e);
            }
        }

        if (!staleRequests.isEmpty()) {
            log.info("Auto-escalation check complete: processed {} stale requests", staleRequests.size());
        }
    }

    /**
     * Compare two roles and return the one with higher authority.
     */
    private String getHigherRole(String role1, String role2) {
        List<String> hierarchy = List.of(
            "TECHNICAL_REVIEWER",
            "CLINICAL_REVIEWER",
            "TECHNICAL_LEAD",
            "CLINICAL_SUPERVISOR",
            "CLINICAL_DIRECTOR",
            "ADMIN",
            "SUPER_ADMIN"
        );

        int index1 = hierarchy.indexOf(role1);
        int index2 = hierarchy.indexOf(role2);

        // Default unknown roles to CLINICAL_REVIEWER level
        if (index1 < 0) index1 = 1;
        if (index2 < 0) index2 = 1;

        return index1 > index2 ? role1 : role2;
    }

    /**
     * Check if a user has sufficient role to approve a request.
     */
    public boolean canApprove(String userRole, String requiredRole) {
        List<String> hierarchy = List.of(
            "TECHNICAL_REVIEWER",
            "CLINICAL_REVIEWER",
            "TECHNICAL_LEAD",
            "CLINICAL_SUPERVISOR",
            "CLINICAL_DIRECTOR",
            "ADMIN",
            "SUPER_ADMIN"
        );

        int userIndex = hierarchy.indexOf(userRole);
        int requiredIndex = hierarchy.indexOf(requiredRole);

        if (userIndex < 0 || requiredIndex < 0) {
            return false;
        }

        return userIndex >= requiredIndex;
    }
}
