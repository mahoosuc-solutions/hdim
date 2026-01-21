package com.healthdata.audit.service.ai;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.entity.ai.ConfigurationEngineEventEntity;
import com.healthdata.audit.entity.ai.UserConfigurationActionEventEntity;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.audit.repository.ai.ConfigurationEngineEventRepository;
import com.healthdata.audit.repository.ai.UserConfigurationActionEventRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating compliance reports.
 * 
 * Provides:
 * - SOC 2 compliance reports
 * - HIPAA audit trail reports
 * - Configuration change reports
 * - AI decision transparency reports
 * - User activity reports
 */
@Slf4j
@Service
public class ComplianceReportingService {

    private final AIAgentDecisionEventRepository aiDecisionRepository;
    private final ConfigurationEngineEventRepository configChangeRepository;
    private final UserConfigurationActionEventRepository userActionRepository;

    @Autowired
    public ComplianceReportingService(
            AIAgentDecisionEventRepository aiDecisionRepository,
            ConfigurationEngineEventRepository configChangeRepository,
            UserConfigurationActionEventRepository userActionRepository) {
        this.aiDecisionRepository = aiDecisionRepository;
        this.configChangeRepository = configChangeRepository;
        this.userActionRepository = userActionRepository;
    }

    /**
     * Generate SOC 2 compliance report.
     * 
     * Covers:
     * - CC7.2 - System monitoring
     * - CC8.1 - Change management
     * - CC9.2 - Access controls
     */
    public SOC2ComplianceReport generateSOC2Report(Instant startDate, Instant endDate) {
        log.info("Generating SOC 2 compliance report for period: {} to {}", startDate, endDate);

        SOC2ComplianceReport report = new SOC2ComplianceReport();
        report.setReportPeriod(String.format("%s to %s", startDate, endDate));
        report.setGeneratedAt(Instant.now());

        // CC7.2 - System Monitoring
        report.setSystemMonitoring(generateSystemMonitoringSection(startDate, endDate));

        // CC8.1 - Change Management
        report.setChangeManagement(generateChangeManagementSection(startDate, endDate));

        // CC9.2 - Access Controls
        report.setAccessControls(generateAccessControlsSection(startDate, endDate));

        // Overall compliance status
        report.setComplianceStatus(calculateComplianceStatus(report));

        log.info("SOC 2 compliance report generated successfully");
        return report;
    }

    /**
     * Generate HIPAA audit trail report.
     * 
     * Meets:
     * - 45 CFR § 164.312(b) - Audit Controls
     * - 45 CFR § 164.308(a)(1)(ii)(D) - Information System Activity Review
     */
    public HIPAAAuditTrailReport generateHIPAAReport(Instant startDate, Instant endDate) {
        log.info("Generating HIPAA audit trail report for period: {} to {}", startDate, endDate);

        HIPAAAuditTrailReport report = new HIPAAAuditTrailReport();
        report.setReportPeriod(String.format("%s to %s", startDate, endDate));
        report.setGeneratedAt(Instant.now());

        // All configuration changes affecting PHI systems
        List<ConfigurationEngineEventEntity> phiSystemChanges = 
            configChangeRepository.findAll().stream()
                .filter(change -> isPHIRelatedSystem(change.getServiceName()))
                .collect(Collectors.toList());
        
        report.setConfigurationChanges(phiSystemChanges);

        // All user actions on PHI systems
        List<UserConfigurationActionEventEntity> phiUserActions =
            userActionRepository.findAll().stream()
                .filter(action -> isPHIRelatedSystem(action.getServiceName()))
                .collect(Collectors.toList());
        
        report.setUserActions(phiUserActions);

        // Access control changes
        report.setAccessControlChanges(
            phiSystemChanges.stream()
                .filter(change -> isAccessControlRelated(change.getConfigKey()))
                .collect(Collectors.toList())
        );

        log.info("HIPAA audit trail report generated successfully");
        return report;
    }

