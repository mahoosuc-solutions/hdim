package com.healthdata.caregap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Bulk Operation Response DTO
 *
 * Response payload for bulk care gap operations (close, intervention, priority update).
 * Includes success/failure counts and detailed error information for failed operations.
 *
 * Issue #241: Care Gap Bulk Actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResponse {

    /**
     * Total number of gaps requested for operation
     */
    private int totalRequested;

    /**
     * Number of gaps successfully processed
     */
    private int successCount;

    /**
     * Number of gaps that failed to process
     */
    private int failureCount;

    /**
     * List of gap IDs that were successfully processed
     */
    @Builder.Default
    private List<String> successfulGapIds = new ArrayList<>();

    /**
     * List of errors for failed operations
     */
    @Builder.Default
    private List<BulkOperationError> errors = new ArrayList<>();

    /**
     * Total processing time in milliseconds
     */
    private long processingTimeMs;

    /**
     * Optional message describing the operation result
     */
    private String message;
}
