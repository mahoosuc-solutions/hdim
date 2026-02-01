package com.healthdata.ingestion.api.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for ingestion start operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResponse {

    /**
     * Unique session ID for tracking progress.
     */
    private String sessionId;

    /**
     * Current status (STARTED, RUNNING, COMPLETED, FAILED, CANCELLED).
     */
    private String status;

    /**
     * Human-readable message.
     */
    private String message;
}
