package com.healthdata.cql.event.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditEventProducer.
 *
 * Tests the Kafka publishing functionality for audit events with
 * mocked KafkaTemplate to verify async fire-and-forget behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditEventProducer Unit Tests")
class AuditEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AuditEventProducer auditEventProducer;

    @BeforeEach
    void setUp() {
        auditEventProducer = new AuditEventProducer(kafkaTemplate);

        // Configure topic and enabled flag
        ReflectionTestUtils.setField(auditEventProducer, "auditTopic", "healthdata.audit.events");
        ReflectionTestUtils.setField(auditEventProducer, "auditEnabled", true);
    }

    @Test
    @DisplayName("Should publish CQL evaluation audit event to Kafka")
    void shouldPublishCqlEvaluationAuditEventToKafka() {
        // Given
        CqlEvaluationAuditEvent event = CqlEvaluationAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId("tenant-123")
                .performedBy("user@example.com")
                .action("EVALUATE_CQL")
                .resourceType("CQL_EVALUATION")
                .resourceId(UUID.randomUUID().toString())
                .result(AuditEvent.OperationResult.SUCCESS)
                .evaluationId(UUID.randomUUID())
                .cqlLibraryId(UUID.randomUUID())
                .cqlLibraryName("DiabetesControl")
                .patientId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .durationMs(250L)
                .dataFlowSteps(Collections.emptyList())
                .build();

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class))).thenReturn(future);

        // When
        auditEventProducer.publishEvaluationAudit(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("healthdata.audit.events");
        assertThat(keyCaptor.getValue()).isEqualTo("tenant-123"); // Partitioned by tenantId
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    @DisplayName("Should publish CQL library audit event to Kafka")
    void shouldPublishCqlLibraryAuditEventToKafka() {
        // Given
        CqlLibraryAuditEvent event = CqlLibraryAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId("tenant-456")
                .performedBy("admin@example.com")
                .action("CREATE_LIBRARY")
                .resourceType("CQL_LIBRARY")
                .resourceId(UUID.randomUUID().toString())
                .result(AuditEvent.OperationResult.SUCCESS)
                .libraryId(UUID.randomUUID())
                .libraryName("CDC-HEDIS")
                .libraryVersion("2024.1.0")
                .libraryContentLength(5000)
                .build();

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class))).thenReturn(future);

        // When
        auditEventProducer.publishLibraryAudit(event);

        // Then
        verify(kafkaTemplate).send(eq("healthdata.audit.events"), eq("tenant-456"), eq(event));
    }

    @Test
    @DisplayName("Should publish value set audit event to Kafka")
    void shouldPublishValueSetAuditEventToKafka() {
        // Given
        ValueSetAuditEvent event = ValueSetAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId("tenant-789")
                .performedBy("user@example.com")
                .action("UPDATE_VALUESET")
                .resourceType("VALUE_SET")
                .resourceId(UUID.randomUUID().toString())
                .result(AuditEvent.OperationResult.SUCCESS)
                .valueSetId(UUID.randomUUID())
                .valueSetOid("2.16.840.1.113883.3.464.1003.103.12.1001")
                .valueSetName("Diabetes")
                .valueSetVersion("2024.1")
                .codesCount(150)
                .build();

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class))).thenReturn(future);

        // When
        auditEventProducer.publishValueSetAudit(event);

        // Then
        verify(kafkaTemplate).send(eq("healthdata.audit.events"), eq("tenant-789"), eq(event));
    }

    @Test
    @DisplayName("Should not publish when auditing is disabled")
    void shouldNotPublishWhenAuditingIsDisabled() {
        // Given
        ReflectionTestUtils.setField(auditEventProducer, "auditEnabled", false);

        CqlEvaluationAuditEvent event = CqlEvaluationAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId("tenant-123")
                .performedBy("user@example.com")
                .action("EVALUATE_CQL")
                .resourceType("CQL_EVALUATION")
                .resourceId(UUID.randomUUID().toString())
                .result(AuditEvent.OperationResult.SUCCESS)
                .evaluationId(UUID.randomUUID())
                .build();

        // When
        auditEventProducer.publishEvaluationAudit(event);

        // Then
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Should handle Kafka send failure gracefully")
    void shouldHandleKafkaSendFailureGracefully() {
        // Given
        CqlEvaluationAuditEvent event = CqlEvaluationAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId("tenant-123")
                .performedBy("user@example.com")
                .action("EVALUATE_CQL")
                .resourceType("CQL_EVALUATION")
                .resourceId(UUID.randomUUID().toString())
                .result(AuditEvent.OperationResult.SUCCESS)
                .evaluationId(UUID.randomUUID())
                .build();

        // Simulate Kafka failure
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka connection failed"));
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class))).thenReturn(future);

        // When/Then - Should not throw exception (fire-and-forget)
        assertThatCode(() -> auditEventProducer.publishEvaluationAudit(event))
                .doesNotThrowAnyException();

        verify(kafkaTemplate).send(anyString(), anyString(), any(AuditEvent.class));
    }

    @Test
    @DisplayName("Should partition events by tenantId for ordered processing")
    void shouldPartitionEventsByTenantIdForOrderedProcessing() {
        // Given
        String tenantId = "tenant-ordering-test";

        CqlEvaluationAuditEvent event1 = CqlEvaluationAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId(tenantId)
                .performedBy("user@example.com")
                .action("EVALUATE_CQL")
                .resourceType("CQL_EVALUATION")
                .resourceId(UUID.randomUUID().toString())
                .result(AuditEvent.OperationResult.SUCCESS)
                .evaluationId(UUID.randomUUID())
                .build();

        CqlEvaluationAuditEvent event2 = CqlEvaluationAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId(tenantId)
                .performedBy("user@example.com")
                .action("EVALUATE_CQL")
                .resourceType("CQL_EVALUATION")
                .resourceId(UUID.randomUUID().toString())
                .result(AuditEvent.OperationResult.SUCCESS)
                .evaluationId(UUID.randomUUID())
                .build();

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class))).thenReturn(future);

        // When
        auditEventProducer.publishEvaluationAudit(event1);
        auditEventProducer.publishEvaluationAudit(event2);

        // Then - Both should use same partition key (tenantId)
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2)).send(anyString(), keyCaptor.capture(), any(AuditEvent.class));

        List<String> keys = keyCaptor.getAllValues();
        assertThat(keys).hasSize(2);
        assertThat(keys.get(0)).isEqualTo(tenantId);
        assertThat(keys.get(1)).isEqualTo(tenantId);
    }


    @Test
    @DisplayName("Should publish data access audit event")
    void shouldPublishDataAccessAuditEvent() {
        // Given
        DataAccessAuditEvent event = DataAccessAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId("tenant-123")
                .performedBy("user@example.com")
                .action("ACCESS_PATIENT_DATA")
                .resourceType("DATA_ACCESS")
                .resourceId(UUID.randomUUID().toString())
                .result(AuditEvent.OperationResult.SUCCESS)
                .patientId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .fhirResourceIds(List.of("Patient/123", "Observation/456"))
                .fhirResourceType("Observation")
                .resourceCount(2)
                .purpose("Quality measure evaluation")
                .build();

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class))).thenReturn(future);

        // When
        auditEventProducer.publishDataAccessAudit(event);

        // Then
        verify(kafkaTemplate).send(eq("healthdata.audit.events"), eq("tenant-123"), eq(event));
    }

    @Test
    @DisplayName("Should publish security audit event")
    void shouldPublishSecurityAuditEvent() {
        // Given
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId("tenant-123")
                .performedBy("attacker@example.com")
                .action("UNAUTHORIZED_ACCESS")
                .resourceType("SECURITY")
                .resourceId(UUID.randomUUID().toString())
                .result(AuditEvent.OperationResult.FAILURE)
                .securityEventType("ACCESS_DENIED")
                .username("attacker@example.com")
                .denialReason("Invalid credentials")
                .severity("HIGH")
                .build();

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class))).thenReturn(future);

        // When
        auditEventProducer.publishSecurityAudit(event);

        // Then
        verify(kafkaTemplate).send(eq("healthdata.audit.events"), eq("tenant-123"), eq(event));
    }

    @Test
    @DisplayName("Should use async fire-and-forget pattern")
    void shouldUseAsyncFireAndForgetPattern() {
        // Given
        CqlEvaluationAuditEvent event = CqlEvaluationAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .tenantId("tenant-123")
                .performedBy("user@example.com")
                .action("EVALUATE_CQL")
                .resourceType("CQL_EVALUATION")
                .resourceId(UUID.randomUUID().toString())
                .result(AuditEvent.OperationResult.SUCCESS)
                .evaluationId(UUID.randomUUID())
                .build();

        // Create a future that completes asynchronously
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any(AuditEvent.class))).thenReturn(future);

        // When
        auditEventProducer.publishEvaluationAudit(event);

        // Then - Method should return immediately without waiting for future completion
        verify(kafkaTemplate).send(anyString(), anyString(), any(AuditEvent.class));
        assertThat(future.isDone()).isFalse(); // Future not completed yet, but method returned
    }
}
