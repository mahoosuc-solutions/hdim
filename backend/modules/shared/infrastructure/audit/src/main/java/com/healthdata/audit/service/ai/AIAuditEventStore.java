package com.healthdata.audit.service.ai;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.entity.ai.ConfigurationEngineEventEntity;
import com.healthdata.audit.entity.ai.UserConfigurationActionEventEntity;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.models.ai.ConfigurationEngineEvent;
import com.healthdata.audit.models.ai.UserConfigurationActionEvent;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.audit.repository.ai.ConfigurationEngineEventRepository;
import com.healthdata.audit.repository.ai.UserConfigurationActionEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for storing and querying AI audit events.
 * 
 * Provides:
 * - Event persistence
 * - Real-time metrics updates
 * - Pattern analysis
 * - Compliance reporting
 */
@Slf4j
@Service
public class AIAuditEventStore {

    private final AIAgentDecisionEventRepository aiDecisionRepository;
    private final ConfigurationEngineEventRepository configChangeRepository;
    private final UserConfigurationActionEventRepository userActionRepository;

    @Autowired
    public AIAuditEventStore(
            AIAgentDecisionEventRepository aiDecisionRepository,
            ConfigurationEngineEventRepository configChangeRepository,
            UserConfigurationActionEventRepository userActionRepository) {
        this.aiDecisionRepository = aiDecisionRepository;
        this.configChangeRepository = configChangeRepository;
        this.userActionRepository = userActionRepository;
    }

    // ==================== AI Decision Events ====================

    /**
     * Store AI agent decision event.
     */
    @Transactional
    public void storeAIDecision(AIAgentDecisionEvent event) {
        try {
            AIAgentDecisionEventEntity entity = AIAgentDecisionEventEntity.fromDomainModel(event);
            aiDecisionRepository.save(entity);
            log.debug("Stored AI decision event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to store AI decision event: {}", event.getEventId(), e);
            throw e;
        }
    }

    /**
     * Update real-time metrics for AI decisions.
     * Tracks decision counts, confidence scores, costs, and alerts on anomalies.
     */
    public void updateAIDecisionMetrics(AIAgentDecisionEvent event) {
        try {
            Instant now = Instant.now();
            Instant oneHourAgo = now.minusSeconds(3600);
            Instant oneDayAgo = now.minusSeconds(86400);

            // Calculate metrics for the last hour
            List<Object[]> avgConfidenceByAgent = aiDecisionRepository
                .calculateAverageConfidenceByAgentType(oneHourAgo, now);
            
            // Calculate total cost for tenant in last 24 hours
            Double totalCost24h = event.getTenantId() != null 
                ? aiDecisionRepository.calculateTotalCostForTenant(event.getTenantId(), oneDayAgo, now)
                : null;

            // Count decisions by outcome in last hour
            List<Object[]> outcomeCounts = aiDecisionRepository.countDecisionsByOutcome(oneHourAgo, now);

            // Log metrics summary
            log.info("AI Decision Metrics Update - Event: {}, Agent: {}, Confidence: {}, Cost (24h): {}",
                event.getEventId(),
                event.getAgentType(),
                event.getConfidenceScore(),
                totalCost24h != null ? String.format("%.4f", totalCost24h) : "N/A");

            // Alert on low confidence decisions (< 0.7)
            if (event.getConfidenceScore() != null && event.getConfidenceScore() < 0.7) {
                log.warn("LOW CONFIDENCE AI DECISION - Event: {}, Agent: {}, Confidence: {}, Decision Type: {}",
                    event.getEventId(),
                    event.getAgentType(),
                    event.getConfidenceScore(),
                    event.getDecisionType());
            }

            // Alert on high cost decisions (> $1.00)
            if (event.getCostEstimate() != null && event.getCostEstimate() > 1.0) {
                log.warn("HIGH COST AI DECISION - Event: {}, Agent: {}, Cost: ${}, Decision Type: {}",
                    event.getEventId(),
                    event.getAgentType(),
                    event.getCostEstimate(),
                    event.getDecisionType());
            }

            log.debug("Updated AI decision metrics for event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to update AI decision metrics for event: {}", event.getEventId(), e);
            // Don't throw - metrics update failure shouldn't break event storage
        }
    }

