package com.healthdata.auditquery.service;

import com.healthdata.audit.entity.clinical.ClinicalDecisionEntity;
import com.healthdata.audit.repository.clinical.ClinicalDecisionRepository;
import com.healthdata.auditquery.dto.clinical.ClinicalDecisionResponse;
import com.healthdata.auditquery.dto.clinical.ClinicalMetricsResponse;
import com.healthdata.auditquery.dto.clinical.ClinicalReviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Clinical Audit Dashboard operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalAuditService {

    private final ClinicalDecisionRepository clinicalDecisionRepository;

    /**
     * Fetch clinical AI decisions for review with optional filtering.
     */
    @Transactional(readOnly = true)
    public Page<ClinicalDecisionResponse> getClinicalDecisions(
        String tenantId,
        String agentType,  // Note: agentType not stored in entity, parameter ignored
        String decisionType,
        String alertSeverity,
        String reviewStatus,
        Instant startDate,
        Instant endDate,
        int page,
        int size
    ) {
        LocalDateTime start = startDate != null
            ? LocalDateTime.ofInstant(startDate, ZoneOffset.UTC)
            : LocalDateTime.now().minusDays(30);

        LocalDateTime end = endDate != null
            ? LocalDateTime.ofInstant(endDate, ZoneOffset.UTC)
            : LocalDateTime.now();

        Pageable pageable = PageRequest.of(page, size);

        Page<ClinicalDecisionEntity> decisionsPage = clinicalDecisionRepository.findDecisionHistory(
            tenantId,
            decisionType,
            alertSeverity,
            reviewStatus,
            start,
            end,
            null,  // evidenceGrade
            null,  // hasOverride
            null,  // specialtyArea
            pageable
        );

        return decisionsPage.map(this::mapToResponse);
    }

    /**
     * Get clinical audit metrics.
     */
    @Transactional(readOnly = true)
    public ClinicalMetricsResponse getClinicalMetrics(String tenantId, Instant startDate, Instant endDate) {
        LocalDateTime start = startDate != null
            ? LocalDateTime.ofInstant(startDate, ZoneOffset.UTC)
            : LocalDateTime.now().minusDays(30);

        LocalDateTime end = endDate != null
            ? LocalDateTime.ofInstant(endDate, ZoneOffset.UTC)
            : LocalDateTime.now();

        Page<ClinicalDecisionEntity> decisionsPage = clinicalDecisionRepository.findByTenantIdAndDateRange(
            tenantId, start, end, Pageable.unpaged()
        );
        List<ClinicalDecisionEntity> decisions = decisionsPage.getContent();

        long totalDecisions = decisions.size();

        long acceptedRecommendations = decisions.stream()
            .filter(d -> "APPROVED".equals(d.getReviewStatus()))
            .count();

        long rejectedRecommendations = decisions.stream()
            .filter(d -> "REJECTED".equals(d.getReviewStatus()))
            .count();

        long modifiedRecommendations = decisions.stream()
            .filter(d -> "NEEDS_REVISION".equals(d.getReviewStatus()))
            .count();

        long pendingReview = decisions.stream()
            .filter(d -> "PENDING".equals(d.getReviewStatus()))
            .count();

        double averageAcceptanceRate = totalDecisions > 0
            ? (double) acceptedRecommendations / totalDecisions
            : 0.0;

        long criticalSeverityCount = decisions.stream()
            .filter(d -> "CRITICAL".equals(d.getAlertSeverity()))
            .count();

        long highSeverityCount = decisions.stream()
            .filter(d -> "HIGH".equals(d.getAlertSeverity()))
            .count();

        long moderateSeverityCount = decisions.stream()
            .filter(d -> "MODERATE".equals(d.getAlertSeverity()))
            .count();

        long lowSeverityCount = decisions.stream()
            .filter(d -> "LOW".equals(d.getAlertSeverity()))
            .count();

        double averageConfidenceScore = decisions.stream()
            .filter(d -> d.getConfidenceScore() != null)
            .mapToDouble(ClinicalDecisionEntity::getConfidenceScore)
            .average()
            .orElse(0.0);

        long overrideCount = decisions.stream()
            .filter(d -> Boolean.TRUE.equals(d.getHasOverride()))
            .count();

        double overrideRate = totalDecisions > 0
            ? (double) overrideCount / totalDecisions
            : 0.0;

        long evidenceGradeACount = decisions.stream()
            .filter(d -> "A".equals(d.getEvidenceGrade()))
            .count();

        long evidenceGradeBCount = decisions.stream()
            .filter(d -> "B".equals(d.getEvidenceGrade()))
            .count();

        long evidenceGradeCCount = decisions.stream()
            .filter(d -> "C".equals(d.getEvidenceGrade()))
            .count();

        long evidenceGradeDCount = decisions.stream()
            .filter(d -> "D".equals(d.getEvidenceGrade()))
            .count();

        return ClinicalMetricsResponse.builder()
            .totalDecisions(totalDecisions)
            .acceptedRecommendations(acceptedRecommendations)
            .rejectedRecommendations(rejectedRecommendations)
            .modifiedRecommendations(modifiedRecommendations)
            .pendingReview(pendingReview)
            .averageAcceptanceRate(averageAcceptanceRate)
            .criticalSeverityCount(criticalSeverityCount)
            .highSeverityCount(highSeverityCount)
            .moderateSeverityCount(moderateSeverityCount)
            .lowSeverityCount(lowSeverityCount)
            .averageConfidenceScore(averageConfidenceScore)
            .overrideRate(overrideRate)
            .evidenceGradeACount(evidenceGradeACount)
            .evidenceGradeBCount(evidenceGradeBCount)
            .evidenceGradeCCount(evidenceGradeCCount)
            .evidenceGradeDCount(evidenceGradeDCount)
            .build();
    }

    /**
     * Accept AI clinical recommendation.
     */
    @Transactional
    public ClinicalDecisionResponse acceptRecommendation(
        String tenantId,
        UUID decisionId,
        ClinicalReviewRequest request,
        String reviewerUsername
    ) {
        ClinicalDecisionEntity decision = clinicalDecisionRepository.findById(decisionId)
            .orElseThrow(() -> new IllegalArgumentException("Clinical decision not found: " + decisionId));

        if (!tenantId.equals(decision.getTenantId())) {
            throw new IllegalArgumentException("Decision not found in tenant: " + tenantId);
        }

        decision.setReviewStatus("APPROVED");
        decision.setReviewedBy(reviewerUsername);
        decision.setReviewedAt(LocalDateTime.now());
        decision.setReviewNotes(request.getClinicalNotes());

        ClinicalDecisionEntity saved = clinicalDecisionRepository.save(decision);

        log.info("Clinical recommendation accepted: {}, reviewer: {}", decisionId, reviewerUsername);

        return mapToResponse(saved);
    }

    /**
     * Reject AI clinical recommendation.
     */
    @Transactional
    public ClinicalDecisionResponse rejectRecommendation(
        String tenantId,
        UUID decisionId,
        ClinicalReviewRequest request,
        String reviewerUsername
    ) {
        ClinicalDecisionEntity decision = clinicalDecisionRepository.findById(decisionId)
            .orElseThrow(() -> new IllegalArgumentException("Clinical decision not found: " + decisionId));

        if (!tenantId.equals(decision.getTenantId())) {
            throw new IllegalArgumentException("Decision not found in tenant: " + tenantId);
        }

        decision.setReviewStatus("REJECTED");
        decision.setReviewedBy(reviewerUsername);
        decision.setReviewedAt(LocalDateTime.now());
        decision.setReviewNotes(request.getClinicalRationale());

        ClinicalDecisionEntity saved = clinicalDecisionRepository.save(decision);

        log.info("Clinical recommendation rejected: {}, reviewer: {}, rationale: {}",
            decisionId, reviewerUsername, request.getClinicalRationale());

        return mapToResponse(saved);
    }

    /**
     * Modify AI clinical recommendation.
     */
    @Transactional
    public ClinicalDecisionResponse modifyRecommendation(
        String tenantId,
        UUID decisionId,
        ClinicalReviewRequest request,
        String reviewerUsername
    ) {
        ClinicalDecisionEntity decision = clinicalDecisionRepository.findById(decisionId)
            .orElseThrow(() -> new IllegalArgumentException("Clinical decision not found: " + decisionId));

        if (!tenantId.equals(decision.getTenantId())) {
            throw new IllegalArgumentException("Decision not found in tenant: " + tenantId);
        }

        decision.setReviewStatus("NEEDS_REVISION");
        decision.setReviewedBy(reviewerUsername);
        decision.setReviewedAt(LocalDateTime.now());
        decision.setReviewNotes(request.getModifications());

        // Apply override if alternative action provided
        if (request.getAlternativeAction() != null && !request.getAlternativeAction().isEmpty()) {
            decision.setHasOverride(true);
            decision.setOverrideReason(request.getClinicalReasoning());
            decision.setOverrideAppliedBy(reviewerUsername);
            decision.setOverrideAppliedAt(LocalDateTime.now());
        }

        ClinicalDecisionEntity saved = clinicalDecisionRepository.save(decision);

        log.info("Clinical recommendation modified: {}, reviewer: {}, modifications: {}",
            decisionId, reviewerUsername, request.getModifications());

        return mapToResponse(saved);
    }

    /**
     * Export clinical audit report as Excel.
     */
    @Transactional(readOnly = true)
    public byte[] exportClinicalReport(String tenantId, Instant startDate, Instant endDate) {
        LocalDateTime start = startDate != null
            ? LocalDateTime.ofInstant(startDate, ZoneOffset.UTC)
            : LocalDateTime.now().minusDays(30);

        LocalDateTime end = endDate != null
            ? LocalDateTime.ofInstant(endDate, ZoneOffset.UTC)
            : LocalDateTime.now();

        Page<ClinicalDecisionEntity> decisionsPage = clinicalDecisionRepository.findByTenantIdAndDateRange(
            tenantId, start, end, Pageable.unpaged()
        );
        List<ClinicalDecisionEntity> decisions = decisionsPage.getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Clinical Audit Report");

            // Create header row with bold font
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Decision ID", "Timestamp", "Patient ID", "Decision Type", "Alert Severity",
                "Review Status", "Evidence Grade", "Confidence Score", "Reviewed By",
                "Reviewed At", "Review Notes", "Has Override", "Override Reason"
            };

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate data rows
            int rowNum = 1;
            for (ClinicalDecisionEntity decision : decisions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(decision.getId() != null ? decision.getId().toString() : "");
                row.createCell(1).setCellValue(decision.getDecisionTimestamp() != null ? decision.getDecisionTimestamp().toString() : "");
                row.createCell(2).setCellValue(decision.getPatientId() != null ? decision.getPatientId() : "");
                row.createCell(3).setCellValue(decision.getDecisionType() != null ? decision.getDecisionType() : "");
                row.createCell(4).setCellValue(decision.getAlertSeverity() != null ? decision.getAlertSeverity() : "");
                row.createCell(5).setCellValue(decision.getReviewStatus() != null ? decision.getReviewStatus() : "");
                row.createCell(6).setCellValue(decision.getEvidenceGrade() != null ? decision.getEvidenceGrade() : "");
                row.createCell(7).setCellValue(decision.getConfidenceScore() != null ? decision.getConfidenceScore() : 0.0);
                row.createCell(8).setCellValue(decision.getReviewedBy() != null ? decision.getReviewedBy() : "");
                row.createCell(9).setCellValue(decision.getReviewedAt() != null ? decision.getReviewedAt().toString() : "");
                row.createCell(10).setCellValue(decision.getReviewNotes() != null ? decision.getReviewNotes() : "");
                row.createCell(11).setCellValue(decision.getHasOverride() != null ? decision.getHasOverride().toString() : "false");
                row.createCell(12).setCellValue(decision.getOverrideReason() != null ? decision.getOverrideReason() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate clinical audit report", e);
            throw new RuntimeException("Failed to generate clinical audit report", e);
        }
    }

    /**
     * Map entity to response DTO.
     */
    private ClinicalDecisionResponse mapToResponse(ClinicalDecisionEntity entity) {
        return ClinicalDecisionResponse.builder()
            .id(entity.getId())
            .patientId(entity.getPatientId())
            .decisionType(entity.getDecisionType())
            .alertSeverity(entity.getAlertSeverity())
            .decisionTimestamp(entity.getDecisionTimestamp())
            .reviewStatus(entity.getReviewStatus())
            .evidenceGrade(entity.getEvidenceGrade())
            .confidenceScore(entity.getConfidenceScore())
            .specialtyArea(entity.getSpecialtyArea())
            .patientContext(entity.getPatientContext())
            .recommendation(entity.getRecommendation())
            .evidence(entity.getEvidence())
            .clinicalDetails(entity.getClinicalDetails())
            .reviewedBy(entity.getReviewedBy())
            .reviewedAt(entity.getReviewedAt())
            .reviewNotes(entity.getReviewNotes())
            .hasOverride(entity.getHasOverride())
            .overrideReason(entity.getOverrideReason())
            .overrideAppliedBy(entity.getOverrideAppliedBy())
            .overrideAppliedAt(entity.getOverrideAppliedAt())
            .build();
    }
}
