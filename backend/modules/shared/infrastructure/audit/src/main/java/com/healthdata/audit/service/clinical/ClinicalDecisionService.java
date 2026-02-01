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
        // Count total decisions for this tenant (not all tenants)
        Long totalDecisions = clinicalDecisionRepository.countByTenantId(tenantId);
        Long approvedDecisions = clinicalDecisionRepository.countByTenantIdAndStatus(tenantId, "APPROVED");
        Long rejectedDecisions = clinicalDecisionRepository.countByTenantIdAndStatus(tenantId, "REJECTED");
        Long pendingReview = clinicalDecisionRepository.countByTenantIdAndStatus(tenantId, "PENDING");
        
        Double approvalRate = totalDecisions > 0 ? (approvedDecisions.doubleValue() / totalDecisions) * 100 : 0.0;
        Double averageConfidenceScore = clinicalDecisionRepository.getAverageConfidenceScore(tenantId);

        // Calculate override rate from database
        Long overrideCount = clinicalDecisionRepository.countOverridesForTenant(tenantId);
        Double overrideRate = totalDecisions > 0 ? (overrideCount.doubleValue() / totalDecisions) * 100 : 0.0;

        // Calculate average review time from database timestamps
        Double averageReviewTime = clinicalDecisionRepository.getAverageReviewTimeHours(tenantId);
        if (averageReviewTime == null) {
            averageReviewTime = 0.0;
        }

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
            .overrideRate(overrideRate)
            .averageConfidenceScore(averageConfidenceScore != null ? averageConfidenceScore : 0.0)
            .averageReviewTimeHours(averageReviewTime != null ? averageReviewTime.intValue() : null)
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
            .relatedAlertsCount(calculateRelatedAlertsCount(entity))
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
            .reviewHistory(buildReviewHistory(entity))
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
            .dueDate(extractDueDate(entity.getClinicalDetails()))
            .daysPastDue(calculateDaysPastDue(extractDueDate(entity.getClinicalDetails())))
            .priority(determinePriority(entity.getConfidenceScore(), entity.getAlertSeverity()))
            .guidelineReference(extractGuidelineReference(entity.getEvidence()))
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
            .contributingFactors(extractContributingFactors(entity.getClinicalDetails()))
            .assessmentModel("AI-BASED")
            .evidenceGrade(entity.getEvidenceGrade())
            .recommendedInterventions(extractRecommendedInterventions(entity.getRecommendation()))
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
    
    @SuppressWarnings("unchecked")
    private ClinicalDecisionDetail.PatientContext buildPatientContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return ClinicalDecisionDetail.PatientContext.builder().build();
        }

        return ClinicalDecisionDetail.PatientContext.builder()
            .patientId(context.containsKey("patientId") ? context.get("patientId").toString() : null)
            .patientName(context.containsKey("patientName") ? context.get("patientName").toString() : "Unknown")
            .age(context.containsKey("age") ? ((Number) context.get("age")).intValue() : null)
            .gender(context.containsKey("gender") ? context.get("gender").toString() : null)
            .activeConditions(context.containsKey("activeConditions") ? (List<String>) context.get("activeConditions") : new ArrayList<>())
            .currentMedications(context.containsKey("currentMedications") ? (List<String>) context.get("currentMedications") : new ArrayList<>())
            .allergies(context.containsKey("allergies") ? (List<String>) context.get("allergies") : new ArrayList<>())
            .riskLevel(context.containsKey("riskLevel") ? context.get("riskLevel").toString() : null)
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private ClinicalDecisionDetail.ClinicalRecommendation buildRecommendation(Map<String, Object> recommendation) {
        if (recommendation == null || recommendation.isEmpty()) {
            return ClinicalDecisionDetail.ClinicalRecommendation.builder().build();
        }

        return ClinicalDecisionDetail.ClinicalRecommendation.builder()
            .recommendationType(recommendation.containsKey("recommendationType") ?
                recommendation.get("recommendationType").toString() : "CLINICAL")
            .recommendationText(recommendation.containsKey("recommendationText") ?
                recommendation.get("recommendationText").toString() : "")
            .actionItems(recommendation.containsKey("actionItems") ?
                (Map<String, Object>) recommendation.get("actionItems") : null)
            .urgency(recommendation.containsKey("urgency") ?
                recommendation.get("urgency").toString() : "ROUTINE")
            .evidenceGrade(recommendation.containsKey("evidenceGrade") ?
                recommendation.get("evidenceGrade").toString() : null)
            .confidenceScore(recommendation.containsKey("confidenceScore") ?
                ((Number) recommendation.get("confidenceScore")).doubleValue() : null)
            .specialty(recommendation.containsKey("specialty") ?
                recommendation.get("specialty").toString() : null)
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private List<ClinicalDecisionDetail.ClinicalEvidence> buildEvidence(Map<String, Object> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return new ArrayList<>();
        }

        List<ClinicalDecisionDetail.ClinicalEvidence> evidenceList = new ArrayList<>();

        if (evidence.containsKey("evidenceItems")) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) evidence.get("evidenceItems");
            for (Map<String, Object> item : items) {
                evidenceList.add(ClinicalDecisionDetail.ClinicalEvidence.builder()
                    .evidenceId(item.containsKey("evidenceId") ? item.get("evidenceId").toString() : null)
                    .evidenceType(item.containsKey("evidenceType") ? item.get("evidenceType").toString() : "GUIDELINE")
                    .evidenceGrade(item.containsKey("evidenceGrade") ? item.get("evidenceGrade").toString() : null)
                    .citation(item.containsKey("citation") ? item.get("citation").toString() : "")
                    .summary(item.containsKey("summary") ? item.get("summary").toString() : "")
                    .relevanceScore(item.containsKey("relevanceScore") ? item.get("relevanceScore").toString() : null)
                    .publishedDate(item.containsKey("publishedDate") ?
                        LocalDateTime.parse(item.get("publishedDate").toString()) : null)
                    .build());
            }
        }

        return evidenceList;
    }
    
    @SuppressWarnings("unchecked")
    private List<ClinicalDecisionDetail.DrugInteraction> buildDrugInteractions(Map<String, Object> details) {
        if (details == null || !details.containsKey("drugInteractions")) {
            return new ArrayList<>();
        }

        List<ClinicalDecisionDetail.DrugInteraction> interactions = new ArrayList<>();
        List<Map<String, Object>> items = (List<Map<String, Object>>) details.get("drugInteractions");

        for (Map<String, Object> item : items) {
            interactions.add(ClinicalDecisionDetail.DrugInteraction.builder()
                .drugA(item.containsKey("drugA") ? item.get("drugA").toString() : "")
                .drugB(item.containsKey("drugB") ? item.get("drugB").toString() : "")
                .interactionSeverity(item.containsKey("interactionSeverity") ? item.get("interactionSeverity").toString() : "MODERATE")
                .interactionType(item.containsKey("interactionType") ? item.get("interactionType").toString() : "DRUG-DRUG")
                .clinicalEffect(item.containsKey("clinicalEffect") ? item.get("clinicalEffect").toString() : "")
                .managementRecommendation(item.containsKey("managementRecommendation") ? item.get("managementRecommendation").toString() : "")
                .build());
        }

        return interactions;
    }
    
    @SuppressWarnings("unchecked")
    private List<ClinicalDecisionDetail.CareGap> buildCareGaps(Map<String, Object> details) {
        if (details == null || !details.containsKey("careGaps")) {
            return new ArrayList<>();
        }

        List<ClinicalDecisionDetail.CareGap> careGaps = new ArrayList<>();
        List<Map<String, Object>> items = (List<Map<String, Object>>) details.get("careGaps");

        for (Map<String, Object> item : items) {
            LocalDateTime dueDate = null;
            if (item.containsKey("dueDate")) {
                try {
                    dueDate = LocalDateTime.parse(item.get("dueDate").toString());
                } catch (Exception e) {
                    log.warn("Failed to parse due date: {}", e.getMessage());
                }
            }

            careGaps.add(ClinicalDecisionDetail.CareGap.builder()
                .gapType(item.containsKey("gapType") ? item.get("gapType").toString() : "PREVENTIVE_CARE")
                .serviceDescription(item.containsKey("serviceDescription") ? item.get("serviceDescription").toString() : "")
                .dueDate(dueDate)
                .daysPastDue(item.containsKey("daysPastDue") ? ((Number) item.get("daysPastDue")).intValue() : null)
                .guidelineReference(item.containsKey("guidelineReference") ? item.get("guidelineReference").toString() : "")
                .priority(item.containsKey("priority") ? item.get("priority").toString() : "MEDIUM")
                .build());
        }

        return careGaps;
    }
    
    @SuppressWarnings("unchecked")
    private ClinicalDecisionDetail.RiskAssessment buildRiskAssessment(Map<String, Object> details) {
        if (details == null || !details.containsKey("riskAssessment")) {
            return null;
        }

        Map<String, Object> assessment = (Map<String, Object>) details.get("riskAssessment");

        // Build risk factors list
        List<ClinicalDecisionDetail.RiskFactor> riskFactors = new ArrayList<>();
        if (assessment.containsKey("riskFactors")) {
            List<Map<String, Object>> factorMaps = (List<Map<String, Object>>) assessment.get("riskFactors");
            for (Map<String, Object> factorMap : factorMaps) {
                riskFactors.add(ClinicalDecisionDetail.RiskFactor.builder()
                    .factorName(factorMap.containsKey("factorName") ? factorMap.get("factorName").toString() : "")
                    .factorValue(factorMap.containsKey("factorValue") ? factorMap.get("factorValue").toString() : "")
                    .contribution(factorMap.containsKey("contribution") ? factorMap.get("contribution").toString() : "MODERATE")
                    .modifiable(factorMap.containsKey("modifiable") ? (Boolean) factorMap.get("modifiable") : false)
                    .build());
            }
        }

        return ClinicalDecisionDetail.RiskAssessment.builder()
            .overallRiskLevel(assessment.containsKey("overallRiskLevel") ? assessment.get("overallRiskLevel").toString() : "MODERATE")
            .riskScore(assessment.containsKey("riskScore") ? ((Number) assessment.get("riskScore")).doubleValue() : 0.0)
            .riskFactors(riskFactors)
            .mitigationStrategies(assessment.containsKey("mitigationStrategies") ?
                (List<String>) assessment.get("mitigationStrategies") : new ArrayList<>())
            .assessmentModel(assessment.containsKey("assessmentModel") ? assessment.get("assessmentModel").toString() : "AI-BASED")
            .build();
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

    /**
     * Calculate count of related alerts for the same patient
     */
    private Integer calculateRelatedAlertsCount(ClinicalDecisionEntity entity) {
        if (entity.getPatientId() == null || entity.getTenantId() == null) {
            return 0;
        }

        try {
            Long count = clinicalDecisionRepository.countByTenantIdAndPatientIdAndDecisionType(
                entity.getTenantId(),
                entity.getPatientId(),
                entity.getDecisionType()
            );
            return count.intValue() - 1; // Exclude current decision
        } catch (Exception e) {
            log.warn("Failed to calculate related alerts count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Build review history from entity tracking fields
     */
    private List<ClinicalDecisionDetail.ReviewAction> buildReviewHistory(ClinicalDecisionEntity entity) {
        List<ClinicalDecisionDetail.ReviewAction> history = new ArrayList<>();

        // Add review event if reviewed
        if (entity.getReviewedAt() != null) {
            history.add(ClinicalDecisionDetail.ReviewAction.builder()
                .actionType(entity.getReviewStatus())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .notes(entity.getOverrideReason())
                .actionDetails(null)
                .build());
        }

        return history;
    }

    /**
     * Extract due date from clinical details JSON
     */
    private LocalDate extractDueDate(Map<String, Object> details) {
        if (details == null || !details.containsKey("dueDate")) {
            return LocalDate.now().plusDays(30); // Default 30 days from now
        }

        try {
            String dueDateStr = details.get("dueDate").toString();
            return LocalDate.parse(dueDateStr);
        } catch (Exception e) {
            log.warn("Failed to parse due date: {}", e.getMessage());
            return LocalDate.now().plusDays(30);
        }
    }

    /**
     * Calculate days past due from a due date
     */
    private Integer calculateDaysPastDue(LocalDate dueDate) {
        if (dueDate == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        if (today.isAfter(dueDate)) {
            return Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(dueDate, today));
        }

        return 0; // Not past due yet
    }

    /**
     * Extract guideline reference from evidence JSON
     */
    private String extractGuidelineReference(Map<String, Object> evidence) {
        if (evidence == null || !evidence.containsKey("guidelineReference")) {
            return "";
        }

        return evidence.get("guidelineReference").toString();
    }

    /**
     * Extract contributing factors from clinical details JSON
     */
    @SuppressWarnings("unchecked")
    private List<RiskStratificationDTO.RiskFactorDTO> extractContributingFactors(Map<String, Object> details) {
        if (details == null || !details.containsKey("contributingFactors")) {
            return new ArrayList<>();
        }

        try {
            List<RiskStratificationDTO.RiskFactorDTO> factors = new ArrayList<>();
            List<Map<String, Object>> factorMaps = (List<Map<String, Object>>) details.get("contributingFactors");

            for (Map<String, Object> factorMap : factorMaps) {
                factors.add(RiskStratificationDTO.RiskFactorDTO.builder()
                    .factorName(factorMap.containsKey("factorName") ? factorMap.get("factorName").toString() : "")
                    .factorValue(factorMap.containsKey("factorValue") ? factorMap.get("factorValue").toString() : "")
                    .contribution(factorMap.containsKey("contribution") ? factorMap.get("contribution").toString() : "MODERATE")
                    .modifiable(factorMap.containsKey("modifiable") ? (Boolean) factorMap.get("modifiable") : false)
                    .build());
            }

            return factors;
        } catch (Exception e) {
            log.warn("Failed to extract contributing factors: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Extract recommended interventions from recommendation JSON
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRecommendedInterventions(Map<String, Object> recommendation) {
        if (recommendation == null || !recommendation.containsKey("recommendedInterventions")) {
            return new ArrayList<>();
        }

        try {
            return (List<String>) recommendation.get("recommendedInterventions");
        } catch (Exception e) {
            log.warn("Failed to extract recommended interventions: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
