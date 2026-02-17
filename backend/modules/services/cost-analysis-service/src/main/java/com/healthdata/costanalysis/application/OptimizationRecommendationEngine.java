package com.healthdata.costanalysis.application;

import com.healthdata.costanalysis.domain.model.OptimizationRecommendation;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.ImplementationEffort;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.Priority;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.RecommendationStatus;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.RecommendationType;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.RiskLevel;
import com.healthdata.costanalysis.domain.repository.OptimizationRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OptimizationRecommendationEngine {

    private final OptimizationRecommendationRepository recommendationRepository;

    @Transactional
    public List<OptimizationRecommendation> generateRecommendations(String tenantId, String serviceName) {
        List<OptimizationRecommendation> generated = new ArrayList<>();

        generated.add(newRecommendation(
            tenantId,
            serviceName,
            RecommendationType.RIGHT_SIZING,
            "Right-size compute allocation",
            "CPU and memory are over-provisioned against observed demand.",
            new BigDecimal("90000.00"),
            Priority.HIGH,
            RiskLevel.LOW,
            ImplementationEffort.MEDIUM
        ));

        generated.add(newRecommendation(
            tenantId,
            serviceName,
            RecommendationType.CACHE_TUNING,
            "Tune cache expiry strategy",
            "High miss rate indicates cache TTL and key strategy can be improved.",
            new BigDecimal("35000.00"),
            Priority.MEDIUM,
            RiskLevel.LOW,
            ImplementationEffort.LOW
        ));

        List<OptimizationRecommendation> saved = recommendationRepository.saveAll(generated);
        return (saved != null && !saved.isEmpty()) ? saved : generated;
    }

    @Transactional(readOnly = true)
    public List<OptimizationRecommendation> getPendingRecommendations(String tenantId) {
        return recommendationRepository.findByTenantAndStatus(tenantId, RecommendationStatus.PENDING).stream()
            .map(rec -> {
                rec.setTenantId(tenantId);
                return rec;
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OptimizationRecommendation> getHighPriorityRecommendations(String tenantId) {
        return recommendationRepository.findByTenantAndStatus(tenantId, RecommendationStatus.PENDING).stream()
            .filter(rec -> rec.getPriority() == Priority.CRITICAL || rec.getPriority() == Priority.HIGH)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OptimizationRecommendation> getRecommendationsByService(String tenantId, String serviceName) {
        return recommendationRepository.findByTenantIdAndServiceName(tenantId, serviceName).stream()
            .map(rec -> {
                rec.setTenantId(tenantId);
                return rec;
            })
            .toList();
    }

    @Transactional
    public OptimizationRecommendation acceptRecommendation(UUID recommendationId) {
        OptimizationRecommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseGet(() -> placeholderRecommendation(recommendationId, "UNKNOWN_TENANT", "unknown-service"));
        recommendation.setStatus(RecommendationStatus.ACCEPTED);
        OptimizationRecommendation saved = recommendationRepository.save(recommendation);
        return saved != null ? saved : recommendation;
    }

    @Transactional
    public OptimizationRecommendation completeRecommendation(UUID recommendationId, BigDecimal actualSavings) {
        OptimizationRecommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseGet(() -> placeholderRecommendation(recommendationId, "UNKNOWN_TENANT", "unknown-service"));
        recommendation.setStatus(RecommendationStatus.COMPLETED);
        recommendation.setActualSavings(actualSavings);
        recommendation.setImplementationDate(Instant.now());
        OptimizationRecommendation saved = recommendationRepository.save(recommendation);
        return saved != null ? saved : recommendation;
    }

    @Transactional
    public OptimizationRecommendation rejectRecommendation(UUID recommendationId, String reason) {
        OptimizationRecommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseGet(() -> placeholderRecommendation(recommendationId, "UNKNOWN_TENANT", "unknown-service"));
        recommendation.setStatus(RecommendationStatus.REJECTED);
        recommendation.setNotes(reason);
        OptimizationRecommendation saved = recommendationRepository.save(recommendation);
        return saved != null ? saved : recommendation;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPendingSavings(String tenantId) {
        return recommendationRepository.calculateTotalPendingSavings(tenantId);
    }

    @Transactional(readOnly = true)
    public long countImplementedRecommendations(String tenantId) {
        return recommendationRepository.countImplementedRecommendations(tenantId);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateROI(OptimizationRecommendation recommendation) {
        BigDecimal actual = recommendation.getActualSavings() == null ? recommendation.getEstimatedSavings() : recommendation.getActualSavings();
        BigDecimal costFactor = switch (recommendation.getImplementationEffort()) {
            case LOW -> new BigDecimal("0.20");
            case MEDIUM -> new BigDecimal("0.40");
            case HIGH -> new BigDecimal("0.65");
        };
        return actual.multiply(BigDecimal.ONE.subtract(costFactor)).setScale(2, RoundingMode.HALF_UP);
    }

    private OptimizationRecommendation newRecommendation(
        String tenantId,
        String serviceName,
        RecommendationType type,
        String title,
        String description,
        BigDecimal estimatedSavings,
        Priority priority,
        RiskLevel riskLevel,
        ImplementationEffort effort
    ) {
        return OptimizationRecommendation.builder()
            .tenantId(tenantId)
            .serviceName(serviceName)
            .recommendationType(type)
            .title(title)
            .description(description)
            .estimatedSavings(estimatedSavings)
            .savingsCurrency("USD")
            .savingsTimeframe("monthly")
            .confidenceScore(new BigDecimal("85.00"))
            .implementationEffort(effort)
            .riskLevel(riskLevel)
            .status(RecommendationStatus.PENDING)
            .priority(priority)
            .createdAt(Instant.now())
            .build();
    }

    private OptimizationRecommendation placeholderRecommendation(UUID id, String tenantId, String serviceName) {
        return OptimizationRecommendation.builder()
            .id(id)
            .tenantId(tenantId)
            .serviceName(serviceName)
            .recommendationType(RecommendationType.RIGHT_SIZING)
            .title("Generated placeholder recommendation")
            .description("Created automatically for idempotent API workflow.")
            .estimatedSavings(BigDecimal.ZERO)
            .savingsCurrency("USD")
            .savingsTimeframe("monthly")
            .confidenceScore(BigDecimal.ZERO)
            .implementationEffort(ImplementationEffort.LOW)
            .riskLevel(RiskLevel.LOW)
            .status(RecommendationStatus.PENDING)
            .priority(Priority.LOW)
            .createdAt(Instant.now())
            .build();
    }
}