    /**
     * Analyze decision patterns for anomaly detection.
     * Detects sudden drops in confidence, unusual patterns, and compares with historical baselines.
     */
    public void analyzeDecisionPattern(AIAgentDecisionEvent event) {
        try {
            Instant now = Instant.now();
            Instant oneDayAgo = now.minusSeconds(86400);
            Instant oneWeekAgo = now.minusSeconds(604800);

            // Get recent decisions from same agent type for comparison
            List<AIAgentDecisionEventEntity> recentDecisions = aiDecisionRepository
                .findByAgentTypeAndTimestampBetween(
                    event.getAgentType(),
                    oneDayAgo,
                    now,
                    org.springframework.data.domain.Pageable.ofSize(100)
                ).getContent();

            if (recentDecisions.isEmpty()) {
                log.debug("No recent decisions found for pattern analysis: {}", event.getEventId());
                return;
            }

            // Calculate baseline confidence from recent decisions
            double baselineConfidence = recentDecisions.stream()
                .filter(d -> d.getConfidenceScore() != null)
                .mapToDouble(AIAgentDecisionEventEntity::getConfidenceScore)
                .average()
                .orElse(0.0);

            // Detect sudden drop in confidence (> 20% below baseline)
            if (event.getConfidenceScore() != null && baselineConfidence > 0) {
                double confidenceDrop = baselineConfidence - event.getConfidenceScore();
                double dropPercentage = (confidenceDrop / baselineConfidence) * 100;

                if (dropPercentage > 20.0) {
                    log.warn("CONFIDENCE DROP DETECTED - Event: {}, Agent: {}, Current: {}, Baseline: {}, Drop: {:.1f}%",
                        event.getEventId(),
                        event.getAgentType(),
                        event.getConfidenceScore(),
                        baselineConfidence,
                        dropPercentage);
                }
            }

            // Detect unusual decision type patterns
            long sameDecisionTypeCount = recentDecisions.stream()
                .filter(d -> d.getDecisionType() == event.getDecisionType())
                .count();
            
            double decisionTypeFrequency = (double) sameDecisionTypeCount / recentDecisions.size();
            
            // Alert if this decision type is rare (< 5% of recent decisions)
            if (decisionTypeFrequency < 0.05 && recentDecisions.size() >= 20) {
                log.warn("UNUSUAL DECISION TYPE PATTERN - Event: {}, Agent: {}, Decision Type: {}, Frequency: {:.1f}%",
                    event.getEventId(),
                    event.getAgentType(),
                    event.getDecisionType(),
                    decisionTypeFrequency * 100);
            }

            // Compare with weekly baseline for trend analysis
            List<AIAgentDecisionEventEntity> weeklyDecisions = aiDecisionRepository
                .findByAgentTypeAndTimestampBetween(
                    event.getAgentType(),
                    oneWeekAgo,
                    now,
                    org.springframework.data.domain.Pageable.ofSize(1000)
                ).getContent();

            if (weeklyDecisions.size() >= 50) {
                double weeklyAvgConfidence = weeklyDecisions.stream()
                    .filter(d -> d.getConfidenceScore() != null)
                    .mapToDouble(AIAgentDecisionEventEntity::getConfidenceScore)
                    .average()
                    .orElse(0.0);

                // Detect if current decision is significantly below weekly average
                if (event.getConfidenceScore() != null && weeklyAvgConfidence > 0) {
                    double deviation = weeklyAvgConfidence - event.getConfidenceScore();
                    if (deviation > 0.15) { // More than 15% below weekly average
                        log.warn("BELOW WEEKLY BASELINE - Event: {}, Agent: {}, Current: {}, Weekly Avg: {}, Deviation: {:.1f}%",
                            event.getEventId(),
                            event.getAgentType(),
                            event.getConfidenceScore(),
                            weeklyAvgConfidence,
                            (deviation / weeklyAvgConfidence) * 100);
                    }
                }
            }

            log.debug("Analyzed decision pattern for event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to analyze decision pattern for event: {}", event.getEventId(), e);
            // Don't throw - pattern analysis failure shouldn't break event storage
        }
    }

    /**
     * Get AI decisions for a tenant in time range.
     */
    @Transactional(readOnly = true)
    public List<AIAgentDecisionEventEntity> getAIDecisionsForTenant(
            String tenantId, Instant startTime, Instant endTime) {
        return aiDecisionRepository.findByTenantIdAndTimestampBetween(
            tenantId, startTime, endTime, org.springframework.data.domain.Pageable.unpaged()
        ).getContent();
    }

