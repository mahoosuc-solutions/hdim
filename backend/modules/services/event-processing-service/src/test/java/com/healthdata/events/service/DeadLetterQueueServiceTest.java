package com.healthdata.events.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.healthdata.events.entity.DeadLetterQueueEntity;
import com.healthdata.events.entity.DeadLetterQueueEntity.DLQStatus;
import com.healthdata.events.metrics.DLQMetrics;
import com.healthdata.events.repository.DeadLetterQueueRepository;
import com.healthdata.events.service.DeadLetterQueueService.DLQStats;

/**
 * TDD Test Suite for DeadLetterQueueService
 *
 * Tests the service layer for DLQ operations including failure recording,
 * retry management, and statistics retrieval.
 * Follows Test-Driven Development approach.
 */
@ExtendWith(MockitoExtension.class)
class DeadLetterQueueServiceTest {

    @Mock
    private DeadLetterQueueRepository dlqRepository;

    @Mock
    private DLQMetrics dlqMetrics;

    @InjectMocks
    private DeadLetterQueueService dlqService;

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
    @DisplayName("Record Failure Tests")
    class RecordFailureTests {

        @Test
        @DisplayName("Should create DLQ entry with all required fields")
        void shouldCreateDLQEntryWithAllFields() {
            // Given
            String topic = "patient.events";
            String eventType = "PATIENT_REGISTERED";
            String tenantId = "tenant-001";
            String patientId = "PATIENT-456";
            Object payload = new TestPayload("test data");
            Exception error = new RuntimeException("Database connection failed");

            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            DeadLetterQueueEntity result = dlqService.recordFailure(
                topic, eventType, tenantId, patientId, payload, error
            );

            // Then
            assertNotNull(result);
            assertEquals(topic, result.getTopic());
            assertEquals(eventType, result.getEventType());
            assertEquals(tenantId, result.getTenantId());
            assertEquals(patientId, result.getPatientId());
            assertNotNull(result.getEventPayload());
            assertEquals(error.getMessage(), result.getErrorMessage());
            assertNotNull(result.getStackTrace());
            assertEquals(0, result.getRetryCount());
            assertEquals(3, result.getMaxRetryCount());
            assertEquals(DLQStatus.FAILED, result.getStatus());
            assertNotNull(result.getFirstFailureAt());
            assertNotNull(result.getNextRetryAt());
        }

        @Test
        @DisplayName("Should serialize payload to JSON")
        void shouldSerializePayloadToJson() {
            // Given
            TestPayload payload = new TestPayload("test value");
            Exception error = new RuntimeException("Error");

            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            DeadLetterQueueEntity result = dlqService.recordFailure(
                "topic", "type", "tenant", "patient", payload, error
            );

            // Then
            assertNotNull(result.getEventPayload());
            assertTrue(result.getEventPayload().contains("test value"));
        }

        @Test
        @DisplayName("Should handle null payload gracefully")
        void shouldHandleNullPayload() {
            // Given
            Exception error = new RuntimeException("Error");

            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            DeadLetterQueueEntity result = dlqService.recordFailure(
                "topic", "type", "tenant", null, null, error
            );

            // Then
            assertNotNull(result);
            assertNotNull(result.getEventPayload());
        }

        @Test
        @DisplayName("Should extract stack trace from exception")
        void shouldExtractStackTrace() {
            // Given
            Exception error = new RuntimeException("Test error");

            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            DeadLetterQueueEntity result = dlqService.recordFailure(
                "topic", "type", "tenant", "patient", new TestPayload("data"), error
            );

            // Then
            assertNotNull(result.getStackTrace());
            assertTrue(result.getStackTrace().contains("RuntimeException"));
            assertTrue(result.getStackTrace().contains("Test error"));
        }

