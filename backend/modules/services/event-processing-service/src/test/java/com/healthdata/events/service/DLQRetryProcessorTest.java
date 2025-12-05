package com.healthdata.events.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.healthdata.events.entity.DeadLetterQueueEntity;
import com.healthdata.events.entity.DeadLetterQueueEntity.DLQStatus;
import com.healthdata.events.model.DLQExhaustionAlert;

import java.util.concurrent.CompletableFuture;

/**
 * TDD Test Suite for DLQRetryProcessor
 *
 * Tests the retry processor including scheduled retry processing,
 * Kafka republishing, and exhaustion alerting.
 * Follows Test-Driven Development approach.
 */
@ExtendWith(MockitoExtension.class)
class DLQRetryProcessorTest {

    @Mock
    private DeadLetterQueueService dlqService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private DLQAlertingService alertingService;

    @InjectMocks
    private DLQRetryProcessor retryProcessor;

    private DeadLetterQueueEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = DeadLetterQueueEntity.builder()
            .id(UUID.randomUUID())
            .eventId(UUID.randomUUID())
            .tenantId("test-tenant")
            .topic("test.topic")
            .eventType("TEST_EVENT")
            .patientId("PATIENT-123")
            .eventPayload("{\"test\":\"data\"}")
            .errorMessage("Test error")
            .stackTrace("Stack trace")
            .retryCount(0)
            .maxRetryCount(3)
            .firstFailureAt(Instant.now())
            .status(DLQStatus.FAILED)
            .build();
    }

    @Nested
    @DisplayName("Process Retries Tests")
    class ProcessRetriesTests {

        @Test
        @DisplayName("Should process retry eligible events")
        void shouldProcessRetryEligibleEvents() {
            // Given
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(
                createTestEntity(0), createTestEntity(1)
            );

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            verify(dlqService, times(1)).getRetryEligible();
            verify(dlqService, times(2)).markForRetry(any(UUID.class));
            verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should not process when no events eligible")
        void shouldNotProcessWhenNoEventsEligible() {
            // Given
            when(dlqService.getRetryEligible()).thenReturn(Collections.emptyList());

            // When
            retryProcessor.processRetries();

            // Then
            verify(dlqService, times(1)).getRetryEligible();
            verify(dlqService, never()).markForRetry(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should mark event for retry before republishing")
        void shouldMarkForRetryBeforeRepublishing() {
            // Given
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            verify(dlqService, times(1)).markForRetry(testEntity.getId());
            verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should handle individual retry failures without stopping batch")
        void shouldHandleIndividualFailures() {
            // Given
            DeadLetterQueueEntity entity1 = createTestEntity(0);
            DeadLetterQueueEntity entity2 = createTestEntity(1);
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(entity1, entity2);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);

            // First entity fails during markForRetry
            doThrow(new RuntimeException("Mark failed")).when(dlqService).markForRetry(entity1.getId());

            // Second entity succeeds
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then - should still try to process second entity
            verify(dlqService, times(1)).recordRetryFailure(eq(entity1.getId()), any(RuntimeException.class));
            verify(dlqService, times(1)).markForRetry(entity2.getId());
        }
    }

    @Nested
    @DisplayName("Kafka Republishing Tests")
    class KafkaRepublishingTests {

        @Test
        @DisplayName("Should republish event to original topic")
        void shouldRepublishToOriginalTopic() {
            // Given
            testEntity.setTopic("patient.events");
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), anyString(), any());
            assertEquals("patient.events", topicCaptor.getValue());
        }

        @Test
        @DisplayName("Should use tenantId as Kafka key")
        void shouldUseTenantIdAsKey() {
            // Given
            testEntity.setTenantId("tenant-123");
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), any());
            assertEquals("tenant-123", keyCaptor.getValue());
        }

        @Test
        @DisplayName("Should deserialize and send original payload")
        void shouldDeserializeAndSendPayload() {
            // Given
            testEntity.setEventPayload("{\"patientId\":\"P123\",\"data\":\"test\"}");
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(kafkaTemplate).send(anyString(), anyString(), payloadCaptor.capture());
            assertNotNull(payloadCaptor.getValue());
        }

        @Test
        @DisplayName("Should mark as resolved on successful republish")
        void shouldMarkAsResolvedOnSuccess() {
            // Given
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            ArgumentCaptor<String> notesCaptor = ArgumentCaptor.forClass(String.class);
            verify(dlqService).markAsResolved(eq(testEntity.getId()), eq("DLQRetryProcessor"), notesCaptor.capture());
            assertTrue(notesCaptor.getValue().contains("Automatically retried successfully"));
        }

        @Test
        @DisplayName("Should record retry failure on Kafka send failure")
        void shouldRecordRetryFailureOnKafkaError() {
            // Given
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka connection failed"));

            // When
            retryProcessor.processRetries();

            // Then
            verify(dlqService).recordRetryFailure(eq(testEntity.getId()), any(RuntimeException.class));
        }

        @Test
        @DisplayName("Should handle JSON deserialization errors")
        void shouldHandleJsonDeserializationErrors() {
            // Given
            testEntity.setEventPayload("invalid json {{{");
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);

            // When
            retryProcessor.processRetries();

            // Then
            verify(dlqService).recordRetryFailure(eq(testEntity.getId()), any(Exception.class));
        }
    }

    @Nested
    @DisplayName("Exhaustion Handling Tests")
    class ExhaustionHandlingTests {

        @Test
        @DisplayName("Should mark as exhausted when max retries reached")
        void shouldMarkAsExhaustedAtMaxRetries() {
            // Given
            testEntity.setRetryCount(3);
            testEntity.setMaxRetryCount(3);
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Final retry failed"));

            // When
            retryProcessor.processRetries();

            // Then
            verify(dlqService).markAsExhausted(testEntity.getId());
        }

        @Test
        @DisplayName("Should send exhaustion alert when event exhausted")
        void shouldSendExhaustionAlert() {
            // Given
            testEntity.setRetryCount(3);
            testEntity.setMaxRetryCount(3);
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Final retry failed"));

            // When
            retryProcessor.processRetries();

            // Then
            ArgumentCaptor<DLQExhaustionAlert> alertCaptor = ArgumentCaptor.forClass(DLQExhaustionAlert.class);
            verify(alertingService).sendExhaustionAlert(alertCaptor.capture());

            DLQExhaustionAlert alert = alertCaptor.getValue();
            assertEquals(testEntity.getEventId(), alert.getEventId());
            assertEquals(testEntity.getEventType(), alert.getEventType());
            assertEquals(testEntity.getTenantId(), alert.getTenantId());
        }

        @Test
        @DisplayName("Should create dashboard entry for exhausted event")
        void shouldCreateDashboardEntry() {
            // Given
            testEntity.setRetryCount(3);
            testEntity.setMaxRetryCount(3);
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Final retry failed"));
            when(alertingService.createDashboardEntry(any())).thenReturn("dashboard-entry-123");

            // When
            retryProcessor.processRetries();

            // Then
            verify(alertingService).createDashboardEntry(any(DLQExhaustionAlert.class));
        }

        @Test
        @DisplayName("Should escalate critical failures")
        void shouldEscalateCriticalFailures() {
            // Given
            testEntity.setRetryCount(3);
            testEntity.setMaxRetryCount(3);
            testEntity.setEventType("PATIENT_REGISTERED"); // Critical event type
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Final retry failed"));
            when(alertingService.createDashboardEntry(any())).thenReturn("dashboard-entry-123");

            // When
            retryProcessor.processRetries();

            // Then
            ArgumentCaptor<DLQExhaustionAlert> alertCaptor = ArgumentCaptor.forClass(DLQExhaustionAlert.class);
            verify(alertingService).escalateCriticalFailure(alertCaptor.capture());

            DLQExhaustionAlert alert = alertCaptor.getValue();
            assertTrue(alert.isCritical());
        }

        @Test
        @DisplayName("Should not escalate non-critical failures")
        void shouldNotEscalateNonCriticalFailures() {
            // Given
            testEntity.setRetryCount(3);
            testEntity.setMaxRetryCount(3);
            testEntity.setEventType("BACKGROUND_SYNC"); // Non-critical event type
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Final retry failed"));
            when(alertingService.createDashboardEntry(any())).thenReturn("dashboard-entry-123");

            // When
            retryProcessor.processRetries();

            // Then
            verify(alertingService, never()).escalateCriticalFailure(any());
        }

        @Test
        @DisplayName("Should handle alerting failures gracefully")
        void shouldHandleAlertingFailuresGracefully() {
            // Given
            testEntity.setRetryCount(3);
            testEntity.setMaxRetryCount(3);
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Final retry failed"));
            doThrow(new RuntimeException("Alerting service down"))
                .when(alertingService).sendExhaustionAlert(any());

            // When & Then - should not throw exception
            assertDoesNotThrow(() -> retryProcessor.processRetries());
        }

        @Test
        @DisplayName("Should include patient ID in exhaustion alert")
        void shouldIncludePatientIdInAlert() {
            // Given
            testEntity.setRetryCount(3);
            testEntity.setMaxRetryCount(3);
            testEntity.setPatientId("PATIENT-789");
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(testEntity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Final retry failed"));

            // When
            retryProcessor.processRetries();

            // Then
            ArgumentCaptor<DLQExhaustionAlert> alertCaptor = ArgumentCaptor.forClass(DLQExhaustionAlert.class);
            verify(alertingService).sendExhaustionAlert(alertCaptor.capture());
            assertEquals("PATIENT-789", alertCaptor.getValue().getAffectedPatientId());
        }
    }

    @Nested
    @DisplayName("Alert On Exhausted Tests")
    class AlertOnExhaustedTests {

        @Test
        @DisplayName("Should retrieve exhausted events")
        void shouldRetrieveExhaustedEvents() {
            // Given
            testEntity.setStatus(DLQStatus.EXHAUSTED);
            List<DeadLetterQueueEntity> exhaustedEvents = Arrays.asList(testEntity);

            when(dlqService.getExhausted()).thenReturn(exhaustedEvents);

            // When
            retryProcessor.alertOnExhausted();

            // Then
            verify(dlqService, times(1)).getExhausted();
        }

        @Test
        @DisplayName("Should send alerts for all exhausted events")
        void shouldSendAlertsForAllExhausted() {
            // Given
            DeadLetterQueueEntity entity1 = createExhaustedEntity();
            DeadLetterQueueEntity entity2 = createExhaustedEntity();
            List<DeadLetterQueueEntity> exhaustedEvents = Arrays.asList(entity1, entity2);

            when(dlqService.getExhausted()).thenReturn(exhaustedEvents);

            // When
            retryProcessor.alertOnExhausted();

            // Then
            verify(alertingService, times(2)).sendExhaustionAlert(any(DLQExhaustionAlert.class));
        }

        @Test
        @DisplayName("Should not alert when no exhausted events")
        void shouldNotAlertWhenNoExhausted() {
            // Given
            when(dlqService.getExhausted()).thenReturn(Collections.emptyList());

            // When
            retryProcessor.alertOnExhausted();

            // Then
            verify(alertingService, never()).sendExhaustionAlert(any());
            verify(alertingService, never()).createDashboardEntry(any());
        }

        @Test
        @DisplayName("Should create dashboard entries for exhausted events")
        void shouldCreateDashboardEntries() {
            // Given
            testEntity.setStatus(DLQStatus.EXHAUSTED);
            List<DeadLetterQueueEntity> exhaustedEvents = Arrays.asList(testEntity);

            when(dlqService.getExhausted()).thenReturn(exhaustedEvents);
            when(alertingService.createDashboardEntry(any())).thenReturn("entry-123");

            // When
            retryProcessor.alertOnExhausted();

            // Then
            verify(alertingService, times(1)).createDashboardEntry(any(DLQExhaustionAlert.class));
        }

        @Test
        @DisplayName("Should handle multiple exhausted events from same topic")
        void shouldHandleMultipleEventsFromSameTopic() {
            // Given
            DeadLetterQueueEntity entity1 = createExhaustedEntity();
            entity1.setTopic("patient.events");
            DeadLetterQueueEntity entity2 = createExhaustedEntity();
            entity2.setTopic("patient.events");
            DeadLetterQueueEntity entity3 = createExhaustedEntity();
            entity3.setTopic("order.events");

            List<DeadLetterQueueEntity> exhaustedEvents = Arrays.asList(entity1, entity2, entity3);

            when(dlqService.getExhausted()).thenReturn(exhaustedEvents);

            // When
            retryProcessor.alertOnExhausted();

            // Then
            verify(alertingService, times(3)).sendExhaustionAlert(any(DLQExhaustionAlert.class));
        }
    }

    @Nested
    @DisplayName("Cleanup Old Entries Tests")
    class CleanupOldEntriesTests {

        @Test
        @DisplayName("Should call cleanup with 30 day retention")
        void shouldCallCleanupWith30Days() {
            // When
            retryProcessor.cleanupOldEntries();

            // Then
            verify(dlqService, times(1)).cleanupOldResolved(30);
        }

        @Test
        @DisplayName("Should handle cleanup errors gracefully")
        void shouldHandleCleanupErrorsGracefully() {
            // Given
            doThrow(new RuntimeException("Database error")).when(dlqService).cleanupOldResolved(anyInt());

            // When & Then - should not throw exception
            assertThrows(RuntimeException.class, () -> retryProcessor.cleanupOldEntries());
        }
    }

    @Nested
    @DisplayName("Exponential Backoff Verification Tests")
    class ExponentialBackoffTests {

        @Test
        @DisplayName("Should respect nextRetryAt calculated by entity")
        void shouldRespectNextRetryAt() {
            // Given
            Instant futureRetry = Instant.now().plusSeconds(300); // 5 minutes
            testEntity.setNextRetryAt(futureRetry);
            testEntity.setRetryCount(1);

            // This entity should not be in the eligible list if backoff is working
            when(dlqService.getRetryEligible()).thenReturn(Collections.emptyList());

            // When
            retryProcessor.processRetries();

            // Then
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should process events with past nextRetryAt")
        void shouldProcessEventsWithPastRetryTime() {
            // Given
            Instant pastRetry = Instant.now().minusSeconds(300);
            testEntity.setNextRetryAt(pastRetry);
            testEntity.setRetryCount(1);

            when(dlqService.getRetryEligible()).thenReturn(Arrays.asList(testEntity));
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Batch Processing Tests")
    class BatchProcessingTests {

        @Test
        @DisplayName("Should process multiple events in single batch")
        void shouldProcessMultipleEventsInBatch() {
            // Given
            List<DeadLetterQueueEntity> batch = Arrays.asList(
                createTestEntity(0),
                createTestEntity(1),
                createTestEntity(2)
            );

            when(dlqService.getRetryEligible()).thenReturn(batch);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            verify(kafkaTemplate, times(3)).send(anyString(), anyString(), any());
            verify(dlqService, times(3)).markAsResolved(any(UUID.class), anyString(), anyString());
        }

        @Test
        @DisplayName("Should continue processing batch even if one fails")
        void shouldContinueBatchOnPartialFailure() {
            // Given
            DeadLetterQueueEntity entity1 = createTestEntity(0);
            DeadLetterQueueEntity entity2 = createTestEntity(1);
            DeadLetterQueueEntity entity3 = createTestEntity(2);
            List<DeadLetterQueueEntity> batch = Arrays.asList(entity1, entity2, entity3);

            when(dlqService.getRetryEligible()).thenReturn(batch);

            // Entity2 will fail
            when(kafkaTemplate.send(eq("test.topic"), eq("test-tenant"), any()))
                .thenReturn(mockSuccessfulSend())
                .thenThrow(new RuntimeException("Send failed"))
                .thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then - should process all 3 entities
            verify(kafkaTemplate, times(3)).send(anyString(), anyString(), any());
            verify(dlqService, times(2)).markAsResolved(any(UUID.class), anyString(), anyString());
            verify(dlqService, times(1)).recordRetryFailure(any(UUID.class), any(RuntimeException.class));
        }
    }

    // Helper methods

    private DeadLetterQueueEntity createTestEntity(int retryCount) {
        return DeadLetterQueueEntity.builder()
            .id(UUID.randomUUID())
            .eventId(UUID.randomUUID())
            .tenantId("test-tenant")
            .topic("test.topic")
            .eventType("TEST_EVENT")
            .eventPayload("{\"test\":\"data\"}")
            .errorMessage("Error")
            .retryCount(retryCount)
            .maxRetryCount(3)
            .firstFailureAt(Instant.now())
            .status(DLQStatus.FAILED)
            .build();
    }

    private DeadLetterQueueEntity createExhaustedEntity() {
        DeadLetterQueueEntity entity = createTestEntity(3);
        entity.setStatus(DLQStatus.EXHAUSTED);
        return entity;
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, Object>> mockSuccessfulSend() {
        return CompletableFuture.completedFuture(mock(SendResult.class));
    }
}
