package com.healthdata.caregap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bulk Operation Error DTO
 *
 * Represents an individual error that occurred during a bulk operation.
 * Used in BulkOperationResponse to provide detailed error information.
 *
 * Issue #241: Care Gap Bulk Actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationError {

    /**
     * Care gap ID that failed to process
     */
    private String gapId;

    /**
     * Human-readable error message
     */
    private String errorMessage;

    /**
     * Error code for programmatic handling
     * Examples: CLOSURE_FAILED, VALIDATION_ERROR, NOT_FOUND, PERMISSION_DENIED
     */
    private String errorCode;

    /**
     * Optional stack trace or detailed error information (for debugging)
     */
    private String details;
}
