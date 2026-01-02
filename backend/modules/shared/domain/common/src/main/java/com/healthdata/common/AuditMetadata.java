package com.healthdata.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

/**
 * Standard audit metadata for all domain entities.
 * Provides HIPAA-compliant audit trail information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditMetadata {

    /**
     * When the entity was created
     */
    private Instant createdAt;

    /**
     * User who created the entity
     */
    private String createdBy;

    /**
     * When the entity was last modified
     */
    private Instant lastModifiedAt;

    /**
     * User who last modified the entity
     */
    private String lastModifiedBy;

    /**
     * IP address of the user who performed the action
     */
    private String ipAddress;

    /**
     * User agent string (for web requests)
     */
    private String userAgent;

    /**
     * Session ID for tracking related operations
     */
    private String sessionId;

    /**
     * Organization/tenant ID for multi-tenancy
     */
    private String organizationId;

    /**
     * Purpose of access (for HIPAA compliance)
     */
    private String accessPurpose;

    /**
     * Additional context information
     */
    private String context;

    /**
     * Creates audit metadata for a new entity creation
     */
    public static AuditMetadata forCreate(String userId, String organizationId) {
        Instant now = Instant.now();
        return AuditMetadata.builder()
                .createdAt(now)
                .createdBy(userId)
                .lastModifiedAt(now)
                .lastModifiedBy(userId)
                .organizationId(organizationId)
                .build();
    }

    /**
     * Updates the modification timestamp and user
     */
    public void markModified(String userId) {
        this.lastModifiedAt = Instant.now();
        this.lastModifiedBy = userId;
    }
}
