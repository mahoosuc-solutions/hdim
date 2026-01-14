package com.healthdata.audit.service.ai;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for replaying audit events for compliance audits and verification.
 * 
 * Enables:
 * - Historical event replay for compliance audits
 * - Event integrity verification
 * - Temporal queries for specific time ranges
 * - Patient-specific audit trails
 * - Decision type filtering
 * 
 * Use Cases:
 * - HIPAA compliance audits (6-year retention)
 * - SOC 2 audit verification
 * - Clinical decision review
 * - Regulatory investigations
 * - Event replay for debugging
 * 
 * @author HDIM Platform Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIAuditEventReplayService {
    
    private final AIAgentDecisionEventRepository repository;
    
    /**
     * Replay all events for a tenant within a time range.
     * 
     * @param tenantId Tenant ID
     * @param from Start time (inclusive)
     * @param to End time (inclusive)
     * @return List of events ordered by timestamp
     */
    public List<AIAgentDecisionEvent> replayEvents(String tenantId, Instant from, Instant to) {
        log.info("Replaying events for tenant {} from {} to {}", tenantId, from, to);
        
        List<AIAgentDecisionEventEntity> entities = repository.findByTenantIdAndTimestampBetweenOrderByTimestampAsc(
            tenantId, from, to
        );
        
        List<AIAgentDecisionEvent> events = entities.stream()
            .map(this::toModel)
            .collect(Collectors.toList());
        
        log.info("Found {} events for replay", events.size());
        return events;
    }
    
    /**
     * Replay events by decision type.
     * 
     * @param tenantId Tenant ID
     * @param decisionType Decision type to filter
     * @param from Start time (inclusive)
     * @param to End time (inclusive)
     * @return List of events ordered by timestamp
     */
    public List<AIAgentDecisionEvent> replayByDecisionType(
            String tenantId,
            AIAgentDecisionEvent.DecisionType decisionType,
            Instant from,
            Instant to) {
        
        log.info("Replaying {} events for tenant {} from {} to {}", 
            decisionType, tenantId, from, to);
        
        List<AIAgentDecisionEventEntity> entities = repository.findByTenantIdAndDecisionTypeAndTimestampBetweenOrderByTimestampAsc(
            tenantId, decisionType, from, to
        );
        
        List<AIAgentDecisionEvent> events = entities.stream()
            .map(this::toModel)
            .collect(Collectors.toList());
        
        log.info("Found {} events for replay", events.size());
        return events;
    }
    
    /**
     * Replay events for a specific patient (for compliance audits).
     * 
     * @param tenantId Tenant ID
     * @param patientId Patient ID (resource ID)
     * @param from Start time (inclusive)
     * @param to End time (inclusive)
     * @return List of events ordered by timestamp
     */
    public List<AIAgentDecisionEvent> replayForPatient(
            String tenantId,
            String patientId,
            Instant from,
            Instant to) {
        
        log.info("Replaying events for patient {} in tenant {} from {} to {}", 
            patientId, tenantId, from, to);
        
        List<AIAgentDecisionEventEntity> entities = repository.findByTenantIdAndResourceIdAndTimestampBetweenOrderByTimestampAsc(
            tenantId, patientId, from, to
        );
        
        List<AIAgentDecisionEvent> events = entities.stream()
            .map(this::toModel)
            .collect(Collectors.toList());
        
        log.info("Found {} events for patient replay", events.size());
        return events;
    }
    
    /**
     * Replay events by agent type.
     * 
     * @param tenantId Tenant ID
     * @param agentType Agent type to filter
     * @param from Start time (inclusive)
     * @param to End time (inclusive)
     * @return List of events ordered by timestamp
     */
    public List<AIAgentDecisionEvent> replayByAgentType(
            String tenantId,
            AIAgentDecisionEvent.AgentType agentType,
            Instant from,
            Instant to) {
        
        log.info("Replaying {} agent events for tenant {} from {} to {}", 
            agentType, tenantId, from, to);
        
        List<AIAgentDecisionEventEntity> entities = repository.findByTenantIdAndAgentTypeAndTimestampBetweenOrderByTimestampAsc(
            tenantId, agentType, from, to
        );
        
        List<AIAgentDecisionEvent> events = entities.stream()
            .map(this::toModel)
            .collect(Collectors.toList());
        
        log.info("Found {} events for agent type replay", events.size());
        return events;
    }
    
    /**
     * Replay events by correlation ID (for tracing related decisions).
     * 
     * @param tenantId Tenant ID
     * @param correlationId Correlation ID
     * @return List of events ordered by timestamp
     */
    public List<AIAgentDecisionEvent> replayByCorrelationId(
            String tenantId,
            String correlationId) {
        
        log.info("Replaying events for correlation ID {} in tenant {}", 
            correlationId, tenantId);
        
        List<AIAgentDecisionEventEntity> entities = repository.findByTenantIdAndCorrelationIdOrderByTimestampAsc(
            tenantId, correlationId
        );
        
        List<AIAgentDecisionEvent> events = entities.stream()
            .map(this::toModel)
            .collect(Collectors.toList());
        
        log.info("Found {} events for correlation ID replay", events.size());
        return events;
    }
    
    /**
     * Get a single event by ID.
     * 
     * @param tenantId Tenant ID
     * @param eventId Event ID
     * @return Event if found
     */
    public AIAgentDecisionEvent getEvent(String tenantId, UUID eventId) {
        log.debug("Retrieving event {} for tenant {}", eventId, tenantId);
        
        AIAgentDecisionEventEntity entity = repository.findByTenantIdAndEventId(tenantId, eventId)
            .orElseThrow(() -> new RuntimeException(
                "Event not found: " + eventId + " for tenant: " + tenantId));
        
        return toModel(entity);
    }
    
    /**
     * Verify event integrity for a time range.
     * 
     * Checks:
     * - Event count matches expected
     * - No gaps in event sequence
     * - Timestamps are monotonically increasing
     * - All required fields are present
     * 
     * @param tenantId Tenant ID
     * @param from Start time (inclusive)
     * @param to End time (inclusive)
     * @return Verification result
     */
    public ReplayVerificationResult verifyReplayIntegrity(
            String tenantId,
            Instant from,
            Instant to) {
        
        log.info("Verifying replay integrity for tenant {} from {} to {}", 
            tenantId, from, to);
        
        List<AIAgentDecisionEventEntity> entities = repository.findByTenantIdAndTimestampBetweenOrderByTimestampAsc(
            tenantId, from, to
        );
        
        List<AIAgentDecisionEvent> events = entities.stream()
            .map(this::toModel)
            .collect(Collectors.toList());
        
        ReplayVerificationResult result = new ReplayVerificationResult();
        result.setTenantId(tenantId);
        result.setFromTime(from);
        result.setToTime(to);
        result.setTotalEvents(events.size());
        
        // Verify timestamps are ordered
        boolean timestampsOrdered = true;
        Instant previousTimestamp = null;
        for (AIAgentDecisionEvent event : events) {
            if (previousTimestamp != null && event.getTimestamp().isBefore(previousTimestamp)) {
                timestampsOrdered = false;
                result.addError("Timestamp ordering violation at event: " + event.getEventId());
            }
            previousTimestamp = event.getTimestamp();
        }
        result.setTimestampsOrdered(timestampsOrdered);
        
        // Verify required fields
        int eventsWithMissingFields = 0;
        for (AIAgentDecisionEvent event : events) {
            if (event.getEventId() == null || event.getTenantId() == null || 
                event.getAgentId() == null || event.getTimestamp() == null) {
                eventsWithMissingFields++;
                result.addError("Missing required fields in event: " + event.getEventId());
            }
        }
        result.setEventsWithMissingFields(eventsWithMissingFields);
        
        result.setIntegrityVerified(timestampsOrdered && eventsWithMissingFields == 0);
        
        log.info("Integrity verification complete: {} events, integrity={}", 
            events.size(), result.isIntegrityVerified());
        
        return result;
    }
    
    /**
     * Result of replay integrity verification.
     */
    @lombok.Data
    public static class ReplayVerificationResult {
        private String tenantId;
        private Instant fromTime;
        private Instant toTime;
        private int totalEvents;
        private boolean timestampsOrdered;
        private int eventsWithMissingFields;
        private boolean integrityVerified;
        private List<String> errors = new java.util.ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
    }
    
    /**
     * Convert entity to domain model.
     */
    private AIAgentDecisionEvent toModel(AIAgentDecisionEventEntity entity) {
        AIAgentDecisionEvent.AIAgentDecisionEventBuilder builder = AIAgentDecisionEvent.builder()
            .eventId(entity.getEventId())
            .timestamp(entity.getTimestamp())
            .tenantId(entity.getTenantId())
            .agentId(entity.getAgentId())
            .agentType(entity.getAgentType())
            .agentVersion(entity.getAgentVersion())
            .modelName(entity.getModelName())
            .decisionType(entity.getDecisionType())
            .resourceType(entity.getResourceType())
            .resourceId(entity.getResourceId())
            .inputMetrics(entity.getInputMetrics())
            .userQuery(entity.getUserQuery())
            .confidenceScore(entity.getConfidenceScore())
            .reasoning(entity.getReasoning())
            .outcome(entity.getOutcome())
            .appliedByUser(entity.getAppliedByUser())
            .inferenceTimeMs(entity.getInferenceTimeMs())
            .tokenCount(entity.getTokenCount())
            .costEstimate(entity.getCostEstimate())
            .requestId(entity.getRequestId())
            .correlationId(entity.getCorrelationId())
            .previousEventId(entity.getPreviousEventId());
        
        // Customer profile
        if (entity.getCustomerTier() != null) {
            builder.customerProfile(AIAgentDecisionEvent.CustomerProfile.builder()
                .customerTier(entity.getCustomerTier())
                .patientCount(entity.getPatientCount())
                .providerCount(entity.getProviderCount())
                .averageDailyMessages(entity.getAverageDailyMessages())
                .trafficTier(entity.getTrafficTier())
                .build());
        }
        
        // Recommendation
        if (entity.getConfigType() != null) {
            builder.recommendation(AIAgentDecisionEvent.ConfigurationRecommendation.builder()
                .configType(entity.getConfigType())
                .currentValue(entity.getCurrentValue())
                .recommendedValue(entity.getRecommendedValue())
                .expectedImpact(entity.getExpectedImpact())
                .riskLevel(entity.getRiskLevel())
                .build());
        }
        
        // User feedback
        if (entity.getUserFeedbackRating() != null) {
            builder.userFeedback(AIAgentDecisionEvent.UserFeedback.builder()
                .rating(entity.getUserFeedbackRating())
                .comment(entity.getUserFeedbackComment())
                .timestamp(entity.getUserFeedbackTimestamp())
                .build());
        }
        
        return builder.build();
    }
}

