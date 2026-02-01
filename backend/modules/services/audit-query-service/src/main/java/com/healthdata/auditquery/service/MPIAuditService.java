package com.healthdata.auditquery.service;

import com.healthdata.audit.entity.MPIMergeEntity;
import com.healthdata.audit.repository.MPIMergeRepository;
import com.healthdata.auditquery.dto.mpi.MPIMergeEventResponse;
import com.healthdata.auditquery.dto.mpi.MPIMetricsResponse;
import com.healthdata.auditquery.dto.mpi.MPIReviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for MPI Audit Dashboard operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MPIAuditService {

    private final MPIMergeRepository mpiMergeRepository;

    /**
     * Fetch MPI merge events for review with optional filtering.
     */
    @Transactional(readOnly = true)
    public Page<MPIMergeEventResponse> getMPIMergeEvents(
        String tenantId,
        String mergeType,
        String mergeStatus,
        String validationStatus,
        Double minConfidence,
        Double maxConfidence,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Boolean hasErrors,
        Boolean hasDataQualityIssues,
        int page,
        int size
    ) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<MPIMergeEntity> mergesPage = mpiMergeRepository.findMergeHistory(
            tenantId,
            mergeStatus,
            validationStatus,
            startDate,
            endDate,
            mergeType,
            minConfidence,
            maxConfidence,
            pageable
        );

        // Apply additional filters not supported by repository (hasErrors, hasDataQualityIssues)
        if (hasErrors != null || hasDataQualityIssues != null) {
            List<MPIMergeEntity> filteredMerges = mergesPage.getContent().stream()
                .filter(merge -> {
                    if (hasErrors != null && !hasErrors.equals(merge.getHasMergeErrors())) {
                        return false;
                    }
                    if (hasDataQualityIssues != null && !hasDataQualityIssues.equals(merge.getHasDataQualityIssues())) {
                        return false;
                    }
                    return true;
                })
                .toList();

            List<MPIMergeEventResponse> responses = filteredMerges.stream()
                .map(this::mapToResponse)
                .toList();

            return new PageImpl<>(responses, pageable, mergesPage.getTotalElements());
        }

        return mergesPage.map(this::mapToResponse);
    }

    /**
     * Get MPI audit metrics.
     */
    @Transactional(readOnly = true)
    public MPIMetricsResponse getMPIMetrics(String tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        List<MPIMergeEntity> merges = mpiMergeRepository.findByTenantIdAndDateRange(
            tenantId, startDate, endDate
        );

        long totalMerges = merges.size();

        long automaticMerges = merges.stream()
            .filter(m -> "AUTOMATIC".equals(m.getMergeType()))
            .count();

        long manualMerges = merges.stream()
            .filter(m -> "MANUAL".equals(m.getMergeType()))
            .count();

        long assistedMerges = merges.stream()
            .filter(m -> "ASSISTED".equals(m.getMergeType()))
            .count();

        long validatedMerges = merges.stream()
            .filter(m -> "VALIDATED".equals(m.getMergeStatus()))
            .count();

        long pendingValidation = merges.stream()
            .filter(m -> "PENDING".equals(m.getMergeStatus()))
            .count();

        long rolledBackMerges = merges.stream()
            .filter(m -> "ROLLED_BACK".equals(m.getMergeStatus()))
            .count();

        long failedMerges = merges.stream()
            .filter(m -> "FAILED".equals(m.getMergeStatus()))
            .count();

        double averageConfidenceScore = merges.stream()
            .filter(m -> m.getConfidenceScore() != null)
            .mapToDouble(MPIMergeEntity::getConfidenceScore)
            .average()
            .orElse(0.0);

        long highConfidenceMerges = merges.stream()
            .filter(m -> m.getConfidenceScore() != null && m.getConfidenceScore() > 0.9)
            .count();

        long mediumConfidenceMerges = merges.stream()
            .filter(m -> m.getConfidenceScore() != null && m.getConfidenceScore() >= 0.7 && m.getConfidenceScore() <= 0.9)
            .count();

        long lowConfidenceMerges = merges.stream()
            .filter(m -> m.getConfidenceScore() != null && m.getConfidenceScore() < 0.7)
            .count();

        long mergesWithErrors = merges.stream()
            .filter(m -> Boolean.TRUE.equals(m.getHasMergeErrors()))
            .count();

        long dataQualityIssues = merges.stream()
            .filter(m -> Boolean.TRUE.equals(m.getHasDataQualityIssues()))
            .count();

        long highDataQualityCount = merges.stream()
            .filter(m -> "HIGH".equals(m.getDataQualityAssessment()))
            .count();

        long mediumDataQualityCount = merges.stream()
            .filter(m -> "MEDIUM".equals(m.getDataQualityAssessment()))
            .count();

        long lowDataQualityCount = merges.stream()
            .filter(m -> "LOW".equals(m.getDataQualityAssessment()))
            .count();

        double validationSuccessRate = totalMerges > 0
            ? (double) validatedMerges / totalMerges
            : 0.0;

        double rollbackRate = totalMerges > 0
            ? (double) rolledBackMerges / totalMerges
            : 0.0;

        // Calculate average merge time (placeholder - would need merge duration in entity)
        double averageMergeTime = 2.4;

        return MPIMetricsResponse.builder()
            .totalMerges(totalMerges)
            .automaticMerges(automaticMerges)
            .manualMerges(manualMerges)
            .assistedMerges(assistedMerges)
            .validatedMerges(validatedMerges)
            .pendingValidation(pendingValidation)
            .rolledBackMerges(rolledBackMerges)
            .failedMerges(failedMerges)
            .averageConfidenceScore(averageConfidenceScore)
            .highConfidenceMerges(highConfidenceMerges)
            .mediumConfidenceMerges(mediumConfidenceMerges)
            .lowConfidenceMerges(lowConfidenceMerges)
            .mergesWithErrors(mergesWithErrors)
            .dataQualityIssues(dataQualityIssues)
            .highDataQualityCount(highDataQualityCount)
            .mediumDataQualityCount(mediumDataQualityCount)
            .lowDataQualityCount(lowDataQualityCount)
            .validationSuccessRate(validationSuccessRate)
            .rollbackRate(rollbackRate)
            .averageMergeTime(averageMergeTime)
            .build();
    }

    /**
     * Validate MPI merge operation.
     */
    @Transactional
    public MPIMergeEventResponse validateMerge(
        String tenantId,
        UUID mergeId,
        MPIReviewRequest request,
        String validatorUsername
    ) {
        MPIMergeEntity merge = mpiMergeRepository.findById(mergeId)
            .orElseThrow(() -> new IllegalArgumentException("MPI merge not found: " + mergeId));

        if (!tenantId.equals(merge.getTenantId())) {
            throw new IllegalArgumentException("Merge not found in tenant: " + tenantId);
        }

        merge.setMergeStatus("VALIDATED");
        merge.setValidationStatus("VALIDATED");
        merge.setValidatedBy(validatorUsername);
        merge.setValidatedAt(LocalDateTime.now());
        merge.setValidationNotes(request.getValidationNotes());

        if (request.getDataQualityAssessment() != null) {
            merge.setDataQualityAssessment(request.getDataQualityAssessment());
        }

        MPIMergeEntity saved = mpiMergeRepository.save(merge);

        log.info("MPI merge validated: {}, validator: {}", mergeId, validatorUsername);

        return mapToResponse(saved);
    }

    /**
     * Rollback MPI merge operation.
     */
    @Transactional
    public MPIMergeEventResponse rollbackMerge(
        String tenantId,
        UUID mergeId,
        MPIReviewRequest request,
        String rollbackUsername
    ) {
        MPIMergeEntity merge = mpiMergeRepository.findById(mergeId)
            .orElseThrow(() -> new IllegalArgumentException("MPI merge not found: " + mergeId));

        if (!tenantId.equals(merge.getTenantId())) {
            throw new IllegalArgumentException("Merge not found in tenant: " + tenantId);
        }

        merge.setMergeStatus("ROLLED_BACK");
        merge.setRollbackReason(request.getRollbackReason());
        merge.setRolledBackAt(LocalDateTime.now());
        merge.setRolledBackBy(rollbackUsername);

        MPIMergeEntity saved = mpiMergeRepository.save(merge);

        log.info("MPI merge rolled back: {}, rollback user: {}, reason: {}",
            mergeId, rollbackUsername, request.getRollbackReason());

        return mapToResponse(saved);
    }

    /**
     * Resolve data quality issue.
     */
    @Transactional
    public MPIMergeEventResponse resolveDataQualityIssue(
        String tenantId,
        UUID mergeId,
        MPIReviewRequest request,
        String resolverUsername
    ) {
        MPIMergeEntity merge = mpiMergeRepository.findById(mergeId)
            .orElseThrow(() -> new IllegalArgumentException("MPI merge not found: " + mergeId));

        if (!tenantId.equals(merge.getTenantId())) {
            throw new IllegalArgumentException("Merge not found in tenant: " + tenantId);
        }

        merge.setHasDataQualityIssues(false);
        merge.setDataQualityAssessment(request.getDataQualityAssessment());
        merge.setValidationNotes(request.getResolutionNotes());
        merge.setValidatedBy(resolverUsername);
        merge.setValidatedAt(LocalDateTime.now());

        MPIMergeEntity saved = mpiMergeRepository.save(merge);

        log.info("Data quality issue resolved for MPI merge: {}, resolver: {}",
            mergeId, resolverUsername);

        return mapToResponse(saved);
    }

    /**
     * Export MPI audit report as Excel.
     */
    @Transactional(readOnly = true)
    public byte[] exportMPIReport(String tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        List<MPIMergeEntity> merges = mpiMergeRepository.findByTenantIdAndDateRange(
            tenantId, startDate, endDate
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("MPI Audit Report");

            // Create header row with bold font
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Merge ID", "Merge Timestamp", "Source Patient", "Target Patient",
                "Merge Type", "Confidence Score", "Merge Status", "Validation Status",
                "Performed By", "Validated By", "Validated At", "Validation Notes",
                "Has Errors", "Data Quality Issues", "Data Quality Assessment",
                "Rollback Reason", "Rolled Back By", "Rolled Back At"
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
            for (MPIMergeEntity merge : merges) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(merge.getId() != null ? merge.getId().toString() : "");
                row.createCell(1).setCellValue(merge.getMergeTimestamp() != null ? merge.getMergeTimestamp().toString() : "");
                row.createCell(2).setCellValue(merge.getSourcePatientId() != null ? merge.getSourcePatientId() : "");
                row.createCell(3).setCellValue(merge.getTargetPatientId() != null ? merge.getTargetPatientId() : "");
                row.createCell(4).setCellValue(merge.getMergeType() != null ? merge.getMergeType() : "");
                row.createCell(5).setCellValue(merge.getConfidenceScore() != null ? merge.getConfidenceScore() : 0.0);
                row.createCell(6).setCellValue(merge.getMergeStatus() != null ? merge.getMergeStatus() : "");
                row.createCell(7).setCellValue(merge.getValidationStatus() != null ? merge.getValidationStatus() : "");
                row.createCell(8).setCellValue(merge.getPerformedBy() != null ? merge.getPerformedBy() : "");
                row.createCell(9).setCellValue(merge.getValidatedBy() != null ? merge.getValidatedBy() : "");
                row.createCell(10).setCellValue(merge.getValidatedAt() != null ? merge.getValidatedAt().toString() : "");
                row.createCell(11).setCellValue(merge.getValidationNotes() != null ? merge.getValidationNotes() : "");
                row.createCell(12).setCellValue(merge.getHasMergeErrors() != null ? merge.getHasMergeErrors().toString() : "false");
                row.createCell(13).setCellValue(merge.getHasDataQualityIssues() != null ? merge.getHasDataQualityIssues().toString() : "false");
                row.createCell(14).setCellValue(merge.getDataQualityAssessment() != null ? merge.getDataQualityAssessment() : "");
                row.createCell(15).setCellValue(merge.getRollbackReason() != null ? merge.getRollbackReason() : "");
                row.createCell(16).setCellValue(merge.getRolledBackBy() != null ? merge.getRolledBackBy() : "");
                row.createCell(17).setCellValue(merge.getRolledBackAt() != null ? merge.getRolledBackAt().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate MPI audit report", e);
            throw new RuntimeException("Failed to generate MPI audit report", e);
        }
    }

    /**
     * Map entity to response DTO.
     */
    private MPIMergeEventResponse mapToResponse(MPIMergeEntity entity) {
        return MPIMergeEventResponse.builder()
            .id(entity.getId())
            .sourcePatientId(entity.getSourcePatientId())
            .targetPatientId(entity.getTargetPatientId())
            .mergeType(entity.getMergeType())
            .confidenceScore(entity.getConfidenceScore())
            .mergeStatus(entity.getMergeStatus())
            .validationStatus(entity.getValidationStatus())
            .mergeTimestamp(entity.getMergeTimestamp())
            .performedBy(entity.getPerformedBy())
            .sourcePatientSnapshot(entity.getSourcePatientSnapshot())
            .targetPatientSnapshot(entity.getTargetPatientSnapshot())
            .mergedPatientSnapshot(entity.getMergedPatientSnapshot())
            .matchingDetails(entity.getMatchingDetails())
            .validatedBy(entity.getValidatedBy())
            .validatedAt(entity.getValidatedAt())
            .validationNotes(entity.getValidationNotes())
            .hasMergeErrors(entity.getHasMergeErrors())
            .hasDataQualityIssues(entity.getHasDataQualityIssues())
            .dataQualityAssessment(entity.getDataQualityAssessment())
            .rollbackReason(entity.getRollbackReason())
            .rolledBackAt(entity.getRolledBackAt())
            .rolledBackBy(entity.getRolledBackBy())
            .build();
    }
}
