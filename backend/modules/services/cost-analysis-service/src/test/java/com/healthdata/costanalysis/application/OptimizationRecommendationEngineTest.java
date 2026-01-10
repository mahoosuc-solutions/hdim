package com.healthdata.costanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.healthdata.costanalysis.domain.model.OptimizationRecommendation;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.ImplementationEffort;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.Priority;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.RecommendationStatus;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.RecommendationType;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.RiskLevel;
import com.healthdata.costanalysis.domain.repository.OptimizationRecommendationRepository;

@ExtendWith(MockitoExtension.class)
class OptimizationRecommendationEngineTest {

    private static final String TENANT_ID = "TENANT_001";
    private static final String SERVICE_NAME = "patient-service";

    @Mock
    private OptimizationRecommendationRepository recommendationRepository;

    private OptimizationRecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new OptimizationRecommendationEngine(recommendationRepository);
    }

    @Test
    void shouldGenerateRecommendationsForService() {
        // When
        List<OptimizationRecommendation> recommendations = engine.generateRecommendations(TENANT_ID, SERVICE_NAME);

        // Then
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).allMatch(r -> r.getTenantId().equals(TENANT_ID));
        assertThat(recommendations).allMatch(r -> r.getServiceName().equals(SERVICE_NAME));
    }

    @Test
    void shouldRetrievePendingRecommendations() {
        // Given
        List<OptimizationRecommendation> pendingRecs = List.of(
            buildRecommendation(RecommendationStatus.PENDING),
            buildRecommendation(RecommendationStatus.PENDING)
        );
        when(recommendationRepository.findByTenantAndStatus(TENANT_ID, RecommendationStatus.PENDING))
            .thenReturn(pendingRecs);

        // When
        List<OptimizationRecommendation> result = engine.getPendingRecommendations(TENANT_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getStatus() == RecommendationStatus.PENDING);
        verify(recommendationRepository).findByTenantAndStatus(TENANT_ID, RecommendationStatus.PENDING);
    }

    @Test
    void shouldFilterHighPriorityRecommendations() {
        // Given
        OptimizationRecommendation critical = buildRecommendation(RecommendationStatus.PENDING);
        critical.setPriority(Priority.CRITICAL);
        critical.setEstimatedSavings(new BigDecimal("100000.00"));

        List<OptimizationRecommendation> allRecs = List.of(critical);
        when(recommendationRepository.findByTenantAndStatus(TENANT_ID, RecommendationStatus.PENDING))
            .thenReturn(allRecs);

        // When
        List<OptimizationRecommendation> highPriority = engine.getHighPriorityRecommendations(TENANT_ID);

        // Then
        assertThat(highPriority).isNotEmpty();
        assertThat(highPriority).allMatch(r -> r.getPriority() == Priority.CRITICAL);
    }

    @Test
    void shouldAcceptRecommendationAndUpdateStatus() {
        // Given
        UUID recommendationId = UUID.randomUUID();
        OptimizationRecommendation rec = buildRecommendation(RecommendationStatus.PENDING);
        rec.setId(recommendationId);
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(rec));

        // When
        OptimizationRecommendation result = engine.acceptRecommendation(recommendationId);

        // Then
        assertThat(result.getStatus()).isEqualTo(RecommendationStatus.ACCEPTED);
        verify(recommendationRepository).save(any(OptimizationRecommendation.class));
    }

    @Test
    void shouldCompleteRecommendationWithActualSavings() {
        // Given
        UUID recommendationId = UUID.randomUUID();
        OptimizationRecommendation rec = buildRecommendation(RecommendationStatus.IN_PROGRESS);
        rec.setId(recommendationId);
        BigDecimal actualSavings = new BigDecimal("85000.00");

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(rec));

        // When
        OptimizationRecommendation result = engine.completeRecommendation(recommendationId, actualSavings);

        // Then
        assertThat(result.getStatus()).isEqualTo(RecommendationStatus.COMPLETED);
        assertThat(result.getActualSavings()).isEqualTo(actualSavings);
        assertThat(result.getImplementationDate()).isNotNull();
        verify(recommendationRepository).save(any(OptimizationRecommendation.class));
    }

    @Test
    void shouldRejectRecommendationWithReason() {
        // Given
        UUID recommendationId = UUID.randomUUID();
        OptimizationRecommendation rec = buildRecommendation(RecommendationStatus.PENDING);
        rec.setId(recommendationId);
        String reason = "Not aligned with current priorities";

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(rec));

        // When
        OptimizationRecommendation result = engine.rejectRecommendation(recommendationId, reason);

        // Then
        assertThat(result.getStatus()).isEqualTo(RecommendationStatus.REJECTED);
        assertThat(result.getNotes()).contains(reason);
        verify(recommendationRepository).save(any(OptimizationRecommendation.class));
    }

    @Test
    void shouldCalculateTotalPendingSavings() {
        // Given
        BigDecimal expectedTotal = new BigDecimal("250000.00");
        when(recommendationRepository.calculateTotalPendingSavings(TENANT_ID))
            .thenReturn(expectedTotal);

        // When
        BigDecimal result = engine.calculateTotalPendingSavings(TENANT_ID);

        // Then
        assertThat(result).isEqualTo(expectedTotal);
        verify(recommendationRepository).calculateTotalPendingSavings(TENANT_ID);
    }

    @Test
    void shouldCountImplementedRecommendations() {
        // Given
        when(recommendationRepository.countImplementedRecommendations(TENANT_ID)).thenReturn(15L);

        // When
        long result = engine.countImplementedRecommendations(TENANT_ID);

        // Then
        assertThat(result).isEqualTo(15L);
    }

    @Test
    void shouldCalculateROIForRecommendation() {
        // Given
        OptimizationRecommendation rec = buildRecommendation(RecommendationStatus.COMPLETED);
        rec.setEstimatedSavings(new BigDecimal("100000.00"));
        rec.setActualSavings(new BigDecimal("95000.00"));
        rec.setImplementationEffort(ImplementationEffort.MEDIUM);

        // When
        BigDecimal roi = engine.calculateROI(rec);

        // Then
        assertThat(roi).isGreaterThan(BigDecimal.ZERO);
        assertThat(roi).isLessThanOrEqualTo(new BigDecimal("95000.00"));
    }

    @Test
    void shouldEnforceTenantIsolation() {
        // Given
        String tenant2 = "TENANT_002";
        List<OptimizationRecommendation> tenant1Recs = List.of(buildRecommendation(RecommendationStatus.PENDING));
        List<OptimizationRecommendation> tenant2Recs = List.of(buildRecommendation(RecommendationStatus.PENDING));

        when(recommendationRepository.findByTenantAndStatus(TENANT_ID, RecommendationStatus.PENDING))
            .thenReturn(tenant1Recs);
        when(recommendationRepository.findByTenantAndStatus(tenant2, RecommendationStatus.PENDING))
            .thenReturn(tenant2Recs);

        // When
        List<OptimizationRecommendation> tenant1Result = engine.getPendingRecommendations(TENANT_ID);
        List<OptimizationRecommendation> tenant2Result = engine.getPendingRecommendations(tenant2);

        // Then
        assertThat(tenant1Result).allMatch(r -> r.getTenantId().equals(TENANT_ID));
        assertThat(tenant2Result).allMatch(r -> r.getTenantId().equals(tenant2));
        verify(recommendationRepository, times(2)).findByTenantAndStatus(any(), eq(RecommendationStatus.PENDING));
    }

    @Test
    void shouldGenerateRecommendationsWithDifferentTypes() {
        // When
        List<OptimizationRecommendation> recommendations = engine.generateRecommendations(TENANT_ID, SERVICE_NAME);

        // Then
        assertThat(recommendations).isNotEmpty();
        // Should include various recommendation types
        long typesCount = recommendations.stream()
            .map(OptimizationRecommendation::getRecommendationType)
            .distinct()
            .count();
        assertThat(typesCount).isGreaterThanOrEqualTo(1);
    }

    private OptimizationRecommendation buildRecommendation(RecommendationStatus status) {
        return OptimizationRecommendation.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .serviceName(SERVICE_NAME)
            .recommendationType(RecommendationType.RIGHT_SIZING)
            .title("Right-size infrastructure")
            .description("Current VM instances are over-sized for actual workload")
            .estimatedSavings(new BigDecimal("90000.00"))
            .savingsCurrency("USD")
            .savingsTimeframe("monthly")
            .confidenceScore(new BigDecimal("85.00"))
            .implementationEffort(ImplementationEffort.MEDIUM)
            .riskLevel(RiskLevel.LOW)
            .status(status)
            .priority(Priority.HIGH)
            .createdAt(Instant.now())
            .build();
    }
}