        @Test
        @DisplayName("Should call metrics to record failure")
        void shouldRecordMetrics() {
            // Given
            String topic = "patient.events";
            String eventType = "PATIENT_UPDATED";
            Exception error = new RuntimeException("Error");

            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.recordFailure(topic, eventType, "tenant", "patient", new TestPayload("data"), error);

            // Then
            verify(dlqMetrics, times(1)).recordFailure(topic, eventType);
        }

        @Test
        @DisplayName("Should save to repository")
        void shouldSaveToRepository() {
            // Given
            Exception error = new RuntimeException("Error");

            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.recordFailure("topic", "type", "tenant", "patient", new TestPayload("data"), error);

            // Then
            verify(dlqRepository, times(1)).save(any(DeadLetterQueueEntity.class));
        }

        @Test
        @DisplayName("Should set nextRetryAt using calculateNextRetry")
        void shouldSetNextRetryAt() {
            // Given
            Exception error = new RuntimeException("Error");

            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            DeadLetterQueueEntity result = dlqService.recordFailure(
                "topic", "type", "tenant", "patient", new TestPayload("data"), error
            );

            // Then
            assertNotNull(result.getNextRetryAt());
            assertTrue(result.getNextRetryAt().isAfter(Instant.now()));
        }
    }

    @Nested
    @DisplayName("Get Retry Eligible Tests")
    class GetRetryEligibleTests {

        @Test
        @DisplayName("Should retrieve retry eligible events from repository")
        void shouldRetrieveRetryEligibleEvents() {
            // Given
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(
                createTestEntity(0), createTestEntity(1), createTestEntity(2)
            );

            when(dlqRepository.findRetryEligible(any(Instant.class))).thenReturn(eligibleEvents);

            // When
            List<DeadLetterQueueEntity> result = dlqService.getRetryEligible();

            // Then
            assertEquals(3, result.size());
            verify(dlqRepository, times(1)).findRetryEligible(any(Instant.class));
        }

        @Test
        @DisplayName("Should pass current time to repository query")
        void shouldPassCurrentTimeToQuery() {
            // Given
            when(dlqRepository.findRetryEligible(any(Instant.class))).thenReturn(Arrays.asList());

            // When
            dlqService.getRetryEligible();

            // Then
            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(dlqRepository).findRetryEligible(instantCaptor.capture());

            Instant captured = instantCaptor.getValue();
            assertTrue(captured.isBefore(Instant.now().plusSeconds(1)));
            assertTrue(captured.isAfter(Instant.now().minusSeconds(1)));
        }

        @Test
        @DisplayName("Should return empty list when no events eligible")
        void shouldReturnEmptyListWhenNoneEligible() {
            // Given
            when(dlqRepository.findRetryEligible(any(Instant.class))).thenReturn(Arrays.asList());

            // When
            List<DeadLetterQueueEntity> result = dlqService.getRetryEligible();

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Mark For Retry Tests")
    class MarkForRetryTests {

        @Test
        @DisplayName("Should mark event for retry when eligible")
        void shouldMarkEventForRetry() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setStatus(DLQStatus.FAILED);
            testEntity.setRetryCount(0);
            testEntity.setMaxRetryCount(3);
            testEntity.setNextRetryAt(Instant.now().minusSeconds(60));

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.markForRetry(dlqId);

            // Then
            verify(dlqRepository, times(1)).save(testEntity);
            assertEquals(1, testEntity.getRetryCount());
            assertEquals(DLQStatus.RETRYING, testEntity.getStatus());
            assertNotNull(testEntity.getLastRetryAt());
        }

        @Test
        @DisplayName("Should throw exception when DLQ entry not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            UUID dlqId = UUID.randomUUID();
            when(dlqRepository.findById(dlqId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> dlqService.markForRetry(dlqId));
        }

        @Test
        @DisplayName("Should throw exception when event not eligible for retry")
        void shouldThrowExceptionWhenNotEligible() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setStatus(DLQStatus.EXHAUSTED);

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));

