package com.healthdata.quality.persistence;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity for measure version history.
 *
 * HIPAA COMPLIANCE: Records in this table are immutable after creation.
 * The cqlText and valueSets fields must never be modified - only new versions should be created.
 *
 * Issue #152: Measure Versioning and Audit Trail
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "measure_versions", indexes = {
        @Index(name = "idx_measure_versions_measure_id", columnList = "measure_id"),
        @Index(name = "idx_measure_versions_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_measure_versions_current", columnList = "measure_id, is_current"),
        @Index(name = "idx_measure_versions_created_at", columnList = "measure_id, created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureVersionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "measure_id", nullable = false)
    private UUID measureId;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "version_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private VersionType versionType;

    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "cql_text", nullable = false, columnDefinition = "TEXT")
    private String cqlText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_sets", columnDefinition = "JSONB")
    private String valueSets;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_by_name", length = 255)
    private String createdByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    /**
     * Version type enumeration for semantic versioning.
     */
    public enum VersionType {
        MAJOR,  // Breaking changes, significant CQL logic changes
        MINOR,  // New features, backward-compatible additions
        PATCH   // Bug fixes, documentation updates
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (isCurrent == null) {
            isCurrent = false;
        }
        if (isPublished == null) {
            isPublished = false;
        }
    }

    /**
     * Parse semantic version string into components.
     *
     * @return Array of [major, minor, patch] or null if invalid
     */
    public int[] parseVersion() {
        if (version == null || version.isBlank()) {
            return null;
        }
        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new int[] {
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Generate next version string based on version type.
     *
     * @param currentVersion Current version string (e.g., "1.2.3")
     * @param type Version type (MAJOR, MINOR, PATCH)
     * @return Next version string
     */
    public static String nextVersion(String currentVersion, VersionType type) {
        if (currentVersion == null || currentVersion.isBlank()) {
            return "1.0.0";
        }
        String[] parts = currentVersion.split("\\.");
        if (parts.length != 3) {
            return "1.0.0";
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);

            return switch (type) {
                case MAJOR -> (major + 1) + ".0.0";
                case MINOR -> major + "." + (minor + 1) + ".0";
                case PATCH -> major + "." + minor + "." + (patch + 1);
            };
        } catch (NumberFormatException e) {
            return "1.0.0";
        }
    }
}
