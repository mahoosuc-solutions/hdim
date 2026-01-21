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
        // TODO: Implement user behavior tracking
        // - Track common user workflows
        // - Identify pain points
        // - Optimize UI/UX based on patterns
        log.debug("Tracked user behavior for action: {}", event.getActionType());
    }

    /**
     * Update AI recommendation feedback.
     */
    public void updateAIRecommendationFeedback(UserConfigurationActionEvent event) {
        // TODO: Implement feedback processing
        // - Update AI recommendation quality scores
        // - Train/fine-tune models with feedback
        // - Identify poor recommendations
        log.debug("Updated AI recommendation feedback for: {}", event.getAiRecommendationId());
    }

    /**
     * Log compliance event for audit trail.
     */
    public void logComplianceEvent(UserConfigurationActionEvent event) {
        // TODO: Implement compliance logging
        // - Generate compliance reports
        // - Track who-did-what-when
        // - Maintain immutable audit trail
        log.debug("Logged compliance event for user: {} action: {}", 
            event.getUserId(), event.getActionType());
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
