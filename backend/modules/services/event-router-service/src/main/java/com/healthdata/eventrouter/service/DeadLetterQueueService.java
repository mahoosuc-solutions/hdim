package com.healthdata.eventrouter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.entity.DeadLetterEventEntity;
import com.healthdata.eventrouter.persistence.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private static final String DLQ_TOPIC = "event-router.dlq";
    private static final int MAX_RETRY_COUNT = 5;

    private final DeadLetterEventRepository dlqRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendToDeadLetterQueue(EventMessage event, String reason) {
        try {
            // Persist to database
            DeadLetterEventEntity dlqEvent = new DeadLetterEventEntity();
            dlqEvent.setTenantId(event.getTenantId());
            dlqEvent.setEventType(event.getEventType());
            dlqEvent.setSourceTopic(event.getSourceTopic());
            dlqEvent.setOriginalPayload(objectMapper.writeValueAsString(event.getPayload()));
            dlqEvent.setFailureReason(reason);
            dlqEvent.setRetryCount(0);
            dlqRepository.save(dlqEvent);

            // Send to DLQ topic
            String dlqMessage = objectMapper.writeValueAsString(dlqEvent);
            kafkaTemplate.send(DLQ_TOPIC, dlqMessage);

            log.warn("Event sent to DLQ - Type: {}, Reason: {}", event.getEventType(), reason);
        } catch (Exception e) {
            log.error("Error sending event to DLQ", e);
        }
    }

    public boolean retryEvent(Long dlqEventId) {
        try {
            DeadLetterEventEntity dlqEvent = dlqRepository.findById(dlqEventId)
                .orElse(null);

            if (dlqEvent == null) {
                log.warn("DLQ event not found: {}", dlqEventId);
                return false;
            }

            if (dlqEvent.getRetryCount() >= MAX_RETRY_COUNT) {
                log.warn("Max retry count reached for DLQ event: {}", dlqEventId);
                return false;
            }

            // Increment retry count
            dlqEvent.setRetryCount(dlqEvent.getRetryCount() + 1);
            dlqEvent.setLastRetryAt(Instant.now());
            dlqRepository.save(dlqEvent);

            // Re-publish to original topic
            kafkaTemplate.send(dlqEvent.getSourceTopic(), dlqEvent.getOriginalPayload());

            log.info("Retried DLQ event: {}, attempt: {}", dlqEventId, dlqEvent.getRetryCount());
            return true;
        } catch (Exception e) {
            log.error("Error retrying DLQ event: {}", dlqEventId, e);
            return false;
        }
    }

    public long getDeadLetterCount() {
        return dlqRepository.countAll();
    }

    public long getDeadLetterCountByTenant(String tenantId) {
        return dlqRepository.countByTenantId(tenantId);
    }
}
