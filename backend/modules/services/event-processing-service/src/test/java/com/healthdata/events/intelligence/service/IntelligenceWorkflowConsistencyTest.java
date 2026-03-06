package com.healthdata.events.intelligence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.entity.EventEntity;
import com.healthdata.events.intelligence.audit.RecommendationAuditService;
import com.healthdata.events.intelligence.audit.ValidationFindingAuditService;
import com.healthdata.events.intelligence.dto.ReviewRecommendationRequest;
import com.healthdata.events.intelligence.dto.UpdateValidationFindingStatusRequest;
import com.healthdata.events.intelligence.entity.IntelligenceRecommendationEntity;
import com.healthdata.events.intelligence.entity.IntelligenceTenantTrustProjectionEntity;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import com.healthdata.events.intelligence.projection.TenantTrustProjectionService;
import com.healthdata.events.intelligence.repository.IntelligenceRecommendationRepository;
import com.healthdata.events.intelligence.repository.IntelligenceTenantTrustProjectionRepository;
import com.healthdata.events.intelligence.repository.IntelligenceValidationFindingRepository;
import com.healthdata.events.repository.EventRepository;
import com.healthdata.eventsourcing.intelligence.CanonicalEventEnvelope;
import com.healthdata.eventsourcing.intelligence.RecommendationReviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Intelligence Workflow Consistency Tests")
class IntelligenceWorkflowConsistencyTest {

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("ingest, review, resolve findings, and dashboard trust should stay consistent")
    void ingestReviewResolveAndDashboardTrustShouldStayConsistent() {
        IntelligenceValidationFindingRepository findingRepository = mock(IntelligenceValidationFindingRepository.class);
        IntelligenceRecommendationRepository recommendationRepository = mock(IntelligenceRecommendationRepository.class);
        IntelligenceTenantTrustProjectionRepository projectionRepository = mock(IntelligenceTenantTrustProjectionRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        Map<UUID, IntelligenceValidationFindingEntity> findingStore = new HashMap<>();
        Map<UUID, IntelligenceRecommendationEntity> recommendationStore = new HashMap<>();
        Map<String, IntelligenceTenantTrustProjectionEntity> projectionStore = new HashMap<>();
        List<EventEntity> eventStore = new ArrayList<>();

        when(findingRepository.save(any(IntelligenceValidationFindingEntity.class))).thenAnswer(invocation -> {
            IntelligenceValidationFindingEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(Instant.now());
            }
            findingStore.put(entity.getId(), entity);
            return entity;
        });

        when(findingRepository.findById(any(UUID.class))).thenAnswer(invocation ->
                Optional.ofNullable(findingStore.get(invocation.getArgument(0)))
        );
        when(findingRepository.findByIdAndTenantId(any(UUID.class), anyString())).thenAnswer(invocation ->
                Optional.ofNullable(findingStore.get(invocation.getArgument(0)))
                        .filter(f -> invocation.getArgument(1).equals(f.getTenantId()))
        );

        when(findingRepository.findByTenantIdAndPatientRefOrderByCreatedAtDesc(anyString(), anyString())).thenAnswer(invocation ->
                findingStore.values().stream()
                        .filter(f -> invocation.getArgument(0).equals(f.getTenantId()))
                        .filter(f -> invocation.getArgument(1).equals(f.getPatientRef()))
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .toList()
        );

        when(findingRepository.findByTenantIdAndPatientRefAndStatusOrderByCreatedAtDesc(anyString(), anyString(), any(FindingStatus.class))).thenAnswer(invocation ->
                findingStore.values().stream()
                        .filter(f -> invocation.getArgument(0).equals(f.getTenantId()))
                        .filter(f -> invocation.getArgument(1).equals(f.getPatientRef()))
                        .filter(f -> invocation.getArgument(2).equals(f.getStatus()))
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .toList()
        );

        when(findingRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(anyString(), any(FindingStatus.class))).thenAnswer(invocation ->
                findingStore.values().stream()
                        .filter(f -> invocation.getArgument(0).equals(f.getTenantId()))
                        .filter(f -> invocation.getArgument(1).equals(f.getStatus()))
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .toList()
        );

        when(recommendationRepository.save(any(IntelligenceRecommendationEntity.class))).thenAnswer(invocation -> {
            IntelligenceRecommendationEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(Instant.now());
            }
            recommendationStore.put(entity.getId(), entity);
            return entity;
        });

