package com.healthdata.migration.websocket;

/**
 * Types of WebSocket events for migration progress
 */
public enum MigrationEventType {
    PROGRESS_UPDATE,      // Periodic progress update
    RECORD_PROCESSED,     // Individual record processed (optional, high-volume)
    ERROR_OCCURRED,       // Error notification
    STATUS_CHANGED,       // Job status change
    CHECKPOINT_SAVED,     // Checkpoint saved
    JOB_COMPLETED         // Final summary
}
