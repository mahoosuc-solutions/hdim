package com.healthdata.quality.websocket;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Audit Event Publisher Tests")
class AuditEventPublisherTest {

    @Test
    @DisplayName("Should skip publishing when disabled")
    void shouldSkipPublishingWhenDisabled() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        AuditEventPublisher publisher = new AuditEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "auditEnabled", false);

        publisher.publish(Map.of("eventType", "TEST", "tenantId", "tenant-1"));

        verify(kafkaTemplate, never()).send(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    @DisplayName("Should publish audit event when enabled")
    void shouldPublishAuditEventWhenEnabled() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        AuditEventPublisher publisher = new AuditEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "auditEnabled", true);
        ReflectionTestUtils.setField(publisher, "auditTopic", "audit-topic");

        SendResult<String, Object> sendResult = new SendResult<>(
            new ProducerRecord<>("audit-topic", "tenant-1", Map.of()),
            new RecordMetadata(new TopicPartition("audit-topic", 0), 0, 0, 0L, 0L, 0, 0)
        );
        when(kafkaTemplate.send(eq("audit-topic"), eq("tenant-1"), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publish(Map.of("eventType", "TEST", "tenantId", "tenant-1"));

        verify(kafkaTemplate).send(eq("audit-topic"), eq("tenant-1"), Mockito.any());
    }

    @Test
    @DisplayName("Should publish security events to security topic")
    void shouldPublishSecurityEventToSecurityTopic() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        AuditEventPublisher publisher = new AuditEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "auditEnabled", true);
        ReflectionTestUtils.setField(publisher, "auditTopic", "audit-topic");

        SendResult<String, Object> sendResult = new SendResult<>(
            new ProducerRecord<>("audit-topic-security", "tenant-1", Map.of()),
            new RecordMetadata(new TopicPartition("audit-topic-security", 0), 0, 0, 0L, 0L, 0, 0)
        );
        when(kafkaTemplate.send(eq("audit-topic-security"), eq("tenant-1"), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publishSecurityEvent(Map.of("eventType", "SECURITY", "tenantId", "tenant-1"));

        verify(kafkaTemplate).send(eq("audit-topic-security"), eq("tenant-1"), Mockito.any());
    }

    @Test
    @DisplayName("Should swallow exceptions when publish fails")
    void shouldSwallowExceptionsWhenPublishFails() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        AuditEventPublisher publisher = new AuditEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "auditEnabled", true);
        ReflectionTestUtils.setField(publisher, "auditTopic", "audit-topic");

        when(kafkaTemplate.send(eq("audit-topic"), eq("tenant-1"), Mockito.any()))
            .thenThrow(new RuntimeException("kafka down"));

        publisher.publish(Map.of("eventType", "TEST", "tenantId", "tenant-1"));

        verify(kafkaTemplate).send(eq("audit-topic"), eq("tenant-1"), Mockito.any());
    }

    @Test
    @DisplayName("Should skip security publish when disabled")
    void shouldSkipSecurityPublishWhenDisabled() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        AuditEventPublisher publisher = new AuditEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "auditEnabled", false);

        publisher.publishSecurityEvent(Map.of("eventType", "SECURITY", "tenantId", "tenant-1"));

        verify(kafkaTemplate, never()).send(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    @DisplayName("Should swallow exceptions when security publish fails")
    void shouldSwallowExceptionsWhenSecurityPublishFails() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        AuditEventPublisher publisher = new AuditEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "auditEnabled", true);
        ReflectionTestUtils.setField(publisher, "auditTopic", "audit-topic");

        when(kafkaTemplate.send(eq("audit-topic-security"), eq("tenant-1"), Mockito.any()))
            .thenThrow(new RuntimeException("kafka down"));

        publisher.publishSecurityEvent(Map.of("eventType", "SECURITY", "tenantId", "tenant-1"));

        verify(kafkaTemplate).send(eq("audit-topic-security"), eq("tenant-1"), Mockito.any());
    }

    @Test
    @DisplayName("Should handle async publish failures for audit events")
    void shouldHandleAsyncPublishFailures() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        AuditEventPublisher publisher = new AuditEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "auditEnabled", true);
        ReflectionTestUtils.setField(publisher, "auditTopic", "audit-topic");

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("async fail"));

        when(kafkaTemplate.send(eq("audit-topic"), eq("tenant-1"), Mockito.any()))
            .thenReturn(future);

        publisher.publish(Map.of("eventType", "TEST", "tenantId", "tenant-1"));

        verify(kafkaTemplate).send(eq("audit-topic"), eq("tenant-1"), Mockito.any());
    }

    @Test
    @DisplayName("Should handle async publish failures for security events")
    void shouldHandleAsyncSecurityPublishFailures() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        AuditEventPublisher publisher = new AuditEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "auditEnabled", true);
        ReflectionTestUtils.setField(publisher, "auditTopic", "audit-topic");

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("async fail"));

        when(kafkaTemplate.send(eq("audit-topic-security"), eq("tenant-1"), Mockito.any()))
            .thenReturn(future);

        publisher.publishSecurityEvent(Map.of("eventType", "SECURITY", "tenantId", "tenant-1"));

        verify(kafkaTemplate).send(eq("audit-topic-security"), eq("tenant-1"), Mockito.any());
    }
}
