package com.healthdata.eventstore.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

/**
 * EventStoreEntry - JPA Entity for event_store table
 *
 * Purpose: Immutable log entry for a single domain event
 *
 * Key Design:
 * - Immutable (no setters except for JPA)
 * - JSONB for flexible event payloads
 * - Unique constraint on (aggregate_id, aggregate_type, event_version)
 * - Supports event sourcing, event replay, temporal queries
 *
 * Usage:
 * <pre>
 * EventStoreEntry entry = EventStoreEntry.builder()
 *     .aggregateId(patientId)
 *     .aggregateType("Patient")
 *     .eventId(UUID.randomUUID())
 *     .eventType("PatientCreatedEvent")
 *     .eventVersion(1)
 *     .payload(eventPayload)
 *     .occurredAt(Instant.now())
 *     .tenantId(tenantId)
 *     .build();
 * </pre>
 */
@Entity
@Table(name = "event_store",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_store_aggregate_version",
                columnNames = {"aggregate_id", "aggregate_type", "event_version"}
        ),
        indexes = {
                @Index(name = "idx_event_store_aggregate", columnList = "aggregate_id, aggregate_type"),
                @Index(name = "idx_event_store_event_type", columnList = "event_type"),
                @Index(name = "idx_event_store_tenant", columnList = "tenant_id"),
                @Index(name = "idx_event_store_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_event_store_correlation", columnList = "correlation_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class EventStoreEntry {

    /**
     * Primary Key: Auto-incrementing global event ID
     * Ensures total ordering of all events across all aggregates
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Aggregate ID: The entity this event applies to (e.g., patient UUID)
     */
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    /**
     * Aggregate Type: The type of entity (e.g., "Patient", "CareGap", "QualityMeasure")
     */
    @Column(name = "aggregate_type", nullable = false, length = 255)
    private String aggregateType;

    /**
     * Event ID: Unique identifier for this specific event
     */
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    /**
     * Event Type: The type of event (e.g., "PatientCreatedEvent")
     */
    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    /**
     * Event Version: Monotonically increasing version number for this aggregate
     * Starts at 1, increments with each event
     * Prevents duplicate/out-of-order events via unique constraint
     */
    @Column(name = "event_version", nullable = false)
    private Integer eventVersion;

    /**
     * Payload: Event data in JSON format
     * Stored as JSONB for efficient querying
     *
     * Example:
     * {
     *   "patientId": "123e4567-e89b-12d3-a456-426614174000",
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "dateOfBirth": "1980-01-15"
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    /**
     * Occurred At: Business timestamp when the event actually happened
     * May differ from recorded_at (system time)
     */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /**
     * Recorded At: System timestamp when the event was persisted
     * Set automatically by database
     */
    @Column(name = "recorded_at", nullable = false, updatable = false, insertable = false)
    private Instant recordedAt;

    /**
     * Tenant ID: Multi-tenant isolation (HIPAA requirement)
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Causation ID: The event that caused this event to be created
     * Used for event correlation and debugging
     */
    @Column(name = "causation_id")
    private UUID causationId;

    /**
     * Correlation ID: Groups related events across services
     * Used for distributed tracing
     */
    @Column(name = "correlation_id")
    private UUID correlationId;

    /**
     * User ID: The user who triggered this event
     * Used for audit trails
     */
    @Column(name = "user_id", length = 255)
    private String userId;

    /**
     * User Email: The email of the user who triggered this event
     * Used for audit trails
     */
    @Column(name = "user_email", length = 255)
    private String userEmail;

    /**
     * Immutability: Events MUST never be updated or deleted
     * This is enforced at the application layer
     */
    @PreUpdate
    protected void preventUpdate() {
        throw new UnsupportedOperationException("Event store entries are immutable and cannot be updated");
    }

    @PreRemove
    protected void preventDelete() {
        throw new UnsupportedOperationException("Event store entries are immutable and cannot be deleted");
    }
}