        when(recommendationRepository.findById(any(UUID.class))).thenAnswer(invocation ->
                Optional.ofNullable(recommendationStore.get(invocation.getArgument(0)))
        );
        when(recommendationRepository.findByIdAndTenantId(any(UUID.class), anyString())).thenAnswer(invocation ->
                Optional.ofNullable(recommendationStore.get(invocation.getArgument(0)))
                        .filter(r -> invocation.getArgument(1).equals(r.getTenantId()))
        );

        when(recommendationRepository.findByTenantIdAndPatientRefOrderByCreatedAtDesc(anyString(), anyString())).thenAnswer(invocation ->
                recommendationStore.values().stream()
                        .filter(r -> invocation.getArgument(0).equals(r.getTenantId()))
                        .filter(r -> invocation.getArgument(1).equals(r.getPatientRef()))
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .toList()
        );

        when(projectionRepository.save(any(IntelligenceTenantTrustProjectionEntity.class))).thenAnswer(invocation -> {
            IntelligenceTenantTrustProjectionEntity entity = invocation.getArgument(0);
            projectionStore.put(entity.getTenantId(), entity);
            return entity;
        });

        when(projectionRepository.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(projectionStore.get(invocation.getArgument(0)))
        );

        when(eventRepository.save(any(EventEntity.class))).thenAnswer(invocation -> {
            EventEntity entity = invocation.getArgument(0);
            eventStore.add(entity);
            return entity;
        });

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        TenantTrustProjectionService tenantTrustProjectionService =
                new TenantTrustProjectionService(findingRepository, projectionRepository);
        ValidationFindingAuditService validationFindingAuditService =
                new ValidationFindingAuditService(eventRepository, objectMapper);
        IntelligenceValidationService validationService =
                new IntelligenceValidationService(
                        findingRepository,
                        tenantTrustProjectionService,
                        validationFindingAuditService,
                        kafkaTemplate,
                        objectMapper
                );
        RecommendationAuditService recommendationAuditService =
                new RecommendationAuditService(eventRepository, objectMapper);
        IntelligenceRecommendationService recommendationService =
                new IntelligenceRecommendationService(
                        recommendationRepository,
                        validationService,
                        recommendationAuditService,
                        kafkaTemplate,
                        objectMapper
                );

        CanonicalEventEnvelope envelope = CanonicalEventEnvelope.builder()
                .eventId("evt-1")
                .tenantId("tenant-a")
                .patientRef("patient-1")
                .sourceType("FHIR_R4")
                .resourceType("Observation")
                .schemaVersion("1.0")
                .occurredAt(Instant.now().minusSeconds(10_000))
                .payload(Map.of(
                        "conflictingIdentifiers", true,
                        "careGap", true
                ))
                .provenance(Map.of("source", "fhir"))
                .confidence(0.7)
                .riskTier("HIGH")
                .build();

        var generatedRecommendations = recommendationService.ingestAndGenerate(envelope);
        assertThat(generatedRecommendations).isNotEmpty();

        var initialDashboard = tenantTrustProjectionService.getTenantDashboard("tenant-a");
        assertThat(initialDashboard.trustScore()).isLessThan(100);
        assertThat(initialDashboard.totalOpenFindings()).isGreaterThan(0);

        UUID recommendationId = generatedRecommendations.getFirst().id();
        recommendationService.reviewRecommendation(
                "tenant-a",
                recommendationId,
                new ReviewRecommendationRequest(RecommendationReviewStatus.TRIAGED, "reviewer-1", "triaged")
        );

        // Resolve all open findings and validate trust score recovery
        List<IntelligenceValidationFindingEntity> openFindings = findingStore.values().stream()
                .filter(f -> "tenant-a".equals(f.getTenantId()))
                .filter(f -> f.getStatus() == FindingStatus.OPEN)
                .toList();

        for (IntelligenceValidationFindingEntity finding : openFindings) {
            validationService.updateFindingStatus(
                    "tenant-a",
                    finding.getId(),
                    new UpdateValidationFindingStatusRequest(FindingStatus.RESOLVED, "analyst-1", "resolved")
            );
        }

        var finalDashboard = tenantTrustProjectionService.getTenantDashboard("tenant-a");
        assertThat(finalDashboard.trustScore()).isEqualTo(100);
        assertThat(finalDashboard.totalOpenFindings()).isEqualTo(0);

        assertThat(eventStore)
                .extracting(EventEntity::getEventType)
                .contains("RECOMMENDATION_STATE_CHANGED", "VALIDATION_FINDING_STATE_CHANGED");
    }
}
