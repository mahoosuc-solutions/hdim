package com.healthdata.audit.service.clinical;

import com.healthdata.audit.dto.clinical.*;
import com.healthdata.audit.entity.clinical.ClinicalDecisionEntity;
import com.healthdata.audit.repository.clinical.ClinicalDecisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for clinical decision audit operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalDecisionService {
    
    private final ClinicalDecisionRepository clinicalDecisionRepository;
    
    public Page<ClinicalDecisionEvent> getDecisionHistory(
            String tenantId,
            ClinicalDecisionFilter filter,
            Pageable pageable) {
        
        LocalDateTime startDateTime = filter.getStartDate() != null 
            ? filter.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime = filter.getEndDate() != null 
            ? filter.getEndDate().atTime(23, 59, 59) : null;
        
        Page<ClinicalDecisionEntity> entities = clinicalDecisionRepository.findDecisionHistory(
            tenantId,
            filter.getDecisionType(),
            filter.getAlertSeverity(),
            filter.getReviewStatus(),
            startDateTime,
            endDateTime,
            filter.getEvidenceGrade(),
            filter.getHasOverride(),
            filter.getSpecialtyArea(),
            pageable
        );
        
        return entities.map(this::toClinicalDecisionEvent);
    }
    
    public ClinicalDecisionDetail getDecisionDetail(String decisionId, String tenantId) {
        ClinicalDecisionEntity entity = clinicalDecisionRepository
            .findByDecisionIdAndTenantId(decisionId, tenantId)
            .orElseThrow(() -> new RuntimeException("Clinical decision not found"));
        
        return toClinicalDecisionDetail(entity);
    }
    
    @Transactional
    public ClinicalReviewResult reviewDecision(
            String decisionId,
            String tenantId,
            ClinicalReviewRequest request,
            String reviewedBy) {
        
        ClinicalDecisionEntity entity = clinicalDecisionRepository
            .findByDecisionIdAndTenantId(decisionId, tenantId)
            .orElseThrow(() -> new RuntimeException("Clinical decision not found"));
        
        entity.setReviewStatus(request.getReviewOutcome());
        entity.setReviewedBy(reviewedBy);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setReviewNotes(request.getReviewNotes());
        
        if (Boolean.TRUE.equals(request.getApplyOverride())) {
            entity.setHasOverride(true);
            entity.setOverrideReason(request.getOverrideReason());
            entity.setOverrideAppliedBy(reviewedBy);
            entity.setOverrideAppliedAt(LocalDateTime.now());
        }
        
        clinicalDecisionRepository.save(entity);
        
        log.info("Clinical decision {} reviewed by {} with outcome: {}", 
            decisionId, reviewedBy, request.getReviewOutcome());
        
        return ClinicalReviewResult.builder()
            .decisionId(decisionId)
            .reviewOutcome(request.getReviewOutcome())
            .reviewedBy(reviewedBy)
            .reviewedAt(LocalDateTime.now())
            .success(true)
            .message("Decision reviewed successfully")
            .overrideApplied(request.getApplyOverride())
            .build();
    }
    
    public Page<MedicationAlertDTO> getMedicationAlerts(
            String tenantId,
            String severity,
            Pageable pageable) {
        
        ClinicalDecisionFilter filter = ClinicalDecisionFilter.builder()
            .decisionType("MEDICATION_ALERT")
            .alertSeverity(severity)
            .build();
        
        Page<ClinicalDecisionEntity> entities = clinicalDecisionRepository.findDecisionHistory(
            tenantId, "MEDICATION_ALERT", severity, null, null, null, null, null, null, pageable
        );
        
        return entities.map(this::toMedicationAlertDTO);
    }
    
    public Page<CareGapDTO> getCareGaps(
            String tenantId,
            String status,
            Pageable pageable) {
        
        Page<ClinicalDecisionEntity> entities = clinicalDecisionRepository.findDecisionHistory(
            tenantId, "CARE_GAP", null, status, null, null, null, null, null, pageable
        );
        
        return entities.map(this::toCareGapDTO);
    }
    
    public Page<RiskStratificationDTO> getRiskStratifications(
            String tenantId,
            String riskLevel,
            Pageable pageable) {
        
        Page<ClinicalDecisionEntity> entities = clinicalDecisionRepository.findDecisionHistory(
            tenantId, "RISK_STRATIFICATION", riskLevel, null, null, null, null, null, null, pageable
        );
        
        return entities.map(this::toRiskStratificationDTO);
    }
    
    public ClinicalMetrics getMetrics(String tenantId, LocalDate startDate, LocalDate endDate) {
        Long totalDecisions = clinicalDecisionRepository.count();
        Long approvedDecisions = clinicalDecisionRepository.countByTenantIdAndStatus(tenantId, "APPROVED");
        Long rejectedDecisions = clinicalDecisionRepository.countByTenantIdAndStatus(tenantId, "REJECTED");
        Long pendingReview = clinicalDecisionRepository.countByTenantIdAndStatus(tenantId, "PENDING");
        
        Double approvalRate = totalDecisions > 0 ? (approvedDecisions.doubleValue() / totalDecisions) * 100 : 0.0;
        Double averageConfidenceScore = clinicalDecisionRepository.getAverageConfidenceScore(tenantId);
        
        // Decision type distribution
        Long medicationAlerts = clinicalDecisionRepository.countByTenantIdAndDecisionType(tenantId, "MEDICATION_ALERT");
        Long careGaps = clinicalDecisionRepository.countByTenantIdAndDecisionType(tenantId, "CARE_GAP");
        Long riskStratifications = clinicalDecisionRepository.countByTenantIdAndDecisionType(tenantId, "RISK_STRATIFICATION");
        Long clinicalPathways = clinicalDecisionRepository.countByTenantIdAndDecisionType(tenantId, "CLINICAL_PATHWAY");
        
        // Severity distribution
        Long critical = clinicalDecisionRepository.countByTenantIdAndSeverity(tenantId, "CRITICAL");
        Long high = clinicalDecisionRepository.countByTenantIdAndSeverity(tenantId, "HIGH");
        Long moderate = clinicalDecisionRepository.countByTenantIdAndSeverity(tenantId, "MODERATE");
        Long low = clinicalDecisionRepository.countByTenantIdAndSeverity(tenantId, "LOW");
        
        // Evidence grade distribution
        Long gradeA = clinicalDecisionRepository.countByTenantIdAndEvidenceGrade(tenantId, "A");
        Long gradeB = clinicalDecisionRepository.countByTenantIdAndEvidenceGrade(tenantId, "B");
        Long gradeC = clinicalDecisionRepository.countByTenantIdAndEvidenceGrade(tenantId, "C");
        Long gradeD = clinicalDecisionRepository.countByTenantIdAndEvidenceGrade(tenantId, "D");
        
        return ClinicalMetrics.builder()
            .totalDecisions(totalDecisions)
            .approvedDecisions(approvedDecisions)
            .rejectedDecisions(rejectedDecisions)
            .pendingReview(pendingReview)
            .approvalRate(approvalRate)
            .overrideRate(0.0) // TODO: Calculate from override tracking
            .averageConfidenceScore(averageConfidenceScore != null ? averageConfidenceScore : 0.0)
            .averageReviewTimeHours(24) // TODO: Calculate from review timestamps
            .decisionTypeDistribution(ClinicalMetrics.DecisionTypeDistribution.builder()
                .medicationAlerts(medicationAlerts)
                .careGaps(careGaps)
                .riskStratifications(riskStratifications)
                .clinicalPathways(clinicalPathways)
                .build())
            .severityDistribution(ClinicalMetrics.SeverityDistribution.builder()
                .critical(critical)
                .high(high)
                .moderate(moderate)
                .low(low)
                .build())
            .evidenceGradeDistribution(ClinicalMetrics.EvidenceGradeDistribution.builder()
                .gradeA(gradeA)
                .gradeB(gradeB)
                .gradeC(gradeC)
                .gradeD(gradeD)
                .build())
            .build();
    }
    
    public ClinicalTrendData getPerformanceTrends(String tenantId, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        
        Page<ClinicalDecisionEntity> decisions = clinicalDecisionRepository.findByTenantIdAndDateRange(
            tenantId, startDate, endDate, Pageable.unpaged()
        );
        
        Map<LocalDate, List<ClinicalDecisionEntity>> groupedByDate = decisions.getContent().stream()
            .collect(Collectors.groupingBy(d -> d.getDecisionTimestamp().toLocalDate()));
        
        List<ClinicalTrendData.DailyClinicalTrend> dailyTrends = groupedByDate.entrySet().stream()
            .map(entry -> {
                LocalDate date = entry.getKey();
                List<ClinicalDecisionEntity> dayDecisions = entry.getValue();
                
                long total = dayDecisions.size();
                long approved = dayDecisions.stream().filter(d -> "APPROVED".equals(d.getReviewStatus())).count();
                long rejected = dayDecisions.stream().filter(d -> "REJECTED".equals(d.getReviewStatus())).count();
                long overridden = dayDecisions.stream().filter(d -> Boolean.TRUE.equals(d.getHasOverride())).count();
                
                double approvalRate = total > 0 ? (approved * 100.0 / total) : 0.0;
                double overrideRate = total > 0 ? (overridden * 100.0 / total) : 0.0;
                double avgConfidence = dayDecisions.stream()
                    .filter(d -> d.getConfidenceScore() != null)
                    .mapToDouble(ClinicalDecisionEntity::getConfidenceScore)
                    .average()
                    .orElse(0.0);
                
                return ClinicalTrendData.DailyClinicalTrend.builder()
                    .date(date)
                    .totalDecisions(total)
                    .approved(approved)
                    .rejected(rejected)
                    .overridden(overridden)
                    .approvalRate(approvalRate)
                    .overrideRate(overrideRate)
                    .averageConfidence(avgConfidence)
                    .build();
            })
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .collect(Collectors.toList());
        
        double avgApprovalRate = dailyTrends.stream()
            .mapToDouble(ClinicalTrendData.DailyClinicalTrend::getApprovalRate)
            .average()
            .orElse(0.0);
        
        double avgOverrideRate = dailyTrends.stream()
            .mapToDouble(ClinicalTrendData.DailyClinicalTrend::getOverrideRate)
            .average()
            .orElse(0.0);
        
        double avgConfidence = dailyTrends.stream()
            .mapToDouble(ClinicalTrendData.DailyClinicalTrend::getAverageConfidence)
            .average()
            .orElse(0.0);
        
        return ClinicalTrendData.builder()
            .dailyTrends(dailyTrends)
            .averageApprovalRate(avgApprovalRate)
            .averageOverrideRate(avgOverrideRate)
            .averageConfidenceScore(avgConfidence)
            .build();
    }
    
    private ClinicalDecisionEvent toClinicalDecisionEvent(ClinicalDecisionEntity entity) {
        String priority = determinePriority(entity.getConfidenceScore(), entity.getAlertSeverity());
        
        return ClinicalDecisionEvent.builder()
            .decisionId(entity.getId().toString())
            .decisionType(entity.getDecisionType())
            .patientId(entity.getPatientId())
            .patientName(extractPatientName(entity.getPatientContext()))
            .alertSeverity(entity.getAlertSeverity())
            .reviewStatus(entity.getReviewStatus())
            .decisionTimestamp(entity.getDecisionTimestamp())
            .evidenceGrade(entity.getEvidenceGrade())
            .confidenceScore(entity.getConfidenceScore())
            .specialtyArea(entity.getSpecialtyArea())
            .clinicalRecommendation(extractRecommendation(entity.getRecommendation()))
            .priority(priority)
            .hasOverride(entity.getHasOverride())
            .overrideReason(entity.getOverrideReason())
            .relatedAlertsCount(0) // TODO: Calculate related alerts
            .build();
    }
    
    private ClinicalDecisionDetail toClinicalDecisionDetail(ClinicalDecisionEntity entity) {
        return ClinicalDecisionDetail.builder()
            .decisionId(entity.getId().toString())
            .decisionType(entity.getDecisionType())
            .reviewStatus(entity.getReviewStatus())
            .decisionTimestamp(entity.getDecisionTimestamp())
            .patientContext(buildPatientContext(entity.getPatientContext()))
            .recommendation(buildRecommendation(entity.getRecommendation()))
            .evidence(buildEvidence(entity.getEvidence()))
            .drugInteractions("MEDICATION_ALERT".equals(entity.getDecisionType()) ? 
                buildDrugInteractions(entity.getClinicalDetails()) : new ArrayList<>())
            .careGaps("CARE_GAP".equals(entity.getDecisionType()) ? 
                buildCareGaps(entity.getClinicalDetails()) : new ArrayList<>())
            .riskAssessment("RISK_STRATIFICATION".equals(entity.getDecisionType()) ? 
                buildRiskAssessment(entity.getClinicalDetails()) : null)
            .reviewHistory(new ArrayList<>()) // TODO: Build from review tracking fields
            .build();
    }
    
    private MedicationAlertDTO toMedicationAlertDTO(ClinicalDecisionEntity entity) {
        return MedicationAlertDTO.builder()
            .alertId(entity.getId().toString())
            .patientId(entity.getPatientId())
            .alertType(extractAlertType(entity.getClinicalDetails()))
            .severity(entity.getAlertSeverity())
            .involvedMedications(extractMedications(entity.getClinicalDetails()))
            .alertMessage(extractRecommendation(entity.getRecommendation()))
            .clinicalRecommendation(extractRecommendation(entity.getRecommendation()))
            .evidenceGrade(entity.getEvidenceGrade())
            .acknowledged(entity.getReviewedAt() != null)
            .acknowledgedBy(entity.getReviewedBy())
            .build();
    }
    
    private CareGapDTO toCareGapDTO(ClinicalDecisionEntity entity) {
        return CareGapDTO.builder()
            .gapId(entity.getId().toString())
            .patientId(entity.getPatientId())
            .gapType(extractGapType(entity.getClinicalDetails()))
            .serviceDescription(extractRecommendation(entity.getRecommendation()))
            .dueDate(LocalDate.now()) // TODO: Extract from clinical details
            .daysPastDue(0) // TODO: Calculate
            .priority(determinePriority(entity.getConfidenceScore(), entity.getAlertSeverity()))
            .guidelineReference("") // TODO: Extract from evidence
            .status(entity.getReviewStatus())
            .evidenceGrade(entity.getEvidenceGrade())
            .build();
    }
    
    private RiskStratificationDTO toRiskStratificationDTO(ClinicalDecisionEntity entity) {
        return RiskStratificationDTO.builder()
            .stratificationId(entity.getId().toString())
            .patientId(entity.getPatientId())
            .riskCategory(extractRiskCategory(entity.getClinicalDetails()))
            .overallRiskLevel(entity.getAlertSeverity())
            .riskScore(entity.getConfidenceScore())
            .contributingFactors(new ArrayList<>()) // TODO: Extract from clinical details
            .assessmentModel("AI-BASED")
            .evidenceGrade(entity.getEvidenceGrade())
            .recommendedInterventions(new ArrayList<>()) // TODO: Extract from recommendation
            .build();
    }
    
    // Helper methods
    private String determinePriority(Double confidenceScore, String severity) {
        if ("CRITICAL".equals(severity) || (confidenceScore != null && confidenceScore < 0.7)) {
            return "CRITICAL";
        } else if ("HIGH".equals(severity) || (confidenceScore != null && confidenceScore < 0.8)) {
            return "HIGH";
        } else if ("MODERATE".equals(severity) || (confidenceScore != null && confidenceScore < 0.9)) {
            return "MEDIUM";
        }
        return "LOW";
    }
    
    @SuppressWarnings("unchecked")
    private String extractPatientName(Map<String, Object> context) {
        if (context != null && context.containsKey("patientName")) {
            return context.get("patientName").toString();
        }
        return "Unknown";
    }
    
    @SuppressWarnings("unchecked")
    private String extractRecommendation(Map<String, Object> recommendation) {
        if (recommendation != null && recommendation.containsKey("recommendationText")) {
            return recommendation.get("recommendationText").toString();
        }
        return "";
    }
    
    private ClinicalDecisionDetail.PatientContext buildPatientContext(Map<String, Object> context) {
        // TODO: Implement full patient context building from JSON
        return ClinicalDecisionDetail.PatientContext.builder().build();
    }
    
    private ClinicalDecisionDetail.ClinicalRecommendation buildRecommendation(Map<String, Object> recommendation) {
        // TODO: Implement full recommendation building from JSON
        return ClinicalDecisionDetail.ClinicalRecommendation.builder().build();
    }
    
    private List<ClinicalDecisionDetail.ClinicalEvidence> buildEvidence(Map<String, Object> evidence) {
        // TODO: Implement evidence building from JSON
        return new ArrayList<>();
    }
    
    private List<ClinicalDecisionDetail.DrugInteraction> buildDrugInteractions(Map<String, Object> details) {
        // TODO: Implement drug interaction building from JSON
        return new ArrayList<>();
    }
    
    private List<ClinicalDecisionDetail.CareGap> buildCareGaps(Map<String, Object> details) {
        // TODO: Implement care gap building from JSON
        return new ArrayList<>();
    }
    
    private ClinicalDecisionDetail.RiskAssessment buildRiskAssessment(Map<String, Object> details) {
        // TODO: Implement risk assessment building from JSON
        return null;
    }
    
    private String extractAlertType(Map<String, Object> details) {
        return details != null && details.containsKey("alertType") ? 
            details.get("alertType").toString() : "GENERAL";
    }
    
    @SuppressWarnings("unchecked")
    private List<String> extractMedications(Map<String, Object> details) {
        if (details != null && details.containsKey("medications")) {
            return (List<String>) details.get("medications");
        }
        return new ArrayList<>();
    }
    
    private String extractGapType(Map<String, Object> details) {
        return details != null && details.containsKey("gapType") ? 
            details.get("gapType").toString() : "GENERAL";
    }
    
    private String extractRiskCategory(Map<String, Object> details) {
        return details != null && details.containsKey("riskCategory") ? 
            details.get("riskCategory").toString() : "GENERAL";
    }
}
