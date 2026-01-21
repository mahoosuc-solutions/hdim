package com.healthdata.{{SERVICE_NAME}}.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * {{ENTITY_DESCRIPTION}}
 *
 * HIPAA Compliance: This entity may contain PHI. Ensure proper access controls.
 * Multi-tenant: All queries MUST filter by tenantId to ensure data isolation.
 */
@Entity
@Table(name = "{{TABLE_NAME}}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class {{ENTITY_CLASS_NAME}} {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Tenant ID for multi-tenant data isolation.
     * CRITICAL: All repository queries MUST filter by this field.
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    // TODO: Add your domain-specific fields here
    // Example:
    // @Column(name = "field_name", nullable = false, length = 255)
    // private String fieldName;

    /**
     * Audit field: Record creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Audit field: Last update timestamp
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Audit field: User who created this record
     */
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /**
     * Audit field: User who last updated this record
     */
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
