package com.healthdata.agentbuilder.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Agent version entity for immutable configuration snapshots.
 */
@Entity
@Table(name = "agent_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_configuration_id", nullable = false)
    private AgentConfiguration agentConfiguration;

    @Column(name = "version_number", nullable = false)
    private String versionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configurationSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VersionStatus status;

    @Column(name = "change_summary")
    private String changeSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type")
    private ChangeType changeType;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private String publishedBy;

    @Column(name = "rolled_back_at")
    private Instant rolledBackAt;

    @Column(name = "rolled_back_by")
    private String rolledBackBy;

    @Column(name = "rollback_reason")
    private String rollbackReason;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = VersionStatus.DRAFT;
        }
        if (changeType == null) {
            changeType = ChangeType.MINOR;
        }
    }

    public enum VersionStatus {
        DRAFT,
        PUBLISHED,
        ROLLED_BACK,
        SUPERSEDED
    }

    public enum ChangeType {
        MAJOR,
        MINOR,
        PATCH
    }
}
