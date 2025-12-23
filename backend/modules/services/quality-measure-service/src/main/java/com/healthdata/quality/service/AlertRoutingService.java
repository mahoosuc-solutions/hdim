package com.healthdata.quality.service;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.persistence.AlertRoutingConfigEntity;
import com.healthdata.quality.persistence.AlertRoutingConfigRepository;
import com.healthdata.quality.persistence.CareTeamAssignmentEntity;
import com.healthdata.quality.persistence.CareTeamAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Alert Routing Service (Phase 5 - Database-Driven)
 *
 * Determines which care team members should receive clinical alerts
 * based on database-driven routing configuration:
 * - Tenant-specific routing rules
 * - Patient-specific care team assignments
 * - Business hours vs after-hours routing
 * - Escalation path configuration
 *
 * Routing Logic:
 * 1. Load tenant-specific routing configuration from database
 * 2. Load patient's care team assignments
 * 3. Apply time-based routing rules (business hours vs after-hours)
 * 4. Map roles to actual providers
 * 5. Support escalation path configuration
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertRoutingService {

    private final AlertRoutingConfigRepository routingConfigRepository;
    private final CareTeamAssignmentRepository careTeamRepository;

    // Default recipient roles (fallback when no database configuration exists)
    private static final String ON_CALL_PSYCHIATRIST = "on-call-psychiatrist";
    private static final String ON_CALL_PROVIDER = "on-call-provider";
    private static final String CARE_TEAM_LEAD = "care-team-lead";
    private static final String CARE_COORDINATOR = "care-coordinator";
    private static final String PRIMARY_CARE_PROVIDER = "primary-care-provider";
    private static final String ORDERING_PROVIDER = "ordering-provider";

    // Business hours configuration (8 AM - 6 PM)
    private static final LocalTime BUSINESS_HOURS_START = LocalTime.of(8, 0);
    private static final LocalTime BUSINESS_HOURS_END = LocalTime.of(18, 0);

    /**
     * Determine which care team members should receive this alert
     * Uses database-driven routing configuration with fallback to hardcoded rules
     *
     * @param tenantId Tenant ID for multi-tenant routing configuration
     * @param alert Clinical alert to route
     * @return List of recipient IDs/roles
     */
    public List<String> determineRecipients(String tenantId, ClinicalAlertDTO alert) {
        log.debug("Determining recipients for alert {} (type: {}, severity: {})",
            alert.getId(), alert.getAlertType(), alert.getSeverity());

        // Try database-driven routing first
        List<String> recipients = determineRecipientsFromDatabase(tenantId, alert);

        // If no database configuration, fall back to hardcoded routing
        if (recipients.isEmpty()) {
            log.debug("No database routing configuration found for alert type: {}, using hardcoded routing",
                alert.getAlertType());
            recipients = determineRecipientsHardcoded(tenantId, alert);
        }

        log.info("Alert {} routed to {} recipients: {}",
            alert.getId(), recipients.size(), recipients);

        return recipients;
    }

    /**
     * Determine recipients using database routing configuration
     */
    private List<String> determineRecipientsFromDatabase(String tenantId, ClinicalAlertDTO alert) {
        String alertType = alert.getAlertType();
        String severity = alert.getSeverity();

        // Determine if it's business hours or after-hours
        boolean isBusinessHours = isBusinessHours();

        // Load routing configuration from database
        List<AlertRoutingConfigEntity> routingConfigs = isBusinessHours
            ? routingConfigRepository.findBusinessHoursRoutingConfigs(tenantId, alertType, severity, true)
            : routingConfigRepository.findAfterHoursRoutingConfigs(tenantId, alertType, severity, true);

        // If no time-specific configuration, use general configuration
        if (routingConfigs.isEmpty()) {
            routingConfigs = routingConfigRepository.findRoutingConfigsForAlertAndSeverity(
                tenantId, alertType, severity, true
            );
        }

        if (routingConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract roles from routing configs
        List<String> roles = routingConfigs.stream()
            .map(AlertRoutingConfigEntity::getRecipientRole)
            .collect(Collectors.toList());

        // Map roles to actual provider IDs from care team assignments
        return mapRolesToProviders(tenantId, alert.getPatientId(), roles);
    }

    /**
     * Map role names to actual provider IDs from patient's care team
     */
    private List<String> mapRolesToProviders(String tenantId, UUID patientId, List<String> roles) {
        Set<String> providerIds = new LinkedHashSet<>();
        LocalDate today = LocalDate.now();

        for (String role : roles) {
            // Find active care team assignments for this role
            List<CareTeamAssignmentEntity> assignments = careTeamRepository
                .findActiveAssignmentsByRoleOnDate(tenantId, patientId, role, today, true);

            if (!assignments.isEmpty()) {
                // Add actual provider IDs
                assignments.stream()
                    .map(CareTeamAssignmentEntity::getProviderId)
                    .forEach(providerIds::add);
            } else {
                // No specific provider assigned, use role name as fallback
                providerIds.add(role);
            }
        }

        return new ArrayList<>(providerIds);
    }

    /**
     * Determine recipients for business hours
     */
    public List<String> determineRecipientsForBusinessHours(String tenantId, ClinicalAlertDTO alert) {
        List<AlertRoutingConfigEntity> configs = routingConfigRepository
            .findBusinessHoursRoutingConfigs(tenantId, alert.getAlertType(), alert.getSeverity(), true);

        List<String> roles = configs.stream()
            .map(AlertRoutingConfigEntity::getRecipientRole)
            .collect(Collectors.toList());

        return mapRolesToProviders(tenantId, alert.getPatientId(), roles);
    }

    /**
     * Determine recipients for after-hours
     */
    public List<String> determineRecipientsForAfterHours(String tenantId, ClinicalAlertDTO alert) {
        List<AlertRoutingConfigEntity> configs = routingConfigRepository
            .findAfterHoursRoutingConfigs(tenantId, alert.getAlertType(), alert.getSeverity(), true);

        List<String> roles = configs.stream()
            .map(AlertRoutingConfigEntity::getRecipientRole)
            .collect(Collectors.toList());

        return mapRolesToProviders(tenantId, alert.getPatientId(), roles);
    }

    /**
     * Get escalation recipients after specified minutes
     */
    public List<String> getEscalationRecipientsAfterMinutes(String tenantId, ClinicalAlertDTO alert, int minutes) {
        log.debug("Determining escalation recipients for alert {} after {} minutes", alert.getId(), minutes);

        List<AlertRoutingConfigEntity> escalationConfigs = routingConfigRepository
            .findEscalationRoutingConfigs(tenantId, alert.getAlertType(), true);

        // Filter configs where escalation threshold has been reached
        List<String> roles = escalationConfigs.stream()
            .filter(config -> config.getEscalationThresholdMinutes() != null
                && config.getEscalationThresholdMinutes() <= minutes)
            .map(AlertRoutingConfigEntity::getRecipientRole)
            .collect(Collectors.toList());

        return mapRolesToProviders(tenantId, alert.getPatientId(), roles);
    }

    /**
     * Check if current time is during business hours
     */
    private boolean isBusinessHours() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(BUSINESS_HOURS_START) && !now.isAfter(BUSINESS_HOURS_END);
    }

    /**
     * Fallback: Determine recipients using hardcoded routing logic
     */
    private List<String> determineRecipientsHardcoded(String tenantId, ClinicalAlertDTO alert) {
        Set<String> recipients = new LinkedHashSet<>();
        String alertType = alert.getAlertType();
        String severity = alert.getSeverity();

        switch (alertType) {
            case "MENTAL_HEALTH_CRISIS":
                recipients.addAll(routeMentalHealthCrisis(severity));
                break;

            case "CRITICAL_LAB":
                recipients.addAll(routeCriticalLab(severity));
                break;

            case "RISK_ESCALATION":
                recipients.addAll(routeRiskEscalation(severity));
                break;

            case "HEALTH_DECLINE":
                recipients.addAll(routeHealthDecline(severity));
                break;

            case "CARE_GAP_OVERDUE":
                recipients.addAll(routeCareGapOverdue(severity));
                break;

            case "CHRONIC_DETERIORATION":
                recipients.addAll(routeChronicDeterioration(severity));
                break;

            default:
                recipients.addAll(getDefaultRecipients());
                log.warn("Unknown alert type: {} - using default recipients", alertType);
        }

        return new ArrayList<>(recipients);
    }

    /**
     * Route mental health crisis alerts
     * CRITICAL severity requires immediate psychiatric intervention
     */
    private List<String> routeMentalHealthCrisis(String severity) {
        List<String> recipients = new ArrayList<>();

        if ("CRITICAL".equals(severity)) {
            // Suicide risk or severe crisis - escalate to on-call psychiatrist
            recipients.add(ON_CALL_PSYCHIATRIST);
            recipients.add(CARE_TEAM_LEAD);
            recipients.add(PRIMARY_CARE_PROVIDER);
        } else if ("HIGH".equals(severity)) {
            // Severe symptoms - care team and primary provider
            recipients.add(CARE_TEAM_LEAD);
            recipients.add(PRIMARY_CARE_PROVIDER);
        } else {
            // MEDIUM/LOW - primary provider
            recipients.add(PRIMARY_CARE_PROVIDER);
        }

        return recipients;
    }

    /**
     * Route critical lab result alerts
     */
    private List<String> routeCriticalLab(String severity) {
        List<String> recipients = new ArrayList<>();

        if ("CRITICAL".equals(severity)) {
            // Critical lab value - immediate attention from ordering provider
            recipients.add(ORDERING_PROVIDER);
            recipients.add(CARE_TEAM_LEAD);
        } else {
            // Abnormal but not critical
            recipients.add(ORDERING_PROVIDER);
        }

        return recipients;
    }

    /**
     * Route risk escalation alerts
     */
    private List<String> routeRiskEscalation(String severity) {
        List<String> recipients = new ArrayList<>();

        if ("CRITICAL".equals(severity) || "HIGH".equals(severity)) {
            // High/Very high risk - care coordinator for care plan updates
            recipients.add(CARE_COORDINATOR);
            recipients.add(PRIMARY_CARE_PROVIDER);
        } else {
            // Medium/low risk - primary provider awareness
            recipients.add(PRIMARY_CARE_PROVIDER);
        }

        return recipients;
    }

    /**
     * Route health score decline alerts
     */
    private List<String> routeHealthDecline(String severity) {
        List<String> recipients = new ArrayList<>();

        if ("CRITICAL".equals(severity) || "HIGH".equals(severity)) {
            // Significant decline - care coordinator and primary provider
            recipients.add(CARE_COORDINATOR);
            recipients.add(PRIMARY_CARE_PROVIDER);
        } else {
            // MEDIUM/LOW decline - primary provider monitoring
            recipients.add(PRIMARY_CARE_PROVIDER);
        }

        return recipients;
    }

    /**
     * Route care gap overdue alerts
     */
    private List<String> routeCareGapOverdue(String severity) {
        List<String> recipients = new ArrayList<>();

        if ("CRITICAL".equals(severity) || "HIGH".equals(severity)) {
            // High priority gap - care coordinator for outreach
            recipients.add(CARE_COORDINATOR);
            recipients.add(PRIMARY_CARE_PROVIDER);
        } else {
            // Medium/low priority gap - primary provider
            recipients.add(PRIMARY_CARE_PROVIDER);
        }

        return recipients;
    }

    /**
     * Route chronic disease deterioration alerts
     */
    private List<String> routeChronicDeterioration(String severity) {
        List<String> recipients = new ArrayList<>();

        if ("CRITICAL".equals(severity)) {
            // Severe deterioration - immediate attention
            recipients.add(ON_CALL_PROVIDER);
            recipients.add(CARE_TEAM_LEAD);
            recipients.add(PRIMARY_CARE_PROVIDER);
        } else if ("HIGH".equals(severity)) {
            // Significant deterioration - care team
            recipients.add(CARE_COORDINATOR);
            recipients.add(PRIMARY_CARE_PROVIDER);
        } else {
            // MEDIUM/LOW - primary provider monitoring
            recipients.add(PRIMARY_CARE_PROVIDER);
        }

        return recipients;
    }

    /**
     * Get default recipients when no specific routing applies
     */
    private List<String> getDefaultRecipients() {
        return List.of(PRIMARY_CARE_PROVIDER);
    }

    /**
     * Get recipients for escalated alerts (unacknowledged after threshold)
     * Uses database-driven escalation path configuration
     */
    public List<String> getEscalationRecipients(String tenantId, ClinicalAlertDTO alert) {
        log.warn("Alert {} being escalated - determining escalation recipients", alert.getId());

        Set<String> recipients = new LinkedHashSet<>();

        // Get original recipients
        recipients.addAll(determineRecipients(tenantId, alert));

        // Try to get database-configured escalation recipients
        // Assume 30 minutes for default escalation
        List<String> escalationRecipients = getEscalationRecipientsAfterMinutes(tenantId, alert, 30);

        if (!escalationRecipients.isEmpty()) {
            recipients.addAll(escalationRecipients);
        } else {
            // Fallback to hardcoded escalation logic
            if ("CRITICAL".equals(alert.getSeverity())) {
                recipients.add(ON_CALL_PROVIDER);
                recipients.add(CARE_TEAM_LEAD);
            } else if ("HIGH".equals(alert.getSeverity())) {
                recipients.add(CARE_COORDINATOR);
                recipients.add(CARE_TEAM_LEAD);
            }
        }

        return new ArrayList<>(recipients);
    }
}
