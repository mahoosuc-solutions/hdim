package com.healthdata.ingestion.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for data ingestion initiation.
 *
 * <p>Returns a unique session identifier that can be used to track progress via the
 * /api/v1/ingestion/progress endpoint or stream events via the /api/v1/ingestion/stream-events
 * endpoint.
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResponse {

  /**
   * Unique session identifier for this ingestion request.
   *
   * <p>Use this ID to:
   *
   * <ul>
   *   <li>Query progress: GET /api/v1/ingestion/progress?sessionId={sessionId}
   *   <li>Stream events: GET /api/v1/ingestion/stream-events?sessionId={sessionId}
   *   <li>Cancel ingestion: POST /api/v1/ingestion/cancel?sessionId={sessionId}
   * </ul>
   */
  private String sessionId;

  /**
   * Current status of the ingestion request.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li><strong>STARTED:</strong> Ingestion has been queued and will begin shortly
   *   <li><strong>RUNNING:</strong> Ingestion is actively processing
   *   <li><strong>COMPLETED:</strong> Ingestion finished successfully
   *   <li><strong>FAILED:</strong> Ingestion encountered an error
   *   <li><strong>CANCELLED:</strong> Ingestion was manually cancelled
   * </ul>
   */
  private String status;

  /**
   * Human-readable message providing additional context.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>"Ingestion started successfully. Use sessionId to track progress."
   *   <li>"Invalid patient count. Must be between 10 and 10,000."
   * </ul>
   */
  private String message;
}
