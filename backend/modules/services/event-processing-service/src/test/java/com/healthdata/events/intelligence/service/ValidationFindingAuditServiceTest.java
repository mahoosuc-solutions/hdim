package com.healthdata.events.intelligence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.entity.EventEntity;
import com.healthdata.events.intelligence.audit.ValidationFindingAuditService;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import com.healthdata.events.repository.EventRepository;
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
@DisplayName("ValidationFindingAuditService Tests")
class ValidationFindingAuditServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ValidationFindingAuditService auditService;

    @Test
    @DisplayName("recordStateTransition should persist audit event")
    void recordStateTransitionShouldPersistAuditEvent() {
        UUID findingId = UUID.randomUUID();

        auditService.recordStateTransition(
                "tenant-a",
                findingId,
                FindingStatus.OPEN,
                FindingStatus.RESOLVED,
                "analyst-1",
                "resolved"
        );

        ArgumentCaptor<EventEntity> captor = ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).save(captor.capture());

        EventEntity saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        assertThat(saved.getEventType()).isEqualTo("VALIDATION_FINDING_STATE_CHANGED");
        assertThat(saved.getAggregateType()).isEqualTo("INTELLIGENCE_VALIDATION_FINDING");
        assertThat(saved.getAggregateId()).isEqualTo(findingId.toString());
        assertThat(saved.getUserId()).isEqualTo("analyst-1");
        assertThat(saved.getProcessed()).isTrue();
    }
}
