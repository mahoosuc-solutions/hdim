package com.healthdata.featureflags;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant Feature Flag Entity
 *
 * Stores feature flag configuration per tenant to enable/disable integrations
 * and features on a per-customer basis.
 *
 * HIPAA Compliance:
 * - Multi-tenant isolation enforced via tenant_id
 * - Audit trail via created_at/updated_at/created_by/updated_by
 * - Configuration stored as JSON to support flexible integration configs
 *
 * Use Cases:
 * - Enable Twilio SMS reminders for specific tenants
 * - Enable SMART on FHIR only for Epic-integrated health systems
 * - Disable features during maintenance or troubleshooting
 * - A/B testing of new features with select customers
 */
@Entity
@Table(name = "tenant_feature_flags",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_tenant_feature_flags_tenant_feature",
           columnNames = {"tenant_id", "feature_key"}
       ),
       indexes = {
           @Index(name = "idx_tenant_feature_flags_tenant_id", columnList = "tenant_id"),
           @Index(name = "idx_tenant_feature_flags_feature_key", columnList = "feature_key"),
           @Index(name = "idx_tenant_feature_flags_enabled", columnList = "enabled")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantFeatureFlagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant ID (HIPAA §164.312(d) - Multi-tenant isolation)
     */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    /**
     * Feature key (kebab-case identifier)
     *
     * Standard feature keys:
     * - twilio-sms-reminders
     * - smart-on-fhir
     * - cds-hooks
     * - nowpow-sdoh
     * - validic-rpm
     */
    @Column(name = "feature_key", nullable = false, length = 255)
    private String featureKey;

    /**
     * Feature enabled flag
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    /**
     * Feature-specific configuration as JSON
     *
     * Examples:
     * - Twilio: {"reminder_days": [1, 3, 7], "default_from_number": "+11234567890"}
     * - SMART on FHIR: {"client_id": "abc123", "launch_url": "https://..."}
     * - CDS Hooks: {"discovery_url": "https://...", "enabled_hooks": ["patient-view"]}
     */
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    /**
     * Audit: When flag was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Audit: When flag was last updated
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Audit: User who created the flag
     */
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /**
     * Audit: User who last updated the flag
     */
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