            // When & Then
            assertThrows(IllegalStateException.class, () -> dlqService.markForRetry(dlqId));
        }

        @Test
        @DisplayName("Should record retry metric")
        void shouldRecordRetryMetric() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setStatus(DLQStatus.FAILED);
            testEntity.setRetryCount(0);
            testEntity.setMaxRetryCount(3);
            testEntity.setNextRetryAt(Instant.now().minusSeconds(60));

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.markForRetry(dlqId);

            // Then
            verify(dlqMetrics, times(1)).recordRetry();
        }
    }

    @Nested
    @DisplayName("Mark As Resolved Tests")
    class MarkAsResolvedTests {

        @Test
        @DisplayName("Should mark event as resolved with user and notes")
        void shouldMarkAsResolved() {
            // Given
            UUID dlqId = testEntity.getId();
            String resolvedBy = "admin@test.com";
            String notes = "Manually processed the event";

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.markAsResolved(dlqId, resolvedBy, notes);

            // Then
            verify(dlqRepository, times(1)).save(testEntity);
            assertEquals(DLQStatus.RESOLVED, testEntity.getStatus());
            assertEquals(resolvedBy, testEntity.getResolvedBy());
            assertEquals(notes, testEntity.getResolutionNotes());
            assertNotNull(testEntity.getResolvedAt());
        }

        @Test
        @DisplayName("Should throw exception when DLQ entry not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            UUID dlqId = UUID.randomUUID();
            when(dlqRepository.findById(dlqId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(IllegalArgumentException.class,
                () -> dlqService.markAsResolved(dlqId, "user", "notes"));
        }

        @Test
        @DisplayName("Should handle null resolution notes")
        void shouldHandleNullNotes() {
            // Given
            UUID dlqId = testEntity.getId();

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.markAsResolved(dlqId, "user", null);

            // Then
            assertEquals(DLQStatus.RESOLVED, testEntity.getStatus());
            assertNull(testEntity.getResolutionNotes());
        }
    }

    @Nested
    @DisplayName("Mark As Exhausted Tests")
    class MarkAsExhaustedTests {

        @Test
        @DisplayName("Should mark event as exhausted")
        void shouldMarkAsExhausted() {
            // Given
            UUID dlqId = testEntity.getId();

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.markAsExhausted(dlqId);

            // Then
            verify(dlqRepository, times(1)).save(testEntity);
            assertEquals(DLQStatus.EXHAUSTED, testEntity.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when DLQ entry not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            UUID dlqId = UUID.randomUUID();
            when(dlqRepository.findById(dlqId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> dlqService.markAsExhausted(dlqId));
        }
    }

    @Nested
    @DisplayName("Record Retry Failure Tests")
    class RecordRetryFailureTests {

        @Test
        @DisplayName("Should update error message and stack trace on retry failure")
        void shouldUpdateErrorInformation() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setRetryCount(1);
            testEntity.setMaxRetryCount(3);
            Exception error = new RuntimeException("New error during retry");

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.recordRetryFailure(dlqId, error);

            // Then
            assertEquals("New error during retry", testEntity.getErrorMessage());
            assertNotNull(testEntity.getStackTrace());
            assertTrue(testEntity.getStackTrace().contains("New error during retry"));
        }

        @Test
        @DisplayName("Should set status back to FAILED after retry failure")
        void shouldSetStatusToFailed() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setStatus(DLQStatus.RETRYING);
            testEntity.setRetryCount(1);
            testEntity.setMaxRetryCount(3);
            Exception error = new RuntimeException("Error");

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.recordRetryFailure(dlqId, error);

            // Then
            assertEquals(DLQStatus.FAILED, testEntity.getStatus());
        }

        @Test
        @DisplayName("Should mark as exhausted when retry count reaches max")
        void shouldMarkAsExhaustedAtMaxRetries() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setRetryCount(3);
            testEntity.setMaxRetryCount(3);
            Exception error = new RuntimeException("Error");

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.recordRetryFailure(dlqId, error);

            // Then
            assertEquals(DLQStatus.EXHAUSTED, testEntity.getStatus());
        }

        @Test
        @DisplayName("Should calculate next retry time when not exhausted")
        void shouldCalculateNextRetryTime() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setRetryCount(1);
            testEntity.setMaxRetryCount(3);
            Exception error = new RuntimeException("Error");

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.recordRetryFailure(dlqId, error);

            // Then
            assertNotNull(testEntity.getNextRetryAt());
            assertTrue(testEntity.getNextRetryAt().isAfter(Instant.now()));
        }

        @Test
        @DisplayName("Should update lastRetryAt timestamp")
        void shouldUpdateLastRetryAt() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setRetryCount(1);
            testEntity.setMaxRetryCount(3);
            Exception error = new RuntimeException("Error");
            Instant before = Instant.now();

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class))).thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.recordRetryFailure(dlqId, error);

            // Then
            assertNotNull(testEntity.getLastRetryAt());
            assertTrue(testEntity.getLastRetryAt().isAfter(before.minusSeconds(1)));
        }

        @Test
        @DisplayName("Should throw exception when DLQ entry not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            UUID dlqId = UUID.randomUUID();
            when(dlqRepository.findById(dlqId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(IllegalArgumentException.class,
                () -> dlqService.recordRetryFailure(dlqId, new RuntimeException("Error")));
        }
    }

    @Nested
    @DisplayName("Get Failed By Tenant Tests")
    class GetFailedByTenantTests {

        @Test
        @DisplayName("Should retrieve failed events for tenant with pagination")
        void shouldRetrieveFailedByTenant() {
            // Given
            String tenantId = "tenant-001";
            Pageable pageable = PageRequest.of(0, 10);
            List<DeadLetterQueueEntity> entities = Arrays.asList(testEntity);
            Page<DeadLetterQueueEntity> page = new PageImpl<>(entities, pageable, 1);

            when(dlqRepository.findByTenantIdAndStatus(tenantId, DLQStatus.FAILED, pageable)).thenReturn(page);

            // When
            Page<DeadLetterQueueEntity> result = dlqService.getFailedByTenant(tenantId, pageable);

            // Then
            assertEquals(1, result.getTotalElements());
            assertEquals(testEntity, result.getContent().get(0));
        }
    }

    @Nested
    @DisplayName("Get Failed By Patient Tests")
    class GetFailedByPatientTests {

        @Test
        @DisplayName("Should retrieve failed events for patient")
        void shouldRetrieveFailedByPatient() {
            // Given
            String patientId = "PATIENT-123";
            List<DeadLetterQueueEntity> entities = Arrays.asList(testEntity);

            when(dlqRepository.findByPatientIdAndStatus(patientId, DLQStatus.FAILED)).thenReturn(entities);

            // When
            List<DeadLetterQueueEntity> result = dlqService.getFailedByPatient(patientId);

            // Then
            assertEquals(1, result.size());
            assertEquals(testEntity, result.get(0));
        }
    }

    @Nested
    @DisplayName("Get Failed By Topic Tests")
    class GetFailedByTopicTests {

        @Test
        @DisplayName("Should retrieve failed events for topic")
        void shouldRetrieveFailedByTopic() {
            // Given
            String topic = "patient.events";
            List<DeadLetterQueueEntity> entities = Arrays.asList(testEntity);

            when(dlqRepository.findByTopicAndStatus(topic, DLQStatus.FAILED)).thenReturn(entities);

            // When
            List<DeadLetterQueueEntity> result = dlqService.getFailedByTopic(topic);

            // Then
            assertEquals(1, result.size());
            assertEquals(testEntity, result.get(0));
        }
    }

    @Nested
    @DisplayName("Get Exhausted Tests")
    class GetExhaustedTests {

        @Test
        @DisplayName("Should retrieve all exhausted events")
        void shouldRetrieveExhausted() {
            // Given
            testEntity.setStatus(DLQStatus.EXHAUSTED);
            List<DeadLetterQueueEntity> entities = Arrays.asList(testEntity);

            when(dlqRepository.findExhausted()).thenReturn(entities);

            // When
            List<DeadLetterQueueEntity> result = dlqService.getExhausted();

            // Then
            assertEquals(1, result.size());
            assertEquals(DLQStatus.EXHAUSTED, result.get(0).getStatus());
        }
    }

    @Nested
    @DisplayName("Get Recent Failures Tests")
    class GetRecentFailuresTests {

        @Test
        @DisplayName("Should retrieve failures from last N hours")
        void shouldRetrieveRecentFailures() {
            // Given
            int hours = 24;
            List<DeadLetterQueueEntity> entities = Arrays.asList(testEntity);

            when(dlqRepository.findRecentFailures(any(Instant.class))).thenReturn(entities);

            // When
            List<DeadLetterQueueEntity> result = dlqService.getRecentFailures(hours);

            // Then
            assertEquals(1, result.size());

            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(dlqRepository).findRecentFailures(instantCaptor.capture());

            Instant since = instantCaptor.getValue();
            assertTrue(since.isBefore(Instant.now()));
            assertTrue(since.isAfter(Instant.now().minusSeconds(hours * 3600 + 2)));
        }
    }

    @Nested
    @DisplayName("Get Stats Tests")
    class GetStatsTests {

        @Test
        @DisplayName("Should retrieve DLQ statistics for tenant")
        void shouldRetrieveStats() {
            // Given
            String tenantId = "tenant-001";
            when(dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.FAILED)).thenReturn(5L);
            when(dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.EXHAUSTED)).thenReturn(2L);
            when(dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.RETRYING)).thenReturn(1L);

            // When
            DLQStats stats = dlqService.getStats(tenantId);

            // Then
            assertEquals(5L, stats.failed());
            assertEquals(2L, stats.exhausted());
            assertEquals(1L, stats.retrying());
        }

        @Test
        @DisplayName("Should return zero counts when no events exist")
        void shouldReturnZeroCountsWhenNoEvents() {
            // Given
            String tenantId = "tenant-empty";
            when(dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.FAILED)).thenReturn(0L);
            when(dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.EXHAUSTED)).thenReturn(0L);
            when(dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.RETRYING)).thenReturn(0L);

            // When
            DLQStats stats = dlqService.getStats(tenantId);

            // Then
            assertEquals(0L, stats.failed());
            assertEquals(0L, stats.exhausted());
            assertEquals(0L, stats.retrying());
        }
    }

    @Nested
    @DisplayName("Cleanup Old Resolved Tests")
    class CleanupOldResolvedTests {

        @Test
        @DisplayName("Should delete resolved entries older than specified days")
        void shouldDeleteOldResolvedEntries() {
            // Given
            int daysToKeep = 30;

            // When
            dlqService.cleanupOldResolved(daysToKeep);

            // Then
            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(dlqRepository).deleteOldResolved(instantCaptor.capture());

            Instant before = instantCaptor.getValue();
            Instant expectedBefore = Instant.now().minusSeconds(daysToKeep * 86400L);

            assertTrue(before.isBefore(Instant.now()));
            assertTrue(before.isAfter(expectedBefore.minusSeconds(2)));
        }

        @Test
        @DisplayName("Should handle cleanup with different retention periods")
        void shouldHandleDifferentRetentionPeriods() {
            // When
            dlqService.cleanupOldResolved(7);
            dlqService.cleanupOldResolved(90);

            // Then
            verify(dlqRepository, times(2)).deleteOldResolved(any(Instant.class));
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

    // Test payload class for serialization tests
    private static class TestPayload {
        private String value;

        public TestPayload(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
