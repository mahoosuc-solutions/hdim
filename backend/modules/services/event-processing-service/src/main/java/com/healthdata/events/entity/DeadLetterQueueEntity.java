package com.healthdata.events.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dead Letter Queue Entity
 *
 * Stores failed events for manual review and retry.
 * Part of the event-driven patient health assessment pipeline.
 */
@Entity
@Table(name = "dead_letter_queue")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterQueueEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "event_payload", columnDefinition = "jsonb")
    private String eventPayload;

    @Column(name = "error_message", nullable = false, columnDefinition = "text")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retry_count")
    @Builder.Default
    private Integer maxRetryCount = 3;

    @Column(name = "first_failure_at", nullable = false)
    private Instant firstFailureAt;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DLQStatus status = DLQStatus.FAILED;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by", length = 128)
    private String resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;

    public enum DLQStatus {
        FAILED,           // Initial failure, pending retry
        RETRYING,         // Currently being retried
        EXHAUSTED,        // Max retries reached, needs manual intervention
        RESOLVED,         // Manually resolved
        DISCARDED         // Intentionally discarded
    }

    /**
     * Calculate next retry time using exponential backoff
     */
    public Instant calculateNextRetry() {
        // Exponential backoff: 1min, 5min, 30min, 2hr, 12hr
        long[] backoffMinutes = {1, 5, 30, 120, 720};
        int index = Math.min(retryCount, backoffMinutes.length - 1);
        return Instant.now().plusSeconds(backoffMinutes[index] * 60);
    }

    /**
     * Check if event is eligible for retry
     */
    public boolean isRetryEligible() {
        return status == DLQStatus.FAILED
            && retryCount < maxRetryCount
            && (nextRetryAt == null || Instant.now().isAfter(nextRetryAt));
    }

    /**
     * Mark event for retry
     */
    public void markForRetry() {
        this.retryCount++;
        this.lastRetryAt = Instant.now();
        this.status = DLQStatus.RETRYING;
        this.nextRetryAt = calculateNextRetry();
    }

    /**
     * Mark event as exhausted (max retries reached)
     */
    public void markAsExhausted() {
        this.status = DLQStatus.EXHAUSTED;
    }

    /**
     * Mark event as resolved
     */
    public void markAsResolved(String resolvedBy, String notes) {
        this.status = DLQStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
    }
}
