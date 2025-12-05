package com.healthdata.sdoh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.sdoh.entity.SdohRiskScoreEntity;
import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.repository.SdohRiskScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SDOH Risk Scoring and Impact Assessment Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SdohRiskCalculator {

    private final SdohRiskScoreRepository riskScoreRepository;
    private final ObjectMapper objectMapper;

    private static final Map<SdohCategory, Double> CATEGORY_WEIGHTS = new HashMap<>();

    static {
        CATEGORY_WEIGHTS.put(SdohCategory.FOOD_INSECURITY, 0.15);
        CATEGORY_WEIGHTS.put(SdohCategory.HOUSING_INSTABILITY, 0.20);
        CATEGORY_WEIGHTS.put(SdohCategory.TRANSPORTATION, 0.10);
        CATEGORY_WEIGHTS.put(SdohCategory.FINANCIAL_STRAIN, 0.15);
        CATEGORY_WEIGHTS.put(SdohCategory.EDUCATION, 0.08);
        CATEGORY_WEIGHTS.put(SdohCategory.EMPLOYMENT, 0.12);
        CATEGORY_WEIGHTS.put(SdohCategory.UTILITIES, 0.05);
        CATEGORY_WEIGHTS.put(SdohCategory.SOCIAL_ISOLATION, 0.07);
        CATEGORY_WEIGHTS.put(SdohCategory.INTERPERSONAL_VIOLENCE, 0.08);
    }

    @Transactional
    public SdohRiskScore calculateRiskScore(SdohAssessment assessment) {
        Map<SdohCategory, Double> categoryScores = calculateCategoryScores(assessment.getIdentifiedNeeds());
        double totalScore = categoryScores.values().stream().mapToDouble(Double::doubleValue).sum() * 100;

        SdohRiskScore riskScore = SdohRiskScore.builder()
                .scoreId(java.util.UUID.randomUUID().toString())
                .patientId(assessment.getPatientId())
                .tenantId(assessment.getTenantId())
                .totalScore(totalScore)
                .categoryScores(categoryScores)
                .riskLevel(SdohRiskScore.RiskLevel.fromScore(totalScore))
                .assessmentId(assessment.getAssessmentId())
                .calculatedAt(LocalDateTime.now())
                .build();

        SdohRiskScoreEntity entity = convertToEntity(riskScore);
        riskScoreRepository.save(entity);

        return riskScore;
    }

    public Map<SdohCategory, Double> calculateCategoryScores(Map<SdohCategory, Boolean> needs) {
        Map<SdohCategory, Double> categoryScores = new HashMap<>();

        for (Map.Entry<SdohCategory, Boolean> entry : needs.entrySet()) {
            if (entry.getValue()) {
                double weight = getCategoryWeight(entry.getKey());
                categoryScores.put(entry.getKey(), weight);
            }
        }

        return categoryScores;
    }

    public double getCategoryWeight(SdohCategory category) {
        return CATEGORY_WEIGHTS.getOrDefault(category, 0.05);
    }

    public SdohImpact assessImpact(String patientId, SdohCategory category) {
        return SdohImpact.builder()
                .impactId(java.util.UUID.randomUUID().toString())
                .patientId(patientId)
                .category(category)
                .impactLevel(SdohImpact.ImpactLevel.MODERATE)
                .predictedHospitalizationRisk(0.15)
                .predictedEmergencyVisitRisk(0.20)
                .predictedMedicationAdherenceImpact(-0.10)
                .build();
    }

    public double predictHospitalizationRisk(SdohRiskScore riskScore) {
        // Higher SDOH risk correlates with higher hospitalization risk
        return Math.min(1.0, riskScore.getTotalScore() / 100.0 * 0.3);
    }

    public double predictEmergencyVisitRisk(SdohRiskScore riskScore) {
        // Higher SDOH risk correlates with higher ER visit risk
        return Math.min(1.0, riskScore.getTotalScore() / 100.0 * 0.4);
    }

    public double predictMedicationAdherenceImpact(SdohRiskScore riskScore) {
        // Higher SDOH risk negatively impacts medication adherence
        return -(riskScore.getTotalScore() / 100.0 * 0.3);
    }

    public List<SdohRiskScore> getRiskScoreHistory(String tenantId, String patientId) {
        return riskScoreRepository.findByTenantIdAndPatientIdOrderByCalculatedAtDesc(tenantId, patientId).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    public String identifyTrend(List<SdohRiskScore> history) {
        if (history.size() < 2) {
            return "INSUFFICIENT_DATA";
        }

        double firstScore = history.get(history.size() - 1).getTotalScore();
        double lastScore = history.get(0).getTotalScore();

        if (lastScore > firstScore + 5) {
            return "INCREASING";
        } else if (lastScore < firstScore - 5) {
            return "DECREASING";
        } else {
            return "STABLE";
        }
    }

    private SdohRiskScoreEntity convertToEntity(SdohRiskScore score) {
        try {
            return SdohRiskScoreEntity.builder()
                    .scoreId(score.getScoreId())
                    .patientId(score.getPatientId())
                    .tenantId(score.getTenantId())
                    .totalScore(score.getTotalScore())
                    .categoryScoresJson(objectMapper.writeValueAsString(score.getCategoryScores()))
                    .riskLevel(score.getRiskLevel())
                    .assessmentId(score.getAssessmentId())
                    .calculatedAt(score.getCalculatedAt())
                    .notes(score.getNotes())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting risk score to entity", e);
        }
    }

    @SuppressWarnings("unchecked")
    private SdohRiskScore convertToModel(SdohRiskScoreEntity entity) {
        try {
            return SdohRiskScore.builder()
                    .scoreId(entity.getScoreId())
                    .patientId(entity.getPatientId())
                    .tenantId(entity.getTenantId())
                    .totalScore(entity.getTotalScore())
                    .categoryScores(entity.getCategoryScoresJson() != null ?
                            objectMapper.readValue(entity.getCategoryScoresJson(), Map.class) : new HashMap<>())
                    .riskLevel(entity.getRiskLevel())
                    .assessmentId(entity.getAssessmentId())
                    .calculatedAt(entity.getCalculatedAt())
                    .notes(entity.getNotes())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting entity to risk score", e);
        }
    }
}
