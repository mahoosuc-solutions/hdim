package com.healthdata.{{DOMAIN}}event.projection;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * {{DOMAIN_PASCAL}}Projection - CQRS Read Model
 *
 * Denormalized projection built from domain events for optimized read queries.
 * Updated asynchronously via Kafka event listeners (eventual consistency < 500ms).
 *
 * Event Sources:
{{EVENT_SOURCES_COMMENT}}
 *
 * Multi-Tenant: All queries MUST filter by tenantId
 * HIPAA: May contain PHI - ensure proper access controls
 */
@Entity
@Table(
    name = "{{TABLE_NAME}}",
    indexes = {
        // Multi-tenant query performance (CRITICAL)
        @Index(name = "idx_{{TABLE_ABBR}}_tenant_id", columnList = "tenant_id"),

        // Unique constraint: one projection per entity per tenant
        @Index(name = "idx_{{TABLE_ABBR}}_tenant_{{DOMAIN}}",
               columnList = "tenant_id, {{DOMAIN}}_id",
               unique = true),

        // Query optimization: sort by last updated
        @Index(name = "idx_{{TABLE_ABBR}}_updated_at", columnList = "last_updated_at")

        // TODO: Add domain-specific indexes here (e.g., status, priority)
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class {{DOMAIN_PASCAL}}Projection {

    /**
     * Surrogate primary key (auto-increment for performance)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /**
     * Tenant ID for multi-tenant data isolation.
     * CRITICAL: All repository queries MUST filter by this field.
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Domain entity identifier (UUID from command-side)
     */
    @Column(name = "{{DOMAIN}}_id", nullable = false)
    private UUID {{DOMAIN}}Id;

    // ========================================
    // Domain-Specific Denormalized Fields
    // ========================================
    // TODO: Add denormalized fields from events here
    // Examples:
    // - Aggregate fields (counts, totals, averages)
    // - Frequently queried fields (status, priority, name)
    // - Pre-calculated flags (isActive, hasAlert, isOverdue)
    // - Joined data from multiple events (patient name + risk score + gap count)
    //
    // Remember: Denormalization trades storage for read performance

    /**
     * Event version for optimistic locking and event ordering.
     * Incremented with each event processed.
     */
    @Column(name = "event_version", nullable = false)
    @Builder.Default
    private Long eventVersion = 0L;

    /**
     * Timestamp when projection was first created (from first event)
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of last event processed (for staleness detection)
     */
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    /**
     * JPA lifecycle hook: Set createdAt on insert
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastUpdatedAt == null) {
            lastUpdatedAt = Instant.now();
        }
    }

    /**
     * JPA lifecycle hook: Update lastUpdatedAt on every update
     */
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = Instant.now();
    }

    /**
     * Increment event version (call when processing events)
     */
    public void incrementVersion() {
        this.eventVersion++;
    }
}
