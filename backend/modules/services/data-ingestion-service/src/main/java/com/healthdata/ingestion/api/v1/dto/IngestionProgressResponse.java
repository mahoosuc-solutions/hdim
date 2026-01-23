package com.healthdata.ingestion.api.v1.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for ingestion progress tracking.
 *
 * <p>Provides real-time metrics on data ingestion progress, including patient counts, care gaps
 * created, quality measures seeded, and current pipeline stage.
 *
 * <p><strong>Pipeline Stages:</strong>
 *
 * <ul>
 *   <li><strong>INITIALIZING (0-5%):</strong> Session setup and configuration
 *   <li><strong>GENERATING (5-40%):</strong> Synthetic patient generation using Faker library
 *   <li><strong>PERSISTING (40-70%):</strong> Batch persistence to FHIR service
 *   <li><strong>CARE_GAPS (70-90%):</strong> Care gap identification and creation
 *   <li><strong>MEASURES (90-100%):</strong> Quality measure seeding and evaluation
 * </ul>
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionProgressResponse {

  /** Unique session identifier. */
  private String sessionId;

  /**
   * Current status.
   *
   * <p>Values: RUNNING, COMPLETED, FAILED, CANCELLED
   */
  private String status;

  /** Progress percentage (0-100). */
  @Builder.Default private Integer progressPercent = 0;

  /** Total patients generated so far. */
  @Builder.Default private Long patientsGenerated = 0L;

  /** Total patients successfully persisted to FHIR service. */
  @Builder.Default private Long patientsPersisted = 0L;

  /** Total care gaps created. */
  @Builder.Default private Long careGapsCreated = 0L;

  /** Total quality measures seeded. */
  @Builder.Default private Long measuresSeeded = 0L;

  /** Timestamp when ingestion started (ISO 8601 format). */
  private Instant startTime;

  /** Time elapsed since ingestion started (milliseconds). */
  @Builder.Default private Long elapsedTimeMs = 0L;

  /**
   * Current pipeline stage.
   *
   * <p>Values: INITIALIZING, GENERATING, PERSISTING, CARE_GAPS, MEASURES
   */
  private String currentStage;

  /** Human-readable description of current operation. */
  private String currentOperation;

  /** Error message if status is FAILED. */
  private String errorMessage;

  /** Estimated time remaining (milliseconds) - calculated based on current throughput. */
  private Long estimatedTimeRemainingMs;
}