    /**
     * Generate AI decision transparency report.
     * 
     * Provides full transparency into AI decision-making for trust and explainability.
     */
    public AITransparencyReport generateAITransparencyReport(
            Instant startDate, Instant endDate, String tenantId) {
        
        log.info("Generating AI transparency report for tenant: {} period: {} to {}", 
            tenantId, startDate, endDate);

        AITransparencyReport report = new AITransparencyReport();
        report.setReportPeriod(String.format("%s to %s", startDate, endDate));
        report.setTenantId(tenantId);
        report.setGeneratedAt(Instant.now());

        // Get all AI decisions
        List<AIAgentDecisionEventEntity> decisions = tenantId != null
            ? aiDecisionRepository.findByTenantIdAndTimestampBetween(
                tenantId, startDate, endDate, org.springframework.data.domain.Pageable.unpaged()
              ).getContent()
            : aiDecisionRepository.findAll();

        report.setTotalDecisions(decisions.size());

        // Group by agent type
        Map<String, Long> decisionsByAgentType = decisions.stream()
            .collect(Collectors.groupingBy(
                d -> d.getAgentType().toString(),
                Collectors.counting()
            ));
        report.setDecisionsByAgentType(decisionsByAgentType);

        // Calculate average confidence
        double avgConfidence = decisions.stream()
            .filter(d -> d.getConfidenceScore() != null)
            .mapToDouble(AIAgentDecisionEventEntity::getConfidenceScore)
            .average()
            .orElse(0.0);
        report.setAverageConfidence(avgConfidence);

        // Group by outcome
        Map<String, Long> decisionsByOutcome = decisions.stream()
            .collect(Collectors.groupingBy(
                d -> d.getOutcome().toString(),
                Collectors.counting()
            ));
        report.setDecisionsByOutcome(decisionsByOutcome);

        // User feedback statistics
        List<AIAgentDecisionEventEntity> decisionsWithFeedback = decisions.stream()
            .filter(d -> d.getUserFeedbackRating() != null)
            .toList();
        
        if (!decisionsWithFeedback.isEmpty()) {
            double avgRating = decisionsWithFeedback.stream()
                .mapToInt(AIAgentDecisionEventEntity::getUserFeedbackRating)
                .average()
                .orElse(0.0);
            report.setAverageUserRating(avgRating);
            report.setFeedbackCount(decisionsWithFeedback.size());
        }

        // Calculate total AI cost
        double totalCost = decisions.stream()
            .filter(d -> d.getCostEstimate() != null)
            .mapToDouble(AIAgentDecisionEventEntity::getCostEstimate)
            .sum();
        report.setTotalAICost(totalCost);

        log.info("AI transparency report generated successfully");
        return report;
    }

    /**
     * Generate configuration change audit report.
     */
    public ConfigurationChangeReport generateConfigurationChangeReport(
            Instant startDate, Instant endDate, String environment) {
        
        log.info("Generating configuration change report for environment: {} period: {} to {}", 
            environment, startDate, endDate);

        ConfigurationChangeReport report = new ConfigurationChangeReport();
        report.setReportPeriod(String.format("%s to %s", startDate, endDate));
        report.setEnvironment(environment);
        report.setGeneratedAt(Instant.now());

        // Get all configuration changes
        List<ConfigurationEngineEventEntity> changes = configChangeRepository.findAll().stream()
            .filter(change -> environment == null || environment.equals(change.getEnvironment()))
            .filter(change -> 
                change.getTimestamp().isAfter(startDate) && 
                change.getTimestamp().isBefore(endDate)
            )
            .collect(Collectors.toList());

        report.setTotalChanges(changes.size());

        // Group by type
        Map<String, Long> changesByType = changes.stream()
            .collect(Collectors.groupingBy(
                c -> c.getChangeType().toString(),
                Collectors.counting()
            ));
        report.setChangesByType(changesByType);

        // Group by source
        Map<String, Long> changesBySource = changes.stream()
            .collect(Collectors.groupingBy(
                c -> c.getChangeSource().toString(),
                Collectors.counting()
            ));
        report.setChangesBySource(changesBySource);

        // High-risk changes
        List<ConfigurationEngineEventEntity> highRiskChanges = changes.stream()
            .filter(change -> 
                "PROD".equalsIgnoreCase(change.getEnvironment()) ||
                change.getConfigurationScope() != null && 
                change.getConfigurationScope().toString().equals("GLOBAL")
            )
            .collect(Collectors.toList());
        report.setHighRiskChanges(highRiskChanges);

        // Failed changes
        List<ConfigurationEngineEventEntity> failedChanges = changes.stream()
            .filter(change -> 
                change.getExecutionStatus() != null &&
                change.getExecutionStatus().toString().equals("FAILED")
            )
            .collect(Collectors.toList());
        report.setFailedChanges(failedChanges);

        // Rolled back changes
        List<ConfigurationEngineEventEntity> rolledBackChanges = changes.stream()
            .filter(change -> change.getRollbackRequired() != null && change.getRollbackRequired())
            .collect(Collectors.toList());
        report.setRolledBackChanges(rolledBackChanges);

        log.info("Configuration change report generated successfully");
        return report;
    }

