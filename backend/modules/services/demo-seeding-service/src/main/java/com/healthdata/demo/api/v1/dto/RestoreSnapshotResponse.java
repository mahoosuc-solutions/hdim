package com.healthdata.demo.api.v1.dto;

/**
 * Response DTO for snapshot restore operations.
 */
public class RestoreSnapshotResponse {
    private String snapshotName;
    private boolean success;
    private long restoreTimeMs;
    private String errorMessage;

    public String getSnapshotName() { return snapshotName; }
    public void setSnapshotName(String snapshotName) { this.snapshotName = snapshotName; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public long getRestoreTimeMs() { return restoreTimeMs; }
    public void setRestoreTimeMs(long restoreTimeMs) { this.restoreTimeMs = restoreTimeMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
