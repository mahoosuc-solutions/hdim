package com.healthdata.demo.api.v1.dto;

import java.time.Instant;

/**
 * Response DTO for snapshot information.
 */
public class SnapshotResponse {
    private String id;
    private String name;
    private String description;
    private Long fileSizeBytes;
    private String status;
    private Instant createdAt;
    private String createdBy;
    private Instant lastRestoredAt;
    private Integer restoreCount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getLastRestoredAt() { return lastRestoredAt; }
    public void setLastRestoredAt(Instant lastRestoredAt) { this.lastRestoredAt = lastRestoredAt; }

    public Integer getRestoreCount() { return restoreCount; }
    public void setRestoreCount(Integer restoreCount) { this.restoreCount = restoreCount; }
}
