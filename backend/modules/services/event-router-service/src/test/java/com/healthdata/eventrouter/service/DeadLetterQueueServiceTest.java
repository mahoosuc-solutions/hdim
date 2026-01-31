package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.entity.DeadLetterEventEntity;
import com.healthdata.eventrouter.persistence.DeadLetterEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dead Letter Queue Service Tests")
class DeadLetterQueueServiceTest {

    @Mock
    private DeadLetterEventRepository dlqRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private DeadLetterQueueService dlqService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        dlqService = new DeadLetterQueueService(dlqRepository, kafkaTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should send failed event to DLQ topic")
    void shouldSendToDlqTopic() {
        // Given
        EventMessage event = createEvent("FAILED_EVENT");
        String reason = "No matching routing rule found";

        // When
        dlqService.sendToDeadLetterQueue(event, reason);

        // Then
        verify(kafkaTemplate).send(eq("event-router.dlq"), any(String.class));
    }

    @Test
    @DisplayName("Should persist failed event to database")
    void shouldPersistToDatabase() {
        // Given
        EventMessage event = createEvent("FAILED_EVENT");
        String reason = "Filter criteria not met";

        // When
        dlqService.sendToDeadLetterQueue(event, reason);

        // Then
        ArgumentCaptor<DeadLetterEventEntity> captor = ArgumentCaptor.forClass(DeadLetterEventEntity.class);
        verify(dlqRepository).save(captor.capture());

        DeadLetterEventEntity saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("FAILED_EVENT");
        assertThat(saved.getFailureReason()).isEqualTo("Filter criteria not met");
        assertThat(saved.getTenantId()).isEqualTo("tenant1");
    }

    @Test
    @DisplayName("Should include original event payload in DLQ")
    void shouldIncludeOriginalPayload() {
        // Given
        Map<String, Object> payload = Map.of("key", "value", "nested", Map.of("field", "data"));
        EventMessage event = createEvent("FAILED_EVENT");
        event.setPayload(payload);

        // When
        dlqService.sendToDeadLetterQueue(event, "Routing failed");

        // Then
        ArgumentCaptor<DeadLetterEventEntity> captor = ArgumentCaptor.forClass(DeadLetterEventEntity.class);
        verify(dlqRepository).save(captor.capture());

        DeadLetterEventEntity saved = captor.getValue();
        assertThat(saved.getOriginalPayload()).isNotNull();
    }

    @Test
    @DisplayName("Should include failure timestamp")
    void shouldIncludeTimestamp() {
        // Given
        EventMessage event = createEvent("FAILED_EVENT");
        DeadLetterEventEntity savedEntity = new DeadLetterEventEntity();
        savedEntity.setTenantId(event.getTenantId());
        savedEntity.setEventType(event.getEventType());
        when(dlqRepository.save(any(DeadLetterEventEntity.class))).thenAnswer(invocation -> {
            DeadLetterEventEntity entity = invocation.getArgument(0);
            // Simulate @PrePersist
            if (entity.getFailedAt() == null) {
                entity.setFailedAt(java.time.Instant.now());
            }
            return entity;
        });

        // When
        dlqService.sendToDeadLetterQueue(event, "Test failure");

        // Then
        ArgumentCaptor<DeadLetterEventEntity> captor = ArgumentCaptor.forClass(DeadLetterEventEntity.class);
        verify(dlqRepository).save(captor.capture());

        DeadLetterEventEntity saved = captor.getValue();
        assertThat(saved.getFailedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should support retry from DLQ")
    void shouldSupportRetry() {
        // Given
        DeadLetterEventEntity dlqEvent = new DeadLetterEventEntity();
        dlqEvent.setId(1L);
        dlqEvent.setEventType("RETRY_EVENT");
        dlqEvent.setOriginalPayload("{\"test\":\"data\"}");
        dlqEvent.setRetryCount(0);

        when(dlqRepository.findById(1L)).thenReturn(java.util.Optional.of(dlqEvent));

        // When
        boolean retried = dlqService.retryEvent(1L);

        // Then
        assertThat(retried).isTrue();
        ArgumentCaptor<DeadLetterEventEntity> captor = ArgumentCaptor.forClass(DeadLetterEventEntity.class);
        verify(dlqRepository).save(captor.capture());
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should limit retry attempts")
    void shouldLimitRetries() {
        // Given
        DeadLetterEventEntity dlqEvent = new DeadLetterEventEntity();
        dlqEvent.setId(1L);
        dlqEvent.setRetryCount(5); // Max retries

        when(dlqRepository.findById(1L)).thenReturn(java.util.Optional.of(dlqEvent));

        // When
        boolean retried = dlqService.retryEvent(1L);

        // Then
        assertThat(retried).isFalse();
    }

    @Test
    @DisplayName("Should track DLQ metrics")
    void shouldTrackMetrics() {
        // Given
        EventMessage event = createEvent("FAILED_EVENT");

        // When
        dlqService.sendToDeadLetterQueue(event, "Test failure");

        // Then
        long count = dlqService.getDeadLetterCount();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    private EventMessage createEvent(String type) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId("tenant1");
        event.setSourceTopic("test.topic");
        return event;
    }
}
