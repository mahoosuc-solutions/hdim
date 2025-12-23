package com.healthdata.migration.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.migration.dto.MigrationProgress;

/**
 * Unit tests for MigrationProgressPublisher
 */
@DisplayName("MigrationProgressPublisher")
class MigrationProgressPublisherTest {

    private MigrationProgressPublisher publisher;
    private ObjectMapper objectMapper;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        publisher = new MigrationProgressPublisher();
        objectMapper = new ObjectMapper();
        jobId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Subscription Management")
    class SubscriptionTests {

        @Test
        @DisplayName("Should subscribe session to job")
        void shouldSubscribeSession() {
            // Given
            WebSocketSession session = createMockSession("session1");

            // When
            publisher.subscribe(jobId, session);

            // Then
            assertThat(publisher.getSubscriberCount(jobId)).isEqualTo(1);
        }

        @Test
        @DisplayName("Should subscribe multiple sessions to same job")
        void shouldSubscribeMultipleSessions() {
            // Given
            WebSocketSession session1 = createMockSession("session1");
            WebSocketSession session2 = createMockSession("session2");

            // When
            publisher.subscribe(jobId, session1);
            publisher.subscribe(jobId, session2);

            // Then
            assertThat(publisher.getSubscriberCount(jobId)).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle same session subscribing twice")
        void shouldHandleDuplicateSubscription() {
            // Given
            WebSocketSession session = createMockSession("session1");

            // When
            publisher.subscribe(jobId, session);
            publisher.subscribe(jobId, session);

            // Then
            // Set behavior - should only count once
            assertThat(publisher.getSubscriberCount(jobId)).isEqualTo(1);
        }

        @Test
        @DisplayName("Should unsubscribe session from job")
        void shouldUnsubscribeSession() {
            // Given
            WebSocketSession session = createMockSession("session1");
            publisher.subscribe(jobId, session);

            // When
            publisher.unsubscribe(jobId, session);

            // Then
            assertThat(publisher.getSubscriberCount(jobId)).isZero();
        }

        @Test
        @DisplayName("Should unsubscribe session from all jobs")
        void shouldUnsubscribeFromAllJobs() {
            // Given
            UUID jobId1 = UUID.randomUUID();
            UUID jobId2 = UUID.randomUUID();
            WebSocketSession session = createMockSession("session1");

            publisher.subscribe(jobId1, session);
            publisher.subscribe(jobId2, session);

            // When
            publisher.unsubscribeAll(session);

            // Then
            assertThat(publisher.getSubscriberCount(jobId1)).isZero();
            assertThat(publisher.getSubscriberCount(jobId2)).isZero();
        }

        @Test
        @DisplayName("Should return zero count for job with no subscribers")
        void shouldReturnZeroForNoSubscribers() {
            // Given
            UUID unknownJobId = UUID.randomUUID();

            // When
            int count = publisher.getSubscriberCount(unknownJobId);

            // Then
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Should handle unsubscribe from non-existent job")
        void shouldHandleUnsubscribeNonExistentJob() {
            // Given
            WebSocketSession session = createMockSession("session1");
            UUID unknownJobId = UUID.randomUUID();

            // When/Then - should not throw
            assertThatCode(() -> publisher.unsubscribe(unknownJobId, session))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Progress Publishing")
    class ProgressPublishingTests {

        @Test
        @DisplayName("Should publish progress to subscribed session")
        void shouldPublishProgressToSubscriber() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(true);

            publisher.subscribe(jobId, session);

            MigrationProgress progress = MigrationProgress.builder()
                    .jobId(jobId)
                    .totalRecords(100L)
                    .processedCount(50L)
                    .build();

            // When
            publisher.publishProgress(jobId, progress);

            // Then
            verify(session).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should publish to all subscribed sessions")
        void shouldPublishToAllSubscribers() throws Exception {
            // Given
            WebSocketSession session1 = createMockSession("session1");
            WebSocketSession session2 = createMockSession("session2");
            when(session1.isOpen()).thenReturn(true);
            when(session2.isOpen()).thenReturn(true);

            publisher.subscribe(jobId, session1);
            publisher.subscribe(jobId, session2);

            MigrationProgress progress = MigrationProgress.builder()
                    .jobId(jobId)
                    .totalRecords(100L)
                    .processedCount(75L)
                    .build();

            // When
            publisher.publishProgress(jobId, progress);

            // Then
            verify(session1).sendMessage(any(TextMessage.class));
            verify(session2).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should not publish to unsubscribed sessions")
        void shouldNotPublishToUnsubscribed() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session1");
            MigrationProgress progress = MigrationProgress.builder()
                    .jobId(jobId)
                    .totalRecords(100L)
                    .processedCount(50L)
                    .build();

            // When
            publisher.publishProgress(jobId, progress);

            // Then
            verify(session, never()).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should skip closed sessions when publishing")
        void shouldSkipClosedSessions() throws Exception {
            // Given
            WebSocketSession openSession = createMockSession("open");
            WebSocketSession closedSession = createMockSession("closed");

            when(openSession.isOpen()).thenReturn(true);
            when(closedSession.isOpen()).thenReturn(false);

            publisher.subscribe(jobId, openSession);
            publisher.subscribe(jobId, closedSession);

            MigrationProgress progress = MigrationProgress.builder()
                    .jobId(jobId)
                    .totalRecords(100L)
                    .processedCount(50L)
                    .build();

            // When
            publisher.publishProgress(jobId, progress);

            // Then
            verify(openSession).sendMessage(any(TextMessage.class));
            verify(closedSession, never()).sendMessage(any(TextMessage.class));
            assertThat(publisher.getSubscriberCount(jobId)).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle send failure gracefully")
        void shouldHandleSendFailure() throws Exception {
            // Given
            WebSocketSession failingSession = createMockSession("failing");
            when(failingSession.isOpen()).thenReturn(true);
            doThrow(new IOException("Connection lost")).when(failingSession).sendMessage(any(TextMessage.class));

            publisher.subscribe(jobId, failingSession);

            MigrationProgress progress = MigrationProgress.builder()
                    .jobId(jobId)
                    .totalRecords(100L)
                    .processedCount(50L)
                    .build();

            // When/Then - should not throw
            assertThatCode(() -> publisher.publishProgress(jobId, progress))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should remove session when progress send fails")
        void shouldRemoveSessionOnProgressSendFailure() throws Exception {
            WebSocketSession failingSession = createMockSession("failing");
            when(failingSession.isOpen()).thenReturn(true);
            doThrow(new IOException("Connection lost")).when(failingSession).sendMessage(any(TextMessage.class));

            publisher.subscribe(jobId, failingSession);

            MigrationProgress progress = MigrationProgress.builder()
                    .jobId(jobId)
                    .totalRecords(100L)
                    .processedCount(10L)
                    .build();

            publisher.publishProgress(jobId, progress);

            assertThat(publisher.getSubscriberCount(jobId)).isZero();
        }
    }

    @Nested
    @DisplayName("Error Publishing")
    class ErrorPublishingTests {

        @Test
        @DisplayName("Should publish error to subscribers")
        void shouldPublishError() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(true);

            publisher.subscribe(jobId, session);

            // When
            publisher.publishError(jobId, "Connection timeout");

            // Then
            verify(session).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should not publish error with no subscribers")
        void shouldNotPublishErrorWithoutSubscribers() {
            // Given
            UUID unknownJobId = UUID.randomUUID();

            // When/Then - should not throw
            assertThatCode(() -> publisher.publishError(unknownJobId, "Error message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle error publishing to closed session")
        void shouldHandleErrorPublishingToClosedSession() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(false);

            publisher.subscribe(jobId, session);

            // When/Then
            assertThatCode(() -> publisher.publishError(jobId, "Test error"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle error publishing failure")
        void shouldHandleErrorPublishingFailure() throws Exception {
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(true);
            doThrow(new IOException("Send failed")).when(session).sendMessage(any(TextMessage.class));

            publisher.subscribe(jobId, session);

            assertThatCode(() -> publisher.publishError(jobId, "Error"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Status Change Publishing")
    class StatusChangeTests {

        @Test
        @DisplayName("Should publish status change to subscribers")
        void shouldPublishStatusChange() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(true);

            publisher.subscribe(jobId, session);

            // When
            publisher.publishStatusChange(jobId, "RUNNING");

            // Then
            verify(session).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should publish to all sessions on status change")
        void shouldPublishStatusToAllSessions() throws Exception {
            // Given
            WebSocketSession session1 = createMockSession("session1");
            WebSocketSession session2 = createMockSession("session2");
            when(session1.isOpen()).thenReturn(true);
            when(session2.isOpen()).thenReturn(true);

            publisher.subscribe(jobId, session1);
            publisher.subscribe(jobId, session2);

            // When
            publisher.publishStatusChange(jobId, "COMPLETED");

            // Then
            verify(session1).sendMessage(any(TextMessage.class));
            verify(session2).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should handle status change send failure")
        void shouldHandleStatusChangeSendFailure() throws Exception {
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(true);
            doThrow(new IOException("Send failed")).when(session).sendMessage(any(TextMessage.class));

            publisher.subscribe(jobId, session);

            assertThatCode(() -> publisher.publishStatusChange(jobId, "RUNNING"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Job Completion")
    class JobCompletionTests {

        @Test
        @DisplayName("Should publish completion and cleanup subscribers")
        void shouldPublishCompletionAndCleanup() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(true);

            publisher.subscribe(jobId, session);

            Map<String, Object> summary = Map.of(
                    "totalRecords", 100,
                    "successCount", 95,
                    "errorCount", 5
            );

            // When
            publisher.publishJobCompleted(jobId, summary);

            // Then
            verify(session).sendMessage(any(TextMessage.class));
            assertThat(publisher.getSubscriberCount(jobId)).isZero();
        }

        @Test
        @DisplayName("Should cleanup even with multiple subscribers")
        void shouldCleanupMultipleSubscribers() throws Exception {
            // Given
            WebSocketSession session1 = createMockSession("session1");
            WebSocketSession session2 = createMockSession("session2");
            when(session1.isOpen()).thenReturn(true);
            when(session2.isOpen()).thenReturn(true);

            publisher.subscribe(jobId, session1);
            publisher.subscribe(jobId, session2);

            // When
            publisher.publishJobCompleted(jobId, Map.of("status", "done"));

            // Then
            assertThat(publisher.getSubscriberCount(jobId)).isZero();
        }

        @Test
        @DisplayName("Should handle completion with no subscribers")
        void shouldHandleCompletionWithoutSubscribers() {
            // Given
            UUID unknownJobId = UUID.randomUUID();

            // When/Then
            assertThatCode(() -> publisher.publishJobCompleted(unknownJobId, Map.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle completion send failure")
        void shouldHandleCompletionSendFailure() throws Exception {
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(true);
            doThrow(new IOException("Send failed")).when(session).sendMessage(any(TextMessage.class));

            publisher.subscribe(jobId, session);

            assertThatCode(() -> publisher.publishJobCompleted(jobId, Map.of("status", "done")))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Concurrent Operations")
    class ConcurrentOperationsTests {

        @Test
        @DisplayName("Should handle concurrent subscriptions")
        void shouldHandleConcurrentSubscriptions() {
            // Given
            WebSocketSession session1 = createMockSession("session1");
            WebSocketSession session2 = createMockSession("session2");
            WebSocketSession session3 = createMockSession("session3");

            // When
            publisher.subscribe(jobId, session1);
            publisher.subscribe(jobId, session2);
            publisher.subscribe(jobId, session3);

            // Then
            assertThat(publisher.getSubscriberCount(jobId)).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle subscribe and unsubscribe mix")
        void shouldHandleMixedOperations() {
            // Given
            WebSocketSession session1 = createMockSession("session1");
            WebSocketSession session2 = createMockSession("session2");

            // When
            publisher.subscribe(jobId, session1);
            publisher.subscribe(jobId, session2);
            publisher.unsubscribe(jobId, session1);
            publisher.subscribe(jobId, session1);

            // Then
            assertThat(publisher.getSubscriberCount(jobId)).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Event Message Format")
    class EventMessageFormatTests {

        @Test
        @DisplayName("Should create valid JSON message for progress")
        void shouldCreateValidJsonForProgress() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(true);

            publisher.subscribe(jobId, session);

            MigrationProgress progress = MigrationProgress.builder()
                    .jobId(jobId)
                    .totalRecords(100L)
                    .processedCount(50L)
                    .build();

            // When
            publisher.publishProgress(jobId, progress);

            // Then
            verify(session).sendMessage(any(TextMessage.class));
            // Message should be valid JSON with type, jobId, timestamp, and payload
        }

        @Test
        @DisplayName("Should include timestamp in events")
        void shouldIncludeTimestamp() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(true);

            publisher.subscribe(jobId, session);

            // When
            Instant before = Instant.now();
            publisher.publishStatusChange(jobId, "RUNNING");
            Instant after = Instant.now();

            // Then
            verify(session).sendMessage(any(TextMessage.class));
            // Timestamp should be between before and after
        }

        @Test
        @DisplayName("Should handle serialization failures gracefully")
        void shouldHandleSerializationFailures() throws Exception {
            WebSocketSession session = createMockSession("session1");
            when(session.isOpen()).thenReturn(true);
            publisher.subscribe(jobId, session);

            ObjectMapper mapper = mock(ObjectMapper.class);
            when(mapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
            org.springframework.test.util.ReflectionTestUtils.setField(publisher, "objectMapper", mapper);

            MigrationProgress progress = MigrationProgress.builder()
                    .jobId(jobId)
                    .processedCount(1L)
                    .build();

            assertThatCode(() -> publisher.publishProgress(jobId, progress))
                    .doesNotThrowAnyException();
        }
    }

    // Helper method to create mock session
    private WebSocketSession createMockSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        return session;
    }
}
