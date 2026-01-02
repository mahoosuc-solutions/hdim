package com.healthdata.events.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.events.entity.DeadLetterQueueEntity;
import com.healthdata.events.entity.DeadLetterQueueEntity.DLQStatus;
import com.healthdata.events.metrics.DLQMetrics;
import com.healthdata.events.repository.DeadLetterQueueRepository;

import lombok.RequiredArgsConstructor;

/**
 * Dead Letter Queue Service
 *
 * Manages failed event processing with automatic retry and monitoring.
 * Part of the event-driven patient health assessment pipeline.
 */
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DeadLetterQueueRepository dlqRepository;
    private final DLQMetrics dlqMetrics;

    /**
     * Record a failed event to the DLQ
     */
    @Transactional
    public DeadLetterQueueEntity recordFailure(
            String topic,
            String eventType,
            String tenantId,
            String patientId,
            Object eventPayload,
            Exception error) {

        String stackTrace = getStackTrace(error);
        String payloadJson = serializePayload(eventPayload);

        DeadLetterQueueEntity dlqEntry = DeadLetterQueueEntity.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .tenantId(tenantId)
                .topic(topic)
                .eventType(eventType)
                .patientId(patientId)
                .eventPayload(payloadJson)
                .errorMessage(error.getMessage())
                .stackTrace(stackTrace)
                .retryCount(0)
                .maxRetryCount(3)
                .firstFailureAt(Instant.now())
                .status(DLQStatus.FAILED)
                .build();

        dlqEntry.setNextRetryAt(dlqEntry.calculateNextRetry());

        DeadLetterQueueEntity saved = dlqRepository.save(dlqEntry);

        // Record metrics
        dlqMetrics.recordFailure(topic, eventType);

        log.error("Event failed and recorded to DLQ: topic={}, eventType={}, tenantId={}, patientId={}, dlqId={}",
                topic, eventType, tenantId, patientId, saved.getId(), error);

        return saved;
    }

    /**
     * Get all events eligible for retry
     */
    @Transactional(readOnly = true)
    public List<DeadLetterQueueEntity> getRetryEligible() {
        return dlqRepository.findRetryEligible(Instant.now());
    }

    /**
     * Mark event for retry
     */
    @Transactional
    public void markForRetry(UUID dlqId) {
        DeadLetterQueueEntity entry = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ entry not found: " + dlqId));

        if (!entry.isRetryEligible()) {
            throw new IllegalStateException("Event is not eligible for retry: " + dlqId);
        }

        entry.markForRetry();
        dlqRepository.save(entry);

        // Record metrics
        dlqMetrics.recordRetry();

        log.info("DLQ entry marked for retry: dlqId={}, retryCount={}", dlqId, entry.getRetryCount());
    }

    /**
     * Mark event as successfully retried (removes from DLQ)
     */
    @Transactional
    public void markAsResolved(UUID dlqId, String resolvedBy, String notes) {
        DeadLetterQueueEntity entry = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ entry not found: " + dlqId));

        entry.markAsResolved(resolvedBy, notes);
        dlqRepository.save(entry);

        log.info("DLQ entry resolved: dlqId={}, resolvedBy={}", dlqId, resolvedBy);
    }

    /**
     * Mark event as exhausted (max retries reached)
     */
    @Transactional
    public void markAsExhausted(UUID dlqId) {
        DeadLetterQueueEntity entry = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ entry not found: " + dlqId));

        entry.markAsExhausted();
        dlqRepository.save(entry);

        log.warn("DLQ entry exhausted all retries: dlqId={}, topic={}, eventType={}",
                dlqId, entry.getTopic(), entry.getEventType());
    }

    /**
     * Record retry failure
     */
    @Transactional
    public void recordRetryFailure(UUID dlqId, Exception error) {
        DeadLetterQueueEntity entry = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ entry not found: " + dlqId));

        entry.setErrorMessage(error.getMessage());
        entry.setStackTrace(getStackTrace(error));
        entry.setLastRetryAt(Instant.now());
        entry.setStatus(DLQStatus.FAILED);

        if (entry.getRetryCount() >= entry.getMaxRetryCount()) {
            entry.markAsExhausted();
        } else {
            entry.setNextRetryAt(entry.calculateNextRetry());
        }

        dlqRepository.save(entry);

        log.error("DLQ retry failed: dlqId={}, retryCount={}", dlqId, entry.getRetryCount(), error);
    }

    /**
     * Get failed events by tenant
     */
    @Transactional(readOnly = true)
    public Page<DeadLetterQueueEntity> getFailedByTenant(String tenantId, Pageable pageable) {
        return dlqRepository.findByTenantIdAndStatus(tenantId, DLQStatus.FAILED, pageable);
    }

    /**
     * Get failed events by patient
     */
    @Transactional(readOnly = true)
    public List<DeadLetterQueueEntity> getFailedByPatient(String patientId) {
        return dlqRepository.findByPatientIdAndStatus(patientId, DLQStatus.FAILED);
    }

    /**
     * Get failed events by topic
     */
    @Transactional(readOnly = true)
    public List<DeadLetterQueueEntity> getFailedByTopic(String topic) {
        return dlqRepository.findByTopicAndStatus(topic, DLQStatus.FAILED);
    }

    /**
     * Get exhausted events (need manual intervention)
     */
    @Transactional(readOnly = true)
    public List<DeadLetterQueueEntity> getExhausted() {
        return dlqRepository.findExhausted();
    }

    /**
     * Get recent failures
     */
    @Transactional(readOnly = true)
    public List<DeadLetterQueueEntity> getRecentFailures(int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600);
        return dlqRepository.findRecentFailures(since);
    }

    /**
     * Get DLQ statistics for monitoring
     */
    @Transactional(readOnly = true)
    public DLQStats getStats(String tenantId) {
        long failed = dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.FAILED);
        long exhausted = dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.EXHAUSTED);
        long retrying = dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.RETRYING);

        return new DLQStats(failed, exhausted, retrying);
    }

    /**
     * Clean up old resolved entries (data retention)
     */
    @Transactional
    public void cleanupOldResolved(int daysToKeep) {
        Instant before = Instant.now().minusSeconds(daysToKeep * 86400);
        dlqRepository.deleteOldResolved(before);
        log.info("Cleaned up DLQ entries resolved before {}", before);
    }

    private String getStackTrace(Exception error) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);
        return sw.toString();
    }

    private String serializePayload(Object payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event payload", e);
            return "{}";
        }
    }

    public record DLQStats(long failed, long exhausted, long retrying) {}
}
