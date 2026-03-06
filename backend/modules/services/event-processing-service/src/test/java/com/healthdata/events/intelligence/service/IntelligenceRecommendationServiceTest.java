package com.healthdata.events.intelligence.service;

import com.healthdata.events.intelligence.dto.ReviewRecommendationRequest;
import com.healthdata.events.intelligence.dto.ValidationFindingResponse;
import com.healthdata.events.intelligence.audit.RecommendationAuditService;
import com.healthdata.events.intelligence.controller.TenantScopedResourceNotFoundException;
import com.healthdata.events.intelligence.entity.IntelligenceRecommendationEntity;
import com.healthdata.events.intelligence.repository.IntelligenceRecommendationRepository;
import com.healthdata.eventsourcing.intelligence.CanonicalEventEnvelope;
import com.healthdata.eventsourcing.intelligence.RecommendationReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntelligenceRecommendationService Tests")
class IntelligenceRecommendationServiceTest {

    @Mock
    private IntelligenceRecommendationRepository recommendationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private IntelligenceValidationService validationService;

    @Mock
    private RecommendationAuditService recommendationAuditService;

    @InjectMocks
    private IntelligenceRecommendationService service;

    private CanonicalEventEnvelope envelope;

    @BeforeEach
    void setUp() {
        envelope = CanonicalEventEnvelope.builder()
                .eventId("evt-1")
                .tenantId("tenant-a")
                .patientRef("patient-1")
                .sourceType("CLAIMS")
                .resourceType("Observation")
                .schemaVersion("1.0")
                .traceId("trace-1")
                .occurredAt(Instant.now())
                .payload(Map.of("deniedClaim", true, "careGap", true, "value", 9.2))
                .provenance(Map.of("source", "claims-feed"))
                .confidence(0.55)
                .riskTier("HIGH")
                .build();
    }

    @Test
    @DisplayName("ingest should generate and persist recommendations")
    void ingestShouldGenerateAndPersistRecommendations() {
        when(validationService.runValidation(any())).thenReturn(List.of(
                new ValidationFindingResponse(
                        UUID.randomUUID(),
                        "evt-1",
                        "RULE",
                        "title",
                        "desc",
                        null,
                        null,
                        null,
                        "{}",
                        Instant.now()
                )
        ));
        when(recommendationRepository.save(any(IntelligenceRecommendationEntity.class)))
                .thenAnswer(invocation -> {
                    IntelligenceRecommendationEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        entity.setId(UUID.randomUUID());
                    }
                    if (entity.getCreatedAt() == null) {
                        entity.setCreatedAt(Instant.now());
                    }
                    return entity;
                });

