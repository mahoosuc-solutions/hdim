package com.healthdata.events.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.entity.DeadLetterQueueEntity;
import com.healthdata.events.model.DLQExhaustionAlert;

import lombok.RequiredArgsConstructor;

/**
 * DLQ Retry Processor
 *
 * Automatically retries failed events on a schedule using exponential backoff.
 * Part of the event-driven patient health assessment pipeline.
 */
@Service
@RequiredArgsConstructor
public class DLQRetryProcessor {

    private static final Logger log = LoggerFactory.getLogger(DLQRetryProcessor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DeadLetterQueueService dlqService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DLQAlertingService alertingService;

    /**
     * Process DLQ retries every 2 minutes
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void processRetries() {
        List<DeadLetterQueueEntity> retryEligible = dlqService.getRetryEligible();

        if (retryEligible.isEmpty()) {
            log.debug("No DLQ entries eligible for retry");
            return;
        }

        log.info("Processing {} DLQ entries for retry", retryEligible.size());

        for (DeadLetterQueueEntity entry : retryEligible) {
            try {
                retryEvent(entry);
            } catch (Exception e) {
                log.error("Failed to retry DLQ entry: {}", entry.getId(), e);
                dlqService.recordRetryFailure(entry.getId(), e);
            }
        }
    }

    /**
     * Retry a single failed event by republishing to Kafka
     */
    private void retryEvent(DeadLetterQueueEntity entry) {
        log.info("Retrying DLQ entry: dlqId={}, topic={}, eventType={}, retryCount={}",
                entry.getId(), entry.getTopic(), entry.getEventType(), entry.getRetryCount());

        // Mark as retrying
        dlqService.markForRetry(entry.getId());

        // Republish event to original topic
        try {
            Object payload = deserializePayload(entry.getEventPayload());
            kafkaTemplate.send(entry.getTopic(), entry.getTenantId(), payload);

            // Mark as resolved if successful
            dlqService.markAsResolved(entry.getId(), "DLQRetryProcessor",
                    "Automatically retried successfully on attempt " + entry.getRetryCount());

            log.info("DLQ entry successfully retried: dlqId={}", entry.getId());

        } catch (Exception e) {
            // Record failure
            dlqService.recordRetryFailure(entry.getId(), e);

            // Check if exhausted
            if (entry.getRetryCount() >= entry.getMaxRetryCount()) {
                dlqService.markAsExhausted(entry.getId());
                log.error("DLQ entry exhausted after {} retries: dlqId={}, topic={}, eventType={}",
                        entry.getRetryCount(), entry.getId(), entry.getTopic(), entry.getEventType());

                // Send exhaustion alert to operations team
                handleExhaustion(entry);
            }
        }
    }

    /**
     * Alert on exhausted events (runs every hour)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void alertOnExhausted() {
        List<DeadLetterQueueEntity> exhausted = dlqService.getExhausted();

        if (!exhausted.isEmpty()) {
            log.error("ALERT: {} DLQ entries have exhausted all retries and require manual intervention",
                    exhausted.size());

            // Group by topic for better visibility
            exhausted.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            DeadLetterQueueEntity::getTopic,
                            java.util.stream.Collectors.counting()))
                    .forEach((topic, count) ->
                            log.error("  - Topic {}: {} exhausted events", topic, count));

            // Process each exhausted entry for alerting (if not already alerted)
            for (DeadLetterQueueEntity entry : exhausted) {
                handleExhaustion(entry);
            }
        }
    }

    /**
     * Handle DLQ exhaustion by sending alerts and creating dashboard entries
     */
    private void handleExhaustion(DeadLetterQueueEntity entry) {
        try {
            // Build alert from DLQ entry
            DLQExhaustionAlert alert = DLQExhaustionAlert.builder()
                .dlqId(entry.getId())
                .eventId(entry.getEventId())
                .eventType(entry.getEventType())
                .tenantId(entry.getTenantId())
                .originalErrorMessage(entry.getErrorMessage())
                .retryCount(entry.getRetryCount())
                .firstFailureTimestamp(entry.getFirstFailureAt())
                .lastFailureTimestamp(entry.getLastRetryAt() != null
                    ? entry.getLastRetryAt() : entry.getFirstFailureAt())
                .affectedPatientId(entry.getPatientId())
                .topic(entry.getTopic())
                .stackTrace(entry.getStackTrace())
                .build();

            // Send standard exhaustion alert
            alertingService.sendExhaustionAlert(alert);

            // Create dashboard entry for monitoring
            String dashboardEntry = alertingService.createDashboardEntry(alert);
            log.info("Dashboard entry created for exhausted DLQ event: {}", dashboardEntry);

            // Escalate if critical
            if (alert.isCritical()) {
                alertingService.escalateCriticalFailure(alert);
                log.warn("Critical DLQ failure escalated: eventType={}, patientId={}",
                    alert.getEventType(), alert.getAffectedPatientId());
            }

        } catch (Exception e) {
            log.error("Failed to handle DLQ exhaustion for entry: {}", entry.getId(), e);
            // Don't rethrow - we don't want alerting failures to break DLQ processing
        }
    }

    /**
     * Cleanup old resolved entries (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldEntries() {
        log.info("Starting DLQ cleanup of old resolved entries");
        dlqService.cleanupOldResolved(30); // Keep 30 days
        log.info("DLQ cleanup completed");
    }

    private Object deserializePayload(String payloadJson) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(payloadJson, Object.class);
    }
}
