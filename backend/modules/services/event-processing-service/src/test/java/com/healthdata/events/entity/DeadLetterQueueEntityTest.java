package com.healthdata.events.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.healthdata.events.entity.DeadLetterQueueEntity.DLQStatus;

/**
 * TDD Test Suite for DeadLetterQueueEntity
 *
 * Tests entity business logic including retry calculations, eligibility checks,
 * and status transitions. Follows Test-Driven Development approach.
 */
class DeadLetterQueueEntityTest {

    private DeadLetterQueueEntity dlqEntity;

    @BeforeEach
    void setUp() {
        dlqEntity = DeadLetterQueueEntity.builder()
            .id(UUID.randomUUID())
            .eventId(UUID.randomUUID())
            .tenantId("test-tenant")
            .topic("test.topic")
            .eventType("TEST_EVENT")
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
    @DisplayName("Exponential Backoff Calculation Tests")
    class ExponentialBackoffTests {

        @Test
        @DisplayName("Should calculate 1 minute backoff for first retry (retry count 0)")
        void shouldCalculateOneMinuteForFirstRetry() {
            // Given
            dlqEntity.setRetryCount(0);
            Instant before = Instant.now();

            // When
            Instant nextRetry = dlqEntity.calculateNextRetry();

            // Then
            Instant after = Instant.now().plusSeconds(60);
            assertTrue(nextRetry.isAfter(before.plusSeconds(59)));
            assertTrue(nextRetry.isBefore(after.plusSeconds(2)));
        }

        @Test
        @DisplayName("Should calculate 5 minutes backoff for second retry (retry count 1)")
        void shouldCalculateFiveMinutesForSecondRetry() {
            // Given
            dlqEntity.setRetryCount(1);
            Instant before = Instant.now();

            // When
            Instant nextRetry = dlqEntity.calculateNextRetry();

            // Then
            Instant after = Instant.now().plusSeconds(5 * 60);
            assertTrue(nextRetry.isAfter(before.plusSeconds(299)));
            assertTrue(nextRetry.isBefore(after.plusSeconds(2)));
        }

        @Test
        @DisplayName("Should calculate 30 minutes backoff for third retry (retry count 2)")
        void shouldCalculateThirtyMinutesForThirdRetry() {
            // Given
            dlqEntity.setRetryCount(2);
            Instant before = Instant.now();

            // When
            Instant nextRetry = dlqEntity.calculateNextRetry();

            // Then
            Instant after = Instant.now().plusSeconds(30 * 60);
            assertTrue(nextRetry.isAfter(before.plusSeconds(1799)));
            assertTrue(nextRetry.isBefore(after.plusSeconds(2)));
        }

        @Test
        @DisplayName("Should calculate 2 hours backoff for fourth retry (retry count 3)")
        void shouldCalculateTwoHoursForFourthRetry() {
            // Given
            dlqEntity.setRetryCount(3);
            Instant before = Instant.now();

            // When
            Instant nextRetry = dlqEntity.calculateNextRetry();

            // Then
            Instant after = Instant.now().plusSeconds(120 * 60);
            assertTrue(nextRetry.isAfter(before.plusSeconds(7199)));
            assertTrue(nextRetry.isBefore(after.plusSeconds(2)));
        }

        @Test
        @DisplayName("Should calculate 12 hours backoff for fifth retry (retry count 4)")
        void shouldCalculateTwelveHoursForFifthRetry() {
            // Given
            dlqEntity.setRetryCount(4);
            Instant before = Instant.now();

            // When
            Instant nextRetry = dlqEntity.calculateNextRetry();

            // Then
            Instant after = Instant.now().plusSeconds(720 * 60);
            assertTrue(nextRetry.isAfter(before.plusSeconds(43199)));
            assertTrue(nextRetry.isBefore(after.plusSeconds(2)));
        }

        @Test
        @DisplayName("Should cap backoff at 12 hours for retries beyond configured array")
        void shouldCapBackoffAtMaximum() {
            // Given
            dlqEntity.setRetryCount(10);
            Instant before = Instant.now();

            // When
            Instant nextRetry = dlqEntity.calculateNextRetry();

            // Then - should still use 12 hours (720 minutes)
            Instant after = Instant.now().plusSeconds(720 * 60);
            assertTrue(nextRetry.isAfter(before.plusSeconds(43199)));
            assertTrue(nextRetry.isBefore(after.plusSeconds(2)));
        }
    }

    @Nested
    @DisplayName("Retry Eligibility Tests")
    class RetryEligibilityTests {

        @Test
        @DisplayName("Should be eligible when status is FAILED and retry count below max")
        void shouldBeEligibleWhenFailedAndBelowMax() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);
            dlqEntity.setRetryCount(2);
            dlqEntity.setMaxRetryCount(3);
            dlqEntity.setNextRetryAt(Instant.now().minusSeconds(60));

            // When
            boolean eligible = dlqEntity.isRetryEligible();

            // Then
            assertTrue(eligible);
        }

