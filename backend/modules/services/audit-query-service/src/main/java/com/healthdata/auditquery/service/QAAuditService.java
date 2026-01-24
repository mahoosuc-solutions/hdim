package com.healthdata.auditquery.service;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.auditquery.dto.qa.QAMetricsResponse;
import com.healthdata.auditquery.dto.qa.QAReviewQueueResponse;
import com.healthdata.auditquery.dto.qa.QAReviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for QA Audit operations.
 *
 * <p>Provides business logic for reviewing AI decisions, calculating QA metrics,
 * and exporting QA audit reports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QAAuditService {

    private final AIAgentDecisionEventRepository decisionRepository;

    /**
     * Get QA review queue with filtering.
     */
    @Transactional(readOnly = true)
    public Page<QAReviewQueueResponse> getReviewQueue(
        String tenantId,
        String agentType,
        Double minConfidence,
        Double maxConfidence,
        Instant startDate,
        Instant endDate,
        Boolean includeReviewed,
        int page,
        int size
    ) {
        // Default date range: last 30 days
        if (startDate == null) {
            startDate = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endDate == null) {
            endDate = Instant.now();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        // Fetch decisions
        Page<AIAgentDecisionEventEntity> decisions = decisionRepository
            .findByTenantIdAndTimestampBetween(tenantId, startDate, endDate, pageable);

        // Apply filters
        Page<AIAgentDecisionEventEntity> filteredDecisions = decisions.map(decision -> {
            // Filter by agent type
            if (agentType != null && !decision.getAgentType().name().equals(agentType)) {
                return null;
            }

            // Filter by confidence score
            if (minConfidence != null && decision.getConfidenceScore() < minConfidence) {
                return null;
            }
            if (maxConfidence != null && decision.getConfidenceScore() > maxConfidence) {
                return null;
            }

            // Filter by review status
            if (!includeReviewed && decision.getReviewStatus() != null && !"PENDING".equals(decision.getReviewStatus())) {
                return null;
            }

            return decision;
        });

        // Map to response DTOs
        return filteredDecisions.map(this::mapToQAReviewQueueResponse);
    }

    /**
     * Get QA audit metrics.
     */
    @Transactional(readOnly = true)
    public QAMetricsResponse getQAMetrics(String tenantId, Instant startDate, Instant endDate) {
        // Default date range: last 30 days
        if (startDate == null) {
            startDate = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endDate == null) {
            endDate = Instant.now();
        }

        List<AIAgentDecisionEventEntity> decisions = decisionRepository
            .findByTenantIdAndTimestampBetween(tenantId, startDate, endDate, Pageable.unpaged())
            .getContent();

        long totalReviewed = decisions.stream()
            .filter(d -> d.getReviewStatus() != null && !"PENDING".equals(d.getReviewStatus()))
            .count();

        long approvedDecisions = decisions.stream()
            .filter(d -> "APPROVED".equals(d.getReviewStatus()))
            .count();

        long rejectedDecisions = decisions.stream()
            .filter(d -> "REJECTED".equals(d.getReviewStatus()))
            .count();

        long flaggedDecisions = decisions.stream()
            .filter(d -> "FLAGGED".equals(d.getReviewStatus()))
            .count();

        // Note: falsePositive/falseNegative fields not available in entity
        long falsePositives = 0L;
        long falseNegatives = 0L;

        long pendingReview = decisions.stream()
            .filter(d -> d.getReviewStatus() == null || "PENDING".equals(d.getReviewStatus()))
            .count();

        double averageConfidenceScore = decisions.stream()
            .filter(d -> d.getConfidenceScore() != null)
            .mapToDouble(AIAgentDecisionEventEntity::getConfidenceScore)
            .average()
            .orElse(0.0);

        long highConfidenceCount = decisions.stream()
            .filter(d -> d.getConfidenceScore() != null && d.getConfidenceScore() > 0.8)
            .count();

        long mediumConfidenceCount = decisions.stream()
            .filter(d -> d.getConfidenceScore() != null && d.getConfidenceScore() >= 0.5 && d.getConfidenceScore() <= 0.8)
            .count();

        long lowConfidenceCount = decisions.stream()
            .filter(d -> d.getConfidenceScore() != null && d.getConfidenceScore() < 0.5)
            .count();

        double approvalRate = totalReviewed > 0 ? (double) approvedDecisions / totalReviewed : 0.0;
        double falsePositiveRate = totalReviewed > 0 ? (double) falsePositives / totalReviewed : 0.0;
        double falseNegativeRate = totalReviewed > 0 ? (double) falseNegatives / totalReviewed : 0.0;

        return QAMetricsResponse.builder()
            .totalReviewed(totalReviewed)
            .approvedDecisions(approvedDecisions)
            .rejectedDecisions(rejectedDecisions)
            .flaggedDecisions(flaggedDecisions)
            .falsePositives(falsePositives)
            .falseNegatives(falseNegatives)
            .pendingReview(pendingReview)
            .averageConfidenceScore(averageConfidenceScore)
            .highConfidenceCount(highConfidenceCount)
            .mediumConfidenceCount(mediumConfidenceCount)
            .lowConfidenceCount(lowConfidenceCount)
            .approvalRate(approvalRate)
            .falsePositiveRate(falsePositiveRate)
            .falseNegativeRate(falseNegativeRate)
            .build();
    }

    /**
     * Get QA trend data over time.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTrendData(String tenantId, Instant startDate, Instant endDate) {
        // Default date range: last 30 days
        if (startDate == null) {
            startDate = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endDate == null) {
            endDate = Instant.now();
        }

        List<AIAgentDecisionEventEntity> decisions = decisionRepository
            .findByTenantIdAndTimestampBetween(tenantId, startDate, endDate, Pageable.unpaged())
            .getContent();

        // Group by day and calculate metrics
        Map<String, Long> dailyReviewCounts = decisions.stream()
            .filter(d -> d.getReviewedAt() != null)
            .collect(Collectors.groupingBy(
                d -> d.getReviewedAt().truncatedTo(ChronoUnit.DAYS).toString(),
                Collectors.counting()
            ));

        Map<String, Double> dailyApprovalRates = new HashMap<>();
        Map<String, Double> dailyConfidenceScores = new HashMap<>();

        // Calculate daily metrics
        decisions.stream()
            .collect(Collectors.groupingBy(d -> d.getTimestamp().truncatedTo(ChronoUnit.DAYS).toString()))
            .forEach((day, dayDecisions) -> {
                long approved = dayDecisions.stream().filter(d -> "APPROVED".equals(d.getReviewStatus())).count();
                long total = dayDecisions.stream().filter(d -> d.getReviewStatus() != null).count();
                dailyApprovalRates.put(day, total > 0 ? (double) approved / total : 0.0);

                double avgConfidence = dayDecisions.stream()
                    .filter(d -> d.getConfidenceScore() != null)
                    .mapToDouble(AIAgentDecisionEventEntity::getConfidenceScore)
                    .average()
                    .orElse(0.0);
                dailyConfidenceScores.put(day, avgConfidence);
            });

        Map<String, Object> trendData = new HashMap<>();
        trendData.put("dailyReviewCounts", dailyReviewCounts);
        trendData.put("dailyApprovalRates", dailyApprovalRates);
        trendData.put("dailyConfidenceScores", dailyConfidenceScores);

        return trendData;
    }

    /**
     * Approve an AI decision.
     */
    @Transactional
    public void approveDecision(String tenantId, UUID id, QAReviewRequest request, String reviewerUserId) {
        AIAgentDecisionEventEntity decision = decisionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("AI decision not found: " + id));

        if (!tenantId.equals(decision.getTenantId())) {
            throw new RuntimeException("Access denied: Decision belongs to different tenant");
        }

        decision.setReviewStatus("APPROVED");
        decision.setReviewedBy(reviewerUserId);
        decision.setReviewedAt(Instant.now());
        decision.setReviewNotes(request.getReviewNotes());

        decisionRepository.save(decision);

        log.info("AI decision {} approved by {}", id, reviewerUserId);
    }

    /**
     * Reject an AI decision.
     */
    @Transactional
    public void rejectDecision(String tenantId, UUID id, QAReviewRequest request, String reviewerUserId) {
        AIAgentDecisionEventEntity decision = decisionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("AI decision not found: " + id));

        if (!tenantId.equals(decision.getTenantId())) {
            throw new RuntimeException("Access denied: Decision belongs to different tenant");
        }

        decision.setReviewStatus("REJECTED");
        decision.setReviewedBy(reviewerUserId);
        decision.setReviewedAt(Instant.now());
        decision.setReviewNotes(request.getReviewNotes());
        if (request.getRejectionReason() != null) {
            decision.setReviewNotes(
                (decision.getReviewNotes() != null ? decision.getReviewNotes() + "\n\n" : "") +
                "Rejection Reason: " + request.getRejectionReason()
            );
        }

        decisionRepository.save(decision);

        log.info("AI decision {} rejected by {}", id, reviewerUserId);
    }

    /**
     * Flag an AI decision for manual review.
     */
    @Transactional
    public void flagDecision(String tenantId, UUID id, QAReviewRequest request, String reviewerUserId) {
        AIAgentDecisionEventEntity decision = decisionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("AI decision not found: " + id));

        if (!tenantId.equals(decision.getTenantId())) {
            throw new RuntimeException("Access denied: Decision belongs to different tenant");
        }

        decision.setReviewStatus("FLAGGED");
        decision.setReviewedBy(reviewerUserId);
        decision.setReviewedAt(Instant.now());
        decision.setReviewNotes(request.getReviewNotes());
        if (request.getFlagReason() != null) {
            decision.setReviewNotes(
                (decision.getReviewNotes() != null ? decision.getReviewNotes() + "\n\n" : "") +
                "Flag Reason: " + request.getFlagReason()
            );
        }

        decisionRepository.save(decision);

        log.info("AI decision {} flagged by {}", id, reviewerUserId);
    }

    /**
     * Mark an AI decision as false positive.
     */
    @Transactional
    public void markFalsePositive(String tenantId, UUID id, QAReviewRequest request, String reviewerUserId) {
        AIAgentDecisionEventEntity decision = decisionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("AI decision not found: " + id));

        if (!tenantId.equals(decision.getTenantId())) {
            throw new RuntimeException("Access denied: Decision belongs to different tenant");
        }

        // Note: falsePositive field not available in entity, using userFeedbackComment instead
        decision.setReviewedBy(reviewerUserId);
        decision.setReviewedAt(Instant.now());
        if (request.getFalsePositiveContext() != null) {
            decision.setUserFeedbackComment("FALSE_POSITIVE: " + request.getFalsePositiveContext());
        }

        decisionRepository.save(decision);

        log.info("AI decision {} marked as false positive by {}", id, reviewerUserId);
    }

    /**
     * Mark an AI decision as false negative.
     */
    @Transactional
    public void markFalseNegative(String tenantId, UUID id, QAReviewRequest request, String reviewerUserId) {
        AIAgentDecisionEventEntity decision = decisionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("AI decision not found: " + id));

        if (!tenantId.equals(decision.getTenantId())) {
            throw new RuntimeException("Access denied: Decision belongs to different tenant");
        }

        // Note: falseNegative field not available in entity, using userFeedbackComment instead
        decision.setReviewedBy(reviewerUserId);
        decision.setReviewedAt(Instant.now());
        if (request.getFalseNegativeContext() != null) {
            decision.setUserFeedbackComment("FALSE_NEGATIVE: " + request.getFalseNegativeContext());
        }

        decisionRepository.save(decision);

        log.info("AI decision {} marked as false negative by {}", id, reviewerUserId);
    }

    /**
     * Get review detail for a specific AI decision.
     */
    @Transactional(readOnly = true)
    public Optional<QAReviewQueueResponse> getReviewDetail(String tenantId, UUID id) {
        return decisionRepository.findById(id)
            .filter(d -> tenantId.equals(d.getTenantId()))
            .map(this::mapToQAReviewQueueResponse);
    }

    /**
     * Export QA audit report to Excel.
     */
    @Transactional(readOnly = true)
    public byte[] exportQAReport(String tenantId, Instant startDate, Instant endDate) {
        // Default date range: last 30 days
        if (startDate == null) {
            startDate = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endDate == null) {
            endDate = Instant.now();
        }

        List<AIAgentDecisionEventEntity> decisions = decisionRepository
            .findByTenantIdAndTimestampBetween(tenantId, startDate, endDate, Pageable.unpaged())
            .getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("QA Audit Report");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Event ID", "Timestamp", "Agent Type", "Decision Type", "Outcome",
                "Confidence Score", "Review Status", "Reviewed By", "Reviewed At",
                "Review Notes", "False Positive", "False Negative"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (AIAgentDecisionEventEntity decision : decisions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(decision.getEventId().toString());
                row.createCell(1).setCellValue(decision.getTimestamp().toString());
                row.createCell(2).setCellValue(decision.getAgentType() != null ? decision.getAgentType().name() : "");
                row.createCell(3).setCellValue(decision.getDecisionType() != null ? decision.getDecisionType().name() : "");
                row.createCell(4).setCellValue(decision.getOutcome() != null ? decision.getOutcome().name() : "");
                row.createCell(5).setCellValue(decision.getConfidenceScore() != null ? decision.getConfidenceScore() : 0.0);
                row.createCell(6).setCellValue(decision.getReviewStatus() != null ? decision.getReviewStatus() : "PENDING");
                row.createCell(7).setCellValue(decision.getReviewedBy() != null ? decision.getReviewedBy() : "");
                row.createCell(8).setCellValue(decision.getReviewedAt() != null ? decision.getReviewedAt().toString() : "");
                row.createCell(9).setCellValue(decision.getReviewNotes() != null ? decision.getReviewNotes() : "");
                // Note: falsePositive/falseNegative fields not available in entity
                row.createCell(10).setCellValue("N/A");
                row.createCell(11).setCellValue("N/A");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate QA audit report", e);
        }
    }

    /**
     * Map entity to response DTO.
     */
    private QAReviewQueueResponse mapToQAReviewQueueResponse(AIAgentDecisionEventEntity entity) {
        if (entity == null) {
            return null;
        }

        // Build output data map from recommendation fields
        Map<String, Object> outputData = null;
        if (entity.getRecommendedValue() != null || entity.getExpectedImpact() != null) {
            outputData = new java.util.HashMap<>();
            if (entity.getConfigType() != null) {
                outputData.put("configType", entity.getConfigType());
            }
            if (entity.getCurrentValue() != null) {
                outputData.put("currentValue", entity.getCurrentValue());
            }
            if (entity.getRecommendedValue() != null) {
                outputData.put("recommendedValue", entity.getRecommendedValue());
            }
            if (entity.getExpectedImpact() != null) {
                outputData.put("expectedImpact", entity.getExpectedImpact());
            }
            if (entity.getRiskLevel() != null) {
                outputData.put("riskLevel", entity.getRiskLevel().name());
            }
        }

        return QAReviewQueueResponse.builder()
            .eventId(entity.getEventId())
            .timestamp(entity.getTimestamp())
            .agentType(entity.getAgentType() != null ? entity.getAgentType().name() : null)
            .decisionType(entity.getDecisionType() != null ? entity.getDecisionType().name() : null)
            .outcome(entity.getOutcome() != null ? entity.getOutcome().name() : null)
            .confidenceScore(entity.getConfidenceScore())
            .reasoning(entity.getReasoning())
            .inputContext(entity.getInputMetrics())  // Use inputMetrics map
            .outputData(outputData)  // Build from recommendation fields
            .modelVersion(entity.getModelName())
            .qaReviewStatus(entity.getReviewStatus() != null ? entity.getReviewStatus() : "PENDING")
            .qaReviewedBy(entity.getReviewedBy())
            .qaReviewedAt(entity.getReviewedAt())
            .qaReviewNotes(entity.getReviewNotes())
            .falsePositive(null)  // Note: field not available in entity
            .falseNegative(null)  // Note: field not available in entity
            .relatedResourceId(entity.getResourceId())
            .relatedResourceType(entity.getResourceType())
            .build();
    }
}
