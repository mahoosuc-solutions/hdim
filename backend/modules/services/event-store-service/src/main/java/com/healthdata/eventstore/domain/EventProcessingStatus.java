package com.healthdata.eventstore.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * EventProcessingStatus - JPA Entity for event_processing_status table
 *
 * Purpose: Track event processing across multiple consumers
 *
 * Use Cases:
 * - Event replay: Resume processing from last position
 * - Idempotency: Avoid reprocessing same events
 * - Monitoring: Track consumer lag and health
 * - Dead-letter queue: Track failed event processing
 *
 * Example:
 * "care-gap-event-handler has processed events up to ID 12345"
 * "quality-measure-event-handler is stuck at event 10000 with error"
 *
 * Usage:
 * <pre>
 * EventProcessingStatus status = EventProcessingStatus.builder()
 *     .consumerName("patient-event-handler")
 *     .lastProcessedEventId(12345L)
 *     .tenantId(tenantId)
 *     .status(ProcessingStatus.RUNNING)
 *     .build();
 * </pre>
 */
@Entity
@Table(name = "event_processing_status",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_processing_consumer_tenant",
                columnNames = {"consumer_name", "tenant_id"}
        ),
        indexes = {
                @Index(name = "idx_event_processing_consumer", columnList = "consumer_name"),
                @Index(name = "idx_event_processing_tenant", columnList = "tenant_id")
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class EventProcessingStatus {

    /**
     * Primary Key: Auto-incrementing ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Consumer Name: Unique identifier for the event consumer
     * Examples: "patient-event-handler", "care-gap-event-handler"
     */
    @Column(name = "consumer_name", nullable = false, length = 255)
    private String consumerName;

    /**
     * Last Processed Event ID: The highest event_store.id this consumer has processed
     * Used for resuming processing after restart
     */
    @Column(name = "last_processed_event_id", nullable = false)
    private Long lastProcessedEventId;

    /**
     * Last Processed At: Timestamp of last successful processing
     * Used for monitoring consumer lag
     */
    @Column(name = "last_processed_at", nullable = false)
    @Builder.Default
    private Instant lastProcessedAt = Instant.now();

    /**
     * Tenant ID: Multi-tenant isolation (HIPAA requirement)
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Status: Current state of event processing
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ProcessingStatus status = ProcessingStatus.RUNNING;

    /**
     * Error Message: Details of last processing error (if any)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Update last processed event ID and timestamp
     */
    public void markProcessed(Long eventId) {
        this.lastProcessedEventId = eventId;
        this.lastProcessedAt = Instant.now();
        this.status = ProcessingStatus.RUNNING;
        this.errorMessage = null;
    }

    /**
     * Mark consumer as failed with error details
     */
    public void markFailed(String errorMessage) {
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.lastProcessedAt = Instant.now();
    }

    /**
     * Mark consumer as paused
     */
    public void markPaused() {
        this.status = ProcessingStatus.PAUSED;
        this.lastProcessedAt = Instant.now();
    }

    /**
     * Resume consumer from paused state
     */
    public void resume() {
        this.status = ProcessingStatus.RUNNING;
        this.errorMessage = null;
        this.lastProcessedAt = Instant.now();
    }

    /**
     * Processing Status Enum
     */
    public enum ProcessingStatus {
        /**
         * Consumer is actively processing events
         */
        RUNNING,

        /**
         * Consumer encountered an error and stopped
         */
        FAILED,

        /**
         * Consumer is temporarily paused (admin action)
         */
        PAUSED,

        /**
         * Consumer has processed all available events
         */
        UP_TO_DATE
    }
}