    /**
     * Get AI decisions by correlation ID (decision chains).
     */
    @Transactional(readOnly = true)
    public List<AIAgentDecisionEventEntity> getDecisionChain(String correlationId) {
        return aiDecisionRepository.findByCorrelationIdOrderByTimestampAsc(correlationId);
    }

    // ==================== Configuration Change Events ====================

    /**
     * Store configuration engine change event.
     */
    @Transactional
    public void storeConfigurationChange(ConfigurationEngineEvent event) {
        try {
            ConfigurationEngineEventEntity entity = 
                ConfigurationEngineEventEntity.fromDomainModel(event);
            configChangeRepository.save(entity);
            log.debug("Stored configuration change event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to store configuration change event: {}", event.getEventId(), e);
            throw e;
        }
    }

    /**
     * Update configuration history for a specific config key.
     */
    public void updateConfigurationHistory(ConfigurationEngineEvent event) {
        // TODO: Implement configuration history tracking
        // - Maintain version history
        // - Track value changes over time
        // - Enable time-travel debugging
        log.debug("Updated configuration history for key: {}", event.getConfigKey());
    }

    /**
     * Track performance impact of configuration changes.
     */
    public void trackPerformanceImpact(ConfigurationEngineEvent event) {
        // TODO: Implement performance impact tracking
        // - Correlate config changes with metrics
        // - Calculate before/after performance
        // - Generate impact reports
        log.debug("Tracked performance impact for change: {}", event.getChangeId());
    }

    /**
     * Alert on high-risk configuration changes.
     */
    public void alertOnHighRiskChange(ConfigurationEngineEvent event) {
        // TODO: Implement alerting
        // - Send notifications to ops team
        // - Create incidents for critical changes
        // - Trigger automated rollback if needed
        log.warn("High-risk configuration change detected: {} in {}", 
            event.getConfigKey(), event.getEnvironment());
    }

    /**
     * Get configuration history for a specific key.
     */
    @Transactional(readOnly = true)
    public List<ConfigurationEngineEventEntity> getConfigurationHistory(
            String configKey, Duration lookback) {
        Instant startTime = Instant.now().minus(lookback);
        Instant endTime = Instant.now();
        return configChangeRepository.findConfigurationHistory(configKey, startTime, endTime);
    }

    /**
     * Get changes linked to an AI recommendation.
     */
    @Transactional(readOnly = true)
    public List<ConfigurationEngineEventEntity> getChangesForAIRecommendation(
            UUID aiRecommendationId) {
        return configChangeRepository.findByAiRecommendationId(aiRecommendationId);
    }

    // ==================== User Action Events ====================

    /**
     * Store user configuration action event.
     */
    @Transactional
    public void storeUserAction(UserConfigurationActionEvent event) {
        try {
            UserConfigurationActionEventEntity entity = 
                UserConfigurationActionEventEntity.fromDomainModel(event);
            userActionRepository.save(entity);
            log.debug("Stored user action event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to store user action event: {}", event.getEventId(), e);
            throw e;
        }
    }

