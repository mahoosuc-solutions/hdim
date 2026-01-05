package com.healthdata.demo.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a database snapshot for quick demo reset.
 *
 * Snapshots enable:
 * - Quick restore between recording takes (< 30 seconds)
 * - Version management of demo states
 * - Safe experimentation during demos
 */
@Entity
@Table(name = "demo_snapshots")
public class DemoSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private DemoScenario scenario;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SnapshotStatus status = SnapshotStatus.CREATING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "last_restored_at")
    private Instant lastRestoredAt;

    @Column(name = "restore_count")
    private Integer restoreCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // Constructors
    public DemoSnapshot() {}

    public DemoSnapshot(String name, DemoScenario scenario, String filePath) {
        this.name = name;
        this.scenario = scenario;
        this.filePath = filePath;
        this.status = SnapshotStatus.CREATING;
    }

    // Business methods
    public void markReady(Long fileSizeBytes) {
        this.status = SnapshotStatus.READY;
        this.fileSizeBytes = fileSizeBytes;
    }

    public void markFailed() {
        this.status = SnapshotStatus.FAILED;
    }

    public void recordRestore() {
        this.lastRestoredAt = Instant.now();
        this.restoreCount = (this.restoreCount == null ? 0 : this.restoreCount) + 1;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public DemoScenario getScenario() { return scenario; }
    public void setScenario(DemoScenario scenario) { this.scenario = scenario; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public SnapshotStatus getStatus() { return status; }
    public void setStatus(SnapshotStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastRestoredAt() { return lastRestoredAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Integer getRestoreCount() { return restoreCount; }
    public void setRestoreCount(Integer restoreCount) { this.restoreCount = restoreCount; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    /**
     * Snapshot lifecycle states.
     */
    public enum SnapshotStatus {
        CREATING,   // Snapshot in progress
        READY,      // Available for restore
        RESTORING,  // Restore in progress
        FAILED,     // Creation or restore failed
        DELETED     // Marked for cleanup
    }
}
