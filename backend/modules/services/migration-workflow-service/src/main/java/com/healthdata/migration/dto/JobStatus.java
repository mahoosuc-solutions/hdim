package com.healthdata.migration.dto;

/**
 * Migration job status states
 */
public enum JobStatus {
    PENDING,      // Created, not started
    RUNNING,      // Currently processing
    PAUSED,       // User-paused
    COMPLETED,    // Successfully finished
    FAILED,       // Failed with errors
    CANCELLED,    // User-cancelled
    RETRYING      // Automatic retry in progress
}
