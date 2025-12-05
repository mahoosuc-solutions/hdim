package com.healthdata.predictive.service;

import com.healthdata.predictive.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopulationRiskStratifier {

    private final ReadmissionRiskPredictor readmissionPredictor;

    public List<RiskCohort> stratifyPopulation(String tenantId, List<String> patientIds, Map<String, Map<String, Object>> patientDataMap) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (patientIds == null) {
            throw new IllegalArgumentException("Patient IDs cannot be null");
        }

        if (patientIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<RiskTier, List<ReadmissionRiskScore>> tierScores = new HashMap<>();
        for (RiskTier tier : RiskTier.values()) {
            tierScores.put(tier, new ArrayList<>());
        }

        for (String patientId : patientIds) {
            Map<String, Object> patientData = patientDataMap.getOrDefault(patientId, new HashMap<>());
            ReadmissionRiskScore score = readmissionPredictor.predict30DayRisk(tenantId, patientId, patientData);
            tierScores.get(score.getRiskTier()).add(score);
        }

        List<RiskCohort> cohorts = new ArrayList<>();
        for (Map.Entry<RiskTier, List<ReadmissionRiskScore>> entry : tierScores.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                cohorts.add(createCohort(tenantId, entry.getKey(), entry.getValue()));
            }
        }

        return cohorts;
    }

    public List<String> getHighRiskPatients(String tenantId, List<String> patientIds, Map<String, Map<String, Object>> patientDataMap) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (patientIds == null) {
            throw new IllegalArgumentException("Patient IDs cannot be null");
        }

        return patientIds.stream()
            .filter(patientId -> {
                Map<String, Object> patientData = patientDataMap.getOrDefault(patientId, new HashMap<>());
                ReadmissionRiskScore score = readmissionPredictor.predict30DayRisk(tenantId, patientId, patientData);
                return score.getRiskTier() == RiskTier.HIGH || score.getRiskTier() == RiskTier.VERY_HIGH;
            })
            .collect(Collectors.toList());
    }

    private RiskCohort createCohort(String tenantId, RiskTier tier, List<ReadmissionRiskScore> scores) {
        List<String> patientIds = scores.stream()
            .map(ReadmissionRiskScore::getPatientId)
            .collect(Collectors.toList());

        double avgScore = scores.stream()
            .mapToDouble(ReadmissionRiskScore::getScore)
            .average()
            .orElse(0.0);

        return RiskCohort.builder()
            .cohortId(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .riskTier(tier)
            .patientIds(patientIds)
            .patientCount(patientIds.size())
            .averageRiskScore(avgScore)
            .commonRiskFactors(new HashMap<>())
            .characteristics(new HashMap<>())
            .generatedAt(LocalDateTime.now())
            .metadata(new HashMap<>())
            .build();
    }
}
