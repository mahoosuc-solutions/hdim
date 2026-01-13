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

        AIAuditEvent event = auditEventRepository.findByDecisionIdAndTenantId(decisionId, tenantId)
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

        AIAuditEvent event = auditEventRepository.findByDecisionIdAndTenantId(decisionId, tenantId)
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

        AIAuditEvent event = auditEventRepository.findByDecisionIdAndTenantId(decisionId, tenantId)
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

        // Agent performance (simplified - would need more complex query in production)
        Map<String, AgentStats> agentPerformance = new HashMap<>();
        // TODO: Implement per-agent statistics

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

        return QATrendData.builder()
                .dailyTrends(dailyTrends)
                .byAgentType(new HashMap<>()) // TODO: Implement per-agent trends
                .build();
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
}