        var recommendations = service.ingestAndGenerate(envelope);

        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations)
                .extracting(r -> r.status())
                .containsOnly(RecommendationReviewStatus.PROPOSED);

        verify(recommendationRepository, times(recommendations.size())).save(any(IntelligenceRecommendationEntity.class));
        verify(validationService, times(1)).runValidation(any());
        verify(kafkaTemplate, times(recommendations.size() * 2 + 1)).send(any(String.class), eq("tenant-a"), any());
    }

    @Test
    @DisplayName("review should update recommendation state")
    void reviewShouldUpdateState() {
        UUID recommendationId = UUID.randomUUID();
        IntelligenceRecommendationEntity entity = IntelligenceRecommendationEntity.builder()
                .id(recommendationId)
                .tenantId("tenant-a")
                .patientRef("patient-1")
                .sourceEventId("evt-1")
                .signalType("CARE_GAP_OPEN")
                .title("title")
                .description("desc")
                .riskTier("HIGH")
                .confidence(0.8)
                .evidence("{}")
                .status(RecommendationReviewStatus.PROPOSED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(recommendationRepository.findByIdAndTenantId(recommendationId, "tenant-a")).thenReturn(Optional.of(entity));
        when(recommendationRepository.save(any(IntelligenceRecommendationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.reviewRecommendation(
                "tenant-a",
                recommendationId,
                new ReviewRecommendationRequest(RecommendationReviewStatus.TRIAGED, "clinician-1", "Needs triage")
        );

        assertThat(updated.status()).isEqualTo(RecommendationReviewStatus.TRIAGED);
        assertThat(updated.reviewedBy()).isEqualTo("clinician-1");
        assertThat(updated.reviewedAt()).isNotNull();

        ArgumentCaptor<IntelligenceRecommendationEntity> captor = ArgumentCaptor.forClass(IntelligenceRecommendationEntity.class);
        verify(recommendationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RecommendationReviewStatus.TRIAGED);
        verify(recommendationAuditService, times(1)).recordStateTransition(
                eq("tenant-a"),
                eq(recommendationId),
                eq(RecommendationReviewStatus.PROPOSED),
                eq(RecommendationReviewStatus.TRIAGED),
                eq("clinician-1"),
                eq("Needs triage")
        );
    }

    @Test
    @DisplayName("review should hide cross-tenant access as not found")
    void reviewShouldHideCrossTenantAccess() {
        UUID recommendationId = UUID.randomUUID();
        IntelligenceRecommendationEntity entity = IntelligenceRecommendationEntity.builder()
                .id(recommendationId)
                .tenantId("tenant-b")
                .patientRef("patient-1")
                .sourceEventId("evt-1")
                .signalType("CARE_GAP_OPEN")
                .title("title")
                .description("desc")
                .riskTier("HIGH")
                .confidence(0.8)
                .evidence("{}")
                .status(RecommendationReviewStatus.PROPOSED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(recommendationRepository.findByIdAndTenantId(recommendationId, "tenant-a")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reviewRecommendation(
                "tenant-a",
                recommendationId,
                new ReviewRecommendationRequest(RecommendationReviewStatus.REJECTED, "reviewer", "Wrong tenant")
        )).isInstanceOf(TenantScopedResourceNotFoundException.class)
                .hasMessageContaining("Recommendation not found");

        verify(recommendationRepository, never()).save(any());
    }

    @Test
    @DisplayName("review should reject invalid state transition")
    void reviewShouldRejectInvalidStateTransition() {
        UUID recommendationId = UUID.randomUUID();
        IntelligenceRecommendationEntity entity = IntelligenceRecommendationEntity.builder()
                .id(recommendationId)
                .tenantId("tenant-a")
                .patientRef("patient-1")
                .sourceEventId("evt-1")
                .signalType("CARE_GAP_OPEN")
                .title("title")
                .description("desc")
                .riskTier("HIGH")
                .confidence(0.8)
                .evidence("{}")
                .status(RecommendationReviewStatus.PROPOSED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(recommendationRepository.findByIdAndTenantId(recommendationId, "tenant-a")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.reviewRecommendation(
                "tenant-a",
                recommendationId,
                new ReviewRecommendationRequest(RecommendationReviewStatus.IMPLEMENTED, "reviewer", "skip")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid recommendation state transition");

        verify(recommendationRepository, never()).save(any());
        verify(recommendationAuditService, never()).recordStateTransition(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getSignals should map persisted recommendations")
    void getSignalsShouldMapPersistedRecommendations() {
        UUID id = UUID.randomUUID();
        IntelligenceRecommendationEntity entity = IntelligenceRecommendationEntity.builder()
                .id(id)
                .tenantId("tenant-a")
                .patientRef("patient-1")
                .sourceEventId("evt-1")
                .signalType("NOVEL_PATTERN")
                .title("Novel pattern")
                .description("desc")
                .riskTier("MEDIUM")
                .confidence(0.65)
                .evidence("{}")
                .status(RecommendationReviewStatus.PROPOSED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(recommendationRepository.findByTenantIdAndPatientRefOrderByCreatedAtDesc("tenant-a", "patient-1"))
                .thenReturn(List.of(entity));

        var signals = service.getSignals("tenant-a", "patient-1");

        assertThat(signals).hasSize(1);
        assertThat(signals.getFirst().recommendationId()).isEqualTo(id);
        assertThat(signals.getFirst().signalType()).isEqualTo("NOVEL_PATTERN");
    }
}
