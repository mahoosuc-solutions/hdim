package com.healthdata.ingestion.api.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Progress tracking response for active ingestion session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionProgressResponse {

    /**
     * Session ID.
     */
    private String sessionId;

    /**
     * Current status (RUNNING, COMPLETED, FAILED, CANCELLED).
     */
    private String status;

    /**
     * Progress percentage (0-100).
     */
    private Integer progressPercent;

    /**
     * Number of patients generated so far.
     */
    private Long patientsGenerated;

    /**
     * Number of patients persisted to FHIR service.
     */
    private Long patientsPersisted;

    /**
     * Number of care gaps created.
     */
    private Long careGapsCreated;

    /**
     * Number of quality measures seeded.
     */
    private Long measuresSeeded;

    /**
     * Start time (epoch milliseconds).
     */
    private Long startTimeMs;

    /**
     * Elapsed time (milliseconds).
     */
    private Long elapsedTimeMs;

    /**
     * Current processing stage.
     */
    private String currentStage;  // GENERATING, PERSISTING, CARE_GAPS, MEASURES, VALIDATING
}
