package com.healthdata.cms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CMS Integration Configuration Entity
 * 
 * Stores CMS API integration settings per tenant.
 * Supports multiple integration paths (BCDA, DPC, multi-API).
 */
@Entity
@Table(name = "cms_integration_config", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_api_type", columnList = "api_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsIntegrationConfig {

    @Id
    private UUID id;

    // Multi-tenancy
    @Column(nullable = false, unique = true)
    private UUID tenantId;

    // Integration path
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationType apiType;

    // OAuth2 Credentials (encrypted in database)
    @Column(nullable = false)
    private String oauthClientId;

    @Column(nullable = false)
    private String oauthClientSecret;

    // CMS Organization ID (if applicable)
    @Column
    private String cmsOrganizationId;

    // CMS Organization Name
    @Column
    private String organizationName;

    // Environment configuration
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSandbox = true;

    // API endpoints
    @Column
    private String bcdaBaseUrl;

    @Column
    private String dpcBaseUrl;

    @Column
    private String blueButtonBaseUrl;

    @Column
    private String ab2dBaseUrl;

    // Integration status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IntegrationStatus status = IntegrationStatus.PENDING;

    // Last synchronization timestamp
    @Column
    private LocalDateTime lastSyncTimestamp;

    // Sync settings
    @Column
    @Builder.Default
    private Integer bcdaSyncIntervalHours = 24; // Daily

    @Column
    @Builder.Default
    private Boolean dpcRealTimeEnabled = true;

    @Column
    @Builder.Default
    private Integer dpcCacheTtlMinutes = 5;

    // Audit metadata
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime verifiedAt;

    @Column
    private UUID createdBy;

    @Column
    private UUID verifiedBy;

    // Configuration metadata
    @Column
    private String notes;

    @Column
    @Builder.Default
    private Boolean isActive = true;

    /**
     * CMS Integration Type
     */
    public enum IntegrationType {
        BCDA("Bulk Medicare Claims - Weekly Exports"),
        DPC("Real-time Point of Care Queries"),
        MULTI("Multi-API - BCDA + DPC + AB2D"),
        BLUE_BUTTON("Beneficiary-Initiated Data Sharing");

        private final String description;

        IntegrationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Integration Status Lifecycle
     */
    public enum IntegrationStatus {
        PENDING("Awaiting CMS API credentials"),
        TESTING("Testing OAuth2 connection"),
        VERIFIED("Connection verified, ready to sync"),
        ACTIVE("Actively syncing data"),
        PAUSED("Temporarily paused"),
        FAILED("Connection failed - requires remediation"),
        DISABLED("Disabled by user");

        private final String description;

        IntegrationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