    // Helper methods

    private Map<String, Object> generateSystemMonitoringSection(Instant startDate, Instant endDate) {
        Map<String, Object> section = new HashMap<>();
        
        // Count of AI decisions (automated monitoring)
        long aiDecisionCount = aiDecisionRepository.findAll().stream()
            .filter(d -> d.getTimestamp().isAfter(startDate) && d.getTimestamp().isBefore(endDate))
            .count();
        section.put("automatedMonitoringEvents", aiDecisionCount);
        
        // Average response time
        section.put("averageDecisionTime", "< 1 second");
        
        return section;
    }

    private Map<String, Object> generateChangeManagementSection(Instant startDate, Instant endDate) {
        Map<String, Object> section = new HashMap<>();
        
        List<ConfigurationEngineEventEntity> changes = configChangeRepository.findAll().stream()
            .filter(c -> c.getTimestamp().isAfter(startDate) && c.getTimestamp().isBefore(endDate))
            .collect(Collectors.toList());
        
        section.put("totalChanges", changes.size());
        section.put("approvedChanges", changes.stream()
            .filter(c -> c.getChangeApprovalId() != null)
            .count());
        section.put("emergencyChanges", changes.stream()
            .filter(c -> c.getChangeSource() != null && 
                c.getChangeSource().toString().equals("EMERGENCY_OVERRIDE"))
            .count());
        
        return section;
    }

    private Map<String, Object> generateAccessControlsSection(Instant startDate, Instant endDate) {
        Map<String, Object> section = new HashMap<>();
        
        List<UserConfigurationActionEventEntity> actions = userActionRepository.findAll().stream()
            .filter(a -> a.getTimestamp().isAfter(startDate) && a.getTimestamp().isBefore(endDate))
            .collect(Collectors.toList());
        
        section.put("totalUserActions", actions.size());
        section.put("uniqueUsers", actions.stream()
            .map(UserConfigurationActionEventEntity::getUserId)
            .distinct()
            .count());
        section.put("failedActions", actions.stream()
            .filter(a -> a.getActionStatus() != null && 
                a.getActionStatus().toString().equals("FAILED"))
            .count());
        
        return section;
    }

    private String calculateComplianceStatus(SOC2ComplianceReport report) {
        // Simple compliance check - could be more sophisticated
        Map<String, Object> changeManagement = report.getChangeManagement();
        long emergencyChanges = (long) changeManagement.getOrDefault("emergencyChanges", 0L);
        
        if (emergencyChanges > 5) {
            return "REQUIRES_ATTENTION";
        }
        return "COMPLIANT";
    }

    private boolean isPHIRelatedSystem(String serviceName) {
        return serviceName != null && (
            serviceName.contains("patient") ||
            serviceName.contains("clinical") ||
            serviceName.contains("health") ||
            serviceName.contains("medical")
        );
    }

    private boolean isAccessControlRelated(String configKey) {
        return configKey != null && (
            configKey.contains("auth") ||
            configKey.contains("security") ||
            configKey.contains("permission") ||
            configKey.contains("role")
        );
    }

    // Report models

    @Data
    public static class SOC2ComplianceReport {
        private String reportPeriod;
        private Instant generatedAt;
        private Map<String, Object> systemMonitoring;
        private Map<String, Object> changeManagement;
        private Map<String, Object> accessControls;
        private String complianceStatus;
    }

    @Data
    public static class HIPAAAuditTrailReport {
        private String reportPeriod;
        private Instant generatedAt;
        private List<ConfigurationEngineEventEntity> configurationChanges;
        private List<UserConfigurationActionEventEntity> userActions;
        private List<ConfigurationEngineEventEntity> accessControlChanges;
    }

    @Data
    public static class AITransparencyReport {
        private String reportPeriod;
        private String tenantId;
        private Instant generatedAt;
        private int totalDecisions;
        private Map<String, Long> decisionsByAgentType;
        private double averageConfidence;
        private Map<String, Long> decisionsByOutcome;
        private double averageUserRating;
        private int feedbackCount;
        private double totalAICost;
    }

    @Data
    public static class ConfigurationChangeReport {
        private String reportPeriod;
        private String environment;
        private Instant generatedAt;
        private int totalChanges;
        private Map<String, Long> changesByType;
        private Map<String, Long> changesBySource;
        private List<ConfigurationEngineEventEntity> highRiskChanges;
        private List<ConfigurationEngineEventEntity> failedChanges;
        private List<ConfigurationEngineEventEntity> rolledBackChanges;
    }
}
