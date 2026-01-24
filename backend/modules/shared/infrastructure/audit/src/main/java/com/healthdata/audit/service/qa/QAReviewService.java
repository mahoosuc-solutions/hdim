package com.healthdata.audit.service.qa;

import com.healthdata.audit.dto.qa.*;
import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.entity.QAReviewEntity;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.audit.repository.QAReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * QA Review Service
 * 
 * Business logic for QA review workflow management.
 * Handles approval, rejection, and flagging of AI decisions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QAReviewService {

    private final AIAgentDecisionEventRepository auditEventRepository;
    private final QAReviewRepository qaReviewRepository;

    /**
     * Get review queue with filtering and pagination
     */
    public Page<QADecisionReview> getReviewQueue(QAReviewFilter filter, Pageable pageable) {
        log.debug("Getting review queue with filter: {}", filter);

        // Query audit events based on filter
        Page<AIAgentDecisionEventEntity> events = auditEventRepository.findReviewQueue(
                filter.getTenantId(),
                filter.getAgentType(),
                filter.getMinConfidence(),
                filter.getMaxConfidence(),
                filter.getStartDate() != null ? filter.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant() : null,
                filter.getEndDate() != null ? filter.getEndDate().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant() : null,
                filter.isIncludeReviewed(),
                pageable
        );

        // Convert to DTOs
        List<QADecisionReview> reviews = events.getContent().stream()
                .map(this::toQADecisionReview)
                .collect(Collectors.toList());

        return new PageImpl<>(reviews, pageable, events.getTotalElements());
    }

    /**
     * Get detailed decision information
     */
    public QADecisionDetail getDecisionDetail(String tenantId, String decisionId) {
        log.debug("Getting decision detail for: {}", decisionId);

        AIAgentDecisionEventEntity event = auditEventRepository.findByDecisionIdAndTenantId(decisionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Decision not found: " + decisionId));

        // Get review if exists
        Optional<QAReviewEntity> review = qaReviewRepository.findByDecisionId(decisionId);

        // Get related decisions by correlation ID
        List<AIAgentDecisionEventEntity> relatedEvents = auditEventRepository.findByCorrelationIdOrderByTimestampAsc(event.getCorrelationId());

        return toQADecisionDetail(event, review.orElse(null), relatedEvents);
    }

    /**
     * Approve AI decision
     */
    public QAReviewResult approveDecision(String tenantId, String decisionId, QAReviewRequest request) {
        log.info("Approving decision: {} by: {}", decisionId, request.getReviewedBy());

        AIAgentDecisionEventEntity event = auditEventRepository.findByDecisionIdAndTenantId(decisionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Decision not found: " + decisionId));

        // Create or update review
        QAReviewEntity review = qaReviewRepository.findByDecisionId(decisionId)
                .orElse(new QAReviewEntity());

        review.setDecisionId(decisionId);
        review.setTenantId(tenantId);
        review.setReviewedBy(request.getReviewedBy());
        review.setReviewedAt(Instant.now());
        review.setReviewStatus("APPROVED");
        review.setReviewOutcome("APPROVED");
        review.setReviewNotes(request.getReviewNotes());
        review.setIsFalsePositive(request.getIsFalsePositive() != null ? request.getIsFalsePositive() : false);
        review.setIsFalseNegative(request.getIsFalseNegative() != null ? request.getIsFalseNegative() : false);

        qaReviewRepository.save(review);

        return QAReviewResult.builder()
                .decisionId(decisionId)
                .reviewStatus("APPROVED")
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(Instant.now())
                .outcome("APPROVED")
                .message("Decision approved successfully")
                .success(true)
                .build();
    }

    /**
     * Reject AI decision
     */
    public QAReviewResult rejectDecision(String tenantId, String decisionId, QAReviewRequest request) {
        log.info("Rejecting decision: {} by: {}", decisionId, request.getReviewedBy());

        AIAgentDecisionEventEntity event = auditEventRepository.findByDecisionIdAndTenantId(decisionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Decision not found: " + decisionId));

        // Create or update review
        QAReviewEntity review = qaReviewRepository.findByDecisionId(decisionId)
                .orElse(new QAReviewEntity());

        review.setDecisionId(decisionId);
        review.setTenantId(tenantId);
        review.setReviewedBy(request.getReviewedBy());
        review.setReviewedAt(Instant.now());
        review.setReviewStatus("REJECTED");
        review.setReviewOutcome("REJECTED");
        review.setReviewNotes(request.getReviewNotes());
        review.setIsFalsePositive(request.getIsFalsePositive() != null ? request.getIsFalsePositive() : false);
        review.setIsFalseNegative(request.getIsFalseNegative() != null ? request.getIsFalseNegative() : false);
        review.setCorrectDecisionType(request.getCorrectDecisionType());

        qaReviewRepository.save(review);

        return QAReviewResult.builder()
                .decisionId(decisionId)
                .reviewStatus("REJECTED")
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(Instant.now())
                .outcome("REJECTED")
                .message("Decision rejected successfully")
                .success(true)
                .build();
    }

    /**
     * Flag decision for additional review
     */
    public QAReviewResult flagDecision(String tenantId, String decisionId, QAFlagRequest request) {
        log.info("Flagging decision: {} as: {} by: {}", decisionId, request.getFlagType(), request.getReviewedBy());

        AIAgentDecisionEventEntity event = auditEventRepository.findByDecisionIdAndTenantId(decisionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Decision not found: " + decisionId));

        // Create or update review
        QAReviewEntity review = qaReviewRepository.findByDecisionId(decisionId)
                .orElse(new QAReviewEntity());

        review.setDecisionId(decisionId);
        review.setTenantId(tenantId);
        review.setReviewedBy(request.getReviewedBy());
        review.setReviewedAt(Instant.now());
        review.setReviewStatus("FLAGGED");
        review.setFlagType(request.getFlagType());
        review.setFlagReason(request.getFlagReason());
        review.setFlagPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        review.setResolutionStatus("PENDING");

        qaReviewRepository.save(review);

        return QAReviewResult.builder()
                .decisionId(decisionId)
                .reviewStatus("FLAGGED")
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(Instant.now())
                .outcome("FLAGGED")
                .message("Decision flagged for review: " + request.getFlagType())
                .success(true)
                .build();
    }

    /**
     * Get flagged decisions
     */
    public Page<QAFlaggedDecision> getFlaggedDecisions(String tenantId, String flagType, String agentType, Pageable pageable) {
        log.debug("Getting flagged decisions for tenant: {}", tenantId);

        Page<QAReviewEntity> flaggedReviews = qaReviewRepository.findFlagged(
                tenantId, flagType, agentType, pageable);

        List<QAFlaggedDecision> flagged = flaggedReviews.getContent().stream()
                .map(this::toQAFlaggedDecision)
                .collect(Collectors.toList());

        return new PageImpl<>(flagged, pageable, flaggedReviews.getTotalElements());
    }

    /**
     * Get QA metrics
     */
    public QAMetrics getMetrics(String tenantId, String agentType, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting QA metrics for tenant: {}", tenantId);

        Instant start = startDate != null ? startDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : 
                Instant.now().minus(30, ChronoUnit.DAYS);
        Instant end = endDate != null ? endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant() : 
                Instant.now();

        // Get all reviews in date range
        List<QAReviewEntity> reviews = qaReviewRepository.findByTenantIdAndDateRange(tenantId, start, end);

        // Calculate metrics
        long totalDecisions = reviews.size();
        long approved = reviews.stream().filter(r -> "APPROVED".equals(r.getReviewStatus())).count();
        long rejected = reviews.stream().filter(r -> "REJECTED".equals(r.getReviewStatus())).count();
        long flagged = reviews.stream().filter(r -> "FLAGGED".equals(r.getReviewStatus())).count();
        long pending = totalDecisions - approved - rejected - flagged;

        long falsePositives = reviews.stream().filter(QAReviewEntity::getIsFalsePositive).count();
        long falseNegatives = reviews.stream().filter(QAReviewEntity::getIsFalseNegative).count();

        double approvalRate = totalDecisions > 0 ? (double) approved / totalDecisions : 0.0;
        double rejectionRate = totalDecisions > 0 ? (double) rejected / totalDecisions : 0.0;
        double flagRate = totalDecisions > 0 ? (double) flagged / totalDecisions : 0.0;
        double accuracy = totalDecisions > 0 ? 
                (double) (totalDecisions - falsePositives - falseNegatives) / totalDecisions : 1.0;

        // Get average confidence and review time from events
        List<String> decisionIds = reviews.stream().map(QAReviewEntity::getDecisionId).collect(Collectors.toList());
        List<AIAgentDecisionEventEntity> events = auditEventRepository.findByDecisionIdIn(decisionIds);

        double averageConfidence = events.stream()
                .mapToDouble(e -> e.getConfidenceScore() != null ? e.getConfidenceScore() : 0.0)
                .average()
                .orElse(0.0);

        double averageReviewTime = reviews.stream()
                .filter(r -> r.getReviewedAt() != null)
                .mapToLong(r -> {
                    // Find corresponding event
                    AIAgentDecisionEventEntity event = events.stream()
                            .filter(e -> e.getEventId().toString().equals(r.getDecisionId()))
                            .findFirst()
                            .orElse(null);
                    if (event != null) {
                        return ChronoUnit.MINUTES.between(event.getTimestamp(), r.getReviewedAt());
                    }
                    return 0L;
                })
                .average()
                .orElse(0.0);

        // Confidence distribution
        long highConf = events.stream().filter(e -> e.getConfidenceScore() >= 0.9).count();
        long mediumConf = events.stream().filter(e -> e.getConfidenceScore() >= 0.7 && e.getConfidenceScore() < 0.9).count();
        long lowConf = events.stream().filter(e -> e.getConfidenceScore() < 0.7).count();

        ConfidenceDistribution distribution = ConfidenceDistribution.builder()
                .highConfidence(highConf)
                .mediumConfidence(mediumConf)
                .lowConfidence(lowConf)
                .build();

        // Calculate per-agent statistics
        Map<String, AgentStats> agentPerformance = calculatePerAgentStatistics(
            events, reviews, agentType);

        return QAMetrics.builder()
                .totalDecisions(totalDecisions)
                .pendingReview(pending)
                .approved(approved)
                .rejected(rejected)
                .flagged(flagged)
                .approvalRate(approvalRate)
                .rejectionRate(rejectionRate)
                .flagRate(flagRate)
                .falsePositives(falsePositives)
                .falseNegatives(falseNegatives)
                .accuracy(accuracy)
                .averageConfidence(averageConfidence)
                .averageReviewTimeMinutes(averageReviewTime)
                .confidenceDistribution(distribution)
                .agentPerformance(AgentPerformance.builder().byAgentType(agentPerformance).build())
                .build();
    }

    /**
     * Get accuracy trends
     */
    public QATrendData getAccuracyTrends(String tenantId, String agentType, int days) {
        log.debug("Getting accuracy trends for {} days", days);

        Instant start = Instant.now().minus(days, ChronoUnit.DAYS);
        List<QAReviewEntity> reviews = qaReviewRepository.findByTenantIdAndDateRange(tenantId, start, Instant.now());

        // Group by date
        Map<LocalDate, List<QAReviewEntity>> byDate = reviews.stream()
                .collect(Collectors.groupingBy(r -> 
                        r.getReviewedAt().atZone(ZoneId.systemDefault()).toLocalDate()));

        // Build daily trend points
        List<DailyTrendPoint> dailyTrends = byDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<QAReviewEntity> dayReviews = entry.getValue();

                    long total = dayReviews.size();
                    long approved = dayReviews.stream().filter(r -> "APPROVED".equals(r.getReviewStatus())).count();
                    long rejected = dayReviews.stream().filter(r -> "REJECTED".equals(r.getReviewStatus())).count();
                    long falsePos = dayReviews.stream().filter(QAReviewEntity::getIsFalsePositive).count();
                    long falseNeg = dayReviews.stream().filter(QAReviewEntity::getIsFalseNegative).count();

                    double accuracy = total > 0 ? (double) (total - falsePos - falseNeg) / total : 1.0;

            // Get average confidence from events
            List<String> decisionIds = dayReviews.stream().map(QAReviewEntity::getDecisionId).collect(Collectors.toList());
            List<AIAgentDecisionEventEntity> events = auditEventRepository.findByDecisionIdIn(decisionIds);
            double avgConfidence = events.stream()
                    .mapToDouble(e -> e.getConfidenceScore() != null ? e.getConfidenceScore() : 0.0)
                    .average()
                    .orElse(0.0);                    return DailyTrendPoint.builder()
                            .date(date)
                            .totalDecisions(total)
                            .approved(approved)
                            .rejected(rejected)
                            .accuracy(accuracy)
                            .averageConfidence(avgConfidence)
                            .build();
                })
                .sorted(Comparator.comparing(DailyTrendPoint::getDate))
                .collect(Collectors.toList());

        // Calculate per-agent trends
        // Get events for all reviews to calculate trends
        List<String> allDecisionIds = reviews.stream()
                .map(QAReviewEntity::getDecisionId)
                .collect(Collectors.toList());
        List<AIAgentDecisionEventEntity> allEvents = auditEventRepository.findByDecisionIdIn(allDecisionIds);
        
        Map<String, List<DailyTrendPoint>> perAgentTrends = calculatePerAgentTrends(
            reviews, allEvents, agentType, days);

        return QATrendData.builder()
                .dailyTrends(dailyTrends)
                .byAgentType(perAgentTrends)
                .build();
    }

    /**
     * Calculate per-agent statistics.
     * Groups events and reviews by agent type and calculates metrics for each.
     */
    private Map<String, AgentStats> calculatePerAgentStatistics(
            List<AIAgentDecisionEventEntity> events,
            List<QAReviewEntity> reviews,
            String filterAgentType) {
        
        Map<String, AgentStats> agentStats = new HashMap<>();
        
        // Group events by agent type
        Map<String, List<AIAgentDecisionEventEntity>> eventsByAgent = events.stream()
                .filter(e -> filterAgentType == null || 
                    (e.getAgentType() != null && e.getAgentType().name().equals(filterAgentType)))
                .filter(e -> e.getAgentType() != null)
                .collect(Collectors.groupingBy(e -> e.getAgentType().name()));
        
        // Group reviews by decision ID for quick lookup
        Map<String, QAReviewEntity> reviewsByDecisionId = reviews.stream()
                .collect(Collectors.toMap(QAReviewEntity::getDecisionId, r -> r, (r1, r2) -> r1));
        
        // Calculate stats for each agent type
        for (Map.Entry<String, List<AIAgentDecisionEventEntity>> entry : eventsByAgent.entrySet()) {
            String agentTypeName = entry.getKey();
            List<AIAgentDecisionEventEntity> agentEvents = entry.getValue();
            
            // Get reviews for these events
            List<QAReviewEntity> agentReviews = agentEvents.stream()
                    .map(e -> reviewsByDecisionId.get(e.getEventId().toString()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // Calculate metrics
            long totalDecisions = agentEvents.size();
            long approved = agentReviews.stream()
                    .filter(r -> "APPROVED".equals(r.getReviewStatus()))
                    .count();
            long rejected = agentReviews.stream()
                    .filter(r -> "REJECTED".equals(r.getReviewStatus()))
                    .count();
            
            double approvalRate = totalDecisions > 0 
                ? (double) approved / totalDecisions : 0.0;
            
            double averageConfidence = agentEvents.stream()
                    .mapToDouble(e -> e.getConfidenceScore() != null ? e.getConfidenceScore() : 0.0)
                    .average()
                    .orElse(0.0);
            
            // Calculate accuracy (1 - false positive rate - false negative rate)
            long falsePositives = agentReviews.stream()
                    .filter(QAReviewEntity::getIsFalsePositive)
                    .count();
            long falseNegatives = agentReviews.stream()
                    .filter(QAReviewEntity::getIsFalseNegative)
                    .count();
            double accuracy = totalDecisions > 0 
                ? (double) (totalDecisions - falsePositives - falseNegatives) / totalDecisions 
                : 1.0;
            
            AgentStats stats = AgentStats.builder()
                    .totalDecisions(totalDecisions)
                    .approved(approved)
                    .rejected(rejected)
                    .approvalRate(approvalRate)
                    .averageConfidence(averageConfidence)
                    .accuracy(accuracy)
                    .build();
            
            agentStats.put(agentTypeName, stats);
        }
        
        return agentStats;
    }

    /**
     * Calculate per-agent trends over time.
     */
    private Map<String, List<DailyTrendPoint>> calculatePerAgentTrends(
            List<QAReviewEntity> reviews,
            List<AIAgentDecisionEventEntity> events,
            String filterAgentType,
            int days) {
        
        Map<String, List<DailyTrendPoint>> perAgentTrends = new HashMap<>();
        
        // Group events by agent type
        Map<String, List<AIAgentDecisionEventEntity>> eventsByAgent = events.stream()
                .filter(e -> filterAgentType == null || 
                    (e.getAgentType() != null && e.getAgentType().name().equals(filterAgentType)))
                .filter(e -> e.getAgentType() != null)
                .collect(Collectors.groupingBy(e -> e.getAgentType().name()));
        
        // Group reviews by decision ID
        Map<String, QAReviewEntity> reviewsByDecisionId = reviews.stream()
                .collect(Collectors.toMap(QAReviewEntity::getDecisionId, r -> r, (r1, r2) -> r1));
        
        // Calculate trends for each agent type
        for (Map.Entry<String, List<AIAgentDecisionEventEntity>> entry : eventsByAgent.entrySet()) {
            String agentTypeName = entry.getKey();
            List<AIAgentDecisionEventEntity> agentEvents = entry.getValue();
            
            // Group events by review date
            Map<LocalDate, List<AIAgentDecisionEventEntity>> eventsByDate = agentEvents.stream()
                    .filter(e -> {
                        QAReviewEntity review = reviewsByDecisionId.get(e.getEventId().toString());
                        return review != null && review.getReviewedAt() != null;
                    })
                    .collect(Collectors.groupingBy(e -> {
                        QAReviewEntity review = reviewsByDecisionId.get(e.getEventId().toString());
                        return review.getReviewedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                    }));
            
            // Build daily trend points for this agent
            List<DailyTrendPoint> agentTrends = eventsByDate.entrySet().stream()
                    .map(dateEntry -> {
                        LocalDate date = dateEntry.getKey();
                        List<AIAgentDecisionEventEntity> dayEvents = dateEntry.getValue();
                        
                        List<QAReviewEntity> dayReviews = dayEvents.stream()
                                .map(e -> reviewsByDecisionId.get(e.getEventId().toString()))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        
                        long total = dayEvents.size();
                        long approved = dayReviews.stream()
                                .filter(r -> "APPROVED".equals(r.getReviewStatus()))
                                .count();
                        long rejected = dayReviews.stream()
                                .filter(r -> "REJECTED".equals(r.getReviewStatus()))
                                .count();
                        long falsePos = dayReviews.stream()
                                .filter(QAReviewEntity::getIsFalsePositive)
                                .count();
                        long falseNeg = dayReviews.stream()
                                .filter(QAReviewEntity::getIsFalseNegative)
                                .count();
                        
                        double accuracy = total > 0 
                            ? (double) (total - falsePos - falseNeg) / total : 1.0;
                        
                        double avgConfidence = dayEvents.stream()
                                .mapToDouble(e -> e.getConfidenceScore() != null ? e.getConfidenceScore() : 0.0)
                                .average()
                                .orElse(0.0);
                        
                        return DailyTrendPoint.builder()
                                .date(date)
                                .totalDecisions(total)
                                .approved(approved)
                                .rejected(rejected)
                                .accuracy(accuracy)
                                .averageConfidence(avgConfidence)
                                .build();
                    })
                    .sorted(Comparator.comparing(DailyTrendPoint::getDate))
                    .collect(Collectors.toList());
            
            perAgentTrends.put(agentTypeName, agentTrends);
        }
        
        return perAgentTrends;
    }

    /**
     * Batch approve decisions
     */
    public BatchReviewResult batchApprove(String tenantId, BatchReviewRequest request) {
        log.info("Batch approving {} decisions", request.getDecisionIds().size());

        List<String> successful = new ArrayList<>();
        List<BatchReviewError> errors = new ArrayList<>();

        for (String decisionId : request.getDecisionIds()) {
            try {
                QAReviewRequest reviewRequest = QAReviewRequest.builder()
                        .reviewedBy(request.getReviewedBy())
                        .reviewNotes(request.getReviewNotes())
                        .build();
                approveDecision(tenantId, decisionId, reviewRequest);
                successful.add(decisionId);
            } catch (Exception e) {
                errors.add(BatchReviewError.builder()
                        .decisionId(decisionId)
                        .errorMessage(e.getMessage())
                        .errorCode("APPROVAL_FAILED")
                        .build());
            }
        }

        return BatchReviewResult.builder()
                .totalRequested(request.getDecisionIds().size())
                .successful(successful.size())
                .failed(errors.size())
                .successfulIds(successful)
                .errors(errors)
                .build();
    }

    /**
     * Batch reject decisions
     */
    public BatchReviewResult batchReject(String tenantId, BatchReviewRequest request) {
        log.info("Batch rejecting {} decisions", request.getDecisionIds().size());

        List<String> successful = new ArrayList<>();
        List<BatchReviewError> errors = new ArrayList<>();

        for (String decisionId : request.getDecisionIds()) {
            try {
                QAReviewRequest reviewRequest = QAReviewRequest.builder()
                        .reviewedBy(request.getReviewedBy())
                        .reviewNotes(request.getReviewNotes())
                        .build();
                rejectDecision(tenantId, decisionId, reviewRequest);
                successful.add(decisionId);
            } catch (Exception e) {
                errors.add(BatchReviewError.builder()
                        .decisionId(decisionId)
                        .errorMessage(e.getMessage())
                        .errorCode("REJECTION_FAILED")
                        .build());
            }
        }

        return BatchReviewResult.builder()
                .totalRequested(request.getDecisionIds().size())
                .successful(successful.size())
                .failed(errors.size())
                .successfulIds(successful)
                .errors(errors)
                .build();
    }

    // Conversion methods
    
    private QADecisionReview toQADecisionReview(AIAgentDecisionEventEntity event) {
        String decisionId = event.getEventId().toString();
        // Get review status if exists
        Optional<QAReviewEntity> review = qaReviewRepository.findByDecisionId(decisionId);
        String reviewStatus = review.map(QAReviewEntity::getReviewStatus).orElse("PENDING");

        // Calculate time to review
        Integer timeToReview = null;
        if (review.isPresent() && review.get().getReviewedAt() != null) {
            timeToReview = (int) ChronoUnit.MINUTES.between(event.getTimestamp(), review.get().getReviewedAt());
        }

        // Determine priority from confidence score
        String priority = determinePriority(event.getConfidenceScore());

        return QADecisionReview.builder()
                .decisionId(decisionId)
                .agentType(event.getAgentType() != null ? event.getAgentType().name() : "UNKNOWN")
                .decisionType(event.getDecisionType() != null ? event.getDecisionType().name() : "UNKNOWN")
                .timestamp(event.getTimestamp())
                .priority(priority)
                .confidenceScore(event.getConfidenceScore())
                .patientId(event.getResourceId())
                .customerId(event.getResourceId())
                .recommendation(event.getInputMetrics())
                .reviewStatus(reviewStatus)
                .timeToReviewMinutes(timeToReview)
                .build();
    }

    private QADecisionDetail toQADecisionDetail(AIAgentDecisionEventEntity event, QAReviewEntity review, List<AIAgentDecisionEventEntity> relatedEvents) {
        String decisionId = event.getEventId().toString();
        String priority = determinePriority(event.getConfidenceScore());

        List<RelatedDecision> related = relatedEvents.stream()
                .filter(e -> !e.getEventId().equals(event.getEventId()))
                .map(e -> {
                    String relatedId = e.getEventId().toString();
                    Optional<QAReviewEntity> relatedReview = qaReviewRepository.findByDecisionId(relatedId);
                    return RelatedDecision.builder()
                            .decisionId(relatedId)
                            .agentType(e.getAgentType() != null ? e.getAgentType().name() : "UNKNOWN")
                            .decisionType(e.getDecisionType() != null ? e.getDecisionType().name() : "UNKNOWN")
                            .timestamp(e.getTimestamp().toString())
                            .reviewStatus(relatedReview.map(QAReviewEntity::getReviewStatus).orElse("PENDING"))
                            .build();
                })
                .collect(Collectors.toList());

        Map<String, Object> customerProfile = new HashMap<>();
        customerProfile.put("customerId", event.getResourceId());
        customerProfile.put("customerTier", event.getCustomerTier());
        customerProfile.put("patientCount", event.getPatientCount());

        List<String> reasoning = event.getReasoning() != null ? 
                Arrays.asList(event.getReasoning().split("\\n")) : new ArrayList<>();

        Map<String, Object> performanceMetrics = new HashMap<>();
        performanceMetrics.put("inferenceTimeMs", event.getInferenceTimeMs());
        performanceMetrics.put("tokenCount", event.getTokenCount());
        performanceMetrics.put("costEstimate", event.getCostEstimate());

        return QADecisionDetail.builder()
                .decisionId(decisionId)
                .agentType(event.getAgentType() != null ? event.getAgentType().name() : "UNKNOWN")
                .agentVersion(event.getAgentVersion())
                .decisionType(event.getDecisionType() != null ? event.getDecisionType().name() : "UNKNOWN")
                .timestamp(event.getTimestamp())
                .priority(priority)
                .confidenceScore(event.getConfidenceScore())
                .customerProfile(customerProfile)
                .patientId(event.getResourceId())
                .customerId(event.getResourceId())
                .inputParameters(event.getInputMetrics())
                .recommendation(event.getInputMetrics())
                .reasoning(reasoning)
                .performanceMetrics(performanceMetrics)
                .reviewStatus(review != null ? review.getReviewStatus() : "PENDING")
                .reviewedBy(review != null ? review.getReviewedBy() : null)
                .reviewedAt(review != null ? review.getReviewedAt() : null)
                .reviewNotes(review != null ? review.getReviewNotes() : null)
                .reviewOutcome(review != null ? review.getReviewOutcome() : null)
                .correlationId(event.getCorrelationId())
                .relatedDecisions(related)
                .build();
    }

    private QAFlaggedDecision toQAFlaggedDecision(QAReviewEntity review) {
        // Get the original event
        AIAgentDecisionEventEntity event = auditEventRepository.findByDecisionId(review.getDecisionId())
                .orElse(null);

        return QAFlaggedDecision.builder()
                .decisionId(review.getDecisionId())
                .agentType(event != null && event.getAgentType() != null ? event.getAgentType().name() : "UNKNOWN")
                .decisionType(event != null && event.getDecisionType() != null ? event.getDecisionType().name() : "UNKNOWN")
                .timestamp(event != null ? event.getTimestamp() : review.getReviewedAt())
                .confidenceScore(event != null && event.getConfidenceScore() != null ? event.getConfidenceScore() : 0.0)
                .flagType(review.getFlagType())
                .flagReason(review.getFlagReason())
                .flaggedBy(review.getReviewedBy())
                .flaggedAt(review.getReviewedAt())
                .priority(review.getFlagPriority())
                .resolutionStatus(review.getResolutionStatus())
                .build();
    }

    private String determinePriority(Double confidenceScore) {
        if (confidenceScore == null) return "MEDIUM";
        if (confidenceScore < 0.6) return "CRITICAL";
        if (confidenceScore < 0.7) return "HIGH";
        if (confidenceScore < 0.85) return "MEDIUM";
        return "LOW";
    }

    /**
     * Mark decision as false positive
     *
     * A false positive is when the AI incorrectly flagged something as positive
     * when it should have been negative (e.g., flagged a care gap that doesn't exist)
     */
    public QAReviewResult markFalsePositive(String tenantId, String decisionId, QAReviewRequest request) {
        log.info("Marking decision {} as false positive by: {}", decisionId, request.getReviewedBy());

        AIAgentDecisionEventEntity event = auditEventRepository.findByDecisionIdAndTenantId(decisionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Decision not found: " + decisionId));

        // Create or update review
        QAReviewEntity review = qaReviewRepository.findByDecisionId(decisionId)
                .orElse(new QAReviewEntity());

        review.setDecisionId(decisionId);
        review.setTenantId(tenantId);
        review.setReviewedBy(request.getReviewedBy());
        review.setReviewedAt(Instant.now());
        review.setReviewStatus("REJECTED");  // False positives are rejected
        review.setIsFalsePositive(true);
        review.setIsFalseNegative(false);
        review.setReviewNotes(request.getReviewNotes());

        QAReviewEntity saved = qaReviewRepository.save(review);

        log.info("Decision {} marked as false positive successfully", decisionId);

        return QAReviewResult.builder()
                .success(true)
                .decisionId(decisionId)
                .reviewStatus(saved.getReviewStatus())
                .reviewedAt(saved.getReviewedAt())
                .reviewedBy(saved.getReviewedBy())
                .build();
    }

    /**
     * Mark decision as false negative
     *
     * A false negative is when the AI missed a detection - it should have flagged
     * something but didn't (e.g., missed a care gap that actually exists)
     */
    public QAReviewResult markFalseNegative(String tenantId, String decisionId, QAReviewRequest request) {
        log.info("Marking decision {} as false negative by: {}", decisionId, request.getReviewedBy());

        AIAgentDecisionEventEntity event = auditEventRepository.findByDecisionIdAndTenantId(decisionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Decision not found: " + decisionId));

        // Create or update review
        QAReviewEntity review = qaReviewRepository.findByDecisionId(decisionId)
                .orElse(new QAReviewEntity());

        review.setDecisionId(decisionId);
        review.setTenantId(tenantId);
        review.setReviewedBy(request.getReviewedBy());
        review.setReviewedAt(Instant.now());
        review.setReviewStatus("FLAGGED");  // False negatives are flagged for correction
        review.setIsFalsePositive(false);
        review.setIsFalseNegative(true);
        review.setReviewNotes(request.getReviewNotes());

        QAReviewEntity saved = qaReviewRepository.save(review);

        log.info("Decision {} marked as false negative successfully", decisionId);

        return QAReviewResult.builder()
                .success(true)
                .decisionId(decisionId)
                .reviewStatus(saved.getReviewStatus())
                .reviewedAt(saved.getReviewedAt())
                .reviewedBy(saved.getReviewedBy())
                .build();
    }

    /**
     * Export comprehensive QA report
     *
     * Generates a comprehensive report including metrics, trends, and detailed
     * decision history for the specified time period.
     */
    public QAReportExport exportReport(String tenantId, String agentType, LocalDate startDate, LocalDate endDate, String format) {
        log.info("Exporting QA report for tenant: {} from {} to {}", tenantId, startDate, endDate);

        // Get metrics
        QAMetrics metrics = getMetrics(tenantId, agentType, startDate, endDate);

        // Get trends
        int days = startDate != null && endDate != null
                ? (int) ChronoUnit.DAYS.between(startDate, endDate)
                : 30;
        QATrendData trends = getAccuracyTrends(tenantId, agentType, days);

        // Get all reviewed decisions in date range
        Instant start = startDate != null
                ? startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant end = endDate != null
                ? endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        List<QAReviewEntity> reviews = qaReviewRepository.findByTenantIdAndReviewedAtBetween(
                tenantId, start, end);

        // Filter by agent type if specified
        if (agentType != null && !agentType.isBlank()) {
            reviews = reviews.stream()
                    .filter(review -> {
                        AIAgentDecisionEventEntity event = auditEventRepository
                                .findByDecisionIdAndTenantId(review.getDecisionId(), tenantId)
                                .orElse(null);
                        return event != null && agentType.equals(event.getAgentType());
                    })
                    .collect(Collectors.toList());
        }

        // Build export
        return QAReportExport.builder()
                .generatedAt(Instant.now())
                .tenantId(tenantId)
                .agentType(agentType)
                .startDate(startDate)
                .endDate(endDate)
                .format(format)
                .metrics(metrics)
                .trends(trends)
                .totalReviews(reviews.size())
                .approvedCount(reviews.stream().filter(r -> "APPROVED".equals(r.getReviewStatus())).count())
                .rejectedCount(reviews.stream().filter(r -> "REJECTED".equals(r.getReviewStatus())).count())
                .flaggedCount(reviews.stream().filter(r -> "FLAGGED".equals(r.getReviewStatus())).count())
                .falsePositiveCount(reviews.stream().filter(QAReviewEntity::getIsFalsePositive).count())
                .falseNegativeCount(reviews.stream().filter(QAReviewEntity::getIsFalseNegative).count())
                .build();
    }
}
