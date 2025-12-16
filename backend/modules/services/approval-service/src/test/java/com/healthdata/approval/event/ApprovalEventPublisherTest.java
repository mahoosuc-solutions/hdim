package com.healthdata.approval.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ApprovalEventPublisher Tests")
class ApprovalEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ApprovalEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String CORRELATION_ID = "corr-789";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventPublisher, "kafkaEnabled", true);
    }

    @Nested
    @DisplayName("Publish Created Event")
    class PublishCreatedTests {

        @Test
        @DisplayName("should publish created event successfully")
        void publishCreated_Success() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When
            eventPublisher.publishCreated(request);

            // Wait a bit for async execution
            Thread.sleep(100);

            // Then
            verify(kafkaTemplate).send(eq("approval-events"), anyString(), anyString());
        }

        @Test
        @DisplayName("should skip when Kafka disabled")
        void publishCreated_KafkaDisabled_Skips() throws Exception {
            // Given
            ReflectionTestUtils.setField(eventPublisher, "kafkaEnabled", false);
            ApprovalRequest request = createRequest();

            // When
            eventPublisher.publishCreated(request);
            Thread.sleep(100);

            // Then
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should use correlation ID as partition key when available")
        void publishCreated_WithCorrelationId_UsesAsKey() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.setCorrelationId(CORRELATION_ID);
            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When
            eventPublisher.publishCreated(request);
            Thread.sleep(100);

            // Then
            verify(kafkaTemplate).send(eq("approval-events"), eq(CORRELATION_ID), anyString());
        }

        @Test
        @DisplayName("should use request ID as key when no correlation ID")
        void publishCreated_NoCorrelationId_UsesRequestId() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.setCorrelationId(null);
            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When
            eventPublisher.publishCreated(request);
            Thread.sleep(100);

            // Then
            verify(kafkaTemplate).send(eq("approval-events"), eq(request.getId().toString()), anyString());
        }
    }

    @Nested
    @DisplayName("Publish Assigned Event")
    class PublishAssignedTests {

        @Test
        @DisplayName("should publish assigned event with actor")
        void publishAssigned_WithActor_Success() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.setStatus(ApprovalStatus.ASSIGNED);
            request.setAssignedTo("reviewer-123");
            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When
            eventPublisher.publishAssigned(request, "admin-456");
            Thread.sleep(100);

            // Then
            verify(kafkaTemplate).send(eq("approval-events"), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Publish Approved Event")
    class PublishApprovedTests {

        @Test
        @DisplayName("should publish approved event")
        void publishApproved_Success() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.approve("reviewer-123", "Looks good");
            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When
            eventPublisher.publishApproved(request, "reviewer-123");
            Thread.sleep(100);

            // Then
            verify(kafkaTemplate).send(eq("approval-events"), anyString(), anyString());
        }

        @Test
        @DisplayName("should handle serialization error gracefully")
        void publishApproved_SerializationError_HandlesGracefully() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.approve("reviewer-123", "Approved");

            when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

            // When/Then
            assertThatCode(() -> {
                eventPublisher.publishApproved(request, "reviewer-123");
                Thread.sleep(100);
            }).doesNotThrowAnyException();

            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Publish Rejected Event")
    class PublishRejectedTests {

        @Test
        @DisplayName("should publish rejected event")
        void publishRejected_Success() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.reject("reviewer-123", "Not appropriate");
            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When
            eventPublisher.publishRejected(request, "reviewer-123");
            Thread.sleep(100);

            // Then
            verify(kafkaTemplate).send(eq("approval-events"), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Publish Escalated Event")
    class PublishEscalatedTests {

        @Test
        @DisplayName("should publish escalated event")
        void publishEscalated_Success() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.escalate("supervisor-123", "Needs senior review");
            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When
            eventPublisher.publishEscalated(request, "reviewer-123");
            Thread.sleep(100);

            // Then
            verify(kafkaTemplate).send(eq("approval-events"), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Publish Expired Event")
    class PublishExpiredTests {

        @Test
        @DisplayName("should publish expired event with system actor")
        void publishExpired_Success() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            ReflectionTestUtils.setField(request, "status", ApprovalStatus.EXPIRED);
            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When
            eventPublisher.publishExpired(request);
            Thread.sleep(100);

            // Then
            verify(kafkaTemplate).send(eq("approval-events"), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle Kafka send failure gracefully")
        void publish_KafkaFailure_HandlesGracefully() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Kafka error"));

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When/Then
            assertThatCode(() -> {
                eventPublisher.publishCreated(request);
                Thread.sleep(100);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle null request gracefully")
        void publish_NullRequest_HandlesGracefully() throws Exception {
            // When/Then - Should handle NPE gracefully
            assertThatCode(() -> {
                eventPublisher.publishCreated(null);
                Thread.sleep(100);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Event Payload Construction")
    class EventPayloadTests {

        @Test
        @DisplayName("should include all required fields in event")
        void publishEvent_IncludesAllFields() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.setCorrelationId(CORRELATION_ID);
            request.setSourceService("test-service");
            request.approve("reviewer-123", "Approved");

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            ArgumentCaptor<ApprovalEventPublisher.ApprovalEvent> eventCaptor =
                ArgumentCaptor.forClass(ApprovalEventPublisher.ApprovalEvent.class);

            when(objectMapper.writeValueAsString(eventCaptor.capture())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When
            eventPublisher.publishApproved(request, "reviewer-123");
            Thread.sleep(100);

            // Then
            ApprovalEventPublisher.ApprovalEvent event = eventCaptor.getValue();
            assertThat(event).isNotNull();
            assertThat(event.getEventType()).isEqualTo(ApprovalEventPublisher.EventType.APPROVED);
            assertThat(event.getRequestId()).isEqualTo(request.getId().toString());
            assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(event.getCorrelationId()).isEqualTo(CORRELATION_ID);
            assertThat(event.getSourceService()).isEqualTo("test-service");
            assertThat(event.getActor()).isEqualTo("reviewer-123");
        }

        @Test
        @DisplayName("should handle null optional fields")
        void publishEvent_NullOptionalFields_HandlesGracefully() throws Exception {
            // Given
            ApprovalRequest request = ApprovalRequest.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .requestType(RequestType.AGENT_ACTION)
                .entityType("TestEntity")
                .actionRequested("TEST")
                .riskLevel(RiskLevel.LOW)
                .status(ApprovalStatus.PENDING)
                .requestedBy(USER_ID)
                .payload(new HashMap<>())
                .correlationId(null)
                .sourceService(null)
                .decisionBy(null)
                .decisionReason(null)
                .build();

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // When/Then
            assertThatCode(() -> {
                eventPublisher.publishCreated(request);
                Thread.sleep(100);
            }).doesNotThrowAnyException();
        }
    }

    // Helper methods

    private ApprovalRequest createRequest() {
        return ApprovalRequest.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .requestType(RequestType.AGENT_ACTION)
            .entityType("TestEntity")
            .entityId("entity-123")
            .actionRequested("EXECUTE")
            .riskLevel(RiskLevel.MEDIUM)
            .status(ApprovalStatus.PENDING)
            .requestedBy(USER_ID)
            .correlationId(CORRELATION_ID)
            .sourceService("test-service")
            .payload(new HashMap<>())
            .requestedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(86400))
            .build();
    }
}