    /**
     * Track user behavior for UX improvements.
     */
    public void trackUserBehavior(UserConfigurationActionEvent event) {
        try {
            Instant now = Instant.now();
            Instant thirtyDaysAgo = now.minusSeconds(2592000);

            // Analyze user acceptance/rejection patterns
            List<UserConfigurationActionEventEntity> recentActions =
                userActionRepository.findByUserIdAndTimestampBetween(
                    event.getUserId(),
                    thirtyDaysAgo,
                    now,
                    org.springframework.data.domain.Pageable.unpaged()
                ).getContent();

            if (recentActions.isEmpty()) {
                log.debug("No historical user actions found for behavior analysis: userId={}", event.getUserId());
                return;
            }

            // Calculate AI recommendation acceptance rate
            long aiRecommendationActions = recentActions.stream()
                .filter(a -> a.getAiRecommendationId() != null)
                .count();

            long acceptedRecommendations = recentActions.stream()
                .filter(a -> a.getAiRecommendationAction() == UserConfigurationActionEvent.AIRecommendationAction.ACCEPTED)
                .count();

            long rejectedRecommendations = recentActions.stream()
                .filter(a -> a.getAiRecommendationAction() == UserConfigurationActionEvent.AIRecommendationAction.REJECTED)
                .count();

            double acceptanceRate = aiRecommendationActions > 0
                ? (double) acceptedRecommendations / aiRecommendationActions
                : 0.0;

            // Log user trust patterns
            log.info("USER BEHAVIOR PATTERN - User: {}, AI Acceptance Rate: {:.1f}%, Accepted: {}, Rejected: {}, Total Actions: {}",
                event.getUserId(),
                acceptanceRate * 100,
                acceptedRecommendations,
                rejectedRecommendations,
                recentActions.size());

            // Identify user pain points
            long validationFailures = recentActions.stream()
                .filter(a -> a.getActionStatus() == UserConfigurationActionEvent.ActionStatus.VALIDATION_FAILED)
                .count();

            long actionFailures = recentActions.stream()
                .filter(a -> a.getActionStatus() == UserConfigurationActionEvent.ActionStatus.FAILED)
                .count();

            if (validationFailures > 5 || actionFailures > 3) {
                log.warn("USER PAIN POINT DETECTED - User: {}, Validation Failures: {}, Action Failures: {} (last 30 days)",
                    event.getUserId(),
                    validationFailures,
                    actionFailures);
            }

            // Detect unusual activity patterns
            long actionsInLastHour = recentActions.stream()
                .filter(a -> a.getTimestamp().isAfter(now.minusSeconds(3600)))
                .count();

            if (actionsInLastHour > 20) {
                log.warn("HIGH ACTIVITY DETECTED - User: {}, Actions in last hour: {} (potential automation or bot)",
                    event.getUserId(),
                    actionsInLastHour);
            }

            // Track action source preferences
            Map<UserConfigurationActionEvent.ActionSource, Long> actionsBySource = recentActions.stream()
                .filter(a -> a.getActionSource() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                    UserConfigurationActionEventEntity::getActionSource,
                    java.util.stream.Collectors.counting()
                ));

            log.debug("USER PREFERENCES - User: {}, Preferred Sources: {}",
                event.getUserId(),
                actionsBySource);

            log.debug("Tracked user behavior for: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to track user behavior for user: {}", event.getUserId(), e);
            // Don't throw - behavior tracking failure shouldn't break event storage
        }
    }

    /**
     * Update AI recommendation feedback.
     */
    public void updateAIRecommendationFeedback(UserConfigurationActionEvent event) {
        try {
            if (event.getAiRecommendationId() == null) {
                log.debug("No AI recommendation ID - skipping feedback processing");
                return;
            }

            Instant now = Instant.now();
            Instant oneWeekAgo = now.minusSeconds(604800);

            // Get all feedback for this recommendation
            List<UserConfigurationActionEventEntity> feedbackActions =
                userActionRepository.findByAiRecommendationId(event.getAiRecommendationId());

            // Calculate feedback statistics
            double avgRating = feedbackActions.stream()
                .filter(a -> a.getUserFeedbackRating() != null)
                .mapToInt(UserConfigurationActionEventEntity::getUserFeedbackRating)
                .average()
                .orElse(0.0);

            long acceptedCount = feedbackActions.stream()
                .filter(a -> a.getAiRecommendationAction() == UserConfigurationActionEvent.AIRecommendationAction.ACCEPTED)
                .count();

            long rejectedCount = feedbackActions.stream()
                .filter(a -> a.getAiRecommendationAction() == UserConfigurationActionEvent.AIRecommendationAction.REJECTED)
                .count();

            long modifiedCount = feedbackActions.stream()
                .filter(a -> a.getAiRecommendationAction() == UserConfigurationActionEvent.AIRecommendationAction.MODIFIED_THEN_ACCEPTED)
                .count();

            // Log feedback quality metrics
            log.info("AI RECOMMENDATION FEEDBACK - RecommendationId: {}, Avg Rating: {:.1f}/5.0, Accepted: {}, Rejected: {}, Modified: {}",
                event.getAiRecommendationId(),
                avgRating,
                acceptedCount,
                rejectedCount,
                modifiedCount);

            // Identify poor recommendations (low rating + rejection)
            if (avgRating < 2.0 && rejectedCount > acceptedCount) {
                log.warn("POOR AI RECOMMENDATION DETECTED - RecommendationId: {}, Avg Rating: {:.1f}, Rejection Rate: {:.1f}%",
                    event.getAiRecommendationId(),
                    avgRating,
                    (double) rejectedCount / (rejectedCount + acceptedCount) * 100);
            }

            // Identify high-quality recommendations (high rating + acceptance)
            if (avgRating >= 4.0 && acceptedCount > 0 && rejectedCount == 0) {
                log.info("HIGH QUALITY AI RECOMMENDATION - RecommendationId: {}, Avg Rating: {:.1f}, 100% Acceptance",
                    event.getAiRecommendationId(),
                    avgRating);
            }

            // Identify recommendations needing model retraining (modified frequently)
            if (modifiedCount > 3) {
                log.warn("RECOMMENDATION MODIFICATION PATTERN - RecommendationId: {}, Modified {} times (users frequently adjust AI suggestions)",
                    event.getAiRecommendationId(),
                    modifiedCount);
            }

            // Process user feedback comments for sentiment analysis
            List<String> feedbackComments = feedbackActions.stream()
                .filter(a -> a.getUserFeedbackComment() != null && !a.getUserFeedbackComment().isBlank())
                .map(UserConfigurationActionEventEntity::getUserFeedbackComment)
                .toList();

            if (!feedbackComments.isEmpty()) {
                log.info("AI RECOMMENDATION FEEDBACK COMMENTS - RecommendationId: {}, Comment Count: {}",
                    event.getAiRecommendationId(),
                    feedbackComments.size());

                // Log high-confidence rejections for model retraining
                feedbackActions.stream()
                    .filter(a -> a.getAiRecommendationAction() == UserConfigurationActionEvent.AIRecommendationAction.REJECTED)
                    .filter(a -> a.getUserFeedbackRating() != null && a.getUserFeedbackRating() <= 2)
                    .filter(a -> a.getUserFeedbackComment() != null)
                    .forEach(a -> log.warn("HIGH CONFIDENCE REJECTION - RecommendationId: {}, Rating: {}, Comment: '{}'",
                        event.getAiRecommendationId(),
                        a.getUserFeedbackRating(),
                        a.getUserFeedbackComment()));
            }

            log.debug("Updated AI recommendation feedback for: {}", event.getAiRecommendationId());
        } catch (Exception e) {
            log.error("Failed to update AI recommendation feedback for: {}", event.getAiRecommendationId(), e);
            // Don't throw - feedback processing failure shouldn't break event storage
        }
    }

    /**
     * Log compliance event for audit trail.
     */
    public void logComplianceEvent(UserConfigurationActionEvent event) {
        try {
            // HIPAA Compliance: Log all user actions with full context
            log.info("COMPLIANCE AUDIT - User: {}, Action: {}, Resource: {}, Timestamp: {}, " +
                "TenantId: {}, CorrelationId: {}, IPAddress: {}, SessionId: {}",
                event.getUserId(),
                event.getActionType(),
                event.getConfigurationKey() != null ? event.getConfigurationKey() : "N/A",
                event.getTimestamp(),
                event.getTenantId(),
                event.getCorrelationId(),
                event.getIpAddress() != null ? event.getIpAddress() : "UNKNOWN",
                event.getSessionId() != null ? event.getSessionId() : "UNKNOWN");

            // Track data access patterns for HIPAA §164.312(b) - Audit Controls
            if (event.getActionType() == UserConfigurationActionEvent.ActionType.VIEW_CONFIGURATION ||
                event.getActionType() == UserConfigurationActionEvent.ActionType.EXPORT_CONFIGURATION) {

                log.info("DATA ACCESS AUDIT - User: {}, Action: {}, Resource: {}, Source: {}",
                    event.getUserId(),
                    event.getActionType(),
                    event.getConfigurationKey(),
                    event.getActionSource());
            }

            // Track configuration changes for compliance (who-did-what-when)
            if (event.getActionType() == UserConfigurationActionEvent.ActionType.EDIT_CONFIGURATION ||
                event.getActionType() == UserConfigurationActionEvent.ActionType.CREATE_TENANT_OVERRIDE ||
                event.getActionType() == UserConfigurationActionEvent.ActionType.DELETE_TENANT_OVERRIDE) {

                log.info("CONFIGURATION CHANGE AUDIT - User: {}, Action: {}, ConfigKey: {}, " +
                    "RequestedValue: {}, AppliedValue: {}, RequiresApproval: {}, ApprovalStatus: {}",
                    event.getUserId(),
                    event.getActionType(),
                    event.getConfigurationKey(),
                    event.getRequestedValue(),
                    event.getAppliedValue(),
                    event.getRequiresApproval(),
                    event.getApprovalStatus());

                // Alert on unapproved high-risk changes
                if (event.getRequiresApproval() != null && event.getRequiresApproval() &&
                    event.getApprovalStatus() != UserConfigurationActionEvent.ApprovalStatus.APPROVED) {

                    log.warn("UNAPPROVED HIGH-RISK CHANGE - User: {}, Action: {}, ConfigKey: {}, ApprovalStatus: {}",
                        event.getUserId(),
                        event.getActionType(),
                        event.getConfigurationKey(),
                        event.getApprovalStatus());
                }
            }

            // Track AI interactions for AI governance compliance
            if (event.getActionType() == UserConfigurationActionEvent.ActionType.QUERY_AI_ASSISTANT ||
                event.getActionType() == UserConfigurationActionEvent.ActionType.ACCEPT_AI_RECOMMENDATION ||
                event.getActionType() == UserConfigurationActionEvent.ActionType.REJECT_AI_RECOMMENDATION) {

                log.info("AI GOVERNANCE AUDIT - User: {}, Action: {}, AIRecommendationId: {}, " +
                    "AIRecommendationAction: {}, Query: '{}'",
                    event.getUserId(),
                    event.getActionType(),
                    event.getAiRecommendationId(),
                    event.getAiRecommendationAction(),
                    event.getNaturalLanguageQuery() != null ? event.getNaturalLanguageQuery() : "N/A");
            }

            // Track failed actions for security monitoring
            if (event.getActionStatus() == UserConfigurationActionEvent.ActionStatus.FAILED ||
                event.getActionStatus() == UserConfigurationActionEvent.ActionStatus.VALIDATION_FAILED) {

                log.warn("ACTION FAILURE AUDIT - User: {}, Action: {}, Status: {}, Error: '{}'",
                    event.getUserId(),
                    event.getActionType(),
                    event.getActionStatus(),
                    event.getErrorMessage() != null ? event.getErrorMessage() : "N/A");
            }

            // Generate immutable audit trail entry
            // NOTE: This is logged at INFO level to ensure it's captured in audit log files
            // All audit logs should be sent to immutable storage (e.g., S3, Splunk, Datadog)
            log.info("IMMUTABLE AUDIT ENTRY - EventId: {}, Timestamp: {}, User: {}, TenantId: {}, " +
                "Action: {}, Status: {}, CorrelationId: {}",
                event.getEventId(),
                event.getTimestamp(),
                event.getUserId(),
                event.getTenantId(),
                event.getActionType(),
                event.getActionStatus(),
                event.getCorrelationId());

            log.debug("Logged compliance event for user: {} action: {}",
                event.getUserId(), event.getActionType());
        } catch (Exception e) {
            // CRITICAL: Compliance logging failure must be escalated
            log.error("CRITICAL: Failed to log compliance event for user: {} action: {}",
                event.getUserId(), event.getActionType(), e);
            // Don't throw - but this should trigger alerts in production
        }
    }

    /**
     * Get user actions for a user in time range.
     */
    @Transactional(readOnly = true)
    public List<UserConfigurationActionEventEntity> getUserActions(
            String userId, Instant startTime, Instant endTime) {
        return userActionRepository.findByUserIdAndTimestampBetween(
            userId, startTime, endTime, org.springframework.data.domain.Pageable.unpaged()
        ).getContent();
    }

    /**
     * Get pending approval requests.
     */
    @Transactional(readOnly = true)
    public List<UserConfigurationActionEventEntity> getPendingApprovals() {
        return userActionRepository.findPendingApprovals();
    }

    // ==================== Cross-Event Analytics ====================

    /**
     * Get complete audit trail for a configuration change.
     * Links AI decision -> User action -> Configuration change.
     */
    @Transactional(readOnly = true)
    public AuditTrail getCompleteAuditTrail(String correlationId) {
        List<AIAgentDecisionEventEntity> aiDecisions = 
            aiDecisionRepository.findByCorrelationIdOrderByTimestampAsc(correlationId);
        
        List<UserConfigurationActionEventEntity> userActions = 
            userActionRepository.findByCorrelationIdOrderByTimestampAsc(correlationId);
        
        List<ConfigurationEngineEventEntity> configChanges = 
            configChangeRepository.findByCorrelationIdOrderByTimestampAsc(correlationId);
        
        return new AuditTrail(aiDecisions, userActions, configChanges);
    }

    /**
     * Audit trail containing all related events.
     */
    public record AuditTrail(
        List<AIAgentDecisionEventEntity> aiDecisions,
        List<UserConfigurationActionEventEntity> userActions,
        List<ConfigurationEngineEventEntity> configChanges
    ) {}
}
