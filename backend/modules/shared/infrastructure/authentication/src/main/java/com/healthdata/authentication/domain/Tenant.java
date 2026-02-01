package com.healthdata.authentication.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Tenant entity representing multi-tenant organizations in the system.
 * Each tenant represents an independent healthcare organization with isolated data.
 */
@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenants_status", columnList = "status"),
    @Index(name = "idx_tenants_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    /**
     * Unique tenant identifier (e.g., "acme-health", "valley-clinic").
     * Must be lowercase alphanumeric with hyphens, 3-63 characters.
     * Used in API calls via X-Tenant-ID header.
     */
    @Id
    @Column(name = "id", nullable = false, length = 255)
    private String id;

    /**
     * Human-readable tenant name (e.g., "Acme Health Systems").
     * Displayed in UI and reports.
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Tenant operational status.
     * Controls whether users can login and access services.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    /**
     * Timestamp when tenant was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when tenant was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Check if tenant is operational and allows user access.
     */
    public boolean isOperational() {
        return status != null && status.isOperational();
    }

    /**
     * Check if tenant can be reactivated from current status.
     */
    public boolean canBeReactivated() {
        return status != null && status.canBeReactivated();
    }

    /**
     * Activate tenant (set status to ACTIVE).
     */
    public void activate() {
        this.status = TenantStatus.ACTIVE;
    }

    /**
     * Suspend tenant (set status to SUSPENDED).
     */
    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
    }

    /**
     * Deactivate tenant permanently (set status to INACTIVE).
     */
    public void deactivate() {
        this.status = TenantStatus.INACTIVE;
    }
}
