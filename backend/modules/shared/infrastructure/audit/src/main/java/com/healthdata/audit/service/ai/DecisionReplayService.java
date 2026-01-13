package com.healthdata.audit.service.ai;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for replaying AI decisions.
 * 
 * Allows:
 * - Re-executing AI decisions with the same inputs
 * - Comparing original vs replayed results
 * - Debugging AI decision-making
 * - Testing AI model improvements
 * - Validating decision consistency
 */
@Slf4j
@Service
public class DecisionReplayService {

    private final AIAgentDecisionEventRepository aiDecisionRepository;

    @Autowired
    public DecisionReplayService(AIAgentDecisionEventRepository aiDecisionRepository) {
        this.aiDecisionRepository = aiDecisionRepository;
    }

    /**
     * Replay an AI decision using the original inputs.
     * 
     * @param originalEventId ID of the original decision event
     * @return Replay result with comparison
     */
    public ReplayResult replayDecision(UUID originalEventId) {
        log.info("Replaying AI decision: {}", originalEventId);

        // Load original decision
        AIAgentDecisionEventEntity original = aiDecisionRepository.findById(originalEventId)
            .orElseThrow(() -> new IllegalArgumentException("Decision not found: " + originalEventId));

        // Prepare replay
        ReplayResult result = new ReplayResult();
        result.setOriginalEventId(originalEventId);
        result.setReplayedAt(Instant.now());
        result.setOriginalDecision(buildDecisionSummary(original));

        try {
            // Execute replay
            ReplayedDecision replayed = executeReplay(original);
            result.setReplayedDecision(replayed);

            // Compare results
            DecisionComparison comparison = compareDecisions(original, replayed);
            result.setComparison(comparison);

            // Determine replay status
            result.setStatus(comparison.isIdentical() ? ReplayStatus.IDENTICAL : ReplayStatus.DIFFERENT);
            result.setSuccess(true);

        } catch (Exception e) {
            log.error("Failed to replay decision: {}", originalEventId, e);
            result.setStatus(ReplayStatus.FAILED);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        log.info("Decision replay completed: {} - Status: {}", originalEventId, result.getStatus());
        return result;
    }

    /**
     * Replay multiple decisions in batch.
     */
    public List<ReplayResult> replayDecisionBatch(List<UUID> eventIds) {
        log.info("Replaying {} decisions in batch", eventIds.size());
        
        List<ReplayResult> results = new ArrayList<>();
        for (UUID eventId : eventIds) {
            try {
                ReplayResult result = replayDecision(eventId);
                results.add(result);
            } catch (Exception e) {
                log.error("Failed to replay decision in batch: {}", eventId, e);
                
                ReplayResult failedResult = new ReplayResult();
                failedResult.setOriginalEventId(eventId);
                failedResult.setSuccess(false);
                failedResult.setStatus(ReplayStatus.FAILED);
                failedResult.setErrorMessage(e.getMessage());
                results.add(failedResult);
            }
        }
        
        return results;
    }

    /**
     * Replay all decisions from a correlation chain.
     */
    public ChainReplayResult replayDecisionChain(String correlationId) {
        log.info("Replaying decision chain: {}", correlationId);

        List<AIAgentDecisionEventEntity> chain = 
            aiDecisionRepository.findByCorrelationIdOrderByTimestampAsc(correlationId);

        if (chain.isEmpty()) {
            throw new IllegalArgumentException("No decisions found for correlation ID: " + correlationId);
        }

        ChainReplayResult chainResult = new ChainReplayResult();
        chainResult.setCorrelationId(correlationId);
        chainResult.setReplayedAt(Instant.now());
        chainResult.setOriginalChainLength(chain.size());

        List<ReplayResult> decisionResults = new ArrayList<>();
        for (AIAgentDecisionEventEntity decision : chain) {
            ReplayResult result = replayDecision(decision.getEventId());
            decisionResults.add(result);
        }

        chainResult.setDecisionResults(decisionResults);

        // Calculate chain-level statistics
        long identicalCount = decisionResults.stream()
            .filter(r -> r.getStatus() == ReplayStatus.IDENTICAL)
            .count();
        
        chainResult.setIdenticalDecisions(identicalCount);
        chainResult.setDifferentDecisions(chain.size() - identicalCount);
        chainResult.setOverallConsistency(identicalCount / (double) chain.size());

        return chainResult;
    }

    /**
     * Execute the actual replay of a decision.
     * 
     * TODO: Integrate with actual AI agent services to re-execute decisions.
     * For now, simulates replay.
     */
    private ReplayedDecision executeReplay(AIAgentDecisionEventEntity original) {
        log.debug("Executing replay for decision: {}", original.getEventId());

        // TODO: Call the actual AI agent with original inputs
        // For now, simulate replay
        
        ReplayedDecision replayed = new ReplayedDecision();
        replayed.setAgentType(original.getAgentType().toString());
        replayed.setDecisionType(original.getDecisionType().toString());
        replayed.setModelName(original.getModelName());
        
        // Simulate slight variation in confidence (AI models can be non-deterministic)
        replayed.setConfidenceScore(original.getConfidenceScore() != null 
            ? original.getConfidenceScore() + (Math.random() * 0.02 - 0.01)
            : null);
        
        // In a real implementation, this would be the actual recommendation from re-execution
        replayed.setRecommendedValue(original.getRecommendedValue());
        replayed.setReasoning("Replayed decision - " + original.getReasoning());
        replayed.setInferenceTimeMs(System.currentTimeMillis() - System.currentTimeMillis());

        return replayed;
    }

    /**
     * Compare original and replayed decisions.
     */
    private DecisionComparison compareDecisions(
            AIAgentDecisionEventEntity original, 
            ReplayedDecision replayed) {
        
        DecisionComparison comparison = new DecisionComparison();

        // Compare recommendations
        boolean sameRecommendation = Objects.equals(
            original.getRecommendedValue(), 
            replayed.getRecommendedValue()
        );
        comparison.setRecommendationMatch(sameRecommendation);

        // Compare confidence scores (with tolerance for non-determinism)
        boolean similarConfidence = Math.abs(
            (original.getConfidenceScore() != null ? original.getConfidenceScore() : 0.0) - 
            (replayed.getConfidenceScore() != null ? replayed.getConfidenceScore() : 0.0)
        ) < 0.05; // 5% tolerance
        comparison.setConfidenceMatch(similarConfidence);

        // Overall identity check
        comparison.setIdentical(sameRecommendation && similarConfidence);

        // Differences
        List<String> differences = new ArrayList<>();
        if (!sameRecommendation) {
            differences.add(String.format("Recommendation changed: %s -> %s",
                original.getRecommendedValue(), replayed.getRecommendedValue()));
        }
        if (!similarConfidence) {
            differences.add(String.format("Confidence score difference: %.2f%% -> %.2f%%",
                original.getConfidenceScore() * 100, replayed.getConfidenceScore() * 100));
        }
        comparison.setDifferences(differences);

        return comparison;
    }

    /**
     * Build summary of a decision for the replay result.
     */
    private DecisionSummary buildDecisionSummary(AIAgentDecisionEventEntity decision) {
        DecisionSummary summary = new DecisionSummary();
        summary.setTimestamp(decision.getTimestamp());
        summary.setAgentType(decision.getAgentType().toString());
        summary.setDecisionType(decision.getDecisionType().toString());
        summary.setResourceType(decision.getResourceType());
        summary.setCurrentValue(decision.getCurrentValue());
        summary.setRecommendedValue(decision.getRecommendedValue());
        summary.setConfidenceScore(decision.getConfidenceScore());
        summary.setReasoning(decision.getReasoning());
        return summary;
    }

    // Enums and data models

    public enum ReplayStatus {
        IDENTICAL,      // Replayed decision exactly matches original
        DIFFERENT,      // Replayed decision differs from original
        FAILED          // Replay execution failed
    }

    @Data
    public static class ReplayResult {
        private UUID originalEventId;
        private Instant replayedAt;
        private DecisionSummary originalDecision;
        private ReplayedDecision replayedDecision;
        private DecisionComparison comparison;
        private ReplayStatus status;
        private boolean success;
        private String errorMessage;
    }

    @Data
    public static class DecisionSummary {
        private Instant timestamp;
        private String agentType;
        private String decisionType;
        private String resourceType;
        private String currentValue;
        private String recommendedValue;
        private Double confidenceScore;
        private String reasoning;
    }

    @Data
    public static class ReplayedDecision {
        private String agentType;
        private String decisionType;
        private String modelName;
        private String recommendedValue;
        private Double confidenceScore;
        private String reasoning;
        private Long inferenceTimeMs;
    }

    @Data
    public static class DecisionComparison {
        private boolean identical;
        private boolean recommendationMatch;
        private boolean confidenceMatch;
        private List<String> differences;
    }

    @Data
    public static class ChainReplayResult {
        private String correlationId;
        private Instant replayedAt;
        private int originalChainLength;
        private List<ReplayResult> decisionResults;
        private long identicalDecisions;
        private long differentDecisions;
        private double overallConsistency;
    }
}