        @Test
        @DisplayName("Should be eligible when nextRetryAt is null")
        void shouldBeEligibleWhenNextRetryAtIsNull() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);
            dlqEntity.setRetryCount(0);
            dlqEntity.setMaxRetryCount(3);
            dlqEntity.setNextRetryAt(null);

            // When
            boolean eligible = dlqEntity.isRetryEligible();

            // Then
            assertTrue(eligible);
        }

        @Test
        @DisplayName("Should be eligible when nextRetryAt is in the past")
        void shouldBeEligibleWhenNextRetryAtIsPast() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);
            dlqEntity.setRetryCount(1);
            dlqEntity.setMaxRetryCount(3);
            dlqEntity.setNextRetryAt(Instant.now().minusSeconds(3600));

            // When
            boolean eligible = dlqEntity.isRetryEligible();

            // Then
            assertTrue(eligible);
        }

        @Test
        @DisplayName("Should NOT be eligible when status is EXHAUSTED")
        void shouldNotBeEligibleWhenExhausted() {
            // Given
            dlqEntity.setStatus(DLQStatus.EXHAUSTED);
            dlqEntity.setRetryCount(3);
            dlqEntity.setMaxRetryCount(3);

            // When
            boolean eligible = dlqEntity.isRetryEligible();

            // Then
            assertFalse(eligible);
        }

        @Test
        @DisplayName("Should NOT be eligible when status is RESOLVED")
        void shouldNotBeEligibleWhenResolved() {
            // Given
            dlqEntity.setStatus(DLQStatus.RESOLVED);
            dlqEntity.setRetryCount(1);
            dlqEntity.setMaxRetryCount(3);

            // When
            boolean eligible = dlqEntity.isRetryEligible();

            // Then
            assertFalse(eligible);
        }

        @Test
        @DisplayName("Should NOT be eligible when status is RETRYING")
        void shouldNotBeEligibleWhenRetrying() {
            // Given
            dlqEntity.setStatus(DLQStatus.RETRYING);
            dlqEntity.setRetryCount(1);
            dlqEntity.setMaxRetryCount(3);

            // When
            boolean eligible = dlqEntity.isRetryEligible();

            // Then
            assertFalse(eligible);
        }

        @Test
        @DisplayName("Should NOT be eligible when retry count equals max retry count")
        void shouldNotBeEligibleWhenAtMaxRetries() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);
            dlqEntity.setRetryCount(3);
            dlqEntity.setMaxRetryCount(3);

            // When
            boolean eligible = dlqEntity.isRetryEligible();

            // Then
            assertFalse(eligible);
        }

        @Test
        @DisplayName("Should NOT be eligible when retry count exceeds max retry count")
        void shouldNotBeEligibleWhenExceedsMaxRetries() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);
            dlqEntity.setRetryCount(5);
            dlqEntity.setMaxRetryCount(3);

            // When
            boolean eligible = dlqEntity.isRetryEligible();

            // Then
            assertFalse(eligible);
        }

        @Test
        @DisplayName("Should NOT be eligible when nextRetryAt is in the future")
        void shouldNotBeEligibleWhenNextRetryAtIsFuture() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);
            dlqEntity.setRetryCount(1);
            dlqEntity.setMaxRetryCount(3);
            dlqEntity.setNextRetryAt(Instant.now().plusSeconds(3600));

            // When
            boolean eligible = dlqEntity.isRetryEligible();

            // Then
            assertFalse(eligible);
        }
    }

    @Nested
    @DisplayName("Mark For Retry Tests")
    class MarkForRetryTests {

        @Test
        @DisplayName("Should increment retry count when marked for retry")
        void shouldIncrementRetryCount() {
            // Given
            dlqEntity.setRetryCount(0);

            // When
            dlqEntity.markForRetry();

            // Then
            assertEquals(1, dlqEntity.getRetryCount());
        }

        @Test
        @DisplayName("Should set status to RETRYING when marked for retry")
        void shouldSetStatusToRetrying() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);

            // When
            dlqEntity.markForRetry();

            // Then
            assertEquals(DLQStatus.RETRYING, dlqEntity.getStatus());
        }

        @Test
        @DisplayName("Should set lastRetryAt to current time when marked for retry")
        void shouldSetLastRetryAt() {
            // Given
            Instant before = Instant.now();

            // When
            dlqEntity.markForRetry();

            // Then
            Instant after = Instant.now();
            assertNotNull(dlqEntity.getLastRetryAt());
            assertTrue(dlqEntity.getLastRetryAt().isAfter(before.minusSeconds(1)));
            assertTrue(dlqEntity.getLastRetryAt().isBefore(after.plusSeconds(1)));
        }

        @Test
        @DisplayName("Should calculate and set nextRetryAt when marked for retry")
        void shouldCalculateNextRetryAt() {
            // Given
            dlqEntity.setRetryCount(0);

            // When
            dlqEntity.markForRetry();

            // Then
            assertNotNull(dlqEntity.getNextRetryAt());
            assertTrue(dlqEntity.getNextRetryAt().isAfter(Instant.now()));
        }

        @Test
        @DisplayName("Should preserve other fields when marked for retry")
        void shouldPreserveOtherFields() {
            // Given
            UUID eventId = dlqEntity.getEventId();
            String tenantId = dlqEntity.getTenantId();
            String errorMessage = dlqEntity.getErrorMessage();

            // When
            dlqEntity.markForRetry();

            // Then
            assertEquals(eventId, dlqEntity.getEventId());
            assertEquals(tenantId, dlqEntity.getTenantId());
            assertEquals(errorMessage, dlqEntity.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("Mark As Exhausted Tests")
    class MarkAsExhaustedTests {

        @Test
        @DisplayName("Should set status to EXHAUSTED")
        void shouldSetStatusToExhausted() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);

            // When
            dlqEntity.markAsExhausted();

            // Then
            assertEquals(DLQStatus.EXHAUSTED, dlqEntity.getStatus());
        }

        @Test
        @DisplayName("Should preserve retry count when marked as exhausted")
        void shouldPreserveRetryCount() {
            // Given
            dlqEntity.setRetryCount(5);

            // When
            dlqEntity.markAsExhausted();

            // Then
            assertEquals(5, dlqEntity.getRetryCount());
        }

        @Test
        @DisplayName("Should preserve error information when marked as exhausted")
        void shouldPreserveErrorInformation() {
            // Given
            String errorMessage = "Original error message";
            String stackTrace = "Stack trace details";
            dlqEntity.setErrorMessage(errorMessage);
            dlqEntity.setStackTrace(stackTrace);

            // When
            dlqEntity.markAsExhausted();

            // Then
            assertEquals(errorMessage, dlqEntity.getErrorMessage());
            assertEquals(stackTrace, dlqEntity.getStackTrace());
        }
    }

    @Nested
    @DisplayName("Mark As Resolved Tests")
    class MarkAsResolvedTests {

        @Test
        @DisplayName("Should set status to RESOLVED")
        void shouldSetStatusToResolved() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);

            // When
            dlqEntity.markAsResolved("admin@test.com", "Fixed manually");

            // Then
            assertEquals(DLQStatus.RESOLVED, dlqEntity.getStatus());
        }

        @Test
        @DisplayName("Should set resolvedAt to current time")
        void shouldSetResolvedAt() {
            // Given
            Instant before = Instant.now();

            // When
            dlqEntity.markAsResolved("admin@test.com", "Fixed manually");

            // Then
            Instant after = Instant.now();
            assertNotNull(dlqEntity.getResolvedAt());
            assertTrue(dlqEntity.getResolvedAt().isAfter(before.minusSeconds(1)));
            assertTrue(dlqEntity.getResolvedAt().isBefore(after.plusSeconds(1)));
        }

        @Test
        @DisplayName("Should set resolvedBy field")
        void shouldSetResolvedBy() {
            // Given
            String resolvedBy = "admin@test.com";

            // When
            dlqEntity.markAsResolved(resolvedBy, "Fixed manually");

            // Then
            assertEquals(resolvedBy, dlqEntity.getResolvedBy());
        }

        @Test
        @DisplayName("Should set resolution notes")
        void shouldSetResolutionNotes() {
            // Given
            String notes = "Fixed by manually processing the event";

            // When
            dlqEntity.markAsResolved("admin@test.com", notes);

            // Then
            assertEquals(notes, dlqEntity.getResolutionNotes());
        }

        @Test
        @DisplayName("Should handle null resolution notes")
        void shouldHandleNullNotes() {
            // When
            dlqEntity.markAsResolved("admin@test.com", null);

            // Then
            assertEquals(DLQStatus.RESOLVED, dlqEntity.getStatus());
            assertNull(dlqEntity.getResolutionNotes());
        }

        @Test
        @DisplayName("Should preserve original error information when resolved")
        void shouldPreserveOriginalError() {
            // Given
            String errorMessage = "Original error";
            dlqEntity.setErrorMessage(errorMessage);

            // When
            dlqEntity.markAsResolved("admin@test.com", "Fixed");

            // Then
            assertEquals(errorMessage, dlqEntity.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should transition from FAILED to RETRYING to RESOLVED")
        void shouldTransitionThroughNormalFlow() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);

            // When - mark for retry
            dlqEntity.markForRetry();
            assertEquals(DLQStatus.RETRYING, dlqEntity.getStatus());

            // When - mark as resolved
            dlqEntity.markAsResolved("system", "Successfully retried");

            // Then
            assertEquals(DLQStatus.RESOLVED, dlqEntity.getStatus());
        }

        @Test
        @DisplayName("Should transition from FAILED to EXHAUSTED when max retries reached")
        void shouldTransitionToExhaustedAtMaxRetries() {
            // Given
            dlqEntity.setStatus(DLQStatus.FAILED);
            dlqEntity.setRetryCount(3);
            dlqEntity.setMaxRetryCount(3);

            // When
            dlqEntity.markAsExhausted();

            // Then
            assertEquals(DLQStatus.EXHAUSTED, dlqEntity.getStatus());
        }

        @Test
        @DisplayName("Should allow manual resolution from EXHAUSTED status")
        void shouldAllowManualResolutionFromExhausted() {
            // Given
            dlqEntity.setStatus(DLQStatus.EXHAUSTED);

            // When
            dlqEntity.markAsResolved("admin@test.com", "Manually fixed");

            // Then
            assertEquals(DLQStatus.RESOLVED, dlqEntity.getStatus());
        }
    }

    @Nested
    @DisplayName("Builder and Default Values Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should set default retry count to 0")
        void shouldSetDefaultRetryCount() {
            // Given & When
            DeadLetterQueueEntity entity = DeadLetterQueueEntity.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .tenantId("tenant")
                .topic("topic")
                .eventType("type")
                .errorMessage("error")
                .firstFailureAt(Instant.now())
                .build();

            // Then
            assertEquals(0, entity.getRetryCount());
        }

        @Test
        @DisplayName("Should set default max retry count to 3")
        void shouldSetDefaultMaxRetryCount() {
            // Given & When
            DeadLetterQueueEntity entity = DeadLetterQueueEntity.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .tenantId("tenant")
                .topic("topic")
                .eventType("type")
                .errorMessage("error")
                .firstFailureAt(Instant.now())
                .build();

            // Then
            assertEquals(3, entity.getMaxRetryCount());
        }

        @Test
        @DisplayName("Should set default status to FAILED")
        void shouldSetDefaultStatus() {
            // Given & When
            DeadLetterQueueEntity entity = DeadLetterQueueEntity.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .tenantId("tenant")
                .topic("topic")
                .eventType("type")
                .errorMessage("error")
                .firstFailureAt(Instant.now())
                .build();

            // Then
            assertEquals(DLQStatus.FAILED, entity.getStatus());
        }

        @Test
        @DisplayName("Should allow custom max retry count")
        void shouldAllowCustomMaxRetryCount() {
            // Given & When
            DeadLetterQueueEntity entity = DeadLetterQueueEntity.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .tenantId("tenant")
                .topic("topic")
                .eventType("type")
                .errorMessage("error")
                .firstFailureAt(Instant.now())
                .maxRetryCount(5)
                .build();

            // Then
            assertEquals(5, entity.getMaxRetryCount());
        }
    }
}
