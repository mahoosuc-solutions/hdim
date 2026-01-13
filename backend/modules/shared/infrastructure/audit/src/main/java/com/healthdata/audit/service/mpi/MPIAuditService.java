package com.healthdata.audit.service.mpi;

import com.healthdata.audit.dto.mpi.*;
import com.healthdata.audit.entity.DataQualityIssueEntity;
import com.healthdata.audit.entity.MPIMergeEntity;
import com.healthdata.audit.repository.DataQualityIssueRepository;
import com.healthdata.audit.repository.MPIMergeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for MPI audit operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MPIAuditService {
    
    private final MPIMergeRepository mpiMergeRepository;
    private final DataQualityIssueRepository dataQualityIssueRepository;
    
    /**
     * Get paginated merge history with filtering
     */
    public Page<MPIMergeEvent> getMergeHistory(String tenantId, MPIMergeFilter filter, Pageable pageable) {
        LocalDateTime startDate = filter.getStartDate() != null 
            ? filter.getStartDate().atStartOfDay() : null;
        LocalDateTime endDate = filter.getEndDate() != null 
            ? filter.getEndDate().atTime(LocalTime.MAX) : null;
        
        Page<MPIMergeEntity> entities = mpiMergeRepository.findMergeHistory(
            tenantId,
            filter.getMergeStatus(),
            filter.getValidationStatus(),
            startDate,
            endDate,
            filter.getMergeType(),
            filter.getMinConfidenceScore(),
            filter.getMaxConfidenceScore(),
            pageable
        );
        
        return entities.map(this::toMPIMergeEvent);
    }
    
    /**
     * Get detailed merge information
     */
    public MPIMergeDetail getMergeDetail(String mergeId, String tenantId) {
        MPIMergeEntity merge = mpiMergeRepository.findByMergeIdAndTenantId(mergeId, tenantId)
            .orElseThrow(() -> new RuntimeException("Merge not found: " + mergeId));
        
        return toMPIMergeDetail(merge);
    }
    
    /**
     * Validate a merge
     */
    @Transactional
    public MPIValidationResult validateMerge(String mergeId, String tenantId, MPIValidationRequest request, String validatedBy) {
        MPIMergeEntity merge = mpiMergeRepository.findByMergeIdAndTenantId(mergeId, tenantId)
            .orElseThrow(() -> new RuntimeException("Merge not found: " + mergeId));
        
        merge.setValidationStatus("VALIDATED".equals(request.getValidationOutcome()) ? "VALIDATED" : "VALIDATION_FAILED");
        merge.setValidatedBy(validatedBy);
        merge.setValidatedAt(LocalDateTime.now());
        merge.setValidationNotes(request.getValidationNotes());
        merge.setHasMergeErrors(request.getHasMergeErrors());
        merge.setHasDataQualityIssues(request.getHasDataQualityIssues());
        merge.setDataQualityAssessment(request.getDataQualityAssessment());
        
        if ("VALIDATED".equals(request.getValidationOutcome())) {
            merge.setMergeStatus("VALIDATED");
        }
        
        mpiMergeRepository.save(merge);
        
        log.info("Merge {} validated by {} with outcome: {}", mergeId, validatedBy, request.getValidationOutcome());
        
        return MPIValidationResult.builder()
            .mergeId(mergeId)
            .validationOutcome(request.getValidationOutcome())
            .validatedBy(validatedBy)
            .validatedAt(LocalDateTime.now())
            .success(true)
            .message("Merge validation recorded successfully")
            .build();
    }
    
    /**
     * Rollback a merge
     */
    @Transactional
    public MPIRollbackResult rollbackMerge(String mergeId, String tenantId, MPIRollbackRequest request, String rolledBackBy) {
        MPIMergeEntity merge = mpiMergeRepository.findByMergeIdAndTenantId(mergeId, tenantId)
            .orElseThrow(() -> new RuntimeException("Merge not found: " + mergeId));
        
        merge.setMergeStatus("ROLLED_BACK");
        merge.setRollbackReason(request.getRollbackReason());
        merge.setRolledBackBy(rolledBackBy);
        merge.setRolledBackAt(LocalDateTime.now());
        
        mpiMergeRepository.save(merge);
        
        log.info("Merge {} rolled back by {} for reason: {}", mergeId, rolledBackBy, request.getRollbackReason());
        
        // In a real implementation, this would trigger actual patient record restoration
        String restoredSourcePatientId = request.getRecreateSourcePatient() ? merge.getSourcePatientId() : null;
        String updatedTargetPatientId = request.getPreserveTargetPatient() ? merge.getTargetPatientId() : null;
        
        return MPIRollbackResult.builder()
            .mergeId(mergeId)
            .rollbackStatus("ROLLED_BACK")
            .rolledBackBy(rolledBackBy)
            .rolledBackAt(LocalDateTime.now())
            .success(true)
            .message("Merge rolled back successfully")
            .restoredSourcePatientId(restoredSourcePatientId)
            .updatedTargetPatientId(updatedTargetPatientId)
            .build();
    }
    
    /**
     * Get data quality issues
     */
    public Page<DataQualityIssueDTO> getDataQualityIssues(String tenantId, String status, String severity, Pageable pageable) {
        Page<DataQualityIssueEntity> entities = dataQualityIssueRepository.findIssuesByTenant(
            tenantId, status, severity, pageable
        );
        
        return entities.map(this::toDataQualityIssueDTO);
    }
    
    /**
     * Resolve a data quality issue
     */
    @Transactional
    public DataQualityIssueDTO resolveDataQualityIssue(String issueId, String tenantId, DataQualityResolveRequest request, String resolvedBy) {
        DataQualityIssueEntity issue = dataQualityIssueRepository.findByIssueIdAndTenantId(issueId, tenantId)
            .orElseThrow(() -> new RuntimeException("Data quality issue not found: " + issueId));
        
        issue.setStatus("RESOLVED");
        issue.setResolvedBy(resolvedBy);
        issue.setResolvedAt(LocalDateTime.now());
        issue.setResolutionNotes(request.getResolutionNotes());
        
        dataQualityIssueRepository.save(issue);
        
        log.info("Data quality issue {} resolved by {} with action: {}", issueId, resolvedBy, request.getResolutionAction());
        
        return toDataQualityIssueDTO(issue);
    }
    
    /**
     * Get MPI metrics
     */
    public MPIMetrics getMetrics(String tenantId, LocalDate startDate, LocalDate endDate) {
        Long totalMerges = mpiMergeRepository.countByTenantIdAndStatus(tenantId, null);
        Long validatedMerges = mpiMergeRepository.countByTenantIdAndValidationStatus(tenantId, "VALIDATED");
        Long rolledBackMerges = mpiMergeRepository.countByTenantIdAndStatus(tenantId, "ROLLED_BACK");
        Long pendingValidation = mpiMergeRepository.countByTenantIdAndValidationStatus(tenantId, "NOT_VALIDATED");
        
        Double averageConfidence = mpiMergeRepository.getAverageConfidenceScore(tenantId);
        
        Double validationRate = totalMerges > 0 ? (validatedMerges.doubleValue() / totalMerges) : 0.0;
        Double rollbackRate = totalMerges > 0 ? (rolledBackMerges.doubleValue() / totalMerges) : 0.0;
        
        Long totalIssues = dataQualityIssueRepository.countByTenantIdAndStatus(tenantId, null);
        Long criticalIssues = dataQualityIssueRepository.countByTenantIdAndSeverity(tenantId, "CRITICAL");
        Long resolvedIssues = dataQualityIssueRepository.countByTenantIdAndStatus(tenantId, "RESOLVED");
        Double resolutionRate = totalIssues > 0 ? (resolvedIssues.doubleValue() / totalIssues) : 0.0;
        
        return MPIMetrics.builder()
            .totalMerges(totalMerges)
            .validatedMerges(validatedMerges)
            .rolledBackMerges(rolledBackMerges)
            .pendingValidation(pendingValidation)
            .validationRate(validationRate)
            .rollbackRate(rollbackRate)
            .averageConfidenceScore(averageConfidence != null ? averageConfidence : 0.0)
            .averageValidationTimeMinutes(0L)  // Would require time-based calculations
            .mergeTypeDistribution(MPIMetrics.MergeTypeDistribution.builder()
                .automatic(0L)
                .manual(0L)
                .assisted(0L)
                .build())
            .dataQualityMetrics(MPIMetrics.DataQualityMetrics.builder()
                .totalIssues(totalIssues)
                .criticalIssues(criticalIssues)
                .resolvedIssues(resolvedIssues)
                .resolutionRate(resolutionRate)
                .build())
            .build();
    }
    
    /**
     * Get accuracy trends
     */
    public MPITrendData getAccuracyTrends(String tenantId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        List<MPIMergeEntity> merges = mpiMergeRepository.findByTenantIdAndDateRange(
            tenantId,
            startDate.atStartOfDay(),
            endDate.atTime(LocalTime.MAX)
        );
        
        Map<LocalDate, List<MPIMergeEntity>> mergesByDate = merges.stream()
            .collect(Collectors.groupingBy(m -> m.getMergeTimestamp().toLocalDate()));
        
        List<MPITrendData.DailyMergeTrend> dailyTrends = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<MPIMergeEntity> dayMerges = mergesByDate.getOrDefault(current, List.of());
            long totalMerges = dayMerges.size();
            long validated = dayMerges.stream().filter(m -> "VALIDATED".equals(m.getValidationStatus())).count();
            long rolledBack = dayMerges.stream().filter(m -> "ROLLED_BACK".equals(m.getMergeStatus())).count();
            double validationRate = totalMerges > 0 ? (validated / (double) totalMerges) : 0.0;
            double avgConfidence = dayMerges.stream()
                .filter(m -> m.getConfidenceScore() != null)
                .mapToDouble(MPIMergeEntity::getConfidenceScore)
                .average()
                .orElse(0.0);
            
            dailyTrends.add(MPITrendData.DailyMergeTrend.builder()
                .date(current.toString())
                .totalMerges(totalMerges)
                .validatedMerges(validated)
                .rolledBackMerges(rolledBack)
                .validationRate(validationRate)
                .averageConfidence(avgConfidence)
                .build());
            
            current = current.plusDays(1);
        }
        
        double avgValidationRate = dailyTrends.stream()
            .mapToDouble(MPITrendData.DailyMergeTrend::getValidationRate)
            .average()
            .orElse(0.0);
        
        double avgRollbackRate = dailyTrends.stream()
            .filter(t -> t.getTotalMerges() > 0)
            .mapToDouble(t -> t.getRolledBackMerges() / (double) t.getTotalMerges())
            .average()
            .orElse(0.0);
        
        return MPITrendData.builder()
            .dailyTrends(dailyTrends)
            .averageValidationRate(avgValidationRate)
            .averageRollbackRate(avgRollbackRate)
            .build();
    }
    
    // DTO conversion methods
    
    private MPIMergeEvent toMPIMergeEvent(MPIMergeEntity entity) {
        // Count data quality issues for this merge
        Long issueCount = dataQualityIssueRepository.countByTenantIdAndStatus(entity.getTenantId(), "OPEN");
        
        return MPIMergeEvent.builder()
            .mergeId(entity.getId().toString())
            .tenantId(entity.getTenantId())
            .sourcePatientId(entity.getSourcePatientId())
            .targetPatientId(entity.getTargetPatientId())
            .mergeType(entity.getMergeType())
            .confidenceScore(entity.getConfidenceScore())
            .mergeStatus(entity.getMergeStatus())
            .validationStatus(entity.getValidationStatus())
            .mergeTimestamp(entity.getMergeTimestamp())
            .performedBy(entity.getPerformedBy())
            .dataQualityIssueCount(issueCount != null ? issueCount.intValue() : 0)
            .matchedAttributes(extractMatchedAttributes(entity))
            .priority(determinePriority(entity))
            .build();
    }
    
    private MPIMergeDetail toMPIMergeDetail(MPIMergeEntity entity) {
        List<DataQualityIssueEntity> issues = dataQualityIssueRepository
            .findByTenantIdAndPatientIdOrderByDetectedAtDesc(
                entity.getTenantId(),
                entity.getTargetPatientId()
            );
        
        return MPIMergeDetail.builder()
            .mergeId(entity.getId().toString())
            .tenantId(entity.getTenantId())
            .sourcePatient(buildPatientSnapshot(entity.getSourcePatientSnapshot()))
            .targetPatient(buildPatientSnapshot(entity.getTargetPatientSnapshot()))
            .mergedPatient(buildPatientSnapshot(entity.getMergedPatientSnapshot()))
            .mergeType(entity.getMergeType())
            .confidenceScore(entity.getConfidenceScore())
            .mergeStatus(entity.getMergeStatus())
            .validationStatus(entity.getValidationStatus())
            .mergeTimestamp(entity.getMergeTimestamp())
            .performedBy(entity.getPerformedBy())
            .matchedAttributes(buildAttributeMatches(entity.getMatchingDetails()))
            .conflicts(new ArrayList<>())
            .matchingAlgorithmDetails(entity.getMatchingDetails())
            .dataQualityIssues(issues.stream()
                .map(this::toDataQualityIssue)
                .collect(Collectors.toList()))
            .overallDataQualityScore(entity.getDataQualityAssessment())
            .validatedBy(entity.getValidatedBy())
            .validatedAt(entity.getValidatedAt())
            .validationNotes(entity.getValidationNotes())
            .rollbackReason(entity.getRollbackReason())
            .rolledBackAt(entity.getRolledBackAt())
            .rolledBackBy(entity.getRolledBackBy())
            .build();
    }
    
    private DataQualityIssueDTO toDataQualityIssueDTO(DataQualityIssueEntity entity) {
        return DataQualityIssueDTO.builder()
            .issueId(entity.getId().toString())
            .tenantId(entity.getTenantId())
            .patientId(entity.getPatientId())
            .issueType(entity.getIssueType())
            .severity(entity.getSeverity())
            .status(entity.getStatus())
            .description(entity.getDescription())
            .affectedField(entity.getAffectedField())
            .currentValue(entity.getCurrentValue())
            .suggestedValue(entity.getSuggestedValue())
            .recommendation(entity.getRecommendation())
            .detectedAt(entity.getDetectedAt())
            .resolvedAt(entity.getResolvedAt())
            .resolvedBy(entity.getResolvedBy())
            .resolutionNotes(entity.getResolutionNotes())
            .build();
    }
    
    private MPIMergeDetail.DataQualityIssue toDataQualityIssue(DataQualityIssueEntity entity) {
        return MPIMergeDetail.DataQualityIssue.builder()
            .issueId(entity.getId().toString())
            .issueType(entity.getIssueType())
            .severity(entity.getSeverity())
            .description(entity.getDescription())
            .affectedField(entity.getAffectedField())
            .recommendation(entity.getRecommendation())
            .build();
    }
    
    private List<String> extractMatchedAttributes(MPIMergeEntity entity) {
        if (entity.getMatchingDetails() == null) {
            return List.of();
        }
        // Extract matched attribute names from matching details JSON
        return entity.getMatchingDetails().keySet().stream()
            .filter(key -> key.startsWith("match_"))
            .collect(Collectors.toList());
    }
    
    private String determinePriority(MPIMergeEntity entity) {
        if (entity.getConfidenceScore() == null) return "MEDIUM";
        
        double confidence = entity.getConfidenceScore();
        if (confidence < 0.7) return "CRITICAL";
        if (confidence < 0.8) return "HIGH";
        if (confidence < 0.9) return "MEDIUM";
        return "LOW";
    }
    
    private MPIMergeDetail.PatientSnapshot buildPatientSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null) {
            return null;
        }
        
        return MPIMergeDetail.PatientSnapshot.builder()
            .patientId((String) snapshot.get("patientId"))
            .firstName((String) snapshot.get("firstName"))
            .lastName((String) snapshot.get("lastName"))
            .dateOfBirth((String) snapshot.get("dateOfBirth"))
            .gender((String) snapshot.get("gender"))
            .ssn((String) snapshot.get("ssn"))
            .mrn((String) snapshot.get("mrn"))
            .build();
    }
    
    private List<MPIMergeDetail.AttributeMatch> buildAttributeMatches(Map<String, Object> matchingDetails) {
        if (matchingDetails == null) {
            return List.of();
        }
        
        // Build attribute matches from matching details JSON
        return matchingDetails.entrySet().stream()
            .filter(e -> e.getKey().startsWith("match_"))
            .map(e -> MPIMergeDetail.AttributeMatch.builder()
                .attributeName(e.getKey().replace("match_", ""))
                .matchScore(0.0)
                .matchType("EXACT")
                .build())
            .collect(Collectors.toList());
    }
}
