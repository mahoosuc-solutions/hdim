package com.healthdata.quality.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.client.CqlEngineServiceClient;
import com.healthdata.quality.dto.*;
import com.healthdata.quality.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Clinical Decision Support Service
 *
 * Provides CDS functionality including:
 * - Rule evaluation via CQL engine
 * - Recommendation generation and management
 * - Acknowledgment tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CdsService {

    private final CdsRuleRepository ruleRepository;
    private final CdsRecommendationRepository recommendationRepository;
    private final CdsAcknowledgmentRepository acknowledgmentRepository;
    private final CqlEngineServiceClient cqlEngineClient;
    private final ObjectMapper objectMapper;

    // ============================================
    // Rule Management
    // ============================================

    /**
     * Get all active CDS rules
     */
    public List<CdsRuleDTO> getActiveRules(String tenantId) {
        log.debug("Getting active CDS rules for tenant: {}", tenantId);
        return ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId)
            .stream()
            .map(CdsRuleDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get all CDS rules (including inactive)
     */
    public List<CdsRuleDTO> getAllRules(String tenantId) {
        log.debug("Getting all CDS rules for tenant: {}", tenantId);
        return ruleRepository.findByTenantIdOrderByPriorityAsc(tenantId)
            .stream()
            .map(CdsRuleDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get rules by category
     */
    public List<CdsRuleDTO> getRulesByCategory(String tenantId, String category) {
        CdsRuleEntity.CdsCategory cat = CdsRuleEntity.CdsCategory.valueOf(category.toUpperCase());
        return ruleRepository.findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(tenantId, cat)
            .stream()
            .map(CdsRuleDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific rule by code
     */
    public Optional<CdsRuleDTO> getRuleByCode(String tenantId, String ruleCode) {
        return ruleRepository.findByTenantIdAndRuleCode(tenantId, ruleCode)
            .map(CdsRuleDTO::fromEntity);
    }

    // ============================================
    // Recommendation Retrieval
    // ============================================

    /**
     * Get active recommendations for a patient
     */
    public List<CdsRecommendationDTO> getActiveRecommendations(String tenantId, UUID patientId) {
        log.debug("Getting active CDS recommendations for patient: {}", patientId);
        return recommendationRepository.findActiveRecommendations(tenantId, patientId)
            .stream()
            .map(CdsRecommendationDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get urgent recommendations for a patient
     */
    public List<CdsRecommendationDTO> getUrgentRecommendations(String tenantId, UUID patientId) {
        return recommendationRepository.findUrgentRecommendations(tenantId, patientId)
            .stream()
            .map(CdsRecommendationDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get recommendation count by urgency
     */
    public Map<String, Long> getRecommendationCountsByUrgency(String tenantId, UUID patientId) {
        Map<String, Long> counts = new HashMap<>();
        for (CdsRuleEntity.CdsUrgency urgency : CdsRuleEntity.CdsUrgency.values()) {
            counts.put(urgency.name(), recommendationRepository.countActiveByUrgency(tenantId, patientId, urgency));
        }
        return counts;
    }

    /**
     * Get total active recommendation count
     */
    public Long getActiveRecommendationCount(String tenantId, UUID patientId) {
        return recommendationRepository.countActiveRecommendations(tenantId, patientId);
    }

    /**
     * Get overdue recommendations
     */
    public List<CdsRecommendationDTO> getOverdueRecommendations(String tenantId, UUID patientId) {
        return recommendationRepository.findOverdueRecommendations(tenantId, patientId, Instant.now())
            .stream()
            .map(CdsRecommendationDTO::fromEntity)
            .collect(Collectors.toList());
    }

    // ============================================
    // CDS Rule Evaluation
    // ============================================

    /**
     * Evaluate CDS rules for a patient
     */
    @Transactional
    public CdsEvaluateResponse evaluateRules(String tenantId, CdsEvaluateRequest request) {
        log.info("Evaluating CDS rules for patient: {} in tenant: {}", request.getPatientId(), tenantId);

        Instant startTime = Instant.now();
        List<CdsRuleEntity> rulesToEvaluate = getRulesToEvaluate(tenantId, request);

        List<CdsRecommendationDTO> newRecommendations = new ArrayList<>();
        List<CdsRecommendationDTO> existingRecommendations = new ArrayList<>();
        List<CdsEvaluateResponse.EvaluationDetail> evaluationDetails = new ArrayList<>();
        int skippedCount = 0;

        for (CdsRuleEntity rule : rulesToEvaluate) {
            long ruleStartTime = System.currentTimeMillis();
            CdsEvaluateResponse.EvaluationDetail detail = CdsEvaluateResponse.EvaluationDetail.builder()
                .ruleCode(rule.getRuleCode())
                .ruleName(rule.getRuleName())
                .build();

            try {
                // Check if active recommendation already exists
                if (!Boolean.TRUE.equals(request.getForceReEvaluation()) &&
                    recommendationRepository.existsActiveRecommendation(tenantId, request.getPatientId(), rule.getId())) {

                    log.debug("Skipping rule {} - active recommendation exists", rule.getRuleCode());
                    skippedCount++;
                    detail.setTriggered(false);
                    detail.setResult("SKIPPED - Active recommendation exists");
                    detail.setEvaluationTimeMs(System.currentTimeMillis() - ruleStartTime);
                    evaluationDetails.add(detail);
                    continue;
                }

                // Evaluate rule via CQL engine
                boolean triggered = evaluateRule(tenantId, rule, request.getPatientId());
                detail.setTriggered(triggered);

                if (triggered) {
                    CdsRecommendationEntity recommendation = createRecommendation(tenantId, rule, request.getPatientId());
                    newRecommendations.add(CdsRecommendationDTO.fromEntity(recommendation));
                    detail.setResult("TRIGGERED - Recommendation created");
                } else {
                    detail.setResult("NOT_TRIGGERED");
                }

            } catch (Exception e) {
                log.error("Error evaluating rule {}: {}", rule.getRuleCode(), e.getMessage());
                detail.setTriggered(false);
                detail.setErrorMessage(e.getMessage());
                detail.setResult("ERROR");
            }

            detail.setEvaluationTimeMs(System.currentTimeMillis() - ruleStartTime);
            evaluationDetails.add(detail);
        }

        // Get existing recommendations
        recommendationRepository.findActiveRecommendations(tenantId, request.getPatientId())
            .forEach(r -> existingRecommendations.add(CdsRecommendationDTO.fromEntity(r)));

        // Build response
        return CdsEvaluateResponse.builder()
            .patientId(request.getPatientId())
            .evaluatedAt(startTime)
            .rulesEvaluated(rulesToEvaluate.size())
            .recommendationsGenerated(newRecommendations.size())
            .existingRecommendationsSkipped(skippedCount)
            .newRecommendations(newRecommendations)
            .existingRecommendations(existingRecommendations)
            .recommendationsByCategory(countByCategory(newRecommendations))
            .recommendationsByUrgency(countByUrgency(newRecommendations))
            .evaluationDetails(evaluationDetails)
            .build();
    }

    private List<CdsRuleEntity> getRulesToEvaluate(String tenantId, CdsEvaluateRequest request) {
        if (request.getRuleIds() != null && !request.getRuleIds().isEmpty()) {
            return ruleRepository.findAllById(request.getRuleIds())
                .stream()
                .filter(r -> r.getTenantId().equals(tenantId) && Boolean.TRUE.equals(r.getActive()))
                .collect(Collectors.toList());
        }

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            List<CdsRuleEntity> rules = new ArrayList<>();
            for (String category : request.getCategories()) {
                try {
                    CdsRuleEntity.CdsCategory cat = CdsRuleEntity.CdsCategory.valueOf(category.toUpperCase());
                    rules.addAll(ruleRepository.findByTenantIdAndCategoryAndActiveTrueOrderByPriorityAsc(tenantId, cat));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid category: {}", category);
                }
            }
            return rules;
        }

        return ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId);
    }

    private boolean evaluateRule(String tenantId, CdsRuleEntity rule, UUID patientId) {
        if (rule.getCqlLibraryName() == null || rule.getCqlExpression() == null) {
            log.warn("Rule {} has no CQL configuration - using default evaluation", rule.getRuleCode());
            return performDefaultEvaluation(rule, patientId);
        }

        try {
            String result = cqlEngineClient.evaluateCql(
                tenantId,
                rule.getCqlLibraryName(),
                patientId,
                null
            );

            return parseCqlResult(result, rule.getCqlExpression());
        } catch (Exception e) {
            log.error("CQL evaluation failed for rule {}: {}", rule.getRuleCode(), e.getMessage());
            // Fall back to default evaluation
            return performDefaultEvaluation(rule, patientId);
        }
    }

    private boolean performDefaultEvaluation(CdsRuleEntity rule, UUID patientId) {
        // Default evaluation logic when CQL is not available
        // In production, this could use rule-specific conditions
        return true; // For demo purposes, all rules trigger
    }

    private boolean parseCqlResult(String result, String expression) {
        try {
            // Parse CQL engine response to determine if rule triggered
            // This is a simplified implementation
            if (result == null || result.isBlank()) return false;
            return result.toLowerCase().contains("true") || result.contains("\"triggered\":true");
        } catch (Exception e) {
            log.error("Error parsing CQL result: {}", e.getMessage());
            return false;
        }
    }

    private CdsRecommendationEntity createRecommendation(String tenantId, CdsRuleEntity rule, UUID patientId) {
        CdsRecommendationEntity recommendation = CdsRecommendationEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .ruleId(rule.getId())
            .title(rule.getRuleName())
            .description(rule.getRecommendationTemplate())
            .category(rule.getCategory())
            .urgency(rule.getDefaultUrgency())
            .status(CdsRecommendationEntity.CdsStatus.ACTIVE)
            .priority(rule.getPriority())
            .actionItems(rule.getActionItems())
            .evidenceSource(rule.getEvidenceSource())
            .clinicalGuideline(rule.getClinicalGuideline())
            .evaluatedAt(Instant.now())
            .dueDate(calculateDueDate(rule.getDefaultUrgency()))
            .build();

        return recommendationRepository.save(recommendation);
    }

    private Instant calculateDueDate(CdsRuleEntity.CdsUrgency urgency) {
        Instant now = Instant.now();
        return switch (urgency) {
            case EMERGENT -> now; // Immediate
            case URGENT -> now.plusSeconds(48 * 60 * 60); // 48 hours
            case SOON -> now.plusSeconds(14 * 24 * 60 * 60); // 2 weeks
            case ROUTINE -> now.plusSeconds(30 * 24 * 60 * 60); // 30 days
        };
    }

    private Map<String, Integer> countByCategory(List<CdsRecommendationDTO> recommendations) {
        return recommendations.stream()
            .filter(r -> r.getCategory() != null)
            .collect(Collectors.groupingBy(
                CdsRecommendationDTO::getCategory,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    private Map<String, Integer> countByUrgency(List<CdsRecommendationDTO> recommendations) {
        return recommendations.stream()
            .filter(r -> r.getUrgency() != null)
            .collect(Collectors.groupingBy(
                CdsRecommendationDTO::getUrgency,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    // ============================================
    // Acknowledgment & Actions
    // ============================================

    /**
     * Acknowledge or act on a recommendation
     */
    @Transactional
    public CdsRecommendationDTO acknowledgeRecommendation(String tenantId, CdsAcknowledgeRequest request) {
        log.info("Processing CDS action: {} for recommendation: {}", request.getAction(), request.getRecommendationId());

        CdsRecommendationEntity recommendation = recommendationRepository
            .findById(request.getRecommendationId())
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + request.getRecommendationId()));

        if (!recommendation.getTenantId().equals(tenantId)) {
            throw new SecurityException("Access denied to recommendation");
        }

        String previousStatus = recommendation.getStatus().name();
        CdsAcknowledgmentEntity.ActionType actionType = parseActionType(request.getAction());

        // Update recommendation based on action
        updateRecommendationStatus(recommendation, request, actionType);
        recommendationRepository.save(recommendation);

        // Record acknowledgment
        CdsAcknowledgmentEntity acknowledgment = CdsAcknowledgmentEntity.builder()
            .tenantId(tenantId)
            .recommendationId(recommendation.getId())
            .patientId(recommendation.getPatientId())
            .userId(request.getUserId())
            .userName(request.getUserName())
            .userRole(request.getUserRole())
            .actionType(actionType)
            .reason(request.getReason())
            .notes(request.getNotes())
            .outcome(request.getOutcome())
            .followUpDate(request.getFollowUpDate())
            .followUpNotes(request.getFollowUpNotes())
            .previousStatus(previousStatus)
            .newStatus(recommendation.getStatus().name())
            .build();

        acknowledgmentRepository.save(acknowledgment);

        log.info("CDS recommendation {} status changed from {} to {}",
            recommendation.getId(), previousStatus, recommendation.getStatus());

        return CdsRecommendationDTO.fromEntity(recommendation);
    }

    private CdsAcknowledgmentEntity.ActionType parseActionType(String action) {
        try {
            return CdsAcknowledgmentEntity.ActionType.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid action type: " + action);
        }
    }

    private void updateRecommendationStatus(
            CdsRecommendationEntity recommendation,
            CdsAcknowledgeRequest request,
            CdsAcknowledgmentEntity.ActionType actionType) {

        Instant now = Instant.now();

        switch (actionType) {
            case ACKNOWLEDGED:
                recommendation.setStatus(CdsRecommendationEntity.CdsStatus.ACKNOWLEDGED);
                recommendation.setAcknowledgedAt(now);
                recommendation.setAcknowledgedBy(request.getUserId());
                recommendation.setAcknowledgmentNotes(request.getNotes());
                break;

            case ACCEPTED:
                recommendation.setStatus(CdsRecommendationEntity.CdsStatus.IN_PROGRESS);
                recommendation.setAcknowledgedAt(now);
                recommendation.setAcknowledgedBy(request.getUserId());
                break;

            case COMPLETED:
                recommendation.setStatus(CdsRecommendationEntity.CdsStatus.COMPLETED);
                recommendation.setCompletedAt(now);
                recommendation.setCompletedBy(request.getUserId());
                recommendation.setCompletionOutcome(request.getOutcome());
                break;

            case DECLINED:
                recommendation.setStatus(CdsRecommendationEntity.CdsStatus.DECLINED);
                recommendation.setDeclinedAt(now);
                recommendation.setDeclinedBy(request.getUserId());
                recommendation.setDeclineReason(request.getReason());
                break;

            case DISMISSED:
                recommendation.setStatus(CdsRecommendationEntity.CdsStatus.DISMISSED);
                recommendation.setDeclinedAt(now);
                recommendation.setDeclinedBy(request.getUserId());
                recommendation.setDeclineReason(request.getReason());
                break;

            case DEFERRED:
            case SNOOZED:
                // Keep current status but record the deferral
                break;

            default:
                // VIEWED and other actions don't change status
                break;
        }
    }

    /**
     * Get acknowledgment history for a recommendation
     */
    public List<CdsAcknowledgmentEntity> getAcknowledgmentHistory(UUID recommendationId) {
        return acknowledgmentRepository.findByRecommendationIdOrderByCreatedAtDesc(recommendationId);
    }
}
