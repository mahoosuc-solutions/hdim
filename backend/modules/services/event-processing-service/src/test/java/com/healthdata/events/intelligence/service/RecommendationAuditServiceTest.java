package com.healthdata.events.intelligence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.entity.EventEntity;
import com.healthdata.events.intelligence.audit.RecommendationAuditService;
import com.healthdata.events.repository.EventRepository;
import com.healthdata.eventsourcing.intelligence.RecommendationReviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationAuditService Tests")
class RecommendationAuditServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RecommendationAuditService auditService;

    @Test
    @DisplayName("recordStateTransition should persist audit event")
    void recordStateTransitionShouldPersistAuditEvent() {
        UUID recommendationId = UUID.randomUUID();

        auditService.recordStateTransition(
                "tenant-a",
                recommendationId,
                RecommendationReviewStatus.PROPOSED,
                RecommendationReviewStatus.TRIAGED,
                "reviewer-1",
                "triage note"
        );

        ArgumentCaptor<EventEntity> captor = ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).save(captor.capture());

        EventEntity saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        assertThat(saved.getEventType()).isEqualTo("RECOMMENDATION_STATE_CHANGED");
        assertThat(saved.getAggregateType()).isEqualTo("INTELLIGENCE_RECOMMENDATION");
        assertThat(saved.getAggregateId()).isEqualTo(recommendationId.toString());
        assertThat(saved.getUserId()).isEqualTo("reviewer-1");
        assertThat(saved.getProcessed()).isTrue();
    }
}
